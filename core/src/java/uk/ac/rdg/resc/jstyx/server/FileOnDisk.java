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
 * creates a wrapper for an existing file. To create a new file on the hard disk,
 * use createChild().
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
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
    
    /**
     * Gets a StyxFile that wraps the given java.io.File. If the File is a 
     * directory, this will return an instance of DirectoryOnDisk, otherwise
     * it will return an instance of FileOnDisk.
     * @throws StyxException if the File does not exist
     */
    public static StyxFile getFileOnDisk(File file) throws StyxException
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
    
    public FileOnDisk(String name) throws StyxException
    {
        this(new File(name));
    }
    
    /**
     * @param file The java.io.File which this represents
     * @throws StyxException if the java.io.File does not exist
     */
    public FileOnDisk(File file) throws StyxException
    {
        super(file.getName());
        if (!file.exists())
        {
            throw new StyxException("file " + file.getName() + " does not exist");
        }
        this.file = file;
        this.chan = null; // allocate this when the file is first read or written to
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
    
    public synchronized void read(StyxFileClient client, long offset, long count, int tag)
        throws StyxException
    {
        this.checkChannel();
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate((int)count);
        int numRead = 0;
        try
        {
            numRead = this.chan.read(buf, offset);
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
        long count, ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        this.checkChannel();
        try
        {
            data.limit((int)count);
            int nWritten = this.chan.write(data.buf(), offset);
            if (truncate)
            {
                this.chan.truncate(offset + nWritten);
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
