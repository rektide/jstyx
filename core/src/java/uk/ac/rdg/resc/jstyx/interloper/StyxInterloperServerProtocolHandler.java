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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.messages.TversionMessage;
import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Protocol handler for the StyxInterloper server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/03/15 15:51:41  jonblower
 * Removed hard limit on maximum message size
 *
 * Revision 1.2  2005/03/11 14:02:15  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.2.2  2005/03/11 08:30:30  jonblower
 * Moved to log4j logging system (from apache commons logging)
 *
 * Revision 1.1.2.1  2005/03/10 14:30:48  jonblower
 * Replaced SessionListeners with ProtocolHandlers
 *
 * Revision 1.1.1.1  2005/02/16 18:58:26  jonblower
 * Initial import
 *
 */
class StyxInterloperServerProtocolHandler implements ProtocolHandler
{
    private static final Logger log = Logger.getLogger(StyxInterloperServerProtocolHandler.class);
    
    private InetSocketAddress destSockAddr;
    private InterloperListener listener;
    
    public StyxInterloperServerProtocolHandler(InetSocketAddress destSockAddr,
        InterloperListener listener)
    {
        this.destSockAddr = destSockAddr;
        this.listener = listener;
    }
    
    public void sessionOpened(ProtocolSession session )
    {
        log.info("Client connection established.");
        // Now connect to the destination server
        InterloperClient client = new InterloperClient(this.destSockAddr, session, this.listener);
        if (client.start())
        {
            // client started successfully
            session.setAttachment(client);
        }
        else
        {
            // Couldn't start the client. close the session.
            session.close();
        }
    }
    
    public void sessionClosed(ProtocolSession session )
    {
        log.info("Client connection closed.");
        InterloperClient client = (InterloperClient)session.getAttachment();
        client.stop();
    }
    
    public void messageReceived(ProtocolSession session, Object message )
    {
        if (log.isDebugEnabled())
        {
            log.debug("RCVD from client: " + message);
        }
        listener.tMessageReceived((StyxMessage)message);
        
        // Make sure that we can handle the message size that the client
        // is requesting; if not, change the TversionMessage before we pass it
        // through.
        if (message instanceof TversionMessage)
        {
            TversionMessage tVerMsg = (TversionMessage)message;
            if (tVerMsg.getMaxMessageSize() > StyxUtils.MAX_MESSAGE_SIZE)
            {
                tVerMsg.setMaxMessageSize(StyxUtils.MAX_MESSAGE_SIZE);
            }
        }
        
        // Forward the message to the destination server
        InterloperClient client = (InterloperClient)session.getAttachment();
        client.send(message);
    }
    
    public void messageSent( ProtocolSession session, Object message )
    {
        if (log.isDebugEnabled())
        {
            log.debug("SENT to client: " + message);
        }
        // We notify the InterloperListener from the StyxInterloperClientSessionListener
        // because if we did it from here, we can end up with a situation in
        // which the interloper thinks that there are more than one message
        // with the same tag outstanding
    }
    
    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
        // Sessions are never disconnected if they are idle - is this OK?
    }
    
    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        log.error(cause.getMessage());
    }
    
}
