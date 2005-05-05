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

package uk.ac.rdg.resc.jstyx.interloper;

import java.net.InetSocketAddress;
import java.io.IOException;

import org.apache.mina.io.IoFilter;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketConnector;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolFilter;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.io.IoProtocolConnector;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * The client-side part of an Interloper
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/05/05 16:57:37  jonblower
 * Updated MINA library to revision 168337 and changed code accordingly
 *
 * Revision 1.3  2005/03/11 14:01:59  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.2  2005/03/11 08:30:30  jonblower
 * Moved to log4j logging system (from apache commons logging)
 *
 * Revision 1.2.2.1  2005/03/10 14:31:48  jonblower
 * Modified for MINA framework
 *
 * Revision 1.1.1.1  2005/02/16 18:58:26  jonblower
 * Initial import
 *
 */
public class InterloperClient
{
    private static final Logger log = Logger.getLogger(InterloperClient.class);
    
    private static final int CONNECT_TIMEOUT = 30; // seconds
    
    private ProtocolSession session;
    private ProtocolSession serverSession;
    private IoThreadPoolFilter ioThreadPoolFilter;
    private ProtocolThreadPoolFilter protocolThreadPoolFilter;
    
    private InterloperListener listener;
    
    private InetSocketAddress sockAddress;
    
    /** Creates a new instance of InterloperClient */
    public InterloperClient(InetSocketAddress sockAddress,
        ProtocolSession serverSession, InterloperListener listener)
    {
        this.sockAddress = sockAddress;
        this.serverSession = serverSession;
        this.listener = listener;
    }    
    
    /**
     * Starts the InterloperClient
     * @return true if the client was started successfully, false otherwise
     */
    public boolean start()
    {
        this.ioThreadPoolFilter = new IoThreadPoolFilter();
        this.protocolThreadPoolFilter = new ProtocolThreadPoolFilter();

        this.ioThreadPoolFilter.start();
        this.protocolThreadPoolFilter.start();

        IoProtocolConnector connector;
        try
        {
            connector = new IoProtocolConnector( new SocketConnector() );
        }
        catch(IOException ioe)
        {
            log.error("IOException occurred when creating IOProtocolConnector: "
                + ioe.getMessage());
            return false;
        }
        // I don't think the values of the priority constants matter much
        connector.getIoConnector().getFilterChain().addLast( "IO Thread pool filter",
            ioThreadPoolFilter );
        connector.getFilterChain().addLast( "Protocol Thread pool filter",
            protocolThreadPoolFilter );
        
        ProtocolProvider protocolProvider =
            new StyxInterloperProtocolProvider(this.serverSession, this.listener);
        
        try
        {
            this.session = connector.connect( this.sockAddress, CONNECT_TIMEOUT,
                protocolProvider );
        }
        catch( IOException e )
        {
            log.error("Failed to connect: " + e.getMessage());
            return false;
        }
        return true;
    }
    
    public void stop()
    {        
        // stop threads
        this.ioThreadPoolFilter.stop();
        this.protocolThreadPoolFilter.start();
    }
    
    public void send(Object message)
    {
        session.write(message);
    }
}
