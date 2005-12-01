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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;
import org.apache.mina.protocol.ProtocolEncoderOutput;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Abstract superclass for all Styx messages.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.14  2005/12/01 08:21:56  jonblower
 * Fixed javadoc comments
 *
 * Revision 1.13  2005/11/03 17:09:27  jonblower
 * Created more efficient RreadMessage that involves less copying of buffers (still reliable)
 *
 * Revision 1.12  2005/11/03 07:46:55  jonblower
 * Trying to fix bug with sending RreadMessages
 *
 * Revision 1.11  2005/05/10 19:17:54  jonblower
 * Added dispose() method
 *
 * Revision 1.10  2005/03/22 17:48:27  jonblower
 * Removed debug code that tracked ByteBuffer allocation
 *
 * Revision 1.9  2005/03/21 17:57:11  jonblower
 * Trying to fix ByteBuffer leak in SGS server
 *
 * Revision 1.8  2005/03/18 13:56:00  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.7  2005/03/16 17:56:22  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.6  2005/03/15 16:56:19  jonblower
 * Changed to allow re-use of ByteBuffers once message is finished with
 *
 * Revision 1.5  2005/03/15 09:01:48  jonblower
 * Message type now stored as short, not int
 *
 * Revision 1.4  2005/03/11 14:02:15  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.3.2.3  2005/03/11 12:30:46  jonblower
 * Changed so that message payloads are always ints, not longs
 *
 * Revision 1.3.2.2  2005/03/10 14:05:26  jonblower
 * Reinstated getFid() and getName() methods
 *
 * Revision 1.3.2.1  2005/03/10 11:50:59  jonblower
 * Changed to fit with MINA framework
 *
 */
public abstract class StyxMessage
{
    
    private static final Logger log = Logger.getLogger(StyxMessage.class);
    
    protected static final String lock = new String(); // Just used to synchronize
                                                       // writes to the network
    
    protected int length;  // The length of the StyxMessage (although in Styx
                           // this is an *unsigned* int, we guarantee in
                           // StyxMessageDecoder that we can't have messages
                           // longer than Integer.MAX_VALUE
    protected short type;    // The type of the StyxMessage
    protected int tag;     // The tag of the StyxMessage
    protected String name; // The name of the message (e.g. "Tversion")
    protected ByteBuffer buf; // Contains the bytes of the body of the
                              // StyxMessage (i.e. not the header)
    private int bytesRead;  // The number of bytes we have read into the buffer
    
    /**
     * Creates a new instance of StyxMessage.
     */
    protected StyxMessage(int length, short type, int tag)
    {
        this.length = length;
        this.type = type;
        this.tag = tag;
        this.name = "StyxMessage"; // This will be overridden in subclasses
        this.buf = null; // The buffer gets allocated later, when we're sure
                         // what the message length is
    }
    
    /**
     * @return The name of this message (e.g. "Tread", "Rwalk", etc)
     */
    public final String getName()
    {
        return this.name;
    }
    
    /**
     * @return The total length of the StyxMessage in bytes
     */
    public final int getLength()
    {
        return this.length;
    }
    
    /**
     * @return The type of the message
     */
    public final short getType()
    {
        return this.type;
    }
    
    /**
     * @return The tag of the message
     */
    public final int getTag()
    {
        return this.tag;
    }
    
    /**
     * Sets the tag of the message
     */
    public final void setTag(int newTag)
    {
        this.tag = newTag;
    }
    
    /**
     * @return the buffer containing the body of this message
     */
    public final ByteBuffer getBuffer()
    {
        return this.buf;
    }
    
    /**
     * @return the fid associated with this message. This default implementation
     * returns StyxUtils.NOFID; subclasses should override this.  This method
     * only exists in this superclass as a convenience for the StyxMon application.
     */
    public long getFid()
    {
        return StyxUtils.NOFID;
    }
    
