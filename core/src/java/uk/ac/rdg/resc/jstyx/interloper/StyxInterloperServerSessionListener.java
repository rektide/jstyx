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

import net.gleamynode.netty2.Message;
import net.gleamynode.netty2.Session;
import net.gleamynode.netty2.SessionListener;
import net.gleamynode.netty2.SessionLog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;

import uk.ac.rdg.resc.jstyx.messages.TversionMessage;

/**
 * Session listener for the StyxInterloper server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:26  jonblower
 * Initial revision
 *
 */
class StyxInterloperServerSessionListener implements SessionListener
{
    private static final Log log = LogFactory.getLog(StyxInterloperServerSessionListener.class);
    
    private InetSocketAddress destSockAddr;
    
    public StyxInterloperServerSessionListener(InetSocketAddress destSockAddr)
    {
        this.destSockAddr = destSockAddr;
    }
    
    public void connectionEstablished(Session session)
    {
        SessionLog.info(log, session, "Client connection established.");
        // Now connect to the destination server
        InterloperClient client = new InterloperClient(this.destSockAddr, session);
        client.start();
        session.setAttachment(client);
    }
    
    public void connectionClosed(Session session)
    {
        SessionLog.info(log, session, "Client connection closed.");
        InterloperClient client = (InterloperClient)session.getAttachment();
        client.stop();
    }
    
    public void messageReceived(Session session, Message message)
    {
        SessionLog.info(log, session, "RCVD from client: " + message);
        
        // Make sure the message size is <= 8192 bytes (TODO: allow for message
        // sizes larger than this)
        if (message instanceof TversionMessage)
        {
            TversionMessage tVerMsg = (TversionMessage)message;
            if (tVerMsg.getMaxMessageSize() > 8192)
            {
                tVerMsg.setMaxMessageSize(8192);
            }
        }
        
        // Forward the message to the destination server
        InterloperClient client = (InterloperClient)session.getAttachment();
        client.send(message);
    }
    
    public void messageSent(Session session, Message message)
    {
        SessionLog.info(log, session, "SENT to client: " + message);
    }
    
    public void sessionIdle(Session session)
    {
        // Sessions are never disconnected if they are idle - is this OK?
    }
    
    public void exceptionCaught(Session session, Throwable cause)
    {
        SessionLog.error(log, session, "Unexpected exception.", cause);
    }
    
}
