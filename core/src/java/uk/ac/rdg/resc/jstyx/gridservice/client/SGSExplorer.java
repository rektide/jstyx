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

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;

import java.awt.BorderLayout;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.Vector;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.StyxConnectionListener;

/**
 * GUI application for browsing and using Styx Grid Services
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.7  2005/05/26 16:51:40  jonblower
 * Minor change to dialog box contents
 *
 * Revision 1.6  2005/05/18 17:13:51  jonblower
 * Created SGSInstanceGUI
 *
 * Revision 1.5  2005/05/18 08:03:24  jonblower
 * Implemented creation of new service instances
 *
 * Revision 1.4  2005/05/17 18:21:36  jonblower
 * Added initial pop-up menu support
 *
 * Revision 1.3  2005/05/17 15:51:43  jonblower
 * Correct operation of display of tree of SGS servers, services and instances
 *
 * Revision 1.2  2005/05/17 07:52:23  jonblower
 * Further developments
 *
 * Revision 1.1  2005/05/16 16:17:47  jonblower
 * Initial import
 *
 */
public class SGSExplorer extends JFrame implements StyxConnectionListener
{
    
    private JLabel statusBar;
    private Vector openConnections;
    private JTree tree;
    private PropertiesPanel propsPanel; // Properties of the selected node in
                                        // the tree will appear on this panel
    private SGSExplorerTreeModel treeModel;
    
    /** Creates a new instance of SGSExplorer */
    public SGSExplorer()
    {
        super("SGSExplorer");
        this.init();
    }
    
