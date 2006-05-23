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

import java.util.Hashtable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * A client of an SGS server.  Use this class to find the SGSs that are available
 * on a server.  To create an instance of this class, use the <code>getServerClient()</code>
 * static factory method.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2006/02/20 17:34:27  jonblower
 * Added getConnection() method
 *
 * Revision 1.4  2006/01/05 16:06:34  jonblower
 * SGS clients now deal with possibility that client could be created on a different server
 *
 * Revision 1.3  2005/12/01 08:36:02  jonblower
 * Changed constructor to accept hostname and port instead of CStyxFile
 *
 * Revision 1.2  2005/10/14 18:07:06  jonblower
 * Added getSGSClient()
 *
 * Revision 1.1  2005/08/12 08:08:39  jonblower
 * Developments to support web interface
 *
 */
public class SGSServerClient
{
    // Contains the server clients that have already been created
    private static Hashtable serverClients = new Hashtable();
    
    // The underlying connection
    private StyxConnection conn;
    
    // Contains the SGS clients that have been created
    private Hashtable sgsClients;
    
    private CStyxFile serverRoot; // File representing the root of the server
    
    /**
     * Connects to the SGS server and sets up the client.  Blocks until the connection
     * to the server is made.
     * @param conn the StyxConnection which we are using to connect to the server.
     * @throws StyxException if there was an error connecting to the server
     */
    private SGSServerClient(StyxConnection conn) throws StyxException
    {
        this.sgsClients = new Hashtable();
        this.conn = conn;
        //this.conn.connect();
        this.serverRoot = conn.getRootDirectory();
    }
    
    /**
     * @return the underlying connection to the server
     */
    public StyxConnection getConnection()
    {
        return this.conn;
    }
    
    /**
     * Static factory method for creating an SGSServerClient.  If a client
     * already exists for the given connection object it will be returned.
     * If not, a new one will be created.
     * @param conn the StyxConnection which we are using to connect to the server.
     * @throws StyxException if there was an error connecting to the server.
     * @throws UnknownHostException if the host could not be found
     */
    public static SGSServerClient getServerClient(StyxConnection conn)
        throws StyxException, UnknownHostException
    {
        SGSServerClient serverClient = (SGSServerClient)serverClients.get(conn);
        if (serverClient == null)
        {
            serverClient = new SGSServerClient(conn);
            serverClients.put(conn, serverClient);
        }
        return serverClient;
    }
    
    /**
     * Gets an SGSClient object for a given Styx Grid Service.  This method
     * blocks until the SGS is found and proven to exist on the server.  If a
     * client for the given SGS has already been created, it is simply returned.
     * @param serviceName The name of the Styx Grid Service
     * @return an SGSClient object for the requested Styx Grid Service
     * @throws StyxException if there is no SGS with the given name on the server
     */
    public synchronized SGSClient getSGSClient(String serviceName) throws StyxException
    {
        SGSClient sgsClient = (SGSClient)this.sgsClients.get(serviceName);
        if (sgsClient == null)
        {
            // We need to create a new client
            CStyxFile sgsRoot = this.serverRoot.getFile(serviceName);
            if (sgsRoot.exists())
            {
                sgsClient = new SGSClient(sgsRoot);
            }
            else
            {
                throw new StyxException("There is no Styx Grid Service called " +
                    serviceName + " on the server");
            }
        }
        return sgsClient;
    }
    
}
