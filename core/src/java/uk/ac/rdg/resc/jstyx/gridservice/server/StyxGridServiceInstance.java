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
import java.util.Date;

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
 * Revision 1.48  2006/02/23 09:06:25  jonblower
 * Fixed bug in destroy(): instances and their cached files are now destroyed properly
 *
 * Revision 1.47  2006/02/20 17:35:01  jonblower
 * Implemented correct handling of output files/streams (not fully tested yet)
 *
 * Revision 1.45  2006/02/17 09:24:02  jonblower
 * Changed so that output files are added to the namespace on initialization and 
 * parameters representing output files are not present in the namespace
 *
 * Revision 1.44  2006/01/04 16:45:29  jonblower
 * Implemented automatic termination of SGS instances using Quartz scheduler
 *
 * Revision 1.43  2006/01/04 11:24:58  jonblower
 * Implemented time directory in the SGS instance namespace
 *
 * Revision 1.42  2005/12/07 17:47:58  jonblower
 * Changed "commandline" file to "args" - now just contains arguments, not program name
 *
 * Revision 1.41  2005/12/01 08:37:48  jonblower
 * Changed ID from int to String
 *
 * Revision 1.40  2005/11/14 21:31:54  jonblower
 * Got SGSRun working for SC2005 demo
 *
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
 * Improved code for reading SSL info from SGSconfig file and included parameter
 * information for the Grid Services in the config file
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
 * Changed following changes to core JStyx library (replacement of
 * java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.1  2005/02/16 19:22:32  jonblower
 * Commit adding of SGS files to CVS
 *
 */
class StyxGridServiceInstance extends StyxDirectory implements JobChangeListener
{
    
    private static final Logger log = Logger.getLogger(StyxGridServiceInstance.class);
    private static final Runtime runtime = Runtime.getRuntime();
    
    private StyxGridService sgs; // The SGS to which this instance belongs
    private String id; // The ID of this instance
    private SGSConfig sgsConfig; // The configuration object used to create this instance
    private File workDir; // The working directory of this instance
    private AbstractJob job;
    private ServiceDataElement status; // The status of the service
    private ExitCodeFile exitCodeFile; // The exit code from the executable
    private StyxDirectory inputsDir; // Contains the input files
    private StyxDirectory outputsDir; // Contains the output files
    private SGSInputFile.StdinFile stdin;  // The standard input to the program
    private JSAP jsap; // JSAP object for parsing the command-line parameters
    private StyxDirectory paramDir; // Contains the command-line parameters to pass to the executable
    private Vector paramFiles; // Contains the SGSParamFiles
    private StyxFile argsFile; // The file containing the command line arguments
    private String command; // The command to run (i.e. the string that is passed to System.exec)
    
    private Date creationTime;  // The time at which this instance was created
    private Date terminationTime; // The time at which this instance will automatically be terminated
    
    /**
     * Creates a new StyxGridService with the given configuration
     * @todo: sort out permissions and owners on all these files
     */
    public StyxGridServiceInstance(StyxGridService sgs, String id,
        SGSConfig sgsConfig) throws StyxException
    {
        super(id);
        this.sgs = sgs;
        this.id = id;
        this.sgsConfig = sgsConfig;
        
        // Set the creation time and the termination time.  By default, the 
        // termination time is null, i.e. the instance will last forever
        this.creationTime = new Date();
        this.terminationTime = null;
        
        this.command = sgsConfig.getCommand();
        this.workDir = new File(sgsConfig.getWorkingDirectory() +
            StyxUtils.SYSTEM_FILE_SEPARATOR + id);
        
        // Create the underlying Job object
        if (sgsConfig.getType().equals("condor"))
        {
            this.job = new CondorJob(this);
        }
        else
        {
            // Default to a local job
            this.job = new LocalJob(this);
        }
        this.job.addChangeListener(this);
        this.job.setCommand(command);
        
        // Add the ctl file
        this.addChild(new ControlFile(this)); // the ctl file
        
        // Add the parameters as SGSParamFiles.
        this.paramDir = new StyxDirectory("params");
        this.paramFiles = new Vector();
        this.jsap = sgsConfig.getParamParser();
        Vector params = sgsConfig.getParams();
        for (int i = 0; i < params.size(); i++)
        {
            SGSParam param = (SGSParam)params.get(i);
            SGSParamFile paramFile = new SGSParamFile(param, this);
            this.paramFiles.add(paramFile);
            if (param.getType() == SGSParam.OUTPUT_FILE)
            {
                // For parameters that represent output files, we don't allow
                // their values to be changed: the output file is just named
                // after the parameter.  The only reason for bothering to create
                // an SGSParamFile is to make it easier to get the command-line
                // arguments - see the getArguments() method
                paramFile.setParameterValue(param.getName());
            }
            else
            {
                // We don't add parameters pertaining to output files to the namespace:
                // the name of these files in the namespace is fixed
                this.paramDir.addChild(paramFile);
            }
        }
        this.addChild(paramDir);
        
        // Add the inputs and outputs
        this.inputsDir = new SGSInputDirectory("inputs", this.workDir, this.job);
        this.outputsDir = new StyxDirectory("outputs");
        
        Vector inputs = sgsConfig.getInputs();
        for (int i = 0; i < inputs.size(); i++)
        {
            SGSInput input = (SGSInput)inputs.get(i);
            if (input.getType() == SGSInput.STREAM)
            {
                this.stdin = new SGSInputFile.StdinFile(this.job);
                this.inputsDir.addChild(this.stdin);
            }
            // Input files appear in the namespace when they are uploaded: see
            // SGSInputDirectory
        }
        
        // Now add the output files
        this.addOutputFiles();
        
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
        this.exitCodeFile = new ExitCodeFile();
        serviceDataDir.addChild(this.exitCodeFile); 
        // Add the rest of the SDEs
        for (int i = 0; i < serviceDataElements.size(); i++)
        {
            SDEConfig sde = (SDEConfig)serviceDataElements.get(i);
            // Look for the special SDEs.
            if (sde.getName().equals("status") ||
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
        
        // Add a file that, when read, reveals the arguments that will be passed
        // to Runtime.exec(). This is an AsyncStyxFile so
        // that clients can be notified asynchronously of changes to the 
        // command line if they wish
        this.argsFile = new ArgsFile();
        this.addChild(new AsyncStyxFile(this.argsFile));
        
        // Add the files that are pertinent to the lifecycle of the SGS
        this.addChild(new TimeDirectory(this));
    }
    
    /**
     * Adds the output files to the namespace
     */
    private void addOutputFiles() throws StyxException
    {
        Vector outputs = this.sgsConfig.getOutputs();
        for (int i = 0; i < outputs.size(); i++)
        {
            SGSOutput output = (SGSOutput)outputs.get(i);
            if (output.getType() == SGSOutput.STREAM)
            {
                if (output.getName().equals("stdout"))
                {
                    // Add the standard output file
                    this.outputsDir.addChild(this.job.getStdout());
                }
                else if (output.getName().equals("stderr"))
                {
                    // Add the standard error file
                    this.outputsDir.addChild(this.job.getStderr());
                }
            }
            else
            {
                // For fixed-name files we create an SGSOutputFile named after the
                // file name.  For files that get their name from a parameter,
                // we name the SGSOutputFile after the parameter name
                File file = new File(this.workDir, output.getName());
                this.outputsDir.addChild(new SGSOutputFile(file, this.job));
            }
        }
    }
    
    /**
     * @return the working directory of this instance
     */
    File getWorkingDirectory()
    {
        return this.workDir;
    }
    
    /**
     * @return the ID of this instance
     */
    String getID()
    {
        return this.id;
    }
    
    /**
     * @return the list of input files in the <code>inputs/</code> directory
     */
    StyxFile[] getInputFiles()
    {
        return this.inputsDir.getChildren();
    }
    
    /**
     * @return the list of output files in the <code>outputs/</code> directory
     */
    StyxFile[] getOutputFiles()
    {
        return this.outputsDir.getChildren();
    }
    
    /**
     * @return the names of the input files that will be consumed by this service,
     * (not including the standard input) as a comma-separated string.  Returns
     * an empty string if there are no input files
     */
    String getInputFileNames()
    {
        StringBuffer buf = new StringBuffer();
        StyxFile[] files = this.inputsDir.getChildren();
        boolean firstTime = true;
        for (int i = 0; i < files.length; i++)
        {
            if (files[i] instanceof SGSInputFile.File)
            {
                if (!firstTime)
                {
                    buf.append(", ");
                }
                buf.append(files[i].getName());
                firstTime = false;
            }
        }
        return buf.toString();
    }
    
    /**
     * Remove input files from the inputs/ directory
     */
    public void removeInputFiles(String[] filenames)
    {
        synchronized (this.inputsDir)
        {
            for (int i = 0; i < filenames.length; i++)
            {
                StyxFile child = this.inputsDir.getChild(filenames[i]);
                if (child != null)
                {
                    this.inputsDir.removeChild(child);
                }
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
            log.debug("Preparing " + inputFiles[i].getName() + "...");
            if (inputFiles[i] instanceof SGSInputFile)
            {
                log.debug(inputFiles[i].getName() + " is an SGSInputFile");
                SGSInputFile inputFile = (SGSInputFile)inputFiles[i];
                URL url = inputFile.getURL();
                log.debug("URL = " + url);
                if (inputFile instanceof SGSInputFile.File)
                {
                    log.debug(inputFiles[i].getName() + " is an SGSInputFile.File");
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
    }
    
    /**
     * Downloads data from the given InputStream and writes it to the file
     * with the given name in the working directory of this instance.
     */
    public void downloadFrom(URL url, String filename) throws StyxException
    {
        try
        {
            File targetFile = new File(this.workDir, filename);
            log.debug("Downloading from " + url + " to " + targetFile.getPath());
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
            } while (n >= 0);
            in.close();
            fout.close();
            b = null;
        }
        catch(IOException ioe)
        {
            throw new StyxException("IOException downloading from "
                + url + ": " + ioe.getMessage());
        }
    }
    
    /**
     * Gets the time at which this instance was created
     */
    Date getCreationTime()
    {
        return this.creationTime;
    }
    
    /**
     * Gets the time at which this instance will be terminated
     */
    Date getTerminationTime()
    {
        return this.terminationTime;
    }
    
    /**
     * Sets the time at which this instance will be terminated.
     * @param termTime The termination time.  This must be in the future.  This
     * can be null, which means that the instance will never be terminated
     * automatically.
     * @throws StyxException if the termination time is in the past
     */
    void setTerminationTime(Date termTime) throws StyxException
    {
        if (termTime != null)
        {
            Date now = new Date();
            if (!termTime.after(now))
            {
                throw new StyxException("Termination time must be in the future");
            }
        }
        this.sgs.scheduleTermination(this, termTime);
        this.terminationTime = termTime;
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
            // Ignore empty messages (e.g. EOFs)
            if (count == 0)
            {
                this.replyWrite(client, 0, tag);
                return;
            }
            String cmdString = StyxUtils.dataToString(data);
            // Strip the trailing newline if it exists
            if (cmdString.endsWith(StyxUtils.NEWLINE))
            {
                cmdString = cmdString.substring(0, cmdString.length() - 1);
            }            
            if (cmdString.equalsIgnoreCase("start"))
            {
                if (job.getStatusCode() == StatusCode.RUNNING)
                {
                    throw new StyxException("Service is already running");
                }
                // Check that all the parameters are valid
                SGSParamFile[] parFiles = new SGSParamFile[paramFiles.size()];
                for (int i = 0; i < paramFiles.size(); i++)
                {
                    SGSParamFile sgsPF = (SGSParamFile)paramFiles.get(i);
                    parFiles[i] = sgsPF;
                    // The checkValid() method throws a StyxException if the
                    // contents of the parameter file are not valid for some reason
                    // The checkValid() method downloads any input files that are
                    // specified by URLs in the parameters
                    sgsPF.checkValid();
                }
                
                // Check all input files have been uploaded, and download
                // any input files that have been specified by reference.
                // TODO: this will block until all the data have been downloaded.
                prepareInputFiles();
                
                // Set the input parameters and start the executable
                job.setParameters(parFiles);
                
                if (stdin == null)
                {
                    // We're not getting any data for the standard input so
                    // we can tell the job not to wait for any
                    job.stdinDataDownloaded();
                }
                else if (stdin.getURL() != null)
                {
                    // We have set a URL for the standard input.
                    job.setStdinURL(stdin.getURL());
                }
                
                job.start();
                this.replyWrite(client, count, tag);
            }
            else if (cmdString.equalsIgnoreCase("stop"))
            {
                job.stop();
                this.replyWrite(client, count, tag);
            }
            else if (cmdString.equalsIgnoreCase("destroy"))
            {
                if (job.getStatusCode() == StatusCode.RUNNING)
                {
                    throw new StyxException("Cannot destroy a running service: stop it first");
                }
                destroy();
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
     * Destroys this SGS instance
     */
    void destroy()
    {
        // Remove all the children of this directory
        this.removeAllChildren();
        // Now remove this directory
        try
        {
            this.remove();
        }
        catch (StyxException se)
        {
            // This should never happen
            log.error("Internal error: got StyxException when calling remove()" +
                " on instance root directory");
        }
        this.job.destroy();
        log.debug("**** INSTANCE " + this.getName() + " DESTROYED ****");
    }
    
    /**
     * File that, when read, reveals the argument list that will be passed
     * to the executable when the SGS instance is started.  Clients can write
     * the whole argument list to this file and hence set all the parameters
     * at once.
     */
    private class ArgsFile extends StyxFile
    {
        public ArgsFile() throws StyxException
        {
            super("args", 0666);
        }
        
        public void read(StyxFileClient client, long offset, int count, int tag)
            throws StyxException
        {
            this.processAndReplyRead(getArguments(), client, offset, count, tag);
        }
        
        /**
         * Write the arguments all in one go (not including the name of the
         * executable itself).  This will set the values of
         * all the parameters in the params/ directory. At the moment the command
         * line must be written in a single Styx message.
         */
        /*public void write(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException
        {
            if (offset != 0)
            {
                throw new StyxException("Must write to the start of the args file");
            }
            if (!truncate)
            {
                throw new StyxException("Must write to the args file with truncation");
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
            argumentsChanged();
            
            this.replyWrite(client, count, tag);
        }*/
    }
    
    /**
     * This is called when something changes to change the command line arguments
     * (e.g. a parameter value changes)
     */
    public void argumentsChanged()
    {
        this.argsFile.contentsChanged();
    }
    
    /**
     * Gets the status of the underlying Job
     */
    public StatusCode getStatus()
    {
        return this.job.getStatusCode();
    }
    
    /**
     * Gets the argument list that will be passed to the executable as a String
     */
    public String getArguments()
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < this.paramFiles.size(); i++)
        {
            // We can be pretty confident that this cast is safe
            SGSParamFile paramFile = (SGSParamFile)this.paramFiles.get(i);
            String frag = paramFile.getCommandLineFragment();
            if (!frag.trim().equals(""))
            {
                buf.append(frag + " ");
            }
        }
        return buf.toString();
    }
    
    /**
     * Called by the Job object when the status of the job changes.  This 
     * sets the new status value in the status file and hence notifies waiting
     * clients of the new status.
     */
    public void statusChanged(StatusCode statusCode, String message)
    {
        String msg = "";
        if (message != null && !message.trim().equals(""))
        {
            msg = ": " + message;
        }
        if (this.status != null)
        {
            this.status.setValue(statusCode.getText() + msg);
        }
    }
    
    /**
     * Called by the Job object when we have the exit code of the job 
     * @param exitCode The exit code of the job
     */
    public void gotExitCode(int exitCode)
    {
        this.exitCodeFile.setExitCode(exitCode);
    }
    
}

