package uk.ac.rdg.resc.jstyx.mina;
/*
 * StyxMessage.java
 *
 * Created on 07 March 2005, 16:06
 */

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxBuffer;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 *
 * @author jdb
 */
public abstract class StyxMessage
{
    
    protected int length;  // The length of the StyxMessage (although in Styx
                           // this is an *unsigned* int, we guarantee in
                           // StyxMessageDecoder that we can't have messages
                           // longer than Integer.MAX_VALUE
    protected int type;    // The type of the StyxMessage
    protected int tag;     // The tag of the StyxMessage
    protected String name; // The name of the message (e.g. "Tversion")
    private ByteBuffer buf; // Contains the bytes of the body of the
                            // StyxMessage (i.e. not the header)
    
    /**
     * Creates a new instance of StyxMessage.
     */
    protected StyxMessage(int length, int type, int tag)
    {
        this.length = length;
        this.type = type;
        this.tag = tag;
        this.name = "StyxMessage";
        this.buf = null; // The buffer gets allocated later, when we're sure
                         // what the message length is
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
    public static StyxMessage createStyxMessage(int length, int type, int tag)
        throws ProtocolViolationException
    {
        if (type == 100)
        {
            return new TversionMessage(length, type, tag);
        }
        else if (type == 104)
        {
            return new TattachMessage(length, type, tag);
        }
        else
        {
            throw new ProtocolViolationException("Unknown message type " + type);
        }
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
    public final int getType()
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
     * @return the buffer containing the body of this message
     */
    public ByteBuffer getBuffer()
    {
        return this.buf;
    }
    
    /**
     * Read bytes from the given ByteBuffer into this Message. There may still
     * be bytes remaining in the input buffer after this method has been called.
     * @param buf The org.apache.mina.common.ByteBuffer that contains the data.
     * @return true if we now have a complete StyxMessage, false otherwise
     */
    public final boolean readBytesFrom(ByteBuffer in)
    {
        int bodyLength = this.length - StyxUtils.HEADER_LENGTH;
        if (bodyLength == 0)
        {
            // We don't need to read any bytes; this message has no body
            return true;
        }
        if (this.buf == null)
        {
            // This is the first time we've called this method for this
            // message. Create a buffer to hold the bytes. This buffer comes
            // from MINA's pool
            this.buf = ByteBuffer.allocate(bodyLength);
        }
        if (this.buf.position() >= bodyLength)
        {
            // We don't need to read any bytes; we already have the full message
            return true;
        }
        
        // Calculate the number of bytes we can read from the input buffer.
        // We can't rely on this.buf.remaining() here because the buffer could be
        // bigger than the message length
        int bytesLeft = bodyLength - this.buf.position();
        int bytesToRead = bytesLeft < in.remaining() ? bytesLeft : in.remaining();
        
        // Read the bytes and write to this message
        byte[] b = new byte[bytesToRead];
        in.get(b);
        this.buf.put(b);
        
        // Return true if the buffer is now full (i.e. we have the whole message);
        // false otherwise
        if (this.buf.position() >= bodyLength)
        {
            // We now have the full message. Decode these bytes into meaningful
            // information
            this.decode();
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Called when we have a complete message. Simply flips the buffer and 
     * wraps it as a StyxBuffer to make it easy to read Styx primitives
     */
    private void decode()
    {
        this.buf.flip();
        StyxBuffer styxBuf = new StyxBuffer(this.buf.buf());
        this.decodeBody(styxBuf);
    }
    
    /**
     * Called when a complete message has arrived; signals that we are ready
     * to interpret the raw bytes in the buffer and turn them into meaningful
     * information.
     */
    protected abstract void decodeBody(StyxBuffer styxBuf);
    
    /**
     * Called by StyxMessageEncoder when we are about to send a message. Creates
     * the underlying ByteBuffer, wraps it as a StyxBuffer, writes the header
     * information, calls encodeBody() to write the body information, then 
     * flips the buffer so that it is ready for writing to the output stream.
     */
    public void encode()
    {
        // Make sure we have a buffer of the appropriate length
        this.buf = ByteBuffer.allocate(this.length);
        // Wrap the buffer as a StyxBuffer to make it easy to write Styx
        // primitives
        StyxBuffer styxBuf = new StyxBuffer(this.buf.buf());
        styxBuf.putUInt(this.length).putUByte(this.type).putUShort(this.tag);
        this.encodeBody(styxBuf);
        this.buf.flip();
    }
    
    /**
     * Encode the body of the message into bytes in the underlying buffer
     */
    protected abstract void encodeBody(StyxBuffer styxBuf);
    
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
    
}
