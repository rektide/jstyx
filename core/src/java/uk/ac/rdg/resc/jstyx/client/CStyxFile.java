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

import java.util.Vector;
import java.util.Iterator;
import java.util.Date;

import org.apache.log4j.Logger;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;
import uk.ac.rdg.resc.jstyx.messages.*;

/**
 * A Styx file (or directory) from the point of view of the client. (It is called
 * a CStyxFile in order to avoid confusion with the server-side StyxFile class.)
 * @todo should we keep a cache of all the children of this file?
 * @todo implement a create() method
 * @todo implement changing of stat data (length etc) via a Twstat message
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.14  2005/05/12 07:40:52  jonblower
 * CStyxFile.close() no longer throws a StyxException
 *
 * Revision 1.13  2005/05/11 18:25:35  jonblower
 * Added temporary debug code
 *
 * Revision 1.12  2005/05/05 07:08:37  jonblower
 * Improved handling of buffers in change listeners
 *
 * Revision 1.11  2005/03/19 21:46:58  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.10  2005/03/18 13:55:59  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.9  2005/03/16 17:55:52  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.8  2005/03/11 13:58:25  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.7.2.3  2005/03/11 12:30:29  jonblower
 * Changed so that message payloads are always ints, not longs
 *
 * Revision 1.7.2.2  2005/03/10 20:54:57  jonblower
 * Removed references to Netty
 *
 * Revision 1.7.2.1  2005/03/10 11:49:17  jonblower
 * Removed unneeded reference to StyxBuffer
 *
 * Revision 1.7  2005/03/07 08:29:18  jonblower
 * Minor changes (better handling of path building)
 *
 * Revision 1.6  2005/02/28 11:43:36  jonblower
 * Tidied up logging code
 *
 * Revision 1.5  2005/02/21 18:06:13  jonblower
 * Un-synchronized many methods, made more robust in case of multiple calls to asynchronous methods
 *
 * Revision 1.4  2005/02/18 17:57:31  jonblower
 * Changed a constructor to private access
 *
 * Revision 1.3  2005/02/18 09:11:35  jonblower
 * Remove 'synchronized' from some methods, added some comments
 *
 * Revision 1.2  2005/02/17 18:03:35  jonblower
 * Minor changes to comments
 *
 * Revision 1.1.1.1  2005/02/16 18:58:17  jonblower
 * Initial import
 *
 */
public class CStyxFile extends MessageCallback
{
    
    private static final Logger log = Logger.getLogger(CStyxFile.class);
    
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
    
    private boolean gettingFid;      // True when we are in the process of
                                     // getting a fid for this file
    private boolean gettingOpenFid;  // True when we are in the process of
                                     // getting a fid to open for this file
    private boolean openingFile;     // True when we are in the process of 
                                     // opening a file
    private Vector fidListeners;     // Vector of MessageCallbacks that are
                                     // waiting for fids to arrive
    private Vector openFidListeners; // Vector of MessageCallbacks that are
                                     // waiting for fids to arrive
    
    
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
        
        this.gettingFid = false;
        this.gettingOpenFid = false;
        this.openingFile = false;
        this.fidListeners = new Vector();
        this.openFidListeners = new Vector();
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
     * Creates a new instance of CStyxFile. Used by this.getChildren()
     */
    private CStyxFile(StyxConnection conn, String basePath, DirEntry dirEntry)
    {
        this(conn, basePath + (basePath.endsWith("/") ? "" : "/") + dirEntry.getFileName());
        this.dirEntry = dirEntry;
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
     * @throws IllegalStateException if a fid already exists for this file 
     * (should not happen in practice)
     */
    private void getFidAsync(MessageCallback callback)
    {
        synchronized(this.fidListeners)
        {
            if (this.fid >= 0)
            {
                throw new IllegalStateException("This file already has a fid");
            }
            // Make sure the callback is called when the walk reply arrives
            this.fidListeners.add(callback);
            // TODO: is this thread-safe?
            if (!this.gettingFid)
            {
                // If we're not already in the process of getting a fid,
                // send the Twalk message
                this.gettingFid = true;
                long newFid = this.conn.getFreeFid();
                TwalkMessage tWalkMsg = new TwalkMessage(this.conn.getRootFid(),
                    newFid, this.path);
                this.conn.sendAsync(tWalkMsg, new WalkCallback());
            }
        }
    }
    
    /**
     * Gets a fid for this file that we can open (equivalent to cloning a fid
     * in older versions of Styx). Does not block.
     * @throws IllegalStateException if we already have a fid that we can open
     * for this file
     */
    private void getOpenFidAsync(MessageCallback callback)
    {
        synchronized(this.openFidListeners)
        {
            if (this.openFid >= 0)
            {
                throw new IllegalStateException("This file already has a fid" +
                    " that can be opened.");
            }
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
                // Make sure the callback is called when the walk reply arrives
                this.openFidListeners.add(callback);
                if (!this.gettingOpenFid)
                {
                    // If we're not already in the process of getting a fid to open,
                    // send the Twalk message
                    this.gettingOpenFid = true;
                    long newFid = this.conn.getFreeFid();
                    TwalkMessage tWalkMsg = new TwalkMessage(this.fid, newFid, "");
                    this.conn.sendAsync(tWalkMsg, new WalkCallback());
                }
            }
        }
    }
    
