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

public class TupleDouble extends Tuple {

  private double [] vals;
  
  //must implement setVal
  public boolean setVal(int index, String value, double scale) {
    if (value.matches("\\-")) {
      super.noValue = true;
      return false;
    }
    super.noValue = false;
    vals[index] = Double.parseDouble(value) * scale;
    return true;
  }
  
  public void setVal(int index, double v) {
    super.noValue = false;
    vals[index] = v;
  }
  
  //must implement toString
  public String toString() {
    if (super.noValue) return super.toString();
    else {
      String output = "";
      for (int i = 0; i < vals.length; ++i) output += vals[i] + " ";
      return output;
    }
  }
  
  public String stringData() {
    return this.toString();
  }
  
  //constructor
  public TupleDouble(int i) {
    super(i);
    vals = new double [i];
  }
  
  public double getVal(int i) {//no range checking yet. Does this automatically throw an exception?
    return vals[i];
  }
  
  public Tuple multiply(double d) {
    TupleDouble t = new TupleDouble(super.length);
    for (int i = 0; i < super.length; ++i) t.setVal(i, this.getVal(i) * d);
    return t;
  }
  
  public Tuple subtract(double d) {
    TupleDouble t = new TupleDouble(super.length);
    for (int i = 0; i < super.length; ++i) t.setVal(i, this.getVal(i) - d);
    return t;
  }
  
  public Tuple add(double d) {
    TupleDouble t = new TupleDouble(super.length);
    for (int i = 0; i < super.length; ++i) t.setVal(i, this.getVal(i) + d);
    return t;
  }
  
  public double [] toArray() {
    return this.vals;
  }

}