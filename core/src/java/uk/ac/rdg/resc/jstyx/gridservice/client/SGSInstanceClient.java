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
    private CStyxFile status;
    private CStyxFile bytesConsumed;
    
    // State data
    //private String inputURL = "http://www.nerc-essc.ac.uk/~jdb/bbe.txt";
    private String inputURL = "styx://localhost:7777/LICENCE";
    private StringBuffer bytesConsBuf = new StringBuffer();
    private StringBuffer statusBuf = new StringBuffer();
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
        ctlFile.addChangeListener(this);
        
        // Open the files that will give us data and service data
        // TODO: should we only open these when the service is started?
        stdout = this.instanceRoot.getFile("/io/out");
        stderr = this.instanceRoot.getFile("/io/err");
        status = this.instanceRoot.getFile("/serviceData/status");
        bytesConsumed = this.instanceRoot.getFile("/serviceData/bytesConsumed");

        // Register our interest in changes to these files
        stdout.addChangeListener(this);
        stderr.addChangeListener(this);
        status.addChangeListener(this);
        bytesConsumed.addChangeListener(this);
        
        // Start reading the service data immediately
        //status.readAsync(0);
        //bytesConsumed.readAsync(0);
        
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
     * Sends a message to start the service. When the confirmation arrives that
     * the service has been started, the dataSent() event will be fired
     */
    public void startService() throws StyxException
    {
        this.writeAsync(this.ctlFile, "start");
    }
    
    /**
     * Sends a message to stop the service. When the confirmation arrives that
     * the service has been stopped, the dataSent() event will be fired
     */
    public void stopService() throws StyxException
    {
        this.writeAsync(this.ctlFile, "stop");
    }
    
    /**
     * Writes the given message to the given file; when the reply arrives, the
     * dataSent() event will be fired. Records the message so that we can match
     * it with its reply when it arrives
     */
    private void writeAsync(CStyxFile file, String message) throws StyxException
    {
        file.writeAsync(message);
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
     * @return The number of bytes consumed so far by this service, as a String
     */
    public String getBytesConsumed()
    {
        return this.bytesConsBuf.toString();
    }
    
    /**
     * @return The status of the service
     */
    public String getStatus()
    {
        return this.statusBuf.toString();
    }
    
    /**
     * Called when new data has been read from a file (after the Rread message
     * arrives).
     */
    public synchronized void dataArrived(CStyxFile file, TreadMessage tReadMsg,
        ByteBuffer data)
    {
        try
        {
            if (data.remaining() > 0)
            {
                file.setOffset(tReadMsg.getOffset().asLong() + data.remaining());
                // Find out which file this data belongs to
                if (file == stdout)
                {
                    stdoutBuf.append(StyxUtils.dataToString(data));
                    // We don't release the data here; we do this 
                    this.fireNewStdoutData(data);
                }
                else if (file == stderr)
                {
                    stderrBuf.append(StyxUtils.dataToString(data));
                    this.fireNewStderrData(data);
                }
                else if (file == status)
                {
                    statusBuf.append(StyxUtils.dataToString(data));
                    data.release();
                }
                else if (file == bytesConsumed)
                {
                    bytesConsBuf.append(StyxUtils.dataToString(data));
                    data.release();
                }
                file.readAsync();
            }
            else
            {
                // We have reached EOF, so we don't need the data
                data.release();
                if (file == status)
                {
                    this.fireStatusChanged(statusBuf.toString());
                    statusBuf.setLength(0);
                    status.setOffset(0);
                    status.readAsync();
                }
                else if (file == bytesConsumed)
                {
                    this.fireBytesConsumedChanged(bytesConsBuf.toString());
                    bytesConsBuf.setLength(0);
                    // If this is service data, just try reading it again.
                    bytesConsumed.setOffset(0);
                    bytesConsumed.readAsync();
                }
                else
                {
                    // If we have no data, we have reached EOF and can stop reading.
                    file.close();
                }
            }
        }
        catch(StyxException se)
        {
            se.printStackTrace();
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
                status.readAsync(0);
                bytesConsumed.readAsync(0);
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
     * Fires the statusChanged() event on all registered change listeners
     */
    private void fireStatusChanged(String newStatus)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
                listener.statusChanged(newStatus);
            }
        }
    }
    
    /**
     * Fires the bytesConsumedChanged() event on all registered change listeners
     */
    private void fireBytesConsumedChanged(String bytesConsumed)
    {
        synchronized(this.changeListeners)
        {
            SGSInstanceChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (SGSInstanceChangeListener)this.changeListeners.get(i);
                listener.bytesConsumedChanged(bytesConsumed);
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