    /**
     * Read bytes from the given ByteBuffer into this Message. There may still
     * be bytes remaining in the input buffer after this method has been called.
     * @param in The org.apache.mina.common.ByteBuffer that contains the data.
     * @return true if we now have a complete StyxMessage, false otherwise
     * @throws ProtocolViolationException if the bytes do not represent a valid
     * StyxMessage
     */
    public final boolean readBytesFrom(ByteBuffer in) throws ProtocolViolationException
    {
        int bodyLength = this.length - StyxUtils.HEADER_LENGTH;
        if (bodyLength == 0)
        {
            // We don't need to read any bytes; this message has no body
            return true;
        }
        if (this.buf == null)
        {
            this.bytesRead = 0;
            // This is the first time we've called this method for this
            // message.
            if (in.remaining() == bodyLength)
            {
                // If the input buffer contains the full body of the message (and
                // nothing more) we can just use the input buffer.  This is a very
                // common occurrence in practice.
                // TODO: should we allow this to happen if in.remaining() > bodyLength?
                log.debug("input buffer contains a whole message; won't create new buffer");
                // Increment the reference count for this buffer so it doesn't get
                // released before we want it to be
                in.acquire();
                this.buf = in;
                this.bytesRead = bodyLength; // Signify that we have read all of the body
                // We have the full message already. Decode it and return true
                this.decode();
                return true;
            }
            else
            {
                // Create a buffer to hold the bytes. This buffer comes
                // from MINA's pool
                this.buf = ByteBuffer.allocate(bodyLength);
            }
        }
        if (this.bytesRead >= bodyLength) // N.B. should never be > bodyLength
        {
            // We don't need to read any bytes; we already have the full message
            return true;
        }
        
        // Calculate the number of bytes we can read from the input buffer.
        // We can't rely on this.buf.remaining() here because the buffer could be
        // bigger than the message length
        int bytesLeft = bodyLength - this.bytesRead;
        int bytesToRead = bytesLeft < in.remaining() ? bytesLeft : in.remaining();
        
        // Read the bytes and write to this message
        byte[] b = new byte[bytesToRead];
        in.get(b);
        this.buf.put(b);
        this.bytesRead += b.length;
        
        // Return true if the buffer is now full (i.e. we have the whole message);
        // false otherwise
        if (this.bytesRead >= bodyLength) // N.B. Should never be > bodyLength
        {
            // We now have the full message. Decode these bytes into meaningful
            // information
            this.buf.flip();
            this.decode();
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Called when we have a complete message. Simply wraps the buffer as a
     * StyxBuffer to make it easy to read Styx primitives and calls
     * this.decodeBody()
     */
    private void decode() throws ProtocolViolationException
    {
        StyxBuffer styxBuf = new StyxBuffer(this.buf);
        this.decodeBody(styxBuf);
    }
    
    /**
     * Called when a complete message has arrived; signals that we are ready
     * to interpret the raw bytes in the buffer and turn them into meaningful
     * information. Subclasses should make sure that the buffer is no longer
     * needed once this method has finished, as the underlying buffer will
     * be reused.
     * @throws ProtocolViolationException if the buffer doesn't contain a valid
     * StyxMessage body
     */
    protected abstract void decodeBody(StyxBuffer styxBuf)
        throws ProtocolViolationException;
    
    /**
     * Called by StyxMessageEncoder to send a message to the output
     */
    public void write(ProtocolEncoderOutput out) throws ProtocolViolationException
    {
        // Encode this message into a ByteBuffer
        this.encode();
        synchronized(lock)
        {
            // Write this ByteBuffer
            out.write(this.buf);
        }
    }
    
    /**
     * Called by StyxMessageEncoder when we are about to send a message. Creates
     * the underlying ByteBuffer, wraps it as a StyxBuffer, writes the header
     * information, calls encodeBody() to write the body information, then 
     * flips the buffer so that it is ready for writing to the output stream.
     * @throws ProtocolViolationException if a problem occurred encoding the
     * message (shouldn't happen)
     */
    public void encode() throws ProtocolViolationException
    {
        // Make sure we have a buffer of the appropriate length
        log.debug("Allocating new ByteBuffer of length " + this.length);
        this.buf = ByteBuffer.allocate(this.length);
        // Wrap the buffer as a StyxBuffer to make it easy to write Styx
        // primitives
        StyxBuffer styxBuf = new StyxBuffer(this.buf);
        styxBuf.putUInt(this.length).putUByte(this.type).putUShort(this.tag);
        this.encodeBody(styxBuf);
        this.buf.flip();
    }
    
    /**
     * Encode the body of the message into bytes in the underlying buffer
     */
    protected abstract void encodeBody(StyxBuffer styxBuf)
        throws ProtocolViolationException;
    
    /**
     * @return String representation of this StyxMessage
     */
    public String toString()
    {
        StringBuffer s = new StringBuffer(this.name);
        s.append(": ");
        s.append(this.length);
        s.append(", ");
        s.append(this.type);
        s.append(", ");
        s.append(this.tag);
        s.append(this.getElements());
        return s.toString();
    }
    
    /**
     * @return the body elements of this message as a string
     */
    protected abstract String getElements();
    
    /**
     * @return a human-readable string that displays the contents of the message,
     * without the header info. This default implementation simply calls 
     * this.getElements(): subclasses should override this behaviour
     */
    public String toFriendlyString()
    {
        return this.getElements();
    }
    
    /**
     * Sends request to release the underlying ByteBuffer back to the pool.
     * Actually, this just decrements the reference count for the buffer; the
     * buffer is only released when this count reaches zero. This is called 
     * once the StyxMessageDecoder.decode() method has finished.
     */
    void release()
    {
        if (this.buf != null)
        {
            this.buf.release();
        }
    }
    
    /**
     * This is called <b>after</b> the message has been sent (in
     * StyxServerProtocolHandler.messageSent()) and is a signal to free any
     * resources associated with the message (e.g. an RreadMessage can release
     * the ByteBuffer holding the payload). This default implementation does
     * nothing: subclasses should override if necessary.
     */
    public void dispose()
    {
        return;
    }
    
    /**
     * Static factory method for creating a StyxMessage. Called by StyxMessageDecoder
     * when the header of a message has been decoded. Returns the appropriate
     * subclass of StyxMessage, depending on the provided type.
     * @param length The total length of the message (header and body)
     * @param type The numeric code representing the message type
     * @param tag The message tag
     * @return A StyxMessage of the appropriate type, depending on the tag
     * @throws ProtocolViolationException if the message is of an unknown type
     */
    public static StyxMessage createStyxMessage(int length, short type, int tag)
        throws ProtocolViolationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Creating StyxMessage(length = " + length + ", type = "
                + type + ", tag = " + tag + ")");
        }
        if (type == 100)
        {
            return new TversionMessage(length, type, tag);
        }
        else if (type == 101)
        {
            return new RversionMessage(length, type, tag);
        }
        else if (type == 102)
        {
            return new TauthMessage(length, type, tag);
        }
        else if (type == 103)
        {
            return new RauthMessage(length, type, tag);
        }
        else if (type == 104)
        {
            return new TattachMessage(length, type, tag);
        }
        else if (type == 105)
        {
            return new RattachMessage(length, type, tag);
        }
        // There is no message of type 106 ("Terror" doesn't exist)
        else if (type == 107)
        {
            return new RerrorMessage(length, type, tag);
        }
        else if (type == 108)
        {
            return new TflushMessage(length, type, tag);
        }
        else if (type == 109)
        {
            return new RflushMessage(length, type, tag);
        }
        else if (type == 110)
        {
            return new TwalkMessage(length, type, tag);
        }
        else if (type == 111)
        {
            return new RwalkMessage(length, type, tag);
        }
        else if (type == 112)
        {
            return new TopenMessage(length, type, tag);
        }
        else if (type == 113)
        {
            return new RopenMessage(length, type, tag);
        }
        else if (type == 114)
        {
            return new TcreateMessage(length, type, tag);
        }
        else if (type == 115)
        {
            return new RcreateMessage(length, type, tag);
        }
        else if (type == 116)
        {
            return new TreadMessage(length, type, tag);
        }
        else if (type == 117)
        {
            return new RreadMessage(length, type, tag);
        }
        else if (type == 118)
        {
            return new TwriteMessage(length, type, tag);
        }
        else if (type == 119)
        {
            return new RwriteMessage(length, type, tag);
        }
        else if (type == 120)
        {
            return new TclunkMessage(length, type, tag);
        }
        else if (type == 121)
        {
            return new RclunkMessage(length, type, tag);
        }
        else if (type == 122)
        {
            return new TremoveMessage(length, type, tag);
        }
        else if (type == 123)
        {
            return new RremoveMessage(length, type, tag);
        }
        else if (type == 124)
        {
            return new TstatMessage(length, type, tag);
        }
        else if (type == 125)
        {
            return new RstatMessage(length, type, tag);
        }
        else if (type == 126)
        {
            return new TwstatMessage(length, type, tag);
        }
        else if (type == 127)
        {
            return new RwstatMessage(length, type, tag);
        }
        else
        {
            throw new ProtocolViolationException("Unknown message type " + type);
        }
    }
    
}
