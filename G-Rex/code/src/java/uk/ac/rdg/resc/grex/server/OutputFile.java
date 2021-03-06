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
import java.util.Date;
import uk.ac.rdg.resc.grex.db.Job;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    private String relativePath; // Path relative to the working directory of the instance
    private File file; // underlying File
    private Job job;
    private boolean appendOnly;
    private int deleteAfter;
    private static long INITIAL_CHECKSUM=0;
    
    private static final Log log = LogFactory.getLog(OutputFile.class);
    
    /**
     * Creates a new instance of OutputFile
     * @param relativePath Path relative to the working directory of the instance
     * @param job The job to which this output file belongs
     * @param appendOnly True if this file represents a file that is only ever
     * appended to during a run
     */
    public OutputFile(String relativePath, Job job, boolean appendOnly, int deleteAfter)
    {
        this.relativePath = relativePath;
        this.file = new File(job.getWorkingDirectoryFile(), relativePath);
        this.job = job;
        this.appendOnly = appendOnly;
        this.deleteAfter = deleteAfter;
    }
        
    /**
     * @return true if the file can be downloaded immediately (which will be the
     * case if this is an append-only file or if output to the file has finished),
     * or if the service instance has finished.
     */
    public boolean isReadyForDownload()
    {        
       return this.appendOnly || this.isOutputFinished() || this.job.isFinished() ;
    }
        
    /**
     * @return true if output to the file has finished.
     *The method for determining whether or not output to a file has finished
     *is dependent on what type of service this is.  Currently the method
     *is only defined for services of type "sge" and "local", which is why the
     *default value for outputFinished is false.
     *
     */
    public boolean isOutputFinished()
    {
        boolean outputFinished = false;
        //log.debug("Checking " + this.getRelativePath() + " to find out if output has finished");

        String serviceType = this.getJob().getInstance().getGridServiceConfig().getType();
        if (serviceType.equals("sge") || serviceType.equals("local")) {
            //log.debug("Service type = " + serviceType);
            long maxTime=this.deleteAfter()*60*1000; // Convert time in minutes to milliseconds
            long now = new Date().getTime();
            long time = now - this.getFile().lastModified();            
            if (maxTime >= 0 && time > maxTime) {
                outputFinished=true;
                //log.debug("Time since last write to " + this.getRelativePath() + " = " + time + "ms.  Output to the file has finished");
            }
        }
        
        return outputFinished;
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
     * @return the number of minutes since the last modification after which
     * output to the file is considered to have finished.  This value is defined
     * in GRexConfig.xml.  Note that this parameter is currently used for
     * service types "local" and "sge", but other job types (not yet written)
     * may use a different method for deciding whether or not output to a file has finished.
     */
    public int deleteAfter()
    {
        return this.deleteAfter;
    }
    
    /**
     * @return the length of the file in bytes
     */
    public long getLengthBytes()
    {
        return this.file.length();
    }
    
    /**
     * @return the time at which the file was last modified,
     * or the current time if the file is empty. The
     * current time is returned if the file is empty in order
     * to distinguish between a newly created file and a
     * file that has actually been written to. This distinction
     * is important because the last modified time is used
     * by the job runner to decide whether output to the 
     * file has finished.
     * @todo return ISO compatible string, timezone-independent
     */
    public long getLastModified()
    {
        long lastModified = new Date().getTime();
        if (this.getFile().length() > 0) lastModified = this.getFile().lastModified();
        return lastModified;
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
     * @return the job to which this file belongs
     */
    public Job getJob()
    {
        return this.job;
    }
    
    /**
     * @return the check sum of the file. Not yet implemented.
     */
    public long getCheckSum()
    {
        long checkSum=INITIAL_CHECKSUM;
        return checkSum;
    }
    
    /* These are needed to enable the OutputFile class to implement the
     * Comparable interface, which is needed to enable sorted sets of OutputFile
     * objects
     */
    public int compareTo(OutputFile outputFile) {
        return this.getFile().getName().compareTo(outputFile.getFile().getName());
    }
    /*
     * It would be nice to store files in date order to enable the client to be
     * sent a list of only the oldest files (i.e. longest time since modified).
     * Unfortunately this allows two or more files to have the same key value,
     * leading to incorrect identification of finished files
     */
    /*
    public int compareTo(OutputFile outputFile) {
        int retval=0;
        if (this.getFile().lastModified() < outputFile.getFile().lastModified()) {
            retval=-1;
        }
        else if (this.getFile().lastModified() > outputFile.getFile().lastModified()) {
            retval=1;
        }
        return retval;
        //return this.getRelativePath().compareTo(outputFile.getRelativePath());
    }
     */
    public int compareTo(Object o) {
        return this.compareTo((OutputFile)o);
    }
    
}
