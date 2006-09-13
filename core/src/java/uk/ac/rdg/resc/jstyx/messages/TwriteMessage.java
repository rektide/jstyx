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

import org.apache.mina.filter.codec.ProtocolCodecException;

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
 * Revision 1.10  2005/11/03 21:48:24  jonblower
 * Created version based on new RreadMessage (robust and simple, maybe not maximally efficient)
 *
 * Revision 1.9  2005/03/22 17:48:27  jonblower
 * Removed debug code that tracked ByteBuffer allocation
 *
 * Revision 1.8  2005/03/21 17:57:11  jonblower
 * Trying to fix ByteBuffer leak in SGS server
 *
 * Revision 1.7  2005/03/17 07:29:36  jonblower
 * Fixed bug in getData()
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
    
    private ByteBuffer data; // Contains the data
    private int pos; // Position of the first byte of data in the buffer
    private int count; // Number of data bytes in the buffer
    
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
    
    /**
     * Creates a new TwriteMessage that will write the given bytes to the 
     * file described by the given fid at the given offset. N.B. this will
     * use all of the bytes in the given array.
     */
    public TwriteMessage(long fid, ULong offset, byte[] bytes)
    {
        this(fid, offset, bytes, 0, bytes.length);
    }
    
    /**
     * Creates a new TwriteMessage that will write the given bytes to the 
     * file described by the given fid at the given offset.
     * @throws IllegalArgumentException if <code>pos + count > bytes.length</code>
     */
    public TwriteMessage(long fid, ULong offset, byte[] bytes, int pos, int count)
    {
        this(0, (short)118, 0); // The tag and length will be set later
        this.fid = fid;
        this.offset = offset;
        if (pos + count > bytes.length)
        {
            throw new IllegalArgumentException("Not enough bytes in the given byte array:" +
                " pos = " + pos + ", count = " + count + ", length = " + bytes.length);
        }
        this.data = ByteBuffer.wrap(bytes, pos, count);
        this.pos = pos;
        this.count = count;
        this.length = StyxUtils.HEADER_LENGTH + 4 + 8 + 4 + this.count;
    }
    
    /**
     * Simple decodeBody that always makes a copy of the data in the incoming
     * StyxBuffer.  May not be very efficient (see decodeBody2()).
     */
    protected final void decodeBody(StyxBuffer buf)
        throws ProtocolCodecException
    {
        this.fid = buf.getUInt();
        this.offset = buf.getULong();
        long n = buf.getUInt();
        if (n < 0 || n > Integer.MAX_VALUE)
        {
            throw new ProtocolCodecException("Payload of Twrite message " +
                "cannot be less than 0 or greater than Integer.MAX_VALUE bytes");
        }
        this.count = (int)n; // We know this cast must be safe
        
        // We need to copy the data in this buffer.
        byte[] b = buf.getData(this.count);
        this.data = ByteBuffer.wrap(b);
        this.pos = 0;
    }
    
    /**
     * An attempt to increase the efficiency of the decoding by not copying
     * bytes of data unnecessarily.  May cause bugs in some situations.  Will
     * reinstate this message if it is proved that it can increase efficiency.
     * @throws ProtocolCodecException if the payload of the message is more
     * than Integer.MAX_VALUE
     */
    protected final void decodeBody2(StyxBuffer buf)
        throws ProtocolCodecException
    {
        this.fid = buf.getUInt();
        this.offset = buf.getULong();
        long n = buf.getUInt();
        if (n < 0 || n > Integer.MAX_VALUE)
        {
            throw new ProtocolCodecException("Payload of Twrite message " +
                "cannot be less than 0 or greater than Integer.MAX_VALUE bytes");
        }
        this.count = (int)n; // We know this cast must be safe
        
        if (buf.remaining() == this.count)
        {
            // The buffer contains the payload bytes and no more. We can simply
            // keep a reference to this buffer instead of copying it. This happens
            // frequently in practice, so this could be a significant efficiency
            this.data = buf.getBuffer();
            
            // Increment the reference count for the underlying ByteBuffer, so that
            // it is not reused prematurely.
            this.data.acquire();
            
            // Remember the position of this buffer
            this.pos = this.data.position();
        
            // We need to set the position of the input buffer to its limit to make
            // sure that we don't keep reading from this buffer. We'll set the position
            // to zero when we get the buffer using getData();
            this.data.position(this.data.limit());
        }
        else
        {
            // We need to copy the data in this buffer.
            byte[] b = buf.getData(this.count);
            this.data = ByteBuffer.wrap(b);
            this.pos = 0;
        }
    }
    
    /**
     * Writes the message into the given StyxBuffer.
     */
    protected final void encodeBody(StyxBuffer buf)
    {
        buf.putUInt(this.fid).putULong(this.offset).putUInt(this.count).putData(this.getData());
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
    
    /**
     * @return the data contained in this message as a MINA ByteBuffer.  Makes
     * sure that the position and limit of the buffer are set correctly
     */
    public ByteBuffer getData()
    {
        // Set the position and limit correctly in case we have changed it
        // elsewhere
        this.data.position(this.pos).limit(this.pos + this.count);
        return this.data;
    }
    
    protected String getElements()
    {
        StringBuffer s = new StringBuffer(", " + this.fid + ", " + this.offset +
            ", " + this.count + ", ");
        s.append(StyxUtils.getDataSummary(30, this.getData()));
        return s.toString();
    }
    
    public String toFriendlyString()
    {
        StringBuffer s = new StringBuffer("fid: ");
        s.append(this.fid);
        s.append(", offset: ");
        s.append(this.offset);
        s.append(", count: ");
        s.append(this.count);
        s.append(", ");
        s.append(StyxUtils.getDataSummary(30, this.getData()));
        return s.toString();
    }
    
}
