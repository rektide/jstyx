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

import uk.ac.rdg.resc.jstyx.types.Qid;

/**
 * Message returned by the server in response to a TopenMessage
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/03/11 14:02:15  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.1  2005/03/10 11:50:59  jonblower
 * Changed to fit with MINA framework
 *
 * Revision 1.2  2005/02/24 07:44:43  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:26  jonblower
 * Initial import
 *
 */
public class RauthMessage extends StyxMessage
{
    
    private Qid aqid;  // An aqid of type QTAUTH that represents a file that can
                       // be read and written to execute the authentication
                       // protocol (note that the authentication protocol is
                       // outside the scope of Styx)
    
    /** 
     * Creates a new RversionMessage 
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public RauthMessage(int length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Rauth";
    }
    
    public RauthMessage(Qid aqid)
    {
        this(20, 103, 0); // The tag is set later
        this.aqid = aqid;
    }
    
    protected final void decodeBody(StyxBuffer buf)
    {
        this.aqid = buf.getQid();  
    }
    
    protected final void encodeBody(StyxBuffer buf)
    {
        buf.putQid(this.aqid);
    }

    public Qid getAQid()
    {
        return this.aqid;
    }

    public void setAQid(Qid aqid)
    {
        this.aqid = aqid;
    }
    
    protected String getElements()
    {
        return ", " + this.aqid;
    }
    
    public String toFriendlyString()
    {
        return "aqid: " + this.aqid.toFriendlyString();
    }
    
}
