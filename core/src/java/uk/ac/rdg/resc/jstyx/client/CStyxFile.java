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

import java.io.File;
import java.io.InputStream;

import org.apache.log4j.Logger;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.types.Qid;

import uk.ac.rdg.resc.jstyx.messages.*;
import uk.ac.rdg.resc.jstyx.client.callbacks.*;

/**
 * A Styx file (or directory) from the point of view of the client. (It is called
 * a CStyxFile in order to avoid confusion with the server-side StyxFile class.)
 * To create a CStyxFile, open a StyxConnection and use the getFile() method.
 * CStyxFiles cannot be created directly.
 * @todo implement a create() method that automatically creates all necessary
 * subdirectories
 * @todo implement changing of stat data (length etc) via a Twstat message
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.35  2005/08/08 09:36:19  jonblower
 * Minor changes
 *
 * Revision 1.34  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 * Revision 1.33  2005/08/04 16:48:57  jonblower
 * Added and edited upload() methods in CStyxFile
 *
 * Revision 1.32  2005/07/28 16:37:35  jonblower
 * Added isSameFile() method
 *
 * Revision 1.31  2005/07/06 17:46:19  jonblower
 * Added setDirEntry()
 *
 * Revision 1.30  2005/06/27 17:18:03  jonblower
 * Added accelerated downloading methods, and responded to changes with MessageCallback
 *
 * Revision 1.29  2005/06/22 17:08:34  jonblower
 * Changed to allow target file to be null in download()
 *
 * Revision 1.28  2005/06/20 17:20:34  jonblower
 * Added download() and downloadAsync() to CStyxFile
 *
 * Revision 1.27  2005/06/14 07:45:16  jonblower
 * Implemented setting of params and async notification of parameter changes
 *
 * Revision 1.26  2005/05/26 16:48:48  jonblower
 * Fixed bug in uploadFileAsync()
 *
 * Revision 1.24  2005/05/25 16:58:01  jonblower
 * Changed implementation of openOrCreate()
 *
 * Revision 1.23  2005/05/25 15:36:55  jonblower
 * No longer requires open fid, implemented createAsync() and openOrCreateAsync() methods
 *
 * Revision 1.22  2005/05/24 12:55:30  jonblower
 * Added uploadFile()
 *
 * Revision 1.21  2005/05/23 16:48:17  jonblower
 * Overhauled CStyxFile (esp. asynchronous methods) and StyxConnection (added cache of CStyxFiles)
 *
 * Revision 1.20  2005/05/23 07:36:19  jonblower
 * Implementing uploadFileAsync
 *
 * Revision 1.19  2005/05/18 17:12:01  jonblower
 * Added getURL() method
 *
 * Revision 1.18  2005/05/17 14:36:11  jonblower
 * Fixed bug with getChildrenAsync()
 *
 * Revision 1.17  2005/05/12 15:59:59  jonblower
 * Implemented getChildrenAsync()
 *
 * Revision 1.16  2005/05/12 14:20:55  jonblower
 * Changed dataSent() method to dataWritten() (more accurate name)
 *
 * Revision 1.15  2005/05/12 08:00:33  jonblower
 * Added getChildrenAsync() to CStyxFile and childrenFound() to CStyxFileChangeListener
 *
 * Revision 1.14  2005/05/12 07:40:52  jonblower
 * CStyxFile.close() no longer throws a StyxException
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
 * Revision 1.1.1.1  2005/02/16 18:58:17  jonblower
 * Initial import
 *
 */
public class CStyxFile
{
    
    private static final Logger log = Logger.getLogger(CStyxFile.class);
    
    private StyxConnection conn;  // The connection on which the file sits
    private String path;          // The path of the file relative to the
                                  // root of the file server
    private String name;          // The name of the file (i.e. the last
                                  // part of the path)
    private long fid;             // The client's file identifier
    private Qid qid;              // The server's unique identifier for this file
    private DirEntry dirEntry;    // The server's representation of this file
    private int ioUnit;           // The maximum number of bytes that can be
                                  // written to, or read from, this file in
                                  // a single operation.
    private int mode;             // The mode under which we have the file open
                                  // -1 means that the file is not open
    private Vector listeners;     // The CStyxFileChangeListeners that are waiting
                                  // for notification of changes to this file
    private CStyxFile[] children; // The children of this directory
    
