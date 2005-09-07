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
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import info.clearthought.layout.*;
import LBStructures.*;

//TODO: enable keyboard shortcuts?

public class SimPanel extends JPanel implements Terminable {

  private static final int BORDER = 5;
  
  private JData parent;

  private File default_sim_file;
  private File sim_file;
  private boolean sim_file_checked = false;
  private JFileChooser jfilechooser;
  
  private JScrollPane jscrollpane_sim;
    private JEditorPane jeditorpane_sim;
    private Font editor_font;
  
  private JLabel jlabel_file_path;

  public SimPanel(JData jdata, File sim) {
    this.parent = jdata;
    //if sim_file has already been opened by parent and sent, use it
    if (sim != null) this.sim_file = sim;
    //otherwise, use the default sim file template if one is available
    else {
      this.default_sim_file = new File("default.sim");
      if (!this.default_sim_file.isFile()) this.default_sim_file = null;
    }
    // Set the layout of the info panel
    double[][] size = {
      {TableLayout.FILL}, // columns
      {TableLayout.FILL, 2, 18}  // rows
    };
    TableLayout layout = new TableLayout(size);
    this.setLayout(layout);
    //open up the editor
    try {
      //if sim file is set, use it
      if (this.sim_file != null) this.jeditorpane_sim = new JEditorPane(this.sim_file.toURL());
      //otherwise use default sim file template if it is set
      else if (this.default_sim_file != null) this.jeditorpane_sim = new JEditorPane(this.default_sim_file.toURL());
      //otherwise use a blank window
      else this.jeditorpane_sim = new JEditorPane();
    } catch (Exception e) {
      System.out.println("SimPanel()");
      e.printStackTrace();
    }
    this.jscrollpane_sim = new JScrollPane(this.jeditorpane_sim);
    this.editor_font = this.jeditorpane_sim.getFont();
    this.editor_font = new Font("Monospaced", this.editor_font.getStyle(), this.editor_font.getSize());
    this.jeditorpane_sim.setFont(this.editor_font);
    this.add(this.jscrollpane_sim, "0, 0");
    //add file path bar at the bottom
    this.jlabel_file_path = new JLabel();
    this.jlabel_file_path.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    this.setFilePathLabel();
    this.add(this.jlabel_file_path, "0, 2");
  }
  
  public void saveSimFile() {
    if (this.sim_file_checked) {
      //save operation
      try {
        FileOutputStream output_file = new FileOutputStream(this.sim_file);
        FileChannel output_channel = output_file.getChannel();
        String output_string = this.jeditorpane_sim.getText();
        ByteBuffer buf = ByteBuffer.allocate(output_string.length());
        output_channel.write((ByteBuffer)(buf.put(output_string.getBytes()).flip()));
        output_file.close();
        this.setFilePathLabel();
      } catch (Exception e) {
        System.out.println("SimPanel.saveSimFile()#1");
        //something's gone wrong, try again
        e.printStackTrace();
        this.sim_file_checked = false;
        this.saveSimFile();
      }
    }
    else if (this.sim_file == null) this.saveSimFileAs();
    else if (this.sim_file.isFile()) {
      //file already exists, do you want to overwrite?
      String warning_message = "";
      try {
        warning_message += this.sim_file.toURL() + " exists. Overwrite?";
      } catch (Exception e) {
        System.out.println("SimPanel.saveSimFile()#2");
        e.printStackTrace();
      }
      if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this.parent,
                                                                  warning_message,
                                                                  "Confirm overwrite",
                                                                  JOptionPane.YES_NO_OPTION,
                                                                  JOptionPane.WARNING_MESSAGE)) {
        //yes, we do want to overwrite
        this.sim_file_checked = true;
        this.saveSimFile();
      }
      //no, we don't want to overwrite
      else this.saveSimFileAs();
    }
    //must be that file or its parent directories don't exist
    else {
      this.sim_file = this.sim_file.getAbsoluteFile();
      File parent_directory = new File(this.sim_file.getParent());
      if (!parent_directory.exists()) parent_directory.mkdirs();
      this.sim_file_checked = true;
      this.saveSimFile();
    }
  }
  
  public void saveSimFileAs() {
    this.sim_file_checked = false;
    if (this.sim_file == null || !this.sim_file.isFile()) {
      this.jfilechooser = new JFileChooser(JData.DEFAULT_SIM_DIR);
    }
    else this.jfilechooser = new JFileChooser(this.sim_file);
    if (this.jfilechooser.showSaveDialog(this) == this.jfilechooser.APPROVE_OPTION) {
      this.sim_file = this.jfilechooser.getSelectedFile();
      this.jfilechooser = null;
      this.saveSimFile();
    }
  }
  
  public boolean closeSimFile() {
    int return_option = JOptionPane.showConfirmDialog(this.parent,
                                                      "Save changes to the simulation file before closing?",
                                                      "Save before closing?",
                                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                                      JOptionPane.WARNING_MESSAGE);
    if (return_option == JOptionPane.YES_OPTION) {
      //yes, we do want to save
      this.saveSimFile();
      return true;
    }
    else if (return_option == JOptionPane.NO_OPTION) {
      //no we don't want to save
      return true;
    }
    else if (return_option == JOptionPane.CANCEL_OPTION) {
      //we don't even want to close the file after all
      return false;
    }
    //by default, do nothing
    return false;
  }
  
  public String getSimFilePath() {
    if (this.sim_file != null) {
      return this.sim_file.getAbsolutePath();
    }
    else return new String();
  }
  
  private void setFilePathLabel() {
    if (this.sim_file != null) this.jlabel_file_path.setText(" FILE: " + this.sim_file.getAbsolutePath());
    else this.jlabel_file_path.setText(" FILE: File has not been saved");
  }
  
  //must implement terminate() and isTerminated()
  public void terminate() {
    //System.out.println("sim panel dying");
  }
  public boolean isTerminated() {
    return true;
  }

}