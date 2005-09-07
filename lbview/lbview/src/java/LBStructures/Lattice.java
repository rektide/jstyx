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

public class Lattice {

  //goes from 0 at 1, 1, 1, x increases fastest

  private int lx, ly, lz, size;
  private double xo, yo, zo;
  private double dx;
  private boolean periodic = true;
  
  public Lattice(TripleInt d, double d_x, TripleDouble o) {
    lx = d.X(); ly = d.Y(); lz = d.Z();
    size = lx * ly * lz;
    dx = d_x;
    xo = o.X(); yo = o.Y(); zo = o.Z();
  }
  
  public boolean isCongruent(TripleInt d, double d_x, TripleDouble o) {
    return  lx == d.X() && ly == d.Y() && lz == d.Z() &&
            xo == o.X() && yo == o.Y() && zo == o.Z();
  }
  
  public void setNotPeriodic() {periodic = false;}
  
  public TripleInt getDimensions() {return new TripleInt(lx, ly, lz);}
  
  public int getSize() {return size;}
  
  public double getDX() {return dx;}
  
  public String toString() {
    return  "dims: [" + lx + " " + ly + " " + lz + "]\n" + "dx: " + dx +
            "\norigin: [" + xo + " " + yo + " " + zo + "]";
  }
  
  public String stringDims() {
    return  "[" + lx + ", " + ly + ", " + lz + "]";
  }
  
  //subtract dx / 2 to take measurement to the edge of the outermost node, not its centre
  public String stringOrigin() {
    String s = String.format("[ %2.2g, %2.2g, %2.2g]", (xo - (dx / 2)), (yo - (dx / 2)), (zo - (dx / 2)));
    return  s;
  }
  
  //l * d (not (l - 1) * d to take measurement to the edge of the outermost node, not its centre
  public String stringPDims() {
    String s = String.format("[ %2.2g, %2.2g, %2.2g]", lx * dx, ly * dx, lz * dx);
    return  s;
  }
  
  //lattice geometric operations
  
  public TripleInt indexToLatticeCoords(int i) {
    if (i < 0 || i >= size) return new TripleInt(0, 0, 0);
    return new TripleInt(((i % lx) + 1), (((i / lx) % ly) + 1), ((i / (lx * ly)) + 1));
  }

  //returns index of point (x, y, z) accounting for periodic nature of lattice
  public int latticeCoordsToIndex(int x, int y, int z) {
    int index = -1;
    if (!periodic && (x < 1 || y < 1 || z < 1 || x > lx || y > ly || z > lz)) {
      return -1;
    }
    x = x % lx;
    if (x < 1) x += lx;
    y = y % ly;
    if (y < 1) y += ly;
    z = z % lz;
    if (z < 1) z += lz;
    return ((z - 1) * ly * lx) + ((y - 1) * lx) + x - 1;
  }

  public int latticeCoordsToIndex(TripleInt lc) {
    return latticeCoordsToIndex(lc.X(), lc.Y(), lc.Z());
  }

  public TripleDouble latticeCoordsToPhysCoords(int x, int y, int z) {
    return new TripleDouble((xo + (x - 1) * dx), (yo + (y - 1) * dx), (zo + (z - 1) * dx));
  }

  public TripleDouble latticeCoordsToPhysCoords(TripleInt lc) {
    return latticeCoordsToPhysCoords(lc.X(), lc.Y(), lc.Z());
  }

  public TripleDouble indexToPhysCoords(int i){
    return latticeCoordsToPhysCoords(indexToLatticeCoords(i));
  }

  public TripleDouble physCoordsToLatticeCoords(double x, double y, double z) {
    return new TripleDouble(((x - xo) / dx + 1), ((y - yo) / dx + 1), ((z - zo) / dx + 1));
  }

  public TripleDouble physCoordsToLatticeCoords(TripleDouble pc) {
    return physCoordsToLatticeCoords(pc.X(), pc.Y(), pc.Z());
  }

  //this returns coordinates of nearest node (to x, y, z) assuming an infinite lattice
  public TripleInt nearestLatticeCoords(double x, double y, double z) {
    x = ((x - xo) / dx) + 1.5;
    if (x < 0) x -= 1;
    y = ((y - yo) / dx) + 1.5;
    if (y < 0) y -= 1;
    z = ((z - zo) / dx) + 1.5;
    if (z < 0) z -= 1;
    return new TripleInt((int)x, (int)y, (int)z);
  }

  public TripleInt nearestLatticeCoords(TripleDouble pc) {
    return nearestLatticeCoords(pc.X(), pc.Y(), pc.Z());
  }

  public int physCoordsToIndex(TripleDouble pc) {
    return latticeCoordsToIndex(nearestLatticeCoords(pc));
  }

  public int physCoordsToIndex(double x, double y, double z) {
    return latticeCoordsToIndex(nearestLatticeCoords(x, y, z));
  }

  //this returns coordinates of nearest node to pc, in direction (-1, -1, -1), assuming an infinite lattice
  public TripleInt nearestNodeTowardsOrigin(TripleDouble pc) {
    double x = ((pc.X() - xo) / dx) + 1;
    if (x < 0) x -= 1;
    double y = ((pc.Y() - yo) / dx) + 1;
    if (y < 0) y -= 1;
    double z = ((pc.Z() - zo) / dx) + 1;
    if (z < 0) z -= 1; 
    return new TripleInt((int)x, (int)y, (int)z);
  }

}