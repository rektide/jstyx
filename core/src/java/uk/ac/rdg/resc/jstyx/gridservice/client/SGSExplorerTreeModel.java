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

/**
 * Data model for tree view of SGS servers and services in the SGSExplorer.
 * Only allows drilling down to the level of service instances.  Hierarchy is
 * server -> service -> instance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/05/16 16:17:47  jonblower
 * Initial import
 *
 */
class SGSExplorerTreeModel implements TreeModel
{
    private Vector treeModelListeners;
    private Vector root; // This is a Vector of StyxConnectionNodes
    
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
        // Check that a connection with this name doesn't already exist
        synchronized (this.root)
        {
            StyxConnectionNode node;
            String str2 = str;
            int j = 1;
            boolean exists;
            do
            {
                exists = false;
                for (int i = 0; i < this.root.size(); i++)
                {
                    node = (StyxConnectionNode)this.root.get(i);
                    if (str2.equals(node.toString()))
                    {
                        exists = true;
                        break;
                    }
                }
                if (exists)
                {
                    str2 = str + " [" + j + "]";
                    j++;
                }
            } while (exists);
            this.root.add(new StyxConnectionNode(conn, str2));
        }
        this.fireTreeStructureChanged();
    }
    
    public int getChildCount(Object parent)
    {
        if (parent == this.root)
        {
            return this.root.size();
        }
        else
        {
            return 0;
        }
    }
    
    public Object getChild(Object parent, int index)
    {
        synchronized(this.root)
        {
            if (index > this.root.size() - 1 || index < 0)
            {
                return null;
            }
            else
            {
                return this.root.get(index);
            }
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
        else
        {
            return -1;
        }
    }
    
    public boolean isLeaf(Object node)
    {
        if (node == this.root || node instanceof StyxConnectionNode)
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
     */
    protected void fireTreeStructureChanged()
    {
        synchronized(this.treeModelListeners)
        {
            TreeModelEvent e = new TreeModelEvent(this, new Object[]{this.root});
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
    
    private class StyxConnectionNode
    {
        private StyxConnection conn;
        private String name;

        public StyxConnectionNode(StyxConnection conn, String name)
        {
            this.conn = conn;
            this.name = name;
        }

        public String toString()
        {
            return this.name;
        }
    }
    
}


