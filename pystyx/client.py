# Simple Styx client 

from messages import *
from socket import socket, timeout
try:
    from socket import ssl
    SSL_SUPPORTED = True
except:
    SSL_SUPPORTED = False
from threading import Thread, Condition
try:
    from cStringIO import StringIO
except:
    # Use the python version if the C version isn't available
    from StringIO import StringIO
import time

def _getFirstUnused(limit, func, thing):        
    """(Private) Gets the next unused tag or fid.
    func(i) is a function that returns True if tag or fid i is in use."""
    i = 0;
    found = False
    while i < limit and not found:
        if not func(i):
            found = True
        else:
            i += 1
    if i < limit:
        return i
    else:
        # very unlikely to happen unless there is a bug
        raise StyxException("No more free %s!" % (thing))

class StyxConnection(object):
    
    def __init__(self, hostname, port, useSSL = False):
        self.hostname = hostname
        self.port = port
        self.__s = socket()
        self.__s.settimeout(2)
        self.__lock = Condition() # used to synchronize threads
        self.__rMessages = {} # will be filled by rmessages that have arrived
        self.__fidsInUse = [] # List of fids that are currently in use
        self.__tClunksPending = {} # outstanding Tclunk messages (tags and fids)
        self.useSSL = useSSL
        self.authenticated = False # Authentication not supported yet
    
    def connect(self, user=""):
        self.__s.connect((self.hostname, self.port)) # TODO: catch error if can't connect
        if self.useSSL:
            if SSL_SUPPORTED:
                # TODO: how do client authentication?
                self.__ss = ssl(self.__s) #, "z:\\cert\\jonblowerkey.pem", "z:\\cert\\jonblowercert.pem
                self.__s.settimeout(None)
            else:
                raise StyxException("SSL is not supported (ssl module is missing)")
        self.__t = Thread(target=self._receiveMessages) # start message-receiving thread
        self.__t.start()
        try:
            rVerMsg = self.sendMessage(TversionMessage())
            self.maxMessageSize = rVerMsg.maxMessageSize
            self.protocolversion = rVerMsg.version
            self.rootFid = self.getFreeFid() # The fid representing the root of the server
            rAttMsg = self.sendMessage(TattachMessage(self.rootFid, user))
            self.user = user
        except StyxException, se:
            print "Error connecting to server:", se.message
            self.disconnect()
    
    def disconnect(self):
        """Disconnects from the remote server."""
        # Clunk all outstanding fids
        # Do this in reverse order so root fid gets clunked last
        while len(self.__fidsInUse) > 0:
            # The fid gets automatically removed from self.__fidsInUse when the
            # rClunkMessage arrives
            rClunkMsg = self.sendMessage(TclunkMessage(self.__fidsInUse[-1]))            
        # Flush all outstanding messages
        for tag in self.__rMessages.keys():
            rFlushMsg = self.sendMessage(TflushMessage(tag))        
        self.__s.close()
        self.__t.join() # Wait for message-receiving thread to finish

    # This will run as a thread, listening for incoming messages on the socket
    def _receiveMessages(self):        
        buffer = ''
        done = False
        while not done:
            try:
                if self.useSSL:
                    data = self.__ss.read(8192)
                else:
                    data = self.__s.recv(8192)
            except timeout:
                # We ignore timeout errors. This is just to give the system
                # a chance to check if the socket has been closed
                pass
            except Exception, e:
                # This will happen if the socket has been closed locally or
                # remotely
                done = True
                raise e
            else:            
                # There may be more than one message (or a partial message)
                buffer += data # TODO: other appending methods would be more efficient
                if len(buffer) >= 4:
                    length = struct.unpack("<I", buffer[:4])[0]
                    if len(buffer) >= length:
                        message = buffer[:length]
                        buffer = buffer[length:]
                        # We now have a whole message called "message"
                        length, type, tag = struct.unpack("<IBH", message[:7])
                        self.__lock.acquire()
                        self.__rMessages[tag] = message
                        self.__lock.notifyAll() # notify that a new message has arrived
                        self.__lock.release()

    def sendMessageAsync(self, message):
        """Sends a message and returns immediately, returning the message's tag.
           Use getReply() to get the reply."""
        message.tag = self._getFreeTag()
        print message
        self.__rMessages[message.tag] = None # Makes sure the tag exists so it is not reused prematurely
        if message.type == 120:
            # This is a Tclunk message so we are trying to close the file.
            # Remember the fid so we can free it when the Rclunk arrives.
            self.__tClunksPending[message.tag] = message.fid
        if self.useSSL:
            self.__ss.write(message.bytes)
        else:
            self.__s.send(message.bytes)
        return message.tag
    
    def getReply(self, tag):
        """Waits for the message with the given tag to arrive, then returns it.
           The returned message is typed correctly as an RxxxMessage."""
        self.__lock.acquire()
        while self.__rMessages[tag] == None:
            self.__lock.wait() # Wait for a new message to arrive
        msg = self.__rMessages[tag]
        del self.__rMessages[tag]
        self.__lock.release()
        type, = struct.unpack("<B", msg[4:5])
        if type == 101:
            message = RversionMessage(msg)
        elif type == 105:
            message = RattachMessage(msg)
        elif type == 107:
            message = RerrorMessage(msg)
            print message
            raise StyxException(message.message)
        elif type == 109:
            message = RflushMessage(msg)
        elif type == 111:
            message = RwalkMessage(msg)
        elif type == 113:
            message = RopenMessage(msg)
        elif type == 115:
            message = RcreateMessage(msg)
        elif type == 117:
            message = RreadMessage(msg)
        elif type == 119:
            message = RwriteMessage(msg)
        elif type == 121:
            message = RclunkMessage(msg)
            # Find the fid associated with the original Tclunk and free it
            fid = self.__tClunksPending[tag]
            del self.__tClunksPending[tag]
            self.returnFid(fid)
        elif type == 125:
            message = RstatMessage(msg)
        else:
            raise "Unknown message type:", type
        print message
        return message

    def sendMessage(self, message):
        """Sends a message, waits for its reply, then returns the reply."""
        tag = self.sendMessageAsync(message)
        return self.getReply(tag)
    
    def open(self, path, mode='r'):
        """Opens a file on this connection. Throws a StyxException if it does
           not exist."""
        # First get a new fid for this file
        fid = self.getFreeFid()
        # Now walk this fid to the given file
        try:
            tWalkMsg = TwalkMessage(self.rootFid, fid, path)
            rWalkMsg = self.sendMessage(tWalkMsg)
            if rWalkMsg.numSuccessfulWalks != len(tWalkMsg.pathElements):
                raise StyxException("%s: file or directory not found" % tWalkMsg.pathElements[rWalkMsg.numSuccessfulWalks])
            # TODO: what shall we do if the file is a directory?
            # Create, open and return a StyxFile object
            rOpenMsg = self.sendMessage(TopenMessage(fid, mode))
            return StyxFile(self, path, fid, rOpenMsg.iounit, mode)
        except StyxException, se:
            # Error opening the file; fid is not valid
            self.returnFid(fid)
            raise se
    
    # TODO: could also have a conn.create() method.
    
    def _getFreeTag(self):
        """(Private) Gets the next unused tag."""
        return _getFirstUnused(0xffff, lambda i: self.__rMessages.has_key(i), "tags")
    
    def getFreeFid(self):
        """Gets the next unused fid."""
        fid = _getFirstUnused(0xffffffffL, lambda i: i in self.__fidsInUse, "fids")
        self.__fidsInUse.append(fid)
        return fid
    
    def returnFid(self, fid):
        """Returns a fid after we are finished with it."""
        self.__fidsInUse.remove(fid)


