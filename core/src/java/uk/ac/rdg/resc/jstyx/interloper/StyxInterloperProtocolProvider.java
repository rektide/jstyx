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

import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;

import uk.ac.rdg.resc.jstyx.messages.StyxCodecFactory;

/**
 * Protocol provider for Styx interloper; handles client and server part
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/03/11 14:01:59  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.2.2  2005/03/10 18:29:41  jonblower
 * Changed to use StyxCodecFactory
 *
 * Revision 1.1.2.1  2005/03/10 14:30:03  jonblower
 * Initial import
 *
 */
public class StyxInterloperProtocolProvider implements ProtocolProvider
{
    
    public static final int CLIENT_MODE = 0;
    private ProtocolSession serverSession;
    
    public static final int SERVER_MODE = 1;
    private InetSocketAddress destSockAddr;
    
    private InterloperListener listener;
    
    private int mode;
    
    /**
     * Constructor for client mode
     */
    public StyxInterloperProtocolProvider(ProtocolSession serverSession,
        InterloperListener listener)
    {
        this.mode = CLIENT_MODE;
        this.serverSession = serverSession;
        this.listener = listener;
    }
    
    /**
     * Constructor for server mode
     */
    public StyxInterloperProtocolProvider(InetSocketAddress destSockAddr,
        InterloperListener listener)
    {
        this.mode = SERVER_MODE;
        this.destSockAddr = destSockAddr;
        this.listener = listener;
    }
    
    public ProtocolCodecFactory getCodecFactory()
    {
        return StyxCodecFactory.getInstance();
    }
    
    public ProtocolHandler getHandler()
    {
        if (this.mode == CLIENT_MODE)
        {
            return new StyxInterloperClientProtocolHandler(this.serverSession,
                this.listener);
        }
        else
        {
            return new StyxInterloperServerProtocolHandler(this.destSockAddr,
                this.listener);
        }
    }
    
}
