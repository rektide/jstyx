/*
 * StyxBrowserPopUpMenu.java
 *
 * Created on 01 December 2004, 14:46
 */

package uk.ac.rdg.resc.jstyx.client.browser;

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
 * files in the hierarchy
 * @author  jdb
 */
public class StyxBrowserPopupMenu extends JPopupMenu
{
    
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
                System.out.println("Refresh button pressed");
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
            System.out.println("Open button pressed");
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
                System.err.println("node is null");
            }
        }
    }
    
}