class StyxFile(object):
    """A file or directory on a Styx server. When this is complete, this will
    be a file-like object. TODO: make into an iterator?"""
    
    def __init__(self, conn, name, fid, iounit, mode='r'):
        self.__conn = conn # The StyxConnection on which this file sits
        self.__name = name # the full path of this file?
        self.mode = mode # TODO: deal with 'a' (append to file)
        self.closed = True # will be set false when the file is opened or created
        self.__fid = fid
        self.__iounit = iounit # the maximum payload of a Twrite or Rread message
        self.__offset = 0 # The file position
        # TODO: have an "open" (or "closed") flag: disallow ops on closed files
    
    def close(self):
        """Closes the file. Once this has been done, no more read or write
           operations will be possible on this file."""
        # i.e. clunks the fid
        self.flush() # Flush any bytes that need to be written
        # The fid is automatically returned to the pool when the Rclunk arrives
        rClunkMsg = self.__conn.sendMessage(TclunkMessage(self.__fid))
        # TODO: abort all outstanding messages with flushes
    
    def read(self, size=-1):
        """Reads at most size bytes from the current file position, returning the result as a string.
           If the size argument is negative or omitted, read until EOF is reached."""
        contents = StringIO()
        bytesToRead = size
        while True:
            if size < 0 or bytesToRead > self.__iounit:
                n = self.__iounit
            elif bytesToRead <= 0:
                break # bytesToRead shouldn't be < 0 here, but this is defensive
            else:
                n = bytesToRead
            rReadMsg = self.__conn.sendMessage(TreadMessage(self.__fid, self.__offset, n))
            if len (rReadMsg.data) == 0:
                break # EOF has been reached
            self.__offset += len(rReadMsg.data)
            contents.write(rReadMsg.data)
            if size >= 0:
                bytesToRead -= len(rReadMsg.data)
        return contents.getvalue()
    
    # TODO: implement an asynchronous read() method
    
    def write(self, str):
        """Writes the string str to the file at the current offset. Data are
           written immediately, i.e. no buffering occurs."""
        # TODO: implement buffering?
        # May need to write the data in several chunks
        utf8str = str.encode('utf-8')
        pos = 0 # Marker position in the string
        while True:
            bytesLeft = len(utf8str) - pos
            if bytesLeft <= 0:
                break # bytesLeft should never be < 0 here, but this is defensive
            elif bytesLeft > self.__iounit:
                n = self.__iounit
            else:
                n = bytesLeft
            rWriteMsg = self.__conn.sendMessage(TwriteMessage(self.__fid, self.__offset, utf8str[pos:pos+n]))
            if rWriteMsg.count != n:
                raise StyxException("Error writing data.")
            pos += n
            self.__offset += n
    
    # TODO: implement an asynchronous write() message?
    
    def seek(self, offset, whence=0):
        """Move to a new file position."""
        if whence == 0:
            # Seek from the beginning of the file
            self.__offset = offset
        elif whence == 1:
            # Seek from the current file position
            self.__offset += offset
            if (self.__offset < 0):
                self.__offset = 0
        elif whence == 2:
            # TODO: can't handle this right now as we don't know the file length
            raise StyxException("Can't seek from the end of the file")
        else:
            raise StyxException("whence must be 0, 1 or 2")
    
    def flush(self):
        """Flushes any buffered data to the file."""
        # TODO: currently does nothing as data are not buffered.
    
    def stat(self):
        """Gets all the information about this file (permissions, length etc)."""
        rStatMsg = self.__conn.sendMessage(TstatMessage(self.__fid))
        # TODO: store all the file information in this instance
        return rStatMsg.stat
    
    def tell(self):
        """Returns the current file position as a (possibly long) integer."""
        return self.__offset
    
    # TODO: implement self.truncate (will require TwstatMessage)
    # TODO: readline, readlines writelines, name


def main():
    try:
        conn = StyxConnection("localhost", 9000, True)
        conn.connect("Jon")
        f = conn.open("mina/LICENSE.txt")
        s = f.read()
        print s
        f.close()
        
    except StyxException, se:
        print "An error occurred:", se.message
    conn.disconnect()
    print "disconnecting..."
    print "done."

if __name__ == '__main__':
    main()