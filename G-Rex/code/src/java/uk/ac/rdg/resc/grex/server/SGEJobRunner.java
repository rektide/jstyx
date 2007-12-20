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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rdg.resc.grex.db.Job;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.rdg.resc.grex.config.Option;
import uk.ac.rdg.resc.grex.config.Parameter;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.exceptions.InstancesStoreException;
import uk.ac.rdg.resc.grex.server.LocalJobRunner.CheckOutputFiles;

/**
 * Runs a Sun Grid Engine job.
 *
 * @author Dan Bretherton
 * $Revision$
 * $Date$
 * $Log$
 */
public class SGEJobRunner extends LocalJobRunner
{
    private static final Log log = LogFactory.getLog(SGEJobRunner.class);
    private String submitScript;
    private static final String SUBMIT_SCRIPT_NAME = "submit.sh";
    private static final Pattern QSUB_SUCCESS_PATTERN =
        Pattern.compile("Your job ([0-9]*).*");
    private static final Pattern QSUB_FAILURE_PATTERN_1 =
        Pattern.compile("qsub: Unknown option ");  // Not used at the moment. A non-empty error stream means there is an error.
    /*private static final Pattern QSTAT_PATTERN =
        Pattern.compile("[ ]*([0-9]*)[ ]*[^ \t\n\f\r]*[ ]*[^ \t\n\f\r]*[ ]*[^ \t\n\f\r]*[ ]*([^0-9]*).*");*/
    private static final Pattern QSTAT_PATTERN =
        Pattern.compile("[ ]*([0-9]*)[ ]*[^ \t\n\f\r]*[ ]*[^ \t\n\f\r]*[ ]*[^ \t\n\f\r]*[ ]*([^0-9]*).*");
    private static final Pattern QSTAT_PATTERN2 =
        Pattern.compile("[ ]*([0-9]*).*");
    // Example below
    //  50280 0.52500 UMABQXL000 cll          qw    11/29/2007 10:21:54                                    4
    private int sgeJobID=0;
    private int SGE_SUCCESS=0;
    private int SGE_FAILURE=1; // An arbitrary non zero value, nothing to do with SGE
    private int exitCode=SGE_SUCCESS; // will be changed to SGE_FAILURE if SGE related error is detected
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private File submitFile;
    private String qsubCommand="qsub";  // Will be either "qsub" or something like "ssh gorgon qsub"
    private String qstatCommand="qstat"; // Will be either "qstat" or something like "ssh gorgon qstat"
    
    /** Creates a new instance of SGEJobRunner */
    public SGEJobRunner()
    {
    }
    
