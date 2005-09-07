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
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import info.clearthought.layout.*;

//TODO: add light source controls

public class CameraPanel extends JPanel implements ActionListener {

  private static final int BORDER = 5;

  private LBVTKPanel vtkDisplay;
  private double [] p = new double[10];
  
  private Viz3DPanel parent;
  
  //buttons for opening and saving views
  private JButton jbutton_open_view;
  private JButton jbutton_save_view;
  private JButton jbutton_save_view_as;
    //helpers
    private JFileChooser jfilechooser;
    private File view_file;
    private boolean view_file_checked = false;
  
  //panels for camera view and lights view
  private JPanel jpanel_camera;
  private JPanel jpanel_lights;
  
  //TODO: do lights as well as camera

  private JLabel jlabel_camera;
  private JLabel jlabel_x;
  private JLabel jlabel_y;
  private JLabel jlabel_z;
  private JLabel jlabel_up;
  private JLabel jlabel_pos;
  private JLabel jlabel_focus;
  private JLabel jlabel_distance;
  private JTextField jtextfield_up_x;
  private JTextField jtextfield_up_y;
  private JTextField jtextfield_up_z;
  private JTextField jtextfield_pos_x;
  private JTextField jtextfield_pos_y;
  private JTextField jtextfield_pos_z;
  private JTextField jtextfield_focus_x;
  private JTextField jtextfield_focus_y;
  private JTextField jtextfield_focus_z;
  private JTextField jtextfield_distance;
  private JButton jbutton_set_angles;
  private JButton jbutton_update_angles;
  
  private JCheckBox jcheckbox_outline;
  
  public CameraPanel(Viz3DPanel viz_panel) {
    this.parent = viz_panel;
    this.vtkDisplay = this.parent.getVTKDisplay();
    //TODO: should I check at this point that the vtkDisplay have been created? if it is null, there will be probs
    // Set the layout of the camera panel
    double[][] panel_size = {
      {BORDER, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, BORDER }, // columns
      {BORDER, TableLayout.MINIMUM, BORDER, TableLayout.FILL, BORDER, TableLayout.FILL, BORDER, TableLayout.MINIMUM, BORDER}  // rows
    };
    TableLayout panel_layout = new TableLayout(panel_size);
    this.setLayout(panel_layout);
    //create open and save buttons
    this.jbutton_open_view = new JButton("Open");
    this.jbutton_open_view.addActionListener(this);
    this.add(this.jbutton_open_view, "1, 1");
    this.jbutton_save_view = new JButton("Save");
    this.jbutton_save_view.addActionListener(this);
    this.add(this.jbutton_save_view, "3, 1");
    this.jbutton_save_view_as = new JButton("SaveAs");
    this.jbutton_save_view_as.addActionListener(this);
    this.add(this.jbutton_save_view_as, "5, 1");
    //create and add panels for camera and lights
    this.jpanel_camera = new JPanel();
    this.jpanel_camera.setBorder(new TitledBorder(new EtchedBorder(), "Camera controls"));
    this.add(this.jpanel_camera, "1, 3, 5, 3");
    this.jpanel_lights = new JPanel();
    this.jpanel_lights.setBorder(new TitledBorder(new EtchedBorder(), "Lighting controls"));
    this.add(this.jpanel_lights, "1, 5, 5, 5");
    // Set the layout of the camera panel
    double[][] camera_panel_size = {
      {BORDER, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, BORDER }, // columns
      {BORDER, 18, 2, 18, 2, 18, 2, 18, 2, 18, 2, 18, 2, 18, 2, 18, BORDER}  // rows
    };
    TableLayout camera_panel_layout = new TableLayout(camera_panel_size);
    this.jpanel_camera.setLayout(camera_panel_layout);
    //create camera control content
    this.jlabel_camera = new JLabel("Camera position");
    this.jlabel_x = new JLabel("x");
    this.jlabel_y = new JLabel("y");
    this.jlabel_z = new JLabel("z");
    this.jlabel_up = new JLabel("up:");
    this.jlabel_pos = new JLabel("camera:");
    this.jlabel_focus = new JLabel("focus:");
    this.jlabel_distance = new JLabel("distance:");
    this.jtextfield_up_x = new JTextField();
    this.jtextfield_up_y = new JTextField();
    this.jtextfield_up_z = new JTextField();
    this.jtextfield_pos_x = new JTextField();
    this.jtextfield_pos_y = new JTextField();
    this.jtextfield_pos_z = new JTextField();
    this.jtextfield_focus_x = new JTextField();
    this.jtextfield_focus_y = new JTextField();
    this.jtextfield_focus_z = new JTextField();
    this.jtextfield_distance = new JTextField();
    this.updateCameraPosition();
    this.jbutton_set_angles = new JButton("set camera");
    this.jbutton_set_angles.addActionListener(this);
    this.jbutton_update_angles = new JButton("update");
    this.jbutton_update_angles.addActionListener(this);
    //add components to camera panel
    this.jpanel_camera.add(this.jlabel_camera, "1, 1, 7, 1");
    this.jpanel_camera.add(this.jlabel_x, "3, 3");
    this.jpanel_camera.add(this.jlabel_y, "5, 3");
    this.jpanel_camera.add(this.jlabel_z, "7, 3");
    this.jpanel_camera.add(this.jlabel_up, "1, 5");
    this.jpanel_camera.add(this.jtextfield_up_x, "3, 5");
    this.jpanel_camera.add(this.jtextfield_up_y, "5, 5");
    this.jpanel_camera.add(this.jtextfield_up_z, "7, 5");
    this.jpanel_camera.add(this.jlabel_pos, "1, 7");
    this.jpanel_camera.add(this.jtextfield_pos_x, "3, 7");
    this.jpanel_camera.add(this.jtextfield_pos_y, "5, 7");
    this.jpanel_camera.add(this.jtextfield_pos_z, "7, 7");
    this.jpanel_camera.add(this.jlabel_focus, "1, 9");
    this.jpanel_camera.add(this.jtextfield_focus_x, "3, 9");
    this.jpanel_camera.add(this.jtextfield_focus_y, "5, 9");
    this.jpanel_camera.add(this.jtextfield_focus_z, "7, 9");
    this.jpanel_camera.add(this.jlabel_distance, "1, 11, 3, 11");
    this.jpanel_camera.add(this.jtextfield_distance, "5, 11, 7, 11");
    this.jpanel_camera.add(this.jbutton_set_angles, "1, 13, 3, 13");
    this.jpanel_camera.add(this.jbutton_update_angles, "5, 13, 7, 13");
    // Set the layout of the lights panel
    double[][] lights_panel_size = {
      {BORDER, TableLayout.FILL, BORDER }, // columns
      {BORDER, TableLayout.FILL, BORDER}  // rows
    };
    TableLayout lights_panel_layout = new TableLayout(lights_panel_size);
    this.jpanel_lights.setLayout(lights_panel_layout);
    //create camera control content
    
    //create and add outline control
    this.jcheckbox_outline = new JCheckBox("show outline", true);
    this.jcheckbox_outline.addActionListener(this);
    this.add(this.jcheckbox_outline, "1, 7, 5, 7");
    //try making things repaint
    this.repaint();
    this.parent.repaint();
    this.parent.getJData().repaint();
  }
  
  public void updateCameraPosition() {
    this.p = this.vtkDisplay.getCameraPosition();
    this.jtextfield_distance.setText(String.format("%2.2g", this.p[0]));
    this.jtextfield_up_x.setText(String.format("%2.2g", this.p[1]));
    this.jtextfield_up_y.setText(String.format("%2.2g", this.p[2]));
    this.jtextfield_up_z.setText(String.format("%2.2g", this.p[3]));
    this.jtextfield_pos_x.setText(String.format("%2.2g", this.p[4]));
    this.jtextfield_pos_y.setText(String.format("%2.2g", this.p[5]));
    this.jtextfield_pos_z.setText(String.format("%2.2g", this.p[6]));
    this.jtextfield_focus_x.setText(String.format("%2.2g", this.p[7]));
    this.jtextfield_focus_y.setText(String.format("%2.2g", this.p[8]));
    this.jtextfield_focus_z.setText(String.format("%2.2g", this.p[9]));
  }
  
  public void readCameraPosition() {
    this.jtextfield_distance.setText(String.format("%2.2g", this.p[0]));
    this.jtextfield_up_x.setText(String.format("%2.2g", this.p[1]));
    this.jtextfield_up_y.setText(String.format("%2.2g", this.p[2]));
    this.jtextfield_up_z.setText(String.format("%2.2g", this.p[3]));
    this.jtextfield_pos_x.setText(String.format("%2.2g", this.p[4]));
    this.jtextfield_pos_y.setText(String.format("%2.2g", this.p[5]));
    this.jtextfield_pos_z.setText(String.format("%2.2g", this.p[6]));
    this.jtextfield_focus_x.setText(String.format("%2.2g", this.p[7]));
    this.jtextfield_focus_y.setText(String.format("%2.2g", this.p[8]));
    this.jtextfield_focus_z.setText(String.format("%2.2g", this.p[9]));
    this.vtkDisplay.setCameraPosition(p);
  }
  
  //must implement actionPerformed()
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(this.jcheckbox_outline)) {
      this.vtkDisplay.toggleOutline();
    }
    if (e.getSource().equals(this.jbutton_open_view)) {
      this.openViewFile();
    }
    if (e.getSource().equals(this.jbutton_save_view)) {
      this.saveViewFile();
    }
    if (e.getSource().equals(this.jbutton_save_view_as)) {
      this.saveViewFileAs();
    }
    if (e.getSource().equals(this.jbutton_set_angles)) {
      this.p[0] = Double.parseDouble(this.jtextfield_distance.getText());
      this.p[1] = Double.parseDouble(this.jtextfield_up_x.getText());
      this.p[2] = Double.parseDouble(this.jtextfield_up_y.getText());
      this.p[3] = Double.parseDouble(this.jtextfield_up_z.getText());
      this.p[4] = Double.parseDouble(this.jtextfield_pos_x.getText());
      this.p[5] = Double.parseDouble(this.jtextfield_pos_y.getText());
      this.p[6] = Double.parseDouble(this.jtextfield_pos_z.getText());
      this.p[7] = Double.parseDouble(this.jtextfield_focus_x.getText());
      this.p[8] = Double.parseDouble(this.jtextfield_focus_y.getText());
      this.p[9] = Double.parseDouble(this.jtextfield_focus_z.getText());
      this.vtkDisplay.setCameraPosition(p);
    }
    if (e.getSource().equals(this.jbutton_update_angles)) {
      this.updateCameraPosition();
    }
  }
  
  private void openViewFile() {
    //set this to open up ready for previous file
    this.jfilechooser = new JFileChooser(JData.DEFAULT_DATA_DIR);
    if (this.jfilechooser.showOpenDialog(this) == this.jfilechooser.APPROVE_OPTION) {
      this.view_file = this.jfilechooser.getSelectedFile();
      try {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(this.view_file));
        for (int i = 0; i < 10; ++i) {
          this.p[i] = in.readDouble();
        }
        this.readCameraPosition();
        //make sure you don't overwrite without being asked
        this.view_file_checked = false;
      } catch (Exception e) {
        System.out.println("CameraPanel.openViewFile()");
        e.printStackTrace();
      }
    }
  }
  
  //standard save procedure: makes appropriate checks before saving
  public void saveViewFile() {
    if (this.view_file_checked) {
      //save operation
      try {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(this.view_file));
        for (int i = 0; i < 10; ++i) {
          out.writeDouble(this.p[i]);
        }
        out.close();
      } catch (Exception e) {
        System.out.println("CameraPanel.saveViewFile()#1");
        //something's gone wrong, try again
        e.printStackTrace();
        this.view_file_checked = false;
        this.saveViewFile();
      }
    }
    else if (this.view_file == null) this.saveViewFileAs();
    else if (this.view_file.isFile()) {
      //file already exists, do you want to overwrite?
      String warning_message = "";
      try {
        warning_message += this.view_file.toURL() + " exists. Overwrite?";
      } catch (Exception e) {
        System.out.println("CameraPanel.openViewFile()#2");
        e.printStackTrace();
      }
      if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                                                                  warning_message,
                                                                  "Confirm overwrite",
                                                                  JOptionPane.YES_NO_OPTION,
                                                                  JOptionPane.WARNING_MESSAGE)) {
        //yes, we do want to overwrite
        this.view_file_checked = true;
        this.saveViewFile();
      }
      //no, we don't want to overwrite
      else this.saveViewFileAs();
    }
    //must be that file or its parent directories don't exist
    else {
      this.view_file = this.view_file.getAbsoluteFile();
      File parent_directory = new File(this.view_file.getParent());
      if (!parent_directory.exists()) parent_directory.mkdirs();
      this.view_file_checked = true;
      this.saveViewFile();
    }
  }
  
  public void saveViewFileAs() {
    this.view_file_checked = false;
    if (this.view_file == null || !this.view_file.isFile()) {
      this.jfilechooser = new JFileChooser(JData.DEFAULT_DATA_DIR);
    }
    else this.jfilechooser = new JFileChooser(this.view_file);
    if (this.jfilechooser.showSaveDialog(this) == this.jfilechooser.APPROVE_OPTION) {
      this.view_file = this.jfilechooser.getSelectedFile();
      this.jfilechooser = null;
      this.saveViewFile();
    }
  }

}