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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.io.IOException;

import net.gleamynode.netty2.IoProcessor;
import net.gleamynode.netty2.MessageRecognizer;
import net.gleamynode.netty2.SessionServer;
import net.gleamynode.netty2.OrderedEventDispatcher;
import net.gleamynode.netty2.ThreadPooledEventDispatcher;

import uk.ac.rdg.resc.jstyx.StyxMessageRecognizer;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * A Styx server.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:33  jonblower
 * Initial revision
 *
 */
public class StyxServer
{
    private static final Log log = LogFactory.getLog(StyxServer.class);
    
    private static final int DISPATCHER_THREAD_POOL_SIZE = 16;
    
    private StyxDirectory root;
    private int port;
    
    public StyxServer(StyxDirectory root, int port) throws StyxException
    {
        // Check that the port number is valid
        // TODO: should we disallow other port numbers?
        if (port < 0 || port > StyxUtils.MAXUSHORT)
        {
            throw new StyxException("Invalid port number");
        }
        this.root = root;
        this.port = port;
    }
    
    public void start() throws StyxException
    {
        // initialize I/O processor and event dispatcher
        IoProcessor ioProcessor;
        try
        {
            ioProcessor = StyxUtils.getIoProcessor();
        }
        catch(IOException ioe)
        {
            log.fatal("Could not get IoProcessor: " + ioe.getMessage());
            throw new StyxException("Could not start the server: " + ioe.getMessage());
        }
        ThreadPooledEventDispatcher eventDispatcher = new OrderedEventDispatcher();
        eventDispatcher.setThreadPoolSize(DISPATCHER_THREAD_POOL_SIZE);
        eventDispatcher.start();
        
        // The event dispatcher and ioProcessor will have already been started
        
        // prepare message recognizer
        MessageRecognizer recognizer = new StyxMessageRecognizer(StyxMessageRecognizer.SERVER_MODE);
        
        // prepare session event listener which will provide communication workflow.
        StyxServerSessionListener listener = new StyxServerSessionListener(this.root);
        
        // prepare session server
        SessionServer server = new SessionServer();
        server.setIoProcessor(ioProcessor);
        server.setEventDispatcher(eventDispatcher);
        server.setMessageRecognizer(recognizer);
        
        server.addSessionListener(listener);
        server.setBindAddress(new InetSocketAddress(this.port));
        
        // open the server port, accept connections, and start communication
        log.info("Listening on port " + port);
        try
        {
            server.start();
        }
        catch(IOException ioe)
        {
            log.fatal(ioe.getMessage());
            throw new StyxException("An IOException occurred when starting " +
                "the server: " + ioe.getMessage());            
        }
    }
}
