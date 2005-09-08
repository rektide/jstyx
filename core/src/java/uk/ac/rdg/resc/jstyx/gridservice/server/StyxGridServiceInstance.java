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
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;

import java.util.Vector;

import org.apache.mina.common.ByteBuffer;
import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.server.AsyncStyxFile;
import uk.ac.rdg.resc.jstyx.server.InMemoryFile;
import uk.ac.rdg.resc.jstyx.server.FileOnDisk;
import uk.ac.rdg.resc.jstyx.server.DirectoryOnDisk;
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
 * Revision 1.28  2005/09/08 07:08:59  jonblower
 * Removed "String user" from list of parameters to StyxFile.write()
 *
 * Revision 1.27  2005/08/31 17:08:54  jonblower
 * Fixed bug with handling exception when Process could not be created
 *
 * Revision 1.26  2005/08/30 16:29:00  jonblower
 * Added processAndReplyRead() helper functions to StyxFile
 *
 * Revision 1.25  2005/08/02 16:45:20  jonblower
 * *** empty log message ***
 *
 * Revision 1.24  2005/08/02 08:05:18  jonblower
 * Continuing to implement steering
 *
 * Revision 1.23  2005/08/01 17:01:08  jonblower
 * Started to implement steering
 *
 * Revision 1.22  2005/08/01 16:38:05  jonblower
 * Implemented simple parameter handling
 *
 * Revision 1.21  2005/07/29 16:56:07  jonblower
 * Implementing reading command line asynchronously
 *
 * Revision 1.20  2005/06/20 07:17:35  jonblower
 * Wrapped SGSParamFile as AsyncStyxFile
 *
 * Revision 1.19  2005/06/14 07:45:16  jonblower
 * Implemented setting of params and async notification of parameter changes
 *
 * Revision 1.18  2005/06/10 07:53:12  jonblower
 * Changed SGS namespace: removed "inurl" and subsumed functionality into "stdin"
 *
 * Revision 1.17  2005/05/27 17:05:07  jonblower
 * Changes to incorporate GeneralCachingStreamReader
 *
 * Revision 1.16  2005/05/26 21:24:44  jonblower
 * Added exitCode as a new service data element
 *
 * Revision 1.15  2005/05/26 16:50:57  jonblower
 * Fixed bug with input files directory
 *
 * Revision 1.14  2005/05/19 18:42:07  jonblower
 * Implementing specification of input files required by SGS
 *
 * Revision 1.13  2005/05/16 11:00:53  jonblower
 * Changed SGS config XML file structure: separated input and output streams and changed some tag names
 *
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
    
    private static final Logger log = Logger.getLogger(StyxGridServiceInstance.class);
    private static final Runtime runtime = Runtime.getRuntime();
    
    private StyxGridService sgs; // The SGS to which this instance belongs
    private int id; // The ID number of this instance
    private File workDir; // The working directory of this instance
    private Process process = null; // The process object returned by runtime.exec()
    private StatusCode statusCode;
    private ServiceDataElement status; // The status of the service
    private ServiceDataElement exitCode; // The exit code from the executable
    private ServiceDataElement bytesConsumed; // The number of bytes consumed by the service
    private long bytesCons;
    private CachingStreamReader stdout = new CachingStreamReader(this, "stdout");  // The standard output from the program
    private CachingStreamReader stderr = new CachingStreamReader(this, "stderr");  // The standard error from the program
    private StreamWriter stdin  = new StreamWriter("stdin");   // The standard input to the program
    private StyxDirectory paramDir; // Contains the command-line parameters to pass to the executable
    private StyxFile commandLineFile; // The file containing the command line
    private String command; // The command to run (i.e. the string that is passed to System.exec)
    private URL inputURL = null; // Non-null if we're going to read the input from a URL
    private long startTime;
    
    /**
     * Creates a new StyxGridService with the given configuration
     * @todo: sort out permissions and owners on all these files
     */
    public StyxGridServiceInstance(StyxGridService sgs, int id,
        SGSConfig sgsConfig) throws StyxException
    {
        super("" + id);
        this.sgs = sgs;
        this.id = id;
        this.command = sgsConfig.getCommand();
        this.workDir = new File(sgsConfig.getWorkingDirectory() +
            StyxUtils.SYSTEM_FILE_SEPARATOR + id);
        
        if (this.workDir.exists())
        {
            // Delete the directory and all its contents
            deleteDir(this.workDir);
        }
        // (Re)create the working directory
        if (!this.workDir.mkdirs())
        {
            throw new StyxException("Unable to create working directory "
                + this.workDir);
        }
        
        // Add the ctl file
        this.addChild(new ControlFile(this)); // the ctl file
        
        // Add the streams
        // TODO: would the name "streams" be more appropriate?  The presence of
        // the "inurl" file would be contrary to this though (it's not a stream)
        StyxDirectory ioDir = new StyxDirectory("io");
        StyxDirectory inStreams = new StyxDirectory("in");
        StyxDirectory outStreams = new StyxDirectory("out");
        ioDir.addChild(inStreams).addChild(outStreams);
        Vector streams = sgsConfig.getStreams();
        for (int i = 0; i < streams.size(); i++)
        {
            SGSStream stream = (SGSStream)streams.get(i);
            // Currently we're only dealing with the standard streams. In future
            // we may allow other i/o streams (e.g. file-based ones)
            if (stream.getName().equals("stdin"))
            {
                // Add the input stream and the inurl file
                inStreams.addChild(this.stdin);  // input stream
            }
            else if (stream.getName().equals("stdout"))
            {
                // Add the standard output file
                outStreams.addChild(this.stdout);
            }
            else if (stream.getName().equals("stderr"))
            {
                // Add the standard error file
                outStreams.addChild(this.stderr);
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
        Vector params = sgsConfig.getParams();
        for (int i = 0; i < params.size(); i++)
        {
            SGSParam param = (SGSParam)params.get(i);
            // Parameter files exhibit asynchronous behaviour so that many
            // clients can be notified when a parameter value changes
            this.paramDir.addChild(new AsyncStyxFile(new SGSParamFile(param, this)));
        }
        this.addChild(paramDir);
        
        // Add the steerable parameters
        StyxDirectory steeringDir = new StyxDirectory("steering");
        Vector steerables = sgsConfig.getSteerables();
        for (int i = 0; i < steerables.size(); i++)
        {
            Steerable steerable = (Steerable)steerables.get(i);
            // Create a file object for this steerable object
            File file = new File(this.workDir, steerable.getFilePath());
            try
            {
                // Create the backing file and enter the initial value
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.seek(0);
                // Write the initial value to the file as an array of bytes
                raf.write(StyxUtils.strToUTF8(steerable.getInitialValue()));
                // Truncate the file after the data we've just written
                raf.setLength(raf.getFilePointer());
                raf.close();
            }
            catch (IOException ioe)
            {
                if (log.isDebugEnabled())
                {
                    ioe.printStackTrace();
                }
                throw new StyxException("IOException creating steering file " +
                    file.getName() + ": " + ioe.getMessage());
            }
            steeringDir.addChild(new AsyncStyxFile(new FileOnDisk(steerable.getName(), file)));
        }
        this.addChild(steeringDir);
        
        // Add the service data: the files exposing the service data will all
        // have asynchronous behaviour
        StyxDirectory serviceDataDir = new StyxDirectory("serviceData");
        Vector serviceDataElements = sgsConfig.getServiceData();
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
            else if (sde.getName().equals("exitCode"))
            {
                this.exitCode = new StringServiceDataElement("exitCode", true, "");
                serviceDataDir.addChild(this.exitCode.getAsyncStyxFile());
            }
            else
            {
                // This is a custom SDE
                if (sde.getFilePath().equalsIgnoreCase(""))
                {
                    throw new StyxException("Service data element " +
                        sde.getName() + " must have a backing file");
                }
                MonitoredFileOnDisk monFile = new MonitoredFileOnDisk(sde.getName(),
                    new File(this.workDir, sde.getFilePath()), 
                    (long)(sde.getMinUpdateInterval() * 1000));
                monFile.startMonitoring();
                // TODO: stop monitoring somehow, when service is destroyed?
                serviceDataDir.addChild(monFile);
            }
        }
        this.addChild(serviceDataDir);
        
        // Add the input files
        // Create a directory which creates its children as FilesOnDisk in the
        // working directory of the instance
        StyxDirectory inputFilesDir = new StyxDirectory("inputFiles")
        {
             public StyxFile createChild(String name, int perm, boolean isDir,
                boolean isAppOnly, boolean isExclusive)
                throws StyxException
             {
                 File f = new File(workDir, name);
                 return DirectoryOnDisk.createFileOrDirectory(f, isDir, perm);
             }
        };
        inputFilesDir.setName("inputFiles");
        if (sgsConfig.getAllowOtherInputFiles())
        {
            inputFilesDir.setPermissions(0777);
        }
        else
        {
            inputFilesDir.setPermissions(0555);
        }
        // TODO: set permissions of directory depending on whether we can create
        // arbitrary input files inside it
        Vector inputFiles = sgsConfig.getInputFiles();
        for (int i = 0; i < inputFiles.size(); i++)
        {
            File inputFilePath = (File)inputFiles.get(i);
            // Get the real path of this file, relative to the working directory
            File realPath = new File(this.workDir, inputFilePath.getPath());
            // Creates a writeable FileOnDisk that doesn't yet have to exist
            FileOnDisk inputFile = new FileOnDisk(realPath, false);
            inputFilesDir.addChild(inputFile);
        }
        this.addChild(inputFilesDir);
        
        // Add the debug directory
        StyxDirectory debugDir = new StyxDirectory("debug");
        // Add a file that, when read, reveals that command line that will
        // be executed through Runtime.exec(). This is an AsyncStyxFile so
        // that clients can be notified asynchronously of changes to the 
        // command line if they wish
        this.commandLineFile = new CommandLineFile();
        debugDir.addChild(new AsyncStyxFile(this.commandLineFile));
        this.addChild(debugDir);
        
        this.bytesCons = 0;
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
            ByteBuffer data, boolean truncate, int tag)
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
                    stdout.setCacheFile(new File(workDir, "stdout"));
                    stdout.startReading(process.getInputStream());
                    stderr.setCacheFile(new File(workDir, "stderr"));
                    stderr.startReading(process.getErrorStream());
                }
                catch(IOException ioe)
                {
                    ioe.printStackTrace();
                    if (process == null)
                    {
                        // We didn't even start the process
                        throw new StyxException("Internal error: could not create process "
                            + getCommandLine());
                    }
                    else
                    {
                        // We've started the process but an error occurred elsewhere
                        process.destroy();
                        setStatus(StatusCode.ERROR, ioe.getMessage());
                        throw new StyxException("Internal error: could not start "
                            + "reading from output and error streams");
                    }
                }
                if (inputURL != null)
                {
                    // If the inputURL is not null, this means that the executable
                    // expects to get data via stdin (this has been set in the 
                    // config file).  In the following code, we decide whether
                    // we're going to get the data from a URL or whether the user
                    // is expected to write data into the stdin file in the namespace
                    if (inputURL == null)
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
                            InputStream urlin = inputURL.openStream();
                            // Start a thread reading from the url and writing to the 
                            // process's standard input.
                            new RedirectStream(urlin, process.getOutputStream()).start();
                        }
                        catch (IOException ioe)
                        {
                            process.destroy();
                            setStatus(StatusCode.ERROR);
                            throw new StyxException("Cannot read from " + inputURL);
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
            this.processAndReplyRead(getCommandLine(), client, offset, count, tag);
        }
        
        // TODO: Shall we allow direct writing to this file?  This might either
        // automatically populate the individual parameter files (hard) or
        // simply disable the ability to read from or write to the parameter
        // files (easy).
    }
    
    /**
     * This is called when something changes to change the command line arguments
     * (e.g. a parameter value changes
     */
    public void commandLineChanged()
    {
        this.commandLineFile.contentsChanged();
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
                    if (exitCode != null)
                    {
                        exitCode.setValue("" + exitCodeVal);
                    }
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
                AsyncStyxFile asyncFile = (AsyncStyxFile)paramFiles[i];
                SGSParamFile paramFile = (SGSParamFile)asyncFile.getBaseFile();
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
            super(name, 0222); // Set permission to write-only
        }

        public void setOutputStream(OutputStream os)
        {
            this.stream = os;
        }
        
        /**
         * Reading from this file returns the URL from which we are reading
         * (if set) or nothing if we are getting data direct via the Styx
         * interface
         */
        public void read(StyxFileClient client, long offset, int count, int tag)
            throws StyxException
        {
            this.processAndReplyRead(inputURL.toString(), client, offset, count, tag);
        }
        
        /**
         * Writes the given number of bytes to the stream. The offset
         * is ignored; it will always writes to the current stream position, so
         * the behaviour of this method when multiple clients are connected is
         * undefined
         * @todo deal with request to flush the write message
         */
        public void write(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException
        {
            if (statusCode != StatusCode.RUNNING)
            {
                // We haven't started the service this so the only legal input
                // is a "readfrom <URL>" command.  We assume that the entire
                // command will come in a single message
                int dataSize = data.remaining();
                if (offset != 0)
                {
                    throw new StyxException("Must write command to start of file");
                }
                // Split the command into tokens
                String[] tokens = StyxUtils.dataToString(data).split(" ");
                if (tokens.length != 2 || !tokens[0].equals("readfrom")
                    || tokens[0] == null || tokens[1] == null)
                {
                    throw new StyxException("Invalid command (must be \"readfrom <URL>\")");
                }
                try
                {
                    inputURL = new URL(tokens[1]);
                }
                catch(MalformedURLException mue)
                {
                    inputURL = null;
                    throw new StyxException("Invalid URL: " + tokens[1]);
                }
                this.replyWrite(client, dataSize, tag);
            }
            else if (inputURL != null)
            {
                // We're not allowed to write to this file if the service is running
                // and the input URL is set
                throw new StyxException("Cannot write to the input stream " +
                    "because the service is reading from " + inputURL);
            }
            else
            {
                // The service is running
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
     * Deletes a directory and its contents
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
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success)
                {
                    return false;
                }
            }
        }
        return dir.delete();
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
