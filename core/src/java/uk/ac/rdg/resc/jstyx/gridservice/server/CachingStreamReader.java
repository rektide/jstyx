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
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.server.StyxServerProtocolHandler;
import uk.ac.rdg.resc.jstyx.messages.RerrorMessage;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * When a client gets data from one of the StyxGridService's data streams (e.g.
 * out and err), it does so via an instance of this class.
 * In this class, the entire contents of the stream are written to a file on
 * the local file system (this file is the cache).  Greater performance might be
 * achieved by caching the data in memory (although the underlying OS will have
 * its own caching strategy).  The performance of this class might be adequate
 * if the speed of hard disk access is greater than the speed of network traffic,
 * which is probably a reasonable assumption in most cases.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/03/24 14:47:47  jonblower
 * Provided default read() and write() methods for StyxFile so it is no longer abstract
 *
 * Revision 1.4  2005/03/24 09:48:31  jonblower
 * Changed 'count' from long to int throughout for reading and writing
 *
 * Revision 1.3  2005/03/21 17:59:49  jonblower
 * Fixed bug with size of RandomAccessFile
 *
 * Revision 1.2  2005/03/18 13:56:00  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.3  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.2  2005/03/11 09:07:15  jonblower
 * Changes related to switch from Netty to MINA
 *
 * Revision 1.1  2005/02/16 19:22:31  jonblower
 * Commit adding of SGS files to CVS
 *
 */
class CachingStreamReader extends StyxFile
{
    private static final Logger log = Logger.getLogger(CachingStreamReader.class);
    
    private StyxGridServiceInstance sgsInstance; // The SGS instance that owns this
    private InputStream is; // The input stream from which we will read
    private RandomAccessFile cache; // The cache itself (a file on the local filesystem)
    private String cacheFilePath; // The full path to the cache file
    private boolean started;  // True if we have started reading from the stream
    private RerrorMessage rGlobErrMsg; // If this is non-null an error has occurred and 
                                   // any attempt to read the stream will result
                                   // in this message being returned until it is reset.
    private Vector requestQueue; // Queue of DataRequests that have not yet been fulfilled
    private long cacheLength; // The length of the cache (i.e. the size of the cache file)
                              // Note that we could also get this from cache.length() but
                              // this should be faster (saves a call to the OS)
    private boolean eof;      // This will be true when the stream has been completely read.
    private boolean running;  // This is true if there is an active thread reading from the input stream
    
    /**
     * Creates a new CachingStreamReader.
     * @param inst The StyxGridServiceInstance that owns this CachingStreamReader
     * @param name The name for this file as it will appear on the Styx server
     * @throws StyxException if the file could not be created.
     */
    public CachingStreamReader(StyxGridServiceInstance inst, String name) throws StyxException
    {
        super(name);
        this.sgsInstance = inst;
        this.is = null;
        this.requestQueue = new Vector();
        this.running = false;
    }
    
    /**
     * Sets the input stream from which the CachingStreamReader gets data
     * and immediately starts reading from this stream
     * @throws IllegalStateException if this object is already reading from a stream
     * or if there are outstanding requests (the latter would be an internal error
     * @throws StyxException if the cache file could not be created, or if previous
     * cache file(s) could not be deleted
     * and should not happen)
     */
    public void startReading(InputStream is) throws StyxException
    {
        if (this.running)
        {
            throw new IllegalStateException("This CachingStreamReader is already running");
        }
        synchronized(this.requestQueue)
        {
            if (this.requestQueue.size() != 0)
            {
                throw new IllegalStateException("Internal error: there are still requests outstanding");
            }
        }
        try
        {
            if (this.cache != null)
            {
                // Delete any previous cache files
                this.cache.close();
                new File(this.cacheFilePath).delete();
            }
            this.cacheFilePath = this.sgsInstance.getWorkingDirectory() + 
                StyxUtils.SYSTEM_FILE_SEPARATOR + name;
            this.cache = new RandomAccessFile(this.cacheFilePath, "rw");
        }
        catch(FileNotFoundException fnfe)
        {
            throw new StyxException("Could not create cache file " + this.cacheFilePath);
        }
        catch(IOException ioe)
        {
            throw new StyxException("Could not delete previous cache file: " +
                ioe.getMessage());
        }
        this.cacheLength = 0;
        this.rGlobErrMsg = null;
        this.is = is;
        // Start reading from the stream immediately
        new StreamGobbler().start();
    }
    