    /**
     * Creates a new instance of CStyxFile. This doesn't actually open or create
     * the file; use open() or create() for this.
     * @throws InvalidPathException if the path is not valid
     */
    CStyxFile(StyxConnection conn, String path)
    {
        this.conn = conn;
        this.path = getCanonicalPath(path);
        // Get the name of the file (the last part of the path)
        int lastSlash = this.path.lastIndexOf("/");
        this.name = this.path.substring(lastSlash + 1);
        this.fid = -1;
        this.ioUnit = 0;
        this.mode = -1;
        this.qid = null;
        this.dirEntry = null;
        this.listeners = new Vector();
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
     * Gets the full path of the parent of this file
     */
    public String getParentPath()
    {
        int lastSlash = this.path.lastIndexOf("/");
        return this.path.substring(0, lastSlash);
    }
    
    /**
     * @return the full URL to this file (e.g. "styx://localhost:9092/path/to/this")
     * @todo Include user name in the URL?
     */
    public String getURL()
    {
        StringBuffer buf = new StringBuffer("styx://");
        buf.append(this.conn.getRemoteHost());
        buf.append(":");
        buf.append(this.conn.getRemotePort());
        buf.append(this.getPath());
        return buf.toString();
    }
    
    /**
     * @return true if this is a directory. This method will block if it is
     * necessary to send a message to get the stat of this file.
     */
    public boolean isDirectory() throws StyxException
    {
        if (this.qid == null)
        {
            // TODO: we can get the qid with a walk to this location, as long
            // as it isn't the root directory
            this.refresh();
        }
        return (this.qid.getType() == 128);
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
     * @return the length of the file in bytes. This method will block if it is
     * necessary to send a message to get the stat of this file.
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
     * @return the last modified time of the file. This method will block if it is
     * necessary to send a message to get the stat of this file.
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
     * @return the DirEntry object containing this file's attributes. If the
     * DirEntry has not yet been set, this will return null.  In this case, 
     * call refresh() on this file.  Alternatively, if all you want is the Qid, 
     * it is possible that the Qid will be set, even if the DirEntry has not:
     * try getQid().
     */
    public DirEntry getDirEntry()
    {
        return this.dirEntry;
    }
    
    /**
     * Sets the dirEntry of this file.  This method is not used often.  It is most
     * useful when creating a CStyxFile from reading a directory: in this case
     * we have the DirEntry without having to separately stat the file.
     */
    public void setDirEntry(DirEntry dirEntry)
    {
        this.dirEntry = dirEntry;
        // TODO: nasty duplication of Qid information in DirEntry object and Qid
        this.qid = this.dirEntry.getQid();
    }
    
    /**
     * @return true if a fid has been set for this file
     */
    public boolean hasFid()
    {
        return (this.fid >= 0);
    }
    
    /**
     * @return the fid of this file
     */
    public long getFid()
    {
        return this.fid;
    }
    
    /**
     * Set the mode of this file: this should not normally be called, except
     * by the OpenCallback, when the file has been open
     */
    public void setMode(int newMode)
    {
        this.mode = newMode;
    }
    
    /**
     * @return the mode under which this file is open, or -1 if this file is 
     * not open.
     */
    public int getMode()
    {
        return this.mode;
    }
    
    /**
     * Sets the fid of the file.  This should not normally be called except
     * by GetFidCallback
     */
    public void setFid(long newFid)
    {
        this.fid = newFid;
    }
    
    /**
     * Sets the qid of the file.  This should not normally be called except
     * by the callbacks in the uk.ac.rdg.resc.jstyx.client.callbacks package
     */
    public void setQid(Qid newQid)
    {
        this.qid = newQid;
    }
    
    /**
     * @return the Qid of this file, or null if the qid has not yet been set
     */
    public Qid getQid()
    {
        return this.qid;
    }
    
    /**
     * @return true if this file is open (i.e. its mode has been set)
     */
    public boolean isOpen()
    {
        return (this.mode >= 0);
    }
    
    /**
     * Returns true if this CStyxFile represents the same file on the server
     * as the CStyxFile passed in the argument.  This method may block if the
     * dirEntry of one or both of the files is not set.  Compares the qids of
     * the two files (see Qid.equals())
     * @throws StyxException if the dirEntry of one of the files was not set and
     * there was an error getting the file's stat.  If you know that the dirEntry
     * of both files is already set, you can safely ignore this exception.
     */
    public boolean isSameFile(CStyxFile otherFile) throws StyxException
    {
        if (this.getQid() == null)
        {
            this.refresh();
        }
        if (otherFile.getQid() == null)
        {
            otherFile.refresh();
        }
        return this.getQid().equals(otherFile.getQid());
    }
    
    /**
     * Sets the maximum number of bytes that can be read from or written to
     * this file in a single operation.  This method should not normally be
     * called directly, except by the OpenCallback.
     */
    public void setIoUnit(int newIoUnit)
    {
        this.ioUnit = newIoUnit;
    }
    
    /**
     * @return the maximum number of bytes that can be read from or written to
     * this file in a single operation. Will only return valid data once the file
     * has been opened (or created).
     */
    public int getIoUnit()
    {
        return this.ioUnit;
    }
    
    /**
     * Sets the children of this file.  This is not normally called directly,
     * except by the GetChildrenCallback.
     */
    public void setChildren(CStyxFile[] children)
    {
        this.children = children;
    }
    
    /**
     * @return the Logger used by this class
     */
    public static Logger getLogger()
    {
        return log;
    }
    
    /**
     * Gets a new fid from the pool and walks it to the
     * location of this file. Does not block; calls the replyArrived() method
     * of the given callback if the walk was successful, or the error() method
     * otherwise.
     */
    public void walkFidAsync(MessageCallback callback)
    {
        new GetFidCallback(this, callback).walkFid();
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
        this.openAsync(mode, null);
    }
    
    /**
     * Opens the file on the server. This call does not block; the replyArrived()
     * method of the provided callback object will be called when the Ropen message
     * arrives. The error() method of the provided callback will be called if
     * an error occurred when opening the file, or if the file was already open
     * under a different mode.
     * @param mode Integer representing the mode - see the constants in StyxUtils.
     * For example, to open a file for reading, use StyxUtils.OREAD. To open a
     * file for writing with truncation use StyxUtils.OWRITE | StyxUtils.OTRUNC.
     * @param callback The MessageCallback object that will handle the Ropen message
     */
    public void openAsync(int mode, MessageCallback callback)
    {
        new OpenCallback(this, mode, callback).nextStage();
    }
    
    /**
     * Creates this file on the remote server, provided that its parent directory
     * exists.  Sends an error message to the callback if the file already exists.
     */
    public void createAsync(boolean isDirectory, int permissions, int mode,
        MessageCallback callback)
    {
        new CreateCallback(this, isDirectory, permissions, mode, callback).nextStage();
    }
    
    /**
     * Opens or creates this file: if the file exists it will be opened with 
     * the given mode.  If it does not exist it will be created, provided that
     * the parent directory exists.  Files will be created with 0666 permissions
     * and directories with 0777, subject to the permissions of the parent 
     * directory.
     */
    public void openOrCreateAsync(boolean isDirectory, int mode,
        MessageCallback callback)
    {
        new OpenOrCreateCallback(this, isDirectory, mode, callback).nextStage();
    }
    
    /**
     * Closes the file (i.e. clunks the fid). If the fid isn't set, this
     * will do nothing. This sends the Tclunk message but does not wait for a
     * reply (this doesn't matter because the rules of Styx say that the fid
     * of the file is invalid as soon as the Tclunk is sent, whether the Rclunk
     * arrives or not).  I.e. this method will not block.  The Rclunk message
     * is handled in the StyxConnection class.
     */
    public synchronized void close()
    {
        if (this.fid >= 0)
        {
            // Send the message to close the file (note that this will not wait
            // for a reply). We don't need to set a callback; when the reply arrives,
            // the fid will be returned to the connection's pool by the
            // StyxConnection class.
            this.conn.sendAsync(new TclunkMessage(this.fid), null);
        }
        // We reset all the properties of this file immediately; we don't need
        // to wait for the rClunk to arrive. This is because the fid will be
        // invalid even if the clunk fails - see clunk(5) in the Inferno manual.
        this.fid = -1;
        this.ioUnit = 0;
        this.mode = -1;
    }
    
    /**
     * Reads a chunk of data from the file. Reads the maximum amount of data
     * allowed in a single message, starting at the given file offset. Blocks
     * until the server replies with the data.
     *
     * When you have finished with the data in the ByteBuffer that is returned,
     * call release() on the buffer to ensure that the buffer can be re-used.
     *
     * @return a ByteBuffer containing the data that have been read
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
     * allowed in a single message, starting at the given file offset.  If the
     * file was not open before this method is called, the file will be opened
     * with mode StyxUtils.OREAD (but the fileOpen() event will not be fired).
     * Returns immediately; the dataArrived() events of registered
     * CStyxFileChangeListeners will be called when the data arrive, and the
     * error() events of registered CStyxFileChangeListeners will be called if
     * an error occurs.
     */
    public void readAsync(long offset)
    {
        this.readAsync(offset, -1, null);
    }
    
    /**
     * Reads a chunk of data from the file.  Reads the maximum number of bytes
     * allowed in a single message, starting at the given file offset. 
     * Returns immediately; the replyArrived() events of the provided callback
     * will be called when the data arrive, and the error() method of the
     * callback will be called if an error occurs.
     */
    public void readAsync(long offset, MessageCallback callback)
    {
        this.readAsync(offset, -1, callback);
    }
    
    /**
     * Reads a chunk of data from the file.  Sends request to read the given
     * number of bytes, starting at the given file offset. If bytesRequired < 0,
     * the maximum number of bytes allowed in a single message will be read.
     * Returns immediately; the replyArrived() events of the provided callback
     * will be called when the data arrive, and the error() method of the
     * callback will be called if an error occurs.
     */
    public void readAsync(long offset, int bytesRequired, MessageCallback callback)
    {
        new ReadCallback(this, offset, bytesRequired, callback).nextStage();
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
        if (this.mode < 1)
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
        boolean wasOpen = true;
        if (this.mode < 0)
        {
            // If the file isn't open, open it for writing with truncation
            this.open(StyxUtils.OWRITE | StyxUtils.OTRUNC);
            wasOpen = false;
        }
        
        byte[] bytes = StyxUtils.strToUTF8(str);
        // the writeAll() method will automatically take care of splitting the
        // input across multiple Styx messages if necessary. It will also check
        // that the file is open in the correct mode
        this.writeAll(bytes, 0);
        // If this file wasn't open before we called this function, close it
        if (!wasOpen)
        {
            this.close();
        }
    }
    
    /**
     * Writes a block of data to the file at the given offset. Will write the data
     * in several separate messages if necessary. The file will be truncated at
     * the end of the new data.
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
            
            // Write the bytes to the file with truncation
            int bytesWritten = (int)this.write(bytes, pos, bytesToWrite, filePos, true);
            
            // Update the pointers
            pos += bytesWritten;
            filePos += bytesWritten;
            
        } while (pos < bytes.length);
    }
    
    /**
     * Writes a block of data to the file at the given offset. Cannot write more
     * than this.getIOUnit() bytes in a single message. Blocks until the write
     * confirmation arrives. Does not change the offset of the file
     * @param bytes The data to write. Will attempt to write all the data in this array.
     * @param offset The position in the file at which to write the data
     * @param truncate True if the file is to be truncated at the end of the
     * new data
     * @return The number of bytes written to the file
     * @throws StyxException if there is an error writing to the file
     */
    public long write(byte[] bytes, long offset, boolean truncate) throws StyxException
    {
        return this.write(bytes, 0, bytes.length, offset, truncate);
    }
    
    /**
     * Writes a block of data to the file at the given offset. Cannot write more
     * than this.getIOUnit() bytes in a single message. Blocks until the write
     * confirmation arrives. Does not change the offset of the file
     * @param bytes The data to write.
     * @param pos The index of the first data point in the byte array to write
     * @param count The number of bytes from the input array to write
     * @param offset The position in the file at which to write the data
     * @param truncate True if the file is to be truncated at the end of the
     * new data
     * @return The number of bytes written to the file
     * @throws StyxException if there is an error writing to the file
     */
    public long write(byte[] bytes, int pos, int count, long offset,
        boolean truncate) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.writeAsync(bytes, pos, count, offset, truncate, callback);
        RwriteMessage rWriteMsg = (RwriteMessage)callback.getReply();
        return rWriteMsg.getNumBytesWritten();
    }
    
    /**
     * Writes a string to the file at the given offset, with truncation. When the write
     * confirmation arrives, the dataWritten() method of any registered
     * CStyxFileChangeListeners will be called. The file will be truncated
     * at the end of the string.
     */
    public void writeAsync(String str, long offset)
    {
        this.writeAsync(StyxUtils.strToUTF8(str), offset, true);
    }
    
    /**
     * Writes a chunk of data to the file at the given file offset.
     * Returns immediately; the dataWritten() method of any waiting change listeners
     * will be called when the write confirmation arrives, and the error() method
     * of any waiting change listeners will be called if an error occurs.
     * @param bytes The byte array containing the data to write. This will attempt to
     * write all the data in the array
     * @param offset The position in the file at which the data will be written
     * @param truncate True if the file should be truncated at the end of the new data
     */
    public void writeAsync(byte[] bytes, long offset, boolean truncate)
    {
        this.writeAsync(bytes, 0, bytes.length, offset, truncate, null);
    }
    
    /**
     * Writes a chunk of data to the file at the given file offset.
     * Returns immediately; the callback's replyArrived() method will be called
     * when the reply arrives and the callback's error() method will be called
     * if an error occurs.
     * @param bytes The array of bytes to write
     * @param pos The position in the array of the first byte to write
     * @param count The number of bytes from the byte array to write
     * @param offset The position in the file at which the data will be written
     * @param truncate If this is true, the file will be truncated at the end
     * of the new data
     * @param callback The replyArrived() method of this callback object will be
     * called when the write confirmation arrives
     */
    public void writeAsync(byte[] bytes, int pos, int count, long offset,
        boolean truncate, MessageCallback callback)
    {
        new WriteCallback(this, bytes, pos, count, offset, truncate, callback).nextStage();
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
     * set and the statChanged() event of any registered change listeners will be
     * fired.
     * @todo In the case of a directory, should this refresh the list of children?
     */
    public void refreshAsync()
    {
        this.refreshAsync(null);
    }
    
    /**
     * Refreshes the status of the file by sending a TstatMessage. Does not wait
     * for a reply; when the reply arrives, the dirEntry of this file will be 
     * set and the replyArrived() method of the provided callback object
     * will be called with an RwalkMessage as the argument.
     * @todo In the case of a directory, should this refresh the list of children?
     * @todo If there is already a Tstat message in flight, what should we do?
     * Does it matter much?
     */
    public void refreshAsync(MessageCallback callback)
    {
        new RefreshCallback(this, callback).nextStage();
    }
    
    /**
     * Gets a file on the same connection as this CStyxFile, without opening it.
     * @param path The path of the file to be opened, <i>relative to this file</i>.
     * @throws InvalidPathException if the path is not valid
     */
    public CStyxFile getFile(String path)
    {
        StringBuffer fullPath = new StringBuffer(this.getPath());
        if (!this.getPath().endsWith("/"))
        {
            fullPath.append("/");
        }
        fullPath.append(path);
        return this.conn.getFile(fullPath.toString());
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
     * this will return null (TODO: does it?  Or does it throw an Exception?).
     * (For an empty directory, this will return a zero- length array.)
     * This method blocks until the directory has been read.
     */
    public CStyxFile[] getChildren() throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.getChildrenAsync(callback);
        // Wait for the reply to arrive.  The reply will be the last Rmessage
        // in the sequence needed to read the directory contents, i.e. an 
        // empty RreadMessage.  This is just a signal that the process is
        // complete.
        RreadMessage rReadMsg = (RreadMessage)callback.getReply();
        // The children of this directory will now have been set
        return this.children;
    }
    
    /**
     * Reads this directory to get all its children. When the process is completed
     * the childrenFound() event is fired on all registered listeners.
     */
    public void getChildrenAsync()
    {
        this.getChildrenAsync(null);
    }
    
    /**
     * Reads this directory to get all its children. When the process is completed
     * the replyArrived() event is fired on the callback, with a zero-length
     * RreadMessage as the argument.
     */
    private void getChildrenAsync(MessageCallback callback)
    {
        new GetChildrenCallback(this, callback).nextStage();
    }
    
    /**
     * Downloads the data from this file and writes to a local java.io.File.
     * If this file already exists it will be overwritten.
     * This method blocks; it returns when the download is complete and throws
     * a StyxException if an error occurs.
     * @param file The java.io.File to which the data will be written.  If this
     * file already exists it will be overwritten.  If this is null, the data
     * will be downloaded but not written to a file
     */
    public void download(File file) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.downloadAsync(file, callback);
        // The getReply() method blocks until the download is complete.
        StyxMessage message = callback.getReply();
    }
    
    /**
     * Downloads the data from this file and writes to a local java.io.File.
     * If this file already exists it will be overwritten.
     * This method blocks; it returns when the download is complete and throws
     * a StyxException if an error occurs.
     * @param file The java.io.File to which the data will be written.  If this
     * file already exists it will be overwritten.  If this is null, the data
     * will be downloaded but not written to a file
     * @param numRequests The number of simultaneous read requests that will be 
     * sent.  The download may proceed faster with a larger number of simultaneous
     * read requests.  If numRequests > 1, the file on the remote server <b>must</b>
     * be seekable otherwise unpredictable behaviour may result.
     */
    public void download(File file, int numRequests) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.downloadAsync(file, numRequests, callback);
        // The getReply() method blocks until the download is complete.
        StyxMessage message = callback.getReply();
    }
    
