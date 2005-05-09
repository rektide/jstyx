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

package uk.ac.rdg.resc.jstyx.server;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Class representing a file on a hard drive. Note that creating a new instance
 * of this class does not create an actual new file on the hard disk; it simply
 * creates a wrapper for an existing file.
 *
 * Note that this class keeps the underlying file open for as long as possible,
 * so other processes may not be able to change the underlying file.  This class
 * should therefore be used when you know that other processes will not be
 * modifying the underlying file.  TODO Change this behaviour?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.8  2005/05/09 07:13:52  jonblower
 * Changed getFileOnDisk() to getFileOrDirectoryOnDisk()
 *
 * Revision 1.7  2005/04/28 08:11:15  jonblower
 * Modified permissions handling in documentation directory of SGS
 *
 * Revision 1.6  2005/04/27 16:11:43  jonblower
 * Added capability to add documentation files to SGS namespace
 *
 * Revision 1.5  2005/03/24 09:48:31  jonblower
 * Changed 'count' from long to int throughout for reading and writing
 *
 * Revision 1.4  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.3  2005/03/16 17:56:23  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.2  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.1.1.2.1  2005/03/10 20:55:37  jonblower
 * Removed references to Netty
 *
 * Revision 1.1.1.1  2005/02/16 18:58:31  jonblower
 * Initial import
 *
 */
public class FileOnDisk extends StyxFile
{
    
    protected File file;
    protected FileChannel chan;
    protected boolean closeAfterReadOrWrite; // If this is true, the underlying
        // file will be closed after each read or write via the Styx interface.
        // This is useful if another process writes to the file, but is inefficient
        // otherwise.
    
    /**
     * Gets a StyxFile that wraps the given java.io.File. If the File is a 
     * directory, this will return an instance of DirectoryOnDisk, otherwise
     * it will return an instance of FileOnDisk.
     * @todo Set file permissions correctly
     * @throws StyxException if the File does not exist
     */
    public static StyxFile getFileOrDirectoryOnDisk(File file) throws StyxException
    {
        if (!file.exists())
        {
            throw new StyxException(file.getName() + " does not exist");
        }
        if (file.isDirectory())
        {
            return new DirectoryOnDisk(file);
        }
        else
        {
            return new FileOnDisk(file);
        }
    }
    
    /**
     * Creates a new FileOnDisk that wraps the file at the given path
     */
    public FileOnDisk(String filepath) throws StyxException
    {
        this(new File(filepath));
    }
    
    /**
     * Creates a new FileOnDisk whose name is the same as that of (the last part
     * of) the underlying file
     * @param file The java.io.File which this represents
     * @throws StyxException if the java.io.File does not exist
     */
    public FileOnDisk(File file) throws StyxException
    {
        this(file.getName(), file);
    }
    
    /**
     * Creates a FileOnDisk with the default permissions (0666, rw-rw-rw-)
     * @param name The name of this file as it will appear in the namespace
     * @param file The java.io.File which this represents
     * @throws StyxException if the java.io.File does not exist
     */
    public FileOnDisk(String name, File file) throws StyxException
    {
        this(name, file, 0666);
    }
    
    /**
     * @param name The name of this file as it will appear in the namespace
     * @param file The java.io.File which this represents
     * @param permissions the permissions of the file
     * @throws StyxException if the java.io.File does not exist
     */
    public FileOnDisk(String name, File file, int permissions) throws StyxException
    {
        super(name.trim(), permissions);
        if (!file.exists())
        {
            throw new StyxException("file " + file.getName() + " does not exist");
        }
        this.file = file;
        this.chan = null; // allocate this when the file is first read or written to
        this.closeAfterReadOrWrite = false; // By default, file will be left open
            // after each read or write.
    }
    
    /**
     * Sets whether or not the underlying file should be closed after each read or write
     * via this Styx interface.  This is set false when a FileOnDisk is created.  This 
     * should be set true if it is expected that another process might be writing to
     * this file.  However, if other processes are not expected to be writing to the
     * underlying file, this should be set false for efficiency.
     */
    public void setCloseAfterReadOrWrite(boolean flag)
    {
        this.closeAfterReadOrWrite = flag;
    }
    
    private void checkChannel() throws StyxException
    {
        if (this.chan == null || !this.chan.isOpen())
        {
            if (!this.file.exists())
            {
                // remove this file from the directory tree
                this.remove();
                throw new StyxException("file " + file.getName() + " has been removed");
            }
            try
            {
                this.chan = new RandomAccessFile(this.file, "rw").getChannel();
            }
            catch(FileNotFoundException fnfe)
            {
                throw new StyxException("File " + this.file.getName() + 
                    " cannot be opened");
            }
        }        
    }
    
    public synchronized void read(StyxFileClient client, long offset, int count, int tag)
        throws StyxException
    {
        this.checkChannel();
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(count);
        int numRead = 0;
        try
        {
            numRead = this.chan.read(buf, offset);
            if (this.closeAfterReadOrWrite)
            {
                this.chan.close();
            }
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            throw new StyxException("error reading " + this.file.getName() + 
                ": " + ioe.getMessage());
        }
        if (numRead < 1)
        {
            // offset is past the end of the file
            this.replyRead(client, new byte[0], tag);
        }
        else
        {
            buf.flip();
            this.replyRead(client, buf, tag);
        }
    }
    
    public synchronized void write(StyxFileClient client, long offset, 
        int count, ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        this.checkChannel();
        try
        {
            // Remember old limit and position
            int pos = data.position();
            int lim = data.limit();
            data.limit(data.position() + count);
            int nWritten = this.chan.write(data.buf(), offset);
            // Reset former buffer positions
            data.limit(lim);
            data.position(pos);
            if (truncate)
            {
                this.chan.truncate(offset + nWritten);
            }
            if (this.closeAfterReadOrWrite)
            {
                this.chan.close();
            }
            this.replyWrite(client, nWritten, tag);
        }
        catch(IOException ioe)
        {
            throw new StyxException("error writing to " + this.file.getName() + 
                ": " + ioe.getMessage());
        }
    }
    
    /**
     * Reads all metadata from underlying disk file
     */
    public synchronized void refresh()
    {
        // Update the last modified time
        this.lastModifiedTime = this.file.lastModified() / 1000;
    }
    
    public ULong getLength()
    {
        return new ULong(this.file.length());
    }
    
    /**
     * Removes the underlying file from the disk
     */
    protected synchronized void delete()
    {
        this.file.delete();
    }
    
    /**
     * This is called when the file is clunked, just before the current client
     * is removed from the list of active clients.
     */
    protected synchronized void clientDisconnected(StyxFileClient client)
    {
        // Only close the channel if this is the last client to disconnect
        if (this.getNumClients() == 0)
        {
            try
            {
                if (this.chan != null)
                {
                    this.chan.close();
                }
            }
            catch (IOException ioe)
            {
                // ignore any exceptions
            }
        }
    }
    
}
