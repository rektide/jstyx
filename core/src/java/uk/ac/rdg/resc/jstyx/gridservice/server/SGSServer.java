/*
 * Copyright (c) 2006 The University of Reading
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

import java.util.Iterator;
import java.security.GeneralSecurityException;

import uk.ac.rdg.resc.jstyx.StyxException;

import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxServer;
import uk.ac.rdg.resc.jstyx.server.StyxSecurityContext;

import uk.ac.rdg.resc.jstyx.ssh.StyxSSHServer;
    
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfig;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfigException;

/**
 * A Styx Grid Services server.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class SGSServer
{
    private SGSServerConfig config; // The configuration of this server
    private StyxDirectory root; // The root of the SGS namespace
    
    /**
     * Creates a Styx Grid Services server from the given configuration file
     * @throws SGSConfigException if there is an error in the configuration file
     * @throws StyxException if there was an error creating the namespace
     */
    public SGSServer(String configFile) throws SGSConfigException, StyxException
    {
        // Create the server configuration from the given XML config file
        this.config = new SGSServerConfig(configFile);
        // Create the root directory
        this.root = new StyxDirectory("/");
        // Add the SGSs to this directory
        Iterator it = config.getSGSConfigInfo();
        while(it.hasNext())
        {
            SGSConfig conf = (SGSConfig)it.next();
            this.root.addChild(new StyxGridService(conf).getRoot());
        }
    }
    
    /**
     * @return a StyxServer that will listen for connections on a given port and
     * serve the Styx namespace.
     * @throws GeneralSecurityException if there was an error setting up the
     * security context
     */
    public StyxServer getStyxServer() throws GeneralSecurityException
    {
        return new StyxServer(this.getPort(), this.root,
            this.config.getSecurityContextFile());
    }
    
    /**
     * @return the port on which this server will listen (irrelevant if we want
     * a StyxSSHServer)
     */
    public int getPort()
    {
        return this.config.getPort();
    }
    
    /**
     * @return a StyxSSHServer that listens for messages on its standard input 
     * and writes replies to its standard output.  This server is run as a
     * process by clients that connect through SSH.
     * @throws GeneralSecurityException if there was an error setting up the
     * security context
     */
    public StyxSSHServer getStyxSSHServer() throws GeneralSecurityException
    {
        if (this.config.getSecurityContextFile() == null)
        {
            return new StyxSSHServer(this.root, new StyxSecurityContext());
        }
        else
        {
            return new StyxSSHServer(this.root,
                new StyxSecurityContext(this.config.getSecurityContextFile()));
        }
    }
    
    public static void main (String[] args) throws Exception
    {
        System.setProperty("java.protocol.handler.pkgs", "uk.ac.rdg.resc.jstyx.client.protocol");
        if (args.length < 1 || args.length > 2)
        {
            System.err.println("Usage: StyxGridService <config file> [-ssh]");
            return;
        }
        
        SGSServer sgsServer = new SGSServer(args[0]);

        // Check to see if we are doing this over SSH
        if (args.length > 1 && args[1].equals("-ssh"))
        {
            sgsServer.getStyxSSHServer().start();
        }
        else
        {
            // We will create a server that listens on a certain port
            sgsServer.getStyxServer().start();
            System.out.println("Started StyxGridServices, listening on port "
                + sgsServer.getPort());
        }
    }
    
    
}
