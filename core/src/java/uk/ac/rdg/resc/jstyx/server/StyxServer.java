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
import javax.net.ssl.SSLContext;

import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.filter.SSLFilter;
import org.apache.mina.io.socket.SocketAcceptor;
import org.apache.mina.protocol.ProtocolHandlerFilter;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.io.IoProtocolAcceptor;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.ssl.JonSSLContextFactory;

/**
 * A Styx server.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
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
    
    private ProtocolProvider provider;
    private int port;
    
    private SSLContext sslContext; // Non-null if we want to secure the server
    
    /**
     * Creates a Styx server that exposes the given directory under the given
     * port.
     * @throws IllegalArgumentException if the port number is invalid or the
     * root is null.
     */
    public StyxServer(int port, StyxDirectory root)
    {
        this(port, root, null); // By default, don't use SSL
    }
    
    /**
     * Creates a Styx server that exposes the given directory under the given
     * port.
     * @throws IllegalArgumentException if the port number is invalid or the
     * root is null.
     */
    public StyxServer(int port, StyxDirectory root, SSLContext sslContext)
    {
        this(port, new StyxServerProtocolProvider(root), sslContext);
    }
    
    /**
     * Creates a Styx server that listens on the given port and uses the
     * given protocol provider (This is used by the Styx interloper class)
     * @throws IllegalArgumentException if the port number is invalid or the
     * provider is null.
     */
    public StyxServer(int port, ProtocolProvider provider)
    {
        this(port, provider, null); // By default, don't use SSL
    }
    
    /**
     * Creates a Styx server that listens on the given port and uses the
     * given protocol provider (This is used by the Styx interloper class)
     * @throws IllegalArgumentException if the port number is invalid or the
     * provider is null.
     */
    public StyxServer(int port, ProtocolProvider provider, SSLContext sslContext)
    {
        if (provider == null)
        {
            throw new IllegalArgumentException("ProtocolProvider cannot be null");
        }
        // Check that the port number is valid
        // TODO: should we disallow other port numbers?
        if (port < 0 || port > StyxUtils.MAXUSHORT)
        {
            throw new IllegalArgumentException("Invalid port number");
        }
        this.port = port;
        this.provider = provider;
        this.sslContext = sslContext;
    }
    
    /**
     * Starts the Styx server.
     * @throws StyxException if an error occurred
     */
    public void start() throws StyxException
    {
        // Create I/O and Protocol thread pool filter.
        // I/O thread pool performs encoding and decoding of messages.
        // Protocol thread pool performs actual protocol flow.
        IoThreadPoolFilter ioThreadPoolFilter = new IoThreadPoolFilter();
        ProtocolThreadPoolFilter protocolThreadPoolFilter = new ProtocolThreadPoolFilter();
        
        // and start both.
        ioThreadPoolFilter.start();
        protocolThreadPoolFilter.start();
        
        // Create a TCP/IP acceptor.
        IoProtocolAcceptor acceptor;
        try
        {
             acceptor = new IoProtocolAcceptor( new SocketAcceptor() );
        }
        catch(IOException ioe)
        {
            throw new StyxException("An IOException occurred when creating the "
                + "IoProtocolAcceptor: " + ioe.getMessage());
        }
        
        // Add both thread pool filters.
        // I don't think the values of the priority constants matter much here
        acceptor.getIoAcceptor().addFilter( Integer.MAX_VALUE, ioThreadPoolFilter );
        acceptor.addFilter( Integer.MAX_VALUE, protocolThreadPoolFilter );
        
        // Add SSL filter if SSL is enabled.
        if( this.sslContext != null )
        {
            SSLFilter sslFilter = new SSLFilter( this.sslContext );
            //sslFilter.setDebug(SSLFilter.Debug.ON);
            acceptor.getIoAcceptor().addFilter( Integer.MAX_VALUE - 1, sslFilter );
        }
        
        // Bind
        try
        {
            acceptor.bind( new InetSocketAddress(this.port), this.provider);
        }
        catch(IOException ioe)
        {
            throw new StyxException("An IOException occurred when calling acceptor.bind(): "
                + ioe.getMessage());
        }
        
        log.info( "Listening on port " + this.port + ", SSL " +
            (this.sslContext == null ? "disabled" : "enabled"));
    }
}
