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

import org.apache.mina.protocol.ProtocolViolationException;
import org.apache.mina.protocol.ProtocolEncoderOutput;

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
    private int count;   // The number of bytes to write to the file
    
    private ByteBuffer data; // Buffer containing the data to write to the file.
                             // This is only used if we have decoded this message
                             // from bytes that have been read over the network
                             // (i.e. only used by server programs)
    private int dataPos;     // Stores the location of the first byte of payload
                             // data in the ByteBuffer
    
    private byte[] bytes; // The raw data bytes (only used if we have constructed
                          // this Tmessage from scratch before sending it).  It is
                          // not used if this message is decoded from bytes that
                          // have been read over the network. I.e. this is only
                          // used by client programs.
    private int pos;      // If the byte array is filled, this is the index of
                          // the first byte in the array to write
    
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
        this.bytes = null;
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
     */
    public TwriteMessage(long fid, ULong offset, byte[] bytes, int pos, int count)
    {
        this(0, (short)118, 0); // The tag and length will be set later
        this.fid = fid;
        this.offset = offset;
        this.bytes = bytes; // We don't create a ByteBuffer for these bytes
        this.pos = pos;
        this.count = count;
        this.length = StyxUtils.HEADER_LENGTH + 4 + 8 + 4 + this.count;
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
        
        if (buf.remaining() == this.count)
        {
            // The buffer contains the payload bytes and no more. We can simply
            // keep a reference to this buffer instead of copying it. This happens
            // frequently in practice, so this could be a significant efficiency
            
            // Increment the reference count for the underlying ByteBuffer, so that
            // it is not reused prematurely.
            buf.getBuffer().acquire();
            this.data = buf.getBuffer();
            
            // Remember the position of this buffer
            this.dataPos = this.data.position();
        
            // We need to set the position of the input buffer to its limit to make
            // sure that we don't keep reading from this buffer. We'll set the position
            // to zero when we get the buffer using getData();
            this.data.position(this.data.limit());
        }
        else
        {
            // We need to copy the data in this buffer.
            this.data = ByteBuffer.allocate(this.count);
            byte[] b = buf.getData(this.count);
            this.data.put(b);
            this.dataPos = 0;
        }
    }
    
    /**
     * Called by StyxMessageEncoder to send a message to the output. We need to 
     * override StyxMessage's default implementation because we may already have
     * the payload data as a ByteBuffer (for example, if we are forwarding a
     * message in the StyxInterloper).  Note that, after calling this method,
     * the data in the ByteBuffer are no longer valid (so calling this.getData()
     * might not return valid data).
     */
    public void write(ProtocolEncoderOutput out) throws ProtocolViolationException
    {
        if (this.data == null)
        {
            // We need to write all the data to the ByteBuffer, so just do 
            // what we would normally do
            super.write(out);
        }
        else
        {
            // We already have the bulk of the data in a ByteBuffer, so we don't
            // need to copy it to a new one.
            ByteBuffer payload = this.getData();
            
            // Allocate a buffer for everything but the payload
            this.buf = ByteBuffer.allocate(this.length - payload.remaining());
            // Wrap as a StyxBuffer
            StyxBuffer styxBuf = new StyxBuffer(this.buf);
            // Encode everything but the payload, then flip the buffer
            styxBuf.putUInt(this.length).putUByte(this.type).putUShort(this.tag);
            styxBuf.putUInt(this.fid).putULong(this.offset).putUInt(this.count);
            this.buf.flip();
            
            // Write everything but the payload
            out.write(this.buf);
            // Now write the payload; this releases the buffer so subsequent
            // calls to getData() will not return valid results
            out.write(payload);
        }
    }
    
    /**
     * Writes the message into the given StyxBuffer. This method should only be
     * called by client programs, i.e. those that have created the TwriteMessage
     * "from scratch" from a byte array. Otherwise, this will throw an 
     * IllegalStateException
     */
    protected final void encodeBody(StyxBuffer buf)
    {
        buf.putUInt(this.fid).putULong(this.offset).putUInt(this.count);
        if (this.bytes != null)
        {
            buf.put(this.bytes, this.pos, this.count);
        }
        else
        {
            // If this.data were non-null, we wouldn't have called this method.
            throw new IllegalStateException("This RreadMessage contains no data");
        }
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
     * @return the data contained in this message (i.e. the message payload).
     * The position and limit of the ByteBuffer will be set correctly, but
     * please note that the position will probably not be zero!  This ByteBuffer
     * might actually contain all the raw data for the TwriteMessage. When you have
     * finished with the data, call buf.release() to return the buffer to the
     * pool.  After releasing the buffer, calling this method will have undefined
     * consequences (the data returned might not be the data we expect
     * because the buffer might have been reused).
     *
     * This method should only be called by servers (i.e. programs that 
     * interpret TwriteMessages that arrive over the network).  If the 
     * TwriteMessage has been created "from scratch" using a byte array, this 
     * method will throw an IllegalStateException.
     */
    public ByteBuffer getData()
    {
        if (this.data == null)
        {
            throw new IllegalStateException("Data buffer is null");
        }
        this.data.position(this.dataPos);
        return this.data;
    }
    
    protected String getElements()
    {
        StringBuffer s = new StringBuffer(", " + this.fid + ", " + this.offset +
            ", " + this.count + ", ");
        if (this.data == null)
        {
            s.append(StyxUtils.getDataSummary(30, this.bytes));
        }
        else
        {
            s.append(StyxUtils.getDataSummary(30, this.data));
        }
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
        if (this.data == null)
        {
            StyxUtils.getDataSummary(30, this.bytes);
        }
        else
        {
            StyxUtils.getDataSummary(30, this.data);
        }
        return s.toString();
    }
    
}
