package uk.ac.rdg.resc.jstyx.mina;
/*
 * MinaServer.java
 *
 * Created on 07 March 2005, 14:41
 */

import java.net.InetSocketAddress;

import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketAcceptor;
import org.apache.mina.protocol.ProtocolHandlerFilter;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.io.IoProtocolAcceptor;

/**
 *
 * @author jdb
 */
public class MinaServer
{
    
    private static final int PORT = 8081;
    
    public static void main( String[] args ) throws Exception
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
        IoProtocolAcceptor acceptor = new IoProtocolAcceptor( new SocketAcceptor() );
        
        // Add both thread pool filters.
        acceptor.getIoAcceptor().addFilter( 99, ioThreadPoolFilter );
        acceptor.addFilter( 99, protocolThreadPoolFilter );
        
        // Bind
        acceptor.bind( new InetSocketAddress( PORT ),
            new StyxProtocolProvider() );
        
        System.out.println( "Listening on port " + PORT );
    }
}