    /**
     * Downloads the data from this file and writes to a local java.io.File. 
     * This method returns immediately; when the download has finished, the
     * downloadComplete() event will be fired all all registered change listeners.
     * @param file The java.io.File to which the data will be written.  If this
     * file already exists it will be overwritten.
     * @param numRequests The number of simultaneous read requests that will be 
     * sent.  The download may proceed faster with a larger number of simultaneous
     * read requests.  If numRequests > 1, the file on the remote server <b>must</b>
     * be seekable otherwise unpredictable behaviour may result.
     */
    public void downloadAsync(File file, int numRequests)
    {
        this.downloadAsync(file, numRequests, null);
    }
    
    /**
     * Downloads the data from this file and writes to a local java.io.File. 
     * This method returns immediately; when the download has finished, the
     * downloadComplete() event will be fired all all registered change listeners.
     * @param file The java.io.File to which the data will be written.  If this
     * file already exists it will be overwritten.
     */
    public void downloadAsync(File file)
    {
        this.downloadAsync(file, null);
    }
    
    /**
     * Downloads the data from this file and writes to a local java.io.File. 
     * This method returns immediately; when the download has finished, the
     * replyArrived() method of the given MessageCallback will be called.
     * @param file The java.io.File to which the data will be written.  If this
     * file already exists it will be overwritten.
     */
    public void downloadAsync(File file, MessageCallback callback)
    {
        this.downloadAsync(file, 1, callback);
    }
    
