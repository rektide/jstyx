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

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import java.util.Vector;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;

/**
 * Data model for tree view of SGS servers and services in the SGSExplorer.
 * Only allows drilling down to the level of service instances.  Hierarchy is
 * server -> service -> instance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
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
class SGSExplorerTreeModel implements TreeModel
{
    private Vector treeModelListeners;
    private Vector root; // This is a Vector of CStyxFileNodes
    
    /** Creates a new instance of SGSExplorerTreeModel */
    public SGSExplorerTreeModel()
    {
        this.treeModelListeners = new Vector();
        this.root = new Vector();
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
        synchronized (this.root)
        {
            int j = 1;
            boolean exists;
            do
            {
                exists = false;
                for (int i = 0; i < this.root.size(); i++)
                {
                    CStyxFileNode node = (CStyxFileNode)this.root.get(i);
                    if (str2.equals(node.toString()))
                    {
                        exists = true;
                        break;
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
        }
        
        // Add a node representing the root of this connection
        CStyxFileNode newNode = new CStyxFileNode(conn.getRootDirectory(), str2);
        this.root.add(newNode);
        this.fireTreeStructureChanged(this.root);
        // Send message to find the children of this node
        newNode.findChildren();
    }
    
    public int getChildCount(Object parent)
    {
        if (parent == this.root)
        {
            return this.root.size();
        }
        else if (parent instanceof CStyxFileNode)
        {
            CStyxFileNode node = (CStyxFileNode)parent;
            if (node.children == null)
            {
                return 0;
            }
            else
            {
                return node.children.size();
            }
        }
        else
        {
            return 0;
        }
    }
    
    public Object getChild(Object parent, int index)
    {
        if (parent == this.root)
        {
            synchronized(this.root)
            {
                if (index >= this.root.size() || index < 0)
                {
                    return null;
                }
                else
                {
                    return this.root.get(index);
                }
            }
        }
        else if (parent instanceof CStyxFileNode)
        {
            CStyxFileNode node = (CStyxFileNode)parent;
            if (node.children == null || index >= node.children.size() || index < 0)
            {
                return null;
            }
            else
            {
                return node.children.get(index);
            }
        }
        else
        {
            return null;
        }
    }
    
    public int getIndexOfChild(Object parent, Object child)
    {
        if (parent == null || child == null)
        {
            return -1;
        }
        if (parent == this.root)
        {
            return this.root.indexOf(child);
        }
        else if (parent instanceof CStyxFileNode)
        {
            CStyxFileNode node = (CStyxFileNode)parent;
            if (node.children == null)
            {
                return -1;
            }
            else
            {
                return node.children.indexOf(child);
            }
        }
        else
        {
            return -1;
        }
    }
    
    public boolean isLeaf(Object node)
    {
        if (node == this.root || node instanceof CStyxFileNode)
        {
            return false;
        }
        else
        {
            return true;
        }
    }
    
    public Object getRoot()
    {
        return this.root;
    }
    
    /**
     * The only event raised by this model is TreeStructureChanged with the
     * root as path, i.e. the whole tree has changed.
     * @param node The node which needs to be refreshed
     */
    protected void fireTreeStructureChanged(Object node)
    {
        synchronized(this.treeModelListeners)
        {
            TreeModelEvent e = new TreeModelEvent(this, new Object[]{node});
            for (int i = 0; i < treeModelListeners.size(); i++)
            {
                ((TreeModelListener)treeModelListeners.get(i)).treeStructureChanged(e);
            }
        }
    }
    
    /**
     * Does nothing here
     */
    public void valueForPathChanged(TreePath path, Object newValue) {}
    
    public void addTreeModelListener(TreeModelListener l)
    {
        this.treeModelListeners.add(l);
    }
    
    public void removeTreeModelListener(TreeModelListener l)
    {
        this.treeModelListeners.remove(l);
    }
    
    /**
     * Node in the model that represents a CStyxFile.  This incorporates a cache
     * of child nodes and allows the file to be represented with a different name
     * in the GUI.
     */
    private class CStyxFileNode extends CStyxFileChangeAdapter
    {
        private CStyxFile file;
        private String name; // The name as it will appear in the tree
        private Vector children; // The cache of children

        public CStyxFileNode(CStyxFile file, String name)
        {
            this.file = file;
            this.name = name;
            this.file.addChangeListener(this);
            this.children = null;
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
         * arrives, the childrenFound method of this class will be called
         */
        public void findChildren()
        {
            if (this.children == null)
            {
                this.file.getChildrenAsync();
            }
        }
        
        /**
         * Called when the children have been found in the root directory
         * of the server
         */
        public void childrenFound(CStyxFile file, CStyxFile[] children)
        {
            this.children = new Vector(children.length);
            for (int i = 0; i < children.length; i++)
            {
                this.children.add(new CStyxFileNode(children[i]));
            }
            fireTreeStructureChanged(this);
        }
    }
    
}


