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
 * can grow to arbitrary size.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.20  2006/03/20 17:51:50  jonblower
 * Adding authentication to base JStyx system
 *
 * Revision 1.19  2005/09/08 07:08:59  jonblower
 * Removed "String user" from list of parameters to StyxFile.write()
 *
 * Revision 1.18  2005/09/02 16:51:20  jonblower
 * Fixed bug with ByteBuffers being released prematurely and changed to use autoExpanding ByteBuffers
 *
 * Revision 1.17  2005/09/01 07:50:22  jonblower
 * Trying to fix bug with ByteBuffers being released (can't read from InMemoryFile)
 *
 * Revision 1.16  2005/08/30 16:29:00  jonblower
 * Added processAndReplyRead() helper functions to StyxFile
 *
 * Revision 1.13  2005/06/10 07:53:12  jonblower
 * Changed SGS namespace: removed "inurl" and subsumed functionality into "stdin"
 *
 * Revision 1.12  2005/04/28 08:11:15  jonblower
 * Modified permissions handling in documentation directory of SGS
 *
 * Revision 1.10  2005/04/27 16:11:43  jonblower
 * Added capability to add documentation files to SGS namespace
 *
 * Revision 1.9  2005/04/26 07:46:11  jonblower
 * Continuing to improve setting of parameters in Styx Grid Services
 *
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
     * Creates a new instance of InMemoryFile
     */
    public InMemoryFile(String name, String userID, String groupID,
        int permissions, boolean isAppendOnly, boolean isExclusive)
        throws StyxException
    {
        super(name, userID, groupID, permissions, isAppendOnly, isExclusive);
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
    
    /**
     * Creates an InMemoryFile with default permissions (0666, rw-rw-rw-)
     */
    public InMemoryFile(String name)
        throws StyxException
    {
        this(name, 0666);
    }
    
    public synchronized void read(StyxFileClient client, long offset, int count,
        int tag) throws StyxException
    {
        if (this.buf != null)
        {
            // We must increment the reference count to this buffer because it will
            // be decremented when the return message is sent.
            this.buf.acquire();
        }
        this.processAndReplyRead(this.buf, client, offset, count, tag);
    }
    
    public synchronized void write(StyxFileClient client, long offset,
        int count, ByteBuffer data, boolean truncate, int tag)
        throws StyxException
    {
        if (this.buf == null)
        {
            // This is the first write to the file; create the buffer.
            this.buf = ByteBuffer.allocate(count);
            // Set the buffer to expand automatically in response to writes
            this.buf.setAutoExpand(true);
            this.buf.position(0).limit(0);
            log.debug("Allocated InMemoryFile with capacity " + this.buf.capacity());
        }
        if (offset > this.buf.limit())
        {
            throw new StyxException("attempt to write past the end of the file");
        }
        // Set the position of the buffer
        this.buf.position((int)offset);
        // We don't have to worry about the size of the buffer because it will
        // grow automatically if necessary
        this.buf.put(data);
        if (truncate)
        {
            this.buf.limit(this.buf.position());
        }
        this.replyWrite(client, count, tag);
    }
    
    /**
     * Set the contents of this file to the given String.
     */
    public synchronized void setContents(String newContents)
    {
        // Convert the String to bytes
        byte[] bytes = StyxUtils.strToUTF8(newContents);
        // Create a buffer if we need to
        if (this.buf == null)
        {
            this.buf = ByteBuffer.allocate(bytes.length);
            this.buf.setAutoExpand(true);
            log.debug("Allocated InMemoryFile with capacity " + this.buf.capacity());
        }
        // Write the data at the start of the buffer, then set the length
        this.buf.position(0);
        // We don't have to worry about the size of the buffer because it will
        // grow automatically if necessary
        this.buf.put(bytes);
        this.buf.limit(bytes.length);
        // Notify that the contents of the file have changed
        this.contentsChanged();
    }
    
    public synchronized ULong getLength()
    {
        if (this.buf == null)
        {
            return ULong.ZERO;
        }
        return new ULong(this.buf.limit());
    }
    
    /**
     * Frees the resources associated with the file (releases the underlying
     * ByteBuffer back to the pool).  After this is called, the file can no
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
    public synchronized String getContents()
    {
        if (this.buf == null)
        {
            return "";
        }
        // Make sure the position is set to zero
        this.buf.position(0);
        // The limit will have already been set
        return StyxUtils.dataToString(this.buf);
    }
    
    public static void main (String[] args) throws Exception
    {
        // Create the root directory of the Styx server
        StyxDirectory root = new StyxDirectory("/");
        // Add an InMemoryFile to the root
        root.addChild(new InMemoryFile("inmem"));
        // Start a StyxServer, listening on port 9876
        new StyxServer(9876, root).start();
    }
    
}
