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

import org.apache.log4j.Logger;

import org.apache.mina.common.ByteBuffer;

import org.apache.mina.protocol.ProtocolViolationException;
import org.apache.mina.protocol.ProtocolEncoderOutput;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Message returned by the server in response to a TreadMessage
 * @todo: Use MINA's ByteBuffer
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.15  2005/11/03 16:02:54  jonblower
 * Created simplified version (less efficient, more reliable)
 *
 * Revision 1.13  2005/09/02 16:52:38  jonblower
 * Fixed bugs that caused message payload to be printed as empty string
 *
 * Revision 1.12  2005/08/30 16:29:40  jonblower
 * Minor update to comments
 *
 * Revision 1.11  2005/05/10 19:17:54  jonblower
 * Added dispose() method
 *
 * Revision 1.10  2005/05/10 08:02:06  jonblower
 * Changes related to implementing MonitoredFileOnDisk
 *
 * Revision 1.9  2005/03/22 17:48:27  jonblower
 * Removed debug code that tracked ByteBuffer allocation
 *
 * Revision 1.8  2005/03/21 17:57:10  jonblower
 * Trying to fix ByteBuffer leak in SGS server
 *
 * Revision 1.7  2005/03/18 13:56:00  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.6  2005/03/16 22:16:43  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.5  2005/03/16 17:56:22  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.4  2005/03/15 09:01:48  jonblower
 * Message type now stored as short, not int
 *
 * Revision 1.3  2005/03/11 14:02:15  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.2  2005/03/11 12:30:45  jonblower
 * Changed so that message payloads are always ints, not longs
 *
 * Revision 1.2.2.1  2005/03/10 11:50:59  jonblower
 * Changed to fit with MINA framework
 *
 * Revision 1.2  2005/02/24 07:44:43  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:27  jonblower
 * Initial import
 *
 */
public class RreadMessage extends StyxMessage
{

    private static final Logger log = Logger.getLogger(RreadMessage.class);
    
    private byte[] bytes; // Byte array containing the payload bytes
    private int pos; // position of the first byte of data in the array
    private int count; // number of data bytes in the array
    
    /** 
     * Creates a new RversionMessage 
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public RreadMessage(int length, short type, int tag)
    {
        super(length, type, tag);
        this.name = "Rread";
    }
    
    /**
     * Creates an RreadMessage from the given byte array
     */
    public RreadMessage(byte[] bytes)
    {
        this(bytes, 0, bytes.length);
    }
    
    /**
     * Creates an RreadMessage from the given byte array. This will return
     * <code>count</code> bytes, starting at position <code>pos</code> in the
     * given array
     * @throws IllegalArgumentException if <code>pos + count > bytes.length</code>
     */
    public RreadMessage(byte[] bytes, int pos, int count)
    {
        this(0, (short)117, 0); // We'll set the length and tag later
        if (pos + count > bytes.length)
        {
            throw new IllegalArgumentException("Not enough bytes in the given byte array:" +
                " pos = " + pos + ", count = " + count + ", length = " + bytes.length);
        }
        this.bytes = bytes;
        this.pos = pos;
        this.count = count;
        this.length = StyxUtils.HEADER_LENGTH + 4 + this.count;
        if (log.isDebugEnabled())
        {
            log.debug("Created RreadMessage from array with " + bytes.length
            + " bytes, pos = " + pos + ", count = " + count + ", length  = " + this.length);
        }
    }
    
    /**
     * Creates an RreadMessage from the given org.apache.mina.common.ByteBuffer.
     * The position and limit of the buffer must be set correctly.  This method
     * does <b>not</b> release the buffer: it just copies the data into a new array.
     * This method does not change the position or limit of the data buffer.
     */
    public RreadMessage(ByteBuffer data)
    {
        this(0, (short)117, 0); // We'll set the length and tag later
        int oldPos = data.position();
        this.bytes = new byte[data.remaining()];
        data.get(this.bytes);
        this.pos = 0;
        this.count = this.bytes.length;
        this.length = StyxUtils.HEADER_LENGTH + 4 + this.count;
        data.position(oldPos);
        if (log.isDebugEnabled())
        {
            log.debug("Created RreadMessage from buffer with pos = " +
                data.position() + ", limit = " + data.limit() +
                ", length  = " + this.length);
        }
    }
    
    /**
     * @throws ProtocolViolationException if the payload of the message is more
     * than Integer.MAX_VALUE
     */
    protected final void decodeBody(StyxBuffer buf)
        throws ProtocolViolationException
    {
        long n = buf.getUInt();
        if (n < 0 || n > Integer.MAX_VALUE)
        {
            throw new ProtocolViolationException("Payload of Rread message " +
                "cannot be less than 0 or greater than Integer.MAX_VALUE bytes");
        }
        this.count = (int)n; // We know this cast must be safe
        this.bytes = buf.getData(this.count);
        this.pos = 0;
    }
    
    /**
     * Writes the message into the given StyxBuffer.
     */
    protected final void encodeBody(StyxBuffer buf)
    {
        buf.putUInt(this.count).put(this.bytes, this.pos, this.count);
    }
    
    /**
     * @return the data contained in this message as a MINA ByteBuffer
     */
    public ByteBuffer getData()
    {
        return ByteBuffer.wrap(this.bytes, this.pos, this.count);
    }
    
    /**
     * @return the number of bytes returned in the message (i.e. the payload size)
     */
    public int getCount()
    {
        return this.count;
    }
    
    /**
     * This is called <b>after</b> the message has been sent (in
     * StyxServerProtocolHandler.messageSent().  This releases the ByteBuffer
     * holding the payload of the message, if one exists.
     */
    public void dispose()
    {
        this.bytes = null; // TODO: do we need this method anymore?
    }
    
    protected String getElements()
    {
        StringBuffer s = new StringBuffer(", " + this.count + ", ");
        s.append(StyxUtils.getDataSummary(30, this.bytes));
        return s.toString();
    }
    
    public String toFriendlyString()
    {
        StringBuffer s = new StringBuffer("count: ");
        s.append(this.count);
        s.append(", ");
        s.append(StyxUtils.getDataSummary(30, this.bytes));
        return s.toString();
    }
    
}
