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

package uk.ac.rdg.resc.jstyx.client.browser;

import org.apache.log4j.Logger;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;

import java.io.IOException;

import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.StyxFileInputStream;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Pop-up menu that is displayed when a user requests a pop-up on one of the 
 * files in the hierarchy. Doesn't work very well yet!
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/03/11 13:58:54  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.4.2.1  2005/03/11 08:29:52  jonblower
 * Moved to log4j logging system (from apache commons logging)
 *
 * Revision 1.4  2005/02/28 11:43:38  jonblower
 * Tidied up logging code
 *
 * Revision 1.3  2005/02/18 17:52:40  jonblower
 * Added client.browser package
 *
 */
public class StyxBrowserPopupMenu extends JPopupMenu
{
    
    private static final Logger log = Logger.getLogger(StyxBrowserPopupMenu.class);
    
    private JMenuItem openItem;
    private JMenuItem writeItem;
    private JMenuItem refreshItem;
    private JMenuItem propertiesItem;
    
    private FileNode node; // The file on which this menu will operate
    
    /** Creates a new instance of StyxBrowserPopUpMenu */
    public StyxBrowserPopupMenu()
    {
        openItem = new JMenuItem("Open");
        writeItem = new JMenuItem("Write");
        refreshItem = new JMenuItem("Refresh");
        propertiesItem = new JMenuItem("Properties");
        this.add(openItem);
        this.add(writeItem);
        this.add(refreshItem);
        this.add(propertiesItem);
        refreshItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (node != null)
                {
                    node.refresh();
                }
            }
        });
        openItem.addActionListener(new OpenFileListener());
        this.node = null;
    }
    
    /**
     * Shows the context menu for the particular CStyxFile that is passed in
     */
    public void showContext(FileNode node, Component invoker, int x, int y)
    {
        this.node = node;
        CStyxFile file = node.getFile();
        try
        {
            if (file.isDirectory())
            {
                openItem.setVisible(false);
                writeItem.setVisible(false);
            }
            else
            {
                openItem.setVisible(true);
                writeItem.setVisible(true);
            }
        }
        catch (StyxException se)
        {
            // Log the error but don't do anything
            se.printStackTrace();
        }
        super.show(invoker, x, y);
    }
    
    class OpenFileListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            // "file" should never be null but we check just in case
            if (node != null)
            {
                CStyxFile file = node.getFile();
                StyxFileInputStream is = new StyxFileInputStream(file);
                TextDisplayer display = new TextDisplayer(file.getName());
                display.setVisible(true);
                try
                {
                    display.displayData(is);
                }
                catch(IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }
            else
            {
                log.error("node is null");
            }
        }
    }
    
}
