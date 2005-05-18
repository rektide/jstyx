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

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Component;

/**
 * A popup menu which appears when the user right-clicks on a node in the tree
 * in the SGSExplorer window.  Only one instance of this class is ever created.
 * Access this instance through the static <code>showContext()</code> method.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/05/18 08:03:24  jonblower
 * Implemented creation of new service instances
 *
 * Revision 1.1  2005/05/17 18:21:36  jonblower
 * Added initial pop-up menu support
 *
 */
class StyxExplorerPopupMenu extends JPopupMenu implements ActionListener
{
    
    private static final StyxExplorerPopupMenu popupMenu = new StyxExplorerPopupMenu();
    
    private JMenuItem newInstance;
    private JMenuItem refresh;
    private CStyxFileNode activeNode; // The node to which the menu refers
    
    /** Creates a new instance of StyxExplorerPopupMenu */
    private StyxExplorerPopupMenu()
    {
        this.newInstance = new JMenuItem("New instance");
        this.refresh = new JMenuItem("Refresh");
        this.add(this.newInstance);
        this.add(this.refresh);
        
        // Add this ActionListener to each component so that actionPerfomed will
        // be called when any JMenuItem is clicked
        for (int i = 0; i < this.getComponentCount(); i++)
        {
            Component comp = this.getComponent(i);
            if (comp instanceof JMenuItem)
            {
                ((JMenuItem)comp).addActionListener(this);
            }
        }
    }
    
    /**
     * Sets up the menu (i.e. shows/hides the appropriate menu items) for the
     * given node
     */
    private void setupMenu(CStyxFileNode activeNode)
    {
        this.activeNode = activeNode;
        int nodeType = this.activeNode.getType();
        // The "New instance" menu item is only visible when clicking on a 
        // service node
        this.newInstance.setVisible(nodeType == CStyxFileNode.SERVICE);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        if (source == this.newInstance)
        {
            this.activeNode.createNewInstance();
        }
        else if (source == this.refresh)
        {
            // TODO
        }
    }
    
    public static void showContext(CStyxFileNode node, Component invoker, int x, int y)
    {
        popupMenu.setupMenu(node);
        popupMenu.show(invoker, x, y);
    }
    
}
