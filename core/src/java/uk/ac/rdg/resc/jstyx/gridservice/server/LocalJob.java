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

import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Job that runs on the server machine through Runtime.exec().
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class LocalJob extends AbstractJob
{
    private static final Logger log = Logger.getLogger(AbstractJob.class);
    private static final Runtime runtime = Runtime.getRuntime();
    
    private Process process;              // The process that will be executed
    private String argList;               // The list of command-line arguments as a String
    private boolean redirectingToStdin;   // True if we are reading data from a URL
                                          // and sending it to the stdin of the Process
    
    /**
     * Creates a new instance of LocalJob
     */
    public LocalJob(File workDir) throws StyxException
    {
        super(workDir);
        this.process = null;
        this.redirectingToStdin = false;
        this.stdout = new CachingStreamReader("stdout");
        this.stderr = new CachingStreamReader("stderr");
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
     * Sets the source of the data that is to be sent to the standard input
     * of the job.  This can be called before <b>or</b> after start().  If the
     * job is running, this will start a thread that reads from the given URL
     * and sends the data to the standard input of the process.  If the job is
     * not yet running, this will simply store the URL and the thread will be
     * started when the job is started.
     * @param url The URL from which the data will be read
     * @throws StyxException if no data could be read from the given url
     */
    public void setStdinSource(URL url) throws StyxException
    {
        this.stdinURL = url;
        if (this.statusCode == StatusCode.RUNNING)
        {
            this.redirectToStdin();
        }
    }
    
    /**
     * @return the OutputStream to which we can write data that will be sent
     * to the standard input of the job.  If the process has not been started,
     * this will return null.
     */
    public OutputStream getStdinStream()
    {
        return this.process == null ? null : this.process.getOutputStream();
    }
    
    /**
     * Starts the job running
     * @throws StyxException if the job could not be started
     */
    public void start() throws StyxException
    {
        try
        {
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
            ((CachingStreamReader) this.stdout).setCacheFile(new File(workDir, "stdout"));
            ((CachingStreamReader) this.stdout).startReading(this.process.getInputStream());
            ((CachingStreamReader) this.stderr).setCacheFile(new File(workDir, "stderr"));
            ((CachingStreamReader) this.stderr).startReading(this.process.getErrorStream());
            
            // Start sending data to the standard input of this process
            if (this.stdinURL != null)
            {
                this.redirectToStdin();
            }

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
     * Stops the job because of an error.
     * @param message Description of the error that occurred
     */
    public void error(String message)
    {
        this.process.destroy();
        this.setStatus(StatusCode.ERROR, message);
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
    
    /**
     * Starts a thread that redirects the data from the given URL to the 
     * input stream of the process.  If we have called this method already,
     * subsequent invocations will do nothing.
     * @throws StyxException if no data could be read from the given url
     */
    private void redirectToStdin() throws StyxException
    {
        if (!this.redirectingToStdin)
        {
            try
            {
                this.redirectingToStdin = true;
                InputStream is = this.stdinURL.openStream();
                OutputStream os = this.process.getOutputStream();
                new RedirectStream(is, os).start();
                log.debug("*** Reading stdin from " + this.stdinURL + "***");
            }
            catch (IOException ioe)
            {
                this.error("Cannot read from " + this.stdinURL);
                throw new StyxException("Cannot read from " + this.stdinURL);
            }
        }
    }
    
}
