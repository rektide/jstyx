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

import org.apache.mina.common.ByteBuffer;

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
 * Revision 1.4  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
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
    private CStyxFile instanceRoot; // The file at the root of the instance
    private CStyxFile ctlFile;      // The file that we use to stop, start and
                                    // destroy the instance
    
    // Styx files
    private CStyxFile stdout;
    private CStyxFile stderr;
    
    // State data
    private CStyxFile sdeDir; // serviceData directory for the instance
    private CStyxFile[] serviceDataFiles;
    private String[] sdeNames; // Names of all the service data elements
    private StringBuffer[] sdeBufs; // Contents of each service data element
    
    // Input files
    private CStyxFile inputFilesDir;
    private CStyxFile[] inputFiles; // The compulsory input files
    
    // Input streams (stdin and URL to redirect to stdin)
    private CStyxFile inputDir;
    //private String inputURL = "http://www.nerc-essc.ac.uk/~jdb/bbe.txt";
    private String inputURL = "http://www.resc.rdg.ac.uk/projects.php";
    
    private StringBuffer stdoutBuf = new StringBuffer();
    private StringBuffer stderrBuf = new StringBuffer();
    
    // SGSInstanceChangeListeners that are listening for changes to this SGS instance
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
        
        // Create the directory that contains the input files
        this.inputFilesDir = this.instanceRoot.getFile("inputFiles");
        this.inputFilesDir.addChangeListener(this);
        
        // Create the directory that we will read to get the input methods
        this.inputDir = this.instanceRoot.getFile("/io/in");
        this.inputDir.addChangeListener(this);
        
        // Open the files that will give us data and service data
        this.stdout = this.instanceRoot.getFile("/io/out/stdout");
        this.stderr = this.instanceRoot.getFile("/io/out/stderr");
        // Register our interest in changes to these files
        this.stdout.addChangeListener(this);
        this.stderr.addChangeListener(this);
        
        // We will read this directory to find the service data offered by the SGS
        this.sdeDir = this.instanceRoot.getFile("/serviceData");
        this.sdeDir.addChangeListener(this);
        
        this.changeListeners = new Vector();
    }
    
    /**
     * Creates a new SGSInstanceClient for an instance that sits on the given
     * connection, with the given service name and ID
     */
    public SGSInstanceClient(StyxConnection conn, String serviceName, int instanceID)
    {
        this(new CStyxFile(conn, serviceName + "/" + instanceID));
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
     * the service has been started, the dataSent() event will be fired
     */
    public void startService()
    {
        this.ctlFile.writeAsync("start");
    }
    
    /**
     * Sends a message to stop the service. When the confirmation arrives that
     * the service has been stopped, the dataSent() event will be fired
     */
    public void stopService()
    {
        this.ctlFile.writeAsync("stop");
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
     * Sends a message to get the possible input methods for this instance.
     * This reads the contents of the "io/in" directory.
     */
    public void getInputMethods()
    {
        // When the contents of the directory have been found, the childrenFound
        // method of this class will be called.
        this.inputDir.getChildrenAsync();
    }
    
    /**
     * Sends message(s) to read all the service data elements
     */
    public void readAllServiceData()
    {
        synchronized (this.serviceDataFiles)
        {
            for (int i = 0; i < this.serviceDataFiles.length; i++)
            {
                this.serviceDataFiles[i].readAsync(0);
            }
        }
    }
    
    /**
     * @return The ID of the SGS instance to which this client is connected
     */
    public String getInstanceID()
    {
        return this.instanceRoot.getName();
    }
    
    /**
     * @return the URL from which this SGS instance will read its input data
     */
    public String getInputURL()
    {
        return this.inputURL;
    }
    
    /**
     * Sets the URL from which the service will read its input data. Note that
     * this blocks until the URL is written (this is probably OK because we don't
     * anticipate any problems with this operation).
     */
    public void setInputURL(String inputURL) throws StyxException
    {
        CStyxFile inUrlFile = this.instanceRoot.getFile("io/in/inurl");
        inUrlFile.setContents(inputURL);
        this.inputURL = inputURL;
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
            file.setOffset(tReadMsg.getOffset().asLong() + data.remaining());
            // Find out which file this data belongs to
            if (file == stdout)
            {
                stdoutBuf.append(StyxUtils.dataToString(data));
                this.fireNewStdoutData(data);
            }
            else if (file == stderr)
            {
                stderrBuf.append(StyxUtils.dataToString(data));
                this.fireNewStderrData(data);
            }
            else
            {
                // This is service data: update the relevant buffer
                for (int i = 0; i < this.serviceDataFiles.length; i++)
                {
                    if (file == this.serviceDataFiles[i])
                    {
                        this.sdeBufs[i].append(StyxUtils.dataToString(data));
                        break;
                    }
                }
            }
            file.readAsync();
        }
        else
        {
            // If this is service data, we start reading from the start of the
            // file again
            boolean isServiceData = false;
            for (int i = 0; i < this.serviceDataFiles.length; i++)
            {
                if (file == this.serviceDataFiles[i])
                {
                    isServiceData = true;
                    this.fireServiceDataChanged(file.getName(), this.sdeBufs[i].toString());
                    this.sdeBufs[i].setLength(0);
                    file.setOffset(0);
                    file.readAsync();
                }
            }
            if (!isServiceData)
            {
                // This wasn't service data. We have reached EOF and can stop reading.
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
        if (file == ctlFile)
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
                // Start reading data from the start of the files
                stdout.readAsync(0);
                stderr.readAsync(0);
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
            // We have just discoverd the service data elements
            this.serviceDataFiles = children;
            this.sdeNames = new String[serviceDataFiles.length];
            this.sdeBufs = new StringBuffer[serviceDataFiles.length];

            for (int i = 0; i < this.serviceDataFiles.length; i++)
            {
                this.serviceDataFiles[i].addChangeListener(this);
                this.sdeNames[i] = this.serviceDataFiles[i].getName();
                this.sdeBufs[i] = new StringBuffer();
            }
            this.fireGotSDNames(this.sdeNames);
        }
        else if (file == this.inputDir)
        {
            // We have just discovered the input methods
            this.fireGotInputMethods(children);
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
    
    /**
     * Fires the newStdoutData event on all registered change listeners
     */
    private void fireNewStdoutData(ByteBuffer newData)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
                listener.newStdoutData(newData);
            }
        }
    }
    
    /**
     * Fires the newStderrData event on all registered change listeners
     */
    private void fireNewStderrData(ByteBuffer newData)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
                listener.newStderrData(newData);
            }
        }
    }
    
    /**
     * Fires the bytesConsumedChanged() event on all registered change listeners
     */
    private void fireServiceDataChanged(String sdName, String newData)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
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
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
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
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
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
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
                listener.error(message);
            }
        }
    }
    
    /**
     * Fires the gotServiceDataNames() event on all registered change listeners
     */
    private void fireGotSDNames(String[] sdeNames)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
                listener.gotServiceDataNames(sdeNames);
            }
        }
    }
    
    /**
     * Fires the gotInputMethods() event on all registered change listeners
     * @param inputMethods Array of CStyxFiles representing all the files
     * in the "io/in" directory of the SGS instance
     */
    private void fireGotInputMethods(CStyxFile[] inputMethods)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
                listener.gotInputMethods(inputMethods);
            }
        }
    }
    
}
