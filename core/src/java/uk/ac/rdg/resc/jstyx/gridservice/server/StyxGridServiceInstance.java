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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;

import java.util.Vector;
import java.util.Iterator;

import org.apache.mina.common.ByteBuffer;
import org.apache.log4j.Logger;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.JSAPResult;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileChangeListener;
import uk.ac.rdg.resc.jstyx.server.AsyncStyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.server.InMemoryFile;
import uk.ac.rdg.resc.jstyx.server.FileOnDisk;
import uk.ac.rdg.resc.jstyx.server.DirectoryOnDisk;
import uk.ac.rdg.resc.jstyx.server.MonitoredFileOnDisk;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.ULong;

import uk.ac.rdg.resc.jstyx.gridservice.config.*;

/**
 * Class representing a StyxGridService instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.39  2005/11/11 21:57:21  jonblower
 * Implemented passing of URLs to input files
 *
 * Revision 1.38  2005/11/10 19:50:43  jonblower
 * Added code to handle output files
 *
 * Revision 1.37  2005/11/09 18:01:31  jonblower
 * Changed way that input files are exposed and their relation to parameters
 *
 * Revision 1.36  2005/11/04 19:29:53  jonblower
 * Moved code that writes to std input to SGSInputFile
 *
 * Revision 1.35  2005/11/04 09:11:23  jonblower
 * Made SGSParamFile inherit from AsyncStyxFile instead of InMemoryFile
 *
 * Revision 1.34  2005/11/03 07:42:47  jonblower
 * Implemented JSAP-based parameter parsing
 *
 * Revision 1.30  2005/10/18 14:08:14  jonblower
 * Removed inputfiles from namespace
 *
 * Revision 1.29  2005/10/14 18:03:23  jonblower
 * Fixed bug with writing to input stream
 *
 * Revision 1.28  2005/09/08 07:08:59  jonblower
 * Removed "String user" from list of parameters to StyxFile.write()
 *
 * Revision 1.27  2005/08/31 17:08:54  jonblower
 * Fixed bug with handling exception when Process could not be created
 *
 * Revision 1.26  2005/08/30 16:29:00  jonblower
 * Added processAndReplyRead() helper functions to StyxFile
 *
 * Revision 1.24  2005/08/02 08:05:18  jonblower
 * Continuing to implement steering
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
    private SGSConfig sgsConfig; // The configuration object used to create this instance
    private File workDir; // The working directory of this instance
    private Process process = null; // The process object returned by runtime.exec()
    private StatusCode statusCode;
    private ServiceDataElement status; // The status of the service
    private ServiceDataElement exitCode; // The exit code from the executable
    private ServiceDataElement bytesConsumed; // The number of bytes consumed by the service
    private CachingStreamReader stdout = new CachingStreamReader(this, "stdout");  // The standard output from the program
    private CachingStreamReader stderr = new CachingStreamReader(this, "stderr");  // The standard error from the program
    private StyxDirectory inputsDir; // Contains the input files
    private StyxDirectory outputsDir; // Contains the output files
    private SGSInputFile.StdinFile stdin;  // The standard input to the program
    private JSAP jsap; // JSAP object for parsing the command-line parameters
    private StyxDirectory paramDir; // Contains the command-line parameters to pass to the executable
    private StyxFile commandLineFile; // The file containing the command line
    private String command; // The command to run (i.e. the string that is passed to System.exec)
    private long startTime;
    
    // SGSInstanceChangeListeners that are listening for changes to this SGS instance
    private Vector changeListeners;
    
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
        this.sgsConfig = sgsConfig;
        
        this.command = sgsConfig.getCommand();
        this.workDir = new File(sgsConfig.getWorkingDirectory() +
            StyxUtils.SYSTEM_FILE_SEPARATOR + id);
        this.changeListeners = new Vector();
        
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
        
        // Add the parameters as SGSParamFiles.
        this.paramDir = new StyxDirectory("params");
        this.jsap = sgsConfig.getParamParser();
        Vector params = sgsConfig.getParams();
        for (int i = 0; i < params.size(); i++)
        {
            SGSParam param = (SGSParam)params.get(i);
            // Parameter files exhibit asynchronous behaviour so that other
            // clients can be notified when a parameter value changes
            this.paramDir.addChild(new SGSParamFile(param, this));
        }
        this.addChild(paramDir);
        
        // Add the inputs and outputs
        // Create a directory which creates its children as FilesOnDisk in the
        // working directory of the instance. This allows us to upload new files
        // into the working directory
        // TODO: we may not always want people to upload arbitrary files into
        //   this directory?  Only if some files are specified by a parameter?
        this.inputsDir = new StyxDirectory("inputs")
        {
             public StyxFile createChild(String name, int perm, boolean isDir,
                boolean isAppOnly, boolean isExclusive)
                throws StyxException
             {
                 File f = new File(workDir, name);
                 return DirectoryOnDisk.createFileOrDirectory(f, isDir, perm);
             }
        };
        this.outputsDir = new StyxDirectory("outputs");
        
        Vector inputs = sgsConfig.getInputs();
        for (int i = 0; i < inputs.size(); i++)
        {
            SGSInput input = (SGSInput)inputs.get(i);
            SGSInputFile inputFile = null;
            if (input.getType() == SGSInput.STREAM)
            {
                this.stdin = new SGSInputFile.StdinFile(this);
                inputFile = this.stdin;
            }
            else if (input.getType() == SGSInput.FILE)
            {
                // This is a fixed input file.  Create the java.io.File object
                // that represents the local file itself.
                File file = new File(this.workDir, input.getName());
                inputFile = new SGSInputFile.File(file, this);
            }
            else if (input.getType() == SGSInput.FILE_FROM_PARAM)
            {
                // Do nothing: these files do not appear in the namespace until
                // they are uploaded
            }
            else
            {
                throw new StyxException("Internal error: unknown type of input "
                    + input.getName());
            }
            if (inputFile != null)
            {
                this.inputsDir.addChild(inputFile);
            }
        }
        
        // We add the output files when the service is started
        this.addChild(this.inputsDir).addChild(this.outputsDir);
        
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
        // Add the default SDEs that all services have
        this.status = new StringServiceDataElement("status", true, "created");
        serviceDataDir.addChild(this.status.getAsyncStyxFile());
        this.exitCode = new StringServiceDataElement("exitCode", true, "");
        serviceDataDir.addChild(this.exitCode.getAsyncStyxFile());        
        // If we are reading from stdin, add a bytesConsumed SDE
        if (this.stdin != null)
        {
            this.bytesConsumed = new StringServiceDataElement("bytesConsumed",
                true, "0", 2.0f);
            serviceDataDir.addChild(this.bytesConsumed.getAsyncStyxFile());
        }
        // Add the rest of the SDEs
        for (int i = 0; i < serviceDataElements.size(); i++)
        {
            SDEConfig sde = (SDEConfig)serviceDataElements.get(i);
            // Look for the special SDEs.
            if (sde.getName().equals("status") ||
                sde.getName().equals("bytesConsumed") ||
                sde.getName().equals("exitCode"))
            {
                // Ignore these: these are default SDEs that we will add automatically
                // TODO should we throw an exception here and treat these as
                //     reserved words?
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
        
        // Add a file that, when read, reveals that command line that will
        // be executed through Runtime.exec(). This is an AsyncStyxFile so
        // that clients can be notified asynchronously of changes to the 
        // command line if they wish
        this.commandLineFile = new CommandLineFile();
        this.addChild(new AsyncStyxFile(this.commandLineFile));
        
        this.statusCode = StatusCode.CREATED;
    }
    
    /**
     * Adds the outputs to the outputs/ directory.  This is called after the
     * service is started.
     */
    private void addOutputs() throws StyxException
    {
        Vector outputs = this.sgsConfig.getOutputs();
        for (int i = 0; i < outputs.size(); i++)
        {
            SGSOutput output = (SGSOutput)outputs.get(i);
            StyxFile fileToAdd = null;
            if (output.getType() == SGSOutput.STREAM)
            {
                if (output.getName().equals("stdout"))
                {
                    // Add the standard output file
                    fileToAdd = this.stdout;
                }
                else if (output.getName().equals("stderr"))
                {
                    // Add the standard error file
                    fileToAdd = this.stderr;
                }
            }
            else if (output.getType() == SGSOutput.FILE)
            {
                File file = new File(this.workDir, output.getName());
                fileToAdd = new SGSOutputFile(file, this);
            }
            else if (output.getType() == SGSOutput.FILE_FROM_PARAM)
            {
                // Get the name of the file from the relevant parameter
                boolean found = false;
                StyxFile[] paramFiles = this.paramDir.getChildren();
                for (int j = 0; j < paramFiles.length; j++)
                {
                    SGSParamFile paramFile = (SGSParamFile)paramFiles[j];
                    if (paramFile.getName().equals(output.getName()))
                    {
                        found = true;
                        String val = paramFile.getParameterValue();
                        if (val != null && !val.equals(""))
                        {
                            File file = new File(this.workDir, val);
                            fileToAdd = new SGSOutputFile(file, this);
                        }
                    }
                }
                if (!found)
                {
                    throw new StyxException("Internal error: couldn't find parameter file "
                        + output.getName());
                }
            }
            if (fileToAdd != null)
            {
                this.outputsDir.addChild(fileToAdd);
            }
        }
    }
    
    /**
     * Makes sure all the input files are ready
     * @throws StyxException if a required input file is not ready and a URL
     * has not been set.
     */
    private void prepareInputFiles() throws StyxException
    {
        StyxFile[] inputFiles = this.inputsDir.getChildren();
        for (int i = 0; i < inputFiles.length; i++)
        {
            if (inputFiles[i] instanceof SGSInputFile)
            {
                SGSInputFile inputFile = (SGSInputFile)inputFiles[i];
                URL url = inputFile.getURL();
                if (inputFile instanceof SGSInputFile.File)
                {
                    // This is a fixed-name input file
                    SGSInputFile.File inFile = (SGSInputFile.File)inputFile;
                    if (url == null)
                    {
                        // Check to see if any data have been uploaded
                        if (!inFile.dataUploadComplete())
                        {
                            throw new StyxException("Must upload data to input file "
                                + inFile.getName());
                        }
                    }
                    else
                    {
                        // We have set a URL for this file.  Download the data.
                        this.downloadFrom(url, inFile.getName());
                    }
                }
            }
        }
        // Search through the parameters looking for input files
        
        System.err.println("All input files uploaded");
    }
    
    public void downloadFrom(URL url, String filename) throws StyxException
    {
        try
        {
            File targetFile = new File(this.workDir, filename);
            log.debug("Downloading from " + url + " to " + targetFile.getPath());
            System.err.println("Downloading from " + url + " to " + targetFile.getPath());
            FileOutputStream fout = new FileOutputStream(targetFile);
            InputStream in = url.openStream();
            byte[] b = new byte[8192];
            int n = 0;
            do
            {
                n = in.read(b);
                if (n >= 0)
                {
                    fout.write(b, 0, n);
                }
                else
                {
                    in.close();
                    fout.close();
                    b = null;
                }
            } while (n >= 0);
        }
        catch(IOException ioe)
        {
            throw new StyxException("IOException downloading from "
                + url + ": " + ioe.getMessage());
        }
    }
    
    /**
     * Returns the working directory of this instance
     */
    public File getWorkingDirectory()
    {
        return this.workDir;
    }
    
    /**
     * Gets the status of this service instance
     */
    public StatusCode getStatus()
    {
        return this.statusCode;
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
                // Check that all the parameters are valid
                StyxFile[] paramFiles = paramDir.getChildren();
                for (int i = 0; i < paramFiles.length; i++)
                {
                    SGSParamFile sgsPF = (SGSParamFile)paramFiles[i];
                    // The checkValid() method throws a StyxException if the
                    // contents of the parameter file are not valid for some reason
                    sgsPF.checkValid();
                }
                
                // Check all input files have been uploaded, and download
                // any input files that have been specified by reference.
                // TODO: this will block until all the data have been downloaded.
                prepareInputFiles();
                
                // Add the output files to the namespace
                addOutputs();
                
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
                    // stdout and stderr data)
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
                // Check to see if the process expects some data on standard input
                if (stdin != null)
                {
                    stdin.setOutputStream(process.getOutputStream());
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
                throw new StyxException("unknown command: " + cmdString);
            }
        }        
    }
    
    /**
     * File that, when read, reveals the command line that will be executed
     * when the SGS instance is started.  Clients can write the whole command line
     * to this file and hence set all the parameters at once.
     */
    private class CommandLineFile extends StyxFile
    {
        public CommandLineFile() throws StyxException
        {
            super("commandline", 0666);
        }
        
        public void read(StyxFileClient client, long offset, int count, int tag)
            throws StyxException
        {
            // TODO: a bit inefficient to create the command line from scratch
            // each time, but this won't be called much anyway
            this.processAndReplyRead(getCommandLine(), client, offset, count, tag);
        }
        
        /**
         * Write the command line all in one go (not including the name of the
         * executable itself).  This will set the values of
         * all the parameters in the params/ directory. At the moment the command
         * line must be written in a single Styx message.
         */
        public void write(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException
        {
            if (offset != 0)
            {
                throw new StyxException("Must write to the start of the command line file");
            }
            if (!truncate)
            {
                throw new StyxException("Must write to the command line file with truncation");
            }
            // Set the limit of the input data buffer correctly
            data.limit(data.position() + count);
            String cmdLine = StyxUtils.dataToString(data);
            
            // Parse the command line
            JSAPResult result = jsap.parse(cmdLine);
            
            if (result.success())
            {
                // Parsing was successful: populate all of the parameter files
                // TODO: I guess that if the new and old values are identical
                //   we don't need to change the contents and hence notify waiting clients?
                //   Maybe this logic can be handled in the SGSParamFile class.
                StyxFile[] paramFiles = paramDir.getChildren();
                for (int i = 0; i < paramFiles.length; i++)
                {
                    SGSParamFile sgsPF = (SGSParamFile)paramFiles[i];
                    Parameter param = sgsPF.getJSAPParameter();

                    if (param instanceof Switch)
                    {
                        boolean switchSet = result.getBoolean(sgsPF.getName());
                        sgsPF.setParameterValue(switchSet ? "true" : "false");
                    }
                    else
                    {
                        String[] arr = result.getStringArray(sgsPF.getName());
                        if (arr != null && arr.length > 0)
                        {
                            StringBuffer str = new StringBuffer(arr[0]);
                            for (int j = 1; j < arr.length; j++)
                            {
                                str.append(" " + arr[j]);
                            }
                            sgsPF.setParameterValue(str.toString());
                        }
                        else
                        {
                            // This probably won't be reached but it's here just
                            // in case getStringArray() doesn't return a result
                            // but getString() does - unlikely!
                            String str = result.getString(sgsPF.getName());
                            sgsPF.setParameterValue(str == null ? "" : str);
                        }
                    }
                }
            }
            else
            {
                // An error occurred in parsing: get the first error in the
                // Iterator (TODO: get more errors?)
                Iterator errIt = result.getErrorMessageIterator();
                String errMsg = "Error occurred parsing command line: ";
                if (errIt.hasNext())
                {
                    errMsg += (String)errIt.next();
                }
                else
                {
                    errMsg += "no details";
                }
                throw new StyxException(errMsg);
            }
            
            // Notify waiting clients that the command line has changed
            // TODO: should this just be an AsyncStyxFile instead?
            commandLineChanged();
            
            this.replyWrite(client, count, tag);
        }
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
    
    /**
     * Starts a thread that redirects the data from the given URL to the 
     * given output stream
     * @throws StyxException if no data could be read from the given url
     */
    public void readFrom(URL url, OutputStream os) throws StyxException
    {
        try
        {
            InputStream is = url.openStream();
            new RedirectStream(is, os).start();
            System.out.println("*** Reading stdin from " + url + "***");
        }
        catch (IOException ioe)
        {
            process.destroy();
            setStatus(StatusCode.ERROR);
            throw new StyxException("Cannot read from " + url);
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
            long bytesCons = 0;
            try
            {
                byte[] arr = new byte[8192]; // TODO: is this an appropriate buffer size?
                int n = 0;
                while (n >= 0)
                {
                    n = this.is.read(arr);
                    System.err.println("*** Read " + n + " bytes***");
                    if (n >= 0)
                    {
                        this.os.write(arr, 0, n);
                        bytesCons += n;
                    }
                    // Update the number of bytes consumed
                    // TODO: should we do this here or in another thread?
                    // It won't hold us up as long as the network is the limiting
                    // factor.
                    setBytesConsumed(bytesCons);
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
        this.fireStatusChanged();
    }
    
    private void setStatus(StatusCode code)
    {
        this.setStatus(code, null);
    }
    
    /**
     * Sets the number of bytes consumed by the service instance
     * @param flush If true, will force the waiting clients to get the new value
     * (should only be used sparingly)
     */
    synchronized void setBytesConsumed(long newValue, boolean flush)
    {
        if (this.bytesConsumed != null)
        {
            this.bytesConsumed.setValue("" + newValue);
        }
        if (flush)
        {
            this.bytesConsumed.flush();
        }
    }
    
    /**
     * Sets the number of bytes consumed by the service instance
     */
    synchronized void setBytesConsumed(long newValue)
    {
        this.setBytesConsumed(newValue, false);
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
    
    /**
     * Called when the status of this service instance changes. Fires the
     * statusChanged() event on all registered change listeners
     */
    private void fireStatusChanged()
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
                listener.statusChanged(this.statusCode);
            }
        }
    }
    
    /**
     * Adds a listener that will be notified of changes to this SGS. If the
     * listener is already registered, this will do nothing.
     */
    public void addChangeListener(SGSInstanceChangeListener listener)
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
     * Removes a SGSInstanceChangeListener.  (Note that this will only remove the first
     * instance of a given SGSInstanceChangeListener.  If, for some reason, more than one 
     * copy of the same change listener has been registered, this method will
     * only remove the first.)
     */
    public void removeChangeListener(SGSInstanceChangeListener listener)
    {
        synchronized(this.changeListeners)
        {
            boolean contained = this.changeListeners.remove(listener);
        }
    }
    
}

