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

package uk.ac.rdg.resc.jstyx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import uk.ac.rdg.resc.jstyx.types.Qid;
import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.types.DirEntry;

/**
 * Wrapper class for a ByteBuffer that allows Styx primitives to be easily
 * read from and written to the buffer.  Reads and writes unsigned bytes, shorts
 * and integers, in addition to other Styx types.
 * Before using the methods in this class, you should check that the required
 * number of bytes are available in the ByteBuffer. The easiest way to do this
 * is to read the message length (the first four bytes in the Styx message) and
 * check that this number of bytes are available in the buffer.
 * @todo: do these functions need to be thread-safe?  Are two different threads
 * ever going to want to access the same buffer simultaneously?
 * @todo: does this belong in the messages package?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/03/09 17:01:23  jonblower
 * Added more methods reflecting methods in underlying ByteBuffer
 *
 * Revision 1.1.1.1  2005/02/16 18:58:16  jonblower
 * Initial import
 *
 */
public class StyxBuffer
{
    
    private ByteBuffer buf;  // The underlying java.nio.ByteBuffer
    
    /** 
     * Creates a new instance of StyxBuffer
     * @param buf The ByteBuffer to wrap
     * @throws IllegalArgumentException if the ByteBuffer is null
     */
    public StyxBuffer(ByteBuffer buf)
    {
        if (buf == null)
        {
            throw new IllegalArgumentException("Byte buffer cannot be null");
        }
        this.buf = buf;
        // Make sure that the byte order is little-endian
        this.buf.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * @return the next unsigned byte as an integer between 0 and 255
     */
    public int getUByte()
    {
        byte b = buf.get();
        return b & 0xff;
    }
    
    /**
     * Puts an unsigned byte into the buffer at the current position
     * @param b The value of the byte as an integer between 0 and 255
     * @return this StyxBuffer (allows chaining of put commands)
     * @throws IllegalArgumentException if the parameter is out of range
     */
    public StyxBuffer putUByte(int b)
    {
        if (b < 0 || b > StyxUtils.MAXUBYTE)
        {
            throw new IllegalArgumentException("Value (" + b + ") out of range of UByte (0-255)");
        }
        buf.put((byte)b);
        return this;
    }
    
    /**
     * @return the next unsigned short (2 bytes) as an integer between 0 and 65335
     */
    public int getUShort()
    {
        short s = buf.getShort();
        return s & 0xffff;
    }
    
    /**
     * Puts an unsigned short (2 bytes) into the buffer at the current position
     * @param s The value of the short as an integer between 0 and 65535
     * @return this StyxBuffer (allows chaining of put commands)
     * @throws IllegalArgumentException if the parameter is out of range
     */
    public StyxBuffer putUShort(int s)
    {
        if (s < 0 || s > StyxUtils.MAXUSHORT)
        {
            throw new IllegalArgumentException("Value (" + s + 
                ") out of range of UShort (0-" + StyxUtils.MAXUSHORT + ")");
        }
        buf.putShort((short)s);
        return this;
    }
    
    /**
     * Puts an unsigned short (2 bytes) into the buffer at the given position
     * @param index The place in the buffer at which to put the data
     * @param l The value of the short as an integer between 0 and 65535
     * @return this StyxBuffer (allows chaining of put commands)
     * @throws IllegalArgumentException if the parameter is out of range
     */
    public StyxBuffer putUShort(int index, int s)
    {
        if (s < 0 || s > StyxUtils.MAXUSHORT)
        {
            throw new IllegalArgumentException("Value (" + s + 
                ") out of range of UShort (0-" + StyxUtils.MAXUSHORT + ")");
        }
        buf.putShort(index, (short)s);
        return this;
    }
    
    /**
     * @return the next unsigned int (4 bytes) as a long between 0 and 4,294,967,295
     */
    public long getUInt()
    {
        int i = buf.getInt();
        return i & 0xffffffffL;
    }
    
    /**
     * Puts an unsigned int (4 bytes) into the buffer at the current position
     * @param l The value of the int as a long integer between 0 and 4,294,967,295
     * @return this StyxBuffer (allows chaining of put commands)
     * @throws IllegalArgumentException if the parameter is out of range
     */
    public StyxBuffer putUInt(long i)
    {
        if (i < 0 || i > StyxUtils.MAXUINT)
        {
            throw new IllegalArgumentException("Value (" + i + ") out of range of UInt (0-4294967295)");
        }
        buf.putInt((int)i);
        return this;
    }
    
    /**
     * Puts an unsigned int (4 bytes) into the buffer at the given position
     * @param index The place in the buffer at which to put the data
     * @param l The value of the int as a long integer between 0 and 4,294,967,295
     * @return this StyxBuffer (allows chaining of put commands)
     * @throws IllegalArgumentException if the parameter is out of range
     */
    public StyxBuffer putUInt(int index, long i)
    {
        if (i < 0 || i > StyxUtils.MAXUINT)
        {
            throw new IllegalArgumentException("Value (" + i + ") out of range of UInt (0-4294967295)");
        }
        buf.putInt(index, (int)i);
        return this;
    }
    
    /**
     * @return the next ULong (unsigned 8-byte integer)
     */
    public ULong getULong()
    {
        byte[] bytes = new byte[8];
        buf.get(bytes);
        return new ULong(bytes);
    }
    
    /**
     * Puts the given ULong to the buffer at the current position
     * @param ulong The ulong to write
     * @return this StyxBuffer (allows chaining of put commands)
     */
    public StyxBuffer putULong(ULong ulong)
    {
        byte[] bytes = ulong.getBytes();
        buf.put(bytes);
        return this;
    }
    
    /**
     * @return the next string
     */
    public String getString()
    {
        // First get the length of the string (2-byte integer)
        int strLen = this.getUShort();
        // Create a byte array to hold the bytes containing the String
        // TODO: inefficient? Can we create the string without copying the bytes first?
        byte[] bytes = new byte[strLen];
        // Now get this number of bytes from the buffer
        buf.get(bytes);
        return StyxUtils.utf8ToString(bytes);
    }
    
    /**
     * Puts a string into the buffer at the current position. First converts
     * the string to an array of bytes in UTF-8.
     * @param s The string to write to the buffer
     * @return this StyxBuffer (allows chaining of put commands)
     */
    public StyxBuffer putString(String s)
    {
        byte[] bytes = StyxUtils.strToUTF8(s);
        this.putUShort(bytes.length);
        buf.put(bytes);
        return this;
    }
    
    /**
     * @return The next Qid (server's representation of a file)
     */
    public Qid getQid()
    {
        int type = this.getUByte();    // The type of the file
        long version = this.getUInt(); // The version number of the file
        // Now get the path of the file - an unsigned 8-byte integer
        byte[] pathBytes = new byte[8];
        buf.get(pathBytes);
        return new Qid(type, version, new ULong(pathBytes));        
    }
    
    /**
     * Puts the given Qid to the buffer at the current position
     * @param qid The Qid to write
     * @return this StyxBuffer (allows chaining of put commands)
     */
    public StyxBuffer putQid(Qid qid)
    {
        this.putUByte(qid.getType()).putUInt(qid.getVersion()).putULong(qid.getPath());
        return this;        
    }
    
    /**
     * @return The next DirEntry (Directory entry)
     */
    public DirEntry getDirEntry()
    {
        int dirEntrySize = this.getUShort();      // The size in bytes of the rest of the directory entry
        int type = this.getUShort();              // 2 bytes, for kernel use
        long dev = this.getUInt();                // 4 bytes, for kernel use
        Qid qid = this.getQid();                  // the server's representation of this file
        long mode = this.getUInt();               // 4 bytes, permissions and flags
        long lastAccessTime = this.getUInt();     // last access time
        long lastModTime = this.getUInt();        // 4 bytes, last modification time
        ULong fileLength = this.getULong();       // length of file in bytes
        String fileName = this.getString();       // file name
        String uid = this.getString();            // owner name
        String gid = this.getString();            // group name
        String lastModifiedBy = this.getString(); // Name of user who last modified the file
        
        return new DirEntry(type, dev, qid, mode, lastAccessTime,
            lastModTime, fileLength, fileName, uid, gid, lastModifiedBy);
    }
    
    /**
     * Puts the given DirEntry to the buffer at the current position
     * @param dirEntry The DirEntry to write
     * @return this StyxBuffer (allows chaining of put commands)
     */
    public StyxBuffer putDirEntry(DirEntry dir)
    {
        this.putUShort(dir.getSize() - 2);
        this.putUShort(dir.getType());
        this.putUInt(dir.getDev());
        this.putQid(dir.getQid());
        this.putUInt(dir.getMode());
        this.putUInt(dir.getLastAccessTime());
        this.putUInt(dir.getLastModifiedTime());
        this.putULong(dir.getFileLength());
        this.putString(dir.getFileName());
        this.putString(dir.getOwner());
        this.putString(dir.getGroup());
        this.putString(dir.getLastModifiedBy());
        
        return this;
    }
    
    /**
     * Gets a chunk of data from the buffer.
     * @param size the number of bytes to get (should actually be an int?)
     * @return A new byte array containing a <i>copy</i> of the data in this buffer
     * @throws IllegalArgumentException if there aren't enough bytes left in the buffer
     */
    public byte[] getData(int size)
    {
        if (this.buf.remaining() < size)
        {
            throw new IllegalArgumentException("There are fewer than " + size
                + " bytes remaining in the buffer");
        }
        // Copy the right number of bytes to a new buffer
        byte[] bytes = new byte[size];
        this.buf.get(bytes);
        return bytes;
    }
    
    public StyxBuffer put(byte[] bytes)
    {
        this.buf.put(bytes);
        return this;
    }
    
    /**
     * Puts a chunk of data to the buffer.
     * @param data The data to write. The position and limit of this buffer will
     * be unaffected by this method.
     * @param size The number of bytes to write
     * @return this StyxBuffer (allows chaining of put commands)
     * @throws IllegalArgumentException if the buffer does not contain at least
     * <code>size</code> bytes, or if there is not enough space in the output
     * buffer for the data.
     */
    public StyxBuffer putData(ByteBuffer data, long size)
    {
        if (data.remaining() < size)
        {
            throw new IllegalArgumentException("There are fewer than " + size +
                " bytes remaining in the input data buffer.");
        }
        if (this.buf.remaining() < size)
        {
            throw new IllegalArgumentException("There is not enough space for "
                + size + " bytes in the output buffer.");
        }
        // Remember the current position and limit of the buffer
        int pos = data.position();
        int limit = data.limit();
        // Set the new limit of the buffer
        data.limit(data.position() + (int)size);
        // Now we can write the data in the knowledge that the correct number
        // of bytes will be written
        this.buf.put(data);
        // Now set the old position and limit back
        data.position(pos);
        data.limit(limit);
        
        return this;
    }
    
    /**
     * Gets the underlying ByteBuffer
     */
    public ByteBuffer getBuffer()
    {
        return this.buf;
    }
    
    /**
     * @return the number of bytes remaining in the underlying buffer
     */
    public int remaining()
    {
        return this.buf.remaining();
    }
    
    /**
     * Flip the underlying buffer
     */
    public void flip()
    {
        this.buf.flip();
    }
}
