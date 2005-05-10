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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;
import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Class representing a file on a hard drive. Note that creating a new instance
 * of this class does not create an actual new file on the hard disk; it simply
 * creates a wrapper for an existing file.
 *
 * This class opens a new Input/OutputStream every time a read/write message
 * arrives, so it is probably not very efficient (but this was the simplest way
 * to implement reliably).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.11  2005/05/10 19:19:05  jonblower
 * Reinstated chan.truncate()
 *
 * Revision 1.10  2005/05/10 12:43:57  jonblower
 * Rewrote and simplified: now closes file after each read or write
 *
 * Revision 1.9  2005/05/10 08:02:18  jonblower
 * Changes related to implementing MonitoredFileOnDisk
 *
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
    private static final Logger log = Logger.getLogger(FileOnDisk.class);
    
    protected File file;
    
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
    }
    
    public synchronized void read(StyxFileClient client, long offset, int count, int tag)
        throws StyxException
    {
        try
        {
            // Open a new FileChannel for reading
            FileChannel chan = new FileInputStream(this.file).getChannel();

            // Get a ByteBuffer from MINA's pool. This gets released when the
            // RreadMessage is sent back to the client
            ByteBuffer buf = ByteBuffer.allocate(count);
            // Make sure the position and limit are set correctly (remember that
            // the actual buffer size might be larger than requested)
            buf.position(0).limit(count);

            // Read from the channel. If no bytes were read (due to EOF), the
            // position of the buffer will not have changed
            int numRead = chan.read(buf.buf(), offset);
            log.debug("Read " + numRead + " bytes from " + this.file.getPath());
            // Close the channel
            chan.close();

            buf.flip();
            // The buffer will be released when the RreadMessage has been written
            this.replyRead(client, buf, tag);
        }
        catch(FileNotFoundException fnfe)
        {
            // The file has been removed
            log.debug("The file " + this.file.getPath() +
                " has been removed by another process");
            // Remove the file from the Styx server
            this.remove();
            throw new StyxException("The file " + this.name + " was removed.");
        }
        catch(IOException ioe)
        {
            throw new StyxException("An error of class " + ioe.getClass() + 
                " occurred when trying to read from " + this.getFullPath() +
                ": " + ioe.getMessage());
        }
    }
    
    public synchronized void write(StyxFileClient client, long offset, 
        int count, ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        try
        {
            // Open a new FileChannel for writing. Can't use FileOutputStream
            // as this doesn't allow successful writing at a certain file offset
            FileChannel chan = new RandomAccessFile(this.file, "rw").getChannel();
            
            // Remember old limit and position
            int pos = data.position();
            int lim = data.limit();
            // Make sure only the requested number of bytes get written
            data.limit(data.position() + count);
            
            // Write to the file
            int nWritten = chan.write(data.buf(), offset);
            
            // Reset former buffer positions
            data.limit(lim).position(pos);
            
            // Truncate the file at the end of the new data if requested
            if (truncate)
            {
                log.debug("Truncating file at " + (offset + nWritten) + " bytes");
                chan.truncate(offset + nWritten);
            }
            // Close the channel
            chan.close();
            // Reply to the client
            this.replyWrite(client, nWritten, tag);
        }
        catch(FileNotFoundException fnfe)
        {
            // The file has been removed
            log.debug("The file " + this.file.getPath() +
                " has been removed by another process");
            // Remove the file from the Styx server
            this.remove();
            throw new StyxException("The file " + this.name + " was removed.");
        }
        catch(IOException ioe)
        {
            throw new StyxException("An error of class " + ioe.getClass() + 
                " occurred when trying to write to " + this.getFullPath() +
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
     * Closes the open FileChannel and removes the underlying file from the disk
     */
    protected synchronized void delete()
    {
        if (this.file.exists())
        {
            this.file.delete();
        }
    }
    
}
