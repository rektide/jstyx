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

import org.apache.mina.protocol.ProtocolSession;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Server's representation of a client of a StyxFile; created when a client
 * succesfully opens a StyxFile
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.1.1.2.1  2005/03/09 19:44:18  jonblower
 * Changes concerned with migration to MINA
 *
 * Revision 1.1.1.1  2005/02/16 18:58:33  jonblower
 * Initial import
 *
 */
public class StyxFileClient
{
    
    private ProtocolSession session;  // The connection that the client sits on
    private long fid;              // The client's identifier for the file
    
    private boolean truncate;      // true if the client wants to truncate the file
                                   // after writing data. If the file is append-only
                                   // the file won't be truncated.
    private boolean deleteOnClunk; // true if the client wants the file to be
                                   // removed when the file's fid is clunked
    private long offset;           // When the client reads from or writes to a
                                   // file, this records the new offset (i.e. it
                                   // is a client-specific file pointer)
    private int nextFileToRead;    // used when reading a directory; stores the 
                                   // index of the next child of a StyxFile to include
                                   // in a RreadMessage
    private int mode;              // 0 if OREAD, 1 if OWRITE, 2 if ORDWR, 3 if EXEC
    
    /**
     * A new StyxFileClient
     * @param session The connection the client is using
     * @param fid The client's handle to the file. Remember that clients might
     * have multiple fids opened on the same file
     * @param mode the file mode as it appears in the TopenMessage (including
     * the OTRUNC and ORCLOSE bits)
     */
    public StyxFileClient(ProtocolSession session, long fid, int mode)
    {
        this.session = session;
        this.fid = fid;
        if ((mode & StyxUtils.OTRUNC) == StyxUtils.OTRUNC)
        {
            this.truncate = true;
        }
        if ((mode & StyxUtils.ORCLOSE) == StyxUtils.ORCLOSE)
        {
            this.deleteOnClunk = true;
        }
        this.mode = mode & 3; // Mask off all but last two bits
        this.offset = 0;
        this.nextFileToRead = 0;
    }
    
    public ProtocolSession getSession()
    {
        return this.session;
    }
    
    public long getFid()
    {
        return this.fid;
    }
    
    public boolean deleteOnClunk()
    {
        return this.deleteOnClunk;
    }
    
    public boolean truncate()
    {
        return this.truncate;
    }
    
    public void setOffset(long offset)
    {
        this.offset = offset;
    }
    
    public long getOffset()
    {
        return this.offset;
    }
    
    /**
     * Checks to see if the client can read from this file (i.e. if the client
     * has opened the file with read access)
     */
    public boolean canRead()
    {
        return (this.mode == StyxUtils.OREAD) || (this.mode == StyxUtils.ORDWR);
    }
    
    /**
     * Checks to see if the client can write to this file (i.e. if the client
     * has opened the file with write access)
     */
    public boolean canWrite()
    {
        return (this.mode == StyxUtils.OWRITE) || (this.mode == StyxUtils.ORDWR);
    }

    public int getNextFileToRead()
    {
        return nextFileToRead;
    }

    public void setNextFileToRead(int nextFileToRead)
    {
        this.nextFileToRead = nextFileToRead;
    }
}