    /**
     * Downloads the data from this file and writes to a local java.io.File. 
     * This method returns immediately; when the download has finished, the
     * replyArrived() method of the given MessageCallback will be called.
     * @param file The java.io.File to which the data will be written.  If this
     * file already exists it will be overwritten.
     * @param numRequests The number of simultaneous read requests that will be 
     * sent.  The download may proceed faster with a larger number of simultaneous
     * read requests.  If numRequests > 1, the file on the remote server <b>must</b>
     * be seekable otherwise unpredictable behaviour may result.
     * @throws IllegalArgumentException if numRequests is less than 1 or greater
     * than 100.
     */
    public void downloadAsync(File file, int numRequests, MessageCallback callback)
    {
        if (numRequests < 1 || numRequests > 100)
        {
            throw new IllegalArgumentException("numRequests must be between 1 and 100 inclusive");
        }
        new DownloadCallback(this, file, numRequests, callback).nextStage();
    }
    
    /**
     * Uploads data from an InputStream to this file.  If this file does
     * not exist it will be created with rw-rw-rw- (0666) permissions, subject
     * to the permissions of the host directory.  Blocks until the file has been
     * uploaded, or throws a StyxException if an error occurred.
     * @param in The InputStream from which to read data to be written to this file
     * @todo Add a flag to prevent overwriting a file if it already exists?
     * @todo Allow a callback to be provided for progress monitoring?
     */
    public void upload(InputStream in) throws StyxException
    {
        StyxReplyCallback callback = new StyxReplyCallback();
        this.uploadAsync(in, callback);
        // The getReply() method blocks until the download is complete.
        StyxMessage message = callback.getReply();
    }
    