    private class WalkCallback extends MessageCallback
    {
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
                    synchronized(fidListeners)
                    {
                        fid = tWalkMsg.getNewFid();
                        gettingFid = false;
                        // Notify any registered listeners for this fid
                        notifyReplyArrived(fidListeners, message);
                    }
                }
                else if (openFid < 0)
                {
                    // This must have been the walk to establish a fid for this
                    // file that we can open (but can't be walked)
                    synchronized(openFidListeners)
                    {
                        openFid = tWalkMsg.getNewFid();
                        gettingOpenFid = false;
                        notifyReplyArrived(openFidListeners, message);
                    }
                }
                else
                {
                    this.error("Internal error: both fid and openFid are set",
                        tWalkMsg.getTag());
                }
            }
            else
            {
                // Get the element of the walk that failed
                String errMsg = "'" + 
                    tWalkMsg.getPathElements()[rWalkMsg.getNumSuccessfulWalks()]
                    + "' does not exist.";
                this.error(errMsg, tWalkMsg.getTag());
            }
        }
        public void error(String message, int tag)
        {
            // return the fid to the pool
            TwalkMessage tWalkMsg = (TwalkMessage)this.tMessage;
            conn.returnFid(tWalkMsg.getNewFid());
            // Notify any listeners of the error
            if (fid < 0)
            {
                gettingFid = false;
                notifyError(fidListeners, message, tWalkMsg.getTag());
            }
            else if (openFid < 0)
            {
                gettingOpenFid = false;
                notifyError(openFidListeners, message, tWalkMsg.getTag());
            }
            else
            {
                // Shouldn't happen
                //log.error("Illegal state: got Rwalk when both fid" +
                //    " and openFid are set");
            }
        }
    }
    
    /**
     * Notifies all the MessageCallbacks in the given Vector of the arrival 
     * of a message
     */
    private void notifyReplyArrived(Vector listeners, StyxMessage message)
    {
        synchronized(listeners)
        {
            Iterator it = listeners.iterator();
            while (it.hasNext())
            {
                MessageCallback cb = (MessageCallback)it.next();
                cb.replyArrived(message);
                it.remove();
            }
        }
    }
    
    /**
     * Notifies all the MessageCallbacks in the given Vector of an error
     */
    private void notifyError(Vector listeners, String message, int tag)
    {
        synchronized(listeners)
        {
            Iterator it = listeners.iterator();
            while (it.hasNext())
            {
                MessageCallback cb = (MessageCallback)it.next();
                cb.error(message, tag);
                it.remove();
            }
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
    public void open(int mode) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.openAsync(mode, callback);
        // The properties of the file (mode, iounit, offset) will have been set
        // automatically by the OpenCallback so we don't need to read the Ropen
        // message.
        callback.getReply();
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
     * @throws IllegalStateException if the file is already open or it is being
     * opened
     */
    private synchronized void openAsync(int mode, MessageCallback callback)
    {
        if (this.openingFile)
        {
            throw new IllegalStateException("This file is already being opened");
        }
        if (this.mode >= 0)
        {
            // TODO Should we still throw this exception if the file is open with
            // the same mode as the mode that's requested?
            throw new IllegalStateException("File " + this.getPath() + 
                " is already open; close the file before re-opening it");
        }
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
            this.openingFile = true;
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
     * @throws IllegalStateException if the file is already open
     */
    public synchronized void openAsync(int mode)
    {
        this.openAsync(mode, new MessageCallback()
        {
            public void replyArrived(StyxMessage message)
            {
                TopenMessage tOpenMsg = (TopenMessage)this.tMessage;
                fireOpen(tOpenMsg.getMode());
            }
            public void error(String message, int tag)
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
            openingFile = false;
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
    public synchronized void close()
    {
        if (this.mode < 0)
        {
            // The file isn't open; just return
            return;
        }
        long fid = this.openFid;
        // Send the message to close the file (note that this will not wait
        // for a reply). We don't need to set a callback; when the reply arrives,
        // the fid will be returned to the connection's pool by the
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
                // No message has been sent, so the tag is -1
                callback.error("File isn't open for reading", -1);
            }
        }
    }
    
    /**
     * Reads a chunk of data from the file. Reads the maximum amount of data
     * allowed in a single message, starting at the given file offset. This method
     * does not change the offset of the file; to do this, call CStyxFile.setOffset()
     * when the Rread message arrives. If the file was not open before this
     * method is called, the file will be opened with mode StyxUtils.OREAD (but
     * the fileOpen() event will not be fired).
     * Returns immediately; the dataArrived() events of registered
     * CStyxFileChangeListeners will be called when the data arrive, and the
     * error() events of registered CStyxFileChangeListeners will be called if
     * an error occurs.
     */
    public void readAsync(long offset)
    {
        this.readAsync(offset, this);
    }
    
    /**
     * Reads a chunk of data from the current file offset. When the data arrive,
     * the dataArrived() methods of any CStyxFileChangeListeners will be called.
     * @return the tag of the outgoing Tread message. This does not change the 
     * offset of the file; use CStyxFile.setOffset() to do this when the reply
     * arrives. If the file was not open before this
     * method is called, the file will be opened with mode StyxUtils.OREAD (but
     * the fileOpen() event will not be fired).
     * @todo: for this method, we could update the offset?
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
     * Does not update the current file offset. Blocks until the server replies
     * with the data.
     *
     * When you have finished with the data in the ByteBuffer that is returned,
     * call release() on the buffer to ensure that the buffer can be re-used.
     */
    public ByteBuffer read(long offset) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.readAsync(offset, callback);
        RreadMessage rReadMsg = (RreadMessage)callback.getReply();
        return rReadMsg.getData();
    }
    
    /**
     * Reads a chunk of data from the file. Reads the maximum amount of data
     * allowed in a single message (= this.getIOUnit()), starting at the current file offset.
     * Updates the current file offset. Blocks until the server replies with the
     * data. This is synchronized because it changes the state of the CStyxFile
     * (i.e. the file offset)
     *
     * When you have finished with the data in the ByteBuffer that is returned,
     * call release() on the buffer to ensure that the buffer can be re-used.
     */
    public synchronized ByteBuffer read() throws StyxException
    {
        ByteBuffer buf = this.read(this.offset);
        this.offset += buf.remaining();
        return buf;
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
        long pos = 0;
        int n = 0;
        do
        {
            // Read the data from the file
            buf = this.read(pos);
            // Convert the data to a string and append to the buffer
            strBuf.append(StyxUtils.dataToString(buf));
            // Release the buffer back to the pool
            buf.release();
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
    public void setContents(String str) throws StyxException
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
        this.writeAll(bytes, 0);
        // If this file wasn't open before we called this function, close it
        if (!wasOpen)
        {
            this.close();
        }
    }
    
    /**
     * Writes a block of data to the file at the given offset. Will write the data
     * in several separate messages if necessary.
     * @throws StyxException if there is an error writing to the file
     */
    public void writeAll(byte[] bytes, long offset) throws StyxException
    {
        // Store the original position of the buffer
        long filePos = offset;
        // The position in the byte array of the first byte to write
        int pos = 0;
        do
        {
            // Calculate the number of bytes still to be written
            int bytesRemaining = bytes.length - pos;
            
            // Calculate the number of bytes that we can write in a single message
            int bytesToWrite = (bytesRemaining < this.ioUnit) ? bytesRemaining : this.ioUnit;
            
            // Write the bytes to the file
            int bytesWritten = (int)this.write(bytes, pos, bytesToWrite, filePos);
            
            // Update the pointers
            pos += bytesWritten;
            filePos += bytesWritten;
            
        } while (pos < bytes.length);
    }
    
    /**
     * Writes a block of data to the file at the given offset. Cannot write more
     * than this.getIOUnit() bytes in a single message. Blocks until the write
     * confirmation arrives. Does not change the offset of the file
     * @param bytes The data to write.
     * @param pos The index of the first data point in the byte array to write
     * @param count The number of bytes from the input array to write
     * @param offset The position in the file at which to write the data
     * @return The number of bytes written to the file
     * @throws StyxException if there is an error writing to the file
     */
    public long write(byte[] bytes, int pos, int count, long offset) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.writeAsync(bytes, pos, count, offset, callback);
        RwriteMessage rWriteMsg = (RwriteMessage)callback.getReply();
        return rWriteMsg.getNumBytesWritten();
    }
    
    /**
     * Writes a block of data to the file at the given offset. Cannot write more
     * than this.getIOUnit() bytes in a single message. Blocks until the write
     * confirmation arrives. Does not change the offset of the file
     * @param bytes The data to write. Will attempt to write all the data in this array.
     * @param offset The position in the file at which to write the data
     * @return The number of bytes written to the file
     * @throws StyxException if there is an error writing to the file
     */
    public long write(byte[] bytes, long offset) throws StyxException
    {
        return this.write(bytes, 0, bytes.length, offset);
    }
    
    /**
     * Writes all the data in the given array to the file at the current
     * file position, updating the file position if successful. Blocks until
     * the operation is complete. This method is synchronized as it affects
     * the state of this CStyxFile (i.e. it updates the offset). Therefore users
     * of this method must beware of deadlock conditions.
     * @return the number of bytes that were written to the file
     */
    public synchronized long write(byte[] bytes) throws StyxException
    {
        long bytesWritten = this.write(bytes, this.offset);
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
     * @param bytes The array of bytes to write
     * @param pos The position in the array of the first byte to write
     * @param count The number of bytes from the byte array to write
     * @param offset The position in the file at which the data will be written
     * @param callback The replyArrived() method of this callback object will be
     * called when the write confirmation arrives
     */
    private void writeAsync(byte[] bytes, int pos, int count, long offset,
        MessageCallback callback)
    {
        if (this.mode < 0)
        {
            // If the file isn't open, open it for writing
            // If the file isn't open, we must open it for reading
            this.openAsync(StyxUtils.OWRITE,
                new NestedMessageCallback(callback, new WriteContents(bytes, pos, count, offset))
            {
                public void replyArrived(StyxMessage message)
                {
                    // Retrieve the original parameters of the write message
                    WriteContents contents = (WriteContents)this.attachment;
                    writeAsync(contents.bytes, contents.pos, contents.count, 
                        contents.offset, this.nestedCallback);
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
                callback.error("File isn't open for writing", -1);
            }
            else if (bytes.length > this.ioUnit)
            {
                callback.error("Cannot write more than " + this.ioUnit +
                    " bytes in a single message", -1);
            }
            else
            {
                TwriteMessage tWriteMsg = new TwriteMessage(this.openFid,
                    new ULong(offset), bytes, pos, count);
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
     * Returns immediately; the callback's replyArrived() method will be called
     * when the reply arrives and the callback's error() method will be called
     * if an error occurs
     * @param bytes The array of bytes to write. All the bytes in the array will
     * be written
     * @param offset The position in the file at which the data will be written
     * @param callback The replyArrived() method of this callback object will be
     * called when the write confirmation arrives
     */
    private void writeAsync(byte[] bytes, long offset, MessageCallback callback)
    {
        this.writeAsync(bytes, 0, bytes.length, offset, callback);
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
     * @param bytes The byte array containing the data to write. This will attempt to
     * write all the data in the array
     * @param offset The position in the file at which the data will be written
     */
    public void writeAsync(byte[] bytes, long offset)
    {
        this.writeAsync(bytes, offset, this);
    }
    
    /**
     * Writes a chunk of data to the file at the current offset. When the write
     * confirmation arrives, the dataSent() method of any registered
     * CStyxFileChangeListeners will be called. Will attempt to write all the
     * remaining data in the buffer.
     */
    public void writeAsync(byte[] bytes)
    {
        this.writeAsync(bytes, this.offset);
    }
    
    /**
     * Writes a string to the file at the current offset. Does not update the
     * current file offset. When the write
     * confirmation arrives, the dataSent() method of any registered
     * CStyxFileChangeListeners will be called.
     */
    public void writeAsync(String str)
    {
        this.writeAsync(StyxUtils.strToUTF8(str), this.offset);
    }
    
    /**
     * Simple struct to hold the contents of a pending Write message
     */
    private class WriteContents
    {
        private byte[] bytes;
        private int pos;
        private int count;
        private long offset;
        public WriteContents(byte[] bytes, int pos, int count, long offset)
        {
            this.bytes = bytes;
            this.pos = pos;
            this.count = count;
            this.offset = offset;
        }
    }
    
    /**
     * Refreshes the stat (DirEntry) of the file. Blocks until the operation
     * is complete. Does not fire the statChanged() event on any registered
     * change listeners.
     */
    public void refresh() throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.refreshAsync(callback);
        RstatMessage rStatMsg = (RstatMessage)callback.getReply();
        // the dirEntry will already have been set by this stage
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
            public void error(String message, int tag)
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
        StringBuffer fullPath = new StringBuffer(this.getPath());
        if (!this.getPath().endsWith("/"))
        {
            fullPath.append("/");
        }
        fullPath.append(path);
        return new CStyxFile(this.conn, fullPath.toString());
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
            // We don't need the contents of the buffer anymore.
            data.release();
        } while (!done);
        
        // Now we can close the directory
        this.close();
        
        return (CStyxFile[])dirEntries.toArray(new CStyxFile[0]);
    }
    
    /**
     * Reads this directory to get all its children. When the process is completed
     * the childrenFound() event is fired on all registered listeners
     */
    public void getChildrenAsync()
    {
        // TODO: first check that this is a directory
        this.readAsync(offset, new MessageCallback()
        {
            private Vector dirEntries = new Vector();
            private long offset = 0;
            public void replyArrived(StyxMessage message)
            {
                RreadMessage rReadMsg = (RreadMessage)message;
                ByteBuffer data = rReadMsg.getData();
                this.offset += data.remaining();
                if (data.remaining() > 0)
                {
                    // Wrap data as a StyxBuffer
                    StyxBuffer styxBuf = new StyxBuffer(data);
                    // Get all the DirEntries from this buffer
                    while(data.hasRemaining())
                    {
                        // TODO: how handle buffer underflows?
                        DirEntry dirEntry = styxBuf.getDirEntry();
                        this.dirEntries.add(new CStyxFile(conn, path, dirEntry));
                    }
                    data.release();
                    // Read from this file again
                    readAsync(this.offset, this);
                }
                else
                {
                    // We've read all the data from the file
                    close();
                    fireChildrenFound((CStyxFile[])this.dirEntries.toArray(new CStyxFile[0]));
                }
            }
            public void error(String message, int tag)
            {
                // TODO
            }
        });
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
            // Release the data buffer; if any registered change listeners want
            // to keep the data, they should call data.acquire() to delay the
            // returning of the ByteBuffer to the pool.
            rReadMsg.getData().release();
        }
        else if (message instanceof RwriteMessage)
        {
            RwriteMessage rWriteMsg = (RwriteMessage)message;
            TwriteMessage tWriteMsg = (TwriteMessage)this.tMessage;
            // Check the number of bytes written
            if (rWriteMsg.getNumBytesWritten() != tWriteMsg.getCount())
            {
                this.error("Mismatch: attempted to write " + tWriteMsg.getCount()
                    + " bytes; actually wrote " + rWriteMsg.getNumBytesWritten(),
                    tWriteMsg.getTag());
            }
            else
            {
                this.fireDataSent(tWriteMsg);
            }
        }
        else
        {
            // TODO: do something more useful here?
            log.error("Internal error: got message that isn't a Rread or Rwrite");
        }
    }
    
    /**
     * Called if an error occurs when reading or writing to a file (part of 
     * MessageCallback contract)
     */
    public void error(String message, int tag)
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
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.error(this, message);
            }
        }
    }
    
    /**
     * Fires the fileOpen() method on all registered listeners
     */
    private void fireOpen(int mode)
    {
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.fileOpen(this, mode);
            }
        }
    }
    
    /**
     * Fires the dataArrived() method on all registered listeners
     */
    private void fireDataArrived(TreadMessage tReadMsg, RreadMessage rReadMsg)
    {
        synchronized(this.listeners)
        {
            ByteBuffer data = rReadMsg.getData();
            // Remember the position and limit of the buffer
            int pos = data.position();
            int limit = data.limit();
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.dataArrived(this, tReadMsg, data);
                // Reset the position and limit of the buffer, ready for the next
                // listener
                data.position(pos);
                data.limit(limit);
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
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
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
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.dataSent(this, tWriteMsg);
            }
        }
    }
    
    /**
     * Fires the childrenFound() method on all registered listeners
     */
    private void fireChildrenFound(CStyxFile[] children)
    {
        System.err.println("Found " + children.length + " children:");
        for (int i = 0; i < children.length; i++)
        {
            System.err.println("   " + children[i].getPath());
        }
    }
}
