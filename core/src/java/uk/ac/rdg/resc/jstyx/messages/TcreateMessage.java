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
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Message sent to create and open a file on a Styx server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
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
    public TcreateMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Tcreate";
    }
    
    public TcreateMessage(long fid, String fileName, String permissions, boolean isDirectory, int mode)
    {
        this(0, 114, 0); // The length and tag will be added later
        this.fid = fid;
        this.fileName = fileName; // TODO: check validity of name?
        this.perm = this.parsePermissions(permissions, isDirectory);
        this.mode = mode;
        
        // Get the length of the name string
        int nameLen = StyxUtils.strToUTF8(name).length;
        // Set the length of the message
        this.length = super.HEADER_LENGTH + 4 + 2 + nameLen + 4 + 1;
    }
    
    /**
     * Parses a permission string (e.g. "755") into a long number to be
     * included in the message
     * @throws IllegalArgumentException if the permissions string is invalid
     */
    private long parsePermissions(String permissions, boolean isDirectory)
    {
        if (permissions.length() != 3)
        {
            throw new IllegalArgumentException("The permissions string must be 3 characters long");
        }
        try
        {
            long perm = Integer.parseInt(permissions, 8); // parse as an octal number
            if (isDirectory)
            {
                perm |= StyxUtils.DMDIR; // Set the directory bit
            }
            return perm;
        }
        catch (NumberFormatException nfe)
        {
            throw new IllegalArgumentException(permissions + " is not a valid permission string");
        }
    }
    
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        this.fid = buf.getUInt();
        this.fileName = buf.getString();
        this.perm = buf.getUInt();
        this.mode = buf.getUByte();
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        buf.putUInt(this.fid).putString(this.name).putUInt(this.perm).putUByte(this.mode);
        return true;
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