    /**
     * Uploads data from an InputStream to this file.  If this file does
     * not exist it will be created with rw-rw-rw- (0666) permissions, subject
     * to the permissions of the host directory.  When the process is finished,
     * the uploadComplete() event will be fired on all registered
     * CStyxFileChangeListeners.  If an error occurs, the error() event will be
     * fired on registered change listeners.
     * @param in The InputStream from which to read data to be written to this file
     * @todo Add a flag to prevent overwriting a file if it already exists?
     * @todo Allow a callback to be provided for progress monitoring?
     */
    public void uploadAsync(InputStream in)
    {
        this.uploadAsync(in, null);
    }
    
    /**
     * Uploads data from an InputStream to this file.  If this file does
     * not exist it will be created with rw-rw-rw- (0666) permissions, subject
     * to the permissions of the host directory.  When the process is finished,
     * the replyArrived() method of the provided callback object will be called.
     * If an error occurs, the error() method of the provided callback object
     * will be called.
     * @param in The InputStream from which to read data to be written to this file
     * @param callback The MessageCallback that will be notified when the
     * upload process is complete.
     * @todo Add a flag to prevent overwriting a file if it already exists?
     * @todo Allow a callback to be provided for progress monitoring?
     */
    public void uploadAsync(InputStream in, MessageCallback callback)
    {
        new UploadCallback(this, in, callback).nextStage(null, null);
    }
    
