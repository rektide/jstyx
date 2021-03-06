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

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Vector;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeListener;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

// These imports are only needed for the signatures of the (empty) methods
// of the CStyxFileChangeListener
import org.apache.mina.common.ByteBuffer;
import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

/**
 * Node in the model that represents a CStyxFile.  This allows the file to
 * be represented with a different name in the GUI.
 *
 * @todo How much of this functionality can be shared with the SGSClient or
 * SGSInstanceClient?
 *
 * @todo Would it be better to create subclasses of this for the different node
 * types?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.16  2006/01/05 16:06:34  jonblower
 * SGS clients now deal with possibility that client could be created on a different server
 *
 * Revision 1.15  2005/12/07 17:49:05  jonblower
 * Added getInstanceClient() method to CStyxFileNode
 *
 * Revision 1.14  2005/12/07 08:51:48  jonblower
 * Added getSGSClient() method
 *
 * Revision 1.13  2005/09/14 07:26:46  jonblower
 * Added error() method (from SGSChangeListener interface)
 *
 * Revision 1.12  2005/09/11 19:29:51  jonblower
 * Changed call to getInstances() to getInstancesAsync()
 *
 * Revision 1.11  2005/08/12 08:08:39  jonblower
 * Developments to support web interface
 *
 * Revision 1.10  2005/07/29 11:19:29  jonblower
 * Implemented automatic update of SGS instances in SGS Explorer (and logging)
 *
 * Revision 1.6  2005/06/20 17:20:48  jonblower
 * Added download() and downloadAsync() to CStyxFile
 *
 * Revision 1.5  2005/05/25 16:58:41  jonblower
 * Added fileCreated()
 *
 * Revision 1.3  2005/05/18 17:13:51  jonblower
 * Created SGSInstanceGUI
 *
 * Revision 1.2  2005/05/18 08:03:24  jonblower
 * Implemented creation of new service instances
 *
 * Revision 1.1  2005/05/17 18:20:50  jonblower
 * Separated CStyxFileNode from SGSExplorerTreeModel
 *
 */
