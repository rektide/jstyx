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
import uk.ac.rdg.resc.jstyx.types.Qid;

import java.util.Vector;

/**
 * Message returned by the server in response to a TwalkMessage
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:28  jonblower
 * Initial revision
 *
 */
public class RwalkMessage extends StyxMessage
{
 
    private Vector qids; // Vector of Qids, one for each successful walk of a path element
    
    /** 
     * Creates a new RwalkMessage 
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public RwalkMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Rwalk";
        this.qids = new Vector(5, 2);
    }
    
    public RwalkMessage(Qid[] qids)
    {
        this(0, 111, 0);
        for (int i = 0; i < qids.length; i++)
        {
            this.putQid(qids[i]);
        }
        this.setLength();
    }
    
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        // Read the number of successful walks
        int numQids = buf.getUShort();
        for (int i = 0; i < numQids; i++)
        {
            this.putQid(buf.getQid());
        }
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        // Write the number of qids (successful walks)
        buf.putUShort(this.qids.size());
        // Write the qids
        for (int i = 0; i < this.qids.size(); i++)
        {
            buf.putQid((Qid)this.qids.get(i));
        }
        return true;
    }
    
    public void putQid(Qid qid)
    {
        this.qids.add(qid);
        this.setLength();
    }
    
    /**
     * Sets the correct length of this message
     */
    private void setLength()
    {
        this.length = super.HEADER_LENGTH + 2 + (13 * this.qids.size());        
    }
    
    /**
     * @return The number of successful walks made in the file traversal
     */
    public int getNumSuccessfulWalks()
    {
        return this.qids.size();
    }
    
    protected String getElements()
    {
        String s = ", " + this.qids.size();
        for (int i = 0; i < this.qids.size(); i++)
        {
            s += ", " + this.qids.get(i);
        }
        return s;
    }
    
}
