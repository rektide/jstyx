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

import java.io.InputStream;
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

import org.apache.log4j.Logger;

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
    
    private static final Logger log = Logger.getLogger(SGSInputFile.class);
    
    protected AbstractJob job;
    protected boolean dataWritten;  // True when any data have been written to this file
                                    // (don't necessarily have to have reached EOF)
    
    protected ByteBuffer candidateURLBuffer; // This is set when we have received a message that
                                // seems to contain a valid URL
    protected URL candidateURL; // This is set when we have received a message that
                                // seems to contain a valid URL
    protected int candidateURLLength;
    protected URL url;  // The URL that corresponds to this.urlInputStream
    
    private SGSInputFile(String name, AbstractJob job)
        throws StyxException
    {
        super(name, 0222); // Input files are always write-only
        this.job = job; // The job to which this input file belongs
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
        if (this.url != null && count > 0)
        {
            throw new StyxException("Cannot write to this stream: it is" +
                " reading from " + this.url);
        }
        try
        {
            // First check to see if this is an EOF message
            if (count == 0)
            {
                if (this.candidateURLBuffer == null)
                {
                    // We have reached EOF and we have no stored URL to write.
                    // Just write the empty buffer to signify EOF
                    this.writeData(client, offset, count, data, truncate);
                    this.closeOutput();
                    this.replyWrite(client, count, tag);
                    return;
                }
                else if (offset == this.candidateURLLength)
                {
                    // We're writing EOF at the end of an existing URL, thereby
                    // confirming that the client wishes to download the data
                    // from this URL
                    this.setURL(this.candidateURL);
                    this.replyWrite(client, count, tag);
                    return;
                }
                else
                {
                    // We have a candidate URL but we are writing EOF elsewhere.
                    // This should not happen
                    throw new StyxException("Writing EOF at offset "
                        + offset + " but we have a candidate URL");
                }
            }
            else
            {
                // count > 0 - we are writing actual data
                // First see if we are writing a URL to the beginning of the file
                if (offset == 0 && parseURL(data))
                {
                    // Don't write any data to the stream.  We've stored
                    // the candidate URL for future use
                    this.replyWrite(client, count, tag);
                    return;
                }
                else
                {
                    // Either this is not a valid URL or it is not being written
                    // to the beginning of the file
                    if (this.candidateURLBuffer != null)
                    {
                        // If we have a candidate URL stored we must write it to
                        // the file
                        log.debug("Writing candidate URL to input file");
                        this.writeData(client, 0, this.candidateURLLength,
                            this.candidateURLBuffer, truncate);
                        this.candidateURLBuffer.release();
                        this.candidateURLBuffer = null;
                        this.candidateURL = null;
                    }
                    // Now we can write the data in this message
                    int bytesToWrite = data.remaining();
                    if (count < data.remaining())
                    {
                        // Would normally be an error if count != data.remaining(),
                        // but we'll let the calling application pick this up
                        bytesToWrite = count;
                    }
                    this.writeData(client, offset, count, data, truncate);
                    this.replyWrite(client, bytesToWrite, tag);
                    return;
                }
            }
        }
        catch(IOException ioe)
        {
            throw new StyxException("IOException occurred: " + ioe.getMessage());
        }
    }
            
    /**
     * Attempts to retrieve a URL from the given data. The data buffer must
     * contain the string "readfrom:<url>" where <url> is a valid URL.
     * @return True if we might have a url: this url will be stored in
     * this.url as a string, and this.candidateURLLength will store
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
            try
            {
                this.candidateURL = new URL(urlStr.substring(prefix.length()));
                log.debug("Got candidate URL: " + this.candidateURL);
                // Store this data buffer as we might want to use it later
                data.acquire();
                this.candidateURLBuffer = data;
                this.candidateURLLength = dataLen;
                return true;
            }
            catch (MalformedURLException mue)
            {
                log.debug("Data started with \"readfrom:\" but was not a valid URL");
                return false;
            }
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Sets the URL from which this file will read its data.
     */
    protected void setURL(URL url) throws StyxException
    {
        log.debug("Setting url = " + url);
        this.url = url;
    }
    
    /**
     * Writes data to the underlying file or stream
     */
    protected abstract void writeData(StyxFileClient client, long offset, int count,
        ByteBuffer data, boolean truncate)
        throws StyxException, IOException;
    
    /**
     * Closes the output file or stream.
     */
    protected abstract void closeOutput() throws IOException;
    
    /**
     * file through which clients can write to the process's input stream directly
     */
    public static class StdinFile extends SGSInputFile
    {
        /**
         * Creates new StdinFile - will be called "stdin"
         */
        public StdinFile(AbstractJob job) throws StyxException
        {
            super("stdin", job);
        }
        
        /**
         * This is called from super.write2(), i.e. after we have checked to see
         * if a URL has been written. This just writes the data to the stream
         * and flushes the write.  The offset is ignored.
         */
        protected void writeData(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate)
            throws StyxException, IOException
        {
            if (count == 0)
            {
                // We have an EOF message
                this.job.stdinDataDownloaded();
            }
            else if (this.job.getStdinStream() == null)
            {
                throw new StyxException("The standard input stream of the service" +
                    " is not ready yet");
            }
            else
            {
                log.debug("Writing " + data.remaining() +
                    " bytes to standard input stream");
                byte[] arr = new byte[data.remaining()];
                data.get(arr);
                this.job.getStdinStream().write(arr);
                this.job.getStdinStream().flush();
            }
        }
        
        public void write(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException
        {
            this.write2(client, offset, count, data, truncate, tag);
        }
        
        /**
         * This is called when we have got a URL for this input file and the
         * InputStream to this URL has been opened.  This can happen before or
         * after the job starts.
         */
        protected void setURL(URL url) throws StyxException
        {
            super.setURL(url);
            job.setStdinURL(url);
        }
        
        protected void closeOutput() throws IOException
        {
            this.job.getStdinStream().close();
        }
    }
    
    public static class File extends SGSInputFile
    {
        private FileChannel chan;
        private java.io.File file;
        private boolean eofWritten;
        
        public File(java.io.File file, AbstractJob job)
            throws StyxException
        {
            super(file.getName(), job);
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
         * This is called from super.write2(), i.e. after we have checked to see
         * if a URL has been written. This just writes the data to the given
         * offset in the underlying file on the disk.
         */
        protected void writeData(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate)
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
            if (job.getStatusCode() == StatusCode.RUNNING)
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
     * @return the URL from which the file will get its data, or null if this
     * has not been set.  The corresponding InputStream can be obtained through
     * this.getURLInputStream()
     */
    public URL getURL()
    {
        return this.url;
    }
}
