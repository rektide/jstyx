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
import java.nio.*;
import java.nio.channels.FileChannel;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import info.clearthought.layout.*;
import java.util.HashMap;
import java.util.Vector;
import java.util.Collection;

//TODO: implement removal of steered variables

public class SteeringPanel extends JPanel implements Terminable {

  private JData parent;
  
  private JTabbedPane steering_tabs;
  
  private HashMap<String, SteeredVariable> steered_variables;

  public SteeringPanel(JData jdata) {
    this.parent = jdata;
    //create hashmap
    this.steered_variables = new HashMap<String, SteeredVariable>(5);
    // Set the layout of the steering panel
    double[][] size = {
      { 2, 200 }, // columns
      {TableLayout.FILL}  // rows
    };
    TableLayout layout = new TableLayout(size);
    this.setLayout(layout);
    //create the tab pane
    this.steering_tabs = new JTabbedPane();
    this.add(this.steering_tabs, "1, 0");
  }
  
  public Vector<File> getSteeringFiles() {
    Vector<File> steering_files = new Vector<File>();
    Collection<SteeredVariable> vars = steered_variables.values();
    for (SteeredVariable v : vars) {
      steering_files.add(v.getSteeringFile());
    }
    return steering_files;
  }
  
  public boolean addSteering() {
    int num_vars = this.steered_variables.size();
    SteeringDialog dialog = new SteeringDialog();
    //if the dialog is cancelled, no new variable is added so return false
    if (this.steered_variables.size() == num_vars) return false;
    return true;
  }
  
  public void removeVariable(String name) {
    this.steering_tabs.remove(this.steered_variables.get(name));
    this.steered_variables.remove(name);
  }
  
  public void addVariableToMap(SteeredVariable var) {
    this.steered_variables.put(var.getName(), var);
    this.steering_tabs.addTab(var.getName(), var);
    //add the variable to the remove variable menu in jdata
    this.parent.addSteeredVariableToMenu(var.getName());
  }
  
  //must implement terminate() and isTerminated()
  public void terminate() {
    //System.out.println("steering dying");
  }
  public boolean isTerminated() {
    return true;
  }
  
  //steering dialog class
  public class SteeringDialog extends JDialog implements ActionListener {
  
    private static final int BORDER = 5;
    TableLayout layout;
    Container content;
    //components of dialog UI
    //name of variable for tab
    private JLabel jlabel_name;
    private JTextField jtextfield_name;
    //the file that is going to hold the value
    private JLabel jlabel_file;
    private JTextField jtextfield_file;
    private JButton jbutton_browse;
    private JFileChooser jfilechooser;
    private File steering_file;
    //the type of the variable
    private JLabel jlabel_type;
    private ButtonGroup buttongroup_type;
    private JRadioButton jradiobutton_pulse;
    private JRadioButton jradiobutton_integer;
    private JRadioButton jradiobutton_double;
    private JRadioButton jradiobutton_string;
    private JRadioButton jradiobutton_boolean;
    //initialize type to -1 so that we can tell if it hasn't been set yet
    private int type = -1;
    //initial values
    private JLabel jlabel_initial_value;
    private JTextField jtextfield_initial_value;
    private JLabel jlabel_min_value;
    private JTextField jtextfield_min_value;
    private JLabel jlabel_max_value;
    private JTextField jtextfield_max_value;
    //private JCheckBox jcheckbox_on_off;
    private JComboBox jcombobox_true_false;
    private JLabel jlabel_pulse_default;
    //control buttons
    private JButton jbutton_ok;
    private JButton jbutton_cancel;
    
