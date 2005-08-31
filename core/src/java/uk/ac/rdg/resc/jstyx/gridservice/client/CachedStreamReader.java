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

package uk.ac.rdg.resc.jstyx.gridservice.client;

import java.io.File;
import java.io.IOException;

import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileInputStream;

import uk.ac.rdg.resc.jstyx.gridservice.server.GeneralCachingStreamReader;

/**
 * Reads a CStyxFileInputStream and caches the results locally, allowing multiple
 * windows to be opened on the same data feed whilst saving network traffic
 * 
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/08/31 17:03:19  jonblower
 * Renamed "StyxFile*putStream*" to "CStyxFile*putStream*" for consistency with CStyxFile class
 *
 * Revision 1.3  2005/06/07 16:44:45  jonblower
 * Fixed problem with caching stream reader on client side
 * 
 * Revision 1.2  2005/05/27 21:22:39  jonblower
 * Further development of caching stream readers
 * 
 * Revision 1.1  2005/05/27 17:02:59  jonblower
 * Initial import
 */
public class CachedStreamReader extends GeneralCachingStreamReader
{
    private CStyxFile stream;
    private boolean started;
    
    public CachedStreamReader(CStyxFile stream) throws IOException
    {
        this.stream = stream;
        this.started = false;
        this.setCacheFile(File.createTempFile(stream.getName(), ".tmp"));
    }
    
    /**
     * Starts reading from the stream.  Does nothing if already started.
     */
    public void start() throws IOException
    {
        if (!this.started)
        {
            this.started = true;
            this.startReading(new CStyxFileInputStream(this.stream));
        }
    }
    
    public void read(StreamViewer viewer, long offset, int count)
    {
        DataRequest dr = new DataRequest(viewer, offset, count);
        this.read(dr);
    }
    
    public void newData(DataRequest originalRequest, byte[] data,
        int offset, int count)
    {
        StreamViewer viewer = (StreamViewer)originalRequest.client;
        // TODO: notify viewer
        viewer.newData(data, count);
    }
    
    public void error(DataRequest originalRequest, Exception e)
    {
        StreamViewer viewer = (StreamViewer)originalRequest.client;
        // TODO: notify viewer
        e.printStackTrace();
    }
    
    public String getName()
    {
        return this.stream.getName();
    }
    
    
}
