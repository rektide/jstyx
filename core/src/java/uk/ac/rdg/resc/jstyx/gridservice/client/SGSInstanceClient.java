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

package uk.ac.rdg.resc.jstyx.gridservice.client;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Enumeration;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.apache.mina.common.ByteBuffer;
import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfig;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSParam;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSInput;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSOutput;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileOutputStream;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;
import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * A client for a Styx Grid Service Instance. The client interacts with the SGS
 * instance and fires events when data arrives or the service data change.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.51  2006/02/17 17:34:43  jonblower
 * Implemented (but didn't test) proper handling of output files
 *
 * Revision 1.50  2006/02/17 09:26:59  jonblower
 * Changes to comments
 *
 * Revision 1.49  2006/01/06 10:15:36  jonblower
 * Implemented uploadInputFilesAsync()
 *
 * Revision 1.48  2006/01/05 16:06:34  jonblower
 * SGS clients now deal with possibility that client could be created on a different server
 *
 * Revision 1.47  2005/12/20 09:50:54  jonblower
 * Continuing to implement reading of output streams
 *
 * Revision 1.46  2005/12/19 17:21:01  jonblower
 * Preparing for including automatic download of output files in this class
 *
 * Revision 1.45  2005/12/13 09:04:21  jonblower
 * Implemented correct handling of stdin
 *
 * Revision 1.44  2005/12/09 18:41:56  jonblower
 * Continuing to simplify client interface to SGS instances
 *
 * Revision 1.43  2005/12/07 17:51:32  jonblower
 * Changed "commandline" file to "args"
 *
 * Revision 1.42  2005/12/07 08:56:32  jonblower
 * Refactoring SGS client code
 *
 * Revision 1.41  2005/12/01 17:17:07  jonblower
 * Simplifying client interface to SGS instances
 *
 * Revision 1.39  2005/11/11 21:57:21  jonblower
 * Implemented passing of URLs to input files
 *
 * Revision 1.38  2005/11/10 19:49:28  jonblower
 * Renamed SGSInstanceChangeListener to SGSInstanceClientChangeListener
 *
 * Revision 1.37  2005/11/09 17:43:19  jonblower
 * Added getInputStreamsDir() and removed urls/ directory from getInputStreams()
 *
 * Revision 1.36  2005/11/07 21:05:35  jonblower
 * Added setCommandLineArgs() method
 *
 * Revision 1.35  2005/11/04 19:28:20  jonblower
 * Changed structure of input files in config file and Styx namespace
 *
 * Revision 1.34  2005/11/02 09:01:54  jonblower
 * Continuing to implement JSAP-based parameter parsing
 *
 * Revision 1.33  2005/10/18 14:08:14  jonblower
 * Removed inputfiles from namespace
 *
 * Revision 1.32  2005/10/14 18:09:40  jonblower
 * Changed getInputMethods() to getInputStreams() and added synchronous and async versions
 *
 * Revision 1.31  2005/09/19 07:41:43  jonblower
 * Added a close() method
 *
 * Revision 1.30  2005/09/11 19:28:58  jonblower
 * Added getSteeringFiles() and getOutputStream()
 *
 * Revision 1.29  2005/08/12 08:08:39  jonblower
 * Developments to support web interface
 *
 * Revision 1.28  2005/08/04 16:49:17  jonblower
 * Added and edited upload() methods in CStyxFile
 *
 * Revision 1.27  2005/08/02 08:05:08  jonblower
 * Continuing to implement steering
 *
 * Revision 1.26  2005/08/01 16:38:05  jonblower
 * Implemented simple parameter handling
 *
 * Revision 1.25  2005/07/29 16:55:49  jonblower
 * Implementing reading command line asynchronously
 *
 * Revision 1.24  2005/06/14 07:45:16  jonblower
 * Implemented setting of params and async notification of parameter changes
 *
 * Revision 1.23  2005/06/13 16:46:35  jonblower
 * Implemented setting of parameter values via the GUI
 *
 * Revision 1.22  2005/06/10 07:53:12  jonblower
 * Changed SGS namespace: removed "inurl" and subsumed functionality into "stdin"
 *
 * Revision 1.21  2005/06/07 16:44:45  jonblower
 * Fixed problem with caching stream reader on client side
 *
 * Revision 1.20  2005/05/27 17:05:06  jonblower
 * Changes to incorporate GeneralCachingStreamReader
 *
 * Revision 1.19  2005/05/26 16:52:06  jonblower
 * Implemented detection and viewing of output streams
 *
 * Revision 1.18  2005/05/25 16:59:31  jonblower
 * Added uploadInputFile()
 *
 * Revision 1.17  2005/05/23 16:48:23  jonblower
 * Overhauled CStyxFile (esp. asynchronous methods) and StyxConnection (added cache of CStyxFiles)
 *
 * Revision 1.15  2005/05/20 07:45:27  jonblower
 * Implemented getInputFiles() to find the input files required by the service
 *
 * Revision 1.14  2005/05/19 18:42:06  jonblower
 * Implementing specification of input files required by SGS
 *
 * Revision 1.13  2005/05/18 17:13:51  jonblower
 * Created SGSInstanceGUI
 *
 * Revision 1.12  2005/05/16 11:00:53  jonblower
 * Changed SGS config XML file structure: separated input and output streams and changed some tag names
 *
 * Revision 1.11  2005/05/13 16:49:34  jonblower
 * Coded dynamic detection and display of service data, also included streams in config file
 *
 * Revision 1.10  2005/05/12 16:00:28  jonblower
 * Implementing reading of service data elements
 *
 * Revision 1.9  2005/05/12 14:21:03  jonblower
 * Changed dataSent() method to dataWritten() (more accurate name)
 *
 * Revision 1.8  2005/05/12 08:00:53  jonblower
 * Added getChildrenAsync() to CStyxFile and childrenFound() to CStyxFileChangeListener
 *
 * Revision 1.7  2005/05/12 07:40:54  jonblower
 * CStyxFile.close() no longer throws a StyxException
 *
 * Revision 1.3  2005/03/18 16:45:14  jonblower
 * Released ByteBuffers after use
 *
 * Revision 1.2  2005/03/18 13:55:59  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.4  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.2  2005/02/21 18:12:29  jonblower
 * Following changes to core JStyx library
 *
 * Revision 1.1  2005/02/16 19:22:29  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public class SGSInstanceClient extends CStyxFileChangeAdapter
{
    private static final Logger log = Logger.getLogger(SGSInstanceClient.class);
    
    private SGSClient client;       // Client of the Styx Grid Service
    private SGSConfig config;       // Full configuration information
    private CStyxFile instanceRoot; // The file at the root of the instance
    private CStyxFile ctlFile;      // The file that we use to stop, start and
                                    // destroy the instance
    
    // Hashtable of StringBuffers, one for each CStyxFile that is being read
    // continuously in an asynchronous fashion (see this.readDataAsync())
    private Hashtable/*<CStyxFile, StringBuffer>*/ bufs;
    
    // State data
    private CStyxFile[] serviceDataFiles;
    private Hashtable sdeValues; // Hashtable of service data values, keyed by the
                                 // names of the service data files
    private long sdeValuesVersion; // Version of this Hashtable
    private long sdeValuesVersionLastRead; // Version of this Hashtable on the last read
    private CStyxFile statusFile;
    
    // Input files and streams
    private CStyxFile inputsDir;
    Object stdinSrc; // Source of data to stream to stdin: null if we're using System.in
    
    // The files we're going to upload to the service
    // This hashtable allows multiple files or URLs to be associated with
    // an SGSInput object
    private Hashtable/*<SGSInput, Vector<String or File>*/ filesToUpload;
    
    // The destinations for files that we shall download from the service
    private Hashtable/*<CStyxFile, PrintStream>*/ filesToDownload;
    
    // Output streams
    private CStyxFile[] outputFiles;
    private Hashtable activeStreams;
    
    // Parameters
    private CStyxFile[] paramFiles;
    
    // Steerable parameters
    private CStyxFile[] steeringFiles;
    
    // Used to read command line arguments for debugging
    private CStyxFile argsFile;
    
    // SGSInstanceClientChangeListeners that are listening for changes to this SGS instance
    private Vector changeListeners;
    
    /**
     * Creates a new SGSInstanceClient for an instance that has its root in the
     * given CStyxFile.  Note that this constructor will make <b>blocking</b>
     * reads to the SGS server and therefore should not be called from within
     * a callback method.
     * @param client The SGSClient to which this instance belongs
     * @param instanceRoot The file representing the root of this instance
     * @throws StyxException if there was an error creating the client (for 
     * example, there is no instance with the given ID or there was an error
     * reading the directory contents from the server)
     */
    public SGSInstanceClient(SGSClient client, CStyxFile instanceRoot)
        throws StyxException
    {
        this.init(client, instanceRoot);
    }
    
    /**
     * Gets an SGSInstanceClient, given the full URL to the root of the new
     * instance, e.g.
     * <code>styx://thehost.com:9092/mySGS/instances/1234567890abcde</code>
     * @throws StyxException if there was an error creating the client object
     */
    public SGSInstanceClient(String instanceURL) throws StyxException
    {
        // Check to see if the new instance is on the same server and port
        // as the current connection
        URL url = null;
        try
        {
            url = new URL(instanceURL);
            // Get a client for the server.  If a client already exists (perhaps
            // the current client) it will simply be returned
            SGSServerClient serverClient =
                SGSServerClient.getServerClient(url.getHost(), url.getPort());
            String[] pathEls = url.getPath().split("/");
            if (pathEls.length != 3)
            {
                throw new StyxException("URL format error");
            }
            SGSClient sgsClient = serverClient.getSGSClient(pathEls[0]);
            this.init(sgsClient, pathEls[2]);
        }
        catch(MalformedURLException mue)
        {
            throw new StyxException(instanceURL + " is not recognised as a valid URL");
        }
        catch(UnknownHostException uhe)
        {
            throw new StyxException("The host at address " + url.getHost() +
                " is unknown.");
        }
    }
    
    /**
     * Sets up this instance
     */
    private void init(SGSClient client, String instanceID) throws StyxException
    {
        this.init(client, client.getInstanceFile(instanceID));
    }
    
    /**
     * Sets up this instance
     */
    private void init(SGSClient client, CStyxFile instanceRoot) throws StyxException
    {
        this.client = client;
        this.instanceRoot = instanceRoot;
        this.ctlFile = this.instanceRoot.getFile("ctl");
        this.ctlFile.addChangeListener(this);
        
        // Get the directory that holds the input files
        this.inputsDir = this.instanceRoot.getFile("inputs");
        
        // Get the list of output files
        this.outputFiles = this.instanceRoot.getFile("outputs").getChildren();
        this.activeStreams = new Hashtable();
        
        // Get the files we use to set parameter values
        this.paramFiles = this.instanceRoot.getFile("params").getChildren();
        
        // Get the directory that holds the steerable parameters
        this.steeringFiles = this.instanceRoot.getFile("steering").getChildren();
        
        // We will read this file to get the command line arguments
        this.argsFile = this.instanceRoot.getFile("args");
        
        // Get the files we use to read service data
        this.serviceDataFiles = this.instanceRoot.getFile("serviceData").getChildren();
        this.sdeValues = new Hashtable();
        this.sdeValuesVersion = 0;
        this.sdeValuesVersionLastRead = 0;
        
        // Get the full configuration information from the server
        this.config = this.client.getConfig();
        
        this.bufs = new Hashtable();
        this.changeListeners = new Vector();
        this.filesToUpload = new Hashtable();
        this.filesToDownload = new Hashtable();
    }
    
    /**
     * @return the CStyxFile at the root of this instance
     */
    public CStyxFile getInstanceRoot()
    {
        return this.instanceRoot;
    }
    
    /**
     * @return the name of this Styx Grid Service (note: not the name of this
     * particular instance)
     */
    public String getName()
    {
        return this.client.getName();
    }
    
    /**
     * Sends a message to start the service. When the confirmation arrives that
     * the service has been started, the serviceStarted() event will be fired
     * on all registered change listeners
     */
    public void startServiceAsync()
    {
        this.ctlFile.writeAsync("start", 0);
    }
    
    /**
     * Starts the service.  Blocks until the service is started.
     * @throws StyxException if the service could not be started
     */
    public void startService() throws StyxException
    {
        this.ctlFile.setContents("start");
        this.uploadToStdin();
    }
    
    /**
     * Checks to see if we need to upload any data to the standard input, then
     * starts a thread to do so if necessary
     */
    private void uploadToStdin()
    {
        // Search to see if we need to upload data to stdin
        boolean gotStdin = false;
        for (Iterator it = this.getInputs().iterator(); !gotStdin && it.hasNext(); )
        {
            SGSInput input = (SGSInput)it.next();
            if (input.getName().equals("stdin"))
            {
                gotStdin = true;
            }
        }
        if (gotStdin)
        {
            if (this.stdinSrc == null)
            {
                // No input source has been set, so we read from System.in
                new StdinReader(System.in).start();
            }
            else if (this.stdinSrc instanceof File)
            {
                // We're reading from a local file
                try
                {
                    FileInputStream fin = new FileInputStream((File)this.stdinSrc);
                    new StdinReader(fin).start();
                }
                catch (FileNotFoundException fnfe)
                {
                    // Should not happen because we have already checked to see 
                    // if the file exists
                    log.error("Internal error: " + ((File)this.stdinSrc).getPath()
                        + " not found");
                }
            }
            // If neither of those conditions are true, we have already set a 
            // URL for stdin
        }
    }
    
    /**
     * Sends a message to stop the service. When the confirmation arrives that
     * the service has been stopped, the serviceAborted() event will be fired
     */
    public void stopServiceAsync()
    {
        this.ctlFile.writeAsync("stop", 0);
    }
    
    /**
     * Stops the service.  Blocks until the service is stopped.
     * @throws StyxException if the service could not be stopped
     */
    public void stopService() throws StyxException
    {
        this.ctlFile.setContents("stop");
    }
    
    /**
     * @return Array of Strings containing the names of the files in the input
     * array.
     */
    private static String[] getFileNamesAsArray(CStyxFile[] files)
    {
        String[] arr = new String[files.length];
        for (int i = 0; i < arr.length; i++)
        {
            arr[i] = files[i].getName();
        }
        return arr;
    }
    
    /**
     * @return Array of Strings, one for each element of service data that
     * can be read.
     */
    public String[] getServiceDataNames()
    {
        return getFileNamesAsArray(this.serviceDataFiles);
    }
    
    /**
     * @return Vector of SGSParam objects, one for each input parameter that
     * can be set.
     */
    public Vector getParameters()
    {
        return this.config.getParams();
    }
    
    /**
     * @return Array of Strings, one for each steerable parameter that can be set
     */
    public String[] getSteerableParameterNames()
    {
        return getFileNamesAsArray(this.steeringFiles);
    }
    
    /**
     * @return Vector of SGSInput objects, one for each input file that can be
     * uploaded
     */
    public Vector getInputs()
    {
        return this.config.getInputs();
    }
    
    /**
     * Gets the names of all the output files
     * @return Array of Strings, one for each output stream or file from which data
     * can be read.
     */
    public String[] getOutputFileNames() throws StyxException
    {
        return getFileNamesAsArray(this.outputFiles);
    }
    
    /**
     * @return the full URL to the output file with the given name
     * @throws IllegalArgumentException if there is no file with the given name
     */
    public String getOutputFileURL(String filename)
    {
        for (int i = 0; i < this.outputFiles.length; i++)
        {
            if (this.outputFiles[i].getName().equals(filename))
            {
                return this.outputFiles[i].getURL();
            }
        }
        throw new IllegalArgumentException("There is no output file called " + filename);
    }
    
    /**
     * Sends a message to get the command line arguments that will be executed.  Note that
     * clients only need to call this once: the gotArguments() event on 
     * all registered change listeners will be called automatically whenever
     * the command line changes.
     */
    public void getArgumentsAsync()
    {
        this.readDataAsync(this.argsFile, true);
    }
    
    /**
     * Gets the command line arguments that will be executed.  This method
     * blocks until the data are returned.
     * @return the command line arguments that will be executed.
     * @throws StyxException if there was an error getting the contents
     */
    public String getArguments() throws StyxException
    {
        return this.argsFile.getContents();
    }
    
    /**
     * Sends a message to get the current value of the given piece of service
     * data.  When the service data value is returned, the gotServiceDataValue()
     * event will be fired on all registered change listeners.
     * Apart from the case of <code>exitCode</code>, as soon as the server replies
     * with the new service data value, a message will be sent to read the value
     * again immediately and automatically.  Therefore you only need to call this
     * method once for each element of service data.  In the case of <code>exitCode</code>
     * there is no need to read the value more than once because the server will
     * only reply when the remote service has stopped running: the exit code will
     * never change after this reply.
     * @param sdeName the name of the service data element to read
     * @throws IllegalArgumentException if there is no element of service data
     * with the given name
     */
    public void readServiceDataValueAsync(String sdeName)
    {
        for (int i = 0; i < this.serviceDataFiles.length; i++)
        {
            if (this.serviceDataFiles[i].getName().equals(sdeName))
            {
                this.readDataAsync(this.serviceDataFiles[i], false);
                return;
            }
        }
        // If we've got this far there isn't a piece of service data with the
        // given name
        throw new IllegalArgumentException("There is no service data element called "
            + sdeName);
    }
    
    /**
     * Sends messages to get the current value of all pieces of service
     * data.  When the service data value is returned, the gotServiceDataValue()
     * event will be fired on all registered change listeners.
     * Apart from the case of <code>exitCode</code>, as soon as the server replies
     * with the new service data value, a message will be sent to read the value
     * again immediately and automatically.  Therefore you only need to call this
     * method once for each element of service data.  In the case of <code>exitCode</code>
     * there is no need to read the value more than once because the server will
     * only reply when the remote service has stopped running: the exit code will
     * never change after this reply.
     */
    public void readAllServiceDataValuesAsync()
    {
        this.readDataAsync(this.serviceDataFiles, false);
    }
    
    /**
     * Sends messages to get the current value of all parameters.
     * When the parameter value is returned, the gotParameterValue()
     * event will be fired on all registered change listeners.  You only need
     * to call this method once: every time the parameter value changes, the
     * gotParameterValue() method will be fired.
     */
    public void readAllParameterValuesAsync()
    {
        this.readDataAsync(this.paramFiles, true);
    }
    
    /**
     * Sends messages to get the current value of all steerable parameters.
     * When the parameter value is returned, the gotSteerableParameterValue()
     * event will be fired on all registered change listeners.
     */
    public void readAllSteerableParameterValuesAsync()
    {
        this.readDataAsync(this.steeringFiles, true);
    }
    
    /**
     * Sets the file or URL from which an input file will get its data.  Note
     * that many filenames or URLs can be associated with a single SGSInput
     * object: this method adds, not replaces, a new one.
     * @param inputFile SGSInput object representing the input file, as
     * read using getInputs()
     * @param filenameOrUrl If this String starts with the string "readfrom:", 
     * this will be interpreted as a URL from which the server will read 
     * the input file.  If not, this will be interpreted as the name of a local
     * file.
     * @throws IllegalStateException if there is an attempt to set more than
     * one file or URL for a fixed input file or the standard input
     * @throws FileNotFoundException if <code>filenameOrUrl</code> represents
     * the name of a file that does not exist.
     */
    public void setInputSource(SGSInput inputFile, String filenameOrUrl)
        throws FileNotFoundException
    {
        this.checkAllowMoreDataSources(inputFile);
        Vector v = (Vector)this.filesToUpload.get(inputFile);
        if (filenameOrUrl.startsWith("readfrom:"))
        {
            // This is a URL.  Just add it as a String
            v.add(filenameOrUrl);
            return;
        }
        else
        {
            // We interpret this to be a filename
            File f = new File(filenameOrUrl);
            if (!f.exists())
            {
                throw new FileNotFoundException(filenameOrUrl + " does not exist");
            }
            v.add(f);
        }
    }
    
    /**
     * Sets the local file from which an input file will get its data.  Note
     * that many filenames or URLs can be associated with a single SGSInput
     * object: this method adds, not replaces, a new one.
     * @param inputFile SGSInput object representing the input file, as
     * read using getInputs()
     * @param file The file from which this input file will get its data
     * @throws FileNotFoundException if <code>file</code> does not exist
     * @throws IllegalStateException if there is an attempt to set more than
     * one file or URL for a fixed input file or the standard input
     */
    public void setInputSource(SGSInput inputFile, File file)
        throws FileNotFoundException
    {
        this.checkAllowMoreDataSources(inputFile);
        if (!file.exists())
        {
            throw new FileNotFoundException(file.getName() + " does not exist");
        }
        Vector v = (Vector)this.filesToUpload.get(inputFile);
        v.add(file);
    }
    
    /**
     * Checks to see if we are allowed to add more data sources to the given
     * SGSInput object, throwing an IllegalStateException if we are not.
     * @param inputFile the SGSInput object
     * @throws IllegalStateException if <code>inputFile</code> is a stream
     * or a fixed input file and we have already set an input source for this
     * file. 
     */
    private void checkAllowMoreDataSources(SGSInput inputFile)
    {
        // First check to see if we have already set a data source for this SGSInput
        if (this.filesToUpload.containsKey(inputFile))
        {
            if (inputFile.getType() != SGSInput.FILE_FROM_PARAM)
            {
                throw new IllegalStateException("Cannot set more than one data source for "
                    + inputFile.getName());
            }
        }
        else
        {
            this.filesToUpload.put(inputFile, new Vector());
        }
    }
    
    /**
     * Starts reading data from the given output file and redirects the data
     * to the given PrintStream.  The same output cannot be redirected to multiple
     * destinations (currently) so calling this method multiple times on the same
     * output file name will have no effect.
     * @param outputFileName Name of the output file (as returned by
     * getOutputFileNames())
     * @param dest The PrintStream (e.g. System.out) to which the data will be
     * written.
     * @throws IllegalArgumentException if there is no output file with the
     * given name
     */
    public void redirectOutput(String outputFileName, PrintStream dest)
    {
        for (int i = 0; i < this.outputFiles.length; i++)
        {
            if (outputFileName.equals(this.outputFiles[i].getName()))
            {
                // Don't do anything if we're already downloading from this file
                if (!this.filesToDownload.containsKey(this.outputFiles[i]))
                {
                    this.filesToDownload.put(this.outputFiles[i], dest);
                    this.outputFiles[i].addChangeListener(this);
                    this.outputFiles[i].readAsync(0);
                }
                return;
            }
        }
        throw new IllegalArgumentException(outputFileName + " is not a valid output file");
    }
        
    /**
     * Starts reading data from the given output file and redirects the data
     * to the given local File.  The same output cannot be redirected to multiple
     * destinations (currently) so calling this method multiple times on the same
     * output file name will have no effect.
     * @param outputFileName Name of the output file (as returned by
     * getOutputFileNames())
     * @param file the local file to which the data will be written.
     * @throws FileNotFoundException if the local target file could not be
     * created.
     * @throws IllegalArgumentException if there is no output file with the
     * given name
     */
    public void redirectOutput(String outputFileName, File file)
        throws FileNotFoundException
    {
        this.redirectOutput(outputFileName, new PrintStream(file));
    }
    
    /**
     * Sets the value of the parameter with the given name to the given value.
     * This method blocks until the server responds with confirmation that the
     * write is successful - this is not expected to take long.
     * @param param The SGSParam object representing this parameter (as returned
     * by this.getParameters()
     * @param value The value of this parameter.
     * @throws FileNotFoundException if the parameter represents an input file
     * and the value represents a file that does not exist
     * @throws StyxException if there was an error writing to the parameter file,
     * or if the new value was invalid
     */
    public void setParameterValue(SGSParam param, String value)
        throws FileNotFoundException, StyxException
    {
        this.setParameterValue(param, new String[]{value});
    }
    
    /**
     * Sets the value of the parameter with the given name to the given value.
     * The input array is turned into a space-delimited String before being
     * written to the server.
     * This method blocks until the server responds with confirmation that the
     * write is successful - this is not expected to take long.
     * @param param The SGSParam object representing this parameter (as returned
     * by this.getParameters()
     * @param vals String array representing all the values for this parameter
     * @throws FileNotFoundException if the parameter represents an input file
     * and the value represents a file that does not exist
     * @throws StyxException if there was an error writing to the parameter file,
     * or if the new value was invalid
     */
    public void setParameterValue(SGSParam param, String[] vals)
        throws FileNotFoundException, StyxException
    {
        CStyxFile paramFile = this.getParamFile(param);
        paramFile.setContents(getParameterValue(param, vals));
    }
    
    /**
     * @return a String containing the value of the parameter exactly as it 
     * will be written to the server, taking into account whether the parameter
     * is an input file.
     * @throws FileNotFoundException if the parameter represents an input file
     * and the parameter value represents a file that does not exist
     */
    private String getParameterValue(SGSParam param, String[] vals)
        throws FileNotFoundException
    {
        StringBuffer str = new StringBuffer();
        if (vals != null && vals.length > 0)
        {
            for (int i = 0; i < vals.length; i++)
            {
                if (param.getType() == SGSParam.INPUT_FILE)
                {
                    // This parameter represents an input file
                    this.setInputSource(param.getInputFile(), vals[i]);
                    if (!vals[i].startsWith("readfrom:"))
                    {
                        // This is a file.  Replace the full path of the file
                        // with just its name
                        vals[i] = new File(vals[i]).getName();
                    }
                }
                str.append(vals[i] + " ");
            }
        }
        return str.toString();
    }
    
    /**
     * Sets the value of the parameter with the given name to the given value.
     * This method returns immediately.  In order to receive confirmation that 
     * the write has been successful, you must previously have called 
     * readAllParameterValuesAsync(): when the parameter value has been set, 
     * the gotParameterValue() method will be fired on all registered change
     * listeners.  If the write was unsuccessful, the error() event will be
     * fired on all registered change listeners.
     * @throws FileNotFoundException if the parameter represents an input file
     * and the value represents a file that does not exist
     */
    public void setParameterValueAsync(SGSParam param, String value)
        throws FileNotFoundException
    {
        CStyxFile paramFile = this.getParamFile(param);
        paramFile.writeAsync(this.getParameterValue(param, new String[]{value}), 0);
    }
    
    /**
     * @return the CStyxFile representing the given parameter, or
     * null if this file does not exist
     */
    private CStyxFile getParamFile(SGSParam param)
    {
        for (int i = 0; i < this.paramFiles.length; i++)
        {
            if (this.paramFiles[i].getName().equals(param.getName()))
            {
                return this.paramFiles[i];
            }
        }
        return null;
    }
    
    /**
     * Sets the value of the steerable parameter with the given name to the given value.
     * This method returns immediately.  In order to receive confirmation that 
     * the write has been successful, you must previously have called 
     * readAllSteerableParameterValuesAsync(): when the parameter value has been set, 
     * the gotSteerableParameterValue() method will be fired on all registered change
     * listeners.  If the write was unsuccessful, the error() event will be
     * fired on all registered change listeners.
     * @throws IllegalArgumentException if a parameter with the given name
     * does not exist.
     */
    public void setSteerableParameterValueAsync(String name, String value)
    {
        for (int i = 0; i < this.steeringFiles.length; i++)
        {
            if (this.steeringFiles[i].getName().equals(name))
            {
                // TODO this assumes (reasonably) that the new parameter value
                // can be written in a single Styx message.
                this.steeringFiles[i].writeAsync(value, 0);
                return;
            }
        }
        throw new IllegalArgumentException("Steerable parameter " + name + " does not exist");
    }
    
    /**
     * Uploads the input files to the server.  This method blocks until the
     * upload is complete.
     * @todo Add some progress information to this (e.g. a callback)
     * @todo Shouldn't have to call this explicitly: should be done when we
     * start the service.
     */
    public void uploadInputFiles() throws StyxException
    {
        // Cycle through each input file
        for (Enumeration en = this.filesToUpload.keys(); en.hasMoreElements(); )
        {
            SGSInput inputFile = (SGSInput)en.nextElement();
            // Look for all the files and URLs associated with this input file
            Vector filesAndUrls = (Vector)this.filesToUpload.get(inputFile);
            for (Iterator it = filesAndUrls.iterator(); it.hasNext(); )
            {
                Object fileOrUrl = it.next();
                if (fileOrUrl instanceof File)
                {
                    File f = (File)fileOrUrl;
                    if (inputFile.getType() == SGSInput.STREAM)
                    {
                        // Don't upload any data: wait for the service to start,
                        // then upload in a separate thread (TODO)
                        this.stdinSrc = f;
                    }
                    else
                    {
                        CStyxFile targetFile;
                        if (inputFile.getType() == SGSInput.FILE)
                        {
                            // The name of the target file is fixed
                            targetFile = this.inputsDir.getFile(inputFile.getName());
                        }
                        else
                        {
                            // This must be a file whose name is given by a parameter
                            targetFile = this.inputsDir.getFile(f.getName());
                        }
                        log.debug("Uploading " + fileOrUrl + " to " + targetFile.getPath() + "...");
                        targetFile.upload(f);
                        log.debug("Upload of " + fileOrUrl + " complete.");
                    }
                }
                else
                {
                    // This is a URL to a file.  We do not set this for an input
                    // file that is set by a parameter: this is handled separately
                    // by the server
                    if (inputFile.getType() != SGSInput.FILE_FROM_PARAM)
                    {
                        CStyxFile targetFile = this.inputsDir.getFile(inputFile.getName());
                        log.debug("Setting URL (" + fileOrUrl + ") for " + targetFile.getPath());
                        targetFile.setContents((String)fileOrUrl);
                        log.debug("URL for " + targetFile.getPath() + " set.");
                        if (inputFile.getType() == SGSInput.STREAM)
                        {
                            // Setting stdinSrc to a String is the signal that
                            // we have already set the stdin URL and there is no
                            // need to upload data from the local client
                            this.stdinSrc = (String)fileOrUrl;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Uploads the input files to the server.  This method returns immediately:
     * the XXX method will be fired on all registered change listeners when 
     * each input file is uploaded.
     */
    public void uploadInputFilesAsync()
    {
        new InputFilesUploader().nextStage();
    }
    
    private class InputFilesUploader extends MessageCallback
    {
        Enumeration en = filesToUpload.keys();
        SGSInput inputFile;
        Iterator filesAndUrls;
        
        public void nextStage()
        {
            if (filesAndUrls == null || !filesAndUrls.hasNext())
            {
                // Need to move to the next input file
                if (en.hasMoreElements())
                {
                    this.inputFile = (SGSInput)en.nextElement();
                    filesAndUrls = ((Vector)filesToUpload.get(this.inputFile)).iterator();
                }
                else
                {
                    // There are no more files to upload
                    // TODO move to the next stage
                    return;
                }
            }
            CStyxFile targetFile;
            Object fileOrUrl = filesAndUrls.next();
            if (fileOrUrl instanceof File)
            {
                File f = (File)fileOrUrl;
                if (this.inputFile.getType() == SGSInput.STREAM)
                {
                    // Don't upload any data: wait for the service to start,
                    // then upload in a separate thread (TODO)
                    stdinSrc = f;
                }
                else
                {
                    if (this.inputFile.getType() == SGSInput.FILE)
                    {
                        // The name of the target file is fixed
                        targetFile = inputsDir.getFile(this.inputFile.getName());
                    }
                    else
                    {
                        // This must be a file whose name is given by a parameter
                        targetFile = inputsDir.getFile(f.getName());
                    }
                    targetFile.uploadAsync(f, this);
                }
            }
            else
            {
                // This is a URL to a file.  We do not set this for an input
                // file that is set by a parameter: this is handled separately
                // by the server
                if (this.inputFile.getType() != SGSInput.FILE_FROM_PARAM)
                {
                    targetFile = inputsDir.getFile(this.inputFile.getName());
                    // TODO: do we now have to write EOF and close the file?
                    targetFile.writeAsync((String)fileOrUrl, 0, this);
                    if (inputFile.getType() == SGSInput.STREAM)
                    {
                        // Setting stdinSrc to a String is the signal that
                        // we have already set the stdin URL and there is no
                        // need to upload data from the local client
                        stdinSrc = (String)fileOrUrl;
                    }
                }
            }
        }
        
        public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
        {
            // TODO: notify clients that file has been uploaded
            // Just move to the next file
            this.nextStage();
        }
        
        public void error(String message, StyxMessage tMessage)
        {
        }
    }
    
    /**
     * <p>Gets the values of all service data elements as a Hashtable, in which the
     * keys are the service data element names (Strings) and the values are the
     * service data values (also Strings).  The first time this method is called
     * it will return immediately with the data.  Subsequent calls will block
     * until <b>any one</b> of the service data values change.</p>
     * <p>The procedure followed is to open each file for reading, then send
     * a message to read each file, returning the data when it arrives.  The
     * files are <b>not</b> closed between calls to this function (but they might
     * be closed through the use of other functions)</p>
     */
    public Hashtable getServiceDataValues() throws StyxException
    {
        // Start reading the service data.  If we have already called this,
        // this will do nothing.
        this.readAllServiceDataValuesAsync();
        log.debug("Getting service data");
        log.debug("Version: " + sdeValuesVersion + ", Version last read: "
            + sdeValuesVersionLastRead);
        synchronized(this.sdeValues)
        {
            // Check the version of the hashtable and if it hasn't changed, wait
            // until it does
            while (this.sdeValuesVersionLastRead >= this.sdeValuesVersion)
            {
                try
                {
                    log.debug("Waiting for update to hashtable");
                    this.sdeValues.wait();
                    log.debug("Got update to hashtable");
                    log.debug("Version: " + sdeValuesVersion + ", Version last read: "
                        + sdeValuesVersionLastRead);
                }
                catch(InterruptedException ie)
                {
                    // Do nothing
                }
            }
            // We've got out of the loop so there must have been a change to the
            // service data hashtable
            this.sdeValuesVersionLastRead = this.sdeValuesVersion;
        }
        log.debug("Returning hashtable");
        return this.sdeValues;
    }
    
    /**
     * <p>Read data in an asynchronous manner from all the files in the input array.
     * This can be used to read from parameter files, steerable parameter files,
     * service data files or the command line file.  Unpredictable results may
     * occur if other files are passed to this method.</p>
     * <p>The behaviour of this is as follows: A message is sent to all files 
     * in the input array to start reading.  When the file has been completely
     * read, the appropriate event will be fired on all registered change listeners:
     * </p>
     * <table><tbody><tr><th>File type</th><th>Event fired</th></tr>
     * <tr><td>Parameter</td><td>gotParameterValue()</td></tr>
     * <tr><td>Service data</td><td>gotServiceDataValue()</td></tr>
     * <tr><td>Steerable parameter</td><td>gotSteerableParameterValue()</td></tr>
     * <tr><td>Command line</td><td>gotCommandLine()</td></tr></tbody></table>
     * <p>The reading of the file will go on <b>indefinitely</b> (i.e. until the
     * connection is closed - TODO: should stop when service stops).  When each
     * file has been completely read, it will immediately be read again from the
     * beginning.  This method therefore only needs to be called <b>once</b>
     * for each input file.</p>
     * @param files The files from which we will read data
     * @param openForWriting if true, we will open the files for reading and
     * writing (necessary for parameter files).
     */
    private void readDataAsync(CStyxFile[] files, boolean openForWriting)
    {
        for (int i = 0; i < files.length; i++)
        {
            this.readDataAsync(files[i], openForWriting);
        }
    }
    
    /**
     * <p>Read data in an asynchronous manner from the given file.
     * This can be used to read from parameter files, steerable parameter files,
     * service data files or the command line file.  Unpredictable results may
     * occur if other files are passed to this method.</p>
     * <p>The behaviour of this is as follows: A message is sent to start
     * reading from the file.  When the file has been completely
     * read, the appropriate event will be fired on all registered change listeners:
     * </p>
     * <table><tbody><tr><th>File type</th><th>Event fired</th></tr>
     * <tr><td>Parameter</td><td>gotParameterValue()</td></tr>
     * <tr><td>Service data</td><td>gotServiceDataValue()</td></tr>
     * <tr><td>Steerable parameter</td><td>gotSteerableParameterValue()</td></tr>
     * <tr><td>Command line</td><td>gotCommandLine()</td></tr></tbody></table>
     * <p>The reading of the file will go on <b>indefinitely</b> (i.e. until the
     * connection is closed - TODO: should stop when service stops).  When each
     * file has been completely read, it will immediately be read again from the
     * beginning.  This method therefore only needs to be called <b>once</b>
     * for each input file.</p>
     * @param files The file from which we will read data
     * @param openForWriting if true, we will open the file for reading and
     * writing (necessary for parameter files).
     */
    private void readDataAsync(CStyxFile file, boolean openForWriting)
    {
        // Create a StringBuffer to hold the data from this file, then put it
        // in the Hashtable, keyed by this file
        this.bufs.put(file, new StringBuffer());
        file.addChangeListener(this);
        file.readAsync(0, openForWriting);
    }
    
    /**
     * @return The ID of the SGS instance to which this client is connected
     */
    public String getInstanceID()
    {
        return this.instanceRoot.getName();
    }
    
    /**
     * Gets a CachedStreamReader that can be used to read from the given
     * stream
     */
    public CachedStreamReader getStreamReader(CStyxFile stream)
    {
        // TODO: check that "stream" really does represent an output stream
        if (this.activeStreams.containsKey(stream))
        {
            return (CachedStreamReader)this.activeStreams.get(stream);
        }
        else
        {
            try
            {
                CachedStreamReader reader = new CachedStreamReader(stream);
                this.activeStreams.put(stream, reader);
                return reader;
            }
            catch (IOException ioe)
            {
                // TODO: do something more useful here
                ioe.printStackTrace();
                return null;
            }
        }
    }
    
    /**
     * Thread to redirect input from a given input stream to the standard
     * input of the SGS instance
     * @todo recreates code in StyxGridServiceInstance.RedirectStream: refactor
     */
    private class StdinReader extends Thread
    {
        private InputStream in;
        public StdinReader(InputStream in)
        {
            this.in = in;
        }
        public void run()
        {
            OutputStream os = null;
            try
            {
                os = new CStyxFileOutputStream(inputsDir.getFile("stdin"));
                byte[] b = new byte[1024]; // Read 1KB at a time
                int n = 0;
                do
                {
                    n = in.read(b);
                    if (n >= 0)
                    {
                        os.write(b, 0, n);
                        os.flush();
                    }
                } while (n >= 0);
            }
            catch (StyxException se)
            {
                log.error("Error opening stream to standard input");
            }
            catch (IOException ioe)
            {
                log.error("IOException when writing to standard input: "
                    + ioe.getMessage());
                if (log.isDebugEnabled())
                {
                    ioe.printStackTrace();
                }
            }
            finally
            {
                try
                {
                    if (os != null)
                    {
                        // This will write a zero-byte message to confirm EOF
                        os.close();
                    }
                }
                catch (IOException ex)
                {
                    // Ignore errors here
                }
            }
        }
    }
    
    /**
     * This is called when we have read data asynchronously using readDataAsync().
     */
    public synchronized void dataArrived(CStyxFile file, TreadMessage tReadMsg,
        ByteBuffer data)
    {
        // Get the PrintStream that belongs to this file.  This will only give
        // a PrintStream if the file is an output file
        PrintStream prtStr = (PrintStream)this.filesToDownload.get(file);
        StringBuffer strBuf = null;
        if (prtStr == null)
        {
            // This is not an output file.  Get the StringBuffer that belongs to
            // this file (which is service data, a parameter file, etc)
            strBuf = (StringBuffer)this.bufs.get(file);
            if (strBuf == null)
            {
                // This should never happen
                log.warn("Internal error: strBuf and prtStr are both null");
            }
        }
        else
        {
            if (data.remaining() > 0)
            {
                // Calculate the offset of the next read
                long offset = tReadMsg.getOffset().asLong() + data.remaining();
                if (prtStr == null)
                {
                    // This is not an output file. Add the new data to the buffer.
                    strBuf.append(StyxUtils.dataToString(data));
                }
                else
                {
                    // This is an output file.  Write the data to the stream
                    byte[] buf = new byte[data.remaining()];
                    data.get(buf);
                    prtStr.write(buf, 0, buf.length);
                }
                // Read the next chunk of data from the file, whatever it was
                file.readAsync(offset);
            }
            else
            {
                // We have zero bytes from the file (i.e. EOF), so we know we have
                // the complete data for the file.
                // We now need to know what sort of file this is
                boolean readAgain = true;
                boolean found = false;
                // If this is an output file, close the stream
                if (prtStr != null)
                {
                    found = true;
                    readAgain = false;
                    // We don't close the standard streams
                    if (prtStr != System.out && prtStr != System.err)
                    {
                        prtStr.close();
                    }
                }
                // See if this is the arguments file
                if (!found && file == this.argsFile)
                {
                    found = true;
                    this.fireGotArguments(strBuf.toString());
                }
                // Check to see if this is a service data file
                for (int i = 0; !found && i < this.serviceDataFiles.length; i++)
                {
                    if (file == this.serviceDataFiles[i])
                    {
                        found = true;
                        if (file.getName().equals("exitCode"))
                        {
                            // We don't read the exit code file again because its
                            // contents will never change once set
                            readAgain = false;
                        }
                        this.fireGotServiceDataValue(file.getName(), strBuf.toString());
                    }
                }
                // Now check to see if this is a parameter file
                for (int i = 0; !found && i < this.paramFiles.length; i++)
                {
                    if (file == this.paramFiles[i])
                    {
                        found = true;
                        this.fireGotParameterValue(file.getName(), strBuf.toString());
                    }
                }
                // Now check to see if this is a steerable parameter file
                for (int i = 0; !found && i < this.steeringFiles.length; i++)
                {
                    if (file == this.steeringFiles[i])
                    {
                        found = true;
                        this.fireGotSteerableParameterValue(file.getName(), strBuf.toString());
                    }
                }
                // Clear the buffer and start reading again from the file
                if (strBuf != null)
                {
                    strBuf.setLength(0);
                }
                if (readAgain)
                {
                    file.readAsync(0);
                }
            }
        }
    }
    
    /**
     * Required by the CStyxFileChangeListener interface. Called when confirmation
     * arrives that a message has been written to the ctl file
     */
    public void dataWritten(CStyxFile file, TwriteMessage tWriteMsg)
    {
        if (file == this.ctlFile)
        {
            // We need to find out what the original message was
            String message = StyxUtils.dataToString(tWriteMsg.getData());
            if (message.equalsIgnoreCase("start"))
            {
                // Start writing data to standard input if necessary
                this.uploadToStdin();
                this.fireServiceStarted();
            }
            else if (message.equalsIgnoreCase("stop"))
            {
                this.fireServiceAborted();
            }
        }
    }
    
    /**
     * Required by the StyxFileChangeListener interface. Called when an Rerror
     * message has arrived
     */
    public void error(CStyxFile file, String message)
    {
        this.fireError(message);
    }
    
    /**
     * Closes the underlying StyxConnection
     */
    public void close()
    {
        this.instanceRoot.getConnection().close();
    }
    
    /**
     * Adds a listener that will be notified of changes to this SGS. If the
     * listener is already registered, this will do nothing.
     */
    public void addChangeListener(SGSInstanceClientChangeListener listener)
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
    public void removeChangeListener(SGSInstanceClientChangeListener listener)
    {
        synchronized(this.changeListeners)
        {
            boolean contained = this.changeListeners.remove(listener);
        }
    }
    
    /**
     * Fires the gotServiceDataValue() event on all registered change listeners
     */
    private void fireGotServiceDataValue(String sdName, String newData)
    {
        // Update the Hashtable of service data values
        synchronized(this.sdeValues)
        {
            this.sdeValues.put(sdName, newData);
            // Update the version and notify any waiting clients
            this.sdeValuesVersion++;
            this.sdeValues.notifyAll();
        }
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotServiceDataValue(sdName, newData);
            }
        }
    }
    
    /**
     * Fires the serviceAborted() event on all registered change listeners
     */
    private void fireServiceStarted()
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.serviceStarted();
            }
        }
    }
    
    /**
     * Fires the serviceAborted() event on all registered change listeners
     */
    private void fireServiceAborted()
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.serviceAborted();
            }
        }
    }
    
    /**
     * Fires the error() event on all registered change listeners
     */
    private void fireError(String message)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.error(message);
            }
        }
    }
    
    /**
     * Fires the gotParameterValue() event on all registered change listeners.
     * @param name Name of the parameter
     * @param value The new value of the parameter
     */
    private void fireGotParameterValue(String name, String value)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotParameterValue(name, value);
            }
        }
    }
    
    /**
     * Fires the gotSteerableParameterValue() event on all registered change listeners.
     * @param name Name of the parameter
     * @param value The new value of the parameter
     */
    private void fireGotSteerableParameterValue(String name, String value)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotSteerableParameterValue(name, value);
            }
        }
    }
    
    /**
     * Fires the inputFilesUploaded() event on all registered change listeners
     */
    private void fireInputFilesUploaded()
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.inputFilesUploaded();
            }
        }
    }
    
    /**
     * Fires the gotArguments() event on all registered change listeners
     */
    private void fireGotArguments(String newArgs)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotArguments(newArgs);
            }
        }
    }
    
}
