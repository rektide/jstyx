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

import net.gleamynode.netty2.MessageParseException;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.StyxBuffer;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Message returned by the server in response to a TreadMessage
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:27  jonblower
 * Initial revision
 *
 */
public class RreadMessage extends StyxMessage
{
    
    private long count; // The amount of data in bytes in this message (TODO: should be an int?)
    private ByteBuffer data; // The actual data
    
    /** 
     * Creates a new RversionMessage 
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public RreadMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Rread";
    }
    
    public RreadMessage(long count, ByteBuffer data)
    {
        this(0, 117, 0); // We'll set the length and tag later
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
    
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        this.count = buf.getUInt();
        this.data = ByteBuffer.wrap(buf.getData((int)this.count));
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        // Return some of the contents of the file
        buf.putUInt(this.count).putData(this.data, this.count);
        return true;
    }

    /**
     * Sets the data and the number of bytes to add to the message
     * @throws IllegalArgumentException if data.remaining() < count
     */
    private void setData(long count, ByteBuffer data)
    {
        if (data.remaining() < count)
        {
            throw new IllegalArgumentException("There is not enough space in the data buffer for "
                + count + " bytes: data.remaining() = " + data.remaining());
        }
        this.count = count;
        this.data = data;
        this.length = super.HEADER_LENGTH + 4 + this.count;
    }
    
    /**
     * @return the data contained in this message
     */
    public ByteBuffer getData()
    {
        return this.data;
    }
    
    /**
     * @return the number of bytes returned in the message
     */
    public long getCount()
    {
        return this.count;
    }
    
    protected String getElements()
    {
        StringBuffer s = new StringBuffer(", " + this.count);
        // Print the first few bytes of data
        byte[] bytes;
        synchronized(this.data)
        {
            int numBytes = this.data.remaining() < 30 ? this.data.remaining() : 30;
            bytes = new byte[numBytes];
            this.data.get(bytes);
            // Reset the position of the data buffer
            this.data.position(this.data.position() - numBytes);
        }
        s.append(", \"" + StyxUtils.utf8ToString(bytes) + "\"");
        int moreBytes = this.data.remaining() - bytes.length;
        if (moreBytes > 0)
        {
            s.append(" (plus " + moreBytes + " more bytes)");
        }
        return s.toString();
    }
    
}
