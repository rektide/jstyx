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
import java.util.zip.Checksum;
import java.lang.Number;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.db.Job;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private String relativePath; // Path relative to the working directory of the instance
    private File file; // underlying File
    private Job job;
    private boolean appendOnly;
    private long checkSum=0; /* This should remain zero until output to the file has finished */

    private static final Log log = LogFactory.getLog(OutputFile.class);
    
    /**
     * Creates a new instance of OutputFile
     * @param relativePath Path relative to the working directory of the instance
     * @param job The job to which this output file belongs
     * @param appendOnly True if this file represents a file that is only ever
     * appended to during a run
     */
    public OutputFile(String relativePath, Job job, boolean appendOnly)
    {
        this.relativePath = relativePath;
        this.file = new File(job.getWorkingDirectoryFile(), relativePath);
        this.job = job;
        this.appendOnly = appendOnly;
    }
    
    /**
     * @return true if the file can be downloaded immediately, which will be the
     * case if this is an append-only file, if the service instance has finished
     * or if this is one of the standard streams (stdout or stderr).
     */
    public boolean isReadyForDownload()
    {
        return this.appendOnly || this.job.isFinished() ||
            this.relativePath.equals(AbstractJobRunner.STDOUT) ||
            this.relativePath.equals(AbstractJobRunner.STDERR);
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
        return this.relativePath;
    }
    
    /**
     * @return the path of the file relative to the working directory of the
     * instance, encoded as a URL
     * @todo Implement properly: only handles spaces at the moment
     */
    public String getRelativePathUrlEncoded()
    {
        return this.relativePath.replaceAll(" ", "%20");
    }
    
    /**
     * @return the underlying java.io.File object
     */
    public File getFile()
    {
        return this.file;
    }
    
    /**
     * @return the check sum of the file.  This will return zero
     * if the file is still being written to and (for the time
     * being) a non zero number if output to the file has finished and
     * it can be deleted from the server.
     */
    public long getCheckSum()
    {
        log.debug("Calculating checksum of file " + this.getFile().getName());
        if (!this.outputFinished()) this.checkSum = 0;
        else {
            log.debug("Output to file " + this.getFile().getName() + " has finished. Setting checksum to 1");
            this.checkSum=1;
        }     
        
        return this.checkSum;
    }
    
    /*
     * Decides whether or not output to a file has finished.  This assumes
     * that output takes place over a short space of time in the form of a
     * data dump, after which the file is complete.
     */
    public boolean outputFinished()
    {
        long age=0, maximumWriteTime=300;
        /* Find out when file was created */
        Long creationTimeObject = new Long(0);
        creationTimeObject = (Long)this.job.getCreationTimes().get(this.getFile().getName());
        long creationTime = creationTimeObject.longValue();
        if (creationTime == 0) {
            log.error("File " + this.getFile().getName() + " not in creation times list");
            throw new RuntimeException("File " + this.getFile().getName() + " not in creation times list");
        }
        
        /* Calculate age of file.  If longer than the maximum possible time
         * taken to write the output to a file then output must have finished. */
        age = this.getFile().lastModified() - creationTime;
        if (age > maximumWriteTime) return true;
        else return false;
    }
    
}