    private void init()
    {
        this.setLayout(new BorderLayout());
        this.setSize(200, 500);
        this.createMenus();
        this.createStatusBar();
        this.setupTree();
        
        // Add listener to shut system down cleanly when window is closed
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                shutdown();
            }
        });
        
        this.openConnections = new Vector();
        
        this.propsPanel = new PropertiesPanel();
        this.tree.addTreeSelectionListener(this.propsPanel);
        
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(this.tree), new JScrollPane(this.propsPanel));
        
        this.add(split, BorderLayout.CENTER);
        //this.pack();
    }
    
    private void createMenus()
    {
        // Create a menu bar
        JMenuBar menuBar = new JMenuBar();
        
        // Create the Service menu (contains items to open connections to 
        // remote servers, etc)
        JMenu serviceMenu = new JMenu("Service");
        serviceMenu.setMnemonic(KeyEvent.VK_S);
        
        JMenuItem connectItem = new JMenuItem("Connect to server...");
        // TODO: Ctrl-C should be "copy"?
        connectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
            ActionEvent.CTRL_MASK));
        connectItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // Open a dialog box for the user to enter the URL to the 
                // root of the SGS server
                // TODO: allow entry of username etc (certificate?)
                String input = JOptionPane.showInputDialog("Enter path to the "
                    + "server root (e.g. \"myserver:9092\")");
                if (input != null)
                {
                    connectToServer(input);
                }
            }
        });
        serviceMenu.add(connectItem);
        
        serviceMenu.addSeparator();
        
        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
            ActionEvent.CTRL_MASK));
        quitItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                shutdown();
            }
        });
        serviceMenu.add(quitItem);
        
        menuBar.add(serviceMenu);
        
        this.setJMenuBar(menuBar);
    }
    
    /**
     * Takes the input from the message dialog box and tries to connect to the
     * remote server
     */
    private void connectToServer(String inputURL)
    {
        String host = "";
        int port = -1;
        try
        {
            // prepend correct protocol if it doesn't already exist
            if (inputURL.indexOf("://") == -1)
            {
                inputURL = "styx://".concat(inputURL);
            }
            URL url = new URL(inputURL);
            if (!url.getProtocol().equals("styx"))
            {
                JOptionPane.showMessageDialog(null,
                    "Invalid protocol \"".concat(url.getProtocol()).concat("\""),
                    "Invalid URL", JOptionPane.ERROR_MESSAGE);
            }
            host = url.getHost();
            port = url.getPort();
            StyxConnection conn = new StyxConnection(url.getHost(), url.getPort());
            conn.addListener(this);
            // Start the connection process. When we have connected successfully
            // and performed the necessary handshaking, the connectionReady() 
            // method will be called
            conn.connectAsync();
        }
        catch (MalformedURLException mue)
        {
            JOptionPane.showMessageDialog(null,
                "Invalid URL: " + mue.getMessage(), "Invalid URL",
                JOptionPane.ERROR_MESSAGE);
        }
        catch (StyxException se)
        {
            // This should not happen unless there's an error with MINA, the 
            // underlying network architecture
            JOptionPane.showMessageDialog(null, "Error connecting to " +
                host + ":" + port + ": " + se.getMessage(),
                "Connection error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createStatusBar()
    {
        statusBar = new JLabel("Status bar");
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        this.getContentPane().add(statusBar, BorderLayout.SOUTH);
    }
    
    /**
     * Sets up the tree that is used to browse the available SGS servers
     */
    private void setupTree()
    {
        this.treeModel = new SGSExplorerTreeModel();
        this.tree = new JTree(this.treeModel);
        this.tree.addTreeExpansionListener(this.treeModel);
        this.tree.setRootVisible(false);
        this.tree.setShowsRootHandles(true);
        // Ensure that only one node at a time can be selected in the tree
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.addMouseListener(ml);
    }
    
    /**
     * Sets the status bar text to the given string
     */
    public void setStatusBarText(String text)
    {
        this.statusBar.setText(text);
    }
    
    /**
     * Called just before the application closes. Add code to cleanly shut down
     * here.
     */
    private void shutdown()
    {
        // Close all open connections
        for (int i = 0; i < this.openConnections.size(); i++)
        {
            StyxConnection conn = (StyxConnection)this.openConnections.get(i);
            conn.close();
        }
        System.exit(0);
    }
    
    /**
     * Required by StyxConnectionListener interface. Called when the relevant
     * handshaking has been performed and the connection is ready for Styx
     * messages to be sent
     */
    public void connectionReady(StyxConnection conn)
    {
        System.out.println("Connected to " +
            conn.getRemoteHost() + ":" + conn.getRemotePort());
        // TODO: check that this is a SGS server (look for .version file)
        this.openConnections.add(conn);
        // Add this connection to the tree model
        this.treeModel.addNewConnection(conn);
    }
    
    /**
     * Required by StyxConnectionListener interface.  Called when the connection
     * has been closed.  Does nothing here.
     * TODO: use this as signal to remove connection from tree
     */
    public void connectionClosed(StyxConnection conn){}
    
    /**
     * Required by StyxConnectionListener interface.  Called when an error has
     * occurred when connecting.
     * @param message String describing the problem
     */
    public void connectionError(StyxConnection conn, String message)
    {
        JOptionPane.showMessageDialog(null, "Could not connect to " +
            conn.getRemoteHost() + ":" + conn.getRemotePort() + ": "
            + message, "Connection error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Listens for requests for the pop-up menu.
     * Note that for platform independence we must check for the popup trigger
     * in both mousePressed and mouseReleased
     */
    private MouseListener ml = new MouseAdapter()
    {
        public void mousePressed(MouseEvent e)
        {
            maybeShowPopup(e);
        }
        public void mouseReleased(MouseEvent e)
        {
            maybeShowPopup(e);
        }
        private void maybeShowPopup(MouseEvent e)
        {
            if (e.isPopupTrigger())
            {
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                // If we don't click on something in the tree itself, selPath
                // will be null
                if (selPath != null)
                {
                    Object lastPathComp = selPath.getLastPathComponent();
                    if (lastPathComp instanceof CStyxFileNode)
                    {
                        // Get the CStyxFileNode that we have clicked on
                        CStyxFileNode node = (CStyxFileNode)lastPathComp;
                        StyxExplorerPopupMenu.showContext(node, tree, e.getX(),
                            e.getY());
                    }
                }
            }
        }
    };
    
    public static void main(String[] args)
    {
        // Make sure "styx://" URLs are valid
        System.setProperty("java.protocol.handler.pkgs", "uk.ac.rdg.resc.jstyx.client.protocol");
        new SGSExplorer().setVisible(true);
    }
    
}
