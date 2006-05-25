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

package uk.ac.rdg.resc.jstyx.server;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.common.IoHandler;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * A Styx server.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.10  2006/03/21 14:58:42  jonblower
 * Implemented clear-text password-based authentication and did some simple tests
 *
 * Revision 1.9  2006/03/21 09:06:15  jonblower
 * Still implementing authentication
 *
 * Revision 1.8  2006/03/20 17:51:50  jonblower
 * Adding authentication to base JStyx system
 *
 * Revision 1.7  2005/08/30 16:28:23  jonblower
 * Subsumed TestServer program into StyxServer class
 *
 * Revision 1.6  2005/05/05 16:57:38  jonblower
 * Updated MINA library to revision 168337 and changed code accordingly
 *
 * Revision 1.5  2005/03/24 07:57:41  jonblower
 * Improved code for reading SSL info from SGSconfig file and included parameter information for the Grid Services in the config file
 *
 * Revision 1.4  2005/03/14 16:40:02  jonblower
 * Modifications for using SSL
 *
 * Revision 1.3  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.6  2005/03/11 08:30:30  jonblower
 * Moved to log4j logging system (from apache commons logging)
 *
 * Revision 1.2.2.5  2005/03/10 18:32:18  jonblower
 * Minor change to layout
 *
 * Revision 1.2.2.3 and 1.2.2.4  2005/03/10 14:38:10  jonblower
 * Modified for MINA framework
 *
 * Revision 1.2.2.1  2005/03/09 19:44:18  jonblower
 * Changes concerned with migration to MINA
 *
 * Revision 1.1.1.1  2005/02/16 18:58:33  jonblower
 * Initial import
 *
 */
public class StyxServer
{
    
    private static final Logger log = Logger.getLogger(StyxServer.class);
    
    private IoHandler handler;
    private int port;
    
    private StyxSecurityContext securityContext; // If this is null, access to the
                                                 // server is anonymous and unsecured
    
    /**
     * Creates a Styx server that exposes the given directory under the given
     * port.  No security information is used: server will allow anonymous access
     * and no traffic will be encrypted.
     * @throws IllegalArgumentException if the port number is invalid or the
     * root is null.
     * @throws GeneralSecurityException if there was an error setting up the
     * security context (should never happen since this will be an unsecured
     * server).
     */
    public StyxServer(int port, StyxDirectory root) throws GeneralSecurityException
    {
        this(port, root, null);
    }
    
    /**
     * Creates a Styx server that listens on the given port and uses the
     * given io handler (This is used by the Styx interloper class)
     * Connections are anonymous and unsecured.
     * @throws IllegalArgumentException if the port number is invalid or the
     * provider is null.
     */
    public StyxServer(int port, IoHandler handler)
    {
        if (handler == null)
        {
            throw new IllegalArgumentException("IoHandler cannot be null");
        }
        setPortNumber(port);
        this.handler = handler;
        this.securityContext = new StyxSecurityContext();
    }
    
    /**
     * Creates a Styx server.
     * @param port The port number on which the server will listen
     * @param root The root of the Styx filesystem to serve
     * @param securityConfigFile The file containing security information
     * (user details, SSL setup etc).  If this is null, the server will allow
     * anonymous access and no traffic will be encrypted.
     * @throws IllegalArgumentException if the port number is invalid or 
     * root == null.
     * @throws GeneralSecurityException if there was an error reading security
     * configuration from <code>securityConfigFile</code>.
     */
    public StyxServer(int port, StyxDirectory root, String securityConfigFile)
        throws GeneralSecurityException
    {
        if (root == null)
        {
            throw new IllegalArgumentException("root cannot be null");
        }
        // Set the port number, checking that it is valid
        setPortNumber(port);
        if (securityConfigFile == null)
        {
            this.securityContext = new StyxSecurityContext();
        }
        else
        {
            this.securityContext = new StyxSecurityContext(securityConfigFile);
        }
        this.handler = new StyxServerProtocolHandler(root, this.securityContext);
    }
    
    /**
     * Checks to see if the given port number is valid, throwing an 
     * IllegalArgumentException if it isn't.  If it is valid, sets it
     */
    private void setPortNumber(int port)
    {
        // TODO: should we disallow other port numbers?
        if (port < 0 || port > StyxUtils.MAXUSHORT)
        {
            throw new IllegalArgumentException("Invalid port number");
        }
        this.port = port;
    }
    
    /**
     * Starts the Styx server.  Does nothing if the server is already started.
     * @throws IOException if an error occurred
     */
    public void start() throws IOException
    {
        IoAcceptor acceptor = new SocketAcceptor();
        InetSocketAddress sockAddress = new InetSocketAddress(this.port);
        acceptor.bind(sockAddress, this.handler);

        // Add a shutdown hook that unbinds this acceptor when the Java VM
        // shuts down
        Runtime.getRuntime().addShutdownHook(new Unbinder(acceptor, sockAddress));

        log.info( "Listening on port " + this.port);
    }
    
    /**
     * Shutdown hook for this Styx server; unbinds the IoAcceptor when the
     * JVM shuts down
     */
    private static class Unbinder extends Thread
    {
        private IoAcceptor acceptor;
        private InetSocketAddress sockAddress;
        public Unbinder(IoAcceptor acceptor, InetSocketAddress sockAddress)
        {
            this.acceptor = acceptor;
            this.sockAddress = sockAddress;
        }
        public void run()
        {
            if (this.acceptor != null)
            {
                log.debug("Unbinding from port " + sockAddress.getPort());
                this.acceptor.unbind(sockAddress);
            }
        }
    }
    
    /**
     * Simple test Styx server that exposes the contents of a local directory.
     * The TestServer takes two arguments, both optional.  The first is the
     * port number under which the server will listen (defaults to 8080 if
     * not set). The second is the directory in the host filesystem which
     * will be at the root of the Styx server. This defaults to the user's
     * home directory (i.e. the output of System.getProperty("user.home"))
     * if not set.
     */
    public static void main(String[] args) throws Throwable
    {
        // Set the default port and root directory of the server
        int port = 8080;
        // Default root directory is the user's home directory
        String home = System.getProperty("user.home");
        String securityFile = null;
        
        if (args.length > 0)
        {
            try
            {
                port = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println(args[0] + " is not a valid port number");
                return;
            }
        }
        if (args.length > 1)
        {
            home = args[1];
        }
        if (args.length > 2)
        {
            securityFile = args[2];
        }
        if (args.length > 3)
        {
            System.err.println("Usage: TestServer [port] [root directory] [security file]");
            return;
        }
        
        // Set up the file tree
        System.out.print("Building directory tree (this can take some time)... ");
        StyxDirectory root = new DirectoryOnDisk(home);
        
        // Add some files with different users and groups
        root.addChild(new StyxDirectory("jdbandusers", "jdb", StyxUtils.DEFAULT_GROUP, 0755));
        root.addChild(new StyxDirectory("jdbonly", "jdb", StyxUtils.DEFAULT_GROUP, 0700));
        root.addChild(new StyxDirectory("adminsonly", "jim", "admins", 0750));
        
        // Set up the server and start it with the given configuration file
        StyxServer server = new StyxServer(port, root, securityFile);
        server.start();
        
        System.out.println("done.");
    }
}
