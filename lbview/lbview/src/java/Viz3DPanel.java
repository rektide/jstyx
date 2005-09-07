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
import javax.swing.filechooser.FileFilter;
import javax.swing.event.*;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.BevelBorder;
import info.clearthought.layout.*;
import LBStructures.*;

//TODO: add point sets

public class Viz3DPanel extends JPanel implements Terminable, ActionListener, ChangeListener {

  private static final int BORDER = 5;
  private static final int PANEL_WIDTH = 300;
  private static final Dimension PANEL_DIMS = new Dimension(PANEL_WIDTH, 1);
  
  private JData parent;
  private LBDomain dom;
  private File dom_file;
  //components
  private LBVTKPanel vtkDisplay;
  //file status bar
  private JLabel jlabel_file_path;
  //save image file
  private JFileChooser jfilechooser;
  private File image_file;
  //controls
  private JTabbedPane control_tabs;
    //info panel stuff
    private JPanel tab_jpanel_info;
    private JTextArea jtextarea_variables;
    //view panel stuff
    private CameraPanel tab_camera_panel;
    //panels for different variables
    //t (type data)
    private JPanel tab_jpanel_type;
    private JCheckBox jcheckbox_surface;
    private JSlider jslider_surface;
    //V (velocity vector data)
    private JPanel tab_jpanel_vels;
    private JCheckBox jcheckbox_vels;
    private JTextField jtextfield_length;
    private JButton jbutton_length_set;
    private JButton jbutton_length_update;
    private JCheckBox jcheckbox_scale_to_fit;
    private JCheckBox jcheckbox_streams;
  
  public Viz3DPanel(JData jdata, Dimension scrsize, LBDomain domain) {
    this.dom = domain;
    this.parent = jdata;
    //if these data are from a file, get the name of the file
    this.dom_file = this.parent.getDomFile();
    // Set the layout of the visualization panel
    double[][] size = {
      { BORDER, PANEL_WIDTH, BORDER, TableLayout.FILL, BORDER }, // columns
      { BORDER, TableLayout.FILL, 2, 18, BORDER }  // rows
    };
    TableLayout layout = new TableLayout(size);
    this.setLayout(layout);
    //add file path bar at the bottom
    this.jlabel_file_path = new JLabel();
    this.jlabel_file_path.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    if (this.dom_file != null) this.jlabel_file_path.setText(" FILE: " + this.dom_file.getAbsolutePath());
    else this.jlabel_file_path.setText(" Running in pipeline mode");
    this.add(this.jlabel_file_path, "1, 3, 3, 3");
    // create and add VTK panel
    this.vtkDisplay = new LBVTKPanel(scrsize, this.dom);
    this.add(this.vtkDisplay, "3, 1");
    //create the control panel
    this.control_tabs = new JTabbedPane();
    this.add(this.control_tabs, "1, 1");
    //create and add the information panel to the tabs
    this.control_tabs.addTab("Info", this.createInfoTab());
    //create and add the view control panel to the tabs
    this.tab_camera_panel = new CameraPanel(this);
    this.control_tabs.addTab("View", this.tab_camera_panel);
    //if there are any active variables, read these and add their control panel to the tabs
    this.addVariablesControls();
    //TODO: point sets will also be added via tabs
    
    
    //done (is the repaint needed?)
    super.repaint();
  }
  
  public JData getJData() {
    return this.parent;
  }
  
  public LBVTKPanel getVTKDisplay() {
    return this.vtkDisplay;
  }
  
  //TODO: sort out domainUpdate
  public void domainUpdate(LBDomain d) {
    this.dom = d;
    this.readVariables();
    this.addVariablesControls();
    this.vtkDisplay.domainUpdate(this.dom);
  }
  
