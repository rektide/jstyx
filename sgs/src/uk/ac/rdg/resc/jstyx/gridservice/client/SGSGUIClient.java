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

import javax.swing.*;
import java.awt.Container;
import java.awt.event.*;

import java.util.Hashtable;

import info.clearthought.layout.TableLayout;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.StyxConnection;

/**
 * Simple GUI client for a Styx Grid Service
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/24 07:50:28  jonblower
 * *** empty log message ***
 *
 * Revision 1.1  2005/02/16 19:22:29  jonblower
 * Commit adding of SGS files to CVS
 */
public class SGSGUIClient implements SGSChangeListener
{
    // GUI components
    private JFrame frame;
    private Container contentPane;
    private JButton btnNewInstance = new JButton("New instance");
    private JButton btnPrevInstance = new JButton("Previous");
    private JButton btnNextInstance = new JButton("Next");
    private SGSInstancePanel instancePanel;
    
    // Hashtable of SGSInstanceClients
    private Hashtable instanceClients;
    
    // Styx stuff
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 9999;
    private static final String SERVICE_NAME = "md5sum";
    private SGSClient client;
    
    public SGSGUIClient() throws StyxException
    {
        // Try to connect to the remote service (as anonymous user)
        this.client = new SGSClient(HOSTNAME, PORT, "", SERVICE_NAME);
        this.client.addChangeListener(this);
        
        frame = new JFrame("Styx Grid Service client");
        frame.setBounds (100, 100, 550, 500);
        contentPane = frame.getContentPane();
        
        double size[][] =
        {
            { 10, TableLayout.FILL, 10, TableLayout.FILL, 10, TableLayout.FILL, 10}, // Columns
            { 10, 20, 10, TableLayout.FILL, 10 }  // Rows
        };
        contentPane.setLayout(new TableLayout(size));

        // Add the buttons in the centre of the box, fully-justified in the
        // vertical direction
        contentPane.add(btnNewInstance,  "1, 1, c, f");
        contentPane.add(btnPrevInstance, "3, 1, c, f");
        contentPane.add(btnNextInstance, "5, 1, c, f");
        
        this.instanceClients = new Hashtable();

        // Allow user to close the window to terminate the program
        frame.addWindowListener
        (
            new WindowAdapter()
            {
                public void windowClosing (WindowEvent e)
                {
                    // TODO: close the connection
                    System.exit(0);
                }
            }
        );
        
        // Set response to clicking on the "new instance" button
        btnNewInstance.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    newInstanceClicked();
                }
            }
        );
        
        // Set response to clicking on the "new instance" button
        btnNextInstance.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    nextInstanceClicked();
                }
            }
        );
        
        // Set response to clicking on the "new instance" button
        btnPrevInstance.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    prevInstanceClicked();
                }
            }
        );
        
        frame.setVisible(true);
    }
    
    /**
     * Called when the user clicks the "new instance" button
     */
    private void newInstanceClicked()
    {
        try
        {
            // Send the request to create a new instance. When the new instance
            // has been created, the newInstanceCreated() method will be called
            this.client.createNewInstance();
        }
        catch(StyxException se)
        {
            JOptionPane.showMessageDialog(this.frame, "Error sending request to"
                + " create new instance: " + se.getMessage());
        }
    }
    
    /**
     * Called when the user clicks the "next instance" button
     */
    private void nextInstanceClicked()
    {
        /*if (this.instancePanel != null)
        {
            int nextInstanceID = this.instancePanel.getCurrentInstanceID() + 1;
            SGSInstanceClient client =
                (SGSInstanceClient)this.instanceClients.get(new Integer(nextInstanceID));
            if (client != null)
            {
                this.instancePanel.setClient(client);
            }
        }*/
    }
    
    /**
     * Called when the user clicks the "previous instance" button
     */
    private void prevInstanceClicked()
    {
        /*if (this.instancePanel != null)
        {
            int prevInstanceID = this.instancePanel.getCurrentInstanceID() - 1;
            SGSInstanceClient client =
                (SGSInstanceClient)this.instanceClients.get(new Integer(prevInstanceID));
            if (client != null)
            {
                this.instancePanel.setClient(client);
            }
        }*/
    }
    
    /**
     * Fired when a new instance of the SGS has been created. Required by the
     * SGSChangeListener interface
     * @param id The id of the new instance
     */
    public void newInstanceCreated(String id)
    {
        SGSInstanceClient client = this.client.getClientForInstance(id);
        this.instanceClients.put(new Integer(id), client);
        if (this.instancePanel == null)
        {
            this.instancePanel = new SGSInstancePanel(client);
            contentPane.add(this.instancePanel, "1, 3, 5, 3");
            contentPane.validate();
        }
        else
        {
            this.instancePanel.setClient(client);
        }
        contentPane.repaint();
    }
    
    public static void main (String args[]) throws Exception
    {
        new SGSGUIClient();
    }
    
}
