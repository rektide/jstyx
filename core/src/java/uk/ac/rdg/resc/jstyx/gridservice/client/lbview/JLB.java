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

//run with
//java -cp /usr/local/VTK/bin/vtk.jar:. JLB
//can pipe output of lbflow to input of jlb

import javax.swing.*;

class JLB{

  static private LBGUI gui;
  
  public JLB() {
    gui = new LBGUI();
  }
  
  static public LBGUI getGUI() {
    return gui;
  }
  
  /** Main routine **/
  public static void main( String[] args ){
    JLB app = new JLB();
    //check if the viewer is running in pipeline mode
    int nargs = args.length;
    if (nargs == 1 && args[0].matches("-p")) {
      JLB.getGUI().startPipeParser();
    }
    //what is this here for?
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        gui.pack();
        gui.setVisible(true);
      }
    });
    
  }
  
}
