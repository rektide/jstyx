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

import java.nio.ByteOrder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Decoder for StyxMessages, used by MINA framework
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/03/15 16:56:19  jonblower
 * Changed to allow re-use of ByteBuffers once message is finished with
 *
 * Revision 1.3  2005/03/15 09:03:58  jonblower
 * Now uses MINA's ByteBuffer to read header info, not StyxBuffer
 *
 * Revision 1.2  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.2.3  2005/03/10 18:28:44  jonblower
 * Made decoder and encoder package-private
 *
 * Revision 1.1.2.2  2005/03/10 11:50:14  jonblower
 * Removed reference to StyxBuffer (which is now in this package)
 *
 * Revision 1.1.2.1  2005/03/10 11:26:49  jonblower
 * Moved from mina to messages package
 *
 * Revision 1.2.2.1  2005/03/09 19:44:15  jonblower
 * Changes concerned with migration to MINA
 *
 * Revision 1.2  2005/03/09 16:58:42  jonblower
 * Changes to MINA-related classes
 *
 * Revision 1.1  2005/03/09 08:52:25  jonblower
 * Initial import of MINA-related classes
 *
 */
class StyxMessageDecoder implements ProtocolDecoder
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
        this.headerBuf.order(ByteOrder.LITTLE_ENDIAN);
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
                // Get the message length
                long length = this.headerBuf.getUnsignedInt();
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
                // Get the message type and tag
                short type = this.headerBuf.getUnsigned();
                int tag = this.headerBuf.getUnsignedShort();
                // Create the message; this returns a StyxMessage of the correct
                // type (e.g. TversionMessage, RwalkMessage etc)
                this.message = StyxMessage.createStyxMessage((int)length, type, tag);
            }
            
            // We are now ready to read the message body
            boolean completeMessage = this.message.readBytesFrom(in);
            if (completeMessage)
            {
                // We have read a complete message. Output it, then reset the
                // state of this class, ready for a new message
                out.write(message);
                // We've finished with the message's underlying ByteBuffer now
                // (we will have kept a copy of any data we need)
                this.message.release();
                this.message = null;
                this.headerBuf.clear();
            }
        }
    }
    
}
