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
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeListener;
import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Client for a Styx Grid Service.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.2  2005/02/21 18:12:09  jonblower
 * Following changes to core JStyx library
 *
 * Revision 1.1  2005/02/16 19:22:29  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public class SGSClient implements CStyxFileChangeListener
{
    
    private StyxConnection conn; // The connection on which the SGS sits
    private CStyxFile sgsRoot;   // The file at the root of the SGS
    private CStyxFile cloneFile; // The file that we read to create new instances
    private Vector changeListeners; // The objects that will be notified of changes to this SGS
    
    /**
     * Creates a new instance of SGSClient.
     * @param host The name (or IP address) of the host on which the SGS sits
     * @param port The port number that we must connect to on the remote host
     * @param user The name of the user we are connecting as
     * @param sgsName The name of the Styx Grid Service
     * @throws StyxException if a connection couldn't be established, or if
     * we could not connect to the SGS
     */
    public SGSClient(String host, int port, String user, String sgsName)
        throws StyxException
    {
        this.conn = new StyxConnection(host, port, user);
        this.conn.connectAsync();
        this.sgsRoot = conn.openFile(sgsName, StyxUtils.OREAD);
        this.cloneFile = this.sgsRoot.getFile("clone");
        this.cloneFile.addChangeListener(this);
        this.changeListeners = new Vector();
    }
    
    /**
     * Requests creation a new instance of the SGS on the server.  When the instance
     * has been created, the newInstanceCreated event will be fired on all 
     * registered SGSChangeListeners.
     * @throws StyxException if there was an error sending the request to create
     * the new instance
     */
    public void createNewInstance() throws StyxException
    {
        this.cloneFile.readAsync(0);
    }
    
    /**
     * Gets an SGSInstanceClient for the given instance id. This does not create
     * a new instance.
     */
    public SGSInstanceClient getClientForInstance(String id)
    {
        return new SGSInstanceClient(this.sgsRoot.getFile(id));
    }
    
    /**
     * Required by the CStyxFileChangeListener interface
     */
    public synchronized void dataArrived(CStyxFile file, TreadMessage tReadMsg, 
        ByteBuffer data)
    {
        if (file == this.cloneFile)
        {
            // A new instance has been created.
            String idStr = StyxUtils.dataToString(data);
            this.fireNewInstanceCreated(idStr);
        }
    }
    
    /**
     * Required by the CStyxFileChangeListener interface. Does nothing in this 
     * class (yet)
     */
    public void fileOpen(CStyxFile file, int mode)
    {
        return;
    }
    
    /**
     * Required by the CStyxFileChangeListener interface. Does nothing in this
     * class (yet)
     */
    public void error(CStyxFile file, String message)
    {
        return;
    }
    
    /**
     * Required by the CStyxFileChangeListener interface. Does nothing in this
     * class as we do not write to any files.
     */
    public void dataSent(CStyxFile file, TwriteMessage tWriteMsg)
    {
        return;
    }
    
    /**
     * Required by the StyxFileChangeListener interface. Does nothing here.
     */
    public void statChanged(CStyxFile file, DirEntry newDirEntry)
    {
        return;
    }
    
    /**
     * Adds a new SGSChangeListener to the list of registered change listeners.
     * If the given listener has already been registered, this method does nothing.
     */
    public void addChangeListener(SGSChangeListener listener)
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
     * Removes a SGSChangeListener to the list of registered change listeners.
     * If the given listener has not been registered, this method does nothing.
     */
    public void removeChangeListener(SGSChangeListener listener)
    {
        synchronized(this.changeListeners)
        {
            this.changeListeners.remove(listener);
        }
    }
    
    /**
     * Fires the newInstanceCreated() event in all registered change listeners
     * @param id the ID of the new instance that has been created
     */
    private void fireNewInstanceCreated(String id)
    {
        synchronized(this.changeListeners)
        {
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                SGSChangeListener listener = (SGSChangeListener)this.changeListeners.get(i);
                listener.newInstanceCreated(id);
            }
        }
    }
}
