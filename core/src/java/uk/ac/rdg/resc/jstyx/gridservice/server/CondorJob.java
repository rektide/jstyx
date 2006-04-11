/*
 * Copyright (c) 2006 The University of Reading
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * A Job that runs on a Condor pool.  The SGS server must be running on a Condor
 * submit node.  This creates Condor's job description file and runs condor_submit.
 * The form of the job description file will look like this:
 *
 * ########################
 * # Submit description file for a single GULP run
 * ########################
 * executable     = /home/sufs1/ru6/vx/vxx05160/jdb/gulp/Exe/gulp
 * universe       = vanilla 
 * input          = cordierite.dat                
 * output         = cordierite.out                
 * error          = cordierite.error             
 * log            = cordierite.log  
 * initialdir     = (working directory)                                                  
 * queue
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class CondorJob extends AbstractJob
{
    private static final Logger log = Logger.getLogger(AbstractJob.class);
    
    private static final String STDIN_FILE = "stdin"; // Name of the file that will
                                                      // contain standard input data
    private static final String STDOUT_FILE = "stdout"; // Name of the file that will
                                                        // contain standard output data
    private static final String STDERR_FILE = "stderr"; // Name of the file that will
                                                        // contain standard error data
    
    /**
     * Creates a new instance of CondorJob
     */
    public CondorJob(StyxGridServiceInstance instance, File workDir)
        throws StyxException
    {
        super(instance, workDir);
        this.stdout = new SGSOutputFile(new File(this.workDir, STDOUT_FILE), this);
        this.stderr = new SGSOutputFile(new File(this.workDir, STDERR_FILE), this);
    }
    
    /**
     * Sets the parameters of the Job.  These parameters are contained in the
     * given SGSParamFiles.
     * @param paramFiles Array of SGSParamFiles containing the parameters that
     * have been set
     */
    public void setParameters(SGSParamFile[] paramFiles)
    {
        // Do nothing for the moment: these will eventually end up in the submit file
    }
    
    /**
     * Sets the source of the data that is to be sent to the standard input
     * of the job.  This can be called before <b>or</b> after start().
     * @param url The URL from which the data will be read
     * @throws IOException if data could not be read from the given URL
     */
    public void setStdinURL(URL url) throws IOException
    {
        // TODO
    }
    
    /**
     * Starts the job running
     * @throws StyxException if the job could not be started
     */
    public void start() throws StyxException
    {
        // Create the condor submit file in the working directory
        
    }
    
    /**
     * Aborts the job, forcibly terminating it if necessary.  Does nothing if
     * the job is not running.  This is called when the user (i.e. the remote
     * client) opts to stop the job.  Implementations should remember to set
     * the status code to ABORTED if the stop operation is successful
     */
    public void stop()
    {
        // TODO
    }
    
    /**
     * Stops the job because of an error.  Implementations should remember to set
     * the status code to ERROR 
     * @param message Description of the error that occurred
     */
    public void error(String message)
    {
        // TODO
    }
    
    /**
     * This is called when it is confirmed that the standard input data have
     * been downloaded.  This is important in a CondorJob because the job cannot
     * start until all the input data are ready.
     */
    public void stdinDataDownloaded()
    {
    }
    
    /**
     * Gets an OutputStream representing the standard input of the Job.  In this
     * implementation, this is a FileOutputStream: data must be written to this file
     * before the job can start.
     * @return the OutputStream, or null if the underlying system is not ready yet
     */
    public OutputStream getStdinStream()
    {
        return null;//new FileOutputStream(new File(this.workDir, STDIN_FILE));
    }
    
}
