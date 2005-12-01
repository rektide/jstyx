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

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.IOException;

import org.apache.mina.common.ByteBuffer;
import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.messages.TreadMessage;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
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
 * Revision 1.40  2005/12/01 08:34:41  jonblower
 * Deleted unnecessary code
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
 * Revision 1.16  2005/05/20 16:28:49  jonblower
 * Continuing to implement GUI app
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
    
    private CStyxFile instanceRoot; // The file at the root of the instance
    private CStyxFile ctlFile;      // The file that we use to stop, start and
                                    // destroy the instance
    
    // State data
    private CStyxFile sdeDir; // serviceData directory for the instance
    private CStyxFile[] serviceDataFiles;
    private StringBuffer[] sdeBufs; // Temporary buffers for each service data element
    private boolean readingServiceData; // True if we are already reading service data
    private Hashtable sdeValues; // Hashtable of service data values, keyed by the
                                 // names of the service data files
    private long sdeValuesVersion; // Version of this Hashtable
    private long sdeValuesVersionLastRead; // Version of this Hashtable on the last read
    
    // Input files and streams
    private CStyxFile inputStreamsDir;
    
    // Output streams
    private CStyxFile outputStreamsDir;
    private Hashtable activeStreams;
    
    // Parameters
    private CStyxFile paramsDir;
    private CStyxFile[] paramFiles;
    private StringBuffer[] paramBufs; // Temporary contents for each parameter value
    private boolean readingParameters;
    
    // Steerable parameters
    private CStyxFile steeringDir;
    private CStyxFile[] steeringFiles;
    private StringBuffer[] steeringBufs; // Temporary contents for each parameter value
    
    // Command line file: used to read command line for debugging
    private CStyxFile cmdLineFile;
    private StringBuffer cmdLineBuf; // Temporary contents for command line
    
    // SGSInstanceClientChangeListeners that are listening for changes to this SGS instance
    private Vector changeListeners;
    
    /**
     * Creates a new SGSInstanceClient for an instance that has its root in the
     * given CStyxFile
     */
    public SGSInstanceClient(CStyxFile instanceRoot)
    {
        this.instanceRoot = instanceRoot;
        this.ctlFile = this.instanceRoot.getFile("ctl");
        this.ctlFile.addChangeListener(this);
        
        // Create the directory that will hold the input files
        this.inputStreamsDir = this.instanceRoot.getFile("/inputs");
        this.inputStreamsDir.addChangeListener(this);
        
        // Create the directory that we will read to find the output streams
        this.outputStreamsDir = this.instanceRoot.getFile("/outputs");
        this.outputStreamsDir.addChangeListener(this);
        this.activeStreams = new Hashtable();
        
        // Create the directory that we will read to find the parameters
        this.paramsDir = this.instanceRoot.getFile("params");
        this.paramsDir.addChangeListener(this);
        this.readingParameters = false;
        
        // Create the directory that we will read to find the steerable parameters
        this.steeringDir = this.instanceRoot.getFile("steering");
        this.steeringDir.addChangeListener(this);
        
        // We will read this file to get the command line
        this.cmdLineFile = this.instanceRoot.getFile("commandline");
        this.cmdLineFile.addChangeListener(this);
        this.cmdLineBuf = new StringBuffer();
        
        // We will read this directory to find the service data offered by the SGS
        this.sdeDir = this.instanceRoot.getFile("serviceData");
        this.sdeDir.addChangeListener(this);
        this.sdeValues = new Hashtable();
        this.sdeValuesVersion = 0;
        this.sdeValuesVersionLastRead = 0;
        this.readingServiceData = false;
        
        this.changeListeners = new Vector();
    }
    
    /**
     * @return the CStyxFile at the root of this instance
     */
    public CStyxFile getInstanceRoot()
    {
        return this.instanceRoot;
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
     * Sends a message to start the service.
     * @throws StyxException if the service could not be started
     */
    public void startService() throws StyxException
    {
        this.ctlFile.setContents("start");
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
     * Sends a message to stop the service.
     * @throws StyxException if the service could not be stopped
     */
    public void stopService() throws StyxException
    {
        this.ctlFile.setContents("stop");
    }
    
    /**
     * Sends a message to get the names of the service data elements for this instance.
     * When the SDEs have arrived, the gotServiceDataNames() event will be fired.
     */
    public void getServiceDataNames()
    {
        // When the contents of the directory have been found, the childrenFound
        // method of this class will be called.
        this.sdeDir.getChildrenAsync();
    }
    
    /**
     * @return the directory to which input files can be written.  This method
     * will never block.
     */
    public CStyxFile getInputStreamsDir()
    {
        return this.inputStreamsDir;
    }
    
    /**
     * Sends a message to get the possible input streams for this instance.
     * This reads the contents of the "io/in" directory.  This method
     * does not block: when the available output streams have been read, the 
     * gotInputStreams() event will be fired on all registered change listeners
     */
    public void getInputStreamsAsync()
    {
        // When the contents of the directory have been found, the childrenFound
        // method of this class will be called.
        // TODO: remove the urls/ file from this array
        this.inputStreamsDir.getChildrenAsync();
    }
    
    /**
     * @return Array of CStyxFiles, one for each input stream to which data
     * can be written
     * @throws StyxException if there was an error retrieving the data
     */
    public CStyxFile[] getInputStreams() throws StyxException
    {
        CStyxFile[] children = this.inputStreamsDir.getChildren();
        // We don't include the urls/ file in this array
        // TODO: this code is a bit ugly - should use a Vector?
        CStyxFile[] newChildren = new CStyxFile[children.length - 1];
        int j = 0;
        for (int i = 0; i < children.length; i++)
        {
            if (!children[i].getName().equals(".urls"))
            {
                newChildren[j] = children[i];
                j++;
            }
        }
        return newChildren;
    }
    
    /**
     * @return Handle to the input stream with the given name.  This method
     * does not check that the input stream actually exists!  This method will
     * not block.
     */
    public CStyxFile getInputStream(String name)
    {
        return this.inputStreamsDir.getFile(name);
    }
    
    /**
     * Sends a message to get the command line that will be executed.  Note that
     * clients only need to call this once: the gotCommandLine() event on 
     * all registered change listeners will be called automatically whenever
     * the command line changes.
     */
    public void getCommandLineAsync()
    {
        this.cmdLineFile.readAsync(0);
    }
    
    /**
     * @return the command line that will be executed.
     * @throws StyxException if there was an error getting the contents
     */
    public String getCommandLine() throws StyxException
    {
        return this.cmdLineFile.getContents();
    }
    
    /**
     * Sets all the command-line arguments of the service instance at once.
     * @param args The command-line arguments (does not include the name of the
     * executable itself)
     */
    public void setCommandLineArgs(String args) throws StyxException
    {
        this.cmdLineFile.setContents(args);
    }
    
    /**
     * Sends a message to get the output streams that can be read.  This method
     * does not block: when the available output streams have been read, the 
     * gotOutputStreams() event will be fired on all registered change listeners
     */
    public void getOutputStreamsAsync()
    {
        this.outputStreamsDir.getChildrenAsync();
    }
    
    /**
     * @return Array of CStyxFiles, one for each output stream from which data
     * can be read
     * @throws StyxException if there was an error retrieving the data
     */
    public CStyxFile[] getOutputStreams() throws StyxException
    {
        return this.outputStreamsDir.getChildren();
    }
    
    /**
     * @return Array of CStyxFiles representing the service data elements of the
     * SGS.  The first time this method is called, the server will be queried -
     * this operation might block, and might throw a StyxException if an error
     * occurs.  Subsequent calls to this method do nothing.
     * @todo Merge functionality with getServiceDataNames()
     */
    public CStyxFile[] getServiceDataFiles() throws StyxException
    {
        if (this.serviceDataFiles== null)
        {
            this.serviceDataFiles = this.sdeDir.getChildren();
        }
        return this.serviceDataFiles;
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
     * be closed through the use of other functions</p>
     */
    public Hashtable getServiceDataValues() throws StyxException
    {
        // Start reading the service data.  If we have already called this,
        // this will do nothing.
        this.readAllServiceDataAsync();
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
     * Sends message(s) to read all the service data elements.  This method only
     * needs to be called once (subsequent calls will do nothing) as the service
     * data are continously read.  Each time an element of service data changes,
     * the serviceDataChanged() event will be fired on all registered change
     * listeners.
     */
    public void readAllServiceDataAsync()
    {
        if (!this.readingServiceData)
        {
            this.readingServiceData = true;
            // Get the children of the service data dir.  When the reply arrives, we will
            // fire the gotServiceDataElements() event, then read the values of the 
            // service data.
            log.debug("Sending message to get children of SDE directory");
            this.sdeDir.getChildrenAsync();
        }
    }
    
    /**
     * @return Array of CStyxFiles representing the parameters of the SGS.  The
     * first time this method is called, the server will be queried - this
     * operation might block, and might throw a StyxException if an error
     * occurs.
     */
    public CStyxFile[] getParameterFiles() throws StyxException
    {
        if (this.paramFiles == null)
        {
            this.paramFiles = this.paramsDir.getChildren();
        }
        return this.paramFiles;
    }
    
    /**
     * @return Array of CStyxFiles representing the steerable parameters of the
     * SGS.  The first time this method is called, the server will be queried - this
     * operation might block, and might throw a StyxException if an error
     * occurs.
     */
    public CStyxFile[] getSteeringFiles() throws StyxException
    {
        if (this.steeringFiles == null)
        {
            this.steeringFiles = this.steeringDir.getChildren();
        }
        return this.steeringFiles;
    }
    
    /**
     * @return Handle to the output stream with the given name.  This method
     * does not check that the output stream actually exists!  This method will
     * not block.
     */
    public CStyxFile getOutputStream(String name)
    {
        return this.outputStreamsDir.getFile(name);
    }
    
    /**
     * First sends a message to get the parameters of the SGS.  When this arrives,
     * sends messages to read the parameter values.  When we have got the list
     * of available parameters, the gotParameters() event will be fired.  When
     * the value of a parameter arrives, the gotParameterValue() event will be
     * fired.  Having called this method once, clients will continue to receive
     * updates to parameter values without making any more requests.
     */
    public void readAllParametersAsync()
    {
        if (!this.readingParameters)
        {
            this.readingParameters = true;
            // Get the children of the params dir.  When the reply arrives, we will
            // fire the gotParameters() event, then read the values of the 
            // parameters.
            this.paramsDir.getChildrenAsync();
        }
    }
    
    /**
     * First sends a message to get the steerable parameters of the SGS.  When
     * this arrives, sends messages to read the steerable parameter values.
     * When we have got the list of available parameters, the gotSteerables()
     * event will be fired.  When the value of a steerable parameter arrives, the
     * gotSteerableValue() event will be fired.  Having called this method once,
     * clients will continue to receive updates to steerable parameter values
     * without making any more requests.
     */
    public void readAllSteeringParamsAsync()
    {
        // Get the children of the params dir.  When the reply arrives, we will
        // fire the gotParameters() event, then read the values of the 
        // parameters.
        this.steeringDir.getChildrenAsync();
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
     * Called when a file has been opened.
     * @param file The file that has been opened
     * @param mode The mode with which the file was opened
     */
    public void fileOpen(CStyxFile file, int mode)
    {
        // At the moment, this is only called when we have opened a parameter
        // file (steerable or otherwise) or a service data file for reading and
        // writing. We immediately start reading from the file
        file.readAsync(0);
    }
    
    /**
     * Called when new data has been read from a file (after the Rread message
     * arrives).
     */
    public synchronized void dataArrived(CStyxFile file, TreadMessage tReadMsg,
        ByteBuffer data)
    {
        if (data.remaining() > 0)
        {
            // Calculate the offset of the next read
            long offset = tReadMsg.getOffset().asLong() + data.remaining();
            boolean fileFound = false;
            // Update the relevant buffer
            // First see if this is the command line file
            if (file == this.cmdLineFile)
            {
                this.cmdLineBuf.append(StyxUtils.dataToString(data));
                fileFound = true;
            }
            // Next see if this was a service data file
            if (this.serviceDataFiles != null)
            {
                for (int i = 0; i < this.serviceDataFiles.length && !fileFound; i++)
                {
                    if (file == this.serviceDataFiles[i])
                    {
                        this.sdeBufs[i].append(StyxUtils.dataToString(data));
                        fileFound = true;
                    }
                }
            }
            // Next see if this was a parameter file
            if (this.paramFiles != null)
            {
                for (int i = 0; i < this.paramFiles.length && !fileFound; i++)
                {
                    if (file == this.paramFiles[i])
                    {
                        this.paramBufs[i].append(StyxUtils.dataToString(data));
                        fileFound = true;
                    }
                }
            }
            // Finally see if this was a steerable parameter file
            if (this.steeringFiles != null)
            {
                for (int i = 0; i < this.steeringFiles.length && !fileFound; i++)
                {
                    if (file == this.steeringFiles[i])
                    {
                        this.steeringBufs[i].append(StyxUtils.dataToString(data));
                        fileFound = true;
                    }
                }
            }
            // Read the next chunk of data from the file, whatever it was
            file.readAsync(offset);
        }
        else
        {
            // We have zero bytes from the file (i.e. EOF)
            // If this is the command line file, a service data file or a
            // parameter file, we start reading from
            // the start of the file again: that way, clients always get updates
            boolean readAgain = false;
            boolean fileFound = false;
            if (file == this.cmdLineFile)
            {
                readAgain = true;
                this.fireGotCommandLine(this.cmdLineBuf.toString());
                this.cmdLineBuf.setLength(0);
                fileFound = true;
            }
            if (this.serviceDataFiles != null)
            {
                for (int i = 0; i < this.serviceDataFiles.length && !fileFound; i++)
                {
                    if (file == this.serviceDataFiles[i])
                    {
                        readAgain = true;
                        this.fireServiceDataChanged(file.getName(), this.sdeBufs[i].toString());
                        this.sdeBufs[i].setLength(0);
                        fileFound = true;
                    }
                }
            }
            if (this.paramFiles != null)
            {
                for (int i = 0; i < this.paramFiles.length && !fileFound; i++)
                {
                    if (file == this.paramFiles[i])
                    {
                        readAgain = true;
                        this.fireGotParameterValue(i, this.paramBufs[i].toString());
                        this.paramBufs[i].setLength(0);
                        fileFound = true;
                    }
                }
            }
            if (this.steeringFiles != null)
            {
                for (int i = 0; i < this.steeringFiles.length && !fileFound; i++)
                {
                    if (file == this.steeringFiles[i])
                    {
                        readAgain = true;
                        this.fireGotSteerableParameterValue(i, this.steeringBufs[i].toString());
                        this.steeringBufs[i].setLength(0);
                        fileFound = true;
                    }
                }
            }
            if (readAgain)
            {
                file.readAsync(0);
            }
            else
            {
                // This isn't a file that is continuously monitoried. We have
                // reached EOF and can stop reading.
                file.close();
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
                this.fireServiceStarted();
                // Start reading the service data
                for (int i = 0; i < this.serviceDataFiles.length; i++)
                {
                    this.serviceDataFiles[i].readAsync(0);
                }
            }
            else if (message.equalsIgnoreCase("stop"))
            {
                this.fireServiceAborted();
            }
        }
    }
    
    /**
     * Called when we have the list of children for a directory.
     */
    public void childrenFound(CStyxFile file, CStyxFile[] children)
    {
        if (file == this.sdeDir)
        {
            // We have just discovered the service data elements
            this.serviceDataFiles = children;
            this.sdeBufs = new StringBuffer[serviceDataFiles.length];
            this.fireGotServiceDataElements(this.serviceDataFiles);
            
            // Set up the hashtable of service data values
            for (int i = 0; i < this.serviceDataFiles.length; i++)
            {
                String sdeName = this.serviceDataFiles[i].getName();
                if (!this.sdeValues.containsKey(sdeName))
                {
                    this.sdeValues.put(sdeName, "");
                }
            }
            log.debug("Populated Hashtable");
            // Now start reading all the service data elements
            for (int i = 0; i < this.serviceDataFiles.length; i++)
            {
                this.serviceDataFiles[i].addChangeListener(this);                
                this.sdeBufs[i] = new StringBuffer();
                // We have to open the file for reading and writing. When the
                // open confirmation arrives, we will start reading from the file.
                this.serviceDataFiles[i].openAsync(StyxUtils.ORDWR | StyxUtils.OTRUNC);
            }
        }
        else if (file == this.inputStreamsDir)
        {
            // We have just discovered the input methods
            this.fireGotInputStreams(children);
        }
        else if (file == this.outputStreamsDir)
        {
            this.fireGotOutputStreams(children);
        }
        else if (file == this.paramsDir)
        {
            this.paramFiles = children;
            this.paramBufs = new StringBuffer[this.paramFiles.length];
            this.fireGotParameters(this.paramFiles);
            // Now read the values of all the parameters
            for (int i = 0; i < this.paramFiles.length; i++)
            {
                this.paramFiles[i].addChangeListener(this);
                this.paramBufs[i] = new StringBuffer();
                // We have to open the file for reading and writing. When the
                // open confirmation arrives, we will start reading from the file.
                this.paramFiles[i].openAsync(StyxUtils.ORDWR | StyxUtils.OTRUNC);
            }
        }
        else if (file == this.steeringDir)
        {
            // We have just got the list of steerable parameters
            this.steeringFiles = children;
            this.steeringBufs = new StringBuffer[this.steeringFiles.length];
            this.fireGotSteerableParameters(this.steeringFiles);
            // Now read the values of all the steerable parameters
            for (int i = 0; i < this.steeringFiles.length; i++)
            {
                this.steeringFiles[i].addChangeListener(this);
                // We have to open the file for reading and writing. When the
                // open confirmation arrives, we will start reading from the file.
                this.steeringFiles[i].openAsync(StyxUtils.ORDWR | StyxUtils.OTRUNC);
                this.steeringBufs[i] = new StringBuffer();
            }
        }
        else
        {
            System.err.println("Got children of unknown file " + file.getPath());
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
     * Fires the serviceDataChanged() event on all registered change listeners
     */
    private void fireServiceDataChanged(String sdName, String newData)
    {
        // Update the Hashtable of service data values
        synchronized(this.sdeValues)
        {
            this.sdeValues.put(sdName, newData);
            // Update the version and notify any waiting clients
            this.sdeValuesVersion++;
            log.debug("Updated service data " + sdName + ": notifying");
            this.sdeValues.notifyAll();
        }
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.serviceDataChanged(sdName, newData);
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
     * Fires the gotServiceDataElements() event on all registered change listeners
     */
    private void fireGotServiceDataElements(CStyxFile[] sdeFiles)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotServiceDataElements(sdeFiles);
            }
        }
    }
    
    /**
     * Fires the gotInputStreams() event on all registered change listeners
     * @param inputStreams Array of CStyxFiles representing all the files
     * in the "io/in" directory of the SGS instance
     */
    private void fireGotInputStreams(CStyxFile[] inputStreams)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotInputStreams(inputStreams);
            }
        }
    }
    
    /**
     * Fires the gotOutputStreams() event on all registered change listeners
     * @param inputFiles Array of CStyxFiles representing all the output streams
     * that can be read
     */
    private void fireGotOutputStreams(CStyxFile[] outputStreams)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotOutputStreams(outputStreams);
            }
        }
    }
    
    /**
     * Fires the gotParameters() event on all registered change listeners
     * @param paramFiles Array of CStyxFiles representing the parameters
     */
    private void fireGotParameters(CStyxFile[] paramFiles)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotParameters(paramFiles);
            }
        }
    }
    
    /**
     * Fires the gotParameterValue() event on all registered change listeners.
     * @param index Index of the parameter in the array of parameters previously
     * returned by the gotParameters() event
     * @param value The new value of the parameter
     */
    private void fireGotParameterValue(int index, String value)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotParameterValue(index, value);
            }
        }
    }
    
    /**
     * Fires the gotSteerableParameters() event on all registered change listeners
     * @param steeringFiles Array of CStyxFiles representing the steerable parameters
     */
    private void fireGotSteerableParameters(CStyxFile[] steeringFiles)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotSteerableParameters(steeringFiles);
            }
        }
    }
    
    /**
     * Fires the gotSteerableParameterValue() event on all registered change listeners.
     * @param index Index of the parameter in the array of parameters previously
     * returned by the gotParameters() event
     * @param value The new value of the parameter
     */
    private void fireGotSteerableParameterValue(int index, String value)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotSteerableParameterValue(index, value);
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
     * Fires the gotCommandLine() event on all registered change listeners
     */
    private void fireGotCommandLine(String newCmdLine)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceClientChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceClientChangeListener)this.changeListeners.get(i);
                listener.gotCommandLine(newCmdLine);
            }
        }
    }
    
}
