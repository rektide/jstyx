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

package LBStructures;

import java.util.Vector;

public class PointSet {

  protected Vector<TripleDouble> points = new Vector<TripleDouble>();
  protected Vector<Tuple> vals = new Vector<Tuple>();
  
  public PointSet() {}
  
  public void addPoints(TripleDouble p) {
    points.add(p);
    vals.setSize(points.size());
  }
  
  public void setVal(int i, Tuple t) {
    vals.set(i, t);
  }
  
  public Vector<TripleDouble> getPoints() {
    return points;
  }
  
  public String toString() {
    String output = "";
    for (int i = 0; i < points.size(); ++i) {
      output += "Point: " + points.get(i).toString() + " value " + vals.get(i).toString() + '\n';
    }
    return output;
  }
  
  public String stringVals() {
    String output = "";
    for (int i = 0; i < points.size(); ++i) {
      output += vals.get(i).stringData() + '\n';
    }
    return output;
  }
  
  public String stringData() {
    String output = "";
    for (int i = 0; i < points.size(); ++i) {
      output += points.get(i).stringData() + " " + vals.get(i).stringData() + '\n';
    }
    return output;
  }

}