    /**
     * The task of the start() method is to prepare the job, then kick it off, 
     * setting the state of the job to RUNNING, PENDING or ERROR before returning.
     * Also launches the job and output file monitoring threads.
     */
    public void start()
    {
        // Make sure our view of the instance is up to date
        try {
            this.instance = instancesStore.getServiceInstanceById(this.instance.getId());
        }
        catch (InstancesStoreException ise) {
            log.error("Error retrieving instance from persistant store: " + ise.toString());
        }
        
        log.debug("Starting execution of service instance " + this.instance.getId());
        log.debug("State of service instance " + this.instance.getId() + " is " + this.instance.getState());
        
        // Prepare the application run script command line
        String cmdLine = constructCommmandLine();
        log.debug("Application run script command line: \"" + cmdLine + "\"");
                        
        // Write job and user details to log file
        log.info("User " + this.instance.getOwner() + " started instance " + this.instance.getId() +
                " of service " + this.instance.getServiceName() + ", command = \"" + cmdLine + "\"");
        
        // Get pointer to G-Rex working directory for this instance
        File wdFile = new File(this.instance.getWorkingDirectory());
        
        // Find out if we need to execute SGE commands on a remote submit host
        if (gsConfig.getRemoteHost()!="localhost") {
            qsubCommand = gsConfig.getRemoteShell() + " " + gsConfig.getRemoteHost() + " cd " + wdFile.getAbsolutePath() + "; qsub";
            qstatCommand = gsConfig.getRemoteShell() + " " + gsConfig.getRemoteHost() + " qstat";
        }
        
        // Create script to be submitted
        try {            
            submitFile = new File(wdFile.getPath() + FILE_SEPARATOR + SUBMIT_SCRIPT_NAME);

            PrintStream pstream = new PrintStream(new FileOutputStream(
                submitFile, false));
            
            log.debug("Writing to " + submitFile.getPath());
            pstream.println("#!/bin/bash");
            // Information
            pstream.println("echo \"Starting script\" $0");
            pstream.println("date");
            // For easier debugging..
            pstream.println("set -uax");
            // Change to G-Rex working directory
            pstream.println("cd " + wdFile.getPath());
            
            // Apply SGE options
            boolean nameSupplied=false;
            String jobName, key, value;
            for ( Option option : gsConfig.getOptions() ) {
                
                /*
                 * Add G-Rex user ID to SGE job name
                 */

                // Substitute a space for null values
                if (option.getKey()==null) key=" ";
                else key=option.getKey();
                if (option.getValue()==null) value=" ";
                else value=option.getValue();
                
                // If this option is linked to a parameter, set value to be the
                // value of the parameter in question
                if (option.getLinkedParameterName()!=null) {
                    value = this.instance.getMasterJob().getParameters().get(option.getLinkedParameterName());
                }
                
                // Do not include options linked to optional parameters that have
                // not been specified by user and for which no default value has been
                // specified on command line
                if (value.contains(Parameter.UNSPECIFIED)) continue;
                
                // Start a new line if the key value begins with #$
                if (key.contains("#$")) pstream.println();
                
                // If job name has been supplied as an option, add the G-Rex user ID
                if (key.contains("#$") && key.contains("-N")) {
                    nameSupplied=true;
                    jobName = this.instance.getOwner() + value;
                    pstream.print(key + " " + jobName);
                }
                else {
                    pstream.print(key + " " + value);
                }
            }
            pstream.println();
            
            // Set job name if not supplied by user as a drmParameter
            if (!nameSupplied) {
                jobName = this.instance.getOwner() + "G-Rex";
                pstream.println("#$ -N " + jobName);
            }
            
            // Run application script. Must contain command to launch application (e.g. mpirun)
            pstream.println(cmdLine);
            // End of script notification
            pstream.println("echo \"End of\" $0");
            pstream.println("date");            
            pstream.close();
        }
        catch(IOException ioe)
        {
            log.error("Error Creating submit file for instance " + this.instance.getId());
            this.instance.setState(Job.State.ERROR);
            this.instance.setExitCode(this.SGE_FAILURE);
            this.saveInstance();
        }
            
        /*
         * Submit SGE job
         */    
        log.debug("Submitting job with command \"" + qsubCommand + " " + submitFile.getPath() +"\"...");
        
        // Check for errors and find job ID
        String line;
        InputStream is;
        BufferedReader buf;
        try {
            // Issue qsub command
            Process proc = Runtime.getRuntime().exec(qsubCommand + " " + submitFile.getPath(), null, wdFile);
            
            // Check for errors in qsub error stream.  A non-empty error stream indicates that
            // an error has occurred.
            line = null;
            is=proc.getErrorStream();
            buf = new BufferedReader(new InputStreamReader(is));
            do {
                line = buf.readLine();
                if (line != null) {
                    //Matcher m1 = QSUB_FAILURE_PATTERN_1.matcher(line);
                    //if (line.toLowerCase().contains("error") || line.contains("qsub: Unknown option"))
                    //if (m1.matches()) {
                        log.error("Error running qsub:" + line);
                        if (instance.getState() != Job.State.ERROR) {
                            this.instance.setState(Job.State.ERROR);
                            this.instance.setExitCode(SGE_FAILURE);
                            this.saveInstance();
                            log.debug("Instance state is now " + this.instance.getState());
                        }
                    //}
                }
            } while (line != null);
            buf.close();
                        
            // Get SGE job ID from the qsub output stream
            if (!this.instance.getState().meansFinished()) {
                boolean foundMatch=false;
                line = null;
                is=proc.getInputStream();
                buf = new BufferedReader(new InputStreamReader(is));
                do {
                    line = buf.readLine();
                    if (line != null) {
                        log.debug("qsub output: " + line);
                        // Check to see if the job has been submitted
                        Matcher m = QSUB_SUCCESS_PATTERN.matcher(line);
                        if (m.matches()) {
                            foundMatch=true;
                            sgeJobID = Integer.parseInt(m.group(1));
                            log.debug("Detected SGE job identifier: " + sgeJobID);
                            this.instance.setState(Job.State.PENDING);
                            this.saveInstance();
                            log.debug("Instance state is now " + this.instance.getState());                        
                        }
                    }
                } while (line != null);                
                buf.close();
                    
                if (!foundMatch) {
                    // This should never happen. We should only be checking the output stream if
                    // nothing was found in the error stream.  Something is probably wrong with the
                    // pattern matching
                    log.error("Error running qsub. No matches found in output stream");
                    this.instance.setState(Job.State.ERROR);
                    this.instance.setExitCode(SGE_FAILURE);
                    this.saveInstance();
                    log.debug("Instance state is now " + this.instance.getState());                        
                }
            }
        }
        catch(IOException ioe)
        {
            if (log.isDebugEnabled())
            {
                ioe.printStackTrace();
            }
            log.error("Error running qsub: " + ioe.getMessage());
            this.instance.setState(Job.State.ERROR);
            this.instance.setExitCode(SGE_FAILURE);
            this.saveInstance();
            log.debug("Instance state is now " + this.instance.getState().name());
        }            
           
        // Start a thread that waits for the job to finish and monitors status via qstat.
        // Sets instance state and saves instance every time status changes.
        // This thread uses qstatStreamReader threads to check the output of qstat.
        if (!this.instance.getState().meansFinished()) new MonitorJob(10000).start();
                    
        // Start thread to find out which output files can be deleted. Set the
        // checking interval in milliseconds.
        if (!this.instance.getState().meansFinished()) new CheckOutputFiles(30000).start();
             
    }


