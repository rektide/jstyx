import struct, time

class StyxException(Exception):
    def __init__(self, message):
        self.message = message
    def __str__(self):
        return "Styx Exception: %s" % self.message

class Qid(object):
    """Class representing the server's view of a file."""
    
    def __init__(self, msgBytes):
        # TODO: decode the type byte into meaningful string, e.g. "d" for a directory
        self.type, self.version, self.path = struct.unpack("<BIQ", msgBytes)
    
    def __str__(self):
        return "qid(%s, %s, %s)" % (self.type, self.version, self.path)

class DirEntry(object):
    """Class representing an entry in a directory (i.e. the result of a stat message)."""
    
    def __init__(self, bytes):
        self.size, self.type, self.dev = struct.unpack('<HHI', bytes[:8])
        self.qid = Qid(bytes[8:21])
        self.mode, self.lastAccessTime, self.lastModifiedTime, self.length, nameLen = struct.unpack('<IIIQH', bytes[21:43])
        self.name = bytes[43:43+nameLen].decode('utf-8')
        uidLen, = struct.unpack('<H', bytes[43+nameLen : 45+nameLen])
        self.user = bytes[45+nameLen : 45+nameLen+uidLen].decode('utf-8')
        gidLen, = struct.unpack('<H', bytes[45+nameLen+uidLen : 47+nameLen+uidLen])
        self.group = bytes[47+nameLen+uidLen : 47+nameLen+uidLen+gidLen]
        lastModLen, = struct.unpack('<H', bytes[47+nameLen+uidLen+gidLen : 49+nameLen+uidLen+gidLen])
        self.lastModifiedBy = bytes[49+nameLen+uidLen+gidLen : 49+nameLen+uidLen+gidLen+lastModLen].decode('utf-8')
    
    def __str__(self):
        s  = "%s, %s, %s, %s, " % (self.size, self.type, self.dev, self.qid)
        s += "%s, %s, " % (self.mode, self.lastAccessTime)
        s += "%s, %s, " % (self.lastModifiedTime, self.length)
        s += "%s, %s, %s, %s" % (self.name, self.user, self.group, self.lastModifiedBy)
        return s

class StyxMessage(object):
    """Abstract class representing a message in the Styx protocol."""
    
    def __getBytes(self):
        """Gets the message as a string."""
        return struct.pack('<IBH', self.length, self.type, self.tag) + self.messageBody
    
    def __str__(self):
        return "%s: %s, %s, %s, %s" % (self.name, self.length, self.type, self.tag, self.body)
    
    bytes = property(__getBytes, None, None, "message bytes as a string")

class RerrorMessage(StyxMessage):
    """Message returned to indicate an error condition."""
    
    def __init__(self, message):
        self.name = "Rerror"
        self.length, self.type, self.tag, strLen = struct.unpack('<IBHH', message[:9])
        self.message = message[9:9+strLen].decode('utf-8')
        self.body = self.message

class TversionMessage(StyxMessage):
    """Message sent to negotiate the protocol version and message size."""
    
    def __init__(self, maxMessageSize=8216, version="9P2000"):
        self.name = "Tversion"
        self.maxMessageSize = maxMessageSize
        self.version = version.encode('utf-8')
        self.length = 13 + len(self.version)
        self.type = 100
        # The message tag gets set just before the message is sent by the StyxClient class
        self.messageBody = struct.pack('<IH', self.maxMessageSize, len(self.version)) + self.version
        self.body = "%s, %s" % (self.maxMessageSize, self.version)

class RversionMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rversion"
        self.length, self.type, self.tag, self.maxMessageSize, strLen = struct.unpack('<IBHIH', message[:13])
        # TODO: check that the given string length is correct
        self.version = message[13:13+strLen].decode('utf-8')
        self.body = "%s, %s" % (self.maxMessageSize, self.version)

class TattachMessage(StyxMessage):
    """Message sent to attach to the server as a certain user."""
    
    def __init__(self, fid, user, afid=0xffffffffL, aname=""):
        self.name="Tattach"
        self.fid = fid
        self.afid = afid
        self.user = user.encode('utf-8')
        self.aname = aname.encode('utf-8')
        self.length = 19 + len(self.user) + len(self.aname)
        self.type = 104
        # The message tag gets set just before the message is sent
        # TODO: this could be more efficient, but does it matter with such a short message?
        self.messageBody = struct.pack('<IIH', self.fid, self.afid, len(self.user))
        self.messageBody += self.user
        self.messageBody += struct.pack("<H", len(self.aname))
        self.messageBody += self.aname
        self.body = "%s, %s, %s, %s" % (self.fid, self.afid, self.user, self.aname)

class RattachMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rattach"
        self.length, self.type, self.tag = struct.unpack('<IBH', message[:7])
        self.qid = Qid(message[7:]) # will catch message length errors
        self.body = "%s" % (self.qid)

class TwalkMessage(StyxMessage):
    """Message sent to get a handle to a file on the server."""
    
    def __init__(self, baseFid, newFid, path):
        """baseFid is the fid representing the start of the walk (normally the
           root of the server. newFid is the fid that will represent the target
           file (can be the same as baseFid). path is the path of the target
           file relative to the file represented by baseFid."""
        self.name="Twalk"
        self.baseFid = baseFid
        self.newFid = newFid
        self.type = 110
        self.pathElements=[]
        self.length = 17 # Will be updated with the length of the path elements
        pathElStr = ""
        # Cycle through the path elements, adding them to a message string
        for el in path.split("/"):
            if len(el) > 0:
                utf8el = el.encode('utf-8')
                self.length += 2 + len(utf8el)
                pathElStr += struct.pack('<H', len(utf8el)) + utf8el
                self.pathElements.append(el)
        self.messageBody = struct.pack('<IIH', self.baseFid, self.newFid, len(self.pathElements)) + pathElStr
        self.body = "%s, %s, %s" % (self.baseFid, self.newFid, path)

class RwalkMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rwalk"
        self.length, self.type, self.tag, self.numSuccessfulWalks = struct.unpack('<IBHH', message[:9])
        self.qids = []
        self.body = ""
        for i in range(self.numSuccessfulWalks):
            qid = Qid(message[9 + 13*i : 9 + 13*(i+1)])
            self.qids.append(qid)
            self.body += "%s" % qid
            if i < self.numSuccessfulWalks - 1:
                self.body += ", "
        self.body = "%s, %s" % (len(self.qids), self.body)

def getMode(modeStr):
    if modeStr == 'r':
        return 0
    elif modeStr == 'w': # or mode == 'a': # The StyxFile class takes care of appending to files
        return 1
    elif modeStr == 'r+':
        return 2
    elif modeStr == 'e':
        return 3 # execute mode, not present in regular Python files
    else:
        raise StyxException("Invalid mode string: must be 'r', 'w', 'r+' or 'e'")    

class TopenMessage(StyxMessage):
    """Message sent to prepare an existing file for reading or writing."""
    
    def __init__(self, fid, mode, truncate=False, deleteOnClose=False):
        self.name="Topen"
        self.length = 12
        self.type = 112
        self.fid = fid
        self.mode = getMode(mode)
        if truncate:
            # All write operations will truncate the file
            # TODO: should this be the default for write operations?
            if self.mode != 1 and self.mode != 2:
                raise StyxException("Can only truncate file when opening in write mode")
            self.mode |= 0x10
        if deleteOnClose:
            self.mode |= 0x40
        self.messageBody = struct.pack('<IB', self.fid, self.mode)
        self.body = "%s, %s" % (self.fid, self.mode)

class RopenMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Ropen"
        self.length, self.type, self.tag = struct.unpack('<IBH', message[:7])
        self.qid = Qid(message[7:20])
        self.iounit, = struct.unpack('<I', message[20:24])
        self.body = "%s, %s" % (self.qid, self.iounit)

