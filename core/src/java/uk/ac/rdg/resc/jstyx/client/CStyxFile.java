/*
 * Copyright (c) 2005 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.jstyx.client;

import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.Date;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxBuffer;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;
import uk.ac.rdg.resc.jstyx.messages.*;

/**
 * A Styx file (or directory) from the point of view of the client. (It is called
 * a CStyxFile in order to avoid confusion with the server-side StyxFile class.)
 * @todo should we keep a cache of all the children of this file?
 * @todo implement a create() method
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:17  jonblower
 * Initial revision
 *
 */
public class CStyxFile extends MessageCallback
{
    
    private StyxConnection conn; // The connection on which the file sits
    private String path;         // The path of the file relative to the
                                 // root of the file server
    private String name;         // The name of the file (i.e. the last
                                 // part of the path)
    private long fid;            // The client's handle to this file;
                                 // this fid will never be opened so it can
                                 // always be used as the start point of a
                                 // walk message
    private long openFid;        // This fid will be created when opening
                                 // the file, and invalidated when the file
                                 // is closed.
    private DirEntry dirEntry;   // The server's representation of this file
    private int ioUnit;          // The maximum number of bytes that can be
                                 // written to, or read from, this file in
                                 // a single operation.
    private int mode;            // The mode under which we have the file open
                                 // -1 means that the file is not open
    private long offset;         // The current position in the file from the
                                 // point of view of this client
    private Vector listeners;    // The CStyxFileChangeListeners that are waiting
                                 // for notification of changes to this file
    
    
    /**
     * Creates a new instance of CStyxFile. This doesn't actually open or create
     * the file; use open() or create() for this.
     */
    public CStyxFile(StyxConnection conn, String path)
    {
        this.conn = conn;
        this.path = path.trim();
        // Get the name of the file (the last part of the path)
        String[] pathEls = this.path.split("/");
        if (pathEls.length > 0)
        {
            this.name = pathEls[pathEls.length - 1];
        }
        else
        {
            this.name = this.path;
        }
        this.fid = -1;
        this.openFid = -1;
        this.ioUnit = 0;
        this.mode = -1;
        this.offset = 0;
        this.dirEntry = null;
        
        this.listeners = new Vector();
    }
    
    /**
     * Creates a new instance of CStyxFile. This doesn't actually open the file;
     * use open() for this.
     */
    public CStyxFile(StyxConnection conn, String basePath, DirEntry dirEntry)
    {
        this(conn, basePath + "/" + dirEntry.getFileName());
        this.dirEntry = dirEntry;
    }
    
    /**
     * Package-private constructor that is used by StyxConnection to create a 
     * CStyxFile representing the root of the server; the fid is already known
     * in this case. Should not be used normally.
     */
    CStyxFile(StyxConnection conn, String path, long fid)
    {
        this(conn, path);
        this.fid = fid;
    }
    
    /**
     * @return The connection on which this file sits
     */
    public StyxConnection getConnection()
    {
        return this.conn;
    }
    
    /**
     * Gets the name of the file, i.e. the last part of the path
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * Gets the full path of the file
     */
    public String getPath()
    {
        return this.path;
    }
    
    /**
     * @return true if this is a directory
     */
    public boolean isDirectory() throws StyxException
    {
        if (this.dirEntry == null)
        {
            this.refresh();
        }
        return (this.dirEntry.getQid().getType() == 128);
    }
    
    /**
     * @return the owner of the file
     */
    public String getOwner() throws StyxException
    {
        if (this.dirEntry == null)
        {
            this.refresh();
        }
        return this.dirEntry.getOwner();
    }
    
    /**
     * @return the length of the file in bytes
     */
    public long getLength() throws StyxException
    {
        if (this.dirEntry == null)
        {
            this.refresh();
        }
        return this.dirEntry.getFileLength().asLong();
    }
    
    /**
     * @return the last modified time of the file
     */
    public Date getLastModified() throws StyxException
    {
        if (this.dirEntry == null)
        {
            this.refresh();
        }
        return new Date(this.dirEntry.getLastModifiedTime() * 1000);
    }
    
