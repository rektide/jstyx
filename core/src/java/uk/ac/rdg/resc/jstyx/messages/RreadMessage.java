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

import java.nio.ByteBuffer;

import org.apache.mina.protocol.ProtocolViolationException;

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
    
    private int count; // The amount of data in bytes in this message (TODO: should be an int?)
    private ByteBuffer data; // The actual data
    
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
    
    public RreadMessage(int count, ByteBuffer data)
    {
        this(0, (short)117, 0); // We'll set the length and tag later
        this.setData(count, data);
    }
    
    /**
     * Creates an RreadMessage from the data remaining in the buffer
     */
    public RreadMessage(ByteBuffer data)
    {
        this(data.remaining(), data);
    }
    
    /**
     * Creates an RreadMessage from the given byte array
     * @todo what if the byte array is too long for the message?
     */
    public RreadMessage(byte[] bytes)
    {
        this(bytes.length, ByteBuffer.wrap(bytes));
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
        this.data = ByteBuffer.wrap(buf.getData(this.count));
    }
    
    protected final void encodeBody(StyxBuffer buf)
    {
        // Return some of the contents of the file
        buf.putUInt(this.count).putData(this.data, this.count);
    }

    /**
     * Sets the data and the number of bytes to add to the message
     * @throws IllegalArgumentException if data.remaining() < count
     */
    private void setData(int count, ByteBuffer data)
    {
        if (data.remaining() < count)
        {
            throw new IllegalArgumentException("There is not enough space in the data buffer for "
                + count + " bytes: data.remaining() = " + data.remaining());
        }
        this.count = count;
        this.data = data;
        this.length = StyxUtils.HEADER_LENGTH + 4 + this.count;
    }
    
    /**
     * @return the data contained in this message
     */
    public ByteBuffer getData()
    {
        return this.data;
    }
    
    /**
     * @return the number of bytes returned in the message (i.e. the payload size)
     */
    public int getCount()
    {
        return this.count;
    }
    
    protected String getElements()
    {
        StringBuffer s = new StringBuffer(", " + this.count + ", ");
        s.append(StyxUtils.getDataSummary(30, this.data));
        return s.toString();
    }
    
    public String toFriendlyString()
    {
        return "count: " + this.count + ", " + StyxUtils.getDataSummary(30, this.data);
    }
    
}
