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

import org.apache.mina.common.ByteBuffer;

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
 * Revision 1.2  2005/09/01 17:12:09  jonblower
 * Changes to Input and Output stream code
 *
 * Revision 1.1  2005/08/31 17:03:18  jonblower
 * Renamed "StyxFile*putStream*" to "CStyxFile*putStream*" for consistency with CStyxFile class
 *
 * Revision 1.8  2005/06/22 17:07:29  jonblower
 * Added read(byte[]) method
 * 
 * Revision 1.7  2005/05/12 07:40:52  jonblower
 * CStyxFile.close() no longer throws a StyxException
 * 
 * Revision 1.6  2005/05/05 07:09:06  jonblower
 * Improved comments
 * 
 * Revision 1.5  2005/05/04 16:25:49  jonblower
 * Improved parameter naming in constructor
 * 
 * Revision 1.4  2005/03/22 10:19:52  jonblower
 * Fixed problem with ByteBuffer leak in StyxMessageDecoder and CStyxFileInputStream
 * 
 * Revision 1.3  2005/03/19 21:46:58  jonblower
 * Further fixes relating to releasing ByteBuffers
 * 
 * Revision 1.2  2005/03/16 17:55:53  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 * 
 * Revision 1.1.1.1  2005/02/16 18:58:19  jonblower
 * Initial import
 */
public class CStyxFileInputStream extends InputStream
{
    
    private CStyxFile file; // The file from which we are reading
    private ByteBuffer buf; // Buffer for storing the results of the last read
    private long offset;    // The current position in the file
    private boolean eof;
    private boolean closeConnectionWhenCloseStream; // If this is true, we shall close the underlying
        // StyxConnection when this stream is closed (this is normally set when
        // getting an input stream through the StyxURLConnection class)
    
    /**
     * Creates a CStyxFileInputStream for reading the given file.
     * @param file The file to read from
     * @param closeConnectionWhenCloseStream If this is true, we shall close the underlying
     * StyxConnection when this stream is closed (this is normally set when
     * getting an input stream through the StyxURLConnection class)
     */
    public CStyxFileInputStream(CStyxFile file, boolean closeConnectionWhenCloseStream)
    {
        if (file == null)
        {
            throw new NullPointerException("file cannot be null");
        }
        this.file = file;
        this.buf = null;
        this.offset = 0;
        this.eof = false;
        this.closeConnectionWhenCloseStream = closeConnectionWhenCloseStream;
    }
    
    public CStyxFileInputStream(CStyxFile file)
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
                // Release the previous read buffer if we have one
                if (this.buf != null)
                {
                    this.buf.release();
                }
                // Read a new chunk of data from the file
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
                    // We don't need the buffer any more
                    this.buf.release();
                    return -1;
                }
            }
            catch(StyxException e)
            {
                throw new IOException(e.getMessage());
            }
        }
    }
    
    public int read(byte b[], int off, int len) throws IOException
    {
	if (b == null)
        {
	    throw new NullPointerException();
	}
        else if ((off < 0) || (off > b.length) || (len < 0) ||
		   ((off + len) > b.length) || ((off + len) < 0))
        {
	    throw new IndexOutOfBoundsException();
	}
        else if (len == 0)
        {
	    return 0;
	}
        
        if (this.eof)
        {
            return -1;
        }
        
        // First check to see if there are any bytes left in the buffer
        if (this.buf != null && this.buf.hasRemaining())
        {
            // Read the data into the provided array
            int bytesToGet = Math.min(this.buf.remaining(), len);
            this.buf.get(b, off, bytesToGet);
            return bytesToGet;
        }
        else
        {
            // We have read everything that's in the buffer. 
            // We need to read another block of data.
            try
            {
                // Release the previous read buffer if we have one
                if (this.buf != null)
                {
                    this.buf.release();
                }
                // Read a new chunk of data from the file
                this.buf = this.file.read(this.offset);
                if (this.buf.remaining() > 0)
                {
                    // Update the offset
                    this.offset += this.buf.remaining();
                    // Read the data into the provided array
                    int bytesToGet = Math.min(this.buf.remaining(), len);
                    this.buf.get(b, off, bytesToGet);
                    return bytesToGet;
                }
                else
                {
                    // We have reached the end of the file
                    this.eof = true;
                    // We don't need the buffer any more
                    this.buf.release();
                    return -1;
                }
            }
            catch(StyxException e)
            {
                throw new IOException(e.getMessage());
            }
        }
    }
    
    /**
     * @return the number of bytes that can be read without blocking
     */
    public int available() throws IOException
    {
        return (this.buf == null) ? 0 : this.buf.remaining();
    }
    
    /**
     * Closes the stream (clunks the underlying file). If this InputStream was
     * created with <code>closeConnectionWhenCloseStream = true</code>, this will
     * also close the StyxConnection.
     */
    public void close() throws IOException
    {
        this.file.close();
        this.offset = 0;
        this.eof = false;
        if (this.closeConnectionWhenCloseStream)
        {
            this.file.getConnection().close();
        }
        this.buf = null;
    }
}
