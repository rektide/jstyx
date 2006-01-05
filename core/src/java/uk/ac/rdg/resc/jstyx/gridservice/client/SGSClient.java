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
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfig;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfigException;

/**
 * Client for a Styx Grid Service.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.15  2006/01/05 16:06:34  jonblower
 * SGS clients now deal with possibility that client could be created on a different server
 *
 * Revision 1.14  2005/12/07 17:50:48  jonblower
 * Fixed bug and comments for getConfig()
 *
 * Revision 1.13  2005/12/07 08:53:08  jonblower
 * Improved getConfig() and changed in line with change to SGSInstanceClient constructor
 *
 * Revision 1.12  2005/12/01 08:29:47  jonblower
 * Refactored XML config handling to simplify clients
 *
 * Revision 1.11  2005/11/07 21:03:22  jonblower
 * Added getConfigXML() method
 *
 * Revision 1.9  2005/09/11 18:51:52  jonblower
 * Added error() method and changed name from getInstances() to getInstancesAsync()
 *
 * Revision 1.8  2005/08/12 08:08:39  jonblower
 * Developments to support web interface
 *
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
    private String description;  // The short description of this SGS
    private SGSConfig config;    // Object describing how each instance is configured
    private Vector instances;    // Vector of CStyxFiles representing the root of
                                 // each instance
    private boolean gettingInstances;
    private Vector changeListeners; // The objects that will be notified of changes to this SGS
    
    /**
     * Creates a new instance of SGSClient.
     * @param sgsRoot The CStyxFile representing the root of the SGS
     */
    public SGSClient(CStyxFile sgsRoot)
    {
        this.sgsRoot = sgsRoot;
        this.cloneFile = this.sgsRoot.getFile("clone");
        this.instancesFile = this.sgsRoot.getFile(".instances");
        this.cloneFile.addChangeListener(this);
        this.instancesFile.addChangeListener(this);
        this.gettingInstances = false;
        this.changeListeners = new Vector();
        this.instances = new Vector();
    }
    
    /**
     * @return the name of this SGS (i.e. the name of the root CStyxFile)
     */
    public String getName()
    {
        return this.sgsRoot.getName();
    }
    
    /**
     * Gets the short description of this Styx Grid Service.  If the description
     * has not previously been set, this method will sent a message to get the
     * new description and this method will block until the reply arrives.
     * @throws StyxException if an error occurs when getting the description from
     * the server.  Will never be thrown if the description has already been set.
     */
    public String getDescription() throws StyxException
    {
        if (this.description == null)
        {
            CStyxFile descFile = this.sgsRoot.getFile("docs/description");
            this.description = descFile.getContents();
        }
        return this.description;
    }
    
    /**
     * <p>Reads the configuration file from the server so that we know how to parse
     * parameters, deal with input files etc.  Some of this information cannot be
     * gleaned simply from interpreting the namespace itself.</p>
     * <p>The first time this is called, the server will be queried for the 
     * config information.  This information will be cached so that further
     * calls to this method will not lead to the server being queried again,
     * but will return the cached object.</p>
     * @throws StyxException if there was an error reading the configuration
     * from the server
     */
    public synchronized SGSConfig getConfig() throws StyxException
    {
        try
        {
            if (this.config == null)
            {
                this.config = new SGSConfig(this.sgsRoot.getFile("config").getContents());
            }
            return this.config;
        }
        catch (SGSConfigException sce)
        {
            // This is unlikely to happen: the server should return valid
            // configuration XML
            throw new StyxException(sce.getMessage());
        }
    }
    
    /**
     * Requests creation of a new instance of the SGS on the server.  This method
     * blocks until the instance has been created.
     * @return The full URL to the root of the new instance, e.g.
     * <code>styx://thehost.com:9092/mySGS/instances/1234567890abcde</code>. 
     * Note that the new instance may be created on a different server (for
     * load balancing purposes, for example).
     */
    public String createNewInstance() throws StyxException
    {
        return this.cloneFile.getContents();
    }
    
    /**
     * Gets a file that represents the root of the SGS instance with the given
     * ID.  Makes a blocking read to the server to check to see if the instance
     * exists.
     * @return A CStyxFile representing the root of the instance
     * @throws StyxException if the instance does not exist
     */
    public CStyxFile getInstanceFile(String instanceID) throws StyxException
    {
        CStyxFile instanceFile = this.sgsRoot.getFile("instances/" + instanceID);
        if (!instanceFile.exists())
        {
            throw new StyxException("There is no instance with ID " + instanceID
                + " on this server.");
        }
        return instanceFile;
    }
    
    /**
     * Sends message to get all the instances of this SGS.  When the instances
     * arrive, the gotInstances() event will be fired on all registered
     * change listeners.  This method does not block.
     */
    public void getInstancesAsync()
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
     * This callback is called if there has been an error with one of the
     * asynchronous methods
     */
    public void error(CStyxFile file, String message)
    {
        if (file == this.cloneFile)
        {
            this.fireError("Error creating new instance: " + message);
        }
        else if (file == this.instancesFile)
        {
            this.fireError("Error reading instances: " + message);
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
     * Fires the gotInstances() event in all registered change listeners
     * @param instances The instances belonging to this SGS
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
    
    /**
     * Fires the error() event in all registered change listeners
     * @param message the error message
     */
    private void fireError(String message)
    {
        synchronized(this.changeListeners)
        {
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                SGSChangeListener listener = (SGSChangeListener)this.changeListeners.get(i);
                listener.error(message);
            }
        }
    }
}
