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

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeListener;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;

// These imports are only needed for the signatures of the (empty) methods
// in CStyxFileNode, which implements the CStyxFileChangeListener
import org.apache.mina.common.ByteBuffer;
import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

/**
 * Data model for tree view of SGS servers and services in the SGSExplorer.
 * Only allows drilling down to the level of service instances.  Hierarchy is
 * server -> service -> instance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/05/17 15:51:43  jonblower
 * Correct operation of display of tree of SGS servers, services and instances
 *
 * Revision 1.3  2005/05/17 07:52:23  jonblower
 * Further developments
 *
 * Revision 1.2  2005/05/16 16:49:22  jonblower
 * Updated to use CStyxFileNode as a general node in the model
 *
 * Revision 1.1  2005/05/16 16:17:47  jonblower
 * Initial import
 *
 */
public class SGSExplorerTreeModel extends DefaultTreeModel implements TreeExpansionListener
{
    
    /** Creates a new instance of SGSExplorerTreeModel */
    public SGSExplorerTreeModel()
    {
        super(new DefaultMutableTreeNode("/", true), true);
    }
    
    /**
     * Adds a new connection to a server to this tree model
     */
    public void addNewConnection(StyxConnection conn)
    {
        // Generate a String describing this connection briefly
        String str = conn.getRemoteHost() + ":" + conn.getRemotePort();
        String str2 = str;
        
        // Check that a connection with this name doesn't already exist
        int j = 1;
        boolean exists;
        synchronized (this.root)
        {
            do
            {
                exists = false;
                for (int i = 0; i < this.root.getChildCount(); i++)
                {
                    DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode)this.root.getChildAt(i);
                    CStyxFileNode s = (CStyxFileNode)node.getUserObject();
                    if (str2.equals(s.name))
                    {
                        exists = true;
                        break; // Stop searching through nodes
                    }
                }
                if (exists)
                {
                    // If a node with this name already exists, we must create
                    // a new string to identify this node
                    str2 = str + " [" + j + "]";
                    j++;
                }
            } while (exists);
            
            // Add a node representing the root of this connection
            int insertLocation = this.root.getChildCount();
            DefaultMutableTreeNode newNode =
                new CStyxFileNode(conn.getRootDirectory(), str2);
            // The insertNodeInto() method fires the necessary event
            this.insertNodeInto(newNode, (DefaultMutableTreeNode)this.root,
                insertLocation);
        }
    }
    
    /**
     * Required by TreeExpansionListener. Causes the node's children to be
     * loaded.
     */
    public void treeExpanded(TreeExpansionEvent event)
    {
        Object source = event.getPath().getLastPathComponent();
        if (source instanceof CStyxFileNode)
        {
            CStyxFileNode node = (CStyxFileNode)source;
            node.findChildren();
        }
    }
    
    /**
     * Required by TreeExpansionListener. Does nothing here.
     */
    public void treeCollapsed(TreeExpansionEvent event) {}
    
    /**
     * Node in the model that represents a CStyxFile.  This allows the file to
     * be represented with a different name in the GUI.
     */
    private class CStyxFileNode extends DefaultMutableTreeNode implements CStyxFileChangeListener
    {
        
        private String name; // The name as it will appear in the tree

        public CStyxFileNode(CStyxFile file, String name)
        {
            file.addChangeListener(this);
            this.setUserObject(file);
            this.name = name;
        }
        
        public CStyxFileNode(CStyxFile file)
        {
            this(file, file.getName());
        }
        
        /**
         * @returns the String that will be used to identify this node in the
         * SGSExplorer
         */
        public String toString()
        {
            return this.name;
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
                CStyxFile file = (CStyxFile)this.getUserObject();
                if (this.getPath().length == 3)
                {
                    // If this is a Service, we find the instances by looking in
                    // the "instances" directory.  In this way, the other direct
                    // descendants of the CStyxFile (the clone file, etc) are not
                    // shown in the hierarchy.
                    file = file.getFile("instances");
                    file.addChangeListener(this);
                }
                file.getChildrenAsync();
            }
        }
        
        /**
         * All nodes can have children except those representing service
         * instances, which are always at the fourth level (level 3) of the
         * hierarchy
         */
        public boolean getAllowsChildren()
        {
            return (this.getPath().length < 4);
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
                    this.add(new CStyxFileNode(files[i]));
                }
                // Let the TreeModel know that the nodes were inserted
                nodesWereInserted(this, indices);
            }
        }
        
        public void error(CStyxFile file, String message)
        {
            System.err.println("Error with " + file.getName() + ": " + message);
        }
        
        // These methods are required by the CStyxFileChangeListener interface
        public void fileOpen(CStyxFile file, int mode){}
        public void dataArrived(CStyxFile file, TreadMessage tReadMsg, ByteBuffer data){}
        public void dataWritten(CStyxFile file, TwriteMessage tWriteMsg){}
        public void statChanged(CStyxFile file, DirEntry newDirEntry){}
    }
    
}
