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

public class TripleDouble {

  private double x;
  private double y;
  private double z;
  
  public TripleDouble(double X, double Y, double Z) {
    x = X;
    y = Y;
    z = Z;
  }
  
  public TripleDouble(TripleDouble t) {
    x = t.X();
    y = t.Y();
    z = t.Z();
  }
  
  public void setX(double X) {x = X;}
  public void setY(double Y) {y = Y;}
  public void setZ(double Z) {z = Z;}
  
  public double X() {return x;}
  public double Y() {return y;}
  public double Z() {return z;}
  
  public double getSize() {return x * y * z;}
  
  public TripleDouble add(TripleDouble t) {
    return new TripleDouble(x + t.X(), y + t.Y(), z + t.Z());
  }
  
  public TripleDouble add(TripleInt t) {
    return new TripleDouble(x + t.X(), y + t.Y(), z + t.Z());
  }
  
  //don't need another one for int as it is converted automatically
  public TripleDouble add(double X, double Y, double Z) {
    return new TripleDouble(x + X, y + Y, z + Z);
  }
  
  public TripleDouble subtract(TripleDouble t) {
    return new TripleDouble(x - t.X(), y - t.Y(), z - t.Z());
  }
  
  public TripleDouble subtract(TripleInt t) {
    return new TripleDouble(x - t.X(), y - t.Y(), z - t.Z());
  }
  
  //don't need another one for int as it is converted automatically
  public TripleDouble subtract(double X, double Y, double Z) {
    return new TripleDouble(x - X, y - Y, z - Z);
  }
  
  public boolean isEqualTo(TripleDouble t) {
    return x == t.X() && y == t.Y() && z == t.Z();
  }
  
  public TripleDouble cross(TripleDouble t) {
    return new TripleDouble((y * t.Z() - z * t.Y()), (z * t.X() - x * t.Z()), (x * t.Y() - t.Y() * x));
  }
  
  public String toString() {
    return "[ " + x + ", " + y + ", " + z + "]";
  }
  
  public String stringData() {
    return x + " " + y + " " + z;
  }
  
  public double [] toArray() {
    return new double [] {x, y, z};
  }

}