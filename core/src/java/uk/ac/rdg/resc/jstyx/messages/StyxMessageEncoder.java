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

package uk.ac.rdg.resc.jstyx.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.common.IoSession;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;

/**
 * Encoder for StyxMessages, used by MINA framework
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/03/16 17:56:22  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.2  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.2.2  2005/03/10 18:28:44  jonblower
 * Made decoder and encoder package-private
 *
 * Revision 1.1.2.1  2005/03/10 11:26:49  jonblower
 * Moved from mina to messages package
 *
 * Revision 1.2  2005/03/09 16:58:42  jonblower
 * Changes to MINA-related classes
 *
 * Revision 1.1  2005/03/09 08:52:25  jonblower
 * Initial import of MINA-related classes
 *
 */
class StyxMessageEncoder implements ProtocolEncoder
{
    public void encode( IoSession session, Object message,
        ProtocolEncoderOutput out )
        throws ProtocolCodecException
    {
        if (!(message instanceof StyxMessage))
        {
            // This shouldn't happen
            throw new ProtocolCodecException("message was not a StyxMessage");
        }
        StyxMessage styxMsg = (StyxMessage)message;
        out.write(styxMsg.encode());
    }
    
    public void dispose(IoSession session)
    {
    }
}
