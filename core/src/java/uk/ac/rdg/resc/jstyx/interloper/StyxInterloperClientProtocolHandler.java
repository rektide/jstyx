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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;

/**
 * Protocol handler for the StyxInterloperClient
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/05/05 16:57:37  jonblower
 * Updated MINA library to revision 168337 and changed code accordingly
 *
 * Revision 1.2  2005/03/11 14:01:59  jonblower
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
class StyxInterloperClientProtocolHandler implements ProtocolHandler
{
    private static final Logger log = Logger.getLogger(StyxInterloperClientProtocolHandler.class);
    
    private ProtocolSession serverSession;
    private InterloperListener listener;
    
    private boolean connected;
    
    public StyxInterloperClientProtocolHandler(ProtocolSession serverSession,
        InterloperListener listener)
    {
        this.connected = false;
        this.serverSession = serverSession;
        this.listener = listener;
    }
    
    /**
     * Invoked when the session is created.  Initialize default socket
     * parameters and user-defined attributes here.
     */
    public void sessionCreated( ProtocolSession session ) throws Exception
    {
        // TODO: not sure what we're supposed to do in this method
        log.info("Destination connection created.");
    }
    
    public void sessionOpened(ProtocolSession session )
    {
        log.info("Destination connection established.");
        this.connected = true;
    }
    
    public void sessionClosed(ProtocolSession session )
    {
        log.info("Destination connection closed.");
        this.connected = false;
    }
    
    public void messageReceived(ProtocolSession session, Object message )
    {
        if (log.isDebugEnabled())
        {
            log.debug("RCVD from destination: " + message);
        }
        // Have to notify the listener before we write the message back to the
        // client, otherwise we can get a situation where the interloper thinks
        // that there are more than one message outstanding with the same tag.
        this.listener.rMessageSent((StyxMessage)message);
        this.serverSession.write(message);
    }
    
    public void messageSent( ProtocolSession session, Object message )
    {
        if (log.isDebugEnabled())
        {
            log.debug("SENT to destination: " + message);
        }
    }
    
    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
        // Sessions are never disconnected if they are idle - is this OK?
    }
    
    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        log.error(cause.getMessage());
    }
    
    public boolean isConnected()
    {
        return this.connected;
    }
    
}
