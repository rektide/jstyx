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

import uk.ac.rdg.resc.jstyx.StyxBuffer;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Message sent to write data to a file
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:30  jonblower
 * Initial revision
 *
 */
public class TwriteMessage extends StyxMessage
{
    
    private long fid;     // The fid to write data to
    private ULong offset; // The offset in the file to which to write data
    private long count;   // The number of bytes to write to the file
    private ByteBuffer data;  // Buffer containing the data to write to the file
    
    /** 
     * Creates a new TwriteMessage. This constructor will be called by the
     * MessageRecognizer.
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TwriteMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Twrite";
    }
    
    public TwriteMessage(long fid, ULong offset, long count, ByteBuffer data)
    {
        this(0, 118, 0); // The tag and length will be set later
        this.fid = fid;
        this.offset = offset;
        this.count = count;
        this.data = data;
        this.length = super.HEADER_LENGTH + 4 + 8 + 4 + this.count;
    }
    
    /**
     * This will write all the remaining bytes in the buffer to the file
     */
    public TwriteMessage(long fid, ULong offset, ByteBuffer data)
    {
        this(fid, offset, data.remaining(), data);
    }
    
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        this.fid = buf.getUInt();
        this.offset = buf.getULong();
        this.count = buf.getUInt();
        this.data = ByteBuffer.wrap(buf.getData((int)this.count));
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        buf.putUInt(this.fid).putULong(this.offset).putUInt(this.count).putData(this.data, this.count);
        return true;
    }
    
    public long getFid()
    {
        return this.fid;
    }
    
    public ULong getOffset()
    {
        return this.offset;
    }
    
    public long getCount()
    {
        return this.count;
    }
    
    public ByteBuffer getData()
    {
        return this.data;
    }
    
    protected String getElements()
    {
        StringBuffer s = new StringBuffer(", " + this.fid + ", " + this.offset + ", " + this.count);
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
