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
    
    private JPanel jpanel_controls;
    private Box box_display_options;
    private JCheckBox jcheckbox_outline;
    private JCheckBox jcheckbox_surface;
    private JSlider jslider_surface;
    private JCheckBox jcheckbox_streamlines;
    private JPanel jpanel_display;
    private LBVTKPanel lb_panel;
    private LBMessageArea lb_msg;
    
    private Thread input_parser;
    
    public LBGUI()
    {
        //enable event handling
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        
        //initialize main window (level 0)
        setTitle("LB viewer");
        Toolkit tk = getToolkit();
        scrsize = tk.getScreenSize();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
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
    
    public void start()
    {
        if (!this.started)
        {
            input_parser = new Thread(new InputParser(getInputStream(), lb_panel.getLBData()));
            input_parser.start();
            pack();
            super.start();
        }
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
        lb_panel = new LBVTKPanel(scrsize, this);
        jpanel_display.add(lb_panel);
    }
    
    public void observe(LBData lb_data)
    {
        lb_data.addObserver(this);
    }
    
    //must implement update():
    public void update(Observable lb_data, Object o)
    {
        char what_changed = ((Character)o).charValue();
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
    
    /**
     * Required by StreamViewer. Does nothing here as we will be reading from the stream.
     */
    public void newDataArrived(byte[] data, int size) {}
    
}