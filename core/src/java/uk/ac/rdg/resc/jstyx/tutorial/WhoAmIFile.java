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

package uk.ac.rdg.resc.jstyx.tutorial;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxServer;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Simple custom Styx file that, when read, returns the IP address and local 
 * port of the client that is connected.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/08/30 16:27:49  jonblower
 * Continued to develop website and tutorial
 *
 * Revision 1.1  2005/08/30 09:50:48  jonblower
 * Renamed "WhoAmI" to "WhoAmIFile"
 *
 * Revision 1.1  2005/08/30 08:01:48  jonblower
 * Continuing development of tutorial
 *
 */
public class WhoAmIFile extends StyxFile
{
    
    /** Creates a new instance of WhoAmIFile */
    public WhoAmIFile() throws StyxException
    {
        super("whoami");
    }
    
    /**
     * Reads data from this file.  Gets the IP address of the remote client and
     * returns it.
     *
     * @param client The client that is performing the read
     * @param offset The index of the first byte of data to return
     * @param count The maximum number of bytes to read
     * @param tag The tag of the incoming read message (this is needed when
     * calling readReply())
     */
    public void read(StyxFileClient client, long offset, int count, int tag)
        throws StyxException
    {
        // Get the IP address and port of the remote client
        String clientAddr = client.getSession().getRemoteAddress().toString();
        // Process the request and reply to the client
        this.processAndReplyRead(clientAddr, client, offset, count, tag);
    }
    
    /**
     * Simple test function: sets up a StyxServer that serves up a WhoAmIFile
     */
    public static void main (String[] args) throws Exception
    {
        // Create the root directory
        StyxDirectory root = new StyxDirectory("/");
        // Create a WhoAmI file and add it to the root directory
        root.addChild(new WhoAmIFile());
        // Create and start a Styx server, listening on port 9876
        new StyxServer(9876, root).start();
    }
    
}
