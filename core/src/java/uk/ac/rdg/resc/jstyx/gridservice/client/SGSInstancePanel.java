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
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.apache.mina.common.ByteBuffer;

import info.clearthought.layout.TableLayout;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;

/**
 * Panel containing GUI components to interact with a specific SGS instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.9  2005/05/20 07:45:28  jonblower
 * Implemented getInputFiles() to find the input files required by the service
 *
 * Revision 1.8  2005/05/18 17:13:51  jonblower
 * Created SGSInstanceGUI
 *
 * Revision 1.7  2005/05/13 16:49:34  jonblower
 * Coded dynamic detection and display of service data, also included streams in config file
 *
 * Revision 1.6  2005/05/12 16:00:29  jonblower
 * Implementing reading of service data elements
 *
 * Revision 1.5  2005/05/11 18:25:00  jonblower
 * Implementing automatic detection of service data elements
 *
 * Revision 1.4  2005/05/11 15:13:25  jonblower
 * Implementing automatic display of service data elements
 *
 * Revision 1.3  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.2  2005/03/18 13:56:00  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
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
    
    private static final Logger log = Logger.getLogger(SGSInstancePanel.class);
    
    private SGSInstanceClient client = null;
    
    // GUI components
    private TableLayout layout;
    private JLabel lblInstanceID;
    private JTextField txtInputURL = new JTextField();
    
    private JButton btnStart = new JButton("Start");
    private JButton btnStop = new JButton("Stop");
    
    // Labels for service data elements
    private Hashtable sdeLabels; // Hashtable<String, JLabel>
    
    private JTextArea txtStdout = new JTextArea();
    private JTextArea txtStderr = new JTextArea();
    
    /** Creates a new instance of SGSInstancePanel */
    public SGSInstancePanel(SGSInstanceClient client)
    {
        lblInstanceID = new JLabel("instance id:" );
        
        double size[][] =
        {
            { TableLayout.FILL, 20, 0.25, 10, 0.25 }, // Columns
            { 20, 10, 20, 10, 10, TableLayout.FILL }  // Rows
        };
        this.layout = new TableLayout(size);
        this.setLayout(this.layout);
        
        // Lay out the components; syntax is contentPane.add(component, "col, row")
        this.add(lblInstanceID, "2, 0");
        this.add(txtInputURL, "0, 2");
        this.add(btnStart, "2, 2");
        this.add(btnStop, "4, 2");
        this.sdeLabels = new Hashtable();
        JScrollPane scrStdout = new JScrollPane(txtStdout);
        this.add(scrStdout, "0, 5");
        JScrollPane scrStderr = new JScrollPane(txtStderr);
        this.add(scrStderr, "2, 5, 4, 5");
        
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
        this.setClient(client);
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
        this.client = client;
        // Register this as a change listener for the SGS instance client
        this.client.addChangeListener(this);
        
        // Get the state data from the SGSInstanceClient (this is useful when displaying
        // the data from a service that's already running)
        lblInstanceID.setText("Instance id: " + client.getInstanceID());
        txtInputURL.setText(client.getInputURL());
        
        // Send message to get the service data elements. When they arrive, the
        // gotServiceDataNames() method will be called
        this.client.getServiceDataNames();
        
        this.repaint();
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
        client.stopService();
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
     * Called when an element of service data changes. Updates the label to the
     * new value.
     */
    public void serviceDataChanged(String sdeName, String newData)
    {
        JLabel sdeLabel = (JLabel)this.sdeLabels.get(sdeName);
        if (sdeLabel == null)
        {
            log.debug("Got data for unrecognised service data element " + sdeName);
        }
        else
        {
            sdeLabel.setText(sdeName + ": " + newData);
        }
    }
    
    /**
     * Called when we have got the possible service data elements
     * @param sdeNames The names of the SDEs as a String array
     */
    public void gotServiceDataNames(String[] sdeNames)
    {
        final int rowHeight = 20;
        for (int i = 0; i < sdeNames.length; i++)
        {
            this.layout.insertRow(4, rowHeight);
            JLabel sdeLabel = new JLabel(sdeNames[i]);
            // Add this JLabel to the Hashtable, indexing it by 
            // the name of the service data element
            this.sdeLabels.put(sdeNames[i], sdeLabel);
            this.add(sdeLabel, "0, 4, 4, 4");
        }
        this.layout.layoutContainer(this);
        this.repaint();
    }
    
    /**
     * Called when we have got the names of the service data elements
     * @param inputMethods The names of input files (stdin and the input URL)
     */
    public void gotInputMethods(CStyxFile[] inputMethods) {}
    
    /**
     * Called when we have discovered the input files that the service instance
     * expects.
     * @param inputFiles Array of CStyxFiles representing all the compulsory
     * input files that must be uploaded to the service
     * @param allowOtherInputFiles If true, we will have the option of uploading
     * other input files to the service instance
     */
    public void gotInputFiles(CStyxFile[] inputFiles, boolean allowOtherInputFiles) {}
    
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
