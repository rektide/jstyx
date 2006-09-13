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

package uk.ac.rdg.resc.jstyx.ssh;

import java.io.PrintStream;

import org.apache.log4j.Logger;

import org.apache.mina.common.IoSession;

import uk.ac.rdg.resc.jstyx.server.StyxSecurityContext;
import uk.ac.rdg.resc.jstyx.server.StyxServerProtocolHandler;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.DirectoryOnDisk;

/**
 * Styx "server" that listens for incoming messages on its standard input and
 * writes outgoing messages on its standard output.  This can be executed via
 * SSH using an exec request and hence Styx messages can be exchanged across
 * a secure connection.  The counterpart client class is StyxSSHConnection.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class StyxSSHServer
{
    private static final Logger log = Logger.getLogger(StyxSSHServer.class);
    
    private IoSession session;
    private StyxServerProtocolHandler handler;
    
    public static void main(String[] args) throws Exception
    {
        // Create and start a simple server with a default security context
        // (anonymous logins allowed, no encryption, etc)
        if (args.length != 1)
        {
            System.err.println("Usage: StyxSSHServer <root directory>");
            return;
        }
        StyxDirectory root = new DirectoryOnDisk(args[0]);
        new StyxSSHServer(root, new StyxSecurityContext()).start();
    }
    
    /**
     * Creates a new server that will listen for Styx Tmessages on its standard
     * input and write Rmessages on its standard output.  Call start() to start
     * the server process.
     * @param root Root of the Styx namespace that will be served
     * @param securityContext The security context
     */
    public StyxSSHServer(StyxDirectory root, StyxSecurityContext securityContext)
    {
        // Create a protocol handler with a security context that does not
        // use authentication 
        this.handler = new StyxServerProtocolHandler(root, securityContext);
        // Create an IoSession that writes messages to standard output
        this.session = new StyxSSHIoSession(this.handler, System.out);
    }
    
    /**
     * Starts the StyxSSHServer (reads from standard input)
     */
    public void start()
    {
        new MessageReader(System.in, this.handler, this.session).start();
    }
    
}