    /**
     * Uploads data from a local java.io.File to this file.  If this (Styx) file does
     * not exist it will be created with rw-rw-rw- (0666) permissions, subject
     * to the permissions of the host directory.  When the process is finished,
     * the uploadComplete() event will be fired on all registered
     * CStyxFileChangeListeners.  If an error occurs, the error() event will be
     * fired on registered change listeners.
     * @param file The File to copy/upload
     * @todo Add a flag to prevent overwriting a file if it already exists?
     * @todo Allow a callback to be provided for progress monitoring?
     */
    public void uploadAsync(File file)
    {
        this.uploadAsync(file, null);
    }
    
    /**
     * Uploads data from a local java.io.File to this file.  If this (Styx) file does
     * not exist it will be created with rw-rw-rw- (0666) permissions, subject
     * to the permissions of the host directory.  When the process is finished,
     * the replyArrived() method of the provided callback object will be called.
     * If an error occurs, the error() method of the provided callback object
     * will be called.
     * @param file The File to copy/upload
     * @param callback The MessageCallback that will be notified when the
     * upload process is complete.
     * @todo Add a flag to prevent overwriting a file if it already exists?
     * @todo Allow a callback to be provided for progress monitoring?
     */
    public void uploadAsync(File file, MessageCallback callback)
    {
        new UploadCallback(this, file, callback).nextStage(null, null);
    }
    
