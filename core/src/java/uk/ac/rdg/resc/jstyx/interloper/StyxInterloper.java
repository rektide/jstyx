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

import net.gleamynode.netty2.IoProcessor;
import net.gleamynode.netty2.MessageRecognizer;
import net.gleamynode.netty2.OrderedEventDispatcher;
import net.gleamynode.netty2.SessionServer;
import net.gleamynode.netty2.ThreadPooledEventDispatcher;

import uk.ac.rdg.resc.jstyx.StyxMessageRecognizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.io.IOException;


/**
 * A StyxInterloper listens for Styx messages, then forwards them directly to
 * another Styx server. The replies from the other Styx server are sent back to 
 * the client.  (I.e. it performs a similar function to TCPMon.)  This allows the
 * Styx messages sent between systems to be investigated.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:26  jonblower
 * Initial revision
 *
 */
public class StyxInterloper
{
    
    private static final Log log = LogFactory.getLog(StyxInterloper.class);
    private static final int DISPATCHER_THREAD_POOL_SIZE = 16;
    
    private int port;
    private InetSocketAddress destSockAddr;
    
    /**
     * Creates a new StyxInterloper.
     * @param serverPort The port on which this server will listen
     * @param destHost The host of the destination to which this server will connect
     * @param destPort The port of the destination to which this server will connect
     */
    public StyxInterloper(int serverPort, String destHost, int destPort)
    {
        this.port = serverPort;
        this.destSockAddr = new InetSocketAddress(destHost, destPort);
    }   
    
    public void start() throws IOException
    {
        // initialize I/O processor and event dispatcher
        IoProcessor ioProcessor = new IoProcessor();
        ThreadPooledEventDispatcher eventDispatcher = new OrderedEventDispatcher();
        
        // start with the default number of I/O worker threads
        ioProcessor.start();
        
        // start with a few event dispatcher threads
        eventDispatcher.setThreadPoolSize(DISPATCHER_THREAD_POOL_SIZE);
        eventDispatcher.start();
        
        // prepare message recognizer
        MessageRecognizer recognizer = new StyxMessageRecognizer(StyxMessageRecognizer.SERVER_MODE);
        
        // prepare session event listener which will provide communication workflow.
        StyxInterloperServerSessionListener listener = new StyxInterloperServerSessionListener(destSockAddr);
        
        // prepare session server
        SessionServer server = new SessionServer();
        server.setIoProcessor(ioProcessor);
        server.setEventDispatcher(eventDispatcher);
        server.setMessageRecognizer(recognizer);
        
        server.addSessionListener(listener);
        server.setBindAddress(new InetSocketAddress(this.port));
        
        // open the server port, accept connections, and start communication
        log.info("Listening on port " + this.port);
        server.start();
    }
    
    public static void main (String[] args) throws Throwable
    {
        new StyxInterloper(2910, "lovejoy.nerc-essc.ac.uk", 6678).start();
    }
    
}
