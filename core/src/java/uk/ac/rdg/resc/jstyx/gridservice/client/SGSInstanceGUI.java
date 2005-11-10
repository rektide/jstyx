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
import java.io.IOException;

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
import javax.swing.JComboBox;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.AbstractTableModel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.BorderLayout;

import java.util.Vector;
import java.util.Enumeration;

import info.clearthought.layout.TableLayout;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxException;
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
 * Revision 1.25  2005/11/10 19:49:28  jonblower
 * Renamed SGSInstanceChangeListener to SGSInstanceClientChangeListener
 *
 * Revision 1.24  2005/10/18 14:08:14  jonblower
 * Removed inputfiles from namespace
 *
 * Revision 1.23  2005/10/14 18:09:40  jonblower
 * Changed getInputMethods() to getInputStreams() and added synchronous and async versions
 *
 * Revision 1.22  2005/09/23 09:18:38  jonblower
 * Removed LBGui stuff and hence dependence on VTK libs
 *
 * Revision 1.21  2005/09/11 19:30:40  jonblower
 * Changed call to readAllSteeringParams() to readAllSteeringParamsAsync()
 *
 * Revision 1.20  2005/09/09 16:34:03  jonblower
 * Removed LBViewer as possible stream viewer
 *
 * Revision 1.19  2005/09/09 14:19:35  jonblower
 * Created populatePanel() methods in panel implementations to send messages to get panel details
 *
 * Revision 1.18  2005/08/12 08:08:39  jonblower
 * Developments to support web interface
 *
 * Revision 1.17  2005/08/04 16:49:18  jonblower
 * Added and edited upload() methods in CStyxFile
 *
 * Revision 1.15  2005/08/01 16:38:05  jonblower
 * Implemented simple parameter handling
 *
 * Revision 1.14  2005/07/29 16:55:49  jonblower
 * Implementing reading command line asynchronously
 *
 * Revision 1.13  2005/06/14 07:45:16  jonblower
 * Implemented setting of params and async notification of parameter changes
 *
 * Revision 1.12  2005/06/13 16:46:35  jonblower
 * Implemented setting of parameter values via the GUI
 *
 * Revision 1.11  2005/06/10 07:54:40  jonblower
 * Added code to convert event-based StreamViewer to InputStream-based one
 *
 * Revision 1.10  2005/06/07 16:44:45  jonblower
 * Fixed problem with caching stream reader on client side
 *
 * Revision 1.9  2005/05/27 17:05:07  jonblower
 * Changes to incorporate GeneralCachingStreamReader
 *
 * Revision 1.8  2005/05/27 07:44:07  jonblower
 * Continuing to implement Stream viewers
 *
 * Revision 1.7  2005/05/26 21:33:40  jonblower
 * Added method for viewing streams in a window
 *
 * Revision 1.6  2005/05/26 16:52:06  jonblower
 * Implemented detection and viewing of output streams
 *
 * Revision 1.5  2005/05/25 16:59:31  jonblower
 * Added uploadInputFile()
 *
 * Revision 1.4  2005/05/20 16:28:50  jonblower
 * Continuing to implement GUI app
 *
 */
