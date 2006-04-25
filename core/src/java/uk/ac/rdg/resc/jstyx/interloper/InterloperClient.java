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

import org.apache.mina.common.IoConnector;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoSession;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * The client-side part of an Interloper
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/07/08 15:23:03  jonblower
 * Upgraded MINA library to 0.7.3-SNAPSHOT
 *
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
    
    private IoSession session;
    private IoSession serverSession;
    
    private InterloperListener listener;
    
    private InetSocketAddress sockAddress;
    
    /** Creates a new instance of InterloperClient */
    public InterloperClient(InetSocketAddress sockAddress,
        IoSession serverSession, InterloperListener listener)
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

        IoConnector connector = new SocketConnector();
        
        ConnectFuture future = connector.connect( this.sockAddress,
            new StyxInterloperClientProtocolHandler(this.serverSession, this, this.listener) );
        
        return true;
    }
    
    public void stop()
    {
    }
    
    public void setSession(IoSession session)
    {
        this.session = session;
    }
    
    public void send(Object message)
    {
        session.write(message);
    }
}
