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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rdg.resc.grex.config.Parameter;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;

/**
 * Runs a job on the G-Rex server itself.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class LocalJobRunner extends AbstractJobRunner
{
    private static final Log log = LogFactory.getLog(LocalJobRunner.class);
    
    private Process proc; // The Process that we are running in this job
    private boolean aborted = false;
 
    /**
     * The task of the start() method is to prepare the job, then kick it off, 
     * setting the state of the job to RUNNING or ERROR before returning
     */
    public void start() throws IOException
    {
        log.debug("Starting execution of service instance " + this.instance.getId());
        
        // Prepare the command line that will be executed
        String cmdLine = this.constructCommmandLine();
        log.debug("Command line that will be executed: \"" + cmdLine + "\"");
        
        // TODO: deal with standard input
        File wdFile = new File(this.instance.getWorkingDirectory());
        
        // Start the process running, setting the working directory
        this.proc = Runtime.getRuntime().exec(cmdLine, null, wdFile);
        
        // Start threads to consume the output streams
        File stdoutFile = new File(wdFile, "stdout");
        File stderrFile = new File(wdFile, "stderr");
        new RedirectStream(this.proc.getInputStream(), new FileOutputStream(stdoutFile)).start();
        new RedirectStream(this.proc.getErrorStream(), new FileOutputStream(stderrFile)).start();
        
        // Start a thread that waits for the process to finish and grabs the exit code
        new WaitProcess().start();
        
        log.debug("Process started");
        
        // Update the state of the instance and persist to the database
        this.instance.setState(GRexServiceInstance.State.RUNNING);
        this.saveInstance();
    }
    
    /**
     * Constructs the full command line that will be executed
     */
    private String constructCommmandLine()
    {
        StringBuffer cmdLine = new StringBuffer(this.gsConfig.getCommand());
        
        // Look through all the command-line parameters, inserting the values
        for (Parameter param : this.gsConfig.getParams())
        {
            String paramValue = this.instance.getParamValue(param.getName()).trim();
            if (paramValue == null)
            {
                if (param.isRequired())
                {
                    // TODO: throw an InvalidInstanceStateException or similar
                }
            }
            else
            {
                cmdLine.append(" ");
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
     * Simple class to read an input stream and write it directly to a file
     */
    private class RedirectStream extends Thread
    {
        private InputStream in;
        private OutputStream out;
        
        public RedirectStream(InputStream in, OutputStream out)
        {
            this.in = in;
            this.out = out;
        }
        
        public void run()
        {
            try
            {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
            }
            catch(IOException ioe)
            {
                // Unlikely to happen
                log.error("Error redirecting stream in LocalJobRunner.RedirectStream");
                instance.setState(GRexServiceInstance.State.ERROR);
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
        }
    }
    
    /**
     * Thread that waits for the process to finish, setting the state and the
     * exit code
     */
    private class WaitProcess extends Thread
    {
        public void run()
        {
            boolean done = false;
            while (!done)
            {
                try
                {
                    int exitCode = proc.waitFor();
                    done = true;
                    if (!aborted)
                    {
                        instance.setState(GRexServiceInstance.State.FINISHED);
                    }
                    instance.setExitCode(exitCode);
                    saveInstance();
                }
                catch(InterruptedException ie)
                {
                    log.warn("waitFor() interrupted");
                }
            }
        }
    }
    
    public void abort()
    {
        log.debug("Destroying process for service instance " + this.instance.getId());
        this.aborted = true;
        this.proc.destroy();
        this.instance.setState(GRexServiceInstance.State.ABORTED);
        this.saveInstance();
    }
    
}
