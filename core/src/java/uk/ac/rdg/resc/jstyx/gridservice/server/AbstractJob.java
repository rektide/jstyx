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

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.net.URL;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.server.StyxFile;

/**
 * Abstract class describing a job that can be run by a StyxGridService instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class AbstractJob
{
    private static final Logger log = Logger.getLogger(AbstractJob.class);
    
    protected StyxGridServiceInstance instance;  // The instance to which this job belongs
    protected File workDir;    // The working directory of this job
    protected String command;  // The command that will be run
    protected long startTime;  // The time the job was started
    protected StatusCode statusCode;
    protected StyxFile stdout;   // The standard output from the program
    protected StyxFile stderr;   // The standard error from the program
    protected URL stdinURL;      // The URL from which we will read data to be
                                 // sent to the standard input of the job
    protected int numSubJobs;    // The number of sub-jobs that make up this Job
    protected int runningSubJobs; // The number of sub-jobs that are currently running
    protected int failedSubJobs; // The number of sub-jobs that have failed
    protected int completedSubJobs; // The number of sub-jobs that have completed,
                                    // including those that have failed
    
    private Vector changeListeners; // Objects that are listening for changes to this Job
    
    /**
     * Creates a new instance of AbstractJob, setting the statusCode to CREATED
     * @param instance The StyxGridServiceInstance to which this job belongs
     * @throws StyxException if the working directory could not be created
     */
    public AbstractJob(StyxGridServiceInstance instance) throws StyxException
    {
        this.instance = instance;
        this.statusCode = StatusCode.CREATED;
        this.setWorkingDirectory(instance.getWorkingDirectory());
        this.changeListeners = new Vector();
        this.stdinURL = null;
        this.numSubJobs = 0;
        this.runningSubJobs = 0;
        this.failedSubJobs = 0;
        this.completedSubJobs = 0;
    }
    
    /**
     * Sets the working directory of this Job.  If the working directory
     * already exists it will be deleted along with all its contents
     * @param workDir The to the working directory
     * @throws StyxException if the working directory could not be created
     */
    private void setWorkingDirectory(File workDir) throws StyxException
    {
        if (workDir.exists())
        {
            // Delete the directory and all its contents
            deleteDir(workDir);
        }
        // (Re)create the working directory
        if (workDir.mkdirs())
        {
            log.debug("Created working directory " + workDir.getPath());
        }
        else
        {
            throw new StyxException("Unable to create working directory "
                + workDir);
        }
        this.workDir = workDir;
    }
    
    /**
     * Sets the parameters of the Job.  These parameters are contained in the
     * given SGSParamFiles.
     * @param paramFiles Array of SGSParamFiles containing the parameters that
     * have been set
     */
    public abstract void setParameters(SGSParamFile[] paramFiles);
    
    /**
     * Sets the command that will be executed by this Job
     */
    public void setCommand(String command)
    {
        this.command = command;
    }
    
    /**
     * Sets the URL of the data that is to be sent to the standard input
     * of the job.  This can be called before <b>or</b> after start().
     * @param url The URL from which the data will be read
     * @throws StyxException if data could not be read from the given URL, or
     * if another error occurred (e.g. starting a job on the Condor pool)
     */
    public abstract void setStdinURL(URL url) throws StyxException;
    
    /**
     * @return the OutputStream to which we can write data that will be sent
     * to the standard input of the job, or null if the stream is not ready yet.
     * Note that this method may be called several times by the framework - 
     * therefore, be careful not to create a new object every time if this 
     * means that a previous one will be overwritten!
     * @throws FileNotFoundException if the OutputStream could not be created
     */
    public abstract OutputStream getStdinStream() throws FileNotFoundException;
    
    /**
     * Starts the job running
     * @throws StyxException if the job could not be started
     */
    public abstract void start() throws StyxException;
    
    /**
     * Gets a StyxFile that clients will read to get data from the standard
     * output of the Job
     */
    public StyxFile getStdout()
    {
        return this.stdout;
    }
    
    /**
     * Gets a StyxFile that clients will read to get data from the standard
     * error stream of the Job
     */
    public StyxFile getStderr()
    {
        return this.stderr;
    }
    
    /**
     * Aborts the job, forcibly terminating it if necessary.  Does nothing if
     * the job is not running.  This is called when the user (i.e. the remote
     * client) opts to stop the job.  Implementations should remember to set
     * the status code to ABORTED if the stop operation is successful
     * @throws StyxException if there was an error stopping the job
     */
    public abstract void stop() throws StyxException;
    
    /**
     * Called when the service instance is destroyed.  Deletes the working
     * directory and all of its contents.
     */
    public void destroy()
    {
        // Now remove the working directory
        deleteDir(this.workDir);
    }
    
    /**
     * Stops the job because of an error.  Implementations should remember to set
     * the status code to ERROR 
     * @param message Description of the error that occurred
     */
    public abstract void error(String message);
    
    /**
     * This is called when it is confirmed that the standard input data have
     * been downloaded.  This implementation does nothing but subclasses can 
     * override this method if they want to be notified of this event.
     * @throws StyxException if this causes an error to be generated (e.g. this
     * event might cause a job to be submitted to a Condor pool, which might
     * generate errors)
     */
    public void stdinDataDownloaded() throws StyxException {}
    
    /**
     * Recursive method for deleting a directory and its contents
     * @return true if the deletion was successful, false otherwise
     */
    private static boolean deleteDir(File dir)
    {
        log.debug("Deleting contents of " + dir.getPath());
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                File theDir = new File(dir, children[i]);
                boolean success = deleteDir(theDir);
                if (!success)
                {
                    log.debug("Error deleting " + theDir.getAbsolutePath());
                    return false;
                }
            }
        }
        return dir.delete();
    }
    
    /**
     * Gets the status code of this job
     */
    public StatusCode getStatusCode()
    {
        return this.statusCode;
    }
    
    /**
     * Sets the new status of the service and notifies all registered
     * JobChangeListeners
     * @param code The new StatusCode
     * @param message String containing supplementary information
     */
    protected void setStatus(StatusCode code, String message)
    {
        this.statusCode = code;
        this.fireStatusChanged(code, message);
    }
    
    /**
     * Sets the new status of the service and notifies all registered
     * JobChangeListeners
     * @param code The new StatusCode
     */
    protected void setStatus(StatusCode code)
    {
        this.setStatus(code, null);
    }
    
    /**
     * Sets the number of sub-jobs in this Job
     */
    protected synchronized void setNumSubJobs(int numSubJobs)
    {
        log.debug("Got number of subjobs: " + numSubJobs);
        this.numSubJobs = numSubJobs;
        this.fireProgressChanged();
    }
    
    /**
     * Called when a sub-job has started
     * @param subJobID The ID number of the sub-job that has started
     */
    protected synchronized void subJobStarted(int subJobID)
    {
        log.debug("Subjob " + subJobID + " started");
        this.runningSubJobs++;
        this.fireProgressChanged();
    }
    
    /**
     * Called when a sub-job has failed
     * @param subJobID The ID number of the sub-job that has failed
     */
    protected synchronized void subJobFailed(int subJobID)
    {
        log.debug("Subjob " + subJobID + " failed");
        this.runningSubJobs--;
        this.failedSubJobs++;
        this.completedSubJobs++;
        this.fireProgressChanged();
    }
    
    /**
     * Called when a sub-job has completed successfully
     * @param subJobID The ID number of the sub-job that has completed
     */
    protected synchronized void subJobCompleted(int subJobID)
    {
        log.debug("Subjob " + subJobID + " completed successfully");
        this.runningSubJobs--;
        this.completedSubJobs++;
        this.fireProgressChanged();
    }
    
    /**
     * Adds a listener that will be notified of changes to this Job. If the
     * listener is already registered, this will do nothing.
     */
    public void addChangeListener(JobChangeListener listener)
    {
        synchronized(this.changeListeners)
        {
            if (!this.changeListeners.contains(listener))
            {
                this.changeListeners.add(listener);
            }
        }
    }
    
    /**
     * Removes a JobChangeListener.  (Note that this will only remove the first
     * instance of a given JobChangeListener.  If, for some reason, more than one 
     * copy of the same change listener has been registered, this method will
     * only remove the first.)
     */
    public void removeChangeListener(JobChangeListener listener)
    {
        synchronized(this.changeListeners)
        {
            boolean contained = this.changeListeners.remove(listener);
        }
    }
    
    /**
     * Fires the statusChanged() event on all registered JobChangeListeners
     */
    protected void fireStatusChanged(StatusCode newStatus, String message)
    {
        synchronized(this.changeListeners)
        {
            JobChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (JobChangeListener)this.changeListeners.get(i);
                listener.statusChanged(newStatus, message);
            }
        }
    }
    
    /**
     * Fires the gotExitCode() event on all registered JobChangeListeners
     */
    protected void fireGotExitCode(int exitCode)
    {
        synchronized(this.changeListeners)
        {
            JobChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (JobChangeListener)this.changeListeners.get(i);
                listener.gotExitCode(exitCode);
            }
        }
    }
    
    /**
     * Fires the progressChanged() event on all registered JobChangeListeners.
     */
    protected void fireProgressChanged()
    {
        synchronized(this.changeListeners)
        {
            JobChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (JobChangeListener)this.changeListeners.get(i);
                listener.progressChanged(this.numSubJobs, this.runningSubJobs,
                    this.failedSubJobs, this.completedSubJobs);
            }
        }
    }
    
    // Reads from an input stream and writes the result to an output stream
    protected class RedirectStream extends Thread
    {
        private InputStream is;
        private OutputStream os;
        
        public RedirectStream(InputStream is, OutputStream os)
        {
            this.is = is;
            this.os = os;
        }
        
        public void run()
        {
            try
            {
                byte[] arr = new byte[8192]; // TODO: is this an appropriate buffer size?
                int n = 0;
                while (n >= 0)
                {
                    n = this.is.read(arr);
                    if (n >= 0)
                    {
                        this.os.write(arr, 0, n);
                    }
                }
            }
            catch(IOException ioe)
            {
                if (statusCode != StatusCode.ABORTED)
                {
                    // don't do this if the process was aborted manually
                    error("when reading input data: " + ioe.getMessage());
                }
            }
            finally
            {
                try
                {
                    // Close the streams
                    this.is.close();
                    this.os.close();
                }
                catch(IOException ioe)
                {
                    // Ignore errors when closing the streams.
                }
            }
        }
    }
    
}