  private JPanel createInfoTab() {
    // Set the layout of the info tab
    this.tab_jpanel_info = new JPanel();
    JPanel jpanel_lattice_info = new JPanel();
    jpanel_lattice_info.setBorder(new TitledBorder(new EtchedBorder(), "Lattice info"));
    JPanel jpanel_variables_info = new JPanel();
    jpanel_variables_info.setBorder(new TitledBorder(new EtchedBorder(), "Variables info"));
    //new table layout to hold information tab components
    double[][] jpanel_info_size = {
      { BORDER, TableLayout.FILL, BORDER}, // columns
      { BORDER, TableLayout.MINIMUM, BORDER, TableLayout.FILL, BORDER }  // rows
    };
    TableLayout jpanel_info_layout = new TableLayout(jpanel_info_size);
    this.tab_jpanel_info.setLayout(jpanel_info_layout);
    this.tab_jpanel_info.add(jpanel_lattice_info, "1, 1");
    this.tab_jpanel_info.add(jpanel_variables_info, "1, 3");
    //new table layout to hold lattice info panel components
    double[][] jpanel_lattice_size = {
      { BORDER, TableLayout.FILL, BORDER}, // columns
      { BORDER, 18, 2, 18, 2, 18, 2, 18, 2, 18, 2, 18, BORDER }  // rows
    };
    TableLayout jpanel_lattice_layout = new TableLayout(jpanel_lattice_size);
    jpanel_lattice_info.setLayout(jpanel_lattice_layout);
    //components of lattice info box
    JLabel jlabel_lattice = new JLabel("Lattice points [x, y, z]:");
    JLabel jlabel_dims = new JLabel(this.dom.getLattice().stringDims());
    JLabel jlabel_orig = new JLabel("Origin [x, y, z]:");
    JLabel jlabel_origin = new JLabel(this.dom.getLattice().stringOrigin());
    JLabel jlabel_phys = new JLabel("Physical dimensions [x, y, z]:");
    JLabel jlabel_pdims = new JLabel(this.dom.getLattice().stringPDims());
    //make these all a bit smaller
    jlabel_lattice.setFont(jlabel_lattice.getFont().deriveFont(Font.BOLD, 12));
    jlabel_dims.setFont(jlabel_dims.getFont().deriveFont(Font.PLAIN, 12));
    jlabel_orig.setFont(jlabel_orig.getFont().deriveFont(Font.BOLD, 12));
    jlabel_origin.setFont(jlabel_origin.getFont().deriveFont(Font.PLAIN, 12));
    jlabel_phys.setFont(jlabel_phys.getFont().deriveFont(Font.BOLD, 12));
    jlabel_pdims.setFont(jlabel_pdims.getFont().deriveFont(Font.PLAIN, 12));
    //add components to lattice info panel
    jpanel_lattice_info.add(jlabel_lattice, "1, 1");
    jpanel_lattice_info.add(jlabel_dims, "1, 3");
    jpanel_lattice_info.add(jlabel_orig, "1, 5");
    jpanel_lattice_info.add(jlabel_origin, "1, 7");
    jpanel_lattice_info.add(jlabel_phys, "1, 9");
    jpanel_lattice_info.add(jlabel_pdims, "1, 11");
    //new table layout to hold variables info panel components
    double[][] jpanel_variables_size = {
      { BORDER, TableLayout.FILL, BORDER}, // columns
      { BORDER, TableLayout.FILL, BORDER}  // rows
    };
    TableLayout jpanel_variables_layout = new TableLayout(jpanel_variables_size);
    jpanel_variables_info.setLayout(jpanel_variables_layout);
    //scrollpane with available variables and time of update
    this.jtextarea_variables = new JTextArea();
    this.jtextarea_variables.setRows(10);
    this.jtextarea_variables.setEditable(false);
    JScrollPane jscrollpane_variables = new JScrollPane(this.jtextarea_variables);
    this.readVariables();
    //add components to variables info panel
    jpanel_variables_info.add(jscrollpane_variables, "1, 1");
    
    //done
    return this.tab_jpanel_info;
  }
  
  private void readVariables() {
    this.jtextarea_variables.setText("Variables:\n----------\n");
    this.jtextarea_variables.append(this.dom.stringVariables());
  }
  
  private void addVariablesControls() {
    String labels = this.dom.getVariableLabels();
    if (labels.contains("t")) {
      if (this.tab_jpanel_type == null) {
        this.control_tabs.addTab("t", this.createTypePanel());
      }
    }
    if (labels.contains("V")) {
      if (this.tab_jpanel_vels == null) {
        this.control_tabs.addTab("V", this.createVelsPanel());
        //TODO: variable opacity so that other components can be seen?
      }
    }
    //if (labels.contains("whatever")) {
    //etc
    //}
    
    //this can't be the right way to force a repaint can it???
    this.parent.setVisible(true);
  }
  
