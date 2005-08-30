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

package uk.ac.rdg.resc.jstyx.gridservice.server;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * A StyxFile that provides an interface to a ServiceDataElement
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/08/30 16:29:00  jonblower
 * Added processAndReplyRead() helper functions to StyxFile
 *
 * Revision 1.4  2005/03/24 09:48:31  jonblower
 * Changed 'count' from long to int throughout for reading and writing
 *
 * Revision 1.3  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.2  2005/03/18 16:45:18  jonblower
 * Released ByteBuffers after use
 *
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.2  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.1  2005/02/16 19:22:31  jonblower
 * Commit adding of SGS files to CVS
 *
 */
class SDEFile extends StyxFile
{    
    private ServiceDataElement sde;
    
    public SDEFile(ServiceDataElement sde) throws StyxException
    {
        super(sde.getName());
        this.sde = sde;
    }
    
    public void read(StyxFileClient client, long offset, int count, int tag)
        throws StyxException
    {
        byte[] bytes = this.sde.getBytes();
        this.processAndReplyRead(bytes, client, offset, count, tag);
    }

    /**
     * We implicitly assume that the new value of the Service Data is 
     * contained in a single write message
     * @todo: what happens if this isn't the case?
     */
    public void write(StyxFileClient client, long offset, int count,
        ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        // TODO: if the sde is readonly, we should alter the permission on the SDEFile
        if(this.sde.isReadOnly())
        {
            throw new StyxException("Cannot write to this file");
        }
        // We ignore the offset and just update the file's contents with
        // the new value
        byte[] bytes = new byte[(int)count];
        data.get(bytes);
        this.sde.setValue(bytes);
        this.replyWrite(client, count, tag);
    }

    public ULong getLength()
    {
        return new ULong(this.sde.getBytes().length);
    }
    
}
