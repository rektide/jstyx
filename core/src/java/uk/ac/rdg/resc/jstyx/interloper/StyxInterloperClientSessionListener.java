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

/**
 * Session listener for the StyxInterloperClient
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:26  jonblower
 * Initial revision
 *
 */
class StyxInterloperClientSessionListener implements SessionListener
{
    private static final Log log = LogFactory.getLog(StyxInterloperClientSessionListener.class);
    
    private Session serverSession;
    
    private boolean connected;
    
    public StyxInterloperClientSessionListener(Session serverSession)
    {
        this.connected = false;
        this.serverSession = serverSession;
    }
    
    public void connectionEstablished(Session session)
    {
        SessionLog.info(log, session, "Destination connection established.");
        this.connected = true;
    }
    
    public void connectionClosed(Session session)
    {
        SessionLog.info(log, session, "Destination connection closed.");
        this.connected = false;
    }
    
    public void messageReceived(Session session, Message message)
    {
        //SessionLog.info(log, session, "RCVD from destination: " + message);
        this.serverSession.write(message);
    }
    
    public void messageSent(Session session, Message message)
    {
        //SessionLog.info(log, session, "SENT to destination: " + message);
    }
    
    public void sessionIdle(Session session)
    {
        // Sessions are never disconnected if they are idle - is this OK?
    }
    
    public void exceptionCaught(Session session, Throwable cause)
    {
        SessionLog.error(log, session, "Unexpected exception.", cause);
    }
    
    public boolean isConnected()
    {
        return this.connected;
    }
    
}
