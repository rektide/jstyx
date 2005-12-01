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
 * Message sent to negotiate the protocol version and maximum message size of a 
 * Styx Connection
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/12/01 08:21:56  jonblower
 * Fixed javadoc comments
 *
 * Revision 1.4  2005/03/15 09:01:48  jonblower
 * Message type now stored as short, not int
 *
 * Revision 1.3  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.1  2005/03/10 11:50:59  jonblower
 * Changed to fit with MINA framework
 *
 * Revision 1.1  2005/03/09 16:58:42  jonblower
 * Changes to MINA-related classes
 *
 * Revision 1.2  2005/02/24 07:44:44  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:29  jonblower
 * Initial import
 *
 */
public class TversionMessage extends StyxMessage
{
    
    private long maxMessageSize; // The maximum size of a message that will be 
                                 // sent on this connection by either party
    private String version;      // The version of the protocol 
    
    /** 
     * Creates a new TversionMessage. This constructor will be called by the
     * StyxMessage.createStyxMessage() method
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TversionMessage(int length, short type, int tag)
    {
        super(length, type, tag);
        this.name = "Tversion";
    }
    
    /**
     * Default constructor, sets version to "9P2000" and maximum message size
     * to 8216 (so that 8192 bytes can be read or written with a Tread/Twrite)
     */
    public TversionMessage()
    {
        this(19, (short)100, StyxUtils.NOTAG);
        this.maxMessageSize = 8216;
        this.setVersion("9P2000");
    }
    
    /**
     * Creates a new TversionMessage, with the supplied maximum message size.
     * Sets the version to "9P2000"
     * @param maxMessageSize The requested maximum size of a message that will 
     * be sent on this connection by either party
     */
    public TversionMessage(long maxMessageSize)
    {
        this();
        this.maxMessageSize = maxMessageSize;
    }
    
    /**
     * Creates a new TversionMessage, with the supplied maximum message size
     * and version string. Note that the version string should always be "9P2000";
     * this constructor is used mainly for debugging.
     * @param maxMessageSize The requested maximum size of a message that will
     * be sent on this connection by either party
     * @param protocolVersion The version string (Should always be "9P2000")
     */
    public TversionMessage(long maxMessageSize, String protocolVersion)
    {
        this();
        this.maxMessageSize = maxMessageSize;
        this.setVersion(protocolVersion);
    }
    
    protected final void decodeBody(StyxBuffer styxBuf)
    {
        // Read the maximum message size
        this.maxMessageSize = styxBuf.getUInt();
        // Read the version string
        this.version = styxBuf.getString();
    }
    
    protected final void encodeBody(StyxBuffer styxBuf)
    {
        // Write the max message size, then the version string
        styxBuf.putUInt(this.maxMessageSize).putString(this.version);
    }
    
    /**
     * @return The requested maximum size of a message that will 
     * be sent on this connection by either party
     */
    public long getMaxMessageSize()
    {
        return this.maxMessageSize;
    }
    
    /**
     * @param maxMessageSize The requested maximum size of a message that will 
     * be sent on this connection by either party
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
        this.length = StyxUtils.HEADER_LENGTH + 4 + 2 + versionLen;
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