    /**
     * @return the DirEntry object containing this file's attributes
     */
    public DirEntry getDirEntry() throws StyxException
    {
        if (this.dirEntry == null)
        {
            this.refresh();
        }
        return this.dirEntry;
    }
    
    /**
     * @return the maximum number of bytes that can be read from or written to
     * this file in a single operation
     */
    public int getIOUnit()
    {
        return this.ioUnit;
    }
    
    /**
     * Gets a fid for this file; gets a new fid from the pool and walks it to the
     * location of this file. Does not block; calls the replyArrived() method
     * of the given callback if the walk was successful, or the error() method
     * otherwise.
     */
    private void getFidAsync(MessageCallback callback)
    {
        long newFid = this.conn.getFreeFid();
        TwalkMessage tWalkMsg = new TwalkMessage(this.conn.getRootFid(), newFid,
            this.path);
        this.conn.sendAsync(tWalkMsg, new WalkCallback(callback));
    }
    
    /**
     * Gets a fid for this file that we can open (equivalent to cloning a fid
     * in older versions of Styx). Does not block.
     */
    private void getOpenFidAsync(MessageCallback callback)
    {
        // Need a fid for this file first
        if (this.fid < 0)
        {
            this.getFidAsync(new NestedMessageCallback(callback)
            {
                public void replyArrived(StyxMessage message)
                {
                    getOpenFidAsync(this.nestedCallback);
                }
            });
        }
        else
        {
            long newFid = this.conn.getFreeFid();
            TwalkMessage tWalkMsg = new TwalkMessage(this.fid, newFid, "");
            this.conn.sendAsync(tWalkMsg, new WalkCallback(callback));
        }
    }
    
    private class WalkCallback extends NestedMessageCallback
    {
        public WalkCallback(MessageCallback nestedCallback)
        {
            super(nestedCallback);
        }
        public void replyArrived(StyxMessage message)
        {
            RwalkMessage rWalkMsg = (RwalkMessage)message;
            TwalkMessage tWalkMsg = (TwalkMessage)this.tMessage;
            // Check that the walk was successful
            if (rWalkMsg.getNumSuccessfulWalks() == tWalkMsg.getNumPathElements())
            {
                if (fid < 0)
                {
                    // This must have been the walk to establish the base fid 
                    // for this file (i.e. the fid that can be walked)
                    fid = tWalkMsg.getNewFid();
                }
                else if (openFid < 0)
                {
                    // This must have been the walk to establish a fid for this
                    // file that we can open (but can't be walked)
                    openFid = tWalkMsg.getNewFid();
                }
                else
                {
                    this.error("Internal error: both fid and openFid are set");
                }
                this.nestedCallback.replyArrived(message);
            }
            else
            {
                // Get the element of the walk that failed
                String failedElement =
                    tWalkMsg.getPathElements()[rWalkMsg.getNumSuccessfulWalks()];
                this.error("'" + failedElement + "' does not exist.");
            }
        }
        public void error(String message)
        {
            // return the fid to the pool
            TwalkMessage tWalkMsg = (TwalkMessage)this.tMessage;
            conn.returnFid(tWalkMsg.getNewFid());
            this.nestedCallback.error(message);
        }
    }
    
