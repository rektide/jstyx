/*
 * StyxFileTreeNode.java
 *
 * Created on 02 December 2004, 16:28
 */

package uk.ac.rdg.resc.jstyx.client.dotusefultest;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.StyxFileInputStream;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.client.browser.TextDisplayer;

import org.dotuseful.ui.tree.AutomatedTreeNode;

/**
 * Node representing a node in a Styx hierarchy. This will be plugged into a
 * MouseAdaptedTree so that this class can respond to mouse events on the tree
 * @author  jdb
 */
public class StyxFileTreeNode extends AutomatedTreeNode implements MouseListener
{
    
    private boolean childrenDefined; // True if the list of children of this
                                     // file has been populated
    
    /** Creates a new instance of StyxFileTreeNode */
    public StyxFileTreeNode(CStyxFile file)
    {
        super(file);
        try
        {
            if (file.isDirectory())
            {
                this.setAllowsChildren(true);
            }
            else
            {
                this.setAllowsChildren(false);
            }
        }
        catch(StyxException se)
        {
            se.printStackTrace();
            this.setAllowsChildren(false);
        }
        this.childrenDefined = false;
    }
    
    public int getChildCount()
    {
        if (!childrenDefined)
        {
            this.defineChildren();
        }
        return super.getChildCount();
    }
    
    public boolean isLeaf()
    {
        return !this.getAllowsChildren();
    }
    
    private void defineChildren()
    {
        CStyxFile file = (CStyxFile)this.getUserObject();
        try
        {
            CStyxFile[] children = file.getChildren();
            // Must set "childrenDefined" before we actually add the child nodes
            // because the add() method calls getChildCount() and we would end
            // up in an infinite loop.
            this.childrenDefined = true;
            if (children != null)
            {
                for (int i = 0; i < children.length; i++)
                {
                    this.add(new StyxFileTreeNode(children[i]));
                }
            }
        }
        catch (StyxException se)
        {
            se.printStackTrace();
        }
    }
    
    public String toString()
    {
        CStyxFile file = (CStyxFile)this.getUserObject();
        return file.getName();
    }
    
    ////////////////////////////////////
    /// From MouseListener interface ///
    ////////////////////////////////////
    
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
        {
            System.out.println("Double-clicked on " + this.toString());
        }
    }
    
    public void mousePressed(MouseEvent e)
    {
        this.maybeShowPopup(e);
    }
    
    public void mouseReleased(MouseEvent e)
    {
        this.maybeShowPopup(e);        
    }
    
    /**
     * This method does nothing
     */
    public void mouseEntered(MouseEvent e)
    {        
    }
    
    /**
     * This method does nothing
     */
    public void mouseExited(MouseEvent e)
    {        
    }
    
    private void maybeShowPopup(MouseEvent e)
    {
        if (e.isPopupTrigger())
        {
            System.out.println("Popup triggered for " + this.toString());
            //CStyxFile file = (CStyxFile)getUserObject();
            //new StyxBrowserPopupMenu(file).show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /**
     * A pop-up menu that will appear when the user right-clicks on a node
     */
    private class StyxBrowserPopupMenu extends JPopupMenu
    {
        private CStyxFile theFile;

        /** Creates a new instance of StyxBrowserPopUpMenu */
        public StyxBrowserPopupMenu(CStyxFile file)
        {
            super(file.getName());
            this.theFile = file;
            
            try
            {
                if (!file.isDirectory())
                {
                    JMenuItem openItem = this.add(new JMenuItem("Open"));
                    openItem.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            System.out.println("Open button pressed");
                            StyxFileInputStream is = new StyxFileInputStream(theFile);
                            TextDisplayer display = new TextDisplayer(theFile.getPath());
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
                    });
                }
            }
            catch(StyxException se)
            {
                se.printStackTrace();
            }
                   
            JMenuItem refreshItem = this.add(new JMenuItem("Refresh"));
            refreshItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    System.out.println("Refresh button pressed");
                    try
                    {
                        theFile.refresh();
                        // This is rather a brute-force way of updating the children
                        // of a node
                        // TODO: force another read on the directory, create new
                        // nodes where necessary, delete those that no longer exist
                        // and update the DirEntries of all children
                        if (!isLeaf())
                        {
                            removeAllChildren();
                            childrenDefined = false;
                        }                        
                    }
                    catch(StyxException se)
                    {
                        se.printStackTrace();
                    }
                }
            });
        }
    }
    
}
