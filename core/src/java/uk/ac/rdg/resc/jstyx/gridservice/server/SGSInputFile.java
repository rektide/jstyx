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
import java.net.MalformedURLException;

import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.types.ULong;
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
 * Revision 1.5  2005/12/09 18:42:41  jonblower
 * Changed access privileges to write2() to protected
 *
 * Revision 1.4  2005/11/11 21:57:21  jonblower
 * Implemented passing of URLs to input files
 *
 * Revision 1.3  2005/11/09 17:49:58  jonblower
 * Implemented File subclass (representing fixed-name input files)
 *
 * Revision 1.2  2005/11/04 19:29:53  jonblower
 * Moved code that writes to std input to SGSInputFile
 *
 * Revision 1.1  2005/11/04 09:12:23  jonblower
 * Initial import
 *
 */
public abstract class SGSInputFile extends StyxFile
{
    
    protected StyxGridServiceInstance instance;
    protected boolean dataWritten;  // True when any data have been written to this file
                                    // (don't necessarily have to have reached EOF)
    
    protected ByteBuffer candidateURLBuffer; // This is set when we have received a message that
                                // seems to contain a valid URL
    protected String candidateURL; // This is set when we have received a message that
                                // seems to contain a valid URL
    protected int candidateURLLength;
    protected URL url;  // This is non-null if we have specified that this
                        // file will be read from a URL
    
    private SGSInputFile(String name, StyxGridServiceInstance instance)
        throws StyxException
    {
        super(name, 0222); // Input files are always write-only
        this.instance = instance;
        this.dataWritten = false;
        this.candidateURL = null;
        this.candidateURLLength = 0;
        this.url = null;
    }
    
    /**
     * This method does the job of checking for URLs being written to the file.
     */
    protected void write2(StyxFileClient client, long offset, int count,
        ByteBuffer data, boolean truncate, int tag)
        throws StyxException
    {
        if (this.url != null)
        {
            throw new StyxException("Cannot write to this stream: it is" +
                " reading from " + this.url);
        }
        try
        {
            // First check to see if this is an EOF message
            if (count == 0)
            {
                if (this.candidateURLBuffer != null)
                {
                    if (offset == this.candidateURLLength)
                    {
                        // We're writing EOF at the end of an existing URL
                        // See if this URL is recognised
                        try
                        {
                            URL url = new URL(this.candidateURL);
                            this.setURL(url);
                            this.replyWrite(client, count, tag);
                            return;
                        }
                        catch (MalformedURLException mue)
                        {
                            throw new StyxException(this.candidateURL +
                                " is not recognised as a valid URL");
                        }
                    }
                    else
                    {
                        // We're writing EOF elsewhere in the file.  Write
                        // the "candidate URL" to the file as it is obviously
                        // not really a URL
                        this.writeData(client, offset, count, this.candidateURLBuffer,
                            truncate, tag);
                        this.candidateURLBuffer.release();
                        this.candidateURL = null;
                        this.closeOutput();
                        this.replyWrite(client, count, tag);
                        return;
                    }
                }
                else
                {
                    // We have reached EOF and we have no stored URL to write.
                    // Just write the empty buffer to signify EOF
                    this.writeData(client, offset, count, data, truncate, tag);
                    this.closeOutput();
                    this.replyWrite(client, count, tag);
                    return;
                }
            }
            else
            {
                if (offset == 0 && parseURL(data))
                {
                    // Don't write any data to the stream.  We've stored
                    // the candidate URL for future use
                    this.replyWrite(client, count, tag);
                    return;
                }
                int bytesToWrite = data.remaining();
                if (count < data.remaining())
                {
                    // Would normally be an error if count != data.remaining(),
                    // but we'll let the calling application pick this up
                    bytesToWrite = count;
                }
                this.writeData(client, offset, count, data, truncate, tag);
                this.replyWrite(client, bytesToWrite, tag);
                return;
            }
        }
        catch(IOException ioe)
        {
            throw new StyxException("IOException occurred when writing to "
                    + "the stream: " + ioe.getMessage());
        }
    }
            
