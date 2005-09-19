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
import java.util.Observer;
import java.util.Observable;
import java.util.Vector;
import LBStructures.*;

import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileInputStream;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.gridservice.client.SGSInstanceClient;

public class JData extends JFrame implements Observer
{
    
    public static final String DEFAULT_REMOTE_EXE = "/home/glewl/lbflow/src/lbflow";
    public static final String DEFAULT_SIM_DIR = "/home/glewl/lbflow/src";
    public static final String DEFAULT_DATA_DIR = "/home/glewl/lbflow/data";
    
    private Dimension scrsize;
    
    //menu bar
    private JMenuBar jmenubar_main;
    private JMenu jmenu_data;
    private JMenu jmenu_simulation;
    private JMenu jmenu_remove_steering;
    private JMenu jmenu_image;
    //actions for menubar
    //static data
    private MenuAction action_open_data;
    private MenuAction action_close_data;
    //active data
    private MenuAction action_new_sim;
    private MenuAction action_open_sim;
    private MenuAction action_save_sim;
    private MenuAction action_save_sim_as;
    private MenuAction action_close_sim;
    //steering
    private MenuAction action_add_steering;
    //remote process
    private MenuAction action_remote_info;
    private MenuAction action_start_sim;
    private MenuAction action_stop_sim;
    private MenuAction action_close_reset;
    //image processing
    private MenuAction action_save_image;
    //helpers
    private JFileChooser jfilechooser;
    private File dom_file;
    
    //JON: all the info for the remote process will be stored here
    private SGSInstanceClient instance;
    private boolean newInstance;
    
    private Process remote_process;
    private PipeDialog pipe_dialog;
    private SGSDialog sgs_dialog;
    private File remote_sim_file;
    private String command_line_args;
    private String remote_exe;
    //surface files required by the simulation
    private Vector<File> surface_files;
    //steering files are held in the steering panel and can be accessed with steering_panel.getSteeringFiles()
    //which returns a Vector<File>
    
    //component panels
    private JTabbedPane tabs;
    private Viz3DPanel v3d_panel;
    private AnalysisPanel analysis_panel;
    private SimPanel sim_panel;
    private MessagePanel message_panel;
    private SteeringPanel steering_panel;
    private StyxSteeringPanel styx_steering_panel;
    
    //data parsing: may come from system out of lbflow programme, or from file
    InputParser input_parser;
    Thread input_parser_thread;
    
