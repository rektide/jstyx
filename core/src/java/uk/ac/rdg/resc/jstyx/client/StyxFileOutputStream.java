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

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Output stream for writing data to a Styx File
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/03/16 17:55:53  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.1.1.1  2005/02/16 18:58:19  jonblower
 * Initial import
 *
 */
public class StyxFileOutputStream extends OutputStream
{
    
    private CStyxFile file; // The file to which we are writing
    private byte[] buf;     // Buffer for storing the results of the last write
    private int pos;        // Current position in the buffer
    private long offset;    // The current position in the file
    
    /**
     * Creates a new StyxFileOutputStream to write data to the given CStyxFile.
     * This also opens the file for writing, throwing a StyxException if the file
     * could not be opened.
     */
    public StyxFileOutputStream(CStyxFile file) throws StyxException
    {
        this.file = file;
        this.file.open(StyxUtils.OWRITE);
        this.buf = new byte[(int)this.file.getIOUnit()];
        this.pos = 0;
        this.offset = 0;
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
            this.file.write(this.buf, this.offset);
        }
        catch(StyxException se)
        {
            throw new IOException(se.getMessage());
        }
        // Reset the pointer position
        this.pos = 0;
    }
    
    /**
     * Closes the file stream (i.e. clunks the fid of the underlying file)
     */
    public synchronized void close() throws IOException
    {
        try
        {
            this.file.close();
        }
        catch (StyxException se)
        {
            throw new IOException (se.getMessage());
        }
    }
}
