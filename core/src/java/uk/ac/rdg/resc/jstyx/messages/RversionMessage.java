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
 * Message returned by the server in response to a TversionMessage
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/24 07:44:43  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:28  jonblower
 * Initial import
 *
 */
public class RversionMessage extends StyxMessage
{
    
    private long maxMessageSize; // The maximum size of a message that will be 
                                 // sent on this connection by either party
    private String version;      // The version of the protocol 
    
    /** 
     * Creates a new RversionMessage 
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public RversionMessage(long length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Rversion";
    }
    
    public RversionMessage(long maxMessageSize, String version)
    {
        this(0, 101, 0);
        this.maxMessageSize = maxMessageSize;
        this.setVersion(version);
    }
    
    protected final boolean readBody(StyxBuffer buf) throws MessageParseException
    {
        // We have already checked that the whole message is ready to be read from
        // the buffer
        // Read the maximum message size
        this.maxMessageSize = buf.getUInt();
        // Read the version string
        this.version = buf.getString();        
        return true;
    }
    
    protected final boolean writeBody(StyxBuffer buf)
    {
        // We have already checked that there is room in the buffer for the 
        // message body
        // Write the max message size, then the version string
        buf.putUInt(this.maxMessageSize).putString(this.version);
        return true;
    }
    
    /**
     * @return The maximum size of a message that will be sent on this 
     * connection by either party
     */
    public long getMaxMessageSize()
    {
        return this.maxMessageSize;
    }
    
    /**
     * @param maxMessageSize The maximum size of a message that will be sent on this 
     * connection by either party
     */
    public void setMaxMessageSize(long maxMessageSize)
    {
        this.maxMessageSize = maxMessageSize;
    }
    
    /**
     * @return The version string (normally "9P2000")
     */
    public String getVersion()
    {
        return this.version;
    }
    
    /**
     * @param version The version string (normally "9P2000")
     */
    public void setVersion(String version)
    {
        this.version = version;
        int versionLen = StyxUtils.strToUTF8(version).length;
        this.length = super.HEADER_LENGTH + 4 + 2 + versionLen;
    }
    
    protected String getElements()
    {
        return ", " + this.maxMessageSize + ", " + this.version;
    }
    
    public String toFriendlyString()
    {
        return "Max msg size: " + this.maxMessageSize + " bytes, version = "
            + this.version;
    }
    
}