class CStyxFileNode extends DefaultMutableTreeNode implements CStyxFileChangeListener,
    SGSChangeListener
{

    private static final Logger log = Logger.getLogger(CStyxFileNode.class);
    
    public static final int ROOT = 0;
    public static final int SERVER = 1;
    public static final int SERVICE = 2;
    public static final int INSTANCE = 3;
    
    private CStyxFile file; // The CStyxFile at this node
    private CStyxFile instances; // The CStyxFile representing the instances of this SGS
    private String name; // The name as it will appear in the tree
    private SGSExplorerTreeModel dataModel;

    private SGSClient sgsClient; // For Service nodes only, the client class
    private SGSInstanceClient instanceClient; // For Instance nodes only, the client class
    
    public CStyxFileNode(SGSExplorerTreeModel dataModel, CStyxFile file, String name)
    {
        this.dataModel = dataModel;
        this.file = file;
        this.file.addChangeListener(this);
        this.name = name;
    }

    public CStyxFileNode(SGSExplorerTreeModel dataModel, CStyxFile file)
    {
        this(dataModel, file, file.getName());
    }
    
    /**
     * @return the CStyxFile that this node represents
     */
    public CStyxFile getFile()
    {
        return this.file;
    }

    /**
     * @return the String that will be used to identify this node in the
     * SGSExplorer
     */
    public String toString()
    {
        return this.name;
    }
    
    /**
     * @return the SGSClient object associated with this node.  Only relevant
     * for SERVICE nodes (will return null otherwise)
     */
    public SGSClient getSGSClient()
    {
        return this.sgsClient;
    }
    
    /**
     * @return the type of this node (ROOT, SERVER, SERVICE or INSTANCE) as an
     * integer, corresponding to the constants defined in this class
     */
    public int getType()
    {
        // The type of the node is given by the depth in the hierarchy.  This
        // is one less than the path length to this node.
        return this.getPath().length - 1;
    }

    /**
     * All nodes can have children except those representing service
     * instances
     */
    public boolean getAllowsChildren()
    {
        return (this.getType() != INSTANCE);
    }

    /**
     * Sends a message to get all the children of this node.  When the reply
     * arrives, the childrenFound method of this class will be called.  Does 
     * nothing if the children have already been found.
     */
    public void findChildren()
    {
        if (this.children == null)
        {
            if (this.getType() == SERVICE)
            {
                // If this has already been called, this method will do nothing
                // When we have got the instances of this service, the
                // gotInstances() method will be called
                this.sgsClient.getInstancesAsync();
            }
            else
            {
                // When we have got the children of this file, the
                // childrenFound() method will be called
                this.file.getChildrenAsync();
            }
        }
    }

    /**
     * Called when the children have been found. Adds them to this node's
     * Vector of children
     */
    public void childrenFound(CStyxFile file, CStyxFile[] files)
    {
        // Remove all previous children of this file
        this.removeAllChildren();
        synchronized (this)
        {
            for (int i = 0; i < files.length; i++)
            {
                this.add(new CStyxFileNode(this.dataModel, files[i]));
            }
        }
        this.dataModel.nodeStructureChanged(this);
    }
    
    /**
     * Intercepts calls to add a child to this structure, creating SGSclient 
     * objects if the new child represents a SGS
     */
    private void add(CStyxFileNode newChild)
    {
        super.add(newChild);
        if (newChild.getType() == SERVICE)
        {
            // If this is a service, we can get a client for the SGS
            newChild.sgsClient = new SGSClient(newChild.getFile());
            newChild.sgsClient.addChangeListener(newChild);
        }
    }
    
    /** 
     * Sends a message to create a new instance (reads the clone file). This
     * method will only be called if this node represents a Service.
     */
    public void createNewInstance()
    {
        if (this.getType() == SERVICE)
        {
            // When the new instance has been created, the gotInstances() method
            // will be called
            //this.sgsClient.createNewInstanceAsync();
        }
        else
        {
            log.error("Can't create new instance from this type of node");
        }
    }
    
    /**
     * Gets an SGSInstanceClient for this node.
     * @throws IllegalStateException if this node does not represent a service
     * instance
     * @throws StyxException if there was an error creating the client
     */
    public SGSInstanceClient getInstanceClient() throws StyxException
    {
        if (this.instanceClient == null)
        {
            if (this.getType() != INSTANCE)
            {
                throw new IllegalStateException("Can only call getInstanceClient "
                    + "on a service instance");
            }
            // Get the parent of this node: this will be a SERVICE
            CStyxFileNode parent = (CStyxFileNode)this.getParent();
            this.instanceClient = new SGSInstanceClient(parent.getSGSClient(), this.file);
        }
        return this.instanceClient;
    }
    
    /**
     * Required by SGSChangeListener interface: called when this is a Service
     * and we have a new list of instances
     */
    public void gotInstances(CStyxFile[] newInstances)
    {
        if (this.children == null)
        {
            // Just add the children of this node
            this.childrenFound(null, newInstances);
        }
        else
        {
            // We must work out which of the instance(s) that we have found are
            // new, and which instance(s) have been destroyed
            synchronized (this.children)
            {
                log.debug("Got " + newInstances.length + " instances");
                // We will construct a new Vector of children.  There will be 
                // more efficient algorithms based on in-place modification of the
                // existing Vector of children (TODO), and the fact that the
                // children arrive in a predictable order
                Vector newChildren = new Vector();
                // Search through all of the new instances
                try
                {
                    for (int i = 0; i < newInstances.length; i++)
                    {
                        log.debug("Examining new instance " + newInstances[i].getName());
                        boolean instanceFound = false;
                        // Look to see if this instance already exists
                        for (int j = 0; j < this.children.size(); j++)
                        {
                            log.debug("Instance " + j + " is a "
                                + this.children.get(j).getClass());
                            CStyxFileNode existingInstanceNode =
                                (CStyxFileNode)this.children.get(j);
                            CStyxFile existingInstanceFile =
                                existingInstanceNode.getFile();
                            log.debug("Comparing new instance " +
                                newInstances[i].getName() + " with existing instance "
                                + existingInstanceFile.getName());
                            // Check to see if they are the same file.  The 
                            // isSameFile checks the Qids of the file and *might*
                            // block, but it shouldn't since the dirEntrys of the 
                            // two files should already have been set.
                            if (newInstances[i].isSameFile(existingInstanceFile))
                            {
                                log.debug("They are the same file");
                                newChildren.add(existingInstanceNode);
                                instanceFound = true;
                                break;
                            }
                            else
                            {
                                log.debug("They are not the same file");
                            }
                        }
                        if (!instanceFound)
                        {
                            log.debug("Adding new instance " + newInstances[i].getName()
                                + " to the list of children");
                            CStyxFileNode newNode = new CStyxFileNode(this.dataModel, newInstances[i]);
                            newNode.setParent(this);
                            newChildren.add(newNode);
                        }
                    }
                    this.children = newChildren;
                    this.dataModel.nodeStructureChanged(this);
                }
                catch (Exception e)
                {
                    // This shouldn't happen (isSameFile() can throw a StyxException
                    // but should not in this case: see comments above.
                    // TODO: log error properly
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Overrides method in SGSChangeListener: this is called when an error occurs
     * creating a new instance of the service or reading the instances
     */
    public void error(String message)
    {
        log.error("Error creating or getting instances: " + message);
    }

    /**
     * Overrides method in CStyxFileChangeListener
     */
    public void error(CStyxFile file, String message)
    {
        log.error("Error with " + file.getName() + ": " + message);
    }

    // These methods are required by the CStyxFileChangeListener interface
    public void dataArrived(CStyxFile theFile, TreadMessage tReadMsg, ByteBuffer data) {}
    public void fileOpen(CStyxFile file, int mode){}
    public void fileCreated(CStyxFile file, int mode){}
    public void fileRemoved(CStyxFile file){}
    public void dataWritten(CStyxFile file, TwriteMessage tWriteMsg){}
    public void statChanged(CStyxFile file, DirEntry newDirEntry){}
    public void uploadComplete(CStyxFile targetFile){}
    public void downloadComplete(CStyxFile sourceFile){}
}
