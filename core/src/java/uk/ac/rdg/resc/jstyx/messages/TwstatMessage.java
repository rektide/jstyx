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
import uk.ac.rdg.resc.jstyx.types.DirEntry;

/**
 * Message sent to change the attributes of a file
 * @todo Implement getFriendlyString()
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/24 07:44:44  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:30  jonblower
 * Initial import
 *
 */
public class TwstatMessage extends StyxMessage
{
    
    private long fid;  // Fid chosen by client to represent root of file tree
    private DirEntry dirEntry; // File attributes
    
    /** 
     * Creates a new TwstatMessage. This constructor will be called by the
     * MessageRecognizer.
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TwstatMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Twstat";
    }
    
    public TwstatMessage(long fid, DirEntry dirEntry)
    {
        this(0, 126, 0); // The length and tag will be added later
        this.fid = fid;
        this.dirEntry = dirEntry;
        // Set the length of the message
        this.length = super.HEADER_LENGTH + 4 + 2 + this.dirEntry.getSize();
    }
        
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        this.fid = buf.getUInt();
        int sizeOfDirEntry = buf.getUShort(); // size of DirEntry, ignored
        this.dirEntry = buf.getDirEntry();
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        buf.putUInt(this.fid).putUShort(this.dirEntry.getSize()).putDirEntry(this.dirEntry);
        return true;
    }
    
    public long getFid()
    {
        return this.fid;
    }
    
    public DirEntry getDirEntry()
    {
        return this.dirEntry;
    }
    
    protected String getElements()
    {
        return ", " + this.fid + ", " + this.dirEntry;
    }
    
}
