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

/**
 * Message sent to enquire about the attributes of a file on a Styx server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:29  jonblower
 * Initial revision
 *
 */
public class TstatMessage extends StyxMessage
{
    
    private long fid; // The fid of the file
    
    /** 
     * Creates a new TstatMessage. This constructor will be called by the
     * MessageRecognizer.
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TstatMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Tstat";
    }
    
    /**
     * This constructor should be called when constructing a TstatMessage from
     * scratch
     */
    public TstatMessage(long fid)
    {
        this(0, 124, 0);
        this.fid = fid;
        this.length = super.HEADER_LENGTH + 4;
    }
    
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        // Read the fid of the file to enquire about
        this.fid = buf.getUInt();
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        // Write the fid of the file to enquire about
        buf.putUInt(this.fid);
        return true;
    }
    
    /**
     * @return The requested maximum size of a message that will 
     * be sent on this connection by either party
     */
    public long getFid()
    {
        return this.fid;
    }
    
    /**
     * @param maxMessageSize The requested maximum size of a message that will 
     * be sent on this connection by either party
     */
    public void setFid(long fid)
    {
        this.fid = fid;
    }
    
    protected String getElements()
    {
        return ", " + this.fid;
    }
    
}
