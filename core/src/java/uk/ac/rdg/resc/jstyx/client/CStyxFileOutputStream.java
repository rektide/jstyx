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

import java.io.OutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Output stream for writing data to a Styx File.  The file will be truncated
 * at the end of the data that are written through this class.
 * 
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/12/01 08:21:55  jonblower
 * Fixed javadoc comments
 *
 * Revision 1.3  2005/10/14 18:04:33  jonblower
 * Fixed bug with not updating file offset, and added code to write zero bytes to signify EOF
 *
 * Revision 1.2  2005/09/01 17:12:10  jonblower
 * Changes to Input and Output stream code
 *
 * Revision 1.1  2005/08/31 17:03:18  jonblower
 * Renamed "StyxFile*putStream*" to "CStyxFile*putStream*" for consistency with CStyxFile class
 * 
 * Revision 1.4  2005/05/23 16:48:17  jonblower
 * Overhauled CStyxFile (esp. asynchronous methods) and StyxConnection (added cache of CStyxFiles)
 * 
 * Revision 1.3  2005/05/12 07:40:52  jonblower
 * CStyxFile.close() no longer throws a StyxException
 * 
 * Revision 1.2  2005/03/16 17:55:53  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 * 
 * Revision 1.1.1.1  2005/02/16 18:58:19  jonblower
 * Initial import
 */
public class CStyxFileOutputStream extends OutputStream
{
    private static final Logger log = Logger.getLogger(CStyxFileOutputStream.class);
    
    private CStyxFile file; // The file to which we are writing
    private byte[] buf;     // Buffer for storing the results of the last write
    private int pos;        // Current position in the buffer
    private long offset;    // The current position in the file
    private boolean closeConnectionWhenCloseStream; // If this is true, we shall close the underlying
        // StyxConnection when this stream is closed (this is normally set when
        // getting an output stream through the StyxURLConnection class)
    
    /**
     * Creates a new CStyxFileOutputStream to write data to the given CStyxFile.
     * If the file already exists it will be overwritten.
     * @param file the file to write to
     * @param closeConnectionWhenCloseStream If this is true, we shall close the underlying
     * StyxConnection when this stream is closed (this is normally set when
     * getting an output stream through the StyxURLConnection class)
     * @todo Add flag to prevent overwriting in certain cases?
     */
    public CStyxFileOutputStream(CStyxFile file, boolean closeConnectionWhenCloseStream) throws StyxException
    {
        if (file == null)
        {
            throw new NullPointerException("file cannot be null");
        }
        this.file = file;
        this.file.openOrCreate(false, StyxUtils.OWRITE | StyxUtils.OTRUNC);
        this.buf = new byte[(int)this.file.getIoUnit()];
        this.pos = 0;
        this.offset = 0;
        this.closeConnectionWhenCloseStream = closeConnectionWhenCloseStream;
        log.debug("Created CStyxFileOutputStream for file " + file.getPath());
    }
    
    /**
     * Creates a new CStyxFileOutputStream to write data to the given CStyxFile.
     * If the file already exists it will be overwritten.
     * @todo Add flag to prevent overwriting in certain cases?
     */
    public CStyxFileOutputStream(CStyxFile file) throws StyxException
    {
        this(file, false);
    }
    
    /**
     * Writes the specified byte to the Styx file.  Must call flush() to
     * guarantee that the byte is actually written, as it may be held in a 
     * buffer.
     */
    public synchronized void write(int b) throws IOException
    {
        // Put the byte in the output buffer
        this.buf[this.pos] = (byte)b;
        this.pos++;
        // If the buffer is full, flush it (i.e. write the data to the output file)
        if (this.pos >= this.buf.length)
        {
            this.flush();
        }
    }
    
    /**
     * Flushes the internal buffer and forces any buffered output bytes to be
     * written
     */
    public synchronized void flush() throws IOException
    {
        // Write the contents of the buffer to the file
        try
        {
            log.debug("writing " + pos + " bytes at offset " + this.offset);
            this.file.write(this.buf, 0, pos, this.offset, true);
            // Update the offset of the file
            this.offset += pos;
            // Reset the pointer position
            this.pos = 0;
        }
        catch(StyxException se)
        {
            if (log.isDebugEnabled())
            {
                se.printStackTrace();
            }
            throw new IOException(se.getMessage());
        }
    }
    
    /**
     * Closes the file stream (i.e. clunks the fid of the underlying file) and
     * flushes any remaining data to the file.
     */
    public synchronized void close() throws IOException
    {
        // Write all remaining bytes in the buffer
        this.flush();
        
        // Write an empty message to signify end-of-file
        this.flush();
        
        this.file.close();
        if (this.closeConnectionWhenCloseStream)
        {
            this.file.getConnection().close();
        }
    }
}
