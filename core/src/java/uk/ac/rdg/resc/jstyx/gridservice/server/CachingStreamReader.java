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
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.server.StyxServerProtocolHandler;
import uk.ac.rdg.resc.jstyx.messages.RerrorMessage;
import uk.ac.rdg.resc.jstyx.StyxException;

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
 * Revision 1.8  2005/05/27 17:05:07  jonblower
 * Changes to incorporate GeneralCachingStreamReader
 *
 * Revision 1.7  2005/05/26 20:31:11  jonblower
 * Changed behaviour such that clients can start trying to read from the stream before the service is started
 *
 * Revision 1.6  2005/05/16 11:00:53  jonblower
 * Changed SGS config XML file structure: separated input and output streams and changed some tag names
 *
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
    private GeneralCachingStreamReader reader;
    
    /**
     * Creates a new CachingStreamReader.
     * @param inst The StyxGridServiceInstance that owns this CachingStreamReader
     * @param name The name for this file as it will appear on the Styx server
     * @throws StyxException if the file could not be created.
     */
    public CachingStreamReader(StyxGridServiceInstance inst, String name) throws StyxException
    {
        super(name, 0444); // This is a read-only file
        this.reader = new SFCachingStreamReader();
    }
    
    /**
     * Sets the input stream from which the CachingStreamReader gets data
     * and immediately starts reading from this stream
     * @throws IllegalStateException if this object is already reading from a stream
     * @throws IOException if the cache file could not be created
     * and should not happen)
     */
    public void startReading(InputStream is) throws IOException
    {
        this.reader.startReading(is);
    }
    
    public void setCacheFile(File file) throws FileNotFoundException
    {
        this.reader.setCacheFile(file);
    }
    
    /**
     * Get data from the stream's cache.
     * @param offset The offset <b>relative to the start of the whole stream</b> of
     * the first byte of data requested.
     * @param count The maximum amount of data to return
     */
    public void read(StyxFileClient client, long offset, int count, int tag)
        throws StyxException
    {
        this.reader.read(new SFDataRequest(client, offset, count, tag));
    }
    
    private class SFCachingStreamReader extends GeneralCachingStreamReader
    {
        
        public void newData(DataRequest originalRequest, byte[] data,
            int offset, int count)
        {
            SFDataRequest dr = (SFDataRequest)originalRequest;
            StyxFileClient c = (StyxFileClient)dr.client;
            replyRead(c, data, offset, count, dr.tag);
        }
        
        public void error(DataRequest originalRequest, Exception e)
        {
            RerrorMessage rErrMsg = new RerrorMessage("error reading from stream: " +
                e.getMessage());
            SFDataRequest dr = (SFDataRequest)originalRequest;
            StyxFileClient c = (StyxFileClient)dr.client;
            StyxServerProtocolHandler.reply(c.getSession(), rErrMsg, dr.tag);
        }
    }
    
    private static class SFDataRequest extends GeneralCachingStreamReader.DataRequest
    {
        private int tag;
        public SFDataRequest(StyxFileClient client, long offset, int count, int tag)
        {
            super(client, offset, count);
            this.tag = tag;
        }
    }
    
    /**
     * Called when the file is removed from the server. This deletes the
     * underlying cache file.
     */
    protected synchronized void delete()
    {
        this.reader.delete();
    }
    
}
