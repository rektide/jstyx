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
import java.io.OutputStream;
import java.util.Vector;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Class describing a Job that can be started and stopped.  Provides methods
 * to get the standard streams and monitor status changes
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class Job
{
    private static final Logger log = Logger.getLogger(Job.class);
    private static final Runtime runtime = Runtime.getRuntime();
    
    private Process process; // The process that will be executed
    private File workDir;    // The working directory of this job
    private String command;  // The command that will be run
    private String argList;  // The list of command-line arguments as a String
    private long startTime;  // The time the job was started
    private StatusCode statusCode;
    private CachingStreamReader stdout;   // The standard output from the program
    private CachingStreamReader stderr;   // The standard error from the program
    
    private Vector changeListeners; // Objects that are listening for changes to this Job
    
    /**
     * Creates a new instance of Job
     * @throws StyxException if the job could not be created (unlikely to happen)
     */
    public Job() throws StyxException
    {
        this.process = null;
        this.workDir = null;
        this.changeListeners = new Vector();
        this.stdout = new CachingStreamReader("stdout");
        this.stderr = new CachingStreamReader("stderr");
        this.statusCode = StatusCode.CREATED;
    }
    
    /**
     * Sets the working directory of this Job.  If the working directory
     * already exists it will be deleted along with all its contents
     * @param workDir The to the working directory
     * @throws StyxException if the working directory could not be created
     */
    public void setWorkingDirectory(File workDir) throws StyxException
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
     * given SGSParamFiles.  In this implementation, the parameters are translated
     * into a set of command-line arguments.
     * @param paramFiles Array of SGSParamFiles containing the parameters that
     * have been set
     */
    public void setParameters(SGSParamFile[] paramFiles)
    {
        // Get the argument list as a string 
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < paramFiles.length; i++)
        {
            // We can be pretty confident that this cast is safe
            SGSParamFile paramFile = paramFiles[i];
            String frag = paramFile.getCommandLineFragment();
            if (!frag.trim().equals(""))
            {
                buf.append(frag + " ");
            }
        }
        this.argList = buf.toString();
    }
    
    /**
     * Sets the command that will be executed by this Job
     */
    public void setCommand(String command)
    {
        this.command = command;
    }
    
    /**
     * Starts the job running
     * @throws StyxException if the job could not be started
     */
    public void start() throws StyxException
    {
        try
        {
            System.err.println("workdir = " + this.workDir.getPath());
            this.process = runtime.exec(this.command + " " + this.argList, null,
                this.workDir);
            this.startTime = System.currentTimeMillis();
            // Start a thread that waits for the process to finish, then sets the
            // status
            new Waiter().start();

            // Start reading from stdout and stderr. Note that we do this
            // even if the "stdout" and "stderr" streams are not exposed
            // through the Styx interface (we must do this to consume the
            // stdout and stderr data)
            this.stdout.setCacheFile(new File(workDir, "stdout"));
            this.stdout.startReading(this.process.getInputStream());
            this.stderr.setCacheFile(new File(workDir, "stderr"));
            this.stderr.startReading(this.process.getErrorStream());

            // Notify listeners that the job has started
            this.setStatus(StatusCode.RUNNING);
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            if (this.process == null)
            {
                // We didn't even start the process
                throw new StyxException("Internal error: could not create process "
                    + this.command + " " + this.argList);
            }
            else
            {
                // We've started the process but an error occurred elsewhere
                this.process.destroy();
                this.setStatus(StatusCode.ERROR, ioe.getMessage());
                throw new StyxException("Internal error: could not start "
                    + "reading from output and error streams");
            }
        }
    }
    
    /**
     * Gets an OutputStream representing the standard input of the Job
     * @return the OutputStream, or null if the underlying system is not ready yet
     */
    public OutputStream getStdin()
    {
        return this.process == null ? null : this.process.getOutputStream();
    }
    
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
     * client) opts to stop the job
     */
    public void stop()
    {
        synchronized(statusCode)
        {
            if (statusCode == StatusCode.RUNNING)
            {
                this.process.destroy();
                this.setStatus(StatusCode.ABORTED);
            }
        }
    }
    
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
     * Stops the job because of an error.
     * @param message Description of the error that occurred
     */
    public void error(String message)
    {
        this.process.destroy();
        this.setStatus(StatusCode.ERROR, message);
    }
    
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
    private void setStatus(StatusCode code, String message)
    {
        this.statusCode = code;
        this.fireStatusChanged(code, message);
    }
    
    /**
     * Sets the new status of the service and notifies all registered
     * JobChangeListeners
     * @param code The new StatusCode
     */
    private void setStatus(StatusCode code)
    {
        this.setStatus(code, null);
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
    private void fireStatusChanged(StatusCode newStatus, String message)
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
    private void fireGotExitCode(int exitCode)
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

    // Thread that waits for the executable to finish, then sets the status
    private class Waiter extends Thread
    {
        public void run()
        {
            try
            {
                int exitCodeVal = process.waitFor();
                long duration = System.currentTimeMillis() - startTime;
                synchronized(statusCode)
                {
                    // We must get the lock on the statusCode because
                    // we could be changing the status in another thread
                    if (statusCode != StatusCode.ABORTED && statusCode != StatusCode.ERROR)
                    {
                        // don't set the status if we have terminated abnormally
                        setStatus(StatusCode.FINISHED, "took " +
                            (float)duration / 1000 + " seconds.");
                    }
                    fireGotExitCode(exitCodeVal);
                }
            }
            catch(Exception e)
            {
                if (log.isDebugEnabled())
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
