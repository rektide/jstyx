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

import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.BorderLayout;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * Panel for displaying properties of an SGS server, service or instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/05/17 18:21:57  jonblower
 * Improved detection of node type
 *
 * Revision 1.1  2005/05/17 15:50:43  jonblower
 * Initial import
 *
 */
public class PropertiesPanel extends JPanel implements TreeSelectionListener
{
    
    private JLabel label;
    
    /** Creates a new instance of PropertiesPanel */
    public PropertiesPanel()
    {
        this.setLayout(new BorderLayout());
        this.label = new JLabel("Properties");
        this.add(this.label, BorderLayout.CENTER);
    }
    
    /**
     * This is called when nodes in the SGSExplorer's tree have been selected
     * or deselected.
     */
    public void valueChanged(TreeSelectionEvent e) 
    {
        TreePath leadPath = e.getNewLeadSelectionPath();
        if (leadPath != null)
        {
            Object obj = leadPath.getLastPathComponent();
            if (!(obj instanceof CStyxFileNode))
            {
                return;
            }
            CStyxFileNode node = (CStyxFileNode)obj;
            String nodeStr;
            int nodeType = node.getType();
            if (nodeType == 0)
            {
                // Shouldn't be selectable
                nodeStr = "root";
            }
            else if (nodeType == 1)
            {
                nodeStr = "server";
            }
            else if (nodeType == 2)
            {
                nodeStr = "service";
            }
            else if (nodeType == 3)
            {
                nodeStr = "instance";
            }
            else
            {
                // Shoudn't happen
                nodeStr = "unknown";
            }
            this.label.setText(nodeStr + ": " + node.toString());
            this.repaint();
        }
    }
}
