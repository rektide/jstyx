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
import java.awt.event.*;
import java.awt.Component;

import org.apache.mina.common.ByteBuffer;

import info.clearthought.layout.TableLayout;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Panel containing GUI components to interact with a specific SGS instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.3  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.2  2005/02/21 18:12:29  jonblower
 * Following changes to core JStyx library
 *
 * Revision 1.1  2005/02/16 19:22:30  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public class SGSInstancePanel extends JPanel implements SGSInstanceChangeListener
{
    
    private SGSInstanceClient client = null;
    
    // GUI components
    private JLabel lblInstanceID;
    private JTextField txtInputURL = new JTextField();
    //private JTextField txtInputURL = new JTextField("styx://localhost:7777/LICENCE");
    private JButton btnStart = new JButton("Start");
    private JButton btnStop = new JButton("Stop");
    private JLabel lblStatus = new JLabel("Status:");
    private JLabel lblBytesConsumed = new JLabel("Bytes consumed:");
    private JTextArea txtStdout = new JTextArea();
    private JTextArea txtStderr = new JTextArea();
    
    /** Creates a new instance of SGSInstancePanel */
    public SGSInstancePanel(SGSInstanceClient client)
    {
        lblInstanceID = new JLabel("instance id:" );
        this.setClient(client);
        
        double size[][] =
        {
            { TableLayout.FILL, 20, 0.25, 10, 0.25}, // Columns
            { 20, 10, 20, 10, 20, 20, 10, TableLayout.FILL}  // Rows
        };
        this.setLayout(new TableLayout(size));
        
        // Lay out the components; syntax is contentPane.add(component, "col, row")
        this.add(lblInstanceID, "2, 0");
        this.add(txtInputURL, "0, 2");
        this.add(btnStart, "2, 2");
        this.add(btnStop, "4, 2");
        this.add(lblStatus, "0, 4, 3, 4");
        this.add(lblBytesConsumed, "0, 5");
        JScrollPane scrStdout = new JScrollPane(txtStdout);
        this.add(scrStdout, "0, 7");
        JScrollPane scrStderr = new JScrollPane(txtStderr);
        this.add(scrStderr, "2, 7, 4, 7");
        
        // Set response to clicking on the "start" button
        btnStart.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    startClicked();
                }
            }
        );
        
        // Set response to clicking on the "stop" button
        btnStop.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    stopClicked();
                }
            }
        );
        
        this.setVisible(true);
    }
    
    /**
     * Sets the SGSInstanceClient from which this panel will get its information
     */
    public synchronized void setClient(SGSInstanceClient client)
    {
        // Remove this change listener from any previous clients
        if (this.client != null)
        {
            this.client.removeChangeListener(this);
        }
        // Get the state data from the SGSInstanceClient (this is useful when displaying
        // the data from a service that's already running)
        lblInstanceID.setText("Instance id: " + client.getInstanceID());
        txtInputURL.setText(client.getInputURL());
        lblStatus.setText("Status: " + client.getStatus());
        lblBytesConsumed.setText("Bytes consumed: " + client.getBytesConsumed());
        this.repaint();
        // Register this as a change listener for the SGS instance client
        client.addChangeListener(this);
        this.client = client;
    }
    
    /**
     * Called when the user clicks on the "start" button
     * @todo: this blocks until the service is started
     */
    private void startClicked()
    {
        try
        {
            // TODO: should not let this happen if the service is already started
            // (perhaps we should grey out the start button until the service stops)
            if (!txtInputURL.getText().equals(""))
            {
                client.setInputURL(txtInputURL.getText());
            }
            client.startService();
        }
        catch(StyxException se)
        {
            se.printStackTrace();
        }
    }
    
    /**
     * Called when "stop" is pressed: sends signal to stop the SGS
     */
    private void stopClicked()
    {
        try
        {
            client.stopService();
        }
        catch(StyxException se)
        {
            se.printStackTrace();
        }
    }
    
    /**
     * Called when new data arrive from the standard output of the SGS instance
     */
    public void newStdoutData(ByteBuffer newData)
    {
        this.txtStdout.append(StyxUtils.dataToString(newData));
        this.txtStdout.repaint();
    }
    
    /**
     * Called when new data arrive from the standard error of the SGS instance
     */
    public void newStderrData(ByteBuffer newData)
    {
        this.txtStderr.append(StyxUtils.dataToString(newData));
        this.txtStderr.repaint();
    }
    
    /**
     * Called when the status of the Styx Grid Service changes
     */
    public synchronized void statusChanged(String newStatus)
    {
        this.lblStatus.setText("Status: " + newStatus);
        this.lblStatus.repaint();
    }
    
    /**
     * Called when the number of bytes consumed by the SGS changes
     */
    public synchronized void bytesConsumedChanged(String newBytesConsumed)
    {
        this.lblBytesConsumed.setText("Bytes consumed: " + newBytesConsumed);
        this.lblBytesConsumed.repaint();
    }
    
    /**
     * Called when the service instance has been started
     */
    public synchronized void serviceStarted()
    {
        new ShowMessage(this, "Service has been started").start();
    }
    
    /**
     * Called when the service instance is stopped before it has finished
     */
    public synchronized void serviceAborted()
    {
        new ShowMessage(this, "Service has been aborted").start();
    }
    
    /**
     * Called when an error occurs with the service instance
     */
    public synchronized void error(String message)
    {
        JOptionPane.showMessageDialog(this, message);
    }
    
    /**
     * Gets the id number of the SGS instance that is being displayed on this panel
     */
    public String getCurrentInstanceID()
    {
        return this.client.getInstanceID();
    }
    
    /**
     * Thread that displays a message box (doesn't hang up GUI)
     */
    private static class ShowMessage extends Thread
    {
        private Component component;
        private String message;
        public ShowMessage(Component component, String message)
        {
            this.component = component;
            this.message = message;
            this.setDaemon(true);
        }
        public void run()
        {
            JOptionPane.showMessageDialog(this.component, this.message);
        }
    }
}