    /**
     * Get data from the stream's cache.
     * @param offset The offset *relative to the start of the whole stream* of
     * the first byte of data requested.
     * @param count The maximum amount of data to return
     */
    public void read(StyxFileClient client, long offset, int count, int tag)
        throws StyxException
    {
        log.debug("Received request: offset = " + offset + ", count = " + count);
        if (this.is == null)
        {
            throw new StyxException("Stream not ready for reading");
        }
        DataRequest dr = new DataRequest(client, tag, offset, count);
        synchronized(this.cache)
        {
            // Try to process the request immediately
            boolean processed = this.processRequest(dr);
            if (!processed)
            {
                // if we couldn't fulfil this request, add it to the queue
                synchronized(this.requestQueue)
                {
                    this.requestQueue.add(dr);
                }
            }
        }
    }
    
    /**
     * Processes all outstanding requests. This is called when more data arrive
     * or when EOF is reached.
     */
    private void processOutstandingRequests()
    {
        // We should already have the lock on the cache, but let's make sure
        synchronized(this.cache)
        {
            synchronized(this.requestQueue)
            {
                Iterator it = this.requestQueue.iterator();
                while(it.hasNext())
                {
                    DataRequest dr = (DataRequest)it.next();
                    boolean processed = this.processRequest(dr);
                    if (processed)
                    {
                        // If we have processed the request successfully, remove
                        // it from the queue
                        it.remove();
                    }
                }
            }
        }
    }
    