    public SteeringDialog() {
      super(parent, "Add steered variable", true);
      //set the layout of the info panel
      double[][] size = {
        {BORDER, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, BORDER }, // columns
        {BORDER, 18, 2, 18, 2, 18, 2, 18, 2, 18, BORDER}  // rows
      };
      this.layout = new TableLayout(size);
      this.content = this.getContentPane();
      content.setLayout(layout);
      //create and add components
      //name of variable for tab
      this.jlabel_name = new JLabel("Name of steered variable:");
      this.add(this.jlabel_name, "1, 1, 3, 1");
      this.jtextfield_name = new JTextField();
      this.add(this.jtextfield_name, "5, 1");
      //the file that is going to hold the value
      this.jlabel_file = new JLabel("Steering file:");
      this.add(this.jlabel_file, "1, 3");
      this.jtextfield_file = new JTextField();
      this.add(this.jtextfield_file, "3, 3, 9, 3");
      this.jbutton_browse = new JButton("Browse");
      this.jbutton_browse.addActionListener(this);
      this.add(this.jbutton_browse, "11, 3"); 
      //the type of the variable
      this.jlabel_type = new JLabel("Variable type:");
      this.add(this.jlabel_type, "1, 5");
      this.buttongroup_type = new ButtonGroup();
      //a pulse (normally off, set to on by button, then set back to off by lbflow)
      this.jradiobutton_pulse = new JRadioButton("Pulse");
      this.jradiobutton_pulse.addActionListener(this);
      this.add(this.jradiobutton_pulse, "3, 5");
      this.buttongroup_type.add(this.jradiobutton_pulse);
      //a boolean
      this.jradiobutton_boolean = new JRadioButton("Boolean");
      this.jradiobutton_boolean.addActionListener(this);
      this.add(this.jradiobutton_boolean, "5, 5");
      this.buttongroup_type.add(this.jradiobutton_boolean);
      //an integer
      this.jradiobutton_integer = new JRadioButton("Integer");
      this.jradiobutton_integer.addActionListener(this);
      this.add(this.jradiobutton_integer, "7, 5");
      this.buttongroup_type.add(this.jradiobutton_integer);
      //a double
      this.jradiobutton_double = new JRadioButton("Double");
      this.jradiobutton_double.addActionListener(this);
      this.add(this.jradiobutton_double, "9, 5");
      this.buttongroup_type.add(this.jradiobutton_double);
      //a string
      this.jradiobutton_string = new JRadioButton("String");
      this.jradiobutton_string.addActionListener(this);
      this.add(this.jradiobutton_string, "11, 5");
      this.buttongroup_type.add(this.jradiobutton_string);
      //initial values
      this.jlabel_initial_value = new JLabel("Initial value:");
      this.add(this.jlabel_initial_value, "1, 7");
      this.jtextfield_initial_value = new JTextField();
      this.add(this.jtextfield_initial_value, "3, 7");
      //min and max are not enabled initially (only if number is selected)
      this.jlabel_min_value = new JLabel("Min value:");
      this.jlabel_min_value.setHorizontalAlignment(JLabel.RIGHT);
      this.add(this.jlabel_min_value, "5, 7");
      this.jlabel_min_value.setEnabled(false);
      this.jtextfield_min_value = new JTextField();
      this.add(this.jtextfield_min_value, "7, 7");
      this.jtextfield_min_value.setEnabled(false);
      this.jlabel_max_value = new JLabel("Max value:");
      this.jlabel_max_value.setHorizontalAlignment(JLabel.RIGHT);
      this.add(this.jlabel_max_value, "9, 7");
      this.jlabel_max_value.setEnabled(false);
      this.jtextfield_max_value = new JTextField();
      this.add(this.jtextfield_max_value, "11, 7");
      this.jtextfield_max_value.setEnabled(false);
      //create checkbox for on/off for boolean
      //this.jcheckbox_on_off = new JCheckBox("true");
      this.jcombobox_true_false = new JComboBox(new Boolean [] {true, false});
      //create label for use with pulse
      this.jlabel_pulse_default = new JLabel("Pulse value:");
      //control buttons
      this.jbutton_ok = new JButton("OK");
      this.jbutton_ok.addActionListener(this);
      this.add(this.jbutton_ok, "5, 9");
      this.jbutton_cancel = new JButton("Cancel");
      this.jbutton_cancel.addActionListener(this);
      this.add(this.jbutton_cancel, "7, 9");
    
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
    
    //must implement actionPerformed()
    public void actionPerformed(ActionEvent e) {
      if (e.getSource().equals(this.jbutton_browse)) {
        this.setSteeringFile();
      }
      if (e.getSource().equals(this.jradiobutton_pulse)) {
        this.type = SteeredVariable.PULSE;
        this.jlabel_min_value.setEnabled(false);
        this.jtextfield_min_value.setEnabled(false);
        this.jlabel_max_value.setEnabled(false);
        this.jtextfield_max_value.setEnabled(false);
        if (this.jtextfield_initial_value.isShowing()) {
          this.remove(this.jtextfield_initial_value);
          //this.add(this.jcheckbox_on_off, "3, 7");
          this.add(this.jcombobox_true_false, "3, 7");
          super.validate();
        }
        if (this.jlabel_initial_value.isShowing()) {
          this.remove(this.jlabel_initial_value);
          this.add(this.jlabel_pulse_default, "1, 7");
          super.validate();
        }
        super.repaint();
      }
      if (e.getSource().equals(this.jradiobutton_boolean)) {
        this.type = SteeredVariable.BOOLEAN;
        this.jlabel_min_value.setEnabled(false);
        this.jtextfield_min_value.setEnabled(false);
        this.jlabel_max_value.setEnabled(false);
        this.jtextfield_max_value.setEnabled(false);
        if (this.jtextfield_initial_value.isShowing()) {
          this.remove(this.jtextfield_initial_value);
          //this.add(this.jcheckbox_on_off, "3, 7");
          this.add(this.jcombobox_true_false, "3, 7");
          super.validate();
        }
        if (this.jlabel_pulse_default.isShowing()) {
          this.remove(this.jlabel_pulse_default);
          this.add(this.jlabel_initial_value, "1, 7");
          super.validate();
        }
        super.repaint();
      }
      if (e.getSource().equals(this.jradiobutton_integer)) {
        this.type = SteeredVariable.INTEGER;
        this.jlabel_min_value.setEnabled(true);
        this.jtextfield_min_value.setEnabled(true);
        this.jlabel_max_value.setEnabled(true);
        this.jtextfield_max_value.setEnabled(true);
        if (this.jcombobox_true_false.isShowing()) {
          this.remove(this.jcombobox_true_false);
          this.add(this.jtextfield_initial_value, "3, 7");
          super.validate();
        }
        if (this.jlabel_pulse_default.isShowing()) {
          this.remove(this.jlabel_pulse_default);
          this.add(this.jlabel_initial_value, "1, 7");
          super.validate();
        }
        super.repaint();
      }
      if (e.getSource().equals(this.jradiobutton_double)) {
        this.type = SteeredVariable.DOUBLE;
        this.jlabel_min_value.setEnabled(true);
        this.jtextfield_min_value.setEnabled(true);
        this.jlabel_max_value.setEnabled(true);
        this.jtextfield_max_value.setEnabled(true);
        if (this.jcombobox_true_false.isShowing()) {
          this.remove(this.jcombobox_true_false);
          this.add(this.jtextfield_initial_value, "3, 7");
          super.validate();
        }
        if (this.jlabel_pulse_default.isShowing()) {
          this.remove(this.jlabel_pulse_default);
          this.add(this.jlabel_initial_value, "1, 7");
          super.validate();
        }
        super.repaint();
      }
      if (e.getSource().equals(this.jradiobutton_string)) {
        this.type = SteeredVariable.STRING;
        this.jlabel_min_value.setEnabled(false);
        this.jtextfield_min_value.setEnabled(false);
        this.jlabel_max_value.setEnabled(false);
        this.jtextfield_max_value.setEnabled(false);
        if (this.jcombobox_true_false.isShowing()) {
          this.remove(this.jcombobox_true_false);
          this.add(this.jtextfield_initial_value, "3, 7");
          super.validate();
        }
        if (this.jlabel_pulse_default.isShowing()) {
          this.remove(this.jlabel_pulse_default);
          this.add(this.jlabel_initial_value, "1, 7");
          super.validate();
        }
        super.repaint();
      }
      if (e.getSource().equals(this.jbutton_ok)) {
        if (this.setValues()) {
          super.setVisible(false);
          super.dispose();
        }
      }
      if (e.getSource().equals(this.jbutton_cancel)) {
        super.setVisible(false);
        super.dispose();
      }
    }
    
    private void setSteeringFile() {
      //choose steering file
      this.jfilechooser = new JFileChooser(JData.DEFAULT_SIM_DIR);
      //set file filter
      ExtensionFilter str_filter = new ExtensionFilter(".str", "steering files (*.str)");
      this.jfilechooser.addChoosableFileFilter(str_filter);
      this.jfilechooser.setFileFilter(str_filter);
      if (this.jfilechooser.showOpenDialog(this) == this.jfilechooser.APPROVE_OPTION) {
        this.steering_file = this.jfilechooser.getSelectedFile();
        this.jtextfield_file.setText(this.steering_file.getAbsolutePath());
      }
    }
    
    private boolean setValues() {
      //try creating and adding a new steered variable to the hashmap in the parent panel
      //if name isn't set, show message and return false
      if (this.jtextfield_name.getText().matches("")) {
        JOptionPane.showConfirmDialog(parent, //parent defined in outer class
                                      "You must specify a name for the steered variable",
                                      "ERROR",
                                      JOptionPane.DEFAULT_OPTION,
                                      JOptionPane.ERROR_MESSAGE);
        return false;
      }
      //if steering file isn't set, show message and return false
      if (this.steering_file == null && this.jtextfield_file.getText().matches("")) {
        JOptionPane.showConfirmDialog(parent, //parent defined in outer class
                                      "You must specify a steering file",
                                      "ERROR",
                                      JOptionPane.DEFAULT_OPTION,
                                      JOptionPane.ERROR_MESSAGE);
        return false;
      }
      //if type isn't set, show message and return false
      if (this.type < 0) {
        JOptionPane.showConfirmDialog(parent, //parent defined in outer class
                                      "You must specify a type for the steered variable",
                                      "ERROR",
                                      JOptionPane.DEFAULT_OPTION,
                                      JOptionPane.ERROR_MESSAGE);
        return false;
      }
      //if the file already exists, check if it is ok to overwrite
      this.steering_file = new File(this.jtextfield_file.getText());
      if (this.steering_file.isFile()) {
        String warning_message = "";
        try {
          warning_message += this.steering_file.toURL() + " exists. Overwrite?";
        } catch (Exception e) {
          System.out.println("SteeringPanel.setValues()");
          e.printStackTrace();
        }
        if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(parent,
                                                                    warning_message,
                                                                    "Confirm overwrite",
                                                                    JOptionPane.YES_NO_OPTION,
                                                                    JOptionPane.WARNING_MESSAGE)) {
          return false;
        }
      }
      //if we get here, we are happy to make variable
      //call add variable function in outer class (SteeringPanel)
      if (this.type == SteeredVariable.BOOLEAN || this.type == SteeredVariable.PULSE) {
        addVariableToMap(new SteeredVariable(this.jtextfield_name.getText(),
                                             this.jtextfield_file.getText(),
                                             this.type,
                                             ((Boolean)this.jcombobox_true_false.getSelectedItem()).toString(),
                                             this.jtextfield_min_value.getText(),
                                             this.jtextfield_max_value.getText()));
      }
      else {
        addVariableToMap(new SteeredVariable(this.jtextfield_name.getText(),
                                             this.jtextfield_file.getText(),
                                             this.type,
                                             this.jtextfield_initial_value.getText(),
                                             this.jtextfield_min_value.getText(),
                                             this.jtextfield_max_value.getText()));
      
      }
      return true;
    }
    
