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

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * StyxFile that contains a URL.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/11/07 12:22:25  jonblower
 * Added getLength() method
 *
 * Revision 1.1  2005/11/04 17:32:11  jonblower
 * Initial import
 *
 */

public class URLFile extends StyxFile
{
    private URL url;
    
    /**
     * Creates a new instance of URLFile with read-write permissions for everyone
     * (0666).
     */
    public URLFile(String name) throws StyxException
    {
        super(name, 0666);
        this.url = null;
    }
    
    public synchronized void read(StyxFileClient client, long offset, int count,
        int tag) throws StyxException
    {
        String urlStr = "";
        if (this.url != null)
        {
            urlStr = this.url.toString();
        }
        this.processAndReplyRead(urlStr, client, offset, count, tag);
    }
    
    /**
     * The new value for the URL must come in a single message (i.e.
     * the offset must be zero and the incoming ByteBuffer must contain the
     * entire URL).  Must also write with truncation.
     */
    public synchronized void write(StyxFileClient client, long offset,
        int count, ByteBuffer data, boolean truncate, int tag)
        throws StyxException
    {
        if (offset != 0)
        {
            throw new StyxException("Must write to the start of the URL file");
        }
        if (!truncate)
        {
            throw new StyxException("Must write to the URL file with truncation");
        }
        // Set the limit of the input data buffer correctly
        data.limit(data.position() + count);
        
        String urlStr = StyxUtils.dataToString(data);
        if (urlStr.trim().equals(""))
        {
            this.url = null;
        }
        else
        {
            try
            {
                this.url = new URL(urlStr);
            }
            catch (MalformedURLException mue)
            {
                throw new StyxException(urlStr + " is not recognized as a valid URL");
            }
        }
        this.replyWrite(client, count, tag);
    } 
    
    /**
     * @return the size of this file in bytes
     */
    public ULong getLength()
    {
        String str = this.url.toString();
        int len = StyxUtils.strToUTF8(str).length;
        return new ULong(len);
    }
    
    /**
     * @return the URL contained in this file or null if it hasn't been set
     */
    public URL getURL()
    {
        return this.url;
    }
}
