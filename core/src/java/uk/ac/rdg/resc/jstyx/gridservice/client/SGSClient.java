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
import uk.ac.rdg.resc.jstyx.messages.StyxBuffer;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;
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
 * Revision 1.7  2005/07/06 17:53:43  jonblower
 * Implementing automatic update of SGS instances in SGS Explorer
 *
 * Revision 1.6  2005/05/17 15:10:40  jonblower
 * Changed structure of SGS to put instances in a directory of their own
 *
 * Revision 1.5  2005/05/11 18:24:59  jonblower
 * Implementing automatic detection of service data elements
 *
 * Revision 1.4  2005/03/22 17:44:17  jonblower
 * Changed to use CStyxFileChangeAdapter instead of Listener and removed empty methods
 *
 * Revision 1.3  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.2  2005/03/18 13:55:59  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
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
public class SGSClient extends CStyxFileChangeAdapter
{
    
    private CStyxFile sgsRoot;   // The file at the root of the SGS
    private CStyxFile cloneFile; // The file that we read to create new instances
    private CStyxFile instancesFile; // The special file (behaves like a directory)
                                 // that is read to give the instances of the
                                 // services
    private Vector instances;    // Vector of CStyxFiles representing the root of
                                 // each instance
    private boolean gettingInstances;
    private Vector changeListeners; // The objects that will be notified of changes to this SGS
    
    /**
     * Creates a new instance of SGSClient.
     * @param root The CStyxFile representing the root of the SGS
     */
    public SGSClient(CStyxFile root)
    {
        this.sgsRoot = root;
        this.cloneFile = this.sgsRoot.getFile("clone");
        this.instancesFile = this.sgsRoot.getFile(".instances");
        this.cloneFile.addChangeListener(this);
        this.instancesFile.addChangeListener(this);
        this.gettingInstances = false;
        this.changeListeners = new Vector();
        this.instances = new Vector();
    }
    
    /**
     * Requests creation of a new instance of the SGS on the server.  When the instance
     * has been created, the gotInstances() event will be fired on all registered
     * change listeners, with the new list of instances for this SGS.
     */
    public void createNewInstance()
    {
        this.cloneFile.readAsync(0);
    }
    
    /**
     * Gets an SGSInstanceClient for the given instance id. This does not create
     * a new instance.
     * @throws StyxException if the client could not be created
     */
    public SGSInstanceClient getClientForInstance(String id) throws StyxException
    {
        return new SGSInstanceClient(this.sgsRoot.getFile("instances/" + id));
    }
    
    /**
     * Sends message to get all the instances of this SGS.  When the instances
     * arrive, the gotInstances() event will be fired on all registered
     * change listeners.  This method does not block.
     */
    public void getInstances()
    {
        // If we have already called this method, do nothing; the event will
        // still be fired on all change listeners
        if (!this.gettingInstances)
        {
            this.gettingInstances = true;
            this.instancesFile.readAsync(0);
        }
    }
    
    /**
     * Required by the CStyxFileChangeListener interface
     */
    public synchronized void dataArrived(CStyxFile file, TreadMessage tReadMsg, 
        ByteBuffer data)
    {
        if (file == this.instancesFile)
        {
            // TODO: This nearly repeats code in CStyxFile.GetChildrenCallback.
            // Can we refactor this?  Problem is that in this case we don't want
            // the instances file to be closed after reading the children, or
            // we won't get the desired async behaviour.
            
            // If this is the first read from this file, clear the cache of
            // children
            if (tReadMsg.getOffset().asLong() == 0)
            {
                this.instances.clear();
            }
            
            // We have got a (possibly partial) list of instances for this SGS
            long offset = tReadMsg.getOffset().asLong() + data.remaining();
            if (data.remaining() > 0)
            {
                // Wrap data as a StyxBuffer
                StyxBuffer styxBuf = new StyxBuffer(data);
                // Get all the DirEntries from this buffer
                while(data.hasRemaining())
                {
                    DirEntry dirEntry = styxBuf.getDirEntry();
                    CStyxFile instance = this.sgsRoot.getFile("instances/" +
                        dirEntry.getFileName());
                    instance.setDirEntry(dirEntry);
                    this.instances.add(instance);
                }
                // Read from this file again
                this.instancesFile.readAsync(offset);
            }
            else
            {
                // We've read all the data from the file
                CStyxFile[] instancesRoots =
                    (CStyxFile[])this.instances.toArray(new CStyxFile[0]);
                this.fireGotInstances(instancesRoots);
                // Start reading again from the start of the file
                this.instancesFile.readAsync(0);
            }
        }
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
    private void fireGotInstances(CStyxFile[] instances)
    {
        synchronized(this.changeListeners)
        {
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                SGSChangeListener listener = (SGSChangeListener)this.changeListeners.get(i);
                listener.gotInstances(instances);
            }
        }
    }
}
