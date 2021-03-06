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

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Message sent to create and open a file on a Styx server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/05/25 15:39:02  jonblower
 * Bug fixes
 *
 * Revision 1.4  2005/03/15 09:01:48  jonblower
 * Message type now stored as short, not int
 *
 * Revision 1.3  2005/03/11 14:02:15  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.1  2005/03/10 11:50:59  jonblower
 * Changed to fit with MINA framework
 *
 * Revision 1.2  2005/02/24 07:44:43  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:29  jonblower
 * Initial import
 *
 */
public class TcreateMessage extends StyxMessage
{
    
    private long fid;    // Before the file is created, this fid represents the
                         // directory in which to create the file.  After the
                         // file is created, it represents the file itself.
    private String fileName; // Name for the file
    private long perm;   // Permissions for the file
    private int mode;    // When the file is opened, it will be in this mode
    
    
    /** 
     * Creates a new TcreateMessage. This constructor will be called by the
     * MessageRecognizer.
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TcreateMessage(int length, short type, int tag)
    {
        super(length, type, tag);
        this.name = "Tcreate";
    }
    
    public TcreateMessage(long fid, String fileName, int permissions, boolean isDirectory, int mode)
    {
        this(0, (short)114, 0); // The length and tag will be added later
        this.fid = fid;
        this.fileName = fileName; // TODO: check validity of name?
        this.perm = permissions;
        if (isDirectory)
        {
            this.perm |= StyxUtils.DMDIR; // Set the directory bit
        }
        this.mode = mode;
        
        // Get the length of the name string
        int nameLen = StyxUtils.strToUTF8(fileName).length;
        // Set the length of the message
        this.length = StyxUtils.HEADER_LENGTH + 4 + 2 + nameLen + 4 + 1;
    }
    
    protected final void decodeBody(StyxBuffer buf)
    {
        this.fid = buf.getUInt();
        this.fileName = buf.getString();
        this.perm = buf.getUInt();
        this.mode = buf.getUByte();
    }
    
    protected final void encodeBody(StyxBuffer buf)
    {
        buf.putUInt(this.fid).putString(this.fileName).putUInt(this.perm).putUByte(this.mode);
    }
    
    public long getFid()
    {
        return this.fid;
    }
    
    public String getFileName()
    {
        return this.fileName;
    }
    
    public long getPerm()
    {
        return this.perm;
    }
    
    public int getMode()
    {
        return this.mode;
    }
    
    protected String getElements()
    {
        return ", " + this.fid + ", " + this.fileName + ", " + this.perm + ", " + 
            this.mode;
    }
    
    public String toFriendlyString()
    {
        // TODO: make mode/perm more friendly-looking ("rwxr-xr-x" etc)
        return "fid: " + this.fid + ", name: " + this.fileName + ", perm: " +
            this.perm + ", mode: " + this.mode;
    }
    
}
