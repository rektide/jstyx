package uk.ac.rdg.resc.jstyx.gridservice.client.lbview;
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;
import java.io.*;

import uk.ac.rdg.resc.jstyx.gridservice.client.StreamViewer;

public class LBGUI extends StreamViewer implements Observer
{
    
    private Dimension scrsize;
    
    private JMenuBar jmenubar_main;
    private JMenu jmenu_view;
    private JMenu jmenu_simulation;
    private JPanel jpanel_controls;
    private Box box_display_options;
    private JCheckBox jcheckbox_outline;
    private JCheckBox jcheckbox_surface;
    private JSlider jslider_surface;
    private JCheckBox jcheckbox_streamlines;
    private JPanel jpanel_display;
    private LBVTKPanel lb_panel;
    private LBMessageArea lb_msg;
    
    private ViewAction openAction;
    private JFileChooser jfilechooser_open;
    private Thread input_parser;
    private File input_file;
    
    private ViewAction closeAction;
    private ViewAction pipeAction;
    private ViewAction remoteAction;
    
    public LBGUI()
    {
        //enable event handling
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        
        //initialize main window (level 0)
        setTitle("LB viewer");
        Toolkit tk = getToolkit();
        scrsize = tk.getScreenSize();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        //add menu bar
        jmenubar_main = new JMenuBar();
        setJMenuBar(jmenubar_main);
        
        JMenu jmenu_view = new JMenu("View");
        JMenu jmenu_simulation = new JMenu("Simulation");
        
        openAction = new ViewAction("Open");
        closeAction = new ViewAction("Close");
        pipeAction = new ViewAction("Open pipe");
        remoteAction = new ViewAction("Open remote");
        
        closeAction.setEnabled(false);
        
        jmenu_view.add(new JMenuItem(openAction));
        jmenu_view.add(new JMenuItem(closeAction));
        jmenu_view.add(new JMenuItem(pipeAction));
        jmenu_view.add(new JMenuItem(remoteAction));
        
        jfilechooser_open = new JFileChooser();
        
        jmenubar_main.add(jmenu_view);
        jmenubar_main.add(jmenu_simulation);
        
        //initialize level 1 components
        jpanel_controls = new JPanel();
        box_display_options = Box.createVerticalBox();
        createJPanelControls();
        //initialize display panel
        jpanel_display = new JPanel();
        BoxLayout box_display = new BoxLayout(jpanel_display, BoxLayout.Y_AXIS);
        jpanel_display.setLayout(box_display);
        createLBPanel();
        //set up contents of main window
        Container content = getContentPane();
        BoxLayout box_main = new BoxLayout(content, BoxLayout.X_AXIS);
        content.setLayout(box_main);
        content.add(jpanel_controls);
        content.add(jpanel_display);
    }
    
    public void startPipeParser()
    {
        //start input parser with system in
        input_parser = new Thread(new InputParser());
        input_parser.start();
    }
    
