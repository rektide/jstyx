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

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * InputStream for reading from a file on a Styx server
 * @todo Implement read(byte[]), read(byte[], off, len), skip() in most
 * efficient way possible
 * @todo Also make into a Channel?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:19  jonblower
 * Initial revision
 *
 */
public class StyxFileInputStream extends InputStream
{
    
    private CStyxFile file; // The file from which we are reading
    private ByteBuffer buf; // Buffer for storing the results of the last read
    private long offset;    // The current position in the file
    private boolean eof;
    private boolean openedThroughURL; // If this is true, we shall close the underlying
        // StyxConnection when this stream is closed (this is normally set when
        // getting an input stream through the StyxURLConnection class)
    
    /**
     * Creates a StyxFileInputStream for reading the given file.
     * @param openedThroughURL If this is true, we shall close the underlying
     * StyxConnection when this stream is closed (this is normally set when
     * getting an input stream through the StyxURLConnection class)
     */
    public StyxFileInputStream(CStyxFile file, boolean openedThroughURL)
    {
        if (file == null)
        {
            throw new NullPointerException("file cannot be null");
        }
        this.file = file;
        this.buf = null;
        this.offset = 0;
        this.eof = false;
        this.openedThroughURL = openedThroughURL;
    }
    
    public StyxFileInputStream(CStyxFile file)
    {
        this(file, false);
    }
    
    public int read() throws IOException
    {
        if (this.eof)
        {
            return -1;
        }
        // First check to see if there are any bytes left in the buffer
        if (this.buf != null && this.buf.hasRemaining())
        {
            return buf.get() & 0xff;  // Makes sure byte is always between 0 and 255
        }
        else
        {
            // We have read everything that's in the buffer. 
            // We need to read another block of data.
            try
            {
                this.buf = this.file.read(this.offset);
                if (this.buf.remaining() > 0)
                {
                    // Update the offset
                    this.offset += this.buf.remaining();
                    // Return the first byte in the buffer
                    return buf.get() & 0xff; // Makes sure byte is always between 0 and 255
                }
                else
                {
                    // We have reached the end of the file
                    this.eof = true;
                    return -1;
                }
            }
            catch(StyxException e)
            {
                throw new IOException(e.getMessage());
            }
        }
    }
    
    /* TODO: implement more efficient reading (several bytes at a time) */
    
    /**
     * @return the number of bytes that can be read without blocking
     */
    public int available() throws IOException
    {
        return (this.buf == null) ? 0 : this.buf.remaining();
    }
    
    /**
     * Closes the stream (clunks the underlying file)
     */
    public void close() throws IOException
    {
        try
        {
            this.file.close();
            this.offset = 0;
            this.eof = false;
            if (this.openedThroughURL)
            {
                this.file.getConnection().close();
            }
        }
        catch (StyxException se)
        {
            throw new IOException(se.getMessage());
        }
        this.buf = null;
    }
}