    /**
     * Adds a CStyxFileChangeListener to this file. The methods of the change
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
    public void fireError(String message)
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
    public void fireOpen()
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
     * Fires the fileCreated() method on all registered listeners
     */
    public void fireCreated()
    {
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.fileCreated(this, this.mode);
            }
        }
    }
    
    /**
     * Fires the dataArrived() method on all registered listeners
     */
    public void fireDataArrived(TreadMessage tReadMsg, RreadMessage rReadMsg)
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
    public void fireStatChanged(RstatMessage rStatMsg)
    {
        this.setDirEntry(rStatMsg.getDirEntry());
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
     * Fires the dataWritten() method on all registered listeners
     */
    public void fireDataWritten(TwriteMessage tWriteMsg)
    {
        // Notify all listeners that the data have been written
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.dataWritten(this, tWriteMsg);
            }
        }
    }
    
    /**
     * Fires the childrenFound() method on all registered listeners
     */
    public void fireChildrenFound()
    {
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.childrenFound(this, this.children);
            }
        }
    }
    
    /**
     * Fires the uploadComplete() method on all registered listeners
     */
    public void fireUploadComplete()
    {
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.uploadComplete(this);
            }
        }
    }
    
    /**
     * Fires the downloadComplete() method on all registered listeners
     */
    public void fireDownloadComplete()
    {
        synchronized(this.listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                CStyxFileChangeListener listener =
                    (CStyxFileChangeListener)this.listeners.get(i);
                listener.downloadComplete(this);
            }
        }
    }
    
    /**
     * Gets the canonical version of the given path.  Removes all duplicate
     * slashes, collapses all "." and ".." and checks that the path is
     * valid and absolute, throwing an InvalidPathException if not.  This canonical
     * path is used as the key for the Hashtable of known CStyxFiles.
     * @todo Move to StyxUtils?
     */
    private static String getCanonicalPath(String path) throws InvalidPathException
    {
        if (!path.startsWith("/"))
        {
            throw new InvalidPathException("Path must be absolute");
        }
        Vector canonicalPathEls = new Vector();
        String[] pathEls = path.split("/");
        for (int i = 0; i < pathEls.length; i++)
        {
            // Ignore any "." path elements
            if (!pathEls[i].equals("."))
            {
                // Deal with calls to previous directory
                if (pathEls[i].equals(".."))
                {
                    if (canonicalPathEls.size() > 0)
                    {
                        canonicalPathEls.remove(canonicalPathEls.size() - 1);
                    }
                }
                else
                {
                    // Check for spaces or backslashes in the filename
                    if ((pathEls[i].indexOf(" ") != -1) || 
                        (pathEls[i].indexOf("\\") != -1))
                    {
                        throw new InvalidPathException("'" + pathEls[i] + 
                            "' is an invalid filename");
                    }
                    else
                    {
                        // TODO: check for illegal names that consist entirely of dots
                        if (!pathEls[i].equals(""))
                        {
                            canonicalPathEls.add(pathEls[i]);
                        }
                    }
                }
            }
        }
        // Reconstruct the path
        StringBuffer canonicalPath = new StringBuffer("/");
        for (int i = 0; i < canonicalPathEls.size(); i++)
        {
            canonicalPath.append(canonicalPathEls.get(i));
            if (i < canonicalPathEls.size() - 1)
            {
                canonicalPath.append("/");
            }
        }
        return canonicalPath.toString();
    }
    
    public static void main(String[] args) throws Exception
    {
        StyxConnection conn = new StyxConnection("localhost", 6666);
        conn.connect();
        CStyxFile file = conn.getFile("/tmp/test7.txt");
        file.upload(new java.io.FileInputStream(new java.io.File("C:\\test.log")));
        System.out.println("upload complete");
        conn.close();
    }
}
