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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * Decoder for StyxMessages, used by MINA framework
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/09 08:52:25  jonblower
 * Initial import of MINA-related classes
 *
 */
public class StyxMessageDecoder implements ProtocolDecoder
{
    
    private StyxMessage message;
    
    public StyxMessageDecoder()
    {
        System.err.println("Created new StyxMessageDecoder");
        this.message = null;
    }
    
    public void decode(ProtocolSession session, ByteBuffer in,
        ProtocolDecoderOutput out)
        throws ProtocolViolationException
    {
        System.err.println("Got " + in.remaining() + " bytes");
        while(in.hasRemaining())
        {
            if (this.message == null)
            {
                // This is the first time we've called this method
                this.message = new StyxMessage();
            }
            boolean completeMessage = this.message.readBuffer(in);
            if (completeMessage)
            {
                // We have read a complete message
                System.err.println("Read complete message of length " +
                    this.message.getLength());
                out.write(this.message);
                // Reset the state of this class
                this.message = null;
            }
            // There won't be any bytes remaining in the buffer.
            // Just do nothing and wait for more bytes to arrive.
        }
    }
    
}