    /**
     * Attempts to retrieve a URL from the given data. The data buffer must
     * contain the string "readfrom:<url>" where <url> is a valid URL.
     * @return True if we might have a url: this url will be stored in
     * this.candidateURL as a string, and this.candidateURLLength will store
     * the length of the incoming data buffer
     */
    private boolean parseURL(ByteBuffer data)
    {
        int dataLen = data.remaining();
        // Check to see if this could be a URL.  The dataToString() method does
        // not change the position or limit of the buffer
        String urlStr = StyxUtils.dataToString(data);
        final String prefix = "readfrom:";
        if (urlStr.startsWith(prefix))
        {
            // Store this data buffer as we might want to use it later
            data.acquire();
            this.candidateURLBuffer = data;
            this.candidateURL = urlStr.substring(prefix.length());
            this.candidateURLLength = dataLen;
            System.out.println("*** Got candidate URL: " + this.candidateURL + " ***");
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public void setURL(URL url) throws StyxException
    {
        this.url = url;
    }
    
    /**
     * Writes data to the underlying file or stream
     */
    protected abstract void writeData(StyxFileClient client, long offset, int count,
        ByteBuffer data, boolean truncate, int tag)
        throws StyxException, IOException;
    
    /**
     * Closes the output file or stream. This default implementation does nothing
     */
    protected abstract void closeOutput() throws IOException;
    
    /**
     * file through which clients can write to the process's input stream directly
     */
    public static class StdinFile extends SGSInputFile
    {
        private OutputStream stream = null;

        /**
         * Creates new StdinFile - will be called "stdin"
         */
        public StdinFile(StyxGridServiceInstance instance)
            throws StyxException
        {
            super("stdin", instance);
        }

        public void setOutputStream(OutputStream os)
        {
            this.stream = os;
        }
        
        /**
         * This just writes the data to the current position in the stream
         * and flushes the write.  The offset is ignored.  The "bytesConsumed"
         * service data element is updated
         */
        protected void writeData(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException, IOException
        {
            byte[] arr = new byte[data.remaining()];
            data.get(arr);
            this.stream.write(arr);
            this.stream.flush();
            // Update the number of bytes consumed
            this.instance.setBytesConsumed(offset + arr.length);
        }
        
        public void write(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException
        {
            if (instance.getStatus() != StatusCode.RUNNING)
            {
                throw new StyxException("Can't write data to standard input" +
                    " before the service is running");
            }
            else
            {
                this.write2(client, offset, count, data, truncate, tag);
            }
        }
        
        protected void closeOutput() throws IOException
        {
            this.stream.close();
        }
    
        public void setURL(URL url) throws StyxException
        {
            super.setURL(url);
            this.instance.readFrom(this.url, this.stream);
        }
    }
    
    public static class File extends SGSInputFile
    {
        private FileChannel chan;
        private java.io.File file;
        private boolean eofWritten;
        
        public File(java.io.File file, StyxGridServiceInstance instance)
            throws StyxException
        {
            super(file.getName(), instance);
            this.file = file;
            this.chan = null; // We create this when we get our first write message
            this.eofWritten = false;
        }
        
        protected void closeOutput() throws IOException
        {
            if (this.chan != null)
            {
                this.chan.close();
            }
        }
        
        /**
         * This just writes the data to the underlying fileOnDisk
         */
        protected void writeData(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException, IOException
        {
            if (count == 0)
            {
                this.eofWritten = true;
            }
            else
            {
                if (this.chan == null)
                {
                    try
                    {
                        this.chan = new RandomAccessFile(this.file, "rw").getChannel();
                    }
                    catch (IOException ioe)
                    {
                        throw new StyxException("Error creating file " +
                            this.file.getName() + ": " + ioe.getMessage());
                    }
                }
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
                    chan.truncate(offset + nWritten);
                }
                this.dataWritten = true;
                this.eofWritten = false;
            }
        }
        
        public void write(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException
        {
            // We're not allowed to write to this file if the service is running
            // or the input URL is set
            if (instance.getStatus() == StatusCode.RUNNING)
            {
                throw new StyxException("Cannot write to an input file while " +
                    "the service is running");
            }
            else
            {
                this.write2(client, offset, count, data, truncate, tag);
            }
        }
        
        /**
         * @return true if we have received an EOF message (an empty write message)
         */
        public boolean dataUploadComplete()
        {
            return this.eofWritten = true;
        }
        
        /**
         * @return the length of the underlying file on disk
         */
        public ULong getLength()
        {
            return new ULong(this.file.length());
        }
    }
    
    /**
     * @return the URL from which this file will get its data, or null if the 
     * URL has not been set
     */
    public URL getURL()
    {
        return this.url;
    }
    
    
    
}
