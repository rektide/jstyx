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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Hashtable;
import java.util.Enumeration;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.tar.TarEntry;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.server.StyxFile;

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
    private static final String LOG_FILE = "condor.log"; // Name of the condor submit file
    private static final String SUBMIT_FILE = "condor.submit"; // Name of the condor submit file
    
    private static final Pattern JOB_SUBMITTED_PATTERN =
        Pattern.compile("([0-9]*) job\\(s\\) submitted to cluster ([0-9]*).*");
    private static final Pattern JOB_TERMINATED_PATTERN =
        Pattern.compile("005 \\(([0-9]*)\\.([0-9]*)\\.([0-9]*).*Job terminated\\.");
    private static final Pattern RETURN_VALUE_PATTERN =
        Pattern.compile(".*Normal termination \\(return value ([0-9]*)\\)");
    private static final Pattern ABNORMAL_TERMINATION_PATTERN =
        Pattern.compile(".*Abnormal termination \\(signal ([0-9]*)\\)");
    
    private File stdin;
    private OutputStream stdinStream;
    private String args; // The argument list as a String
    private String clusterID;   // the id of the whole job (Condor's cluster number)
    private boolean startMessageReceived; // True when we have received a message to start the job
    private boolean stdinReady; // True when the standard input data are ready
    private boolean jobStarted;
    private boolean stopThreads;
    
    // This maps the names of output files to tar output streams in the root
    // directory of the job
    private Hashtable/*<String, TarOutputStream>*/ tarOutputStreams;
    
    /**
     * Creates a new instance of CondorJob
     */
    public CondorJob(StyxGridServiceInstance instance)
        throws StyxException
    {
        super(instance);
        this.stdin = new File(this.workDir, STDIN_FILE);
        this.stdinStream = null;
        this.stdout = new SGSOutputFile(new File(this.workDir, STDOUT_FILE), this);
        this.stderr = new SGSOutputFile(new File(this.workDir, STDERR_FILE), this);
        this.clusterID = "";
        this.stopThreads = false;
        this.startMessageReceived = false;
        this.stdinReady = false;
        this.jobStarted = false;
        this.args = "";
        this.tarOutputStreams = new Hashtable();
    }
    
    /**
     * Sets the parameters of the Job.  These parameters are contained in the
     * given SGSParamFiles.
     * @param paramFiles Array of SGSParamFiles containing the parameters that
     * have been set
     */
    public void setParameters(SGSParamFile[] paramFiles)
    {
        // We don't actually use the paramFiles argument since the SGSInstance
        // contains a method for getting all the args as a string
        this.args = this.instance.getArguments().trim();
    }
    
    /**
     * Sets the source of the data that is to be sent to the standard input
     * of the job.  This can be called before <b>or</b> after start().
     * This blocks until the data have been downloaded.
     * @param url The URL from which the data will be read
     * @throws StyxException if data could not be read from the given URL, or 
     * if the data were downloaded and the job could not be started.
     */
    public void setStdinURL(URL url) throws StyxException
    {
        this.instance.downloadFrom(url, STDIN_FILE);
        this.stdinDataDownloaded();
    }
    
    /**
     * Starts the job running.  In the case of a CondorJob we must wait until
     * we have the standard input data so we don't actually start the job in this
     * method.
     * @throws StyxException if the job could not be started
     */
    public void start() throws StyxException
    {
        log.debug("Got start message");
        this.startMessageReceived = true;
        this.startJob();
    }
    
    /**
     * Aborts the job, forcibly terminating it if necessary.  Does nothing if
     * the job is not running.  This is called when the user (i.e. the remote
     * client) opts to stop the job.  Sets the status code to ABORTED if the
     * stop operation is successful.  Aborts the job by calling condor_rm.
     */
    public void stop() throws StyxException
    {
        this.stopThreads = true;
        if (statusCode == StatusCode.RUNNING)
        {
            try
            {
                Process proc = Runtime.getRuntime().exec("condor_rm " + this.clusterID);
                log.debug("Called condor_rm " + this.clusterID);
                // Make sure we consume the outputs from the condor_rm command
                new CondorStreamReader(proc.getInputStream()).start();
                new CondorStreamReader(proc.getErrorStream()).start();
                // We just assume that the condor_rm command has worked
                this.setStatus(StatusCode.ABORTED);
                // TODO: set the progress 
            }
            catch (IOException ioe)
            {
                throw new StyxException("Error stopping job: could not create" +
                    " process \"condor_rm " + this.clusterID + "\"");
            }
        }
    }
    
    /**
     * Stops the job because of an error.  Implementations should remember to set
     * the status code to ERROR 
     * @param message Description of the error that occurred
     */
    public void error(String message)
    {
        this.stopThreads = true;
        log.error(message);
        this.setStatus(StatusCode.ERROR, message);
        // TODO: set the progress
    }
    
    /**
     * This is called when it is confirmed that the standard input data have
     * been downloaded.  This is important in a CondorJob because the job cannot
     * start until all the input data are ready.
     */
    public void stdinDataDownloaded() throws StyxException
    {
        log.debug("Standard input data downloaded (or not used)");
        this.stdinReady = true;
        this.startJob();
    }
    
    /**
     * Starts the job but only if we have received a start message and the 
     * stdin data are ready: otherwise this does nothing
     */
    private void startJob() throws StyxException
    {
        if (this.startMessageReceived && this.stdinReady && !this.jobStarted
            && !this.stopThreads)
        {
            this.jobStarted = true;
            
            try
            {
                // Prepare the directories for each individual job and get the number
                // of jobs that will be run
                int n = this.prepareJobDirectories();
                log.debug("Prepared " + n + " job directories");
                this.setNumSubJobs(n);
                
                if (n > 1)
                {
                    // Set up the output files: these are tar files that will be added
                    // to when each individual job finishes
                    StyxFile[] outputFiles = this.instance.getOutputFiles();
                    for (int i = 0; i < outputFiles.length; i++)
                    {
                        File outputFile = new File(this.workDir, outputFiles[i].getName());
                        this.tarOutputStreams.put(outputFiles[i].getName(),
                            new TarOutputStream(new FileOutputStream(outputFile)));
                    }
                }
                
                // Create the condor submit file in the working directory
                PrintStream submitFile = new PrintStream(new FileOutputStream(
                    new File(this.workDir, SUBMIT_FILE)), true);
                
                // If this is a composite job (numSubJobs > 1), common files
                // are kept in the root directory of the job, i.e. the parent
                // directory of the subjob
                String commonFilePrefix = (n > 1) ? ".."
                    + StyxUtils.SYSTEM_FILE_SEPARATOR : "";
                    
                submitFile.println("########################");
                submitFile.println("# Submit description file for instance " +
                    this.instance.getID());
                submitFile.println("########################");

                submitFile.println("executable = " + this.command);
                submitFile.println("universe = vanilla");
                if (this.stdin.exists())
                {
                    submitFile.println("input = " + commonFilePrefix + STDIN_FILE);
                }
                submitFile.println("output = " + STDOUT_FILE);
                submitFile.println("error = " + STDERR_FILE);
                submitFile.println("arguments = " + this.args);                
                // We use the same log file for each job
                submitFile.println("log = " + commonFilePrefix + LOG_FILE);
                
                // Force Condor to transfer the files using its own mechanism.
                // Not only should this work on more systems (doesn't rely on a
                // shared filesystem) it makes sure that the job does not finish
                // until the output files have been transferred back to the submit
                // host
                submitFile.println("should_transfer_files = YES");
                submitFile.println("when_to_transfer_output = ON_EXIT");
                
                // Now make sure that Condor transfers the input file(s) from the
                // correct directory
                submitFile.print("transfer_input_files = ");
                StyxFile[] inputFiles = this.instance.getInputFiles();
                boolean firstTime = true;
                for (int i = 0; i < inputFiles.length; i++)
                {
                    // Don't do anything with the standard input
                    if (inputFiles[i] instanceof SGSInputFile.File ||
                        inputFiles[i] instanceof SGSInputDirectory)
                    {
                        if (!firstTime)
                        {
                            submitFile.print(", ");
                        }
                        if (inputFiles[i] instanceof SGSInputFile.File && n > 1)
                        {
                            // There is only one version of this input file and
                            // it is found in the root directory of this instance
                            submitFile.print(".." + StyxUtils.SYSTEM_FILE_SEPARATOR);
                        }
                        submitFile.print(inputFiles[i].getName());
                        firstTime = false;
                    }
                }
                submitFile.println("");
                
                if (n > 1)
                {
                    submitFile.println("initialdir = " + this.workDir.getPath()
                        + StyxUtils.SYSTEM_FILE_SEPARATOR + "$(Process)");
                    submitFile.println("queue " + this.numSubJobs);
                }
                else
                {
                    submitFile.println("initialdir = " + this.workDir.getPath());
                    submitFile.println("queue");
                }

                submitFile.close();

                // Now submit this file to the Condor pool
                Process proc = Runtime.getRuntime().exec("condor_submit " +
                    SUBMIT_FILE, null, this.workDir);

                // Read the output from the condor_submit program
                new CondorStreamReader(proc.getInputStream()).start();
                new CondorStreamReader(proc.getErrorStream()).start();
            }
            catch(FileNotFoundException fnfe)
            {
                if (log.isDebugEnabled())
                {
                    fnfe.printStackTrace();
                }
                throw new StyxException("Could not create condor submit file");
            }
            catch(IOException ioe)
            {
                if (log.isDebugEnabled())
                {
                    ioe.printStackTrace();
                }
                throw new StyxException("Error running condor_submit: " + ioe.getMessage());
            }
        }
    }
    
    /**
     * Prepares the directories for each individual job.  Each directory contains
     * a unique combination of input files and will contain the output files for
     * an individual Condor job
     * @return The number of input directories prepared (i.e. the number of jobs
     * that will be run)
     * @throws IOException if there was an error copying input files into a
     * job directory
     */
    private int prepareJobDirectories() throws IOException
    {
        boolean done = false;
        int jobID = 0;
        StyxFile[] inputFiles = this.instance.getInputFiles();

        // This array contains the indices of the files in the directories
        // of input files
        int[] indices = new int[inputFiles.length];
        // This array contains the number of possibilities for each input file
        int[] numFiles = new int[inputFiles.length];
        
        while (!done)
        {
            // Make a new working directory for this job, named after the 
            // job ID
            File jobWorkDir = new File(this.workDir, "" + jobID);
            jobWorkDir.mkdir();

            // Copy the required files to this directory.  We don't need to
            // copy the files that are the same for every job
            for (int i = 0; i < indices.length; i++)
            {
                if (inputFiles[i] instanceof SGSInputDirectory)
                {
                    // There are (potentially) many versions of this file
                    SGSInputDirectory dir = (SGSInputDirectory)inputFiles[i];
                    numFiles[i] = dir.getNumChildren();
                    // Copy the relevant file to this working directory, giving
                    // it the name of the directory in which it's contained
                    // This must be an SGSInputFile.File (no mechanism yet to
                    // upload multiple files to stdin)
                    File src = ((SGSInputFile.File)dir.getChildren()[indices[i]]).getFile();
                    File dest = new File(jobWorkDir, dir.getName());
                    copyFile(src, dest);
                }
                else
                {
                    numFiles[i] = 1;
                }
            }

            // Now move to the next combination of input files
            for (int i = 0; i < indices.length; i++)
            {
                indices[i]++;
                if (indices[i] == numFiles[i])
                {
                    // This index has "clicked over" to zero, so we go round
                    // the loop again and increment the next one, unless this is
                    // the last index in the list.
                    indices[i] = 0;
                    if (i == indices.length - 1)
                    {
                        done = true;
                    }
                }
                else
                {
                    // We've incremented this index so we don't need to do any more
                    break;
                }
            }
            jobID++;
        }
        return jobID;
    }
    
    /**
     * Gets an OutputStream representing the standard input of the Job.  In this
     * implementation, this is a FileOutputStream: data must be written to this file
     * before the job can start.
     * @return the OutputStream, or null if the underlying system is not ready yet
     * @throws FileNotFoundException if the OutputStream could not be created
     */
    public OutputStream getStdinStream() throws FileNotFoundException
    {
        if (this.stdinStream == null)
        {
            this.stdinStream = new FileOutputStream(this.stdin);
        }
        return this.stdinStream;
    }
    
    /**
     * Thread that reads Condor's log file as it is produced and changes the
     * status of the job based on what it finds
     */
    private class LogFileParser extends Thread
    {
        public void run()
        {
            boolean finished = false;
            File logFile = new File(workDir, LOG_FILE);
            int linesRead = 0;
            int jobsComplete = 0;
            String line;
            
            // We have to keep opening the log file
            while(!stopThreads && !finished)
            {
                try
                {
                    if (logFile.exists())
                    {
                        // Read a line at a time from the file, looking for key phrases
                        //log.debug("Opening log file " + logFile);
                        BufferedReader buf = new BufferedReader(new FileReader(logFile));
                        
                        // Read all the lines we have read already
                        // TODO can we use seek() or something like it here?
                        //log.debug("Reading " + linesRead + " lines that we have already read");
                        for (int i = 0; i < linesRead; i++)
                        {
                            line = buf.readLine();
                        }
                        
                        // Now read the first new line
                        do
                        {
                            line = buf.readLine();
                            //log.debug("Read line from log file: " + line);
                            if (line != null)
                            {
                                linesRead++;
                                if (line.startsWith("001"))
                                {
                                    log.debug("Detected that job is executing");
                                    //setStatus(StatusCode.RUNNING);
                                    // TODO: get the proper ID of the job
                                    subJobStarted(0);
                                }
                                else
                                {
                                    Matcher m = JOB_TERMINATED_PATTERN.matcher(line);
                                    if (m.matches())
                                    {
                                        // One of the sub-jobs has finished
                                        int jobID = Integer.parseInt(m.group(2));
                                        // TODO: distinguish between failed and
                                        // successful jobs
                                        subJobCompleted(jobID);
                                        copyOutputToRootDirectory(jobID);
                                        
                                        // TODO get error code etc
                                        jobsComplete++;
                                        if (jobsComplete == numSubJobs)
                                        {
                                            closeAllOutputFiles();
                                            fireGotExitCode(0);
                                            setStatus(StatusCode.FINISHED);
                                            finished = true;
                                        }
                                    }
                                    // Look to see if we have normal termination
                                    // and capture the exit code
                                    /*m = RETURN_VALUE_PATTERN.matcher(line);
                                    if (m.matches())
                                    {
                                        log.debug("Detected that job has finished");
                                        int exitCode = Integer.parseInt(m.group(1));
                                        fireGotExitCode(exitCode);
                                        setStatus(StatusCode.FINISHED);
                                        finished = true;
                                    }
                                    // Look for abnormal termination
                                    m = ABNORMAL_TERMINATION_PATTERN.matcher(line);
                                    if (m.matches())
                                    {
                                        log.debug("Detected abnormal termination");
                                        setStatus(StatusCode.ERROR, "signal = "
                                            + m.group(1));
                                        finished = true;
                                    }*/
                                }
                            }
                        } while (line != null && !stopThreads);
                        buf.close();
                    }
                    if (!finished)
                    {
                        // Wait for a second before we open the file again or
                        // check to see if it exists
                        Thread.sleep(1000);
                    }
                }
                catch(IOException ioe)
                {
                    if (log.isDebugEnabled())
                    {
                        ioe.printStackTrace();
                    }
                    // We don't treat this as a fatal error because this happens
                    // under Windows Condor when we try to read from the log file
                    // but Condor is trying to write to it at the same time
                    log.error("Error reading the Condor log file: " + ioe.getMessage());
                    try
                    {
                        // Give Condor a chance to finish writing to the file
                        Thread.sleep(1000);
                    }
                    catch(InterruptedException ie)
                    {
                        // do nothing
                    }
                }
                catch(InterruptedException ie)
                {
                    // do nothing
                }
            }
        }
    }
    
    /**
     * Copies all the output files from the given subjob (which has completed) into
     * the tar files in the root directory for this whole job.  If this is not a
     * composite job (i.e. if there is only a single job) this method does nothing
     * @throws IOException if the data could not be copied to the tar files
     */
    private void copyOutputToRootDirectory(int jobID) throws IOException
    {
        if (this.numSubJobs > 1)
        {
            File jobWorkDir = new File(this.workDir, "" + jobID);
            for (Enumeration names = this.tarOutputStreams.keys(); names.hasMoreElements(); )
            {
                String name = (String)names.nextElement();
                File file = new File(jobWorkDir, name);
                TarEntry tarEntry = new TarEntry(name + "." + jobID);
                tarEntry.setSize(file.length());
                TarOutputStream tos = (TarOutputStream)this.tarOutputStreams.get(name);
                tos.putNextEntry(tarEntry);
                FileInputStream fin = new FileInputStream(file);
                // Copy the file to the tar stream
                int n;
                byte[] b = new byte[8192];
                do
                {
                    n = fin.read(b);
                    if (n >= 0)
                    {
                        tos.write(b, 0, n);
                    }
                } while (n >= 0);
                fin.close();
                tos.closeEntry();
            }
            log.debug("Copied output from job " + jobID + " to tar output files");
        }
        else
        {
            // We don't need to copy any files
        }
    }
    
    /**
     * Closes all the tar output files
     * @throws IOException if the data could not be copied to the tar files
     */
    private void closeAllOutputFiles() throws IOException
    {
        if (this.numSubJobs > 1)
        {
            for (Enumeration names = this.tarOutputStreams.keys(); names.hasMoreElements(); )
            {
                TarOutputStream tos =
                    (TarOutputStream)this.tarOutputStreams.get(names.nextElement());
                tos.close();
            }
            log.debug("Closed all tar output files");
        }
    }
    
    /**
     * Thread that reads one of the standard streams from the condor_submit
     * program: this consumes the streams (making sure that the condor_submit
     * program does not hang) and checks for errors
     */
    private class CondorStreamReader extends Thread
    {
        private InputStream is;
        
        public CondorStreamReader(InputStream is)
        {
            this.is = is;
        }
        
        public void run()
        {
            try
            {
                BufferedReader buf = new BufferedReader(new InputStreamReader(is));
                String line = null;
                do
                {
                    line = buf.readLine();
                    if (line != null)
                    {
                        // Check for an error (in stderr stream)
                        if (line.startsWith("ERROR"))
                        {
                            error("Error running condor_submit: " + line);
                        }
                        else
                        {
                            // Check to see if the job has been submitted
                            // (in stdout stream)
                            Matcher m = JOB_SUBMITTED_PATTERN.matcher(line);
                            if (m.matches())
                            {
                                int n = Integer.parseInt(m.group(1));
                                clusterID = m.group(2);
                                log.debug("Detected " + n + " jobs submitted: ID = "
                                    + clusterID);
                                setStatus(StatusCode.SUBMITTED, "Condor cluster ID = "
                                    + clusterID);
                                // Now we can start parsing the log file to monitor
                                // the status of the job
                                new LogFileParser().start();
                            }
                        }
                    }
                } while (line != null && !stopThreads);
                buf.close();
            }
            catch(IOException ioe)
            {
                if (log.isDebugEnabled())
                {
                    ioe.printStackTrace();
                }
                error("Error running condor_submit: " + ioe.getMessage());
            }
        }
    }
    
    /**
     * Copies a java.io.File from one place to another.
     * See http://www.experts-exchange.com/Programming/Programming_Languages/Java/Q_10245809.html
     */
    private static void copyFile(File src, File dest) throws IOException
    {
        FileChannel in = null;
        FileChannel out = null;
        try
        {
            in = new FileInputStream(src).getChannel();
            out = new FileOutputStream(dest).getChannel();
            in.transferTo( 0, in.size(), out);
            log.debug("Copied " + src.getPath() + " to " + dest.getPath());
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
            if (out != null)
            {
                out.close();
            }
        }
    }
    
    public static void main (String[] args)
    {
        String test = "005 (496.002.000) 04/21 15:08:14 Job terminated.";
        Matcher m = JOB_TERMINATED_PATTERN.matcher(test);
        if (m.matches())
        {
            System.out.println("Job " + Integer.parseInt(m.group(2)) + " finished");
        }
        else
        {
            System.out.println("No match");
        }
    }
}