public class SGSInstanceGUI extends JFrame implements SGSInstanceClientChangeListener
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
    private ParamsPanel paramsPanel; // Panel for setting input parameters
    private SteeringPanel steeringPanel; // Panel for doing computational steering
    private ControlPanel ctlPanel; // Panel for controlling the service instance
    private ServiceDataPanel sdPanel; // Panel for showing service data
    private OutputStreamsPanel osPanel; // Panel for interacting with output streams
    
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
                  TableLayout.PREFERRED, BORDER, TableLayout.PREFERRED, BORDER,
                  ROW_HEIGHT, BORDER, TableLayout.FILL, BORDER,
                  TableLayout.PREFERRED, BORDER}  // Rows
        };
        this.panelLayout = new TableLayout(size);
        this.masterPanel.setLayout(this.panelLayout);
        
        // Add the input panel
        this.inputPanel = new InputPanel();
        this.inputPanel.populatePanel();
        this.masterPanel.add(this.inputPanel, "1, 1");
        
        // Add the panel for uploading input files
        this.inputFilesPanel = new InputFilesPanel();
        this.inputFilesPanel.populatePanel();
        //this.masterPanel.add(this.inputFilesPanel, "1, 3");
        
        // Add the panel for setting parameters for the SGS
        this.paramsPanel = new ParamsPanel();
        this.paramsPanel.populatePanel();
        this.masterPanel.add(this.paramsPanel, "1, 5");
        
        // Add the panel for steering the SGS
        this.steeringPanel = new SteeringPanel();
        this.steeringPanel.populatePanel();
        this.masterPanel.add(this.steeringPanel, "1, 7");
        
        // Add the control panel
        this.ctlPanel = new ControlPanel();
        this.masterPanel.add(this.ctlPanel, "1, 9");
        
        // Add the service data panel
        this.sdPanel = new ServiceDataPanel();
        this.sdPanel.populatePanel();
        this.masterPanel.add(this.sdPanel, "1, 11");
        
        // Add the output streams panel
        this.osPanel = new OutputStreamsPanel();
        this.osPanel.populatePanel();
        this.masterPanel.add(this.osPanel, "1, 13");
        
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
    public void gotServiceDataElements(CStyxFile[] sdeFiles)
    {
        this.sdPanel.setServiceDataNames(sdeFiles);
    }
    
    /**
     * Called when the given service data element changes
     */
    public void serviceDataChanged(String sdName, String newData)
    {
        this.sdPanel.setSDEValue(sdName, newData);
    }
    
    /**
     * Called when we have got the names of the input streams
     * @param inputMethods The input files (stdin and the input URL)
     */
    public void gotInputStreams(CStyxFile[] inputStreams)
    {
        this.inputPanel.setInputStreams(inputStreams);
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
     * Called when we have got the output streams that can be viewed
     * @param outputStreams Array of CStyxFiles representing the output streams
     */
    public void gotOutputStreams(CStyxFile[] outputStreams)
    {
        this.osPanel.gotOutputStreams(outputStreams);
    }
    
    /**
     * Called when we have got the list of parameters expected by the SGS
     * @param paramFiles CStyxFiles representing the parameters
     */
    public void gotParameters(CStyxFile[] paramFiles)
    {
        this.paramsPanel.gotParameters(paramFiles);
    }
    
    /**
     * Called when we have a new value for a parameter
     * @param index Index of the parameter in the array of parameters previously
     * returned by the gotParameters() event
     * @param value The new value of the parameter
     */
    public void gotParameterValue(int index, String value)
    {
        this.paramsPanel.gotParameterValue(index, value);
    }
    
    /**
     * Called when we have a new command line string
     * @param newCmdLine The new command line
     */
    public void gotCommandLine(String newCmdLine)
    {
        this.paramsPanel.setCommandLine(newCmdLine);
    }
    
    /**
     * Called when we have got the list of steerable parameters
     * @param steerableFiles CStyxFiles representing the parameters
     */
    public void gotSteerableParameters(CStyxFile[] steerableFiles)
    {
        this.steeringPanel.gotSteerableParameters(steerableFiles);
    }
    
    /**
     * Called when we have a new value for a steerable parameter
     * @param index Index of the parameter in the array of parameters previously
     * returned by the gotParameters() event
     * @param value The new value of the parameter
     */
    public void gotSteerableParameterValue(int index, String value)
    {
        this.steeringPanel.gotSteeringParameterValue(index, value);
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
     * Called when we have successfully set the input URL of the SGS.
     */
    public void inputURLSet()
    {
        // Upload the necessary input files. When these are uploaded
        // we will start the service
        File[] srcFiles = inputFilesPanel.getSourceFiles();
        String[] targetNames = inputFilesPanel.getTargetFileNames();
        //client.uploadInputFiles(srcFiles, targetNames);
    }
    
    /**
     * Called when the input files have been successfully uploaded.  This is our
     * cue to start the service going
     * @todo: add arguments to this
     */
    public void inputFilesUploaded()
    {
        this.client.startServiceAsync();
    }
    
    /**
     * Called when the service is stopped before it has finished
     */
    public void serviceAborted() {}
    
    /**
     * Panel for providing input data to the SGS
     */
    private class InputPanel extends JPanel implements ActionListener
    {
        private TableLayout layout;
        private JTextField inputURL;
        private JButton btnPickFile;
        
        public void populatePanel()
        {
            // Send message to find the input methods supported by this instance
            // When the reply arrives, the setInputMethods() method of this
            // class will be called and the GUI will be set up.  If there are
            // no input methods this panel will not appear
            client.getInputStreamsAsync();
        }
        
        public void setInputStreams(CStyxFile[] inputStreams)
        {
            // We only expect 0 or 1 input methods (either the service expects
            // data on stdin or it doesn't).
            if (inputStreams.length > 0)
            {
                double[][] size =
                {
                    { TableLayout.FILL, BORDER, TableLayout.PREFERRED }, // Columns
                    { ROW_HEIGHT }  // Rows - will be added later when setInputMethods() is called
                };
                this.layout = new TableLayout(size);
                this.setLayout(this.layout);

                this.setBorder(BorderFactory.createTitledBorder("File to stream to stdin"));
                
                this.inputURL = new JTextField();
                this.add(this.inputURL, "0, 0");
                
                this.btnPickFile = new JButton("Pick file");
                this.btnPickFile.addActionListener(this);
                this.add(this.btnPickFile, "2, 0");
            }
        }
        
        public void actionPerformed(ActionEvent e)
        {
            if (e.getSource() == this.btnPickFile)
            {
                JFileChooser chooser = new JFileChooser();
                int returnVal = chooser.showOpenDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    try
                    {
                        this.inputURL.setText(chooser.getSelectedFile().toURL().toString());
                    }
                    catch(java.net.MalformedURLException mue)
                    {
                        // This shouldn't happen - what should we do here?
                    }
                }
            }
        }
        
        /**
         * Return the input URL that has been set, or null if we haven't set
         * a URL
         */
        public String getInputURL()
        {
            return this.inputURL == null ? null : this.inputURL.getText();
        }
    }
    
    /**
     * Panel for allowing uploading of input files
     */
    private class InputFilesPanel extends JPanel implements ActionListener
    {
        private TableLayout layout;
        private JButton btnAddInputFile;
        private Vector destFileNames; // Vector of JTextFields
        private Vector srcFileLocations; // Vector of JTextFields
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
        }
        
        public void populatePanel()
        {
            // Send a message to get all the possible input files
            //client.getInputFiles();
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
            this.destFileNames = new Vector();
            this.srcFileLocations = new Vector();
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
        
        private synchronized void addInputFileRow(String name, boolean editable)
        {
            int rowToAdd = this.layout.getNumRow();
            this.layout.insertRow(rowToAdd, ROW_HEIGHT);
            this.layout.insertRow(rowToAdd + 1, BORDER);

            JTextField destFile = new JTextField(name);
            if (!editable)
            {
                destFile.setEditable(false);
            }
            this.destFileNames.add(destFile);
            this.add(destFile, "0, " + rowToAdd);

            JTextField ta = new JTextField();
            this.srcFileLocations.add(ta);
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
         * Gets an array of File objects representing the source files to
         * be uploaded
         */
        public synchronized File[] getSourceFiles()
        {
            File[] srcFiles = new File[this.srcFileLocations.size()];
            for (int i = 0; i < this.srcFileLocations.size(); i++)
            {
                JTextField tf = (JTextField)this.srcFileLocations.get(i);
                srcFiles[i] = new File(tf.getText());
            }
            return srcFiles;
        }
        
        /**
         * Gets an array of Strings representing the names of the target files
         * on the remote server
         */
        public synchronized String[] getTargetFileNames()
        {
            String[] destNames = new String[this.destFileNames.size()];
            for (int i = 0; i < this.destFileNames.size(); i++)
            {
                JTextField tf = (JTextField)this.destFileNames.get(i);
                destNames[i] = tf.getText();
            }
            return destNames;
        }
        
        /**
         * Called when a button is pressed on the panel
         */
        public void actionPerformed(ActionEvent e)
        {
            if (e.getSource() == this.btnAddInputFile)
            {
                this.addInputFileRow("", true);
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
                        JTextField ta = (JTextField)this.srcFileLocations.get(index);
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
     * Panel for entering parameters for the SGS
     */
    private class ParamsPanel extends JPanel
    {
        private JTable table;
        private TableLayout layout;
        private ParamsTableModel model;
        private JLabel cmdLineLabel;
        
        public ParamsPanel()
        {
            this.cmdLineLabel = new JLabel("Command line: ");
        }
        
        public void populatePanel()
        {
            client.readAllParametersAsync();
            client.getCommandLineAsync();
        }
        
        public void gotParameters(CStyxFile[] paramFiles)
        {
            // We won't bother displaying the panel if the SGS doesn't expect
            // any parameters
            if (paramFiles.length > 0)
            {
                double[][] size = 
                {
                    { TableLayout.PREFERRED }, // columns
                    { TableLayout.PREFERRED, BORDER, ROW_HEIGHT} // rows
                };
                this.layout = new TableLayout(size);
                this.setLayout(this.layout);
                this.setBorder(BorderFactory.createTitledBorder("Parameters"));

                this.model = new ParamsTableModel(paramFiles);
                this.table = new JTable(this.model);
                this.add(new JScrollPane(this.table), "0, 0");
                this.add(this.cmdLineLabel, "0, 2");

                for (int i = 0; i < paramFiles.length; i++)
                {
                    this.table.getModel().setValueAt(paramFiles[i].getName(), i, 0);
                }

                double width = this.table.getPreferredScrollableViewportSize().getWidth();
                double height = this.table.getRowHeight() * paramFiles.length;
                Dimension d = new Dimension();
                d.setSize(width, height);
                this.table.setPreferredScrollableViewportSize(d);
                this.repaint();
            }
            else
            {
                double[][] size = 
                {
                    { TableLayout.PREFERRED }, // columns
                    { ROW_HEIGHT} // rows
                };
                this.layout = new TableLayout(size);
                this.setLayout(this.layout);
                this.setBorder(BorderFactory.createTitledBorder("Parameters"));
                this.add(this.cmdLineLabel, "0, 0");
            }
            this.layout.layoutContainer(this);
            repaintGUI();
        }
        
        public void gotParameterValue(int index, String value)
        {
            this.model.setParameterValue(index, value);
        }
        
        public void setCommandLine(String newCmdLine)
        {
            this.cmdLineLabel.setText("Command line: " + newCmdLine);
            this.repaint();
        }
    }
        
    /**
     * Table model for the parameters
     */
    private class ParamsTableModel extends DefaultTableModel
    {
        private CStyxFile[] paramFiles;

        public ParamsTableModel(CStyxFile[] paramFiles)
        {
            this.paramFiles = paramFiles;

            // Set the column names
            this.setColumnIdentifiers(new String[]{"Parameter", "Value"});

            // Add the row data
            for (int i = 0; i < paramFiles.length; i++)
            {
                this.addRow(new String[]{"", ""});
            }
        }

        public void setParameterValue(int index, String value)
        {
            super.setValueAt(value, index, 1);
        }

        /**
         * We override this method so that we can see if the server allows
         * changes to the parameter value to be made
         */
        public void setValueAt(Object value, int row, int col)
        {
            if (col == 0)
            {
                // Just allow setting of parameter names
                super.setValueAt(value, row, col);
            }
            else
            {
                // We're setting a parameter value. Send a message with the
                // new value of the parameter. If the write is successful,
                // the gotParameterValue() method will be called automatically
                // TODO: should be setContentsAsync() in case the value spans
                // multiple messages (very unlikely!)
                this.paramFiles[row].writeAsync((String)value, 0);
            }
        }

        /**
         * Only the "Value" column is editable
         */
        public boolean isCellEditable(int row, int col)
        {
            // TODO: make non-editable once service has started (and if the
            // parameters aren't steerable)
            return (col == 1);
        }
    }
    
    /**
     * Panel for entering steering parameters for the SGS
     */
    private class SteeringPanel extends JPanel
    {
        private JTable table;
        private TableLayout layout;
        private ParamsTableModel model;
    
        public void populatePanel()
        {
            client.readAllSteeringParamsAsync();
        }
        
        public void gotSteerableParameters(CStyxFile[] steeringFiles)
        {
            // We won't bother displaying the panel if the SGS doesn't expect
            // any parameters
            if (steeringFiles.length > 0)
            {
                double[][] size = 
                {
                    { TableLayout.PREFERRED }, // columns
                    { TableLayout.PREFERRED } // rows
                };
                this.layout = new TableLayout(size);
                this.setLayout(this.layout);
                this.setBorder(BorderFactory.createTitledBorder("Steering"));

                this.model = new ParamsTableModel(steeringFiles);
                this.table = new JTable(this.model);
                this.add(new JScrollPane(this.table), "0, 0");

                for (int i = 0; i < steeringFiles.length; i++)
                {
                    this.table.getModel().setValueAt(steeringFiles[i].getName(), i, 0);
                }

                double width = this.table.getPreferredScrollableViewportSize().getWidth();
                double height = this.table.getRowHeight() * steeringFiles.length;
                Dimension d = new Dimension();
                d.setSize(width, height);
                this.table.setPreferredScrollableViewportSize(d);
                this.repaint();
            }
        }
        
        public void gotSteeringParameterValue(int index, String value)
        {
            this.model.setParameterValue(index, value);
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
                // Send the input URL to the SGS, or start reading from the
                // local file
                String inputURL = inputPanel.getInputURL();
                if (inputURL == null)
                {
                    // We haven't set an input URL.  Just go straight to uploading
                    // the input files (normally we do this when confirmation that
                    // the input URL has been set arrives
                    inputURLSet();
                }
                else if (inputPanel.getInputURL().startsWith("file:/"))
                {
                    // TODO: implement reading from local file
                    JOptionPane.showMessageDialog(null,
                        "Uploading from local file not yet supported",
                        "Not supported", JOptionPane.ERROR_MESSAGE);
                }
                else
                {
                    // Set the input URL for the service.  When we get confirmation
                    // that this has been set, the input files will be uploaded.
                    client.setInputURL(inputPanel.getInputURL());
                }
            }
            else if (e.getSource() == this.btnStop)
            {
                client.stopServiceAsync();
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
        
        public void populatePanel()
        {
            // Send message to find the service data elements
            client.getServiceDataNames();
        }

        /**
         * This is called when we have found the children of the service data
         * directory
         */
        public void setServiceDataNames(CStyxFile[] sdeFiles)
        {
            if (sdeFiles.length > 0)
            {
                this.setBorder(BorderFactory.createTitledBorder("Service data"));
                
                this.model = new SDTableModel();
                this.table = new JTable(this.model);
                this.add(new JScrollPane(this.table));
                this.model.setSDENames(sdeFiles);

                // Set the dimensions of the table
                double width = this.table.getPreferredScrollableViewportSize().getWidth();
                double height = this.table.getRowHeight() * sdeFiles.length;
                Dimension d = new Dimension();
                d.setSize(width, height);
                this.table.setPreferredScrollableViewportSize(d);
            }
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
            
            public void setSDENames(CStyxFile[] sdeFiles)
            {
                this.sdeNames = new String[sdeFiles.length];
                for (int i = 0; i < sdeFiles.length; i++)
                {
                    this.sdeNames[i] = sdeFiles[i].getName();
                }
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
    
    /**
     * Panel for displaying data from output streams
     */
    private class OutputStreamsPanel extends JPanel implements ActionListener
    {
        private TableLayout layout;
        private Vector streams; // CStyxFiles
        private Vector buttons; // JButtons
        private Vector combos;  // JComboBoxes
        private Hashtable viewers; // Keys are Strings, values are Classes
        
        public OutputStreamsPanel()
        {
            double[][] size =
            {
                { TableLayout.PREFERRED, BORDER, TableLayout.PREFERRED, BORDER,
                     TableLayout.FILL }, // columns
                {} // We'll add rows later
            };
            this.layout = new TableLayout(size);
            this.setLayout(this.layout);
            this.setBorder(BorderFactory.createTitledBorder("Available output streams"));
            this.streams = new Vector();
            this.buttons = new Vector();
            this.combos = new Vector();
            // Create the hashtable of possible viewing panels
            this.viewers = new Hashtable();
            this.viewers.put("Text Viewer", TextStreamViewer.class);
        }
        
        public void populatePanel()
        {
            // Send a message to get the possible output streams
            client.getOutputStreamsAsync();
        }
        
        private JComboBox makeComboBox()
        {
            JComboBox combo = new JComboBox();
            Enumeration keys = this.viewers.keys();
            while(keys.hasMoreElements())
            {
                combo.addItem(keys.nextElement());
            }
            combo.setSelectedIndex(0);
            return combo;
        }
        
        /**
         * Called when we have got the possible output streams
         */
        public void gotOutputStreams(CStyxFile[] outputStreams)
        {
            for (int i = 0; i < outputStreams.length; i++)
            {
                this.layout.insertRow(2 * i, ROW_HEIGHT);
                this.layout.insertRow((2 * i) + 1, BORDER);
                this.streams.add(outputStreams[i]);
                this.add(new JLabel(outputStreams[i].getName()), "0, " + 2*i);
                JComboBox combo = makeComboBox();
                this.combos.add(combo);
                this.add(combo, "2, " + 2*i);
                JButton btnView = new JButton("View stream");
                btnView.addActionListener(this);
                this.buttons.add(btnView);
                this.add(btnView, "4, " + 2*i + ", c, f");
            }
            this.layout.layoutContainer(this);
            repaintGUI();
        }
        
        public void actionPerformed(ActionEvent e)
        {
            int i = this.buttons.indexOf(e.getSource());
            if (i != -1)
            {
                CStyxFile streamFile = (CStyxFile)this.streams.get(i);
                // Get the CachedStreamReader for this file and start it
                CachedStreamReader reader = client.getStreamReader(streamFile);
                try
                {
                    reader.start();
                    // find out which viewer has been selected
                    JComboBox combo = (JComboBox)this.combos.get(i);
                    Object key = combo.getSelectedItem();
                    Class viewerClass = (Class)this.viewers.get(key);
                    StreamViewer viewer = (StreamViewer)viewerClass.newInstance();
                    viewer.setStreamReader(reader);
                    viewer.start();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace(); // TODO do something better here
                }
                catch (InstantiationException ie)
                {
                    ie.printStackTrace(); // TODO do something better here
                }
                catch (IllegalAccessException iae)
                {
                    iae.printStackTrace();
                }
            }
        }
    }
    
}


