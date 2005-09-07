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

public class PointPlane extends PointLine {

  private int m, n;

  public PointPlane() {}
  
  public PointPlane(TripleDouble a_s, TripleDouble a_f, TripleDouble b_s, TripleDouble b_f, int x, int y) {
    m = x;
    n = y;
    this.setPoints(a_s, a_f, b_s, b_f, n, m);
  }

  public void setPoints(TripleDouble a_s, TripleDouble a_f, TripleDouble b_s, TripleDouble b_f, int n, int m) {
    if (m == 1) {//to avoid divide by zero
      super.setPoints(a_s, a_f, n);
      return;
    }
    TripleDouble s_inc = new TripleDouble((b_s.X() - a_s.X()) / (m - 1), (b_s.Y() - a_s.Y()) / (m - 1), (b_s.Z() - a_s.Z()) / (m - 1));
    TripleDouble f_inc = new TripleDouble((b_f.X() - a_f.X()) / (m - 1), (b_f.Y() - a_f.Y()) / (m - 1), (b_f.Z() - a_f.Z()) / (m - 1));
    TripleDouble s = new TripleDouble(a_s);
    TripleDouble f = new TripleDouble(a_f);
    for (int i = 0; i < m; ++i) {
      super.setPoints(s, f, n);
      s = s.add(s_inc);
      f = f.add(f_inc);
    }
  }
  
  public String stringVals() {
    String output = "";
    double r2;
    double R2 = Math.pow(5.015896e-5, 2);
    double v;
    for (int j = 0; j < n; ++j) {
      for (int i = 0; i < m; ++i) {
        r2 = Math.pow(points.get(i + m * j).Y(), 2) + Math.pow(points.get(i + m * j).Z(), 2);
        v = (R2 - r2) / (4 * 1.002283e-3);
//        if (v < 0) v = 0;
//        output += v + " ";
        output += vals.get(i + m * j).subtract(v).stringData() + " ";
      }
//      System.out.println(output);
      output = "";
    }
    return output;
  }

}