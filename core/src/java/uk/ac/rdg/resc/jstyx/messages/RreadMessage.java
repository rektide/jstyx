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
    
    private int count; // The amount of data in bytes in this message
    
    private ByteBuffer data; // Buffer containing the data to write to the file.
                             // This is normally only used if we have decoded this message
                             // from bytes that have been read over the network
                             // (i.e. only used by client programs). It can also
                             // be used if we have created this message from a
                             // ByteBuffer (e.g. when reading from a FileOnDisk)
    private int dataPos;     // Stores the location of the first byte of payload
                             // data in the ByteBuffer
    
    private byte[] bytes; // The raw data bytes (only used if we have constructed
                          // this Tmessage from scratch before sending it).  It is
                          // not used if this message is decoded from bytes that
                          // have been read over the network.
    private int pos;      // If the byte array is filled, this is the index of
                          // the first byte in the array to write
    
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
    }
    
    /**
     * Creates an RreadMessage from the given org.apache.mina.common.ByteBuffer
     */
    public RreadMessage(ByteBuffer data)
    {
        this(0, (short)117, 0); // We'll set the length and tag later
        this.data = data; // TODO: should we call acquire()?
        this.pos = data.position();
        this.dataPos = this.pos;
        this.count = data.limit() - this.pos;
        this.length = StyxUtils.HEADER_LENGTH + 4 + this.count;
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
            // back to zero when we get the buffer using getData();
            this.data.position(this.data.limit());
        }
        else
        {
            // We need to copy the data in this buffer.
            log.debug("Need to make a copy of the data in this buffer: " +
                this.count + " bytes");
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
            
            // Allocate a buffer for the header. This buffer will get freed
            // when the header is written
            this.buf = ByteBuffer.allocate(11);
            // Wrap as a StyxBuffer
            StyxBuffer styxBuf = new StyxBuffer(this.buf);
            // Encode everything but the payload, then flip the buffer
            styxBuf.putUInt(this.length).putUByte(this.type).putUShort(this.tag).putUInt(this.count);
            this.buf.flip();
            
            synchronized(out)
            {
                // Write the header
                out.write(this.buf);
                // Now write the payload; this releases the buffer so subsequent
                // calls to getData() will not return valid results
                out.write(payload);
            }
        }
    }
    
    /**
     * Writes the message into the given StyxBuffer. This method should only be
     * called by server programs, i.e. those that have created the RreadMessage
     * "from scratch" from a byte array. Otherwise, this will throw an 
     * IllegalStateException
     */
    protected final void encodeBody(StyxBuffer buf)
    {
        buf.putUInt(this.count);
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
    
    /**
     * @return the data contained in this message (i.e. the message payload).
     * The position and limit of the ByteBuffer will be set correctly, but
     * please note that the position will probably not be zero!  This ByteBuffer
     * might actually contain all the raw data for the RreadMessage.  When you have
     * finished with the data, call buf.release() to return the buffer to the
     * pool.
     *
     * @throws IllegalStateException if the ByteBuffer is null.  The ByteBuffer
     * will be null if this RreadMessage has been created from a byte array.
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
        StringBuffer s = new StringBuffer("count: ");
        s.append(this.count);
        s.append(", ");
        if (this.data == null)
        {
            s.append(StyxUtils.getDataSummary(30, this.bytes));
        }
        else
        {
            s.append(StyxUtils.getDataSummary(30, this.getData()));
        }
        return s.toString();
    }
    
}
