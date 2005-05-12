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
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeListener;
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
 * Revision 1.7  2005/05/12 07:40:54  jonblower
 * CStyxFile.close() no longer throws a StyxException
 *
 * Revision 1.6  2005/05/11 18:25:00  jonblower
 * Implementing automatic detection of service data elements
 *
 * Revision 1.5  2005/05/11 15:13:25  jonblower
 * Implementing automatic display of service data elements
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
 *
 * Revision 1.2  2005/02/21 18:12:29  jonblower
 * Following changes to core JStyx library
 *
 * Revision 1.1  2005/02/16 19:22:29  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public class SGSInstanceClient implements CStyxFileChangeListener
{
    private CStyxFile instanceRoot; // The file at the root of the instance
    private CStyxFile ctlFile;      // The file that we use to stop, start and
                                    // destroy the instance
    
    // Styx files
    private CStyxFile stdout;
    private CStyxFile stderr;
    
    // State data
    private CStyxFile[] serviceDataFiles;
    private String[] sdNames; // Names of all the service data elements
    private StringBuffer[] sdBufs; // Contents of each service data element
    
    //private String inputURL = "http://www.nerc-essc.ac.uk/~jdb/bbe.txt";
    private String inputURL = "styx://localhost:7777/LICENCE";
    
    private StringBuffer stdoutBuf = new StringBuffer();
    private StringBuffer stderrBuf = new StringBuffer();
    
    // SGSInstanceChangeListeners that are listening for changes to this SGS instance
    private Vector changeListeners;
    
    /**
     * Creates a new SGSInstanceClient for an instance that has its root in the
     * given CStyxFile
     * @throws StyxException if there was an error creating the client
     */
    public SGSInstanceClient(CStyxFile instanceRoot) throws StyxException
    {
        this.instanceRoot = instanceRoot;
        this.ctlFile = this.instanceRoot.getFile("ctl");
        ctlFile.addChangeListener(this);
        
        //new Exception().printStackTrace();
        
        // Open the files that will give us data and service data
        // TODO: should we only open these when the service is started?
        stdout = this.instanceRoot.getFile("/io/out");
        stderr = this.instanceRoot.getFile("/io/err");
        
        /*System.err.println("Refreshing instanceRoot");
        instanceRoot.refreshAsync();
        System.err.println("Refreshed instanceRoot");
        // Discover the service data elements that we can read
        System.err.println("Getting handle to serviceData directory");
        System.err.println("Finding service data elements");
        this.serviceDataFiles = sdDir.getChildren();
        System.err.println("Found " + this.serviceDataFiles.length +
            " elements of service data");*/
        CStyxFile sdDir = this.instanceRoot.getFile("/serviceData");
        sdDir.getChildrenAsync();
        
        // Register our interest in changes to these files
        stdout.addChangeListener(this);
        stderr.addChangeListener(this);
        
        this.serviceDataFiles = new CStyxFile[0];
        this.sdNames = new String[serviceDataFiles.length];
        this.sdBufs = new StringBuffer[serviceDataFiles.length];
        
        for (int i = 0; i < this.serviceDataFiles.length; i++)
        {
            this.serviceDataFiles[i].addChangeListener(this);
            this.sdNames[i] = this.serviceDataFiles[i].getName();
            this.sdBufs[i] = new StringBuffer();
        }
        
        this.changeListeners = new Vector();
    }
    
    /**
     * Creates a new SGSInstanceClient for an instance that sits on the given
     * connection, with the given service name and ID
     */
    public SGSInstanceClient(StyxConnection conn, String serviceName, int instanceID)
        throws StyxException
    {
        this(new CStyxFile(conn, serviceName + "/" + instanceID));
    }
    
    /**
     * Sends a message to start the service. When the confirmation arrives that
     * the service has been started, the dataSent() event will be fired
     */
    public void startService() throws StyxException
    {
        this.ctlFile.writeAsync("start");
    }
    
    /**
     * Sends a message to stop the service. When the confirmation arrives that
     * the service has been stopped, the dataSent() event will be fired
     */
    public void stopService() throws StyxException
    {
        this.ctlFile.writeAsync("stop");
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
        CStyxFile inUrlFile = this.instanceRoot.getFile("io/inurl");
        inUrlFile.setContents(inputURL);
        this.inputURL = inputURL;
    }
    
    /**
     * Gets the names of all the service data elements exposed by this service
     */
    public String[] getServiceDataNames()
    {
        return this.sdNames;
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
                        this.sdBufs[i].append(StyxUtils.dataToString(data));
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
                    this.fireServiceDataChanged(file.getName(), this.sdBufs[i].toString());
                    this.sdBufs[i].setLength(0);
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
    public void dataSent(CStyxFile file, TwriteMessage tWriteMsg)
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
     * Required by the StyxFileChangeListener interface. Does nothing here
     */
    public void fileOpen(CStyxFile file, int mode)
    {
        return;
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
     * Required by the StyxFileChangeListener interface. Does nothing here.
     */
    public void statChanged(CStyxFile file, DirEntry newDirEntry)
    {
        return;
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
    
}
