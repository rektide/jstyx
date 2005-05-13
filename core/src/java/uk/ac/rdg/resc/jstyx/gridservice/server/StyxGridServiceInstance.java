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

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

import java.util.Vector;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.server.AsyncStyxFile;
import uk.ac.rdg.resc.jstyx.server.InMemoryFile;
import uk.ac.rdg.resc.jstyx.server.MonitoredFileOnDisk;

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
 * Revision 1.12  2005/05/13 16:49:34  jonblower
 * Coded dynamic detection and display of service data, also included streams in config file
 *
 * Revision 1.11  2005/05/11 15:14:31  jonblower
 * Implemented more flexible definition of service data elements
 *
 * Revision 1.10  2005/05/09 07:07:48  jonblower
 * Minor change
 *
 * Revision 1.9  2005/04/27 16:11:43  jonblower
 * Added capability to add documentation files to SGS namespace
 *
 * Revision 1.8  2005/04/26 07:46:11  jonblower
 * Continuing to improve setting of parameters in Styx Grid Services
 *
 * Revision 1.7  2005/03/24 17:33:51  jonblower
 * Improved reading of service parameters from config file
 *
 * Revision 1.6  2005/03/24 14:47:47  jonblower
 * Provided default read() and write() methods for StyxFile so it is no longer abstract
 *
 * Revision 1.5  2005/03/24 09:48:31  jonblower
 * Changed 'count' from long to int throughout for reading and writing
 *
 * Revision 1.4  2005/03/24 07:57:41  jonblower
 * Improved code for reading SSL info from SGSconfig file and included parameter information for the Grid Services in the config file
 *
 * Revision 1.3  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.2  2005/03/18 16:45:18  jonblower
 * Released ByteBuffers after use
 *
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.2  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
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
    private ServiceDataElement status; // The status of the service
    private StatusCode statusCode;
    private ServiceDataElement bytesConsumed; // The number of bytes consumed by the service
    private long bytesCons;
    private CachingStreamReader stdout = new CachingStreamReader(this, "stdout");  // The standard output from the program
    private CachingStreamReader stderr = new CachingStreamReader(this, "stderr");  // The standard error from the program
    private StreamWriter stdin  = new StreamWriter("stdin");   // The standard input to the program
    private StyxDirectory paramDir; // Contains the command-line parameters to pass to the executable
    private String command; // The command to run (i.e. the string that is passed to System.exec)
    private InMemoryFile inputURL; // Non-empty if we're going to read the input from a URL
    private long startTime;
    
    /**
     * Creates a new StyxGridService with the given configuration
     * @todo: sort out permissions and owners on all these files
     */
    public StyxGridServiceInstance(StyxGridService sgs, int id,
        String command, String workDir, Vector streams, Vector params,
        Vector serviceDataElements) throws StyxException
    {
        super("" + id);
        this.sgs = sgs;
        this.id = id;
        this.command = command;
        this.workDir = new File(workDir + StyxUtils.SYSTEM_FILE_SEPARATOR + id);
        
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
        
        // Add the ctl file
        this.addChild(new ControlFile(this)); // the ctl file
        
        // Add the streams
        // TODO: would the name "streams" be more appropriate?  The presence of
        // the "inurl" file would be contrary to this though (it's not a stream)
        StyxDirectory ioDir = new StyxDirectory("io");
        for (int i = 0; i < streams.size(); i++)
        {
            SGSStream stream = (SGSStream)streams.get(i);
            if (stream.getName().equals("stdin"))
            {
                // Add the input stream and the inurl file
                ioDir.addChild(this.stdin);  // input stream
                this.inputURL = new InMemoryFile("inurl");
                ioDir.addChild(this.inputURL);
            }
            else if (stream.getName().equals("stdout"))
            {
                // Add the standard output file
                ioDir.addChild(this.stdout);
            }
            else if (stream.getName().equals("stderr"))
            {
                // Add the standard error file
                ioDir.addChild(this.stderr);
            }
            else
            {
                throw new StyxException("Stream \"" + stream.getName() +
                    "\" is not supported (must be stdin, stdout or stderr");
            }
        }
        this.addChild(ioDir);
        
        // Add the parameters as SGSParamFiles.
        this.paramDir = new StyxDirectory("params");
        for (int i = 0; i < params.size(); i++)
        {
            SGSParam param = (SGSParam)params.get(i);
            this.paramDir.addChild(new SGSParamFile(param));
        }
        this.addChild(paramDir);
        
        // Add the service data: the files exposing the service data will all
        // have asynchronous behaviour
        StyxDirectory serviceDataDir = new StyxDirectory("serviceData");
        for (int i = 0; i < serviceDataElements.size(); i++)
        {
            SDEConfig sde = (SDEConfig)serviceDataElements.get(i);
            // Look for the special SDEs.
            if (sde.getName().equals("status"))
            {
                this.status = new StringServiceDataElement("status", true, "created");
                serviceDataDir.addChild(this.status.getAsyncStyxFile());
            }
            else if (sde.getName().equals("bytesConsumed"))
            {
                this.bytesConsumed = new StringServiceDataElement("bytesConsumed",
                    true, "" + this.bytesCons, 2.0f);
                serviceDataDir.addChild(this.bytesConsumed.getAsyncStyxFile());
            }
            else
            {
                // This is a custom SDE
                if (sde.getFilePath().equalsIgnoreCase(""))
                {
                    throw new StyxException("Service data element " +
                        sde.getName() + " must have a backing file");
                }
                System.err.println("Min update interval for " + sde.getName() 
                    + " is " + sde.getMinUpdateInterval());
                MonitoredFileOnDisk monFile = new MonitoredFileOnDisk(sde.getName(),
                    new File(this.workDir, sde.getFilePath()), 
                    (long)(sde.getMinUpdateInterval() * 1000));
                monFile.startMonitoring();
                // TODO: stop monitoring somehow, when service is destroyed?
                serviceDataDir.addChild(monFile);
            }
        }
        this.addChild(serviceDataDir);
        
        this.bytesCons = 0;
        
        // Add the debug directory
        StyxDirectory debugDir = new StyxDirectory("debug");
        // Add a file that, when read, reveals that command line that will
        // be executed through Runtime.exec():
        debugDir.addChild(new CommandLineFile());
        this.addChild(debugDir);
        
        this.statusCode = StatusCode.CREATED;
    }
    
    /**
     * Returns the working directory of this instance
     */
    public File getWorkingDirectory()
    {
        return this.workDir;
    }
    
    /**
     * File used to control the service instance (start, stop, destroy etc)
     * @todo Reading from this file could return a list of supported commands.
     */
    private class ControlFile extends StyxFile
    {
        
        private StyxDirectory instanceRoot; // The root directory of the SGS instance
        
        /** Creates a new instance of ControlFile */
        public ControlFile(StyxDirectory instanceRoot) throws StyxException
        {
            super("ctl");
            this.instanceRoot = instanceRoot;
        }
        
        public void write(StyxFileClient client, long offset, int count,
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
                    process = runtime.exec(getCommandLine(), null, workDir);
                    setStatus(StatusCode.RUNNING);
                    new Waiter().start(); // Thread that waits for the process
                                          // to finish, then sets status
                    
                    // Start reading from stdout and stderr. Note that we do this
                    // even if the "stdout" and "stderr" streams are not exposed
                    // through the Styx interface (we must do this to consume the
                    // stdout and stderr data
                    stdout.startReading(process.getInputStream());
                    stderr.startReading(process.getErrorStream());
                }
                catch(IOException ioe)
                {
                    throw new StyxException("IOException when calling runtime.exec():"
                        + ioe.getMessage());
                }
                catch(StyxException se)
                {
                    // There was an error reading from the streams. Tidy up.
                    process.destroy();
                    setStatus(StatusCode.ERROR, se.getMessage());
                    throw new StyxException("Internal error: could not start "
                        + "reading from output and error streams");
                }
                if (inputURL != null)
                {
                    // If the inputURL file is not null, this means that the executable
                    // expects to get data via stdin (this has been set in the 
                    // config file).  In the following code, we decide whether
                    // we're going to get the data from a URL or whether the user
                    // is expected to write data into the stdin file in the namespace
                    String urlStr = inputURL.getContents();
                    if (urlStr.trim().equals(""))
                    {
                        // We haven't set a URL, so we connect the "stdin" file in the
                        // Styx namespace to the standard input of the executable.
                        stdin.setOutputStream(process.getOutputStream());
                    }
                    else
                    {
                        // We have set a URL so we shall get an InputStream to read 
                        // data from this URL, then start a thread to read from the URL
                        // and redirect the data to the standard input of the executable
                        // From this point on, we will not be able to write to the
                        // "stdin" file in the namespace
                        // TODO: should we remove the "stdin" file from the namespace?
                        try
                        {
                            InputStream urlin = new URL(urlStr).openStream();
                            // Start a thread reading from the url and writing to the 
                            // process's standard input.
                            new RedirectStream(urlin, process.getOutputStream()).start();
                        }
                        catch (MalformedURLException e)
                        {
                            process.destroy();
                            throw new StyxException(urlStr + " is not a valid URL");
                        }
                        catch (IOException ioe)
                        {
                            process.destroy();
                            throw new StyxException("Cannot read from " + urlStr);
                        }
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
                // We'll comment this out for the moment.
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
    
    /**
     * File that, when read, reveals the command line that will be executed
     * when the SGS instance is started
     */
    private class CommandLineFile extends StyxFile
    {
        public CommandLineFile() throws StyxException
        {
            // The file is read-only
            super("commandline", 0444);
        }
        
        public void read(StyxFileClient client, long offset, int count, int tag)
            throws StyxException
        {
            // TODO: a bit inefficient to create the command line from scratch
            // each time, but this won't be called much anyway
            byte[] bytes = StyxUtils.strToUTF8(getCommandLine());
            if (offset > bytes.length)
            {
                this.replyRead(client, new byte[0], tag);
            }
            int bytesToReturn = (bytes.length - (int)offset) > count ? count :
                (bytes.length - (int)offset);
            this.replyRead(client, bytes, (int)offset, bytesToReturn, tag);
        }
        
        // TODO: Shall we allow direct writing to this file?  This might either
        // automatically populate the individual parameter files (hard) or
        // simply disable the ability to read from or write to the parameter
        // files (easy).
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
     * Gets the command line that will be executed
     */
    private String getCommandLine()
    {
        StringBuffer buf = new StringBuffer(this.command);
        if (this.paramDir != null)
        {
            buf.append(" ");
            StyxFile[] paramFiles = this.paramDir.getChildren();
            for (int i = 0; i < paramFiles.length; i++)
            {
                // We can be pretty confident that this cast is safe
                SGSParamFile paramFile = (SGSParamFile)paramFiles[i];
                buf.append(paramFile.getCommandLineFragment());
                if (i < paramFiles.length - 1)
                {
                    buf.append(" ");
                }
            }
        }
        return buf.toString();
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
        if (this.status != null)
        {
            this.status.setValue(code.getText() + msg);
        }
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
        if (this.bytesConsumed != null)
        {
            this.bytesConsumed.setValue("" + newValue);
        }
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
        public void write(StyxFileClient client, long offset, int count,
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
                    bytesToWrite = count;
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
