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

import java.util.Hashtable;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.JFileChooser;

import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.BorderLayout;

import java.util.Vector;

import info.clearthought.layout.TableLayout;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeListener;
import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

/**
 * GUI for interacting with an SGS instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/05/25 16:59:31  jonblower
 * Added uploadInputFile()
 *
 * Revision 1.4  2005/05/20 16:28:50  jonblower
 * Continuing to implement GUI app
 *
 */
public class SGSInstanceGUI extends JFrame implements SGSInstanceChangeListener
{
    
    private static final int ROW_HEIGHT = 20;
    private static final int BORDER = 10;
    
    // Contains the GUI for each instance, indexed by CStyxFile
    private static final Hashtable guis = new Hashtable();
    
    private SGSInstanceClient client; // Class that we use to interact with the service
    
    private JPanel masterPanel;
    private TableLayout panelLayout;
    private InputPanel inputPanel; // Panel for providing input data to the SGS
    private InputFilesPanel inputFilesPanel;  // Panel for allowing input files to be uploaded
    private ControlPanel ctlPanel; // Panel for controlling the service instance
    private ServiceDataPanel sdPanel; // Panel for showing service data
    
    private JLabel statusBar;
    
    /** Creates a new instance of SGSInstanceGUI */
    private SGSInstanceGUI(SGSInstanceClient client)
    {
        // Set the title of the frame to the full URL to the file,
        // stripping the "styx://" from the start of the URL
        super(client.getInstanceRoot().getURL().substring(7));
        this.setBounds(200, 100, 550, 400);
        
        this.client = client;
        this.client.addChangeListener(this);
        
        // Put the whole GUI in a scroll pane so that all of it can always be visible
        this.masterPanel = new JPanel();
        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(this.masterPanel), BorderLayout.CENTER);
        double size[][] =
        {
            { BORDER, TableLayout.FILL, BORDER }, // Columns
            { BORDER, TableLayout.PREFERRED, BORDER, TableLayout.PREFERRED, BORDER,
                  ROW_HEIGHT, BORDER, TableLayout.FILL, BORDER }  // Rows
        };
        this.panelLayout = new TableLayout(size);
        this.masterPanel.setLayout(this.panelLayout);
        
        // Add the input panel
        this.inputPanel = new InputPanel();
        this.masterPanel.add(new JLabel("Input panel goes here"), "1, 1");
        
        // Add the panel for uploading input files
        this.inputFilesPanel = new InputFilesPanel();
        this.masterPanel.add(this.inputFilesPanel, "1, 3");
        
        // Add the control panel
        this.ctlPanel = new ControlPanel();
        this.masterPanel.add(this.ctlPanel, "1, 5");
        
        // Add the service data panel
        this.sdPanel = new ServiceDataPanel();
        this.masterPanel.add(this.sdPanel, "1, 7");
        
