/*
 * Copyright (c) 2005 The University of Reading
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

import java.nio.ByteBuffer;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.server.AsyncStyxFile;
import uk.ac.rdg.resc.jstyx.server.InMemoryFile;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Class representing a StyxGridService instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 19:22:32  jonblower
 * Commit adding of SGS files to CVS
 *
 */
class StyxGridServiceInstance extends StyxDirectory
{
    
    private static final Runtime runtime = Runtime.getRuntime();
    
    private StyxGridService sgs; // The SGS to which this instance belongs
    private int id; // The ID number of this instance
    private File workDir; // The working directory of this instance
    private Process process = null; // The process object returned by runtime.exec()
    private ServiceDataElement status;
    private ServiceDataElement progress;
    private ServiceDataElement bytesConsumed; // The number of bytes consumed by the service
    private long bytesCons;
    private CachingStreamReader stdout = new CachingStreamReader(this, "out");  // The standard output from the program
    private CachingStreamReader stderr = new CachingStreamReader(this, "err");  // The standard error from the program
    private StreamWriter stdin  = new StreamWriter("in");   // The standard input to the program
    private InMemoryFile params; // The command-line parameters to pass to the executable
    private String command; // The command to run (i.e. the string that is passed to System.exec)
    private InMemoryFile inputURL; // Non-empty if we're going to read the input from a URL
    private long startTime;
    private StatusCode statusCode; // The status of the service
    
    /**
     * Creates a new StyxGridService with the given id number
     * @todo: sort out permissions and owners on all these files
     */
    public StyxGridServiceInstance(StyxGridService sgs, int id, String command, 
        String sgsWorkDir) throws StyxException
    {
        super("" + id);
        this.sgs = sgs;
        this.id = id;
        this.command = command;
        this.workDir = new File(sgsWorkDir + StyxUtils.SYSTEM_FILE_SEPARATOR + id);
        
        if (this.workDir.exists())
        {
            // Check that this is a directory
            if (!this.workDir.isDirectory())
            {
                throw new StyxException("Can't create working directory: " +
                    "a file of the same name already exists");
            }
            // TODO: Should we empty the working directory?
        }
        else
        {
            // We have to make this directory
            if (!this.workDir.mkdirs())
            {
                throw new StyxException("Unable to create working directory "
                    + this.workDir);
            }
        }
        
        // Add the ctl and params file
        this.addChild(new ControlFile(this)); // the ctl file
        this.params = new InMemoryFile("params");
        this.addChild(params);
        
        // Add the service data: the files exposing the service data will all
        // have asynchronous behaviour
        this.status = new StringServiceDataElement("status", true, "created");
        this.progress = new StringServiceDataElement("progress", false, "0");
        this.bytesCons = 0;
        // Replies to clients must be at least 2s apart
        this.bytesConsumed = new StringServiceDataElement("bytesConsumed", true,
            "" + this.bytesCons, 2.0f);
        StyxDirectory serviceDataDir = new StyxDirectory("serviceData");
        this.addChild(serviceDataDir);
        serviceDataDir.addChild(this.status.getAsyncStyxFile());
        serviceDataDir.addChild(this.progress.getAsyncStyxFile());
        serviceDataDir.addChild(this.bytesConsumed.getAsyncStyxFile());
        
        // Add the streams
        StyxDirectory ioDir = new StyxDirectory("io");
        this.addChild(ioDir);
        // TODO: only add these streams when the process is started?
        ioDir.addChild(stdout); // output stream
        ioDir.addChild(stderr); // error stream
        ioDir.addChild(stdin);  // input stream
        inputURL = new InMemoryFile("inurl");
        ioDir.addChild(inputURL);
        
        this.statusCode = StatusCode.CREATED;
    }
    
    /**
     * Returns the working directory of this instance
     */
    public File getWorkingDirectory()
    {
        return this.workDir;
    }
    
    // The ctl file
    private class ControlFile extends StyxFile
    {
        
        private StyxDirectory instanceRoot; // The root directory of the SGS instance
        
        /** Creates a new instance of ControlFile */
        public ControlFile(StyxDirectory instanceRoot) throws StyxException
        {
            super("ctl");
            this.instanceRoot = instanceRoot;
        }
        
