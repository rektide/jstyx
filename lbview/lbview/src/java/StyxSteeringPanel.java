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

import javax.swing.*;
import java.awt.event.*;
import info.clearthought.layout.*;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.gridservice.client.SGSInstanceClient;
import uk.ac.rdg.resc.jstyx.gridservice.client.SGSInstanceChangeListener;

/**
 * Panel containing input boxes to steer a simulation via the Styx interface
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/09/19 07:44:43  jonblower
 * Got steering working via Styx interface
 *
 */
public class StyxSteeringPanel extends JPanel implements ActionListener,
    FocusListener, SGSInstanceChangeListener
{
    
    private SGSInstanceClient instance;
    private CStyxFile[] steeringFiles;
    private JTextField[] txtSteeringValues;
    private JButton[] btnSetSteeringValues;
    
    /** Creates a new instance of StyxSteeringPanel */
    public StyxSteeringPanel(SGSInstanceClient instance) throws StyxException
    {
        this.instance = instance;
        this.instance.addChangeListener(this);
    }
    
    /**
     * Call this to make the panel active
     */
    public void populatePanel()
    {
        this.instance.readAllSteeringParamsAsync();
    }
    
    /**
     * This is called when a button is pressed on the steering panel or when
     * a value in a text field is changed
     */
    public void actionPerformed(ActionEvent e)
    {
        for (int i = 0; i < this.btnSetSteeringValues.length; i++)
        {
            if (e.getSource() == this.btnSetSteeringValues[i] ||
                e.getSource() == this.txtSteeringValues[i])
            {
                // Write the new steering value to the file
                this.steeringFiles[i].writeAsync(this.txtSteeringValues[i].getText(), 0);
                break;
            }
        }
    }
    
    public void focusGained(FocusEvent e) {}
    public void focusLost(FocusEvent e)
    {
        for (int i = 0; i < this.txtSteeringValues.length; i++)
        {
            if (e.getSource() == this.txtSteeringValues[i])
            {
                // Write the new steering value to the file
                this.steeringFiles[i].writeAsync(this.txtSteeringValues[i].getText(), 0);
                break;
            }
        }
    }
    
    /**
     * Called when we have found the possible steerable parameters
     */
    public void gotSteerableParameters(CStyxFile[] steerableFiles)
    {
        this.steeringFiles = steerableFiles;
        
        // Set the layout of the steering panel
        double[] cols = { 5, TableLayout.FILL, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5 };
        double[] rows = new double[2 * this.steeringFiles.length + 1];
        for (int i = 0; i < steeringFiles.length; i++)
        {
            rows[2 * i] = 5.0;
            rows[2 * i + 1] = TableLayout.PREFERRED;
        }
        rows[2 * this.steeringFiles.length] = 5.0;
        double[][] size = { cols, rows };
        TableLayout layout = new TableLayout(size);
        this.setLayout(layout);

        // Add the components
        this.txtSteeringValues = new JTextField[this.steeringFiles.length];
        this.btnSetSteeringValues = new JButton[this.steeringFiles.length];
        for (int i = 0; i < this.steeringFiles.length; i++)
        {
            int row = 2 * i + 1;
            this.add(new JLabel(this.steeringFiles[i].getName()), "1, " + row);

            // Create a text field and associate it with the CStyxFile
            this.txtSteeringValues[i] = new JTextField(7);
            this.txtSteeringValues[i].addActionListener(this);
            this.txtSteeringValues[i].addFocusListener(this);
            this.add(this.txtSteeringValues[i], "3, " + row);
            this.btnSetSteeringValues[i] = new JButton("Set");
            this.btnSetSteeringValues[i].addActionListener(this);
            this.add(this.btnSetSteeringValues[i], "5, " + row);
        }
        
    }
    
    /**
     * Called when the value of a steerable parameter changes
     */
    public void gotSteerableParameterValue(int index, String value)
    {
        this.txtSteeringValues[index].setText(value);
    }
    
    
    public void serviceDataChanged(String sdName, String newData) {}
    public void gotServiceDataElements(CStyxFile[] sdeFiles) {}
    public void gotInputMethods(CStyxFile[] inputMethods) {}
    public void gotInputFiles(CStyxFile[] inputFiles, boolean allowOtherInputFiles) {}
    public void gotOutputStreams(CStyxFile[] outputStreams) {}
    public void gotParameters(CStyxFile[] paramFiles) {}
    public void gotParameterValue(int index, String value) {}
    public void gotCommandLine(String newCmdLine) {}
    public void inputURLSet() {}
    public void inputFilesUploaded() {}
    public void serviceStarted() {}
    public void serviceAborted() {}
    public void error(String message) {}
    
}
