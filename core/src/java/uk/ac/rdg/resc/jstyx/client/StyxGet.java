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

package uk.ac.rdg.resc.jstyx.client;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * A simple application that downloads a file from a remote server and times
 * the download, providing an indication of progress as it goes.
 *
 * @author Jon Blower
 * $Revision$
 * $Date $
 * $Log$
 * Revision 1.1  2005/03/07 08:27:52  jonblower
 * Initial import
 *
 */
public class StyxGet extends CStyxFileChangeAdapter
{
    private URL styxURL;
    private File targetDir;
    
    public StyxGet(String styxURL, String targetDir) throws Exception
    {
        try
        {
            this.styxURL = new URL(styxURL);
        }
        catch(MalformedURLException mue)
        {
            throw new Exception("Not a valid URL (have you set the" +
                " java.protocol.handler.pkgs property correctly?)");
        }
        if (!this.styxURL.getProtocol().equals("styx"))
        {
            throw new Exception("Can only download from styx:// URLs");
        }
        String path = this.styxURL.getPath();
        if (path.endsWith("/"))
        {
            throw new Exception("Cannot download a directory");
        }
        this.targetDir = new File(targetDir);
        if (!this.targetDir.exists() || !this.targetDir.isDirectory())
        {
            throw new Exception(targetDir + " is not a directory");
        }
        
        // Get the name of the remote file (i.e. the last part of the path)
        int lastSlash = path.lastIndexOf("/");
        // We've already checked that the URL doesn't end with a slash
        String filename = path.substring(lastSlash + 1);
        
        // Check that the file we're going to create doesn't exist
        File targetFile = new File(targetDir, filename);
        if (targetFile.exists())
        {
            throw new Exception(targetFile + " already exists");
        }
    }
    
    public void get() throws StyxException
    {
        StyxConnection conn = new
            StyxConnection(this.styxURL.getHost(), this.styxURL.getPort());
        conn.connect();
        CStyxFile file = conn.openFile(this.styxURL.getPath(), StyxUtils.OREAD);
        long length = file.getLength();
        
    }
    
    public static void main (String[] args) throws Exception
    {
        // TODO: throw up a GUI if no arguments or if -gui switch used?
        if (args.length != 2)
        {
            System.err.println("Usage: StyxGet <styx:// URL> <target directory>");
            return;
        }
        
    }
    
}
