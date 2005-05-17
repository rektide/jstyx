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


/**
 * Data model for tree view of SGS servers and services in the SGSExplorer.
 * Only allows drilling down to the level of service instances.  Hierarchy is
 * server -> service -> instance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/05/17 18:20:50  jonblower
 * Separated CStyxFileNode from SGSExplorerTreeModel
 *
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
                    if (str2.equals(s.toString()))
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
                new CStyxFileNode(this, conn.getRootDirectory(), str2);
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
            // Send message to find the children of this node.  When the reply
            // arrives, the GUI will be updated
            node.findChildren();
        }
    }
    
    /**
     * Required by TreeExpansionListener. Does nothing here.
     */
    public void treeCollapsed(TreeExpansionEvent event) {}
    
}
