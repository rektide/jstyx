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

import uk.ac.rdg.resc.jstyx.StyxBuffer;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Decoder for StyxMessages, used by MINA framework
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
public class StyxMessageDecoder implements ProtocolDecoder
{
    
    private StyxMessage message; // Message that's in the process of being read
    
    private ByteBuffer headerBuf; // Buffer to hold header info
    
    public StyxMessageDecoder()
    {
        this.message = null;
        // Create a buffer to hold header information.
        // Note that we might actually get a buffer with more than the requested
        // number of bytes in!
        this.headerBuf = ByteBuffer.allocate(StyxUtils.HEADER_LENGTH);
    }
    
    public void decode(ProtocolSession session, ByteBuffer in,
        ProtocolDecoderOutput out)
        throws ProtocolViolationException
    {
        while(in.hasRemaining())
        {
            if (this.message == null)
            {
                // We're still reading the header of the message.
                // Calculate the number of header bytes that we can read
                // Note that we can't rely on this.headerBuf.remaining() because
                // the buffer could be bigger than we have requested
                int headerLeft = StyxUtils.HEADER_LENGTH - this.headerBuf.position();
                int headerBytesToRead = headerLeft < in.remaining() ? headerLeft
                    : in.remaining();
                // Read the header bytes and write to the header buffer
                byte[] bytes = new byte[headerBytesToRead];
                in.get(bytes);
                this.headerBuf.put(bytes);
                if (this.headerBuf.position() < StyxUtils.HEADER_LENGTH)
                {
                    // We haven't got all the header, but we must have read all
                    // of the input buffer.
                    return;
                }
                // If we've got this far, we must have a complete header
                this.headerBuf.flip();
                // Get the message length, type and tag. Wrap the buffer as a
                // StyxBuffer to make this easier
                StyxBuffer styxBuf = new StyxBuffer(this.headerBuf.buf());
                long length = styxBuf.getUInt();
                if (length > Integer.MAX_VALUE)
                {
                    // This should only happen due to a bug
                    throw new ProtocolViolationException("Illegal message length ("
                        + length + ")");
                }
                if ((int)length < StyxUtils.HEADER_LENGTH)
                {
                    throw new ProtocolViolationException("Got message length "
                        + length + "; Styx messages must be at least " +
                        StyxUtils.HEADER_LENGTH + " bytes long");
                }
                // MINA's ByteBuffer doesn't have a getUByte() equivalent
                int type = styxBuf.getUByte();
                int tag = styxBuf.getUShort();
                this.message = StyxMessage.createStyxMessage((int)length, type, tag);
            }
            
            // We are now ready to read the message body
            boolean completeMessage = this.message.readBytesFrom(in);
            if (completeMessage)
            {
                // We have read a complete message. Output it, then reset the
                // state of this class, ready for a new message
                out.write(message);
                this.message = null;
                this.headerBuf.clear();
            }
        }
    }
    
}