    //another inner class to define a file filter for steering files
    public class ExtensionFilter extends FileFilter {
  
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
  
  }
  
  //Steered variable class
  public class SteeredVariable extends JPanel implements ActionListener, ChangeListener {
    
    private static final int BORDER = 5;
    
    //static type definitions
    public static final int PULSE = 0;
    public static final int INTEGER = 1;
    public static final int DOUBLE = 2;
    public static final int STRING = 3;
    public static final int BOOLEAN = 4;
    
    private File steering_file;
    private String name;
    private int type;
    //default value for puse variable
    private boolean pulse_default;
    //values for slider scaling
    private double min_double;
    private double scaling;
    
    //visible components
    private JLabel jlabel_value;
    private JTextField jtextfield_value;
    private JSlider jslider_value;
    private JButton jbutton_set;
    private JCheckBox jcheckbox_on_off;
    private JButton jbutton_pulse;
    
    public SteeredVariable(String tab_name, String file, int t, Object val, Object min, Object max) {
      this.name = tab_name;
      this.steering_file = new File(file);
      this.type = t;
      switch (this.type) {
        case PULSE:   this.createSteeredPulsePanel(val);
                      break;
        case BOOLEAN: this.createSteeredBooleanPanel(val);
                      break;
        case INTEGER: this.createSteeredNumberPanel(val, min, max);
                      break;
        case DOUBLE:  this.createSteeredNumberPanel(val, min, max);
                      break;
        case STRING:  this.createSteeredStringPanel(val);
                      break;
      }
      //write initial value to file. if it is a pulse type, intial value should be opposite
      if (this.type == PULSE) pulse_default = pulse_default? false : true;
      this.writeValueToFile();
      if (this.type == PULSE) pulse_default = pulse_default? false : true;
    }
    
