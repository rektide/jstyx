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

import java.nio.ByteBuffer;
import net.gleamynode.netty2.Message;
import net.gleamynode.netty2.MessageParseException;

import uk.ac.rdg.resc.jstyx.StyxBuffer;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Base class for all Styx Messages.  Contains header information common
 * to all messages.
 * @todo Allow for message sizes > 8192. Can achieve this by returning false from
 * Message.read() and Message.write() until the message is completely read. Easiest
 * to do this by creating a buffer for each message (or getting one from a pool)
 * of the correct size, reading the message into the buffer and copying the buffer
 * to Netty's own buffers, piece by piece.
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
public abstract class StyxMessage implements Message
{    
    public static final int HEADER_LENGTH = 7; // Length of Styx header
    
    protected long length; // Length of the message (TODO: realistically, an int would be fine)
    private int type;      // Type of the message (number between 100 and 127) (TODO: could be a short)
    private int tag;       // The tag of the message
    
    protected String name; // The name of this message type (e.g. "Rversion", "Twalk")
    
    private boolean headerWritten; // For a message that's being transmitted, 
                                   // this flag is true once the header has been sent
    private int msgStart;          // Records the start point of a message
    
    /** 
     * Creates a new StyxMessage: this method will be called from 
     * StyxMessageRecognizer when a message arrives
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public StyxMessage(long length, int type, int tag)
    {
        this.length = length;
        this.type = type;
        this.tag = tag;
        this.name = "StyxMessage"; // Will be overwritten by subclasses
        this.headerWritten = false;
    }
    
    /**
     * Creates a new StyxMessage: this method is typically called when creating
     * a message from scratch.
     * @param type The type of the message (a number between 100 and 127)
     */
    public StyxMessage(int type)
    {
        this(0, type, 0); // The message length and the tag are figured out
                          // automatically before the message is sent
    }
    
    /**
     * Reads the message. Remember that by this stage, the header information
     * (length, type, tag) has already been read, but the buffer position is at
     * the beginning of the message.
     */
    public final boolean read(ByteBuffer buf) throws MessageParseException
    {
        // Check that the whole message has arrived
        if (buf.remaining() < this.length)
        {
            return false;
        }
        // We've already got the header information so we can skip these bytes
        buf.position(buf.position() + HEADER_LENGTH);
        // Convert the ByteBuffer into a StyxBuffer for ease of retrieval
        // of Styx primitives
        StyxBuffer styxBuf = new StyxBuffer(buf);
        // Now read the message body
        return this.readBody(styxBuf);
    }
    
    protected abstract boolean readBody(StyxBuffer buf) throws MessageParseException;
    
    /**
     * Write the message to the output buffer. The message length needs to be
     * set before this is called; this is usually done automatically by the
     * constructor and setter methods of the subclass. The message tag does not
     * need to be set; it is set automatically by the StyxSession object
     */
    public final boolean write(ByteBuffer buf)
    {
        // Convert the ByteBuffer into a StyxBuffer for ease of writing
        // of Styx primitives
        StyxBuffer styxBuf = new StyxBuffer(buf);        
        // Now write the message body, checking that there is enough space in
        // the buffer
        if (buf.remaining() < this.length)
        {
            return false;
        }
        // Write the message header
        styxBuf.putUInt(this.length).putUByte(this.type).putUShort(this.tag);
        return this.writeBody(styxBuf);
    }
    
    protected abstract boolean writeBody(StyxBuffer buf);
    
    public String toString()
    {
        return this.name + " " + this.length + ", " + this.type +
            ", " + this.tag + this.getElements();
    }
    
    /**
     * @return a human-readable string that displays the contents of the message,
     * without the header info. This implementation does nothing useful; subclasses
     * should override this.
     */
    public String toFriendlyString()
    {
        return "a StyxMessage";
    }
    
    /**
     * @return the message's tag number
     */
    public int getTag()
    {
        return this.tag;
    }
    
    /**
     * Sets the tag of the StyxMessage (called by StyxSession.write())
     */
    public void setTag(int tag)
    {
        this.tag = tag;
    }
    
    /**
     * @return the fid of this message, or StyxUtils.NOFID if this message isn't associated
     * with a fid (most Tmessages are). This default implementation just returns
     * -1; subclasses will override this.
     */
    public long getFid()
    {
        return StyxUtils.NOFID;
    }
    
    /**
     * @return the type of the message as an integer
     * @todo return this as a string for easier debugging?
     */
    public int getType()
    {
        return this.type;
    }
    
    /**
     * @return the name of this message ("Tread", "Rwstat" etc)
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * @return the elements of this message as a string
     */
    protected abstract String getElements();
}
