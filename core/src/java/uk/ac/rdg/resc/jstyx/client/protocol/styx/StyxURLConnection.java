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

package uk.ac.rdg.resc.jstyx.client.protocol.styx;

import java.net.URLConnection;
import java.net.URL;
import java.net.InetSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFileInputStream;
import uk.ac.rdg.resc.jstyx.client.CStyxFileOutputStream;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Represents a connection to a Styx server that's been created with a URL
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.6  2006/03/21 09:06:15  jonblower
 * Still implementing authentication
 *
 * Revision 1.5  2005/09/01 17:12:11  jonblower
 * Changes to Input and Output stream code
 *
 * Revision 1.4  2005/08/31 17:03:18  jonblower
 * Renamed "StyxFile*putStream*" to "CStyxFile*putStream*" for consistency with CStyxFile class
 *
 * Revision 1.3  2005/05/23 16:48:23  jonblower
 * Overhauled CStyxFile (esp. asynchronous methods) and StyxConnection (added cache of CStyxFiles)
 *
 * Revision 1.2  2005/02/24 07:47:06  jonblower
 * Added connect() in response to change to StyxConnection
 *
 * Revision 1.1.1.1  2005/02/16 18:58:25  jonblower
 * Initial import
 *
 */
public class StyxURLConnection extends URLConnection
{
    
    private StyxConnection conn; // The underlying Styx Connection
    private CStyxFile file; // the Styx file we are connecting to.
    
    /** Creates a new instance of StyxURLConnection */
    public StyxURLConnection(URL url)
    {
        super(url);
    }
    
    public void connect() throws IOException
    {
        if (!this.connected)
        {
            try
            {
                if (this.url.getUserInfo() == null)
                {
                    // No user info: connect anonymously
                    conn = new StyxConnection(this.url.getHost(), this.url.getPort());
                }
                else
                {
                    String[] els = this.url.getUserInfo().split(":");
                    String user = els[0];
                    String password = els.length > 1 ? els[1] : "";
                    conn = new StyxConnection(this.url.getHost(),
                        this.url.getPort(), user, password);
                }
                conn.connect();
                this.connected = true;
                this.file = conn.getFile(this.url.getPath());
                // TODO: check that the file exists here?
            }
            catch (Exception e)
            {
                throw new IOException(e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * This is empty because we don't need to set any header information
     * to find a resource on a Styx server
     */
    public void setRequestHeader(String name, String value) 
    {
    }
    
    public InputStream getInputStream() throws IOException
    {
        try
        {
            this.file.open(StyxUtils.OREAD);
            CStyxFileInputStream in = new CStyxFileInputStream(this.file, true);
            return in;
        }
        catch(Exception e)
        {
            throw new IOException(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public OutputStream getOutputStream() throws IOException
    {
        try
        {
            this.file.open(StyxUtils.OWRITE);
            CStyxFileOutputStream out = new CStyxFileOutputStream(this.file, true);
            return out;
        }
        catch(Exception e)
        {
            throw new IOException(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
