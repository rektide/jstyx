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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.io.IOException;

import net.gleamynode.netty2.IoProcessor;
import net.gleamynode.netty2.MessageRecognizer;
import net.gleamynode.netty2.OrderedEventDispatcher;
import net.gleamynode.netty2.SessionServer;
import net.gleamynode.netty2.ThreadPooledEventDispatcher;
import net.gleamynode.netty2.Session;
import net.gleamynode.netty2.Message;

import uk.ac.rdg.resc.jstyx.StyxMessageRecognizer;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * The client-side part of an Interloper
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/24 07:42:44  jonblower
 * *** empty log message ***
 *
 * Revision 1.1.1.1  2005/02/16 18:58:26  jonblower
 * Initial import
 *
 */
public class InterloperClient
{
    private static final Log log = LogFactory.getLog(InterloperClient.class);
    private static final int DISPATCHER_THREAD_POOL_SIZE = 1;
    private static final int CONNECT_TIMEOUT = 30; // seconds
    
    private IoProcessor ioProcessor;
    private ThreadPooledEventDispatcher eventDispatcher;
    private Session session;
    private Session serverSession;
    
    private InterloperListener listener;
    
    private InetSocketAddress sockAddress;
    
    /** Creates a new instance of InterloperClient */
    public InterloperClient(InetSocketAddress sockAddress, Session serverSession,
        InterloperListener listener)
    {
        this.sockAddress = sockAddress;
        this.serverSession = serverSession;
        this.listener = listener;
    }    
    
    public void start()
    {
        // initialize I/O processor and event dispatcher
        eventDispatcher = new OrderedEventDispatcher();
        
        // start with the default number of I/O worker threads
        try
        {
            ioProcessor = StyxUtils.getIoProcessor(); //new IoProcessor();
        }
        catch(IOException ioe)
        {
            throw new ExceptionInInitializerError(ioe);
        }
        
        // start with a few event dispatcher threads
        eventDispatcher.setThreadPoolSize(DISPATCHER_THREAD_POOL_SIZE);
        eventDispatcher.start();
        
        // prepare message recognizer
        MessageRecognizer recognizer = new StyxMessageRecognizer(StyxMessageRecognizer.CLIENT_MODE);
        
        // create a client session
        session = new Session(ioProcessor, this.sockAddress, recognizer, eventDispatcher);
        
        // set configuration
        session.getConfig().setConnectTimeout(CONNECT_TIMEOUT);
        
        // suscribe and start communication
        StyxInterloperClientSessionListener listener = new StyxInterloperClientSessionListener(this.serverSession, this.listener);
        session.addSessionListener(listener);
        
        log.info("Connecting to " + session.getSocketAddress());
        session.start();
        
        // wait until the client is connected
        while ( !listener.isConnected() )
        {
            try
            {
                Thread.sleep(500);
            }
            catch(InterruptedException ie)
            {
            }
        }
    }
    
    public void stop()
    {        
        // stop I/O processor and event dispatcher
        eventDispatcher.stop();
        ioProcessor.stop();        
    }
    
    public void send(Message message)
    {
        session.write(message);
    }
}
