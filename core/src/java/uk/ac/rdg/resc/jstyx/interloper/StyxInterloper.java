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

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxMessageRecognizer;
import uk.ac.rdg.resc.jstyx.server.StyxServer;

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
 * Revision 1.3  2005/02/24 07:42:44  jonblower
 * *** empty log message ***
 *
 * Revision 1.2  2005/02/21 18:09:45  jonblower
 * *** empty log message ***
 *
 * Revision 1.1.1.1  2005/02/16 18:58:26  jonblower
 * Initial import
 *
 */
public class StyxInterloper
{
    
    private static final Log log = LogFactory.getLog(StyxInterloper.class);
    
    private int port;
    private InetSocketAddress destSockAddr;
    private StyxServer styxServer;
    
    /**
     * Creates a new StyxInterloper.
     * @param port The port on which this server will listen
     * @param serverost The host of the destination to which this server will connect
     * @param serverPort The port of the destination to which this server will connect
     * @throws StyxException if there was an error starting the Styx server
     */
    public StyxInterloper(int port, String serverHost, int serverPort)
        throws StyxException
    {
        this.port = port;
        InetSocketAddress destSockAddr = new InetSocketAddress(serverHost, serverPort);
        this.styxServer = new StyxServer(port, new StyxInterloperServerSessionListener(destSockAddr));
        this.styxServer.start();
    }
    
    public static void main (String[] args) throws Throwable
    {
        new StyxInterloper(2911, "localhost", 7777);
    }
    
}