    /**
     * @return true if the request was processed successfully, false otherwise
     */
    private boolean processRequest(DataRequest dr)
    {
        // We get the cache's lock so no data can be written to the cache
        // before we're done.
        synchronized(this.cache)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Processing request: offset = " + dr.offset +
                        ", count = " + dr.count + ": cacheLength = " +
                        this.cacheLength + ", actual length = " + this.cache.length());
                }
                if (dr.offset >= this.cacheLength)
                {
                    // We're asking for data not (yet) in the cache
                    if (this.eof)
                    {
                        // We've reached the end of the stream.
                        log.debug("Offset >= cache length and EOF reached."
                            + " Returning 0 bytes");
                        this.replyRead(dr.client, new byte[0], dr.tag);
                        return true;
                    }
                    else
                    {
                        // The requested data haven't yet arrived in the cache
                        if (this.rGlobErrMsg != null)
                        {
                            // An error has occurred so the data will never
                            // arrive.
                            log.debug("Got error message. Returning Rerror");
                            StyxServerProtocolHandler.reply(dr.client.getSession(),
                                rGlobErrMsg, dr.tag);
                            return true;
                        }
                        else
                        {
                            log.debug("Offset >= cache length but EOF not reached."
                                + " Holding request.");
                            return false;
                        }
                    }
                }
                else
                {
                    // At least some of the requested data are in the cache
                    // Seek to the right position in the cache file
                    this.cache.seek(dr.offset);
                    // Try to read the requested amount of data
                    // TODO: check the number of bytes remaining?
                    byte[] arr = new byte[dr.count];
                    log.debug("Reading " + arr.length + " bytes from cache at offset "
                        + dr.offset);
                    int n = this.cache.read(arr);
                    log.debug("Actually read " + n + " bytes");
                    if (n < 0)
                    {
                        // We reached EOF - this shouldn't happen because we've already
                        // checked the file length
                        log.error("Internal error: no bytes read from stream");
                        return false;
                    }
                    else
                    {
                        // Return the bytes read to the client
                        log.debug("Returning " + n + " bytes to client");
                        this.replyRead(dr.client, arr, 0, n, dr.tag);
                        return true;
                    }
                }
            }
            catch(IOException ioe)
            {
                if (log.isDebugEnabled())
                {
                    ioe.printStackTrace();
                }
                // We have to reply with an error message here (as opposed to
                // throwing a StyxException) because we're no longer within
                // the readFile() method
                RerrorMessage rErrMsg = new RerrorMessage("error reading from stream: " +
                    ioe.getMessage());
                StyxServerProtocolHandler.reply(dr.client.getSession(), rErrMsg, dr.tag);
                return true; // We have processed the message
            }
        }
    }
    
    /**
     * This thread continuously reads from the stream and puts data in the cache,
     * notifying waiting clients of the new data.
     */
    private class StreamGobbler extends Thread
    {
        public void run()
        {
            running = true;
            eof = false;
            // Create a temporary 8K byte buffer
            // TODO: is this size appropriate? Might be best to set it to the
            // maximum payload size on the connection. Or, for services that output
            // very little data, a much smaller size may be appropriate so that
            // clients aren't kept waiting for long between updates
            byte[] arr = new byte[8192];
            try
            {
                while(!eof)
                {
                    int n = is.read(arr);
                    log.debug("Read " + n + " bytes from input stream");
                    synchronized(cache)
                    {
                        if (n < 0)
                        {
                            // We've reached the end of the stream
                            eof = true;
                        }
                        else
                        {
                            // put the newly-read bytes to the end of the cache
                            log.debug("Seeking to " + cacheLength);
                            cache.seek(cacheLength);
                            log.debug("Writing " + n + " bytes to cache file");
                            cache.write(arr, 0, n);
                            cacheLength += n;
                            log.debug("New cache length = " + cacheLength);
                            // For some reason, the length() of the RandomAccessFile
                            // isn't always correct (particularly when first created)
                            // so we have to set it explicitly.
                            // TODO: could we just set this to zero when the
                            // RAF is first created?  Will we notice?
                            cache.setLength(cacheLength);
                        }
                        // Now process any outstanding requests
                        processOutstandingRequests();
                    }
                }
            }
            catch(Exception e)
            {
                if (log.isDebugEnabled())
                {
                    e.printStackTrace();
                }
                // Set the global exception object so that any further
                // attemps to read this stream return an error.
                rGlobErrMsg = new RerrorMessage("An internal error occurred reading from the stream: "
                    + e.getClass() + ", " + e.getMessage());
            }
            // now we close the cache and open it read-only; this allows us to
            // examine the contents of the cache without going through the 
            // Styx interface (useful for debugging)
            synchronized(cache)
            {
                try
                {
                    cache.close();
                    cache = new RandomAccessFile(cacheFilePath, "r");
                }
                catch(Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        e.printStackTrace();
                    }
                    rGlobErrMsg = new RerrorMessage("An internal error occurred "
                        + "closing and reopening the cache file: " + e.getClass()
                        + ", " + e.getMessage());
                }
            }
            running = false;
        }
    }
    
    /**
     * Class representing a client that is waiting for data
     * @todo: this is very similar to the ClientInfo class in AsyncStyxFile.
     * Can we avoid this code repetition?
     */
    private class DataRequest
    {
        private StyxFileClient client;
        private int tag;
        private long offset; // The offset requested by the client
        private int count; // number of bytes requested by the client
        
        private DataRequest(StyxFileClient client, int tag, long offset, int count)
        {
            this.client = client;
            this.tag = tag;
            this.offset = offset;
            this.count = count;
        }
    }
    
    /**
     * Called when the file is removed from the server. This deletes the
     * underlying cache file.
     */
    protected synchronized void delete()
    {
        if (this.cache != null)
        {
            try
            {
                this.cache.close();
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
            new File(this.cacheFilePath).delete();
        }
        return;
    }
    
}
