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

import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeListener;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.StyxUtils;

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
class CStyxFileNode extends DefaultMutableTreeNode implements CStyxFileChangeListener
{

    public static final int ROOT = 0;
    public static final int SERVER = 1;
    public static final int SERVICE = 2;
    public static final int INSTANCE = 3;
    
    private CStyxFile file; // The CStyxFile at this node
    private String name; // The name as it will appear in the tree
    private SGSExplorerTreeModel dataModel;

    private CStyxFile cloneFile; // For Service nodes only, the clone file
    
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
     * arrives, the childrenFound method of this class will be called. If we
     * already have the children for this node, this will do nothing.
     */
    public void findChildren()
    {
        if (this.children == null)
        {
            if (this.getType() == SERVICE)
            {
                // If this is a Service, we find the instances by looking in
                // the "instances" directory.  In this way, the other direct
                // descendants of the CStyxFile (the clone file, etc) are not
                // shown in the hierarchy.
                CStyxFile instancesDir = this.file.getFile("instances");
                instancesDir.addChangeListener(this);
                instancesDir.getChildrenAsync();
            }
            else
            {
                file.getChildrenAsync();
            }
        }
    }

    /**
     * Called when the children have been found. Adds them to this node's
     * Vector of children
     */
    public void childrenFound(CStyxFile file, CStyxFile[] files)
    {
        int[] indices = new int[files.length];
        synchronized (this)
        {
            for (int i = 0; i < files.length; i++)
            {
                // Get the index at which this child will be added
                indices[i] = this.getChildCount();
                this.add(new CStyxFileNode(this.dataModel, files[i]));
            }
            // Let the TreeModel know that the nodes were inserted
            this.dataModel.nodesWereInserted(this, indices);
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
            if (this.cloneFile == null)
            {
                this.cloneFile = this.file.getFile("clone");
            }
            this.cloneFile.addChangeListener(this);
            // To create the new instance, we read from the beginning of the file
            // The dataArrived() method will be called when the reply arrives
            this.cloneFile.readAsync(0);
        }
        else
        {
            // TODO: log a message somewhere
            System.err.println("Can't create new instance from this type of node");
        }
    }
    
    public void dataArrived(CStyxFile theFile, TreadMessage tReadMsg, ByteBuffer data)
    {
        if (theFile == this.cloneFile)
        {
            // We have read the clone file, and have therefore created a new instance
            String instanceID = StyxUtils.dataToString(data);
            // Create a new node for this instance
            CStyxFile instanceFile = this.file.getFile("instances/" + instanceID);
            synchronized (this)
            {
                int index = this.getChildCount();
                this.add(new CStyxFileNode(this.dataModel, instanceFile));
                // Let the TreeModel know that the nodes was inserted
                this.dataModel.nodesWereInserted(this, new int[]{index});
            }
            // Close the clone file
            theFile.close();
        }
    }

    public void error(CStyxFile file, String message)
    {
        System.err.println("Error with " + file.getName() + ": " + message);
    }

    // These methods are required by the CStyxFileChangeListener interface
    public void fileOpen(CStyxFile file, int mode){}
    public void dataWritten(CStyxFile file, TwriteMessage tWriteMsg){}
    public void statChanged(CStyxFile file, DirEntry newDirEntry){}
}
