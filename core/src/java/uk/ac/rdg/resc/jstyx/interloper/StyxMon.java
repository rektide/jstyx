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

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.*;
import java.net.InetSocketAddress;
import java.util.Hashtable;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.server.StyxServer;
import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.TattachMessage;
import uk.ac.rdg.resc.jstyx.messages.TwalkMessage;

import info.clearthought.layout.TableLayout;

/**
 * TCPMon-like GUI application that displays all the messages going between
 * a Styx client and server
 * @todo Make sure all connections are closed properly on exit, and that
 * all threads are stopped.
 * @todo Merge more closely with StyxInterloper
 * @todo Allow messages to be saved as a .csv file or plain ASCII text
 * @todo Handle more events such as clients connecting, disconnecting,
 * server connection going down, etc (feed back to GUI)
 * @todo Filter messages by type, whether they have been replied to, errors
 * @todo Add a "hint" column containing a description of what the message is doing
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/02/24 11:23:32  jonblower
 * Handles filtering by filename correctly
 *
 * Revision 1.3  2005/02/24 09:07:12  jonblower
 * Added code to support filtering by pop-up menu
 *
 * Revision 1.2  2005/02/24 07:42:44  jonblower
 * *** empty log message ***
 *
 * Revision 1.1  2005/02/21 18:08:52  jonblower
 * Initial import
 *
 */
public class StyxMon implements InterloperListener
{
    
    // Styx components
    StyxServer server;   // The server that listens for incoming messages
    
    // GUI components
    private JFrame frame;
    private Container contentPane;
    private JTable table;
    private StyxMonTableModel model;
    private JLabel lblPort;
    private JLabel lblRemoteServer;
    private StyxMonPopupMenu popup;
    
    private int nextFreeRow;    // The next free row in the table
    private Hashtable rowTags;  // Links tags to their row numbers
    private Hashtable fidNames; // Links fids to the file name
    
    private static final String NOT_APPLICABLE = "N/A";
    