  private JPanel createTypePanel() {
    //TODO: create surface, also surface from non-type data???
    this.tab_jpanel_type = new JPanel();
    //new table layout to hold type tab components
    double[][] tab_type_size = {
      { BORDER, TableLayout.FILL, BORDER}, // columns
      { BORDER, TableLayout.MINIMUM, BORDER, TableLayout.MINIMUM, BORDER }  // rows
    };
    TableLayout tab_type_layout = new TableLayout(tab_type_size);
    this.tab_jpanel_type.setLayout(tab_type_layout);
    //show/hide surface
    this.jcheckbox_surface = new JCheckBox("Surface");
    this.jcheckbox_surface.addActionListener(this);
    this.tab_jpanel_type.add(this.jcheckbox_surface, "1, 1");
    //surface opacity slider
    this.jslider_surface = new JSlider(JSlider.HORIZONTAL, 0, 100, 20);
    this.jslider_surface.setMajorTickSpacing(100);
    this.jslider_surface.setMinorTickSpacing(10);
    this.jslider_surface.setPaintTicks(true);
    this.jslider_surface.addChangeListener(this);
    this.tab_jpanel_type.add(this.jslider_surface, "1, 3");
    return this.tab_jpanel_type;
  }
  
  private JPanel createVelsPanel() {
    this.tab_jpanel_vels = new JPanel();
    //new table layout to hold vels tab components
    double[][] tab_vels_size = {
      { BORDER, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, 2, BORDER}, // columns
      { BORDER, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, BORDER }  // rows
    };
    TableLayout tab_vels_layout = new TableLayout(tab_vels_size);
    this.tab_jpanel_vels.setLayout(tab_vels_layout);
    //show/hide velocity cones
    this.jcheckbox_vels = new JCheckBox("Velocities");
    this.jcheckbox_vels.addActionListener(this);
    this.tab_jpanel_vels.add(this.jcheckbox_vels, "1, 1, 3, 1");
    //control length of cones
    JLabel jlabel_length = new JLabel("Vector length");
    this.tab_jpanel_vels.add(jlabel_length, "1, 3");
    this.jtextfield_length = new JTextField("1");
    this.tab_jpanel_vels.add(this.jtextfield_length, "3, 3, 5, 3");
    //set length
    this.jbutton_length_set = new JButton("Set");
    this.jbutton_length_set.addActionListener(this);
    this.tab_jpanel_vels.add(this.jbutton_length_set, "1, 5");
    //update length
    this.jbutton_length_update = new JButton("Update");
    this.jbutton_length_update.addActionListener(this);
    this.tab_jpanel_vels.add(this.jbutton_length_update, "3, 5");
    //scale vectors to lattice spacing?
    this.jcheckbox_scale_to_fit = new JCheckBox("Scale to dx", true);
    this.jcheckbox_scale_to_fit.addActionListener(this);
    this.tab_jpanel_vels.add(this.jcheckbox_scale_to_fit, "5, 5");
    //velocity streamline controls
    this.jcheckbox_streams = new JCheckBox("Streamlines");
    this.jcheckbox_streams.addActionListener(this);
    this.tab_jpanel_vels.add(this.jcheckbox_streams, "1, 7, 5, 7");
    return this.tab_jpanel_vels;
  }

