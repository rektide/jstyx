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
import javax.swing.*;
import java.util.Observable;
import java.util.Observer;
 
public class LBMessageArea extends JTextArea implements Observer {

  public LBMessageArea(LBMessages lb_m) {
    setEditable(false);
    setRows(10);
    setText("Messages:\n");
    lb_m.addObserver(this);
  }

  //must implement update():
  public void update(Observable lb_m, final Object o) {
    //it seems that appending this on the event dispatch thread keeps the window tracking the end
    //
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        append(((String)o) + "\n");
      }
    });
  }

}