    private void createJPanelControls()
    {
        jcheckbox_outline = new JCheckBox("Outline", true);//if the default for this is display, add true.
        jcheckbox_outline.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                jcheckbox_outline_mouseClicked(e);
            }
        });
        
        box_display_options.add(jcheckbox_outline);
        //grey it out until update enables it
        jcheckbox_outline.setEnabled(false);
        
        jcheckbox_surface = new JCheckBox("Surface");
        jcheckbox_surface.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                jcheckbox_surface_mouseClicked(e);
            }
        });
        box_display_options.add(jcheckbox_surface);
        //grey it out until update enables it
        jcheckbox_surface.setEnabled(false);
        
        jslider_surface = new JSlider(JSlider.HORIZONTAL, 0, 100, 20);
        jslider_surface.setMajorTickSpacing(100);
        jslider_surface.setMinorTickSpacing(10);
        jslider_surface.setPaintTicks(true);
        jslider_surface.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                jslider_surface_changed(e);
            }
        });
        box_display_options.add(jslider_surface);
        jslider_surface.setEnabled(false);
        
        jcheckbox_streamlines = new JCheckBox("Streamlines");
        jcheckbox_streamlines.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                jcheckbox_streamlines_mouseClicked(e);
            }
        });
        box_display_options.add(jcheckbox_streamlines);
        //grey it out until update enables it
        jcheckbox_streamlines.setEnabled(false);
        
        jpanel_controls.add(box_display_options);
    }
    
    private void destroyJPanelControls()
    {
        box_display_options.remove(jcheckbox_outline);
        jcheckbox_outline = null;
        box_display_options.remove(jcheckbox_surface);
        jcheckbox_surface = null;
        box_display_options.remove(jslider_surface);
        jslider_surface = null;
        box_display_options.remove(jcheckbox_streamlines);
        jcheckbox_streamlines = null;
    }
    
    private void createLBPanel()
    {
        lb_panel = new LBVTKPanel(scrsize);
        jpanel_display.add(lb_panel);
    }
    
    public void observe(LBData lb_data)
    {
        lb_data.addObserver(this);
    }
    
    //must implement update():
    public void update(Observable lb_data, Object o)
    {
        //I think this needs to be on the event dispatching thread (though I don't know why!)
        //SwingUtilities.invokeLater(new Runnable() {
        //  public void run() {
        //    box_display_options.add(jcheckbox_outline);
        //  }
        //});
        //pack();
        char what_changed = ('o');
        switch(what_changed)
        {
            case 'o': if (!jcheckbox_outline.isEnabled()) jcheckbox_outline.setEnabled(true);
            break;
            case 't': if (!jcheckbox_surface.isEnabled()) jcheckbox_surface.setEnabled(true);
            if (!jslider_surface.isEnabled()) jslider_surface.setEnabled(true);
            break;
            case 'V': if (!jcheckbox_streamlines.isEnabled()) jcheckbox_streamlines.setEnabled(true);
            break;
        }
    }
    
    public void createMessageArea(final LBMessages lb_m)
    {
        //this may need to be put on the event dispatching thread
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                lb_msg = new LBMessageArea(lb_m);
                JScrollPane jscrollpane_msg = new JScrollPane(lb_msg);
                jpanel_display.add(jscrollpane_msg);
                pack();
            }
        });
    }
    
    void jcheckbox_outline_mouseClicked(MouseEvent e)
    {
        if (jcheckbox_outline.isEnabled())
        {
            lb_panel.toggleOutline();
        }
    }
    
    void jcheckbox_surface_mouseClicked(MouseEvent e)
    {
        if (jcheckbox_surface.isEnabled())
        {
            lb_panel.toggleSurface();
        }
    }
    
    void jslider_surface_changed(ChangeEvent e)
    {
        if (jslider_surface.isEnabled())
        {
            if (!jslider_surface.getValueIsAdjusting())
            {
                lb_panel.setSurfaceOpacity((int)jslider_surface.getValue());
            }
        }
    }
    
    void jcheckbox_streamlines_mouseClicked(MouseEvent e)
    {
        if (jcheckbox_streamlines.isEnabled())
        {
            lb_panel.toggleStreamlines();
        }
    }
    
    public LBMessageArea getMsgArea()
    {
        return lb_msg;
    }
    
    public LBVTKPanel getVTKPanel()
    {
        return lb_panel;
    }
    
    public double getSliderSurfaceValue()
    {
        return (double)jslider_surface.getValue();
    }
    
    //Actions
    
    class ViewAction extends AbstractAction
    {
        ViewAction(String name)
        {
            super(name);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            String name = (String)getValue(NAME);
            if (name.equals(openAction.getValue(NAME)))
            {
                if (jfilechooser_open.showOpenDialog(LBGUI.this) == jfilechooser_open.APPROVE_OPTION)
                {
                    //shut down old parsing thread and viewer first if extant
                    if (input_file != null)
                    {
                        closeViewFile();
                    }
                    input_file = jfilechooser_open.getSelectedFile();
                    input_parser = null;
                    input_parser = new Thread(new InputParser(input_file));
                    input_parser.start();
                    closeAction.setEnabled(true);
                    pack();
                }
            }
            else if (name.equals(closeAction.getValue(NAME)))
            {
                closeViewFile();
                pack();
            }
            else if (name.equals(pipeAction.getValue(NAME)))
            {
                try
                {
                    Runtime rt = Runtime.getRuntime();
                    Process proc = rt.exec("c:\\cygwin\\home\\jdb\\lbflow-0.1\\src\\lbflow -i /home/jdb/lbflow-0.1/tests/test.sim");
                    InputStream is = proc.getInputStream();
                    input_parser = new Thread(new InputParser(is));
                    input_parser.start();
                    closeAction.setEnabled(true);
                    pack();
                }
                catch (IOException ex)
                {
                    System.err.println(ex);
                }
            }
            else if (name.equals(remoteAction.getValue(NAME)))
            {
                System.out.println("Opening remote server");
                InputStream is = getInputStream();

                // Now we can pass the stream to the parser as before
                // TODO: this repeats code! should move this to a new 
                // method
                input_parser = new Thread(new InputParser(is));
                input_parser.start();
                closeAction.setEnabled(true);
                pack();
            }
        }
        
    }
    
    private void closeViewFile()
    {
        //wait for input_parser to finish (if it is running)
        try
        {
            input_parser.join();
        }
        catch (InterruptedException e)
        {
            System.out.println(e);
        }
        destroyJPanelControls();
        createJPanelControls();
        //destroy old lb_panel so that all the old actors are removed and we can start again.
        jpanel_display.remove(lb_panel);
        lb_panel = null;
        createLBPanel();
        input_file = null;
        closeAction.setEnabled(false);
    }
    
    /**
     * Required by StreamViewer. Does nothing here as we will be reading from the stream.
     */
    public void newDataArrived(byte[] data, int size) {}
    
}