    private void createSteeredPulsePanel(Object val) {
      //set the layout of the variable tab
      double[][] size = {
        {BORDER, TableLayout.FILL, BORDER }, // columns
        {BORDER, TableLayout.MINIMUM, BORDER}  // rows
      };
      TableLayout layout = new TableLayout(size);
      this.setLayout(layout);
      //create and add components
      //set default value for pulse
      this.pulse_default = Boolean.parseBoolean((String)val);
      //create and add button
      this.jbutton_pulse = new JButton("Pulse");
      this.add(this.jbutton_pulse, "1, 1");
      this.jbutton_pulse.addActionListener(this);
    }
    
    private void createSteeredBooleanPanel(Object val) {
      //set the layout of the variable tab
      double[][] size = {
        {BORDER, TableLayout.FILL, 2, TableLayout.FILL, BORDER }, // columns
        {BORDER, 18, 2, 18, BORDER}  // rows
      };
      TableLayout layout = new TableLayout(size);
      this.setLayout(layout);
      //create and add components
      //set checkbox to initial value
      if (Boolean.parseBoolean((String)val)) this.jcheckbox_on_off = new JCheckBox("Variable on/off", true);
      else this.jcheckbox_on_off = new JCheckBox("Variable on/off", false);
      this.add(this.jcheckbox_on_off, "1, 1, 3, 1");
      this.jcheckbox_on_off.addActionListener(this);
    }
    
