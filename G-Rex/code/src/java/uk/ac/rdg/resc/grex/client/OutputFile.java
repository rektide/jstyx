/*
 * Copyright (c) 2007 The University of Reading
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

package uk.ac.rdg.resc.grex.client;

import simple.xml.Attribute;
import simple.xml.Root;

/**
 * Client-side representation of an output file that can be downloaded from a
 * remote service instance, as contained in an InstanceResponse.  Matches up
 * with instance_xml.jsp.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="outputFile")
class OutputFile
{
    @Attribute(name="relPath")
    private String relativePath;
    
    @Attribute(name="lengthBytes")
    private long fileLength;
    
    @Attribute(name="lastModified")
    private long lastModified;  // TODO: change to proper, interoperable date type
    
    @Attribute(name="readyForDownload")
    private boolean readyForDownload;
    
    /** Creates a new instance of OutputFile */
    public OutputFile()
    {
    }

    /**
     * @return the path to the output file, relative to the base URL of the
     * service instance (e.g. "/outputs/stdout")
     */
    public String getRelativePath()
    {
        return relativePath;
    }

    public long getFileLengthBytes()
    {
        return fileLength;
    }

    /**
     * @todo replace with proper interoperable date string
     */
    public long getLastModified()
    {
        return lastModified;
    }

    public boolean isReadyForDownload()
    {
        return readyForDownload;
    }
    
}
