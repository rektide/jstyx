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

package uk.ac.rdg.resc.grex.server;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;

/**
 * Java Bean representing an output file that belongs to a service instance and
 * can be downloaded by a client via the web.  We use this abstraction of a
 * java.io.File because the JSP pages are much easier to construct from Java beans.
 *
 * @todo Is this the right package for this class?  Should really be in the same
 * package as GRexServiceInstance, but maybe GRSI is in the wrong package...
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class OutputFile
{
    private File file; // The file itself
    private GRexServiceInstance instance;
    private boolean appendOnly;
    
    /**
     * Creates a new instance of OutputFile
     * @param file The File object representing this output file
     * @param instance The instance to which this output file belongs
     * @param appendOnly True if this file represents a file that is only ever
     * appended to during a run
     */
    public OutputFile(File file, GRexServiceInstance instance, boolean appendOnly)
    {
        this.file = file;
        this.instance = instance;
        this.appendOnly = appendOnly;
    }
    
    /**
     * @return true if the file can be downloaded immediately, which will be the
     * case if this is an append-only file or if the service instance has finished.
     */
    public boolean isReadyForDownload()
    {
        return this.appendOnly || this.instance.isFinished();
    }
    
    /**
     * @return true if the file is known to be "append-only", meaning that it 
     * can be partially downloaded before the service has completed.
     */
    public boolean isAppendOnly()
    {
        return this.appendOnly;
    }
    
    /**
     * @return the length of the file in bytes
     */
    public long getLengthBytes()
    {
        return this.file.length();
    }
    
    /**
     * @return the time at which the file was last modified
     * @todo return ISO compatible string, timezone-independent
     */
    public long getLastModified()
    {
        return this.file.lastModified();
    }
    
    /**
     * @return the path of the file relative to the working directory of the
     * instance.
     */
    public String getRelativePath()
    {
        try
        {
            // This is pretty crude but effective
            String wdPath = new File(this.instance.getWorkingDirectory()).getCanonicalPath();
            return this.file.getCanonicalPath().substring(wdPath.length() + 1);
        }
        catch(IOException ioe)
        {
            // Very unlikely to happen!
            return "ERROR";
        }
    }
    
    /**
     * @return the underlying java.io.File object
     */
    public File getFile()
    {
        return this.file;
    }
    
}
