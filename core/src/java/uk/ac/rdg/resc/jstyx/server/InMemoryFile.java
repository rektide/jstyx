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

import java.nio.ByteBuffer;
import java.util.Date;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * File whose underlying data are stored as a block in memory
 * @todo: put a limit on the amount of memory it can take up?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
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
    
    protected String data; // TODO: use StringBuffer or ByteBuffer?
    
    /** Creates a new instance of InMemoryFile */
    public InMemoryFile(String name, String userID, String groupID,
        int permissions, boolean isAppendOnly, boolean isExclusive)
        throws StyxException
    {
        super(name, userID, groupID, permissions, false, isAppendOnly,
            isExclusive);
        this.data = "";
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
    
    public synchronized void read(StyxFileClient client, long offset, long count, int tag)
        throws StyxException
    {
        byte[] strBytes = StyxUtils.strToUTF8(this.data);
        int numBytesToReturn = (strBytes.length - (int)offset) > (int)count ? 
            (int)count : strBytes.length - (int)offset;
        if (numBytesToReturn < 1)
        {
            this.replyRead(client, ByteBuffer.allocate(0), tag);
            return;
        }
        byte[] bytesToReturn = new byte[numBytesToReturn];
        System.arraycopy(strBytes, (int)offset, bytesToReturn, 0, numBytesToReturn);
        this.replyRead(client, ByteBuffer.wrap(bytesToReturn), tag);
    }
    
    public synchronized void write(StyxFileClient client, long offset,
        long count, ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        // Copy the contents of the data buffer
        if ((int)offset > this.data.length())
        {
            throw new StyxException("offset is greater than the current data length");
        }
        // TODO: check the "count" parameter
        byte[] bytes;
        if (data.hasArray())
        {
            bytes = data.array();
        }
        else
        {
            // this buffer has no backing array. We'll have to copy the bytes
            // out "manually"
            int numBytes = (data.remaining() > (int)count) ? (int)count : 
                data.remaining();
            bytes = new byte[numBytes];
            for (int i = 0; i < bytes.length; i++)
            {
                bytes[i] = data.get();
            }
        }
        // add the new data to the current data at the correct offset
        this.data = this.data.substring(0, (int)offset) + StyxUtils.utf8ToString(bytes);
        this.replyWrite(client, bytes.length, tag);
    }
    
    public ULong getLength()
    {
        return new ULong(StyxUtils.strToUTF8(this.data).length);
    }
    
    public synchronized void delete()
    {
        this.data = "";
    }
    
    public void setData(String s)
    {
        this.data = s;
        this.contentsChanged();
    }
    
    public String getData()
    {
        return this.data;
    }
    
}
