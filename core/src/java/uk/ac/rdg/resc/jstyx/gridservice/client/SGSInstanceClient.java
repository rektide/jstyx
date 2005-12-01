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
    
    // Hashtable of StringBuffers, one for each CStyxFile that is being read
    // continuously in an asynchronous fashion (see this.readDataAsync())
    private Hashtable/*<CStyxFile, StringBuffer>*/ bufs;
    
    // State data
    private CStyxFile sdeDir; // serviceData directory for the instance
    private CStyxFile[] serviceDataFiles;
    //private StringBuffer[] sdeBufs; // Temporary buffers for each service data element
    //private boolean readingServiceData; // True if we are already reading service data
    private Hashtable sdeValues; // Hashtable of service data values, keyed by the
                                 // names of the service data files
    private long sdeValuesVersion; // Version of this Hashtable
    private long sdeValuesVersionLastRead; // Version of this Hashtable on the last read
    
    // Input files and streams
    private CStyxFile inputsDir;
    private CStyxFile[] inputFiles;
    
    // Output streams
    private CStyxFile outputsDir;
    private CStyxFile[] outputFiles;
    private Hashtable activeStreams;
    
    // Parameters
    private CStyxFile paramsDir;
    private CStyxFile[] paramFiles;
    private boolean readingParameters;
    
    // Steerable parameters
    private CStyxFile steeringDir;
    private CStyxFile[] steeringFiles;
    
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
        
        // Get the directory that holds the input files
        this.inputsDir = this.instanceRoot.getFile("inputs");
        
        // Get the directory that holds the output files
        this.outputsDir = this.instanceRoot.getFile("outputs");
        this.activeStreams = new Hashtable();
        
        // Get the directory that holds the parameters
        this.paramsDir = this.instanceRoot.getFile("params");
        this.readingParameters = false;
        
        // Get the directory that holds the steerable parameters
        this.steeringDir = this.instanceRoot.getFile("steering");
        
        // We will read this file to get the command line
        this.cmdLineFile = this.instanceRoot.getFile("commandline");
        this.cmdLineFile.addChangeListener(this);
        this.cmdLineBuf = new StringBuffer();
        
        // We will read this directory to find the service data offered by the SGS
        this.sdeDir = this.instanceRoot.getFile("serviceData");
        this.sdeValues = new Hashtable();
        this.sdeValuesVersion = 0;
        this.sdeValuesVersionLastRead = 0;
        
        this.bufs = new Hashtable();
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
     * @return Array of CStyxFiles, one for each element of service data that
     * can be read.  The first time this method is called it will query the
     * server with a blocking call.  Subsequent calls will not block.
     * @throws StyxException if there was an error getting the service
     * data files.
     */
    public CStyxFile[] getServiceDataFiles() throws StyxException
    {
        if (this.serviceDataFiles == null)
        {
            this.serviceDataFiles = this.sdeDir.getChildren();
        }
        return this.serviceDataFiles;
    }
    
    /**
     * @return Array of CStyxFiles, one for each input stream or file to which data
     * can be written.  The first time this method is called it will request
     * the data from the server with a blocking read.  Subsequent calls will 
     * just return the data without blocking.
     * @throws StyxException if there was an error retrieving the data from the
     * server
     */
    public CStyxFile[] getInputs() throws StyxException
    {
        return this.getInputs(false);
    }
    
    /**
     * Gets the files to which input data can be written.
     * @param force Force a fresh query of the server (in case input files have
     * changed, which can happen if a parameter value has been updated).
     * @return Array of CStyxFiles, one for each input stream or file to which data
     * can be written.  If force==false, the first time this method is called it will request
     * the data from the server with a blocking read.  Subsequent calls will 
     * just return the data without blocking.
     * @throws StyxException if there was an error retrieving the data from the
     * server
     */
    public CStyxFile[] getInputs(boolean force) throws StyxException
    {
        if (this.inputFiles == null || force)
        {
            this.inputFiles = this.inputsDir.getChildren();
        }
        return this.inputFiles;
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
     * @return Array of CStyxFiles, one for each output stream or file from which data
     * can be read.  The first time this method is called it will request
     * the data from the server with a blocking read.  Subsequent calls will 
     * just return the data without blocking.
     */
    public CStyxFile[] getOutputs() throws StyxException
    {
        if (this.outputFiles == null)
        {
            this.outputFiles = this.outputsDir.getChildren();
        }
        return this.outputFiles;
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
        this.readDataAsync(this.getServiceDataFiles());
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
     */
    public void readDataAsync(CStyxFile[] files)
    {
        for (int i = 0; i < files.length; i++)
        {
            this.readDataAsync(files[i]);
        }
    }
    
    /**
     * <p>Read data in an asynchronous manner from the given file.
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
     */
    public void readDataAsync(CStyxFile file)
    {
        // Create a StringBuffer to hold the data from this file, then put it
        // in the Hashtable, keyed by this file
        this.bufs.put(file, new StringBuffer());
        // TODO: check that the input file is of the right type
        file.addChangeListener(this);
        file.readAsync(0);
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
     * This is called when we have read data asynchronously using readDataAsync().
     */
    public synchronized void dataArrived(CStyxFile file, TreadMessage tReadMsg,
        ByteBuffer data)
    {
        // Get the StringBuffer that belongs to this file
        Object objBuf = this.bufs.get(file);
        if (objBuf == null || !(objBuf instanceof StringBuffer))
        {
            // This should never happen
            log.warn("Internal error: objBuf = null or is not a StringBuffer");
        }
        else
        {
            StringBuffer strBuf = (StringBuffer)objBuf;
            if (data.remaining() > 0)
            {
                // Calculate the offset of the next read
                long offset = tReadMsg.getOffset().asLong() + data.remaining();
                // Add the new data to the buffer
                strBuf.append(StyxUtils.dataToString(data));
                // Read the next chunk of data from the file, whatever it was
                file.readAsync(offset);
            }
            else
            {
                // We have zero bytes from the file (i.e. EOF), so we know we have
                // the complete data for the file.
                // We now need to know what sort of file this is
                // TODO: this logic is possibly a bit fragile: if the namespace
                // changes we will have to change this logic
                // TODO: should use constants rather than strings here.
                String[] pathEls = file.getPath().split("/");
                String parentDirName = pathEls[pathEls.length - 2];
                if (parentDirName.equals("serviceData"))
                {
                    this.fireGotServiceDataValue(file.getName(), strBuf.toString());
                }
                else if (parentDirName.equals("params"))
                {
                    this.fireGotParameterValue(file.getName(), strBuf.toString());
                }
                else if (parentDirName.equals("steering"))
                {
                    this.fireGotSteerableParameterValue(file.getName(), strBuf.toString());
                }
                else if (file.getName().equals("commandline") && pathEls.length == 4)
                {
                    this.fireGotCommandLine(strBuf.toString());
                }
                // Clear the buffer and start reading again from the file
                strBuf.setLength(0);
                file.readAsync(0);
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
            log.debug("Updated service data " + sdName + ": notifying");
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