    public JData()
    {
        //construct main window
        super("Create, view and process LBFlow data");
        Toolkit tk = getToolkit();
        this.scrsize = tk.getScreenSize();
        //set default close operation
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setBounds(10,
            10,
            3 * this.scrsize.width / 4,
            3 * this.scrsize.height / 4);
        //add menu bar
        this.jmenubar_main = new JMenuBar();
        super.setJMenuBar(this.jmenubar_main);
        //data menu
        this.jmenu_data = new JMenu("Data");
        this.action_open_data = new MenuAction("Open data file");
        this.jmenu_data.add(new JMenuItem(this.action_open_data));
        this.action_close_data = new MenuAction("Close");
        this.action_close_data.setEnabled(false);
        this.jmenu_data.add(new JMenuItem(this.action_close_data));
        //simulation menu
        this.jmenu_simulation = new JMenu("Simulation");
        //create new simulation file
        this.action_new_sim = new MenuAction("New sim file");
        this.action_new_sim.setEnabled(true);
        this.jmenu_simulation.add(new JMenuItem(this.action_new_sim));
        //open simulation file
        this.action_open_sim = new MenuAction("Open sim file");
        this.action_open_sim.setEnabled(true);
        this.jmenu_simulation.add(new JMenuItem(this.action_open_sim));
        //save simulation file
        this.action_save_sim = new MenuAction("Save sim file");
        this.action_save_sim.setEnabled(false);
        this.jmenu_simulation.add(new JMenuItem(this.action_save_sim));
        //save simulation file as
        this.action_save_sim_as = new MenuAction("Save sim file as");
        this.action_save_sim_as.setEnabled(false);
        this.jmenu_simulation.add(new JMenuItem(this.action_save_sim_as));
        //close simulation file
        this.action_close_sim = new MenuAction("Close sim file");
        this.action_close_sim.setEnabled(false);
        this.jmenu_simulation.add(new JMenuItem(this.action_close_sim));
        //steering mechanism
        this.jmenu_simulation.addSeparator();
        //add steering
        this.action_add_steering = new MenuAction("Add steered variable");
        this.action_add_steering.setEnabled(true);
        this.jmenu_simulation.add(new JMenuItem(this.action_add_steering));
        //remove steering menu
        this.jmenu_remove_steering = new JMenu("Remove steered variable");
        this.jmenu_remove_steering.setEnabled(false);
        //add to menu
        this.jmenu_simulation.add(this.jmenu_remove_steering);
        //remote simulation handling
        this.jmenu_simulation.addSeparator();
        //set remote simulation details
        this.action_remote_info = new MenuAction("Set remote process");
        this.jmenu_simulation.add(new JMenuItem(this.action_remote_info));
        //start remote simulation process
        this.action_start_sim = new MenuAction("Start remote process");
        this.action_start_sim.setEnabled(false);
        this.jmenu_simulation.add(new JMenuItem(this.action_start_sim));
        //stop remote simulation process
        this.action_stop_sim = new MenuAction("Stop remote process");
        this.action_stop_sim.setEnabled(false);
        this.jmenu_simulation.add(new JMenuItem(this.action_stop_sim));
        //close everything and reset
        this.action_close_reset = new MenuAction("Close and reset");
        this.action_close_reset.setEnabled(false);
        this.jmenu_simulation.add(new JMenuItem(this.action_close_reset));
        //image menu
        this.jmenu_image = new JMenu("Image");
        this.jmenu_image.setEnabled(false);
        //save an image file
        this.action_save_image = new MenuAction("Save image");
        this.action_save_image.setEnabled(true);
        this.jmenu_image.add(new JMenuItem(this.action_save_image));
        //add to menu bar
        this.jmenubar_main.add(this.jmenu_data);
        this.jmenubar_main.add(this.jmenu_simulation);
        this.jmenubar_main.add(this.jmenu_image);
        
        // Set the layout of the GUI
        double[][] size = {
            {TableLayout.FILL, TableLayout.MINIMUM}, // columns
            {TableLayout.FILL, TableLayout.MINIMUM}  // rows
        };
        TableLayout layout = new TableLayout(size);
        this.setLayout(layout);
        //tabs
        this.tabs = new JTabbedPane();
        //add tabs to GUI
        this.add(tabs, "0, 0");
        //set default close operation
        super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    public static void main(String[] args)
    {
        JData prog = new JData();
        prog.setVisible(true);
    }
    
    //Actions
    
    class MenuAction extends AbstractAction
    {
        
        public MenuAction(String name)
        {
            super(name);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            String name = (String)getValue(NAME);
            //data file actions
            if (name.equals(action_open_data.getValue(NAME)))
            {
                if (dataMenuOpenFile())
                {
                    //enable close action
                    action_close_data.setEnabled(true);
                }
            }
            if (name.equals(action_close_data.getValue(NAME)))
            {
                reset();
                resetSize();
                removeSteeringPanel();
            }
            //simulation file actions
            if (name.equals(action_new_sim.getValue(NAME)))
            {
                simMenuCloseFile();
                //close existing data sources
                startSimPanel(null);
                //enable close and save action
                action_save_sim.setEnabled(true);
                action_save_sim_as.setEnabled(true);
                action_close_sim.setEnabled(true);
                action_close_reset.setEnabled(true);
            }
            if (name.equals(action_open_sim.getValue(NAME)))
            {
                if (simMenuOpenFile())
                {
                    //enable close and save action
                    action_save_sim.setEnabled(true);
                    action_save_sim_as.setEnabled(true);
                    action_close_sim.setEnabled(true);
                    action_close_reset.setEnabled(true);
                }
            }
            if (name.equals(action_save_sim.getValue(NAME)))
            {
                sim_panel.saveSimFile();
            }
            if (name.equals(action_save_sim_as.getValue(NAME)))
            {
                sim_panel.saveSimFileAs();
            }
            if (name.equals(action_close_sim.getValue(NAME)))
            {
                simMenuCloseFile();
            }
            //steering actions
            if (name.equals(action_add_steering.getValue(NAME)))
            {
                simMenuAddSteering();
            }
            //remote process actions
            if (name.equals(action_remote_info.getValue(NAME)))
            {
                simMenuSetRemoteInfo();
            }
            if (name.equals(action_start_sim.getValue(NAME)))
            {
                if (startRemoteProcess())
                {
                    action_stop_sim.setEnabled(true);
                    action_start_sim.setEnabled(false);
                }
            }
            if (name.equals(action_stop_sim.getValue(NAME)))
            {
                //kill remote process if it exists. what happens here if it is already finished?
                /*if (remote_process != null)
                {
                    remote_process.destroy();
                    remote_process = null;
                }*/
                if (instance != null)
                {
                    try
                    {
                        instance.stopService();
                        // Close the connection
                        instance.close();
                    }
                    catch (StyxException se)
                    {
                        se.printStackTrace();
                    }
                }
                action_stop_sim.setEnabled(false);
                action_start_sim.setEnabled(true);
            }
            if (name.equals(action_close_reset.getValue(NAME)))
            {
                reset();
                resetSize();
                removeSteeringPanel();
            }
            //image menu actions
            if (name.equals(action_save_image.getValue(NAME)))
            {
                if (v3d_panel != null) v3d_panel.saveImageAs();
            }
        }
        
    }
    
    private void reset()
    {
        //kill remote process if it exists. what happens here if it is already finished?
        if (this.remote_process != null)
        {
            this.remote_process.destroy();
            this.remote_process = null;
        }
        //wait for the input parser thread to die
        if (this.input_parser_thread != null)
        {
            if (this.input_parser_thread.isAlive())
            {
                try
                {
                    this.input_parser_thread.join();
                }
                catch (InterruptedException e)
                {
                    System.out.println("JData.reset()");
                    e.printStackTrace();
                }
            }
            this.input_parser_thread = null;
        }
        //kill any component panes that have been created cleanly
        int tab_count = this.tabs.getTabCount();
        for (int i = 0; i < tab_count; ++i)
        {
            ((Terminable)this.tabs.getComponentAt(i)).terminate();
        }
        //remove all the windows from the tabbed pane
        this.tabs.removeAll();
        //destroy any created panels
        this.v3d_panel = null;
        this.analysis_panel = null;
        this.sim_panel = null;
        //including message panel
        if (this.message_panel != null)
        {
            this.remove(this.message_panel);
            this.message_panel.terminate();
            this.message_panel = null;
        }
        //make data members null
        this.dom_file = null;
        this.remote_sim_file = null;
        this.command_line_args = null;
        this.remote_exe = null;
        //destroy input parser
        if (this.input_parser != null)
        {
            this.input_parser.terminate();
            this.input_parser = null;
        }
        this.action_close_data.setEnabled(false);
        this.action_save_sim.setEnabled(false);
        this.action_save_sim_as.setEnabled(false);
        this.action_close_sim.setEnabled(false);
        this.action_start_sim.setEnabled(false);
        this.action_stop_sim.setEnabled(false);
        this.action_close_reset.setEnabled(false);
        this.jmenu_image.setEnabled(false);
        //this seems to redraw things properly
        this.setVisible(true);
    }
    
    private void resetSize()
    {
        //reset size of box (in case it was increased by setmessage
        this.setBounds(10,
            10,
            3 * this.scrsize.width / 4,
            3 * this.scrsize.height / 4);
        //this seems to redraw things properly
        this.setVisible(true);
    }
    
    private void removeSteeringPanel()
    {
        //remove steering panel
        if (this.steering_panel != null)
        {
            this.remove(this.steering_panel);
            this.steering_panel.terminate();
            this.steering_panel = null;
        }
        //remove items from steering menu and make it vanish
        this.jmenu_remove_steering.removeAll();
        this.jmenu_remove_steering.setEnabled(false);
        //this seems to redraw things properly
        this.setVisible(true);
    }
    
    private boolean dataMenuOpenFile()
    {
        //close existing data sources
        this.reset();
        this.resetSize();
        this.removeSteeringPanel();
        //TODO: set this to open up ready for previous file (or from preferences file???)
        this.jfilechooser = new JFileChooser(JData.DEFAULT_DATA_DIR);
        if (this.jfilechooser.showOpenDialog(this) == this.jfilechooser.APPROVE_OPTION)
        {
            this.dom_file = this.jfilechooser.getSelectedFile();
            this.input_parser = new InputParser(this.dom_file, this);
            this.input_parser_thread = new Thread(this.input_parser);
            this.input_parser_thread.start();
            this.jfilechooser = null;
            return true;
        }
        else
        {
            this.jfilechooser = null;
            return false;
        }
    }
    
    private boolean simMenuOpenFile()
    {
        //close existing data sources
        this.jfilechooser = new JFileChooser(JData.DEFAULT_SIM_DIR);
        if (this.jfilechooser.showOpenDialog(this) == this.jfilechooser.APPROVE_OPTION)
        {
            this.simMenuCloseFile();
            this.startSimPanel(this.jfilechooser.getSelectedFile());
            this.jfilechooser = null;
            return true;
        }
        else
        {
            this.jfilechooser = null;
            return false;
        }
    }
    
    private void simMenuCloseFile()
    {
        if (this.sim_panel != null)
        {
            if (this.sim_panel.closeSimFile())
            {
                this.sim_panel.terminate();
                this.action_close_sim.setEnabled(false);
                this.action_save_sim.setEnabled(false);
                this.action_save_sim_as.setEnabled(false);
                this.tabs.remove(this.sim_panel);
                this.sim_panel = null;
            }
        }
    }
    
    //functions to get and set remote process information
    
    public void simMenuSetRemoteInfo()
    {
        if (this.sgs_dialog == null)
        {
            this.sgs_dialog = new SGSDialog(this);
        }
        this.sgs_dialog.setVisible(true);
    }
    
    /**
     * This is set just before the SGSDialog exits successfully
     */
    public void setSGSInstanceClient(SGSInstanceClient instanceClient,
        boolean newInstance)
    {
        this.instance = instanceClient;
        this.newInstance = newInstance;
        try
        {
            this.styx_steering_panel = new StyxSteeringPanel(instance);
            this.styx_steering_panel.populatePanel();
            this.add(this.styx_steering_panel, "1, 0");
            this.setVisible(true); // repaint the panel
            action_start_sim.setEnabled(true);
        }
        catch(StyxException se)
        {
            JOptionPane.showMessageDialog(this, "Error creating steering panel: "
                + se.getMessage());
        }
    }
    
    public String getCommandLineArgs()
    {
        return this.command_line_args;
    }
    
    public void setCommandLineArgs(String s)
    {
        this.command_line_args = s;
    }
    
    public String getRemoteExe()
    {
        return this.remote_exe;
    }
    
    public void setRemoteExe(String s)
    {
        this.remote_exe = s;
    }
    
    public boolean setRemoteSimFile(String file_path)
    {
        File this_file = new File(file_path);
        if (this_file.isFile())
        {
            this.remote_sim_file = this_file;
            return true;
        }
        else return false;
    }
    
    public String getOpenSimFile()
    {
        if (this.sim_panel != null)
        {
            return this.sim_panel.getSimFilePath();
        }
        return new String();
    }
    
    public File getRemoteSimFile()
    {
        return this.remote_sim_file;
    }
    
    public Vector<File> getSurfaceFiles()
    {
        return this.surface_files;
    }
    
    public void setSurfaceFiles(Vector<File> files)
    {
        this.surface_files = files;
    }
    
    private boolean startRemoteProcess()
    {
        if (this.instance != null)
        {
            // The instance should never be null, otherwise this menu option
            // would have been disabled
            try
            {
                // Only start the service if this is a new instance: if not, we
                // are just watching an existing instance
                if (this.newInstance)
                {
                    this.instance.startService();
                }
                CStyxFile outStreamFile = this.instance.getOutputStream("stdout");
                InputStream is = new CStyxFileInputStream(outStreamFile);
                // TODO: the InputParser constructor blocks until we start reading
                // from the stream
                this.input_parser = new InputParser(is, this);
                input_parser_thread = new Thread(this.input_parser);
                input_parser_thread.start();
                return true;
            }
            catch (Exception e)
            {
                System.out.println("JData.RemoteProcess()");
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public SGSInstanceClient getSGSInstanceClient()
    {
        return this.instance;
    }
    
    public File getDomFile()
    {
        return this.dom_file;
    }
    
    private void simMenuAddSteering()
    {
        if (this.steering_panel == null)
        {
            //create steering panel
            this.steering_panel = new SteeringPanel(this);
            //add steered variable
            if (this.steering_panel.addSteering())
            {
                //resize window and add panel
                this.startSteeringPanel();
                this.action_close_reset.setEnabled(true);
            }
            else
            {
                //if we get here, the steered variable was cancelled so we tidy up
                this.steering_panel.terminate();
                this.steering_panel = null;
            }
        }
        //if steering panel is already created
        else this.steering_panel.addSteering();
    }
    
    public void addSteeredVariableToMenu(String var_name)
    {
        //if this is the first steered variable on the menu, enable menu
        if (this.jmenu_remove_steering.getMenuComponentCount() == 0) this.jmenu_remove_steering.setEnabled(true);
        this.jmenu_remove_steering.add(new JMenuItem(new RemoveVariableAction(var_name)));
    }
    
    class RemoveVariableAction extends AbstractAction
    {
        
        public RemoveVariableAction(String name)
        {
            super(name);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            //destroy steered variable, tab and remove from menu
            //if it is the last tab, remove variables window
            String name = (String)getValue(NAME);
            steering_panel.removeVariable(name);
            int num_components = jmenu_remove_steering.getMenuComponentCount();
            Component [] components = jmenu_remove_steering.getMenuComponents();
            for (int i = 0; i < num_components; ++i)
            {
                if (((JMenuItem)components[i]).getAction() == this)
                {
                    jmenu_remove_steering.remove(components[i]);
                    break;
                }
            }
            if (jmenu_remove_steering.getMenuComponentCount() == 0)
            {
                jmenu_remove_steering.setEnabled(false);
                removeSteeringPanel();
                resetSize();
            }
        }
        
    }
    
    //start various tabbed panes
    
    public void startMessagePane(LBMessage msg)
    {
        //if possible, resize main frame
        int width = (int)(this.getBounds().getWidth());
        int height = (int)(this.getBounds().getHeight());
        int x = (int)(this.getBounds().getX());
        int y = (int)(this.getBounds().getY());
        //create message panel and add to main window
        this.message_panel = new MessagePanel(msg, width);
        //make the window bigger to accommodate the extra message panel
        if (height + 100 < this.scrsize.getHeight())
        {
            if ((y + height + 100) <= this.scrsize.getHeight()) this.setBounds(x, y, width, height + 100);
            else this.setBounds(x, (int)(y - (y + height + 100 - this.scrsize.getHeight())), width, height + 100);
        }
        //if can't accommodate the whole extra size, make it as big as possible
        else this.setBounds(x, 0, width, (int)this.scrsize.getHeight());
        this.add(this.message_panel, "0, 1, 1, 1");
        //this seems to redraw things properly
        this.setVisible(true);
    }
    
    public void start3DVizPanel(LBDomain dom)
    {
        //create 3dviz panel and add to tabs
        this.v3d_panel = new Viz3DPanel(this, this.scrsize, dom);
        this.tabs.addTab("Visualize", this.v3d_panel);
        //enable the image save menu
        this.jmenu_image.setEnabled(true);
        //this seems to redraw things properly
        this.setVisible(true);
    }
    
    public void startAnalysisPanel(LBDomain dom)
    {
        //create analysis panel and add to tabs
        this.analysis_panel = new AnalysisPanel(dom);
        this.tabs.addTab("Analyse", this.analysis_panel);
        //this seems to redraw things properly
        this.setVisible(true);
    }
    
    private void startSimPanel(File sim)
    {
        //create sim panel and add to tabs
        this.sim_panel = new SimPanel(this, sim);
        this.tabs.addTab("Sim File", this.sim_panel);
        //this seems to redraw things properly
        this.setVisible(true);
    }
    
    private void startSteeringPanel()
    {
        //if possible, resize main frame
        int width = (int)(this.getBounds().getWidth());
        int height = (int)(this.getBounds().getHeight());
        int x = (int)(this.getBounds().getX());
        int y = (int)(this.getBounds().getY());
        //make the window bigger to accommodate the extra message panel
        if (width + 202 < this.scrsize.getWidth())
        {
            if ((x + width + 202) <= this.scrsize.getWidth()) this.setBounds(x, y, width + 202, height);
            else this.setBounds((int)(x - (x + width + 202 - this.scrsize.getWidth())), y, width + 202, height);
        }
        //if can't accommodate the whole extra size, make it as big as possible
        else this.setBounds(0, y, (int)this.scrsize.getWidth(), height);
        this.add(this.steering_panel, "1, 0");
        //this seems to redraw things properly
        this.setVisible(true);
    }
    
    //update programme depending on input found by parser
    public void update(Observable o, Object name)
    {
        if (((String)name).matches("NEW_DOMAIN"))
        {
            //TODO: if domain stuff is already set up, destroy it and create new
            LBDomain this_dom = ((InputParser)o).getDomain();
            this.start3DVizPanel(this_dom);
            //remove this until development of AnalysisPanel is completed
            //this.startAnalysisPanel(this_dom);
        }
        if (((String)name).matches("DOMAIN"))
        {
            //TODO: must check that domain stuff is already set up
            LBDomain this_dom = ((InputParser)o).getDomain();
            this.v3d_panel.domainUpdate(this_dom);
        }
        if (((String)name).matches("NEW_MESSAGE"))
        {
            this.startMessagePane(((InputParser)o).getMessage());
        }
    }
    
}
