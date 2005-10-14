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

import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * A client of an SGS server.  Use this class to find the SGSs that are available
 * on a server.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/10/14 18:07:06  jonblower
 * Added getSGSClient()
 *
 * Revision 1.1  2005/08/12 08:08:39  jonblower
 * Developments to support web interface
 *
 */
public class SGSServerClient
{
    
    private CStyxFile serverRoot; // File representing the root of the server
    
    /**
     * Creates a new instance of SGSServerClient.
     * @param root CStyxFile representing the root directory of the SGS server
     */
    public SGSServerClient(CStyxFile root)
    {
        this.serverRoot = root;
    }
    
    /**
     * Gets an SGSClient object for a given Styx Grid Service.  This method
     * blocks until the SGS is found and proven to exist on the server
     * @param serviceName The name of the Styx Grid Service
     * @return an SGSClient object for the requested Styx Grid Service
     * @throws StyxException if there is no SGS with the given name on the server
     */
    public SGSClient getSGSClient(String serviceName) throws StyxException
    {
        CStyxFile sgsRoot = this.serverRoot.getFile(serviceName);
        if (sgsRoot.exists())
        {
            return new SGSClient(sgsRoot);
        }
        else
        {
            throw new StyxException("There is no Styx Grid Service called " +
                serviceName + " on the server");
        }
    }
    
    /**
     * @return an SGSClient object for each Styx Grid Service on the server
     */
    public SGSClient[] getSGSClients() throws StyxException
    {
        CStyxFile[] services = this.serverRoot.getChildren();
        SGSClient[] clients = new SGSClient[services.length];
        for (int i = 0; i < clients.length; i++)
        {
            clients[i] = new SGSClient(services[i]);
        }
        return clients;
    }
    
}
