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

public class Mapping {

  private double L0;
  private double T0;
  private double M0;
  
  public Mapping(TripleDouble t) {
    L0 = t.X();
    T0 = t.Y();
    M0 = t.Z();
  }
  
  public double getL0() {
    return L0;
  }
  
  public double getT0() {
    return T0;
  }
  
  public double getM0() {
    return M0;
  }
  
  public double simToPhys(double x, int l, int t, int m) {
    return x * Math.pow(L0,  l) * Math.pow(T0,  t) * Math.pow(M0,  m);
  }
  
  public double simToPhys(double x, TripleInt t) {
    return x * Math.pow(L0,  t.X()) * Math.pow(T0,  t.Y()) * Math.pow(M0,  t.Z());
  }
  
  public double physToSim(double x, int l, int t, int m) {
    return x * Math.pow(L0, -l) * Math.pow(T0, -t) * Math.pow(M0, -m);
  }
  
  public double physToSim(double x, TripleInt t) {
    return x * Math.pow(L0, -t.X()) * Math.pow(T0, -t.Y()) * Math.pow(M0, -t.Z());
  }  

}