    private void createSteeredNumberPanel(Object val, Object min, Object max) {
      //set the layout of the variable tab
      double[][] size = {
        {BORDER, TableLayout.MINIMUM, 2, TableLayout.FILL, 2, TableLayout.MINIMUM, BORDER }, // columns
        {BORDER, 18, 2, TableLayout.MINIMUM, BORDER}  // rows
      };
      TableLayout layout = new TableLayout(size);
      this.setLayout(layout);
      //create and add components
      this.jlabel_value = new JLabel("Value:");
      this.add(this.jlabel_value, "1, 1");
      this.jtextfield_value = new JTextField();
      //set to zero if no value has been set
      if (val.toString().matches("")) this.jtextfield_value.setText("0");
      else this.jtextfield_value.setText(val.toString());
      this.add(this.jtextfield_value, "3, 1");
      //you can always set the value directly
      this.jbutton_set = new JButton("Set");
      this.add(this.jbutton_set, "5, 1");
      this.jbutton_set.addActionListener(this);
      //see if limits are set, if so, add slider
      if (!min.toString().matches("") && !max.toString().matches("")) {
        //if this is an integer type
        if (this.type == INTEGER) {
          int val_int = Integer.parseInt((String)val);
          int min_int = Integer.parseInt((String)min);
          int max_int = Integer.parseInt((String)max);
          this.jslider_value = new JSlider(JSlider.HORIZONTAL, min_int, max_int, val_int);
          this.jslider_value.setMajorTickSpacing(max_int - min_int);
          this.jslider_value.setMinorTickSpacing((int)((max_int - min_int) / 10));
          this.jslider_value.setPaintTicks(true);
        
        }
        else if (this.type == DOUBLE) { //otherwise, if it is a double type
          double val_double = Double.parseDouble((String)val);
          this.min_double = Double.parseDouble((String)min);
          double max_double = Double.parseDouble((String)max);
          this.scaling = (max_double - this.min_double) / 100;
          this.jslider_value = new JSlider(JSlider.HORIZONTAL, 0, 100, (int)((val_double - this.min_double) / this.scaling));
          this.jslider_value.setMajorTickSpacing(100);
          this.jslider_value.setMinorTickSpacing(10);
          this.jslider_value.setPaintTicks(true);
        }
        this.jslider_value.addChangeListener(this);
        this.add(this.jslider_value, "1, 3, 5, 3");
      }
    }
    