  //must implement actionPerformed()
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(this.jcheckbox_surface)) {
      //vtkDisplay.setVels(0, -1, 0);
    }
    if (e.getSource().equals(this.jcheckbox_vels)) {
      //TODO: change to toggle velocity
      this.vtkDisplay.toggleVels();
    }
    if (e.getSource().equals(this.jbutton_length_set)) {
    //System.out.println(this.jtextfield_length.getText());
      this.vtkDisplay.setVectorLength(Double.parseDouble(this.jtextfield_length.getText()));
    }
    if (e.getSource().equals(this.jbutton_length_update)) {
      this.jtextfield_length.setText(String.format("%f", this.vtkDisplay.getVectorLength()));
    }
    if (e.getSource().equals(this.jcheckbox_scale_to_fit)) {
      this.jtextfield_length.setText(String.format("%f", this.vtkDisplay.toggleScaleToFit(Double.parseDouble(this.jtextfield_length.getText()))));
    }
    if (e.getSource().equals(this.jcheckbox_streams)) {
      //vtkDisplay.setVels(0, -1, 0);
    }
  }
  
  //must implement stateChanged()
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(this.jslider_surface)) {
      if (!this.jslider_surface.getValueIsAdjusting()) {
        //this.vtkDisplay.setSurfaceOpacity((int)this.jslider_surface.getValue());
      }
    }
  }
  
  //must implement terminate() and isTerminated()
  public void terminate() {
    //this should release all the old actors etc from vtkpanel
    //System.out.println("viz dying");
  }
  public boolean isTerminated() {
    return true;
  }
  
  //standard save procedure: makes appropriate checks before saving
  //note that this version never automatically writes over a file
  public void saveImage(int image_file_type) {
    switch(image_file_type) {
      case ExtensionFilter.JPEG:  //open image dialog to gather information for image writer
                                  JPEGDialog jpeg_dialog = new JPEGDialog();
                                  //if jpeg dialog is null, operation was cancelled or closed
                                  if (jpeg_dialog.isDisplayable()) {
                                    //save operation
                                    this.vtkDisplay.saveImageJPG(this.image_file.getAbsolutePath(),
                                                                 jpeg_dialog.getMagnification(),
                                                                 jpeg_dialog.getQuality());
                                    this.vtkDisplay.setCameraPosition(this.vtkDisplay.getCameraPosition());
                                    //tidy up
                                    jpeg_dialog.dispose();
                                  }
                                  break;
      case ExtensionFilter.STL:   this.vtkDisplay.saveImageSTL(this.image_file.getAbsolutePath());
                                  break;
      case ExtensionFilter.EPS:   this.vtkDisplay.saveImagePostScript(this.image_file.getAbsolutePath());
                                  break;
    }
  }
  
  public void saveImageAs() {
    if (this.image_file == null || !this.image_file.isFile()) {
      this.jfilechooser = new JFileChooser(JData.DEFAULT_DATA_DIR);
    }
    else this.jfilechooser = new JFileChooser(this.image_file);
    //set file filter
    //warning, eps is rubbish! TODO: find a vector output if there is one (maybe just .vtk)
    ExtensionFilter eps_filter = new ExtensionFilter(".eps", "PostScript files (*.eps)");
    ExtensionFilter stl_filter = new ExtensionFilter(".stl", "Vector files (*.stl)");
    ExtensionFilter jpeg_filter = new ExtensionFilter(".jpg", "JPEG files (*.jpg)");
    this.jfilechooser.addChoosableFileFilter(eps_filter);
    //this one doesn't work at the moment
    //this.jfilechooser.addChoosableFileFilter(stl_filter);
    this.jfilechooser.addChoosableFileFilter(jpeg_filter);
    this.jfilechooser.setFileFilter(jpeg_filter);
    this.jfilechooser.setAcceptAllFileFilterUsed(false);
    int image_file_type = -1;
    if (this.jfilechooser.showSaveDialog(this) == this.jfilechooser.APPROVE_OPTION) {
      if (this.jfilechooser.getFileFilter() == jpeg_filter) image_file_type = ExtensionFilter.JPEG;
      if (this.jfilechooser.getFileFilter() == stl_filter) image_file_type = ExtensionFilter.STL;
      if (this.jfilechooser.getFileFilter() == eps_filter) image_file_type = ExtensionFilter.EPS;
      this.image_file = this.jfilechooser.getSelectedFile();
      this.jfilechooser = null;
      if (this.image_file.isFile()) {
        //file already exists, do you want to overwrite?
        String warning_message = "";
        try {
          warning_message += this.image_file.toURL() + " exists. Overwrite?";
        } catch (Exception e) {
          System.out.println("Viz3DPanel.saveImageAs()");
          e.printStackTrace();
        }
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                                                                    warning_message,
                                                                    "Confirm overwrite",
                                                                    JOptionPane.YES_NO_OPTION,
                                                                    JOptionPane.WARNING_MESSAGE)) {
          //yes, we do want to overwrite
          this.saveImage(image_file_type);
        }
        //no, we don't want to overwrite
        else this.saveImageAs();
      }
      //must be that file or its parent directories don't exist
      else {
        this.image_file = this.image_file.getAbsoluteFile();
        File parent_directory = new File(this.image_file.getParent());
        if (!parent_directory.exists()) parent_directory.mkdirs();
        this.saveImage(image_file_type);
      }
    }
  }
  
  //file filter class
  public class ExtensionFilter extends FileFilter {
  
    public static final int JPEG = 0;
    public static final int STL = 1;
    public static final int EPS = 2;
  
    private String extension;
    private String description;
  
    public ExtensionFilter(String ext, String desc) {
      extension = ext.toLowerCase();
      description = desc;
    }
    
    //override two functions
    public boolean accept(File file) {
      return (file.isDirectory() || file.getName().toLowerCase().endsWith(extension));
    }
    
    public String getDescription() {
      return description;
    }
  
  }
  
  //JPEG dialog class
  public class JPEGDialog extends JDialog implements ActionListener {
  
    private JLabel jlabel_magnification;
    private JSpinner jspinner_magnification;
    private JLabel jlabel_quality;
    private JSlider jslider_quality;
    private JButton jbutton_ok;
    private JButton jbutton_cancel;
  
    public JPEGDialog() {
      super(parent, "JPEG parameters", true);
      //set the layout of the info panel
      double[][] size = {
        {BORDER, TableLayout.FILL, 2, TableLayout.FILL, BORDER }, // columns
        {BORDER, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, BORDER}  // rows
      };
      TableLayout layout = new TableLayout(size);
      Container content = this.getContentPane();
      content.setLayout(layout);
      //create and add components
      //image magnification (TODO: should have tooltip warning about image jumping
      this.jlabel_magnification = new JLabel("Magnification:");
      this.add(this.jlabel_magnification, "1, 1");
      this.jspinner_magnification = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
      this.add(this.jspinner_magnification, "3, 1");
      //jpeg quality
      this.jlabel_quality = new JLabel("JPEG quality");
      this.add(this.jlabel_quality, "1, 3");
      this.jslider_quality = new JSlider(JSlider.HORIZONTAL, 10, 100, 50);
      this.jslider_quality.setMajorTickSpacing(90);
      this.jslider_quality.setMinorTickSpacing(10);
      this.jslider_quality.setPaintTicks(true);
      this.jslider_quality.setPaintLabels(true);
      this.add(this.jslider_quality, "3, 3");
      //control buttons
      this.jbutton_ok = new JButton("OK");
      this.jbutton_ok.addActionListener(this);
      this.add(this.jbutton_ok, "1, 5");
      this.jbutton_cancel = new JButton("Cancel");
      this.jbutton_cancel.addActionListener(this);
      this.add(this.jbutton_cancel, "3, 5");
      //make it so!
      //set default close operation
      super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      super.pack();
      //set position of the dialog
      Dimension parent_dimension = parent.getSize();
      Dimension dialog_dimension = super.getSize();
      Point parent_location = parent.getLocation();
      super.setLocation(parent_location.x + (parent_dimension.width - dialog_dimension.width) / 2,
                        parent_location.y + (parent_dimension.height - dialog_dimension.height) / 2);
      super.setVisible(true);
    }
    
    public int getMagnification() {
      return (Integer)this.jspinner_magnification.getValue();
    }
    
    public int getQuality() {
      return this.jslider_quality.getValue();
    }
    
    public void dispose() {
      super.setVisible(false);
      super.dispose();
    }
    
    //must implement actionPerformed()
    public void actionPerformed(ActionEvent e) {
      if (e.getSource().equals(this.jbutton_ok)) {
        this.setVisible(false);
      }
      if (e.getSource().equals(this.jbutton_cancel)) {
        this.dispose();
      }
    }
  
  }
  
}