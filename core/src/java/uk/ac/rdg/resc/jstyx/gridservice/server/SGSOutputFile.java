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

import java.util.Vector;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;
import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileChangeListener;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.server.StyxServerProtocolHandler;
import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.messages.RerrorMessage;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * A file that can be read to obtain data from a file that is put out by an
 * executable in a Styx Grid Service
 * @todo this repeats code in FileOnDisk and GeneralCachingStreamReader.  Can
 * we refactor?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/11/11 21:57:21  jonblower
 * Implemented passing of URLs to input files
 *
 * Revision 1.1  2005/11/10 19:50:43  jonblower
 * Added code to handle output files
 *
 */

public class SGSOutputFile extends StyxFile implements SGSInstanceChangeListener
{
    private static final Logger log = Logger.getLogger(SGSOutputFile.class);
    
    private File file; // The underlying file that is produced by the executable
    private SGSParamFile paramFile;  // The parameter that is linked to this output file
    private Vector requestQueue; // Queue of DataRequests that have not yet been fulfilled
    private boolean serviceFinished;
    
    /** Creates a new instance of SGSOutputFile */
    public SGSOutputFile(File file, StyxGridServiceInstance instance)
        throws StyxException
    {
        super(file.getName(), 0444); // File is read-only
        this.file = file;
        instance.addChangeListener(this);
        this.serviceFinished = false;
        this.requestQueue = new Vector();
        new FileMonitor().start();
    }
    
    public synchronized void read(StyxFileClient client, long offset, int count,
        int tag) throws StyxException
    {
        DataRequest dr = new DataRequest(client, offset, count, tag);
        if (!processRequest(dr))
        {
            // Add the request to the queue
            synchronized(this.requestQueue)
            {
                this.requestQueue.add(dr);
            }
        }
    }
    
    /**
     * @return true if the request was processed successfully, false otherwise
     */
    private synchronized boolean processRequest(DataRequest dr)
    {
        if (dr.offset < this.getLength().asLong())
        {
            // There are data available and we can return them to the client
            this.readAndReply(dr);
            return true;
        }
        else if (this.serviceFinished)
        {
            // We're not going to get any more data from this file.  Reply with
            // zero bytes to signify EOF
            this.replyRead(dr.client, new byte[0], dr.tag);
            return true;
        }
        else
        {
            // Keep this request in the queue
            return false;
        }        
    }
    
    /**
     * Processes all outstanding requests. This is called when more data arrive
     * or when EOF is reached.
     */
    private void processOutstandingRequests()
    {
        // We should already have the lock on the cache, but let's make sure
        synchronized(this.requestQueue)
        {
            for (int i = 0; i < this.requestQueue.size(); )
            {
                DataRequest dr = (DataRequest)this.requestQueue.get(i);
                boolean processed = this.processRequest(dr);
                if (processed)
                {
                    // If we have processed the request successfully, remove
                    // it from the queue
                    this.requestQueue.remove(i);
                }
                else
                {
                    i++;
                }
            }
        }
    }
    
    /** 
     * Reads from the underlying file and replies to the client. Before this
     * method is called, we must check to see if dr.offset is less than the 
     * length of the file
     */
    private synchronized void readAndReply(DataRequest dr)
    {
        try
        {
            // Open a new FileChannel for reading
            FileChannel chan = new FileInputStream(this.file).getChannel();

            // Get a ByteBuffer from MINA's pool.  This becomes part of the Rread
            // message and is automatically released when the message is sent
            ByteBuffer buf = ByteBuffer.allocate(dr.count);
            // Make sure the position and limit are set correctly (remember that
            // the actual buffer size might be larger than requested)
            buf.position(0).limit(dr.count);

            // Read from the channel. If no bytes were read (due to EOF), the
            // position of the buffer will not have changed
            int numRead = chan.read(buf.buf(), dr.offset);
            log.debug("Read " + numRead + " bytes from " + this.file.getPath());
            // Close the channel
            chan.close();

            buf.flip();
            if (numRead > 0)
            {
                this.replyRead(dr.client, buf, dr.tag);
            }
            else
            {
                RerrorMessage rErrMsg = new RerrorMessage("Internal error: " +
                    "zero bytes read from " + this.file.getName());
                StyxServerProtocolHandler.reply(dr.client.getSession(),
                    rErrMsg, dr.tag);
            }
        }
        catch(FileNotFoundException fnfe)
        {
            RerrorMessage rErrMsg = new RerrorMessage("Internal error: file " +
               this.file.getName() + " does not exist");
            StyxServerProtocolHandler.reply(dr.client.getSession(),
                rErrMsg, dr.tag);
        }
        catch(IOException ioe)
        {
            RerrorMessage rErrMsg = new RerrorMessage("An error of class " +
                ioe.getClass() + " occurred when trying to read from " +
                this.getFullPath() + ": " + ioe.getMessage());
            StyxServerProtocolHandler.reply(dr.client.getSession(),
                rErrMsg, dr.tag);
        }
    }
    
    public ULong getLength()
    {
        return new ULong(this.file.length());
    }
    
    /**
     * Called automatically when the status of the Styx Grid Service instance
     * changes.
     */
    public void statusChanged(StatusCode newStatus)
    {
        log.debug("Got newStatus = " + newStatus.getText());
        if (newStatus == StatusCode.FINISHED ||
            newStatus == StatusCode.ABORTED ||
            newStatus == StatusCode.ERROR)
        {
            this.serviceFinished = true;
        }
        this.processOutstandingRequests();
    }
    
    /** 
     * Simple thread that monitors the state of the underlying file.  When the
     * state changes, sends a signal to process outstanding requests
     */
    private class FileMonitor extends Thread
    {
        private long length = 0L;
        
        public void run()
        {
            while(!serviceFinished)
            {
                long newLength = file.length();
                if (newLength != this.length)
                {
                    this.length = newLength;
                    processOutstandingRequests();
                }
                // Wait for 2 seconds
                try
                {
                    Thread.sleep(2000);
                }
                catch(InterruptedException ie)
                {
                    // do nothing
                }
            }
        }
    }
    
    /**
     * Class representing a request for data
     */
    private static class DataRequest
    {
        private StyxFileClient client;
        private long offset; // The offset requested by the client
        private int count; // The number of bytes requested by the client
        private int tag;
        
        private DataRequest(StyxFileClient client, long offset, int count, int tag)
        {
            this.client = client;
            this.offset = offset;
            this.count = count;
            this.tag = tag;
        }
    }
    
}