    private void createSteeredStringPanel(Object val) {
      //set the layout of the variable tab
      double[][] size = {
        {BORDER, TableLayout.FILL, 2, TableLayout.FILL, BORDER }, // columns
        {BORDER, 18, 2, 18, BORDER}  // rows
      };
      TableLayout layout = new TableLayout(size);
      this.setLayout(layout);
      //create and add components
      this.jlabel_value = new JLabel("String:");
      this.add(this.jlabel_value, "1, 1");
      this.jtextfield_value = new JTextField(val.toString());
      this.add(this.jtextfield_value, "3, 1");
      this.jbutton_set = new JButton("Set value");
      this.add(this.jbutton_set, "1, 3");
      this.jbutton_set.addActionListener(this);
    }
    
    public String getName() {
      return this.name;
    }
    
    public File getSteeringFile() {
      return this.steering_file;
    }
    
    private void writeValueToFile() {
      try {
        FileOutputStream output_file = new FileOutputStream(this.steering_file);
        FileChannel output_channel = output_file.getChannel();
        String output_string = new String();
        if (this.type == PULSE) output_string = ((Boolean)pulse_default).toString();
        else if (this.type == BOOLEAN) output_string = ((Boolean)this.jcheckbox_on_off.isSelected()).toString();
        else output_string = this.jtextfield_value.getText();
        ByteBuffer buf = ByteBuffer.allocate(output_string.length());
        output_channel.write((ByteBuffer)(buf.put(output_string.getBytes()).flip()));
        output_file.close();
        //TODO: show in status bar the location of the file?
        //this.setFilePathLabel();
      } catch (Exception e) {
        System.out.println("SteeringPanel.writeValueToFile()");
        e.printStackTrace();
      }
    }
  
    //must implement actionPerformed()
    public void actionPerformed(ActionEvent e) {
      if (e.getSource().equals(this.jbutton_set)) {
        this.writeValueToFile();
      }
      if (e.getSource().equals(this.jcheckbox_on_off)) {
        this.writeValueToFile();
      }
      if (e.getSource().equals(this.jbutton_pulse)) {
        this.writeValueToFile();
      }
    }
  
    //must implement stateChanged()
    public void stateChanged(ChangeEvent e) {
      if (e.getSource().equals(this.jslider_value)) {
        if (this.type == INTEGER) {
          this.jtextfield_value.setText(((Integer)this.jslider_value.getValue()).toString());
        }
        else if (this.type == DOUBLE) {
          this.jtextfield_value.setText(String.format("%.5f",
                                        (Double)((this.jslider_value.getValue() * this.scaling) + this.min_double)));
        }
        if (!this.jslider_value.getValueIsAdjusting()) {
          //when it has stopped moving, reset file
          this.writeValueToFile();
        }
      }
    }
    
  }

}