class TcreateMessage(StyxMessage):
    """Message sent to create a new file on the server."""
    
    def __init__(self, fid, name, mode, isDirectory=False, perm=-1, isAppendOnly=False, isExclusive=False, deleteOnClose=False):
        self.name = "Tcreate"
        self.name = name.encode('utf-8')
        self.length = 18 + len(self.name)
        self.type = 114
        self.fid = fid
        if perm < 0:
            # Use default permissions
            if isDirectory: self.fullPerm = 0755
            else: self.fullPerm = 0644
        else:
            self.fullPerm = perm
        if isDirectory:
            self.fullPerm |= 0x80000000L
        if isAppendOnly:
            self.fullPerm |= 0x40000000L
        if isExclusive:
            self.fullPerm |= 0x20000000L
        self.mode = getMode(mode)
        if deleteOnClose:
            self.mode |= 0x40
        self.messageBody  = struct.pack('<IH', self.fid, len(self.name))
        self.messageBody += self.name + struct.pack('<IB', self.fullPerm, self.mode)
        self.body = "%s, %s, %s, %s" % (self.fid, self.name, self.fullPerm, self.mode)

class RcreateMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rcreate"
        self.length, self.type, self.tag = struct.unpack('<IBH', message[:7])
        self.qid = Qid(message[7:20])
        self.iounit, = struct.unpack('<I', message[20:24])
        self.body = "%s, %s" % (self.qid, self.iounit)        

class TclunkMessage(StyxMessage):
    """Message sent to forget about a fid (i.e. close a file)."""
    
    def __init__(self, fid):
        self.name = "Tclunk"
        self.length = 11
        self.type = 120
        self.fid = fid
        self.messageBody = struct.pack('<I', self.fid)
        self.body = "%s" % self.fid

class RclunkMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rclunk"
        self.length, self.type, self.tag = struct.unpack('<IBH', message[:7])
        self.body = ""

class TreadMessage(StyxMessage):
    """Message sent to read data from a file."""
    
    def __init__(self, fid, offset, count):
        self.name = "Tread"
        self.length = 23
        self.type = 116
        self.fid = fid
        self.offset = offset
        self.count = count
        self.messageBody = struct.pack('<IQI', self.fid, self.offset, self.count)
        self.body = "%s, %s, %s" % (self.fid, self.offset, self.count)

class RreadMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rread"
        self.length, self.type, self.tag, self.count = struct.unpack('<IBHI', message[:11])
        self.data = message[11:]
        self.body = "%s, %s" % (self.count, self.data[:20]) # output the first 20 bytes

class TwriteMessage(StyxMessage):
    """Message sent to write data to a file."""
    
    def __init__(self, fid, offset, data):
        """Note the data must be already encoded in utf-8"""
        self.name = "Twrite"
        self.length = 23 + len(data)
        self.type = 118
        self.fid = fid
        self.offset = offset
        self.count = len(data)
        self.data = data
        self.messageBody = struct.pack('<IQI', self.fid, self.offset, self.count) + self.data
        self.body = "%s, %s, %s, %s" % (self.fid, self.offset, self.count, self.data[:20])

class RwriteMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rwrite"
        self.length, self.type, self.tag, self.count = struct.unpack('<IBHI', message)
        self.body = "%s" % (self.count)

class TflushMessage(StyxMessage):
    """Message sent to abort a previous message."""
    
    def __init__(self, oldtag):
        self.name = "Tflush"
        self.length = 9
        self.type = 108
        self.oldtag = oldtag
        self.messageBody = struct.pack('<H', self.oldtag)
        self.body = "%s" % self.oldtag

class RflushMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rflush"
        self.length, self.type, self.tag = struct.unpack('<IBH', message[:7])
        self.body = ""

class TstatMessage(StyxMessage):
    """Message sent to enquire about the status of a file (type, length, permissions etc)."""
    
    def __init__(self, fid):
        self.name = "Tstat"
        self.length = 11
        self.type = 124
        self.fid = fid
        self.messageBody = struct.pack('<I', self.fid)
        self.body = "%s" % self.fid

class RstatMessage(StyxMessage):
    
    def __init__(self, message):
        self.name = "Rstat"
        self.length, self.type, self.tag = struct.unpack('<IBH', message[:7])
        self.stat = DirEntry(message[7:])
        self.body = "%s" % self.stat