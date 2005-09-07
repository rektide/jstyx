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

//this should have ability to look at the different data sources
//TODO: will bring up variable data and allow interpolation of it. The created point sets should
//be sent to the vizualization panel and saved to file.

public class AnalysisPanel extends JPanel implements Terminable, ActionListener {

  private LBDomain lb_domain;
  
  private JButton jbutton_interpolate;

  public AnalysisPanel(LBDomain dom) {
    lb_domain = dom;
    this.jbutton_interpolate = new JButton("Interpolate");
    this.jbutton_interpolate.addActionListener(this);
    this.add(this.jbutton_interpolate);
  }

  //must implement actionPerformed()
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(this.jbutton_interpolate)) {
      this.interpolate();
    }
  }
  
  private void interpolate() {
  try {
      TripleDouble point = new TripleDouble(0, 0.2, 0.2);
      TripleDouble start = new TripleDouble(0, 0, -1);
      TripleDouble finish = new TripleDouble(0, 0, 1);
      PointLine line = new PointLine(start, finish, 10);
      TripleDouble a_s = new TripleDouble(0, 0, 0);
      TripleDouble a_f = new TripleDouble(0, 0, 5e-05);
      TripleDouble b_s = new TripleDouble(0, 5e-05, 0);
      TripleDouble b_f = new TripleDouble(0, 5e-05, 5e-05);
      PointPlane plane = new PointPlane(a_s, a_f, b_s, b_f, 100, 100);
      LBVariable this_var = lb_domain.getVariable('s');
      //System.out.println(this_var.stringMetaData());
      //System.out.println(this_var);
      //System.out.println(this_dom.interpolate(point, this_var));
      //System.out.println(this_dom.interpolate(line, this_var));
      lb_domain.interpolate(plane, this_var).stringVals();
    } catch (Exception e) {
      System.out.println("AnalysisPanel.interpolate()");
      e.printStackTrace();
    }  
  }
  
  //must implement terminate() and isTerminated()
  public void terminate() {
    //System.out.println("analysis dying");
  }
  public boolean isTerminated() {
    return true;
  }
  

}
