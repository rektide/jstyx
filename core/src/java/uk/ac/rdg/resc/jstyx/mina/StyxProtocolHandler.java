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

package uk.ac.rdg.resc.jstyx.mina;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;

/**
 * Protocol handler for for Styx protocol, used by MINA framework
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/03/09 16:58:42  jonblower
 * Changes to MINA-related classes
 *
 * Revision 1.1  2005/03/09 08:52:25  jonblower
 * Initial import of MINA-related classes
 *
 */
public class StyxProtocolHandler implements ProtocolHandler
{
    
    public void sessionOpened( ProtocolSession session )
    {
        System.err.println( session.getRemoteAddress() + " OPENED" );
    }
    
    public void sessionClosed( ProtocolSession session )
    {
        System.err.println( session.getRemoteAddress() + " CLOSED" );
    }
    
    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
        System.err.println( session.getRemoteAddress() + " IDLE(" + status
            + ")" );
    }
    
    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        System.err.println( session.getRemoteAddress() + " EXCEPTION" );
        cause.printStackTrace( System.err );
        
        // Close connection when unexpected exception is caught.
        session.close();
    }
    
    public void messageReceived( ProtocolSession session, Object message )
    {
        System.err.println( session.getRemoteAddress() + " RCVD: " + message );
        session.write(new RversionMessage(8192, "9P2000"));
    }
    
    public void messageSent( ProtocolSession session, Object message )
    {
        // Invoked the reversed string is actually written to socket channel.
        System.err.println( session.getRemoteAddress() + " SENT: " + message );
    }
}
