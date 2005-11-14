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

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;

/**
 * File that is read in order to get the exit code from the service instance.
 * If the exit code has not yet been set, read requests will be queued and replied
 * to when the exit code is set.
 * @ todo This class might be more generally useful - refactor?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/11/14 21:31:54  jonblower
 * Got SGSRun working for SC2005 demo
 *
 */

public class ExitCodeFile extends StyxFile
{
    
    private String exitCode;
    private Vector queuedRequests;
    
    /** Creates a new instance of ExitCodeFile */
    public ExitCodeFile() throws StyxException
    {
        super("exitCode", 0444); // This is a read-only file
        this.exitCode = null;
        this.queuedRequests = new Vector();
    }
    
    public synchronized void read(StyxFileClient client, long offset, int count,
        int tag) throws StyxException
    {
        if (this.exitCode != null)
        {
            // Exit code has been set: reply to the client 
            this.processAndReplyRead(this.exitCode, client, offset, count, tag);
        }
        else
        {
            // Enqueue this request
            DataRequest dr = new DataRequest(client, offset, count, tag);
            synchronized(this.queuedRequests)
            {
                this.queuedRequests.add(dr);
            }
        }
    }
    
    /**
     * Sets the exit code.  When this is called, all waiting clients will be notified
     * of the new exit code.
     */
    public synchronized void setExitCode(int exitCode)
    {
        this.exitCode = "" + exitCode;
        synchronized(this.queuedRequests)
        {
            for (int i = 0; i < this.queuedRequests.size(); i++)
            {
                DataRequest dr = (DataRequest)this.queuedRequests.get(i);
                this.processAndReplyRead(this.exitCode, dr.client, dr.offset,
                    dr.count, dr.tag);
            }
        }
    }
    
    /**
     * Class representing a request for data that we have enqueued
     */
    private static class DataRequest
    {
        private StyxFileClient client;
        private int tag;
        private long offset; // The offset requested by the client
        private int count; // The number of bytes requested by the client
        
        private DataRequest(StyxFileClient client, long offset, int count, int tag)
        {
            this.client = client;
            this.tag = tag;
            this.offset = offset;
            this.count = count;
        }
    }
    
}
