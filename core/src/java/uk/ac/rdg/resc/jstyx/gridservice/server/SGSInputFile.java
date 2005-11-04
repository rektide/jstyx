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

package uk.ac.rdg.resc.jstyx.gridservice.server;

import java.io.OutputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;

/**
 * A file that is used to provide input (as a file or as stdin stream) to a 
 * Styx Grid Service.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/11/04 19:29:53  jonblower
 * Moved code that writes to std input to SGSInputFile
 *
 * Revision 1.1  2005/11/04 09:12:23  jonblower
 * Initial import
 *
 */
public abstract class SGSInputFile extends StyxFile
{
    
    protected InputURLFile urlFile;
    protected StyxGridServiceInstance instance;
    
    private SGSInputFile(String name, InputURLFile urlFile, StyxGridServiceInstance instance)
        throws StyxException
    {
        super(name, 0222); // Input files are always write-only
        this.urlFile = urlFile;
        this.instance = instance;
    }
    
    /**
     * file through which clients can write to the process's input stream directly
     */
    public static class StdinFile extends SGSInputFile
    {
        private OutputStream stream = null;
        private long bytesCons;

        /**
         * Creates new StdinFile - will be called "stdin"
         */
        public StdinFile(InputURLFile urlFile, StyxGridServiceInstance instance)
            throws StyxException
        {
            super("stdin", urlFile, instance);
        }

        public void setOutputStream(OutputStream os)
        {
            this.stream = os;
            this.bytesCons = 0;
        }
        
        /**
         * Writes the given number of bytes to the stream. The offset
         * is ignored; it will always writes to the current stream position, so
         * the behaviour of this method when multiple clients are connected is
         * undefined
         * @todo deal with request to flush the write message
         */
        public void write(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException
        {
            if (instance.getStatus() != StatusCode.RUNNING)
            {
                // Can't write to this file until the service is running
                // TODO: should we allow this but cache the input?
                throw new StyxException("Cannot write to the standard input" +
                    " when the service is not running");
            }
            else if (this.getInputURL() != null)
            {
                // We're not allowed to write to this file if the service is running
                // and the input URL is set
                throw new StyxException("Cannot write to the input stream " +
                    "because the service is reading from " + this.getInputURL());
            }
            else
            {
                // The service is running
                try
                {
                    if (count == 0)
                    {
                        stream.close();
                    }
                    int bytesToWrite = data.remaining();
                    if (count < data.remaining())
                    {
                        // Would normally be an error if count != data.remaining(),
                        // but we'll let the calling application pick this up
                        bytesToWrite = count;
                    }
                    byte[] arr = new byte[bytesToWrite];
                    data.get(arr);
                    stream.write(arr);
                    stream.flush();
                    // Update the number of bytes consumed
                    bytesCons += bytesToWrite;
                    instance.setBytesConsumed(bytesCons);
                    this.replyWrite(client, bytesToWrite, tag);
                }
                catch(IOException ioe)
                {
                    throw new StyxException("IOException occurred when writing to "
                            + "the stream: " + ioe.getMessage());
                }
            }
        }
    }
    
    /**
     * @return the url from which this file will get its data, or null if this
     * has not been set
     */
    public URL getInputURL()
    {
        return this.urlFile.getURL();
    }
    
    public static class File extends SGSInputFile
    {
        private File(String name, InputURLFile urlFile,
            StyxGridServiceInstance instance)
            throws StyxException
        {
            super(name, urlFile, instance);
        }
        
        
    }
    
    /*public static class FileFromParam extends SGSInputFile
    {
        private FileFromParam(String name) throws StyxException
        {
            super(name);
        }
    }*/
    
    
    
}
