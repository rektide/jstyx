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

package uk.ac.rdg.resc.jstyx.mina;

import uk.ac.rdg.resc.jstyx.StyxBuffer;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Message sent to establish a connection
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/09 16:58:42  jonblower
 * Changes to MINA-related classes
 *
 * Revision 1.2  2005/02/24 07:44:43  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:28  jonblower
 * Initial import
 *
 */
public class TattachMessage extends StyxMessage
{
    
    private long fid;  // Fid chosen by client to represent root of file tree
    private long afid; // Fid previously established by an auth message
    private String uname; // The username supplied by the client
    private String aname; // The file tree that the user wishes to access
    
    /** 
     * Creates a new TattachMessage.
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TattachMessage(int length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Tattach";
    }
    
    public TattachMessage(long fid, long afid, String uname, String aname)
    {
        this(0, 104, 0); // The length and tag will be added later
        this.fid = fid;
        this.afid = afid;
        this.uname = uname;
        this.aname = aname;
        // Get the length of the strings
        int unameLen = StyxUtils.strToUTF8(uname).length;
        int anameLen = StyxUtils.strToUTF8(aname).length;
        // Set the length of the message
        this.length = StyxUtils.HEADER_LENGTH + 4 + 4 + 2 + unameLen + 2 + anameLen;
    }
    
    public TattachMessage(long fid, String uname)
    {
        this(fid, StyxUtils.NOFID, uname, "");
    }
    
    protected final void decodeBody(StyxBuffer buf)
    {
        this.fid = buf.getUInt();
        this.afid = buf.getUInt();
        this.uname = buf.getString();
        this.aname = buf.getString();
    }
    
    protected final void encodeBody(StyxBuffer buf)
    {
        buf.putUInt(this.fid).putUInt(this.afid).putString(this.uname).putString(this.aname);
    }
    
    protected String getElements()
    {
        return ", " + this.fid + ", " + this.afid + ", " + this.uname +
            ", " + this.aname;
    }

    public long getFid()
    {
        return fid;
    }

    public long getAfid()
    {
        return afid;
    }

    public String getUname()
    {
        return uname;
    }

    public String getAname()
    {
        return aname;
    }
    
    public String toFriendlyString()
    {
        return "fid: " + this.fid + ", auth fid: " + this.afid + 
            ", user: " + this.uname + ", aname: " + this.aname;
    }
}
