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

import net.gleamynode.netty2.MessageParseException;

import uk.ac.rdg.resc.jstyx.StyxBuffer;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Message sent to read data from a file
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:29  jonblower
 * Initial revision
 *
 */
public class TreadMessage extends StyxMessage
{
    
    private long fid;     // The fid to read data from
    private ULong offset; // The offset in the file from which to read data
    private long count;   // The number of bytes to read from the file    
    
    /**
     * Creates a new TreadMessage. This constructor will be called by the
     * MessageRecognizer.
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TreadMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Tread";
    }
    
    public TreadMessage(long fid, ULong offset, long count)
    {
        this(23, 116, 0); // The tag will be set later
        this.fid = fid;
        this.offset = offset;
        this.count = count;
    }
    
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        this.fid = buf.getUInt();
        this.offset = buf.getULong();
        this.count = buf.getUInt();
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        buf.putUInt(this.fid).putULong(this.offset).putUInt(this.count);
        return true;
    }
    
    public long getFid()
    {
        return fid;
    }
    
    public void setFid(long fid)
    {
        this.fid = fid;
    }

    public ULong getOffset()
    {
        return offset;
    }

    public void setOffset(ULong offset)
    {
        this.offset = offset;
    }

    public long getCount()
    {
        return count;
    }

    public void setCount(long count)
    {
        this.count = count;
    }
    
    protected String getElements()
    {
        return ", " + this.fid + ", " + this.offset + ", " + this.count;
    }
    
}
