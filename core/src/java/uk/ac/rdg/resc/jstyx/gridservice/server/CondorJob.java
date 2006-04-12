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
import java.io.FileOutputStream;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
    private static final String LOG_FILE = "condor.log"; // Name of the condor submit file
    private static final String SUBMIT_FILE = "condor.submit"; // Name of the condor submit file
    
    private static final Pattern RETURN_VALUE_PATTERN =
        Pattern.compile(".*Normal termination \\(return value ([0-9]*)\\)");
    
    private boolean stopThreads;
    
    /**
     * Creates a new instance of CondorJob
     */
    public CondorJob(StyxGridServiceInstance instance)
        throws StyxException
    {
        super(instance);
        this.stdout = new SGSOutputFile(new File(this.workDir, STDOUT_FILE), this);
        this.stderr = new SGSOutputFile(new File(this.workDir, STDERR_FILE), this);
        this.stopThreads = false;
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
        try
        {
            // Create the condor submit file in the working directory
            PrintStream submitFile = new PrintStream(new FileOutputStream(
                new File(this.workDir, SUBMIT_FILE)), true);
            
            submitFile.println("########################");
            submitFile.println("# Submit description file for instance " +
                this.instance.getID());
            submitFile.println("########################");
            
            submitFile.println("executable = " + this.command);
            submitFile.println("universe = vanilla");
            submitFile.println("#input = " + STDIN_FILE);
            submitFile.println("output = " + STDOUT_FILE);
            submitFile.println("error = " + STDERR_FILE);
            submitFile.println("log = " + LOG_FILE);
            submitFile.println("initialdir = " + this.workDir.getPath());
            
            submitFile.println("queue");
            
            submitFile.close();
            
            // Now submit this file to the Condor pool
            Process proc = Runtime.getRuntime().exec("condor_submit " +
                SUBMIT_FILE, null, this.workDir);
            
            // Read the output from the condor_submit program
            new CondorStreamReader(proc.getInputStream()).start();
            new CondorStreamReader(proc.getErrorStream()).start();
            
            // Start a thread that parses the log file: this is how we know
            // the status of a job
            new LogFileParser().start();
        }
        catch(FileNotFoundException fnfe)
        {
            throw new StyxException("Could not create condor submit file");
        }
        catch(IOException ioe)
        {
            throw new StyxException("Error running condor_submit: " + ioe.getMessage());
        }
    }
    
    /**
     * Aborts the job, forcibly terminating it if necessary.  Does nothing if
     * the job is not running.  This is called when the user (i.e. the remote
     * client) opts to stop the job.  Implementations should remember to set
     * the status code to ABORTED if the stop operation is successful
     */
    public void stop()
    {
        this.stopThreads = true;
        this.setStatus(StatusCode.ABORTED);
        // TODO: call condor_rm: requires us to have captured the cluster number
        // of the job
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
     * @throws FileNotFoundException if the OutputStream could not be created
     */
    public OutputStream getStdinStream() throws FileNotFoundException
    {
        return new FileOutputStream(new File(this.workDir, STDIN_FILE));
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
            String line;
            
            // We have to keep opening the log file
            while(!stopThreads && !finished)
            {
                try
                {
                    if (logFile.exists())
                    {
                        // Read a line at a time from the file, looking for key phrases
                        log.debug("Opening log file " + logFile);
                        BufferedReader buf = new BufferedReader(new FileReader(logFile));
                        
                        // Read all the lines we have read already
                        log.debug("Reading " + linesRead + " lines that we have already read");
                        for (int i = 0; i < linesRead; i++)
                        {
                            line = buf.readLine();
                        }
                        
                        // Now read the first new line
                        do
                        {
                            line = buf.readLine();
                            log.debug("Read line from log file: " + line);
                            if (line != null)
                            {
                                linesRead++;
                                if (line.indexOf("Job submitted") >= 0)
                                {
                                    log.debug("Detected that job has been submitted");
                                    setStatus(StatusCode.SUBMITTED);
                                }
                                else if (line.indexOf("Job executing") >= 0)
                                {
                                    log.debug("Detected that job is executing");
                                    setStatus(StatusCode.RUNNING);
                                }
                                else
                                {
                                    // Look to see if we have normal termination
                                    // and capture the exit code
                                    Matcher m = RETURN_VALUE_PATTERN.matcher(line);
                                    if (m.matches())
                                    {
                                        log.debug("Detected that job has finished");
                                        int exitCode = Integer.parseInt(m.group(1));
                                        fireGotExitCode(exitCode);
                                        // Wait for a few seconds for files to be
                                        // transferred to the submit host
                                        // This is a rather ugly workaround!
                                        Thread.sleep(5000);
                                        setStatus(StatusCode.FINISHED);
                                        finished = true;
                                    }
                                }
                            }
                        } while (line != null && !stopThreads);
                        buf.close();
                    }
                    // Wait for half a second before we open the file again or
                    // check to see if it exists
                    Thread.sleep(500);
                }
                catch(InterruptedException ie)
                {
                    // do nothing
                }
                catch(IOException ioe)
                {
                    if (log.isDebugEnabled())
                    {
                        ioe.printStackTrace();
                    }
                    error("Error reading the Condor log file: " + ioe.getMessage());
                }
            }
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
                    if (line != null && line.startsWith("ERROR"))
                    {
                        error("Error running condor_submit: " + line);
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
    
    public static void main (String[] args)
    {
        Pattern pattern = Pattern.compile(".*Normal termination \\(return value ([0-9]*)\\)");
        
        String test = "\t(1) Normal termination (return value 29)";
        
        Matcher matcher = pattern.matcher(test);
        if (matcher.matches())
        {
            System.out.println("Pattern matches");
            System.out.println("Return value: " + matcher.group(1));
        }
        else
        {
            System.out.println("No match");
        }
    }
}