        public void read(StyxFileClient client, long offset, long count, int tag)
            throws StyxException
        {
            // TODO: could return documentation about supported commands?
            throw new StyxException("can't read from a ctl file");
        }
        
        public void write(StyxFileClient client, long offset, long count,
            ByteBuffer data, String user, boolean truncate, int tag)
            throws StyxException
        {
            String cmdString = StyxUtils.dataToString(data);
            // Strip the trailing newline if it exists
            if (cmdString.endsWith(StyxUtils.NEWLINE))
            {
                cmdString = cmdString.substring(0, cmdString.length() - 1);
            }            
            if (cmdString.equalsIgnoreCase("start"))
            {
                synchronized(statusCode)
                {
                    if (statusCode == StatusCode.RUNNING)
                    {
                        throw new StyxException("Service is already running");
                    }
                }
                // Start the executable
                try
                {
                    setBytesConsumed(0);
                    startTime = System.currentTimeMillis();
                    // Start the process running in the correct working directory
                    process = runtime.exec(command + " " + params.getData(),
                        null, workDir);
                    setStatus(StatusCode.RUNNING);
                    new Waiter().start(); // Waits for the process to finish, then sets status
                }
                catch(IOException ioe)
                {
                    throw new StyxException("IOException occurred: " + ioe.getMessage());
                }
                try
                {
                    // Start reading from stdout and stderr
                    stdout.startReading(process.getInputStream());
                    stderr.startReading(process.getErrorStream());
                }
                catch(StyxException se)
                {
                    // There was an error reading from the streams. Tidy up.
                    process.destroy();
                    setStatus(StatusCode.ERROR, se.getMessage());
                    throw new StyxException("Internal error: could not start "
                        + "reading from output and error streams");
                }
                String urlStr = inputURL.getData();
                if (urlStr.equals(""))
                {
                    // We haven't set a URL
                    stdin.setOutputStream(process.getOutputStream());
                }
                else
                {
                    try
                    {
                        URL inURL = new URL(urlStr);
                        URLConnection urlConn = inURL.openConnection();
                        InputStream urlis = urlConn.getInputStream();
                        // Start a thread reading from the url and writing to the 
                        // process's standard input.
                        new RedirectStream(urlis, process.getOutputStream()).start();
                    }
                    catch (Exception e)
                    {
                        // TODO: check that the URL is valid at an earlier stage
                        process.destroy();
                        e.printStackTrace();
                        throw new StyxException("Cannot read from " + urlStr);
                    }
                }
                this.replyWrite(client, count, tag);
            }
            else if (cmdString.equalsIgnoreCase("stop"))
            {
                synchronized(statusCode)
                {
                    // This synchronization prevents the Waiter thread from 
                    // setting the status to "finished" before we can set the
                    // status to "aborted" here
                    if (statusCode == StatusCode.RUNNING)
                    {
                        // Only do this if the process is running
                        process.destroy();
                        setStatus(StatusCode.ABORTED);
                    }
                }
                this.replyWrite(client, count, tag);
            }
            else if (cmdString.equalsIgnoreCase("destroy"))
            {
                if (statusCode == StatusCode.RUNNING)
                {
                    throw new StyxException("Cannot destroy a running service: stop it first");
                }
                // Remove all children of the root directory of this instance
                this.instanceRoot.removeAllChildren();
                // Now remove the root directory itself
                this.instanceRoot.remove();
                // Return this instance ID to the pool of valid IDs
                // TODO: is this a good thing? If a service is destroyed, and
                // a new one created with the same ID, could clients get confused?
                //sgs.returnInstanceID(id);
                // TODO: should we remove the working directory too?
                this.replyWrite(client, count, tag);              
            }
            else
            {
                // TODO: implement "destroy" (frees all resources and deletes all caches)            
                throw new StyxException("unknown command: " + cmdString);
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
                int exitCode = process.waitFor();
                long duration = System.currentTimeMillis() - startTime;
                synchronized(statusCode)
                {
                    // We must get the lock on the statusCode because
                    // we could be changing the status in another thread
                    if (statusCode != StatusCode.ABORTED && statusCode != StatusCode.ERROR)
                    {
                        // don't set the status if we have terminated abnormally
                        setStatus(StatusCode.FINISHED, "exit code " + exitCode + 
                            ", time taken = " + (float)duration / 1000 + " seconds.");
                    }
                }
            }
            catch(InterruptedException ie)
            {
                // do nothing
            }
        }
    }
    
