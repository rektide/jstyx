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
import java.awt.Container;
import java.awt.event.*;

import net.gleamynode.netty2.Message;
import net.gleamynode.netty2.Session;
import net.gleamynode.netty2.SessionListener;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.server.StyxServer;
import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;
import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.TversionMessage;
import uk.ac.rdg.resc.jstyx.messages.RerrorMessage;

import info.clearthought.layout.TableLayout;

/**
 * TCPMon-like GUI application that displays all the messages going between
 * a Styx client and server
 *
 * @author Jon Blower
 * $Revision$
 * $Date $
 * $Log$
 * Revision 1.1  2005/02/21 18:08:52  jonblower
 * Initial import
 *
 */
public class StyxMon implements SessionListener
{
    
    // Styx components
    StyxServer server;   // The server that listens for incoming messages
    StyxConnection conn; // The client that forwards the messages to the remote
                         // server.
    
    // GUI components
    private JFrame frame;
    private Container contentPane;
    private JTable table;
    private JLabel lblPort;
    private JLabel lblRemoteServer;
    
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
        // Create a StyxConnection as an anonymous user. This does not actually
        // open the connection.
        this.conn = new StyxConnection(serverHost, serverPort, "");
        // Create a StyxServer that will listen for connections from clients
        this.server = new StyxServer(port, this);
        this.server.start();
        
        frame = new JFrame("Styx Monitor");
        frame.setBounds (100, 100, 550, 500);
        contentPane = frame.getContentPane();
        
        lblPort = new JLabel("Listening on port " + port);
        lblRemoteServer = new JLabel("Remote server: " + serverHost + ":"
            + serverPort);
        
        // Create the column headings for the JTable
        String[] columnHeadings = {"Type", "Tag", "TMessage", "RMessage", "Hint"};
        // Create the table data (a line of blank data)
        Object[][] data = { {"", "", "", "", ""} };
        table = new JTable(data, columnHeadings);
        // Set the table column widths (the Tag column should be much narrower
        // than the rest)
        TableColumn column = null;
        for (int i = 0; i < 4; i++)
        {
            column = table.getColumnModel().getColumn(i);
            if (i == 0)
            {
                column.setPreferredWidth(50); // The message type column
            }
            else if (i == 1)
            {
                column.setPreferredWidth(20); // The tag column
            }
            else
            {
                column.setPreferredWidth(100);
            }
        }
        // Prevent users from editing the table
        table.setEnabled(false);
        
        // Create the scroll pane and add the table to it
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Create the table layout
        double size[][] =
        {
            { 10, TableLayout.FILL, 10}, // Columns
            { 10, 20, 10, 20, 20, TableLayout.FILL, 10 }  // Rows
        };
        contentPane.setLayout(new TableLayout(size));

        // Add the buttons in the centre of the box, fully-justified in the
        // vertical direction
        contentPane.add(lblPort, "1, 1, l, f");
        contentPane.add(lblRemoteServer, "1, 3, l, f");
        contentPane.add(scrollPane, "1, 5, f, f");
        
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
     * Required by SessionListener interface. Called when a client connects
     * to this server
     * @todo this should raise an error if more than one client tries to connect
     */
    public void connectionEstablished(Session session)
    {
        // TODO: send the client's details to the GUI?
        System.err.println("Got connection from client.");
        try
        {
            // Starts the connection process. If we try to send messages on 
            // this connection before the connection/handshaking is finished,
            // this doesn't matter because the message will be queued.
            this.conn.connect();
        }
        catch(StyxException se)
        {
            // This shouldn't happen; it only happens if the IOProcessor
            // could not be started
            this.exceptionCaught(session, se);
        }
    }
    
    /**
     * Required by SessionListener interface. Called when a client disconnects
     * from this server
     */
    public void connectionClosed(Session session)
    {
        System.err.println("Client connection closed.");
        this.conn.close();
    }
    
    /**
     * Required by SessionListener interface. Called when a Tmessage is received
     * from the client
     */
    public void messageReceived(Session session, Message message)
    {
        System.err.println("RCVD from client: " + message);
        
        // Make sure the message size is <= 8192 bytes (TODO: allow for message
        // sizes larger than this)
        if (message instanceof TversionMessage)
        {
            TversionMessage tVerMsg = (TversionMessage)message;
            if (tVerMsg.getMaxMessageSize() > 8192)
            {
                tVerMsg.setMaxMessageSize(8192);
            }
        }
        this.conn.sendAsync((StyxMessage)message, new StyxMonCallback(session));
    }
    
    /**
     * Required by SessionListener interface. Called when an RMessage is sent
     * back to the client.
     */
    public void messageSent(Session session, Message message)
    {
        System.err.println("SENT to client: " + message);
    }
    
    /**
     * Required by SessionListener interface. Does nothing here.
     */
    public void sessionIdle(Session session)
    {
        // Sessions are never disconnected if they are idle - is this OK?
    }
    
    /**
     * Required by SessionListener interface.
     */
    public void exceptionCaught(Session session, Throwable cause)
    {
        System.err.println("Unexpected exception: " + cause);
    }
    
    /**
     * Callback class whose methods will be called when a reply arrives from
     * the remote server
     */
    private class StyxMonCallback extends MessageCallback
    {
        private Session session;
        public StyxMonCallback(Session session)
        {
            this.session = session;
        }
        public void replyArrived(StyxMessage message)
        {
            System.err.println("Arrived from client: " + message);
            this.session.write(message);
        }
        public void error(String message, int tag)
        {
            System.err.println("Error from client: " + message);
            // We have to reconstruct the Rerror message; TODO is there a better
            // way of doing this?
            RerrorMessage rErrMsg = new RerrorMessage(message);
            rErrMsg.setTag(tag);
            this.session.write(rErrMsg);
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        new StyxMon(9999, "localhost", 7777);
    }
    
}