    /**
     * Thread that monitors SGE job using qstat. Sets the instance state and the
     * SGE exit code.  Saves instance to persistant store every time state changes
     */
    private class MonitorJob extends Thread
    {
        private long checkIntervalMs;
        private uk.ac.rdg.resc.grex.db.Job.State oldState, newState;
        
        public MonitorJob(long checkIntervalMs)
        {
            // TODO: should be the instance and job ID
            super("monitor-" + instance.getId());
            this.checkIntervalMs = checkIntervalMs;
        }
        public void run()
        {
            log.debug("Waiting for SGE job to finish");
            String line;
            InputStream is;
            BufferedReader buf;
            Process proc;
            Integer SGEjobID = new Integer(sgeJobID) ;
            boolean foundMatch=false;
            boolean foundJob=false;
            boolean qstatError=false;
            
            while (!instance.getState().meansFinished()) 
            {                
                try {
                    // Make sure our view of the instance is up to date
                    instance = instancesStore.getServiceInstanceById(instance.getId());                    
                    oldState = instance.getState();
                    newState = oldState;
                    
                    // Wait for the required time then run qstat to get SGE job information
                    Thread.sleep(checkIntervalMs);
                }
                catch (InstancesStoreException ise) {
                    log.error("Error retrieving instance from persistant store: " + ise.toString());
                }
                catch (InterruptedException ie) {
                    log.error("Thread sleep interrupted: " + ie.toString());
                }
                
                // Run qstat command
                try {
                    //log.debug("Running qstat...");
                    proc = Runtime.getRuntime().exec(qstatCommand);
                        
                    // Check for errors
                    line = null;
                    qstatError=false;
                    is=proc.getErrorStream();
                    buf = new BufferedReader(new InputStreamReader(is));
                    do {
                        line = buf.readLine();
                        if (line != null) {
                            log.debug("Error running qtat: " + line);
                            qstatError=true;
                        }
                    } while (line != null);
                    buf.close();
                                
                    // See if job is listed in qstat output. If it is get the job state
                    if (!newState.meansFinished() && !qstatError) {
                        line = null;
                        is=proc.getInputStream();
                        buf = new BufferedReader(new InputStreamReader(is));
                        foundJob=false;
                        do {
                            line = buf.readLine();
                            //log.debug("Looking for " + SGEjobID.toString() + " in qstat -s z output: \"" + line + "\"");
                            if (line == null) continue;
                            // Check qstat output
                            Matcher m = QSTAT_PATTERN.matcher(line);
                            if (m.matches()) {
                                if (m.group(1).contains(SGEjobID.toString())) {
                                    //log.debug("Status of job number " + sgeJobID + " is " + m.group(2).toString());
                                    foundJob=true;
                                    if (m.group(2).contains("t") || m.group(2).contains("w")) {
                                        if (instance.getState() != Job.State.PENDING) {
                                            log.info("Job number " + sgeJobID + " is waiting to run");
                                            newState = Job.State.PENDING;
                                            log.debug("Instance state is now " + newState.name());
                                        }
                                    }
                                    else if (m.group(2).contains("r")) {
                                        if (instance.getState() != Job.State.RUNNING) {
                                            log.info("Job number " + sgeJobID + " is running");
                                            newState = Job.State.RUNNING;
                                            log.debug("Instance state is now " + newState.name());
                                        }
                                    }                                    
                                    else if (m.group(2).contains("d")) {
                                        if (instance.getState() != Job.State.ABORTED) {
                                            log.info("Job number " + sgeJobID + " was deleted");
                                            newState = Job.State.ABORTED;
                                            log.debug("Instance state is now " + newState.name());
                                        }
                                    }                                    
                                    else if (m.group(2).contains("E")) {
                                        if (instance.getState() != Job.State.ERROR) {
                                            log.info("Job number " + sgeJobID + " is in the error state");
                                            newState = Job.State.ERROR;
                                            exitCode=SGE_FAILURE;
                                            log.debug("Instance state is now " + newState.name());
                                        }
                                    }
                                }
                            }
                        } while (line != null);
                        buf.close();
                        
                        if (!foundJob && !qstatError) {
                            /* Job is not in the SGE system any more. This is not a very
                             * robust way to find out if a job has finished, but unfortunately
                             * we will have to rely on this for the time being.  See comments
                             * about running "qstat -s z" via ssh below.
                             */
                            if (instance.getState() != Job.State.FINISHED) {
                                log.info("Job number " + sgeJobID + " has finished.");
                                newState = Job.State.FINISHED;
                                log.debug("Instance state is now " + newState.name());
                            }                            
                        }
                    } // end of "if (instance.getState().meansFinished()) ..."                    
                    
                    // Run qstat -s z to find out if job has finished
                    /*
                     * The command "qstat -s z" does not work when executed via ssh for
                     * some reason.  Checking output of "qstat -s z" is a more robust way to
                     * find out if a job has finished than looking for the job not being
                     * listed by qstat.  This is because the output from the "q" commands
                     * is not always up to date.  That means, for example, that a job could
                     * be submitted successfully but not show up in the qstat listing for a
                     * few seconds.
                     */
                    /*
                    log.debug("Running qstat -s z ...");
                    //proc = Runtime.getRuntime().exec(qstatCommand + " -s z");
                    log.debug("Exit value from qstat = " + proc.exitValue());
                        
                    // Check for errors
                    line = null;
                    is=proc.getErrorStream();
                    buf = new BufferedReader(new InputStreamReader(is));
                    do {
                        line = buf.readLine();
                        if (line != null) {
                            if (instance.getState() != Job.State.ERROR) {
                                log.error("Error running qtat -s z: " + line);
                                newState = Job.State.ERROR;
                                exitCode = SGE_FAILURE;
                                log.debug("Instance state is now " + newState.name());
                            }
                        }
                    } while (line != null);
                    buf.close();
                    
                    // Now see if job is listed in qstat -s z output. If it is the job has ended
                    if (!newState.meansFinished()) {
                        line = null;
                        //log.debug("Looking for " + SGEjobID.toString() + " in qstat -s z output: \"" + line + "\"");
                        is=proc.getInputStream();
                        buf = new BufferedReader(new InputStreamReader(is));
                        foundMatch=false;
                        do {
                            line = buf.readLine();
                            if (line == null) continue;
                            // Check qstat output
                            Matcher m = QSTAT_PATTERN2.matcher(line);
                            if (m.matches()) {
                                foundMatch=true;
                                if (m.group(1).contains(SGEjobID.toString())) {
                                    if (instance.getState() != Job.State.FINISHED) {
                                        log.info("Job number " + sgeJobID + " has finished.");
                                        newState = Job.State.FINISHED;
                                        log.debug("Instance state is now " + newState.name());
                                    }
                                }
                            }
                        } while (line != null);
                        buf.close();
                        if (!foundMatch) {
                            // This should never happen. There is always something output by
                            // qstat -s z even if there are no jobs running at all
                            log.error("Error running qstat -s z. No matches found in output stream");
                            instance.setState(Job.State.ERROR);
                            instance.setExitCode(SGE_FAILURE);
                            saveInstance();
                            log.debug("Instance state is now " + instance.getState());                        
                        }
                        
                    } // end of "if (instance.getState().meansFinished()) ..."
                     */
                }
                catch(IOException ioe)
                {
                    if (log.isDebugEnabled())
                    {
                        ioe.printStackTrace();
                    }
                    log.error("Error running qstat: " + ioe.getMessage());
                    instance.setState(Job.State.ERROR);
                    instance.setExitCode(SGE_FAILURE);
                    saveInstance();
                    log.debug("Instance state is now " + instance.getState().name());
                }                        
                                    
                // Save instance if state has changed
                if (newState.name().compareTo(oldState.name()) != 0) {
                    log.debug("Status has changed from " + oldState.name() + " to " + newState.name() + ". Saving to persistant store...");
                    instance.setExitCode(exitCode);
                    instance.setState(newState);
                    saveInstance();
                }
                
            } // End of while loop for periodic state check
        } // End of run method
    } // End of MonitorJob class

} // End of SGEJobRunner class