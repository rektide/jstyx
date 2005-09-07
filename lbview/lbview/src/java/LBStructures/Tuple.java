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

public class Tuple {

  protected boolean noValue = true;
  protected int length = 0;
  
  protected Tuple(int l) {length = l;}
  
  public boolean hasNoValue() {return noValue;}

  //setVal has no meaning for a non-typed tuple
  public boolean setVal(int i, String v, double s) {return false;}
  
  //these is included to allow them to be called polymorphically from derived classes
  public double getVal(int i) {return 0;}
  
  //getLength has no meaning for a non-typed tuple
  public int getLength() {return length;}
  
  public String toString() {return "-";}
  
  public String stringData() {
    String output = "";
    for (int i = 0; i < length; ++i) output += "0 ";
    return output;
  }
  
  public Tuple multiply(double d) {
    return this;
  }
  
  public Tuple subtract(double d) {
    return this;
  }
  
  public Tuple add(double d) {
    return this;
  }

}