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
import LBStructures.*;
import java.util.Observable;
import java.util.Observer;

public class MessagePanel extends JScrollPane implements Terminable, Observer {

  private JTextArea message_area;

  private boolean track_end = true;
  private boolean first_touch = true;

  public MessagePanel(LBMessage msg, int width) {
    //set up message area
    this.message_area = new JTextArea();
    this.message_area.setMaximumSize(new Dimension(width, 120));
    this.message_area.setMinimumSize(new Dimension(10, 120));
    this.message_area.setEditable(false);
    //set up scroll pane
    super.setPreferredSize(new Dimension(width, 120));
    super.setMaximumSize(new Dimension(width + 1000, 120));
    super.setMinimumSize(new Dimension(10, 120));
    super.setViewportView(this.message_area);
    msg.addObserver(this);
  }
  
  public void update(Observable o, Object msg) {
    if (this.getVerticalScrollBar().getValue() !=
        this.getVerticalScrollBar().getMaximum() - this.getVerticalScrollBar().getVisibleAmount()) track_end = false;
    this.message_area.append(((String)msg) + "\n");
    if (track_end) message_area.setCaretPosition(this.message_area.getText().length());
    track_end = true;
  }
  
  //must implement terminate() and isTerminated()
  public void terminate() {
    //System.out.println("message panel dying");
  }
  public boolean isTerminated() {
    return true;
  }
  

}