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

public class PipeDialog extends JDialog implements ActionListener
{
    
    private static final int BORDER = 5;
    
    private JData parent;
    
    //for specifying the simulation file
    private JLabel jlabel_sim_file;
    private JTextField jtextfield_sim_file;
    private JButton jbutton_use_open_sim;
    private JButton jbutton_browse;
    private JFileChooser jfilechooser;
    private File sim_file;
    
    //for specifying the remote machine
    private JLabel jlabel_remote_machine;
    private JTextField jtextfield_remote_machine;
    private String remote_machine;
    
    //for specifying the command line arguments
    private JLabel jlabel_command_line_args;
    private JTextField jtextfield_command_line_args;
    private String command_line_args;
    
    //for specifying the required surface files
    private JLabel jlabel_surface_files;
    private JComboBox jcombobox_surface_files;
    private JButton jbutton_add_surface_file;
    private JButton jbutton_remove_surface_file;
    private Vector<File> surface_files;
    
    //dialog control buttons
    private JButton jbutton_ok;
    private JButton jbutton_cancel;
    
    public PipeDialog(JData jdata)
    {
        super(jdata, "Remote process information", true);
        this.parent = jdata;
        //gather extant data from parent
        if ((this.command_line_args = this.parent.getCommandLineArgs()) == null) this.command_line_args = new String();
        if ((this.remote_machine = this.parent.getRemoteExe()) == null) this.remote_machine = JData.DEFAULT_REMOTE_EXE;
        this.surface_files = this.parent.getSurfaceFiles();
        //see if remote sim file is already set
        if (this.parent.getRemoteSimFile() == null)
        {
            this.parent.setRemoteSimFile(this.parent.getOpenSimFile());
        }
        this.sim_file = this.parent.getRemoteSimFile();
        //set the layout of the info panel
        double[][] size = {
            {BORDER, TableLayout.FILL, 2, TableLayout.FILL, 2, TableLayout.FILL, BORDER }, // columns
            {BORDER, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, 2, TableLayout.MINIMUM, BORDER}  // rows
        };
        TableLayout layout = new TableLayout(size);
        Container content = this.getContentPane();
        content.setLayout(layout);
        //create sim file handling
        this.jlabel_sim_file = new JLabel("Sim file location:");
        this.jlabel_sim_file.setHorizontalAlignment(JLabel.RIGHT);
        this.jtextfield_sim_file = new JTextField(20);
        if (this.sim_file != null) this.jtextfield_sim_file.setText(this.sim_file.getAbsolutePath());
        this.jbutton_use_open_sim = new JButton("Use open sim file");
        this.jbutton_use_open_sim.addActionListener(this);
        //check if there is already a sim file open (for editing)
        if ((this.parent.getOpenSimFile().matches(""))) this.jbutton_use_open_sim.setEnabled(false);
        this.jbutton_browse = new JButton("Browse");
        this.jbutton_browse.addActionListener(this);
        //create surface file handling
        this.jlabel_surface_files = new JLabel("Surface files:");
        this.jlabel_surface_files.setHorizontalAlignment(JLabel.RIGHT);
        this.jbutton_add_surface_file = new JButton("Add");
        this.jbutton_add_surface_file.addActionListener(this);
        this.jbutton_remove_surface_file = new JButton("Remove");
        if (this.surface_files == null || this.surface_files.size() == 0)
        {
            this.jbutton_remove_surface_file.setEnabled(false);
        }
        this.jbutton_remove_surface_file.addActionListener(this);
        this.createSurfaceFileComboBox();
        //create remote process handlingmachine
        this.jlabel_remote_machine = new JLabel("Remote process:");
        this.jlabel_remote_machine.setHorizontalAlignment(JLabel.RIGHT);
        this.jtextfield_remote_machine = new JTextField(this.remote_machine, 20);
        //create command line arg handling
        this.jlabel_command_line_args = new JLabel("Command line arguments:");
        this.jlabel_command_line_args.setHorizontalAlignment(JLabel.RIGHT);
        this.jtextfield_command_line_args = new JTextField(20);
        this.jtextfield_command_line_args.setText(this.command_line_args);
        //create control buttons
        this.jbutton_ok = new JButton("OK");
        this.jbutton_ok.addActionListener(this);
        this.jbutton_cancel = new JButton("Cancel");
        this.jbutton_cancel.addActionListener(this);
        //add components
        content.add(this.jlabel_sim_file, "1, 1");
        content.add(this.jtextfield_sim_file, "3, 1, 5, 1");
        content.add(this.jbutton_use_open_sim, "3, 3");
        content.add(this.jbutton_browse, "5, 3");
        content.add(this.jlabel_surface_files, "1, 5");
        content.add(this.jbutton_add_surface_file, "3, 7");
        content.add(this.jbutton_remove_surface_file, "5, 7");
        content.add(this.jlabel_remote_machine, "1, 9");
        content.add(this.jtextfield_remote_machine, "3, 9, 5, 9");
        content.add(this.jlabel_command_line_args, "1, 11");
        content.add(this.jtextfield_command_line_args, "3, 11, 5, 11");
        content.add(this.jbutton_ok, "3, 13");
        content.add(this.jbutton_cancel, "5, 13");
        
        //make it so!
        //set default close operation
        super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        super.pack();
        //set position of the dialog
        Dimension parent_dimension = this.parent.getSize();
        Dimension dialog_dimension = super.getSize();
        Point parent_location = this.parent.getLocation();
        super.setLocation(parent_location.x + (parent_dimension.width - dialog_dimension.width) / 2,
            parent_location.y + (parent_dimension.height - dialog_dimension.height) / 2);
    }
    
    private void createSurfaceFileComboBox()
    {
        if (this.jcombobox_surface_files != null) this.remove(this.jcombobox_surface_files);
        if (this.surface_files != null) this.jcombobox_surface_files = new JComboBox(this.surface_files);
        else this.jcombobox_surface_files = new JComboBox();
        if (this.surface_files == null || this.surface_files.size() == 0)
        {
            this.jbutton_remove_surface_file.setEnabled(false);
        }
        else this.jbutton_remove_surface_file.setEnabled(true);
        this.add(this.jcombobox_surface_files, "3, 5, 5, 5");
        super.validate();
        super.repaint();
    }
    
    private void getSimFile()
    {
        //choose .sim file to run simulation with
        if (this.sim_file == null) this.jfilechooser = new JFileChooser(JData.DEFAULT_SIM_DIR);
        else this.jfilechooser = new JFileChooser(this.sim_file);
        if (this.jfilechooser.showOpenDialog(this) == this.jfilechooser.APPROVE_OPTION)
        {
            this.sim_file = this.jfilechooser.getSelectedFile();
        }
        this.jtextfield_sim_file.setText(this.sim_file.getAbsolutePath());
    }
    
    private void addSurfaceFile()
    {
        this.jfilechooser = new JFileChooser(JData.DEFAULT_SIM_DIR);
        if (this.jfilechooser.showOpenDialog(this) == this.jfilechooser.APPROVE_OPTION)
        {
            File this_file = this.jfilechooser.getSelectedFile();
            if (!this_file.isFile())
            {
                JOptionPane.showConfirmDialog(this,
                    "Specified surface file does not exist",
                    "ERROR",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.ERROR_MESSAGE);
                this.jfilechooser = null;
                this.addSurfaceFile();
            }
            else
            {
                if (this.surface_files == null) this.surface_files = new Vector<File>();
                this.surface_files.add(this_file);
                this.createSurfaceFileComboBox();
            }
        }
    }
    
    private void removeSurfaceFile()
    {
        File this_file = (File)this.jcombobox_surface_files.getSelectedItem();
        this.jcombobox_surface_files.removeItem(this_file);
        this.surface_files.remove(this_file);
        if (this.jcombobox_surface_files.getItemCount() == 0) this.jbutton_remove_surface_file.setEnabled(false);
    }
    
    private boolean setValues()
    {
        if (this.jtextfield_sim_file.getText().matches(""))
        {
            //no sim file specified
            JOptionPane.showConfirmDialog(this,
                "You must specify a sim file",
                "ERROR",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if (!this.parent.setRemoteSimFile(this.jtextfield_sim_file.getText()))
        {
            //the specified sim file doesn't exist so try again
            JOptionPane.showConfirmDialog(this,
                "Specified sim file does not exist",
                "ERROR",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        this.parent.setSurfaceFiles(this.surface_files);
        this.parent.setRemoteExe(this.jtextfield_remote_machine.getText());
        if (this.jtextfield_command_line_args.getText() != null)
            this.parent.setCommandLineArgs(this.jtextfield_command_line_args.getText());
        return true;
    }
    
    //must implement actionPerformed()
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource().equals(this.jbutton_browse))
        {
            this.getSimFile();
        }
        if (e.getSource().equals(this.jbutton_use_open_sim))
        {
            this.parent.setRemoteSimFile(this.parent.getOpenSimFile());
            this.sim_file = this.parent.getRemoteSimFile();
            this.jtextfield_sim_file.setText(this.sim_file.getAbsolutePath());
        }
        if (e.getSource().equals(this.jbutton_add_surface_file))
        {
            this.addSurfaceFile();
        }
        if (e.getSource().equals(this.jbutton_remove_surface_file))
        {
            this.removeSurfaceFile();
        }
        if (e.getSource().equals(this.jbutton_ok))
        {
            //only dispose if the values are set in the parent properly
            if (this.setValues())
            {
                super.setVisible(false);
                super.dispose();
            }
        }
        if (e.getSource().equals(this.jbutton_cancel))
        {
            super.setVisible(false);
            super.dispose();
        }
    }
    
    
}