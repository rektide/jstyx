/***************************************************************************
 *   Copyright (C) 2005 by Ed Llewellin                                    *
 *   ed.llewellin@bristol.ac.uk                                            *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import info.clearthought.layout.*;
import java.util.Vector;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.gridservice.client.SGSClient;
import uk.ac.rdg.resc.jstyx.gridservice.client.SGSInstanceClient;
import uk.ac.rdg.resc.jstyx.gridservice.client.SGSChangeListener;
import uk.ac.rdg.resc.jstyx.StyxException;

public class SGSDialog extends JDialog implements ActionListener, SGSChangeListener
{
    
    private static final int BORDER = 5;
    
    private JData jdata;
    
    private JTextField txtSGSServer;
    private JButton btnConnect;
    private JComboBox cmbInstances;
    private JRadioButton btnNewInstance;
    private JRadioButton btnExistingInstance;
    
    private JButton btnOk;
    private JButton btnCancel;
    
    private StyxConnection conn;
    private SGSClient client;
    
    public SGSDialog(JData jdata)
    {
        super(jdata, "Set up Styx Grid Service", true);
        this.jdata = jdata;
        
        //set the layout of the info panel
        double[][] size =
        {
            {BORDER, TableLayout.FILL, 2, TableLayout.PREFERRED, 2, TableLayout.PREFERRED, BORDER }, // columns
            {BORDER, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, BORDER}  // rows
        };
        TableLayout layout = new TableLayout(size);
        Container content = this.getContentPane();
        content.setLayout(layout);
        
        JLabel lblSGSServer = new JLabel("SGS server location:");
        lblSGSServer.setHorizontalAlignment(JLabel.RIGHT);
        content.add(lblSGSServer, "1, 1");
        this.txtSGSServer = new JTextField("lovejoy.nerc-essc.ac.uk:9092");
        content.add(this.txtSGSServer, "3, 1");
        this.btnConnect = new JButton("Connect");
        this.btnConnect.addActionListener(this);
        content.add(this.btnConnect, "5, 1");
        
        this.btnNewInstance = new JRadioButton("Create new instance");
        this.btnNewInstance.setHorizontalTextPosition(SwingConstants.LEADING);
        this.btnNewInstance.setHorizontalAlignment(SwingConstants.RIGHT);
        this.btnNewInstance.setEnabled(false);
        this.btnNewInstance.addActionListener(this);
        this.btnExistingInstance = new JRadioButton("Use existing instance");
        this.btnExistingInstance.setHorizontalTextPosition(SwingConstants.LEADING);
        this.btnExistingInstance.setHorizontalAlignment(SwingConstants.RIGHT);
        this.btnExistingInstance.setEnabled(false);
        this.btnExistingInstance.addActionListener(this);
        ButtonGroup group = new ButtonGroup();
        group.add(this.btnNewInstance);
        group.add(this.btnExistingInstance);
        content.add(this.btnNewInstance, "3, 3");
        content.add(this.btnExistingInstance, "3, 5");
        
        this.cmbInstances = new JComboBox();
        this.cmbInstances.addItem("(select)");
        this.cmbInstances.setEnabled(false);
        this.cmbInstances.addActionListener(this);
        content.add(this.cmbInstances, "5, 5");
        
        this.btnCancel = new JButton("Cancel");
        this.btnCancel.addActionListener(this);
        content.add(this.btnCancel, "1, 9");
        this.btnOk = new JButton("OK");
        this.btnOk.addActionListener(this);
        this.btnOk.setEnabled(false);
        content.add(this.btnOk, "3, 9");
        
        // Add listener to shut system down cleanly when window is closed
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                closeDialog();
            }
        });
        
        this.pack();
        //set position of the dialog
        Dimension jdata_dimension = this.jdata.getSize();
        Dimension dialog_dimension = super.getSize();
        Point jdata_location = this.jdata.getLocation();
        this.setLocation(jdata_location.x + (jdata_dimension.width - dialog_dimension.width) / 2,
            jdata_location.y + (jdata_dimension.height - dialog_dimension.height) / 2);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this.btnConnect)
        {
            try
            {
                // Try to establish a connection with the server
                this.connect();
                // If we've got this far, we must have an active connection.
                this.client = new SGSClient(conn.getRootDirectory().getFile("lbflow"));
                this.client.addChangeListener(this);
                // Send a message to get all the instances of this SGS.  When the
                // reply arrives, the gotInstances() method will be called
                this.client.getInstancesAsync();
            }
            catch(Exception ex)
            {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        }
        else if (e.getSource() == this.btnNewInstance)
        {
            this.cmbInstances.setEnabled(false);
            this.btnOk.setEnabled(true);
        }
        else if (e.getSource() == this.btnExistingInstance)
        {
            this.cmbInstances.setEnabled(true);
            if (this.cmbInstances.getSelectedItem() instanceof CStyxFile)
            {
                this.btnOk.setEnabled(true);
            }
            else
            {
                this.btnOk.setEnabled(false);
            }
        }
        else if (e.getSource() == this.btnCancel)
        {
            this.closeDialog();
        }
        else if (e.getSource() == this.btnOk)
        {
            String instanceID = "";
            boolean ok = true;
            boolean newInstance = true;
            if (this.btnNewInstance.isSelected())
            {
                try
                {
                    // Create a new instance
                    instanceID = this.client.createNewInstance();
                    JOptionPane.showMessageDialog(this, "Created new instance with ID "
                        + instanceID);
                }
                catch(StyxException se)
                {
                    ok = false;
                    JOptionPane.showMessageDialog(this,
                        "Error creating new instance: " + se.getMessage());
                }
            }
            else
            {
                // Use an existing instance
                instanceID = ((CStyxFile)this.cmbInstances.getSelectedItem()).getName();
                newInstance = false;
            }
            if (ok)
            {
                SGSInstanceClient instance = this.client.getClientForInstance(instanceID);
                this.jdata.setSGSInstanceClient(instance, newInstance);
                this.closeDialog();
            }
        }
        else if (e.getSource() == this.cmbInstances)
        {
            if (this.cmbInstances.getSelectedItem() instanceof CStyxFile)
            {
                this.btnOk.setEnabled(true);
            }
            else
            {
                this.btnOk.setEnabled(false);
            }
        }
    }
    
    private void closeDialog()
    {
        this.setVisible(false);
    }
    
    /**
     * Connect to the Styx Grid Service server
     * @throws StyxException if the connection could not be made
     * @throws Exception if the format of the text field is incorrect
     */
    private void connect() throws Exception
    {
        // Try to get the server and port from the text field
        String[] hostPort = null;
        try
        {
            hostPort = this.txtSGSServer.getText().split(":");
            if (hostPort.length != 2)
            {
                throw new Exception("Format must be \"host:port\"");
            }
            this.conn = new StyxConnection(hostPort[0], Integer.parseInt(hostPort[1]));
            this.conn.connect();
        }
        catch(NumberFormatException nfe)
        {
            throw new Exception("Invalid port number: " + hostPort[1]);
        }
    }
    
    /**
     * This method is called when we have a new list of SGS instances.  This can
     * happen when we make a new connection, or when a third party creates a new
     * instance
     */
    public void gotInstances(CStyxFile[] instances)
    {
        this.cmbInstances.removeAllItems();
        this.cmbInstances.addItem("(select)");
        for (CStyxFile file : instances)
        {
            this.cmbInstances.addItem(file);
        }
        this.btnNewInstance.setEnabled(true);
        this.btnExistingInstance.setEnabled(true);
    }
    
    /**
     * This method is called when the SGSClient returns an error (e.g. if there
     * has been an error reading the instances of the SGS
     */
    public void error(String message)
    {
        // TODO: This method blocks!  Should run in a separate thread to avoid
        // blocking the message dispatching thread.
        JOptionPane.showMessageDialog(this, message);
    }
    
}