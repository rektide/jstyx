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
 * Message returned by the server in response to a TstatMessage
 * @todo Implement toFriendlyString() method
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/24 07:44:43  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:27  jonblower
 * Initial import
 *
 */
public class RstatMessage extends StyxMessage
{
    
    private DirEntry dirEntry; // The attributes of the file that was queried
    
    /** 
     * Creates a new RversionMessage 
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public RstatMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Rstat";
    }
    
    public RstatMessage(DirEntry dirEntry)
    {
        this(0, 125, 0); // The tag and length are set later
        this.dirEntry = dirEntry;
        this.length = super.HEADER_LENGTH + 2 + this.dirEntry.getSize();
    }
    
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        int dirEntrySize = buf.getUShort(); // The size of the whole DirEntry, redundant
        this.dirEntry = buf.getDirEntry();     
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        buf.putUShort(this.dirEntry.getSize()).putDirEntry(this.dirEntry);
        return true;
    }
    
    public DirEntry getDirEntry()
    {
        return this.dirEntry;
    }
    
    protected String getElements()
    {
        return ", " + this.dirEntry;
    }
    
}
