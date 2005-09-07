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

class Cube {

  private Tuple [] t;
  
  //corners in order
  //(0, 0, 0)
  //(0, 0, 1)
  //(0, 1, 0)
  //(0, 1, 1)
  //(1, 0, 0)
  //(1, 0, 1)
  //(1, 1, 0)
  //(1, 1, 1)
  public Cube(Tuple [] corners) {
    t = corners;
  }
  
  public Cube() {} //default constructor

  public TupleDouble interpolate(double x, double y, double z) { //if we interpolate, these must be double
    int l = 0;
    for (int i = 0; i < 8; ++i) {//find length of tuple bearing in mind only one has to be non-zero
      if ((l = t[i].getLength()) != 0) break;
    }
    TupleDouble interpolated_vals = new TupleDouble(l);
    double a;
    double [] v = new double[8];
    for (int i = 0; i < l; ++i) {
      for (int j = 0; j < 8; ++j) {
        if (t[j].hasNoValue()) v[j] = 0;
        else v[j] = ((TupleDouble)(t[j])).getVal(i);
//System.out.println("vj: " + v[j]);
      }
      a = v[0] * (1 - x) * (1 - y) * (1 - z) +
          v[1] * (1 - x) * (1 - y) * z +
          v[2] * (1 - x) * y * (1 - z) +
          v[3] * (1 - x) * y * z +
          v[4] * x * (1 - y) * (1 - z) +
          v[5] * x * (1 - y) * z +
          v[6] * x * y * (1 - z) +
          v[7] * x * y * z;
      interpolated_vals.setVal(i, a);
//System.out.println("fx: " + x + " fy: " + y + " fz: " + z);
//System.out.println("value: " + a);
    }
    return interpolated_vals;
  }

  public Tuple interpolate(TripleDouble pc) {
    return interpolate(pc.X(), pc.Y(), pc.Z());
  }

  public boolean hasNulls() {
    for (int i = 0; i < 8; ++i) {
      if (t[i].hasNoValue()) return true; //returns true if one of the corners is null
    }
    return false;
  }
  
  public boolean isNull(int i) {
    return t[i].hasNoValue();
  }
  
  public String toString() {
    return  "(0, 0, 0): " + t[0].toString() + "\n" +
            "(0, 0, 1): " + t[1].toString() + "\n" +
            "(0, 1, 0): " + t[2].toString() + "\n" +
            "(0, 1, 1): " + t[3].toString() + "\n" +
            "(1, 0, 0): " + t[4].toString() + "\n" +
            "(1, 0, 1): " + t[5].toString() + "\n" +
            "(1, 1, 0): " + t[6].toString() + "\n" +
            "(1, 1, 1): " + t[7].toString();
  }

}