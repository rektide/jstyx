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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.SortedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import uk.ac.rdg.resc.grex.config.Output;
import uk.ac.rdg.resc.grex.config.Parameter;
import uk.ac.rdg.resc.grex.db.Job;

/**
 * Runs a job on the G-Rex server itself.
 * @todo Adapt for sub-jobs or disallow somehow
 * @todo Think very carefully about race conditions between state updates, 
 * particularly when erroring out.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class LocalJobRunner extends AbstractJobRunner
{
    private static final Log log = LogFactory.getLog(LocalJobRunner.class);
    
    protected Process proc; // The Process that we are running in this job
    protected long NewFilesCheckIntervalMs = 2000;
 
    /**
     * The task of the start() method is to prepare the job, then kick it off, 
     * setting the state of the job to RUNNING or ERROR before returning
     */
    public void start()
    {
        log.debug("Starting execution of service instance " + this.instance.getId());
        
        // Prepare the command line that will be executed
        String cmdLine = this.constructCommmandLine();
        
        
        File wdFile = new File(this.instance.getWorkingDirectory());
        
        try
        {
            // If command is to be run on a remote machine, add remote shell command to command line
            if (gsConfig.getRemoteHost()!="localhost") {
                String tempCommand = gsConfig.getRemoteShell() + " " + gsConfig.getRemoteHost() +
                        " cd " + wdFile.getAbsolutePath() + "; " + cmdLine;
                cmdLine = tempCommand;
            }
            log.debug("Command line that will be executed: \"" + cmdLine + "\"");

            // Write job and user details to log file
            log.info("User " + this.instance.getOwner() + " started instance " + this.instance.getId() +
                " of service " + this.instance.getServiceName() + ", command = \"" + cmdLine + "\"");
            
            // Start the process running, setting the working directory
            this.proc = Runtime.getRuntime().exec(cmdLine, null, wdFile);
            log.debug("Process started");
            
            // Update the state of the instance
            this.instance.setState(Job.State.RUNNING);
           
            // Start a thread that waits for the process to finish and grabs the
            // exit code
            new WaitProcess().start();
        
            // Start a thread that redirects the standard input data to the process
            File stdinFile = new File(wdFile, STDIN);
            // TODO: should we check at an earlier stage whether this exists?
            if (stdinFile.exists())
            {
                FileInputStream fin = new FileInputStream(stdinFile);
                new RedirectStream(fin, this.proc.getOutputStream(), STDIN).start();
            }

            // Start threads to consume the output streams
            File stdoutFile = new File(wdFile, STDOUT);
            File stderrFile = new File(wdFile, STDERR);
            new RedirectStream(this.proc.getInputStream(),
                new FileOutputStream(stdoutFile), STDOUT).start();
            new RedirectStream(this.proc.getErrorStream(),
                new FileOutputStream(stderrFile), STDERR).start();
            
            // Start thread to find out which output files can be deleted. Set the
            // checking interval to 2000ms
            new CheckOutputFiles(2000).start();
             
        }
        catch(FileNotFoundException fnfe)
        {
            // Unlikely to happen
            log.error("Can't create file for output stream", fnfe);
            // TODO: save the error message
            this.instance.setState(Job.State.ERROR);
        }
        catch(IOException ioe)
        {
            log.error("Error starting process for instance " + this.instance.getId());
            // TODO: save the error message
            this.instance.setState(Job.State.ERROR);
        }
        finally
        {
            // Save the new state of the instance to the persistent store
            this.saveInstance();
        }
    }
    
    /**
     * Constructs the full command line that will be executed
     */
    protected String constructCommmandLine()
    {
        StringBuffer cmdLine = new StringBuffer(this.gsConfig.getCommand());
        
        // Look through all the command-line parameters, inserting the values
        for (Parameter param : this.gsConfig.getParams())
        {
            String paramValue = this.instance.getParamValue(param.getName());
            
            // Don't add DRM parameters or unspecified optional parameters to command line
            if (param.isDRMparameter() || paramValue.contains(Parameter.UNSPECIFIED)) continue;
            
            if (paramValue == null)
            {
                if (param.isRequired())
                {
                    // Shouldn't happen: we've checked in
                    // PostOperationController.controlServiceInstance()
                    throw new AssertionError("Internal error: parameter " +
                        param.getName() + " must have a value");
                }
            }
            else
            {
                cmdLine.append(" ");
                if (paramValue.contains(" ") && !param.isGreedy())
                {
                    paramValue = "\"" + paramValue + "\"";
                }
                if (param.getType() == Parameter.Type.SWITCH)
                {
                    if (paramValue.equalsIgnoreCase("true"))
                    {
                        if (param.getLongFlag() == null)
                        {
                            // We have already validated (in Parameter.validate)
                            // that either the short or long flag (or both) is set
                            cmdLine.append("-" + param.getFlag());
                        }
                        else
                        {
                            cmdLine.append("--" + param.getLongFlag());
                        }
                    }
                }
                else if (param.getType() == Parameter.Type.FLAGGED_OPTION)
                {
                    if (param.getLongFlag() == null)
                    {
                        cmdLine.append("-" + param.getFlag() + " " + paramValue);
                    }
                    else
                    {
                        cmdLine.append("--" + param.getLongFlag() + " " + paramValue);
                    }
                }
                else if (param.getType() == Parameter.Type.UNFLAGGED_OPTION)
                {
                    cmdLine.append(paramValue);
                }
                else
                {
                    // Shouldn't happen
                    throw new AssertionError("Unrecognized parameter type");
                }
            }
        }
        return cmdLine.toString();
    }
    
    /**
     * Simple class to read an input stream and write it to an output stream
     */
    protected class RedirectStream extends Thread
    {
        private InputStream in;
        private OutputStream out;
        
        public RedirectStream(InputStream in, OutputStream out, String name)
        {
            super("redirect-stream-" + instance.getId() + "-" + name);
            this.in = in;
            this.out = out;
        }
        
        public void run()
        {
            log.debug("Redirecting stream");
            try
            {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) >= 0)
                {
                    out.write(buf, 0, len);
                }
            }
            catch(IOException ioe)
            {
                // Unlikely to happen
                log.error("Error redirecting stream", ioe);
                instance.setState(Job.State.ERROR);
                saveInstance();
                // TODO: store the error message?
            }
            finally
            {
                try
                {
                    in.close();
                    out.close();
                }
                catch(IOException ioe)
                {
                    log.error("Error closing input or output stream in RedirectStream");
                }
            }
            log.debug("Finished redirecting stream");
        }
    }
    
    /**
     * Thread that waits for the process to finish, setting the state and the
     * exit code
     */
    private class WaitProcess extends Thread
    {
        public WaitProcess()
        {
            // TODO: should be the instance and job ID
            super("wait-" + instance.getId());
        }
        public void run()
        {
            boolean done = false;
            while (!done)
            {
                try
                {
                    log.debug("Waiting for process to finish");
                    int exitCode = proc.waitFor();
                    log.debug("Process finished with exit code " + exitCode);
                    done = true;
                    // Don't change state to FINISHED if already aborted or an
                    // error has occurred
                    // TODO: read the state from the persistent store, just to be safe?
                    if (!instance.getState().meansFinished())
                    {
                        instance.setState(Job.State.FINISHED);
                        log.debug("changed instance state to " + instance.getState());
                    }
                    instance.setExitCode(exitCode);
                    saveInstance();
                    log.debug("saved instance state");
                    log.info("Instance " + instance.getId() + " of service " + instance.getServiceName() +
                            " belonging to User " + instance.getOwner() + " has finished.");

                }
                catch(InterruptedException ie)
                {
                    log.warn("waitFor() interrupted");
                }
            }
        }
    }
    
    
    /*
     Thread that periodically checks the last modified time for output files to
     find out which can be deleted.  In the future the threshold value of time
     since last modification will be supplied by the user in the G-Rex config */
    protected class CheckOutputFiles extends Thread {
        
        private long checkIntervalMs;
        private int numOutputFiles=0, numOutputFinished=0;
        private int prevNumOutputFiles=0, prevNumOutputFinished=0;
            
        public CheckOutputFiles(long checkIntervalMs)
        {
            super("checkOutputFiles-" + instance.getId());
            this.checkIntervalMs = checkIntervalMs;
        }
        public void run()
        {
            try {
                log.debug("Checking output files...");
                do {
                    // Wait for the required time before checking again
                    Thread.sleep(checkIntervalMs);
                    numOutputFiles=0;
                    
                    // Make sure our view of the instance is up to date
                    instance = instancesStore.getServiceInstanceById(instance.getId());
                    
                    // First delete all the existing elements in the set of all output
                    // files and the set of finished files
                    getOutputFiles().clear();
                    getOutputFinished().clear();
                
                    /* Update sets of output files for all jobs */
                    updateOutputFiles(instance.getMasterJob());                    
                    for (Job subJob : instance.getSubJobs()) updateOutputFiles(subJob);
                                        
                    // Report numbers if different from last time
                    if (numOutputFiles!=prevNumOutputFiles)
                        log.debug("Total No. of downloadable output files is " +  numOutputFiles);
                    prevNumOutputFiles = numOutputFiles;
                    //
                    numOutputFinished = getOutputFinished().size();
                    if (numOutputFinished!=prevNumOutputFinished)
                        log.info("No. of finished files is " +  numOutputFinished);
                    prevNumOutputFinished = numOutputFinished;
                                                                            
                 } while (!instance.isFinished());
            }
            catch (InterruptedException ie) {}
            catch (Exception ex) {
                log.error(ex.toString(), ex);
            }
        }
              
       
        /**
         * Maintains sets of output files, including the set of all output files
         * and the set of files that have finished being writted to.
        */
        public void updateOutputFiles(Job job)
        {            
            
            // Add output files belonging to this job.  The
            // addCurrentOutputFiles method recursively descends the directory
            // structure in the working directory.
            addCurrentOutputFiles("", getOutputFiles(), job);
            
        }
    
        /**
        * Recursive method to get the output files in the the directory whose
        * path (relative to the instance's working directory) is given by
        * relativeDirPath.  The results are added to the given List of OutputFiles.
        * If relativeDirPath is not the empty string, it must end with a forward
        * slash (irrespective of operating system).
        */
        private void addCurrentOutputFiles(String relativeDirPath, SortedSet<OutputFile> files, Job job)
        {
            File dir = new File(job.getWorkingDirectoryFile(), relativeDirPath);
            //log.debug("Working directory of job: " + dir.getPath());
            for (String filename : dir.list())
            {
                //log.debug("Checking file or directory " + filename + "...");
                String relativePath = relativeDirPath + filename;
                File f = new File(dir, relativePath);
                if (f.isDirectory())
                {
                    // recursively call this method
                    // We must always use forward slashes even on Windows for the
                    // pattern matching in getOutputFile() to work
                    addCurrentOutputFiles(relativePath + "/", files, job);
                }
                else
                {
                    // Check to see if this file is downloadable or deleteable
                    OutputFile opFile = job.getOutputFile(relativePath);
                    if (opFile != null) {
                        numOutputFiles++;
                        //files.add(opFile);
                        //log.debug("Downloadable file " + opFile.getFile().getName() + 
                        //        " will be finished " + opFile.deleteAfter() + " mins after last write");
                        /*
                        Decide whether output to file has finished
                        */
                        long maxTime=opFile.deleteAfter()*60*1000; // Convert time in minutes to milliseconds
                        long now = new Date().getTime();
                        long time = now - opFile.getFile().lastModified();            
                        /* If time since last modified is longer than a certain maximum
                        then output must have finished.  If so, add file name to list of
                         finished files in the master job object. A negative value of
                         maxTime means that the file should not be deleted until the
                         end of the job. */
                        if (maxTime >= 0 && time > maxTime) {
                            //log.debug("Time since last write to " + opFile.getFile().getName() +
                            //        " is " + time + " ms. Maximum is " + maxTime + "ms (" + opFile.deleteAfter() + "min)");
                            addFinishedFile(opFile);
                        }
                        
                    }
                }
            }
        }

    }
    
    
    public void abort()
    {
        log.debug("Destroying process for service instance " + this.instance.getId());
        // Must change the state before destroying the process otherwise there
        // will be a race condition with the WaitProcess() thread
        this.instance.setState(Job.State.ABORTED);
        this.proc.destroy();
        this.saveInstance();
    }
    
}
