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

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Message sent to write data to a file
 * @todo make count an int, and use MINA's ByteBuffer
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/03/15 09:01:48  jonblower
 * Message type now stored as short, not int
 *
 * Revision 1.3  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.2  2005/03/11 12:30:46  jonblower
 * Changed so that message payloads are always ints, not longs
 *
 * Revision 1.2.2.1  2005/03/10 11:50:59  jonblower
 * Changed to fit with MINA framework
 *
 * Revision 1.2  2005/02/24 07:44:44  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:30  jonblower
 * Initial import
 *
 */
public class TwriteMessage extends StyxMessage
{
    
    private long fid;     // The fid to write data to
    private ULong offset; // The offset in the file to which to write data
    private int count;   // The number of bytes to write to the file
    private ByteBuffer data;  // Buffer containing the data to write to the file
    
    /** 
     * Creates a new TwriteMessage. This constructor will be called by the
     * MessageRecognizer.
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TwriteMessage(int length, short type, int tag)
    {
        super(length, type, tag);
        this.name = "Twrite";
    }
    
    public TwriteMessage(long fid, ULong offset, int count, ByteBuffer data)
    {
        this(0, (short)118, 0); // The tag and length will be set later
        this.fid = fid;
        this.offset = offset;
        this.count = count;
        this.data = data;
        this.length = StyxUtils.HEADER_LENGTH + 4 + 8 + 4 + this.count;
    }
    
    /**
     * This will write all the remaining bytes in the buffer to the file
     */
    public TwriteMessage(long fid, ULong offset, ByteBuffer data)
    {
        this(fid, offset, data.remaining(), data);
    }
    
    /**
     * @throws ProtocolViolationException if the payload of the message is more
     * than Integer.MAX_VALUE
     */
    protected final void decodeBody(StyxBuffer buf)
        throws ProtocolViolationException
    {
        this.fid = buf.getUInt();
        this.offset = buf.getULong();
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
        buf.putUInt(this.fid).putULong(this.offset).putUInt(this.count).putData(this.data, this.count);
    }
    
    public long getFid()
    {
        return this.fid;
    }
    
    public ULong getOffset()
    {
        return this.offset;
    }
    
    public int getCount()
    {
        return this.count;
    }
    
    public ByteBuffer getData()
    {
        return this.data;
    }
    
    protected String getElements()
    {
        StringBuffer s = new StringBuffer(", " + this.fid + ", " + this.offset +
            ", " + this.count + ", ");
        s.append(StyxUtils.getDataSummary(30, this.data));
        return s.toString();
    }
    
    public String toFriendlyString()
    {
        return "fid: " + this.fid + ", offset: " + this.offset + ", count: "
            + this.count + ", " + StyxUtils.getDataSummary(30, this.data);
    }
    
}
