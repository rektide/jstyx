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

public class PointLine extends PointSet {

  public PointLine() {}
  
  public PointLine(TripleDouble s, TripleDouble f, int n) {
    this.setPoints(s, f, n);
  }

  public void setPoints(TripleDouble s, TripleDouble f, int n) {
    if (n == 1) {
      super.addPoints(s);
      return;
    }
    for (int i = 0; i < n; ++i) {
      double x = s.X() + (f.X() - s.X()) / (n - 1) * i;
      double y = s.Y() + (f.Y() - s.Y()) / (n - 1) * i;
      double z = s.Z() + (f.Z() - s.Z()) / (n - 1) * i;
      super.addPoints(new TripleDouble(x, y, z));
    }
  }

}