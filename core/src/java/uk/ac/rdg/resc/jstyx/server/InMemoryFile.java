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

package uk.ac.rdg.resc.jstyx.server;

import org.apache.log4j.Logger;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * File whose underlying data are stored as a ByteBuffer in memory. This buffer
 * can grow to arbitrary size
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.8  2005/03/24 15:11:07  jonblower
 * Changed so that underlying ByteBuffer is allocated on the first write to the file
 *
 * Revision 1.7  2005/03/24 13:07:09  jonblower
 * Added code to prevent the file growing to larger than a specified size
 *
 * Revision 1.6  2005/03/24 12:55:14  jonblower
 * Changed to use ByteBuffer as backing store
 *
 * Revision 1.5  2005/03/24 09:48:31  jonblower
 * Changed 'count' from long to int throughout for reading and writing
 *
 * Revision 1.4  2005/03/16 17:56:23  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.3  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.1  2005/03/10 20:55:38  jonblower
 * Removed references to Netty
 *
 * Revision 1.2  2005/03/01 13:47:43  jonblower
 * Changed default user and group to 'user' and 'group'
 *
 * Revision 1.1.1.1  2005/02/16 18:58:31  jonblower
 * Initial import
 *
 */
public class InMemoryFile extends StyxFile
{
    
    private static final Logger log = Logger.getLogger(InMemoryFile.class);
    
    /**
     * The org.apache.mina.common.ByteBuffer that holds the data exposed by this file.
     * The first byte of readable data is always at position zero and the number
     * of valid bytes in the buffer is its limit
     */
    protected ByteBuffer buf;
    
    /**
     * The maximum size to which this file can grow. This is set to 8192 bytes
     * by default. Use setCapacity() to increase this limit.
     */
    protected int capacity;
    
    /** Creates a new instance of InMemoryFile */
    public InMemoryFile(String name, String userID, String groupID,
        int permissions, boolean isAppendOnly, boolean isExclusive)
        throws StyxException
    {
        super(name, userID, groupID, permissions, false, isAppendOnly,
            isExclusive);
        this.capacity = 8192;
    }
    
    public InMemoryFile(String name, int permissions,
        boolean isAppendOnly, boolean isExclusive)
        throws StyxException
    {
        this(name, "user", "group", permissions, isAppendOnly, isExclusive);
    }
    
    public InMemoryFile(String name, int permissions)
        throws StyxException
    {
        this(name, permissions, false, false);
    }
    
    public InMemoryFile(String name)
        throws StyxException
    {
        this(name, 0777);
    }
    
    public synchronized void read(StyxFileClient client, long offset, int count,
        int tag) throws StyxException
    {
        if (this.buf == null || offset >= this.buf.limit())
        {
            // Attempt to read off the end of the file, or no data have yet
            // been written to the file
            this.replyRead(client, new byte[0], tag);
            return;
        }
        int numBytesToReturn = (this.buf.limit() - (int)offset) > count ? 
            count : this.buf.limit() - (int)offset;
        // Must create a copy of the data to return to the client
        byte[] bytes = new byte[numBytesToReturn];
        this.buf.position((int)offset);
        this.buf.get(bytes);
        this.replyRead(client, bytes, tag);
    }
    
    public synchronized void write(StyxFileClient client, long offset,
        int count, ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        if (this.buf == null)
        {
            // This is the first write to the file; create the buffer.
            this.buf = ByteBuffer.allocate(count);
            this.buf.position(0).limit(0);
            log.debug("Allocated InMemoryFile with capacity " + this.buf.capacity());
        }
        if (offset > this.buf.limit())
        {
            throw new StyxException("offset is greater than the current data length");
        }
        // Calculate the new size of the data after the write operation
        int newSize = (int)offset + count;
        if (!truncate)
        {
            newSize = this.buf.limit() > newSize ? this.buf.limit() : newSize;
        }
        if (newSize > this.capacity)
        {
            throw new StyxException(this.name + " cannot grow to more than "
                + this.capacity + " bytes in size");
        }
        // Make sure the buffer is big enough to hold the new data
        this.growBuffer(newSize);
        // Now we can write the new data to the buffer at the correct offset
        this.buf.position((int)offset);
        this.buf.limit(newSize);
        // Set the limit of the input data correctly
        data.limit(data.position() + count);
        this.buf.put(data);
        this.replyWrite(client, count, tag);
    }
    
    /**
     * Grows the underlying ByteBuffer to the given size (actually allocates
     * a new ByteBuffer and copies all the bytes to the new buffer).
     * Does nothing if the existing ByteBuffer's capacity is >= the given size.
     * When this method is done, the position of the buffer will be set to 
     */
    private void growBuffer(int newSize)
    {
        if (newSize > this.buf.capacity())
        {
            // Current ByteBuffer is not large enough. Allocate a new one.
            ByteBuffer newBuf = ByteBuffer.allocate(newSize);
            // Copy all the bytes (up to the old buffer's limit) from the old
            // buffer to the new buffer
            this.buf.position(0);
            newBuf.put(this.buf);
            // Free the existing buffer
            this.buf.release();
            // Keep the new buffer
            this.buf = newBuf;
            log.debug("Grew buffer to size " + this.buf.capacity());
        }
    }
    
    public synchronized ULong getLength()
    {
        if (this.buf == null)
        {
            return new ULong(0);
        }
        return new ULong(this.buf.limit());
    }
    
    /**
     * Frees the resources associated with the file (releases the underlying
     * ByteBuffer back to the pool.  After this is called, the file can no
     * longer be used.
     */
    public synchronized void delete()
    {
        if (this.buf != null)
        {
            this.buf.release();
        }
    }
    
    /**
     * Gets the data in this file as a String
     */
    public synchronized String getDataAsString()
    {
        if (this.buf == null)
        {
            return "";
        }
        this.buf.position(0);
        return StyxUtils.dataToString(this.buf);
    }
    
    /**
     * Sets the maximum number of bytes that this file can hold
     */
    public synchronized void setCapacity(int newCapacity)
    {
        this.capacity = newCapacity;
    }
    
    public static void main (String[] args) throws Exception
    {
        // Create the root directory of the Styx server
        StyxDirectory root = new StyxDirectory("/");
        // Add a DateFile to the root
        root.addChild(new InMemoryFile("inmem"));
        // Start a StyxServer, listening on port 9876
        new StyxServer(9876, root).start();
    }
    
}