        this.statusBar = new JLabel("Status bar");
        this.add(this.statusBar, BorderLayout.SOUTH);
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        
        this.repaintGUI();
    }
    
    public static SGSInstanceGUI getGUI(CStyxFile instanceRoot)
    {
        // Looks for the GUI in the cache, then returns it if it exists.
        // If it does not exist then create it.
        synchronized(guis)
        {
            if (guis.containsKey(instanceRoot))
            {
                return (SGSInstanceGUI)guis.get(instanceRoot);
            }
            else
            {
                // TODO: we might want to share the instance client with another
                // window (e.g. properties window) so we might not want to
                // create it here
                SGSInstanceClient client = new SGSInstanceClient(instanceRoot);
                SGSInstanceGUI gui = new SGSInstanceGUI(client);
                guis.put(instanceRoot, gui);
                return gui;
            }
        }
    }
    
    /**
     * Called when we have got the names of the service data elements
     * @param sdeNames The names of the SDEs as a String array
     */
    public void gotServiceDataNames(String[] sdeNames)
    {
        this.sdPanel.setServiceDataNames(sdeNames);
        // Now start reading all the SDEs. When a SDE changes, the 
        // serviceDataChanged() method will be called
        this.client.readAllServiceData();
    }
    
    /**
     * Called when the given service data element changes
     */
    public void serviceDataChanged(String sdName, String newData)
    {
        this.sdPanel.setSDEValue(sdName, newData);
    }
    
    /**
     * Called when we have got the names of the service data elements
     * @param inputMethods The input files (stdin and the input URL)
     */
    public void gotInputMethods(CStyxFile[] inputMethods)
    {
        this.inputPanel.setInputMethods(inputMethods);
    }
    
    /**
     * Called when we have discovered the input files that the service instance
     * expects.
     * @param inputFiles Array of CStyxFiles representing all the compulsory
     * input files that must be uploaded to the service
     * @param allowOtherInputFiles If true, we will have the option of uploading
     * other input files to the service instance
     */
    public void gotInputFiles(CStyxFile[] inputFiles, boolean allowOtherInputFiles)
    {
        // Just pass this on to the input files panel
        this.inputFilesPanel.gotInputFiles(inputFiles, allowOtherInputFiles);
    }
    
    /**
     * Forces the window to be laid out and packed
     */
    private void repaintGUI()
    {
        this.panelLayout.layoutContainer(this);
        this.pack();
    }
    
    /**
     * Called when an error occurs. Shows a dialog box with the message
     */
    public void error(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Called when the service is started
     */
    public void serviceStarted()
    {
        // Disable the start button here?  How do we know when the service
        // has finished so that we can enable it again?
    }
    
    /**
     * Called when the service is stopped before it has finished
     */
    public void serviceAborted() {}
    
    /**
     * Called when new data arrive from the standard output of the SGS instance.
     * After this method is called, the ByteBuffer will be released. If you 
     * want to prevent this, call newData.acquire().  When you have finished
     * with the data in the buffer, call newData.release().
     */
    public void newStdoutData(ByteBuffer newData) {}
    
    /**
     * Called when new data arrive from the standard error of the SGS instance.
     * After this method is called, the ByteBuffer will be released. If you 
     * want to prevent this, call newData.acquire().  When you have finished
     * with the data in the buffer, call newData.release().
     */
    public void newStderrData(ByteBuffer newData) {}
    
    /**
     * Panel for providing input data to the SGS
     */
    private class InputPanel extends JPanel
    {
        private TableLayout layout;
        private ButtonGroup btnGroup;
        
        public InputPanel()
        {
            double[][] size =
            {
                { 20, 0.25, BORDER, TableLayout.FILL, BORDER, 20 }, // Columns
                { }  // Rows - will be added later when setInputMethods() is called
            };
            this.layout = new TableLayout(size);
            this.setLayout(this.layout);
            
            this.btnGroup = new ButtonGroup();
            this.setBorder(BorderFactory.createTitledBorder("Select input method"));
            
            // Send message to find the input methods supported by this instance
            // When the reply arrives, the setInputMethods() method of this
            // class will be called and the GUI will be set up.
            client.getInputMethods();
        }
        
        public void setInputMethods(CStyxFile[] inputMethods)
        {
            // This method should only be called once so we don't need to look
            // for existing rows
            for (int i = 0; i < inputMethods.length; i++)
            {
                // Add a new row for the input method plus a border
                this.layout.insertRow(2 * i, ROW_HEIGHT);
                if (i < inputMethods.length - 1)
                {
                    this.layout.insertRow((2 * i) + 1, BORDER);
                }
                JRadioButton btn = new JRadioButton();
                this.btnGroup.add(btn);
                this.add(btn, "0, " + (2 * i));
                this.add(new JLabel(inputMethods[i].getName()), "1, " + (2 * i));
                this.add(new JTextField(), "3, " + (2 * i));
            }
            this.layout.layoutContainer(this);
        }
    }
    
    /**
     * Panel for allowing uploading of input files
     */
    private class InputFilesPanel extends JPanel implements ActionListener
    {
        private TableLayout layout;
        private JButton btnAddInputFile;
        private Vector fileLocations; // Vector of JTextFields
        private Vector pickFileButtons; // Vector of JButtons
        private Vector deleteRowButtons; // Vector of JButtons
        private int numCompulsoryFiles;
        
        public InputFilesPanel()
        {
            double[][] size = 
            {
                { TableLayout.PREFERRED, BORDER, 300, BORDER, 80, BORDER, 80 }, // columns
                { } // rows will be added later
            };
            this.layout = new TableLayout(size);
            this.setLayout(this.layout);
            this.setBorder(BorderFactory.createTitledBorder("Input files to upload"));
            // Send a message to get all the possible input files
            client.getInputFiles();
        }
        
        private void updateGUI()
        {
            this.layout.layoutContainer(this);
            repaintGUI();
        }
        
        /**
         * Called when we have found the input files required by this service
         */
        public void gotInputFiles(CStyxFile[] inputFiles, boolean allowOtherInputFiles)
        {
            this.numCompulsoryFiles = inputFiles.length;
            this.fileLocations = new Vector();
            this.pickFileButtons = new Vector();
            this.deleteRowButtons = new Vector();
            
            if (allowOtherInputFiles)
            {
                // create a row for the "more input files" button
                this.layout.insertRow(0, ROW_HEIGHT);
                this.layout.insertRow(1, BORDER);
                this.btnAddInputFile = new JButton("Add another input file");
                this.btnAddInputFile.addActionListener(this);
                this.add(btnAddInputFile, "0, 0, 2, 0, l, f");
            }
            
            for (int i = 0; i < inputFiles.length; i++)
            {
                this.addInputFileRow(inputFiles[i].getName(), false);
            }
            this.updateGUI();
        }
        
        private void addInputFileRow(String name, boolean deleteable)
        {
            int rowToAdd = this.layout.getNumRow();
            this.layout.insertRow(rowToAdd, ROW_HEIGHT);
            this.layout.insertRow(rowToAdd + 1, BORDER);
            System.err.println("Added row " + rowToAdd);

            this.add(new JLabel(name), "0, " + rowToAdd);

            JTextField ta = new JTextField();
            this.fileLocations.add(ta);
            this.add(ta, "2, " + rowToAdd);
            JButton btn = new JButton("Pick file");

            this.pickFileButtons.add(btn);
            btn.addActionListener(this);
            this.add(btn, "4, " + rowToAdd);
            
            /*if (deleteable)
            {
                JButton delBtn = new JButton("Remove");
                this.deleteRowButtons.add(delBtn);
                delBtn.addActionListener(this);
                this.add(delBtn, "6, " + rowToAdd);
            }*/
        }
        
        /**
         * Called when a button is pressed on the panel
         */
        public void actionPerformed(ActionEvent e)
        {
            if (e.getSource() == this.btnAddInputFile)
            {
                this.addInputFileRow("New file", true);
                this.updateGUI();
            }
            else
            {
                // See if we've clicked a "pick file button"
                int index = this.pickFileButtons.indexOf(e.getSource());
                if (index != -1)
                {
                    JFileChooser chooser = new JFileChooser();
                    int returnVal = chooser.showOpenDialog(this);
                    if (returnVal == JFileChooser.APPROVE_OPTION)
                    {
                        JTextField ta = (JTextField)this.fileLocations.get(index);
                        ta.setText(chooser.getSelectedFile().getPath());
                    }
                }
                else
                {
                    // We must have clicked a "remove" button
                    // TODO: make this work
                }
            }
        }
    }
    
    /**
     * Panel for displaying control buttons (start, stop)
     */
    private class ControlPanel extends JPanel implements ActionListener
    {
        private JButton btnStart;
        private JButton btnStop;
        
        public ControlPanel()
        {
            double size[][] =
            {
                { TableLayout.FILL, BORDER, TableLayout.FILL }, // Columns
                { TableLayout.FILL }  // Rows
            };
            this.setLayout(new TableLayout(size));
            this.btnStart = new JButton("Start");
            this.btnStop = new JButton("Stop");
            // Add the action listeners to both buttons
            this.btnStart.addActionListener(this);
            this.btnStop.addActionListener(this);
            // Add the buttons centred in the horizontal and fully-justified
            // in the vertical
            this.add(this.btnStart, "0, 0, c, f");
            this.add(this.btnStop, "2, 0, c, f");
        }
        
        /**
         * Called when a button on the panel is clicked
         */
        public void actionPerformed(ActionEvent e)
        {
            if (e.getSource() == this.btnStart)
            {
                // Upload the necessary input files
                client.uploadInputFile(new File("C:\\md5lines.py"), "new.txt");
                client.uploadInputFile(new File("C:\\md5lines.py"), "input.sim");
                //client.startService();
            }
            else if (e.getSource() == this.btnStop)
            {
                client.stopService();
            }
        }
    }
    
    /**
     * Panel for displaying service data
     */
    private class ServiceDataPanel extends JPanel
    {
        private JTable table;
        private SDTableModel model;

        /**
         * Creates a ServiceDataPanel based on the service data in the given
         * directory
         */
        public ServiceDataPanel()
        {
            this.model = new SDTableModel();
            this.table = new JTable(this.model);
            this.add(new JScrollPane(this.table));
            this.table.setPreferredScrollableViewportSize(new Dimension(500, 100));
            // Send message to find the service data elements
            client.getServiceDataNames();
        }

        /**
         * This is called when we have found the children of the service data
         * directory
         */
        public void setServiceDataNames(String[] sdeNames)
        {
            this.model.setSDENames(sdeNames);
        }
        
        /**
         * Sets the given service data element to the given value
         */
        public void setSDEValue(String sdeName, String value)
        {
            this.model.setSDEValue(sdeName, value);
        }
        
        /**
         * Table model for the service data
         */
        private class SDTableModel extends AbstractTableModel
        {
            private String[] sdeNames;
            private String[] sdeValues;
            
            public void setSDENames(String[] sdeNames)
            {
                this.sdeNames = sdeNames;
                this.sdeValues = new String[this.sdeNames.length];
                this.fireTableDataChanged();
            }
            
            public int getRowCount()
            {
                if (this.sdeNames == null)
                {
                    return 0;
                }
                else
                {
                    return this.sdeNames.length;
                }
            }
            
            public int getColumnCount()
            {
                return 2;
            }
            
            public Object getValueAt(int row, int column)
            {
                if (column == 0)
                {
                    return this.sdeNames[row];
                }
                else
                {
                    return this.sdeValues[row];
                }
            }
            
            /**
             * Set the given service data element to the given value
             */
            public void setSDEValue(String sdeName, String value)
            {
                for (int i = 0; i < this.sdeNames.length; i++)
                {
                    if (sdeName.equals(this.sdeNames[i]))
                    {
                        this.sdeValues[i] = value;
                        this.fireTableRowsUpdated(i, i);
                        break;
                    }
                }
            }
            
            public String getColumnName(int column)
            {
                if (column == 0)
                {
                    return "Service data element";
                }
                else
                {
                    return "Value";
                }
            }
        }
    }
    
}