    // Reads from an input stream and writes the result to an output stream
    // TODO: can this be done using pipes?
    private class RedirectStream extends Thread
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
                    // Update the number of bytes consumed
                    // TODO: should we do this here or in another thread?
                    // It won't hold us up as long as the network is the limiting
                    // factor.
                    setBytesConsumed(bytesCons + n);
                }
            }
            catch(IOException ioe)
            {
                // This will be thrown if there was an error reading from the stream
                // or writing to the output stream
                synchronized(statusCode)
                {
                    // We must get the lock on the statusCode because
                    // we could be changing the status in another thread
                    if (statusCode != StatusCode.ABORTED)
                    {
                        // don't do this if the process was aborted manually
                        process.destroy();
                        setStatus(StatusCode.ERROR, "when reading input data: " + ioe.getMessage());
                    }
                }
            }
            finally
            {
                // Make sure all clients have the final value of bytesConsumed
                bytesConsumed.flush();
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
    
    /**
     * Sets the status of the service and updates the status service data
     */
    private void setStatus(StatusCode code, String message)
    {
        synchronized(this.statusCode)
        {
            this.statusCode = code;
        }
        String msg = "";
        if (message != null && message != "")
        {
            msg = ": " + message;
        }
        this.status.setValue(code.getText() + msg);
    }
    
    private void setStatus(StatusCode code)
    {
        this.setStatus(code, null);
    }
    
    /**
     * Sets the number of bytes consumed by the service instance
     */
    private synchronized void setBytesConsumed(long newValue)
    {
        this.bytesCons = newValue;
        this.bytesConsumed.setValue("" + newValue);
    }
    
    /**
     * file through which clients can write to the process's input stream directly
     */
    private class StreamWriter extends StyxFile
    {
        private OutputStream stream = null;

        public StreamWriter(String name) throws StyxException
        {
            super(name);
        }

        public void setOutputStream(OutputStream os)
        {
            this.stream = os;
        }        

        /**
         * Writes the given number of bytes to the stream. The offset
         * is ignored; it will always writes to the current stream position, so
         * the behaviour of this method when multiple clients are connected is
         * undefined
         * @todo deal with request to flush the write message
         */
        public void write(StyxFileClient client, long offset, long count,
            ByteBuffer data, String user, boolean truncate, int tag)
            throws StyxException
        {
            if (stream == null)
            {
                throw new StyxException("Stream not ready for writing");
            }
            try
            {
                if (count == 0)
                {
                    // make sure waiting clients get the final value of bytesConsumed
                    bytesConsumed.flush();
                    stream.close();
                }
                int bytesToWrite = data.remaining();
                if (count < data.remaining())
                {
                    // Would normally be an error if count != data.remaining(),
                    // but we'll let the calling application pick this up
                    bytesToWrite = (int)count;
                }
                byte[] arr = new byte[bytesToWrite];
                data.get(arr);
                stream.write(arr);
                stream.flush();
                // Update the number of bytes consumed
                setBytesConsumed(bytesCons + bytesToWrite);
                this.replyWrite(client, bytesToWrite, tag);
            }
            catch(IOException ioe)
            {
                throw new StyxException("IOException occurred when writing to "
                    + "the stream: " + ioe.getMessage());
            }
        }

        public void read(StyxFileClient client, long offset, long count, int tag)
            throws StyxException
        {
            throw new StyxException("cannot read from an input stream");
        }        
    }
    
}

/**
 * Type-safe enumeration of possible status codes
 */
class StatusCode
{
    private String text;
    private StatusCode(String text)
    {
        this.text = text;
    }
    public static final StatusCode CREATED  = new StatusCode("created");
    public static final StatusCode RUNNING  = new StatusCode("running");
    public static final StatusCode FINISHED = new StatusCode("finished");
    public static final StatusCode ABORTED  = new StatusCode("aborted");
    public static final StatusCode ERROR    = new StatusCode("error");
    /**
     * @return a short string describing this status code
     */
    public String getText()
    {
        return this.text;
    }
}