    /**
     * Opens the file on the server, i.e. prepares the file for reading or
     * writing. This blocks until the open is complete. Use of this method will
     * not cause the fileOpen() events of registered change listeners to be fired.
     * @param mode Integer representing the mode - see the constants in StyxUtils.
     * For example, to open a file for reading, use StyxUtils.OREAD. To open a
     * file for writing with truncation use StyxUtils.OWRITE | StyxUtils.OTRUNC.
     */
    public synchronized void open(int mode) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.openAsync(mode, callback);
        RopenMessage rOpenMsg = (RopenMessage)callback.getReply();
        // The properties of the file (mode, iounit, offset) will have been set
        // automatically by the OpenCallback
    }
    
    /**
     * Opens the file on the server. This call does not block; the replyArrived()
     * method of the provided callback object will be called when the Ropen message
     * arrives. The error() method of the provided callback will be called if
     * an error occurred when opening the file.
     * @param mode Integer representing the mode - see the constants in StyxUtils.
     * For example, to open a file for reading, use StyxUtils.OREAD. To open a
     * file for writing with truncation use StyxUtils.OWRITE | StyxUtils.OTRUNC.
     * @param callback The MessageCallback object that will handle the Ropen message
     */
    private void openAsync(int mode, MessageCallback callback)
    {
        if (this.openFid < 0)
        {
            // We need to send a walk message, then when the reply arrives,
            // set the fid and call this function again
            this.getOpenFidAsync(new NestedMessageCallback(callback, new Integer(mode))
            {
                public void replyArrived(StyxMessage message)
                {
                    // Get the mode of the open request
                    int mode = ((Integer)this.attachment).intValue();
                    // The walk must have succeeded so call the open again
                    openAsync(mode, this.nestedCallback);
                }
            });
        }
        else
        {
            TopenMessage tOpenMsg = new TopenMessage(this.openFid, mode);
            this.conn.sendAsync(tOpenMsg, new OpenCallback(callback));
        }
    }
    
    /**
     * Opens the file on the server. This call does not block; the fileOpen()
     * method of any registered change listeners will be called when the Ropen message
     * arrives. The error() method of any registered change listeners will be
     * called if an error occurs opening the file.
     * @param mode Integer representing the mode - see the constants in StyxUtils.
     * For example, to open a file for reading, use StyxUtils.OREAD. To open a
     * file for writing with truncation use StyxUtils.OWRITE | StyxUtils.OTRUNC.
     */
    public void openAsync(int mode)
    {
        this.openAsync(mode, new MessageCallback()
        {
            public void replyArrived(StyxMessage message)
            {
                TopenMessage tOpenMsg = (TopenMessage)this.tMessage;
                fireOpen(tOpenMsg.getMode());
            }
            public void error(String message)
            {
                fireError(message);
            }
        });
    }
    
    private class OpenCallback extends NestedMessageCallback
    {
        public OpenCallback(MessageCallback callback)
        {
            super(callback);
        }
        public void replyArrived(StyxMessage message)
        {
            TopenMessage tOpenMsg = (TopenMessage)this.tMessage;
            RopenMessage rOpenMsg = (RopenMessage)message;
            // Set the properties of this file
            offset = 0;
            mode = tOpenMsg.getMode();
            ioUnit = (int)rOpenMsg.getIoUnit();
            this.nestedCallback.replyArrived(message);
        }
    }
    
    /**
     * Closes the file (i.e. clunks the open fid). If the file isn't open, this
     * will do nothing. This sends the Tclunk message but does not wait for a
     * reply (this doesn't matter because the rules of Styx say that the fid
     * of the file is invalid as soon as the Tclunk is sent, whether the Rclunk
     * arrives or not. The Rclunk message is handled in the StyxConnection class.
     */
    public synchronized void close() throws StyxException
    {
        if (this.mode < 0)
        {
            return;
        }
        long fid = this.openFid;
        // Send the message to close the file (note that this will not wait
        // for a reply). We don't need to set a callback; when the reply arrives,
        // the fid will be returned to the connection's pool in the
        // StyxConnection class.
        this.conn.sendAsync(new TclunkMessage(fid), null);
        // We reset all the properties of this file immediately; we don't need
        // to wait for the rClunk to arrive. This is because the fid will be
        // invalid even if the clunk fails - see clunk(5) in the Inferno manual.
        this.openFid = -1;
        this.ioUnit = 0;
        this.mode = -1;
        this.offset = 0;
    }
    
    /**
     * Reads a chunk of data from the file. Reads the maximum amount of data
     * allowed in a single message, starting at the given file offset. This method
     * does not change the offset of the file; to do this, call CStyxFile.setOffset()
     * when the Rread message arrives.
     * Returns immediately; the replyArrived() method of the given callback
     * will be called when the data arrive, and the error() method of the given
     * callback will be called if an error occurs.
     */
    private void readAsync(long offset, MessageCallback callback)
    {
        if (this.mode < 0)
        {
            // If the file isn't open, we must open it for reading
            this.openAsync(StyxUtils.OREAD, new NestedMessageCallback(callback, new Long(offset))
            {
                public void replyArrived(StyxMessage message)
                {
                    // Retrieve the original offset
                    long offset = ((Long)this.attachment).longValue();
                    readAsync(offset, this.nestedCallback);
                }
            });
        }
        else
        {
            int rwx = this.mode & 3; // mask off last two bits to get OREAD, OWRITE, 
                                     // ORDWR or OEXEC (i.e. ignore OTRUNC/ORCLOSE)
            if (rwx == StyxUtils.OREAD || rwx == StyxUtils.ORDWR)
            {
                // Try to read the maximum number of bytes from the file
                TreadMessage tReadMsg = new TreadMessage(this.openFid, new ULong(offset),
                    this.ioUnit);
                conn.sendAsync(tReadMsg, callback);
            }
            else
            {
                callback.error("File isn't open for reading");
            }
        }
    }
    
    /**
     * Reads a chunk of data from the file. Reads the maximum amount of data
     * allowed in a single message, starting at the given file offset. This method
     * does not change the offset of the file; to do this, call CStyxFile.setOffset()
     * when the Rread message arrives.
     * Returns immediately; the dataArrived() events of registered
     * CStyxFileChangeListeners will be called when the data arrive, and the
     * error() events of registered CStyxFileChangeListeners will be called ifan error occurs.
     * @todo: should we make sure the fileOpen() event is fired if this results
     * in the file opening?
     */
    public synchronized void readAsync(long offset)
    {
        this.readAsync(offset, this);
    }
    
    /**
     * Reads a chunk of data from the current file offset. When the data arrive,
     * the dataArrived() methods of any CStyxFileChangeListeners will be called.
     * @return the tag of the outgoing Tread message. This does not change the 
     * offset of the file; use CStyxFile.setOffset() to do this when the reply
     * arrives
     * @todo: for this method, we could update the offset?
     * @todo: should we make sure the fileOpen() event is fired if this results
     * in the file opening?
     */
    public void readAsync()
    {
        this.readAsync(this.offset);
    }
    
    /**
     * Adds the given value to the current file offset
     */
    public void setOffset(long offset)
    {
        this.offset = offset;
    }
    
    /**
     * Reads a chunk of data from the file. Reads the maximum amount of data
     * allowed in a single message, starting at the given file offset.
     * Updates the current file offset. Blocks until the server replies with the
     * data.
     */
    public synchronized ByteBuffer read(long offset) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.readAsync(offset, callback);
        RreadMessage rReadMsg = (RreadMessage)callback.getReply();
        this.offset += rReadMsg.getCount();
        return rReadMsg.getData();
    }
    
    /**
     * Reads a chunk of data from the file. Reads the maximum amount of data
     * allowed in a single message, starting at the current file offset.
     * Updates the current file offset. Blocks until the server replies with the
     * data.
     */
    public synchronized ByteBuffer read() throws StyxException
    {
        return this.read(this.offset);
    }
    
    /**
     * Reads the entire contents of the file and returns them as a String.
     * This should only be used for relatively short files that can sensibly
     * fit into a String - do not use for large files as not only will this 
     * method block until the file is read, you may run into memory problems.
     */
    public String getContents() throws StyxException
    {
        boolean wasOpen = true;
        if (this.openFid < 1)
        {
            wasOpen = false;
        }
        StringBuffer strBuf = new StringBuffer();
        ByteBuffer buf;
        byte[] arr = null;
        long offset = 0;
        int n = 0;
        do
        {
            buf = this.read(offset);
            n = buf.remaining();
            if (n > 0)
            {
                // TODO: inefficient to allocate new array each time?
                arr = new byte[n];
                offset += n;
                buf.get(arr);
                strBuf.append(StyxUtils.utf8ToString(arr));
            } 
        } while (n > 0);
        if (!wasOpen)
        {
            // If the file wasn't open before we called this function, close it
            this.close();
        }
        return strBuf.toString();        
    }
    
    /**
     * Sets the contents of the file to the given string. Overwrites anything
     * else in the file. Closes the file after use unless the file was open
     * before this method was called.  If the file was open before this message
     * is called, it must be open for writing with truncation
     * (i.e. StyxUtils.OWRITE | StyxUtils.OTRUNC)
     * @param str The new file contents
     * @throws StyxException if there was an error opening or writing to the file
     */
    public synchronized void setContents(String str) throws StyxException
    {
        boolean wasOpen;
        // TODO: this (almost) repeats code in write() - refactor this?
        if (this.mode < 0)
        {
            // If the file isn't open, open it for writing with truncation
            this.open(StyxUtils.OWRITE | StyxUtils.OTRUNC);
            wasOpen = false;
        }
        else
        {
            // The file is open already - check that it's opened in the right mode
            int rwx = this.mode & 3; // mask off last two bits to get OREAD, OWRITE, 
                                     // ORDWR or OEXEC (i.e. ignore OTRUNC/ORCLOSE)
            if (rwx != StyxUtils.OWRITE && rwx != StyxUtils.ORDWR)
            {
                throw new StyxException("File isn't open for writing");
            }
            if ((this.mode & StyxUtils.OTRUNC) != StyxUtils.OTRUNC)
            {
                throw new StyxException("File must be open for writing with truncation");
            }
            wasOpen = true;
        }
        
        byte[] bytes = StyxUtils.strToUTF8(str);
        // the writeAll() method will automatically take care of splitting the
        // input across multiple Styx messages if necessary
        this.writeAll(ByteBuffer.wrap(bytes), 0);
        // If this file wasn't open before we called this function, close it
        if (!wasOpen)
        {
            this.close();
        }
    }
    
    /**
     * Writes a block of data to the file at the given offset. Will write the data
     * in several separate messages if necessary. This method leaves the position
     * and limit of the buffer unchanged.
     * @throws StyxException if there is an error writing to the file
     */
    public synchronized void writeAll(ByteBuffer buf, long offset) throws StyxException
    {
        // Store the original position of the buffer
        int origPos = buf.position();
        
        do
        {
            int bytesToWrite;
            if (buf.remaining() <= this.ioUnit)
            {
                // This will complete the write operation
                bytesToWrite = buf.remaining();
            }
            else
            {
                // We can only write a portion of the data
                bytesToWrite = this.ioUnit;
            }
            int bytesWritten = (int)this.write(buf);
            
            // Set the new position of the buffer
            buf.position(buf.position() + bytesWritten);
            
        } while (buf.hasRemaining());        
        
        // Restore the original position of the buffer
        buf.position(origPos);
    }
    
    /**
     * Writes a block of data to the file at the given offset. Cannot write more
     * than this.getIOUnit() bytes in a single message. Blocks until the write
     * confirmation arrives. Does not change the offset of the file
     * @param buf The data to write.
     * @param offset The position in the file at which to write the data
     * @param count The number of bytes to write
     * @return The number of bytes written to the file
     * @throws StyxException if there is an error writing to the file
     */
    public long write(ByteBuffer buf, long offset, long count) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.writeAsync(buf, offset, count, callback);
        RwriteMessage rWriteMsg = (RwriteMessage)callback.getReply();
        return rWriteMsg.getNumBytesWritten();
    }
    
    /**
     * Writes a block of data to the file at the given offset. Cannot write more
     * than this.getIOUnit() bytes in a single message. Blocks until the write
     * confirmation arrives. Does not change the offset of the file.
     * @param buf The data to write. Will attempt to write all the remaining
     * data in the buffer.
     * @param offset The position in the file at which to write the data
     * @return The number of bytes written to the file
     * @throws StyxException if there is an error writing to the file
     */
    public long write(ByteBuffer buf, long offset) throws StyxException
    {
        return this.write(buf, offset, buf.remaining());
    }
    
    /**
     * Writes all the data in the given buffer to the file at the current
     * file position, updating the file position if successful. Blocks until
     * the operation is complete.
     * @return the number of bytes that were written to the file
     */
    public synchronized long write(ByteBuffer buf) throws StyxException
    {
        long bytesWritten = this.write(buf, this.offset, buf.remaining());
        // If we've got this far the write must have been successful
        this.offset += bytesWritten;
        return bytesWritten;
    }
    
    /**
     * Writes a chunk of data to the file at the given file offset. This method
     * will throw a StyxException if we try to write more data than
     * can fit in a single StyxMessage (i.e. more than this.getIOUnit()). This method
     * does not alter the offset of the file; to do this, call CStyxFile.addOffset()
     * when the Rwrite message arrives.
     * Returns immediately; the callback's replyArrived() method will be called
     * when the reply arrives and the callback's error() method will be called
     * if an error occurs
     * @param buf The buffer containing the data to write.
     * @param offset The position in the file at which the data will be written
     * @param count The number of bytes to write
     * @param callback The replyArrived() method of this callback object will be
     * called when the write confirmation arrives
     */
    private synchronized void writeAsync(ByteBuffer buf, long offset, long count,
        MessageCallback callback)
    {
        if (this.mode < 0)
        {
            // If the file isn't open, open it for writing
            // If the file isn't open, we must open it for reading
            this.openAsync(StyxUtils.OWRITE,
                new NestedMessageCallback(callback, new WriteContents(buf, offset, count))
            {
                public void replyArrived(StyxMessage message)
                {
                    // Retrieve the original parameters of the write message
                    WriteContents contents = (WriteContents)this.attachment;
                    writeAsync(contents.buf, contents.offset, contents.count, this.nestedCallback);
                }
            });
        }
        else
        {
            // Check that the mode is correct
            int rwx = this.mode & 3; // mask off last two bits to get OREAD, OWRITE, 
                                     // ORDWR or OEXEC (i.e. ignore OTRUNC/ORCLOSE)
            if (rwx != StyxUtils.OWRITE && rwx != StyxUtils.ORDWR)
            {
                callback.error("File isn't open for writing");
            }
            else if (count > this.ioUnit)
            {
                callback.error("Cannot write more than " + this.ioUnit +
                    " bytes in a single message");
            }
            else
            {
                TwriteMessage tWriteMsg =
                    new TwriteMessage(this.openFid, new ULong(offset), count, buf);
                conn.sendAsync(tWriteMsg, callback);
            }
        }
    }
    
    /**
     * Writes a chunk of data to the file at the given file offset. This method
     * will throw a StyxException if we try to write more data than
     * can fit in a single StyxMessage (i.e. more than this.getIOUnit()). This method
     * does not alter the offset of the file; to do this, call CStyxFile.addOffset()
     * when the Rwrite message arrives.
     * Returns immediately; the dataSent() method of any waiting change listeners
     * will be called when the write confirmation arrives, and the error() method
     * of any waiting change listeners will be called if an error occurs.
     * @param buf The buffer containing the data to write. This will attempt to
     * write all the data in the buffer
     * @param offset The position in the file at which the data will be written
     */
    public synchronized void writeAsync(ByteBuffer data, long offset)
    {
        this.writeAsync(data, offset, data.remaining(), this);
    }
    
    /**
     * Writes a chunk of data to the file at the current offset. When the write
     * confirmation arrives, the dataSent() method of any registered
     * CStyxFileChangeListeners will be called. Will attempt to write all the
     * remaining data in the buffer.
     */
    public synchronized void writeAsync(ByteBuffer data)
    {
        this.writeAsync(data, this.offset);
    }
    
    /**
     * Writes a string to the file at the current offset. When the write
     * confirmation arrives, the dataSent() method of any registered
     * CStyxFileChangeListeners will be called. Will attempt to write all the
     * remaining data in the buffer.
     */
    public synchronized void writeAsync(String str)
    {
        this.writeAsync(ByteBuffer.wrap(StyxUtils.strToUTF8(str)), this.offset);
    }
    
    /**
     * Simple struct to hold the contents of a pending Write message
     */
    private class WriteContents
    {
        private ByteBuffer buf;
        private long offset;
        private long count;
        public WriteContents(ByteBuffer buf, long offset, long count)
        {
            this.buf = buf;
            this.offset = offset;
            this.count = count;
        }
    }
    
    /**
     * Refreshes the stat (DirEntry) of the file. Blocks until the operation
     * is complete. Does not fire the statChanged() event on any registered
     * change listeners.
     */
    public synchronized void refresh() throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.refreshAsync(callback);
        RstatMessage rStatMsg = (RstatMessage)callback.getReply();
    }
    
    /**
     * Refreshes the status of the file by sending a TStatMessage. Does not wait
     * for a reply; when the reply arrives, the dirEntry of this file will be 
     * set and the replyArrived() method of the provided callback object
     * will be called.
     * @todo In the case of a directory, should this refresh the list of children?
     * @todo If there is already a Tstat message in flight, what should we do?
     * Does it matter much?
     */
    private void refreshAsync(MessageCallback callback)
    {
        if (this.fid < 0)
        {
            // Send a walk message and when the reply arrives, set the fid of this
            // file and call this method again
            this.getFidAsync
            (
                new NestedMessageCallback(callback)
                {
                    public void replyArrived(StyxMessage message)
                    {
                        refreshAsync(this.nestedCallback);
                    }
                }
            );
        }
        else
        {
            // We've already set the fid of this file, so just send the Tstat message
            this.conn.sendAsync(new TstatMessage(this.fid), new RefreshCallback(callback));
        }
    }
    
    /**
     * Refreshes the status of the file by sending a TStatMessage. Does not wait
     * for a reply; when the reply arrives, the dirEntry of this file will be 
     * set and the statChanged() event of any registered change listeners will be
     * fired.
     * @todo In the case of a directory, should this refresh the list of children?
     */
    public void refreshAsync()
    {
        this.refreshAsync(new MessageCallback()
        {
            public void replyArrived(StyxMessage message)
            {
                // The dirEntry will have been set by the RefreshCallback
                RstatMessage rStatMsg = (RstatMessage)message;
                fireStatChanged(rStatMsg);
            }
            public void error(String message)
            {
                fireError(message);
            }
        });
    }
    
    private class RefreshCallback extends NestedMessageCallback
    {
        public RefreshCallback(MessageCallback callback)
        {
            super(callback);
        }
        public void replyArrived(StyxMessage message)
        {
            RstatMessage rStatMsg = (RstatMessage)message;
            dirEntry = rStatMsg.getDirEntry();
            this.nestedCallback.replyArrived(message);
        }
    }
    
    /**
     * Gets a file on the same connection as this CStyxFile, without opening it.
     * @param path The path of the file to be opened, <i>relative to this file</i>.
     */
    public CStyxFile getFile(String path)
    {
        return new CStyxFile(this.conn, this.getPath() + "/" + path);
    }
    
    /**
     * Gets a file on the same connection as this CStyxFile, then opens it. This
     * blocks until the file is opened.
     * @param path The path of the file to be opened, <i>relative to this file</i>.
     * @param mode The mode with which to open the file
     * @throws StyxException if the file could not be opened with the given mode
     */
    public CStyxFile openFile(String path, int mode) throws StyxException
    {
        CStyxFile sf = this.getFile(path);
        sf.open(mode);
        return sf;
    }
    
    /**
     * Gets all the children of this directory. If this is not a directory,
     * this will return null. (For an empty directory, this will return a zero-
     * length array)
     */
    public CStyxFile[] getChildren() throws StyxException
    {
        if (!this.isDirectory())
        {
            return null;
        }
        Vector dirEntries = new Vector();
        // Read the contents of the directory
        this.open(StyxUtils.OREAD);
        boolean done = false;
        do
        {
            ByteBuffer data = this.read();
            if (data.remaining() > 0)
            {
                // Wrap data as a StyxBuffer
                StyxBuffer styxBuf = new StyxBuffer(data);
                // Get all the DirEntries from this buffer
                while(data.hasRemaining())
                {
                    // TODO: how handle buffer underflows?
                    DirEntry dirEntry = styxBuf.getDirEntry();
                    dirEntries.add(new CStyxFile(this.conn, this.path, dirEntry));
                }
            }
            else
            {
                // We've read everything from the directory
                done = true;
            }
        } while (!done);
        
        // Now we can close the directory
        this.close();
        
        return (CStyxFile[])dirEntries.toArray(new CStyxFile[0]);
    }
    
    /**
     * Called when a Rread or Rwrite message arrives. We do this in this class
     * to save creating a new callback class for every read or write to a file.
     */
    public void replyArrived(StyxMessage message)
    {
        if (message instanceof RreadMessage)
        {
            RreadMessage rReadMsg = (RreadMessage)message;
            TreadMessage tReadMsg = (TreadMessage)this.tMessage;
            this.fireDataArrived(tReadMsg, rReadMsg);
        }
        else if (message instanceof RwriteMessage)
        {
            RwriteMessage rWriteMsg = (RwriteMessage)message;
            TwriteMessage tWriteMsg = (TwriteMessage)this.tMessage;
            // Check the number of bytes written
            if (rWriteMsg.getNumBytesWritten() != tWriteMsg.getCount())
            {
                this.error("Mismatch: attempted to write " + tWriteMsg.getCount()
                    + " bytes; actually wrote " + rWriteMsg.getNumBytesWritten());
            }
            else
            {
                this.fireDataSent(tWriteMsg);
            }
        }
        else
        {
            // TODO: do something more useful here?
            System.err.println("Internal error: got message that isn't a Rread or Rwrite");
        }
    }
    
    /**
     * Called if an error occurs when reading or writing to a file (part of 
     * MessageCallback contract)
     */
    public void error(String message)
    {
        this.fireError(message);
    }
    
    /**
     * Adds a StyxFileChangeListener to this file. The methods of the change
     * listener will be called when events happen, such as new data arriving
     * or a change in permissions, etc (note that this notification will only
     * happen if the changes are made by calling methods of this CStyxFile
     * instance.  If another client makes changes, we will not be notified here).
     * If this listener is already registered, this method does nothing.
     */
    public void addChangeListener(CStyxFileChangeListener listener)
    {
        synchronized(this.listeners)
        {
            if (!this.listeners.contains(listener))
            {
                this.listeners.add(listener);
            }
        }
    }
    
    /**
     * Removes the given change listener
     */
    public void removeChangeListener(CStyxFileChangeListener listener)
    {
        this.listeners.remove(listener);
    }
    
    /**
     * Fires the error() method on all registered listeners
     */
    private void fireError(String message)
    {
        synchronized(listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)listeners.get(i);
                listener.error(this, message);
            }
        }
    }
    
    /**
     * Fires the fileOpen() method on all registered listeners
     */
    private void fireOpen(int mode)
    {
        synchronized(listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)listeners.get(i);
                listener.fileOpen(this, mode);
            }
        }
    }
    
    /**
     * Fires the dataArrived() method on all registered listeners
     */
    private void fireDataArrived(TreadMessage tReadMsg, RreadMessage rReadMsg)
    {
        synchronized(listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)listeners.get(i);
                listener.dataArrived(this, tReadMsg, rReadMsg.getData());
            }
        }
    }
    
    /**
     * Fires the statChanged() method on all registered listeners
     */
    private void fireStatChanged(RstatMessage rStatMsg)
    {
        dirEntry = rStatMsg.getDirEntry();
        // Notify all the listeners that the stat may have changed
        synchronized(listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)listeners.get(i);
                listener.statChanged(this, dirEntry);
            }
        }
    }
    
    /**
     * Fires the dataSent() method on all registered listeners
     */
    private void fireDataSent(TwriteMessage tWriteMsg)
    {
        // Notify all listeners that the data have been written
        synchronized(listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)listeners.get(i);
                listener.dataSent(this, tWriteMsg);
            }
        }
    }
}
