package uk.ac.rdg.resc.jstyx.mina;
/*
 * StyxMessage.java
 *
 * Created on 07 March 2005, 16:06
 */

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 *
 * @author jdb
 */
public class StyxMessage
{
    
    private long length; // Length of the message in bytes
    private int headerBytesRead; // Number of header bytes read
    protected java.nio.ByteBuffer buf; // Contains the bytes of the StyxMessage
    
    private static final int HEADER_LENGTH = 4;
    
    /**
     * Creates a new instance of StyxMessage.
     */
    public StyxMessage()
    {
        System.err.println("Created new StyxMessage");
        this.length = 0;
        this.headerBytesRead = 0;
        this.buf = null;
    }
    
    /**
     * Reads bytes from the given buffer.
     * @param in the org.apache.mina.common.ByteBuffer that was passed by
     * the StyxMessageDecoder.decode() method
     * @return true if the StyxMessage is now complete, false otherwise.  Note
     * that there could still be bytes remaining in minaBuf, representing the 
     * start of the next message
     * @throws ProtocolViolationException if the message length turns out to be
     * greater than Integer.MAX_VALUE
     */
    public boolean readBuffer(ByteBuffer in)
        throws ProtocolViolationException
    {
        // Read the first four bytes (the message length) if we have not
        // done so already
        if (this.headerBytesRead < HEADER_LENGTH)
        {
            while(in.hasRemaining() && this.headerBytesRead < HEADER_LENGTH)
            {
                // Get the next byte, making sure it's between 0 and 255
                long b = in.get() & 0xff;
                // Multiply by appropriate power of 256
                for (int i = 0; i < this.headerBytesRead; i++)
                {
                    b *= 256;
                }
                this.length += b;
                this.headerBytesRead++;
            }
            if (this.headerBytesRead < HEADER_LENGTH)
            {
                // We still haven't got the message length
                return false;
            }
            else
            {
                System.err.println("Got message length: " + this.length);
                // We've got the message length
                if (this.length < HEADER_LENGTH)
                {
                    throw new ProtocolViolationException("Message size cannot "
                        + "be less than " + HEADER_LENGTH + "; got " + this.length);
                }
                if (this.length > Integer.MAX_VALUE)
                {
                    throw new ProtocolViolationException("Read message size of "
                        + this.length + " bytes; cannot be greater than " +
                        Integer.MAX_VALUE + " bytes");
                }
                // We've got the message length. Allocate the buffer
                // TODO: get this buffer from a pool and return to the pool as
                // soon as we've finished with this message.
                this.buf = java.nio.ByteBuffer.allocate((int)(this.length - HEADER_LENGTH));
            }
        }
        
        // Read as many bytes as we can from the input buffer
        int bytesToRead = in.remaining() < this.buf.remaining() ?
            in.remaining() : this.buf.remaining();
        byte[] b = new byte[bytesToRead];
        in.get(b);
        this.buf.put(b);
        
        if (this.buf.hasRemaining())
        {
            // We still have some bytes to read
            return false;
        }
        else
        {
            // We now have the complete message
            this.buf.flip();
            return true;
        }
    }
    
    /**
     * @return the length of this message in bytes
     * @throws IllegalStateException if we haven't yet read enough bytes to know
     * the message length
     */
    public int getLength()
    {
        if (this.buf == null)
        {
            throw new IllegalStateException("Haven't read enough bytes to "
                + "know the message length");
        }
        return this.buf.capacity() + HEADER_LENGTH;
    }
    
}