    /**
     * Creates a new instance of StyxMon
     * @param port The port on which this application will listen
     * @param serverHost The host of the remote Styx server
     * @param serverPort The port of the remote Styx server
     * @throws StyxException if the server process could not be started.
     */
    public StyxMon(int port, String serverHost, int serverPort)
        throws StyxException
    {
        // Create a StyxServer that will listen for connections from clients
        this.server = new StyxServer(port, new
                StyxInterloperServerSessionListener(new InetSocketAddress(serverHost,
                serverPort), this));
        this.server.start();
        
        frame = new JFrame("Styx Monitor");
        frame.setBounds (100, 100, 550, 500);
        contentPane = frame.getContentPane();
        
        lblPort = new JLabel("Listening on port " + port);
        lblRemoteServer = new JLabel("Remote server: " + serverHost + ":"
            + serverPort);
        
        // Create the table model
        this.model = new StyxMonTableModel();
        this.table = new JTable(model);
        // All the cells in the table are Strings, so the StyxMonTableCellRenderer
        // will be used by all cells
        this.table.setDefaultRenderer(String.class, new StyxMonTableCellRenderer());
        
        this.rowTags = new Hashtable();
        this.fidNames = new Hashtable();
        
        // Set the table column widths (the Tag column should be much narrower
        // than the rest)
        TableColumn column = null;
        for (int i = 0; i < this.model.getColumnCount(); i++)
        {
            column = table.getColumnModel().getColumn(i);
            if (i == 0 || i == 1)
            {
                column.setPreferredWidth(50);
                column.setMaxWidth(50);
            }
            else
            {
                column.setPreferredWidth(100);
            }
        }
        // Prevent users from editing the table
        table.setEnabled(false);
        this.nextFreeRow = 0; // The first free row in the table
        
        // Create the scroll pane and add the table to it
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Create the table layout
        double size[][] =
        {
            { 10, TableLayout.FILL, 10, TableLayout.FILL, 10}, // Columns
            { 10, 20, 10, 20, 20, TableLayout.FILL, 10 }  // Rows
        };
        contentPane.setLayout(new TableLayout(size));
        // Add the components to the table layout
        contentPane.add(lblPort, "1, 1, l, f");
        contentPane.add(lblRemoteServer, "1, 3, l, f");
        contentPane.add(scrollPane, "1, 5, 3, 5, f, f");
        
        // Create the popup menu
        this.popup = new StyxMonPopupMenu(this.model);
        
        // Allow user to close the window to terminate the program
        frame.addWindowListener
        (
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    exit();
                }
            }
        );
        
        // Add the mouse listener that will listen for right-click events
        // on the table
        this.table.addMouseListener(ml);
        
        frame.setVisible(true);
    }
    
    /**
     * Closes the connections and exits the application
     */
    private void exit()
    {
        // TODO: close the connection
        System.exit(0);
    }
    
    /**
     * Filters the data
     */
    private void filterClicked()
    {
        this.model.filterByFilename("/md5sum/1/ctl");
    }
    
    /**
     * Removes any filters from the data
     */
    private void showAllClicked()
    {
        this.model.showAllData();
    }
    
    /**
     * Required by the InterloperListener interface. Called when a Tmessage
     * arrives from a client
     */
    public synchronized void tMessageReceived(StyxMessage message)
    {
        // Get the full path of the file represented by this fid
        String path = (String)this.fidNames.get(new Long(message.getFid()));
        if (path == null)
        {
            // This happens for messages that aren't associated with a fid
            path = NOT_APPLICABLE;
        }
        // If this is a TattachMessage we know what fid is associated with the root
        // of the remote server
        if (message instanceof TattachMessage)
        {
            TattachMessage tAttMsg = (TattachMessage)message;
            this.fidNames.put(new Long(tAttMsg.getFid()), "/");
        }
        // If this is a TwalkMessage we can associate the new fid with the
        // path of the file in questions
        else if (message instanceof TwalkMessage)
        {
            TwalkMessage tWalkMsg = (TwalkMessage)message;
            this.fidNames.put(new Long(tWalkMsg.getNewFid()), path + tWalkMsg.getPath());
        }
        int theRow = this.getRow(message.getTag());
        model.addTMessageData(theRow, getMessageName(message.getName()),
            message.getTag(), path, message.toFriendlyString());
        table.repaint();
    }
    
    /**
     * Required by the InterloperListener interface. Called when an Rmessage
     * has been sent back to the client
     */
    public synchronized void rMessageSent(StyxMessage message)
    {
        // Get the row number for this message
        int theRow = this.getRow(message.getTag());
        model.setValueAt(message.toFriendlyString(), theRow, 4);
        table.repaint();
    }
        
    /**
     * Converts the name of a Tmessage (e.g. "Tread") into the generic name
     * of the message (e.g. "Read")
     */
    private String getMessageName(String tMessageName)
    {
        // Strip off first two letters
        String lastBit = tMessageName.substring(2);
        // Capitalise the second letter of the original string
        String firstLetter = tMessageName.substring(1, 2).toUpperCase();
        return firstLetter + lastBit;
    }
    
    /**
     * @return the row corresponding to the given tag. If the tag does not exist
     * in the cache, the index of the next free row in the data model will be
     * returned (creating a new row if necessary).
     */
    private synchronized int getRow(int tag)
    {
        // Find out if we have a row for this tag
        Integer intRow = (Integer)rowTags.remove(new Integer(tag));
        if (intRow == null)
        {
            // We don't have a row for this tag. Create a new row in the data model
            model.addRow();
            // Associate the new row with the tag
            rowTags.put(new Integer(tag), new Integer(nextFreeRow));
            int r = nextFreeRow;
            nextFreeRow++;
            return r;
        }
        else
        {
            // We already have a row for this tag
            return intRow.intValue();
        }
    }
    
    /**
     * Listens for right clicks (i.e. requests for the pop-up menu)
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
                int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
                if (row != -1)
                {
                    // Get the filename at this row
                    String filename = (String)model.getValueAt(row, 2);
                    if (filename != null && !filename.equals("") &&
                        !filename.equals(NOT_APPLICABLE))
                    {
                        System.out.println("filtering on filename " + model.getValueAt(row, 2));
                        popup.showContext(filename, table, e.getX(), e.getY());
                    }
                }
            }
        }
    };
    
    public static void main(String[] args) throws Exception
    {
        /*if (args.length != 3)
        {
            System.err.println("Usage: java StyxMon <port> <remote host> <remote port>");
            return;
        }
        int port;
        int remotePort;
        try
        {
            port = Integer.parseInt(args[0]);
            if (port < 0 || port > StyxUtils.MAXUSHORT)
            {
                System.err.println("Port number must be between 0 and " + StyxUtils.MAXUSHORT);
                return;
            }
        }
        catch(NumberFormatException nfe)
        {
            System.err.println("Invalid port number");
            return;
        }
        try
        {
            remotePort = Integer.parseInt(args[2]);
        }
        catch(NumberFormatException nfe)
        {
            System.err.println("Invalid remote port number");
            return;
        }
        new StyxMon(port, args[1], remotePort);*/
        new StyxMon(9999, "localhost", 9092);
    }
    
}
