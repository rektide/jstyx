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

package uk.ac.rdg.resc.jstyx.interloper;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.Component;

/**
 * Pop-up menu that is shown when a user right-clicks on the table in 
 * the StyxMon.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/24 09:06:26  jonblower
 * Initial import
 *
 */
public class StyxMonPopupMenu extends JPopupMenu
{
    
    private StyxMonTableModel model;
    private JMenuItem menuFilter;
    private JMenuItem menuShowAll;
    
    /** Creates a new instance of StyxMonPopUpMenu */
    public StyxMonPopupMenu(StyxMonTableModel model)
    {
        this.model = model;
        menuFilter = new JMenuItem("Filter by filename: ");
        menuShowAll = new JMenuItem("Show all messages");
        this.add(menuFilter);
        this.add(menuShowAll);
    }
    
    /**
     * Shows a popup menu that gives the user the option to filter by a
     * given filename
     */
    public void showContext(String filename, Component invoker, int x, int y)
    {
        if (this.model.isFiltered())
        {
            this.menuFilter.setVisible(false);
            this.menuShowAll.setVisible(true);
        }
        else
        {
            this.menuFilter.setText("Filter by filename: " + filename);
            this.menuFilter.setVisible(true);
            this.menuShowAll.setVisible(false);
        }
        super.show(invoker, x, y);
    }
    
}
