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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import java.util.Vector;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Abstract class that reads from an InputStream in a separate thread, caching
 * the results to the local hard disk.  Permits multiple clients to make
 * DataRequests for data, replying with chunks of data when they are ready.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/06/10 07:54:49  jonblower
 * Added code to convert event-based StreamViewer to InputStream-based one
 *
 * Revision 1.2  2005/05/27 21:22:39  jonblower
 * Further development of caching stream readers
 *
 * Revision 1.1  2005/05/27 17:02:59  jonblower
 * Initial import
 *
 */
public abstract class GeneralCachingStreamReader
{
    private static final Logger log = Logger.getLogger(GeneralCachingStreamReader.class);
    
    private InputStream is; // The input stream from which we will read
    private RandomAccessFile cache; // The cache itself (a file on the local filesystem)
    private Integer cacheLock = new Integer(0); // We use this for synchronization; it has no other purpose
    private File cacheFile;   // The cache file
    private boolean started;  // True if we have started reading from the stream
    private Exception globEx; // If this is non-null an error has occurred and 
                              // any attempt to read the stream will result
                              // in this message being returned until it is reset.
    private Vector requestQueue; // Queue of DataRequests that have not yet been fulfilled
    private long cacheLength; // The length of the cache (i.e. the size of the cache file)
                              // Note that we could also get this from cache.length() but
                              // this should be faster (saves a call to the OS)
    private boolean eof;      // This will be true when the stream has been completely read.
    private boolean running;  // This is true if there is an active thread reading from the input stream
    
    public GeneralCachingStreamReader()
    {
        this.is = null;
        this.requestQueue = new Vector();
        this.running = false;
    }
    
    /**
     * Gets the File that is being used to cache the contents of the stream
     */
    public File getCacheFile()
    {
        return this.cacheFile;
    }
    
    /**
     * Gets the size of the cache in bytes
     */
    public long getCacheLength()
    {
        return this.cacheLength;
    }
    
    public void setCacheFile(File cacheFile) throws FileNotFoundException
    {
        this.cacheFile = cacheFile;
    }
    
    /**
     * @return true if we have reached end of file
     */
    public boolean isEOF()
    {
        return this.eof;
    }
    
    /**
     * Sets the input stream from which the CachingStreamReader gets data
     * and immediately starts reading from this stream
     * @throws IllegalStateException if this object is already reading from a stream
     * @throws IOException if the cache file could not be created
     */
    public void startReading(InputStream is) throws IOException
    {
        if (this.running)
        {
            throw new IllegalStateException("This CachingStreamReader is already running");
        }
        
        if (this.cache != null)
        {
            // Delete any previous cache files
            this.cache.close();
            this.cacheFile.delete();
        }
        
        this.cache = new RandomAccessFile(this.cacheFile, "rw");
        log.info("Created cache file " + this.cacheFile.getPath());
        
        this.cacheLength = 0;
        this.globEx = null;
        this.is = is;
        // Start reading from the stream immediately
        new StreamGobbler().start();
    }
    
    /**
     * Get data from the stream's cache.
     */
    public void read(DataRequest dr)
    {
        log.debug("Received request: offset = " + dr.offset + ", count = " + dr.count);
        synchronized(this.cacheLock)
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
        synchronized(this.cacheLock)
        {
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
    }
    
    /**
     * @return true if the request was processed successfully, false otherwise
     */
    private boolean processRequest(DataRequest dr)
    {
        // We get the cache's lock so no data can be written to the cache
        // before we're done.
        synchronized(this.cacheLock)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Processing request: offset = " + dr.offset +
                        ", count = " + dr.count + ": cacheLength = " +
                        this.cacheLength);
                }
                if (dr.offset >= this.cacheLength)
                {
                    // We're asking for data not (yet) in the cache
                    if (this.eof)
                    {
                        // We've reached the end of the stream.
                        log.debug("Offset >= cache length and EOF reached."
                            + " Returning 0 bytes");
                        this.newData(dr, new byte[0], 0, 0);
                        return true;
                    }
                    else
                    {
                        // The requested data haven't yet arrived in the cache
                        if (this.globEx != null)
                        {
                            // An error has occurred so the data will never
                            // arrive.
                            log.debug("Got error message. Returning Rerror");
                            this.error(dr, this.globEx);
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
                        this.newData(dr, arr, 0, n);
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
                this.error(dr, ioe);
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
                    synchronized(cacheLock)
                    {
                        if (n < 0)
                        {
                            // We've reached the end of the stream
                            eof = true;
                            is.close();
                        }
                        else
                        {
                            // put the newly-read bytes to the end of the cache
                            cache.seek(cacheLength);
                            cache.write(arr, 0, n);
                            cacheLength += n;
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
                globEx = e;
            }
            // now we close the cache and open it read-only; this allows us to
            // examine the contents of the cache without going through the 
            // Styx interface (useful for debugging)
            synchronized(cache)
            {
                try
                {
                    cache.close();
                    cache = new RandomAccessFile(cacheFile, "r");
                }
                catch(Exception e)
                {
                    if (log.isDebugEnabled())
                    {
                        e.printStackTrace();
                    }
                    // Set the global error message
                    globEx = e;
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
    public static class DataRequest
    {
        // TODO create get() methods
        public Object client;
        public long offset; // The offset requested by the client
        public int count; // number of bytes requested by the client
        
        public DataRequest(Object client, long offset, int count)
        {
            this.client = client;
            this.offset = offset;
            this.count = count;
        }
    }
    
    public abstract void newData(DataRequest originalRequest, byte[] data,
        int offset, int count);
    
    public abstract void error(DataRequest originalRequest, Exception e);
    
    /**
     * Called when the file is removed from the server. This deletes the
     * underlying cache file.
     */
    public synchronized void delete()
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
            this.cacheFile.delete();
        }
        return;
    }
    
}
