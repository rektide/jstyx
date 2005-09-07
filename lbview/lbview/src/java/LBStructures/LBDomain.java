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

import java.util.HashMap;
import java.util.Collection;
import java.util.Vector;
import java.util.Observable;

//TODO: should (if required) automatically serialize any overwritten variable data

public class LBDomain extends Observable {

  private Lattice lattice;
  private Mapping map;
  private HashMap<Character, LBVariable> variable_map;

  public LBDomain(TripleInt d, double d_x, TripleDouble o, TripleDouble m) {
    lattice = new Lattice(d, d_x, o);
    map = new Mapping(m);
    variable_map = new HashMap<Character, LBVariable>(7);
  }
  
  public boolean isCongruent(TripleInt d, double d_x, TripleDouble o, TripleDouble m) {
    return  lattice.isCongruent(d, d_x, o) &&
            map.getL0() == m.X() && map.getT0() == m.Y() && map.getM0() == m.Z();
  }
  
  public TripleInt getDimensions() {
    return this.lattice.getDimensions();
  }
  
  public Lattice getLattice() {
    return this.lattice;
  }
  
  public Mapping getMapping() {
    return this.map;
  }
  
  public LBVariable addVariableToMap(char l, TripleInt m, int t, int p, String d, double u) {
    //remove variable if it already exists as it is to be updated
    
//TODO: should (if required) automatically serialize any overwritten variable data
    variable_map.remove(l) ;
    variable_map.put(l, new LBVariable(lattice.getSize(), l, m, t, p, d, u));
    return variable_map.get(l);
  }
  
  public boolean hasVariables() {
    return (variable_map.size() != 0);
  }
  
  public String getVariableLabels() {
    String s = "";
    Collection<LBVariable> vars = variable_map.values();
    for (LBVariable v : vars) {
      s += v.getLabel();
    }
    return s;
  }
  
  public LBVariable getVariable(char l) throws NoSuchFieldException {
    LBVariable this_var;
    if ((this_var = variable_map.get(l)) == null) throw new NoSuchFieldException("No such LBVariable: " + l);
    return this_var;
  }
  
  public String stringVariables() {
    String s = "";
    Collection<LBVariable> vars = variable_map.values();
    for (LBVariable v : vars) {
      s += v.stringMetaData() + "\n----------\n";
    }
    return s;
  }
  
  public String toString() {
    String s = lattice.toString();
    s += "\nMapping: L0 " + map.getL0() + " T0 " + map.getT0() + " M0 " + map.getM0();
    Collection<LBVariable> vars = variable_map.values();
    for (LBVariable v : vars) {
      s += v.toString() + "\n";
    }
    return s;
  }
  
  //interpolation routines

  public PointSet interpolate(PointSet set, LBVariable var) {
    Vector<TripleDouble> points = set.getPoints();
    for (int i = 0; i < points.size(); ++i) {
      set.setVal(i, this.interpolate(points.get(i), var));
    }
    return set;
  }
  
  public PointSet interpolate(PointSet set, char l) throws NoSuchFieldException {
    LBVariable var = this.getVariable(l);
    return this.interpolate(set, var);
  }

  public Tuple interpolate(TripleDouble pc, char l) throws NoSuchFieldException {
    LBVariable var = this.getVariable(l);
    return this.interpolate(pc, var);
  }
  
  public Tuple interpolate(TripleDouble pc, LBVariable var) {
    TripleInt nearest = lattice.nearestLatticeCoords(pc);
    int index = lattice.latticeCoordsToIndex(nearest);
    //deal with cases where no value will be found
    if (index < 0 || index >= lattice.getSize()) {
//System.out.println("off lattice");
      return new Tuple(var.getTupleSize()); //off lattice
    }
    if (var.getTuple(index).hasNoValue()) {
//System.out.println("not a fluid node");
      return new Tuple(var.getTupleSize()); //not a fluid node (no value at point)
    }
    //now cases where integer or boolean value will be returned (no interpolation)
    if (var.getDataType() == LBVariable.BOOLEAN || var.getDataType() == LBVariable.INTEGER) {
//System.out.println("non-interpolatable type");
      return var.getTuple(index);
    }
    //now for cases where interpolation will be required
    TripleDouble frac = lattice.physCoordsToLatticeCoords(pc).subtract(lattice.nearestNodeTowardsOrigin(pc));
    Cube unit_cube = this.createCube(pc, lattice, var);
//System.out.println(unit_cube);
    //straightforward case: all corners of cube contain values
    if (!unit_cube.hasNulls()) {
//System.out.println("straight interpolation");
      return unit_cube.interpolate(frac);
    }
    //if a corner has no value, look at smaller cube
//System.out.println("interpolate via small lattice");
    Cube sc = this.createSmallCube(pc, var, unit_cube);
//System.out.println(sc);
    return sc.interpolate((frac.X() * 2) % 1, (frac.Y() * 2) % 1, (frac.Z() * 2) % 1);
  }
  
  public Cube createCube(TripleDouble pc, Lattice l, LBVariable var) {
    TripleInt origin = l.nearestNodeTowardsOrigin(pc);
    Tuple [] corners = new Tuple[8];
    corners[0] = var.getTuple(l.latticeCoordsToIndex(origin));
    corners[1] = var.getTuple(l.latticeCoordsToIndex(origin.add(0, 0, 1)));
    corners[2] = var.getTuple(l.latticeCoordsToIndex(origin.add(0, 1, 0)));
    corners[3] = var.getTuple(l.latticeCoordsToIndex(origin.add(0, 1, 1)));
    corners[4] = var.getTuple(l.latticeCoordsToIndex(origin.add(1, 0, 0)));
    corners[5] = var.getTuple(l.latticeCoordsToIndex(origin.add(1, 0, 1)));
    corners[6] = var.getTuple(l.latticeCoordsToIndex(origin.add(1, 1, 0)));
    corners[7] = var.getTuple(l.latticeCoordsToIndex(origin.add(1, 1, 1)));
    return new Cube(corners);
  }
  
  public Cube createSmallCube(TripleDouble pc, LBVariable var, Cube unit_cube) {
    Lattice sl = new Lattice(new TripleInt(3, 3, 3), 0.5, new TripleDouble(0, 0, 0));
    //set values from interpolation of large cube
    LBVariable sl_var = new LBVariable(27, 'a', var.getMap(), var.getTupleSize(), LBVariable.DOUBLE, "", 0);
    for (int i = 0; i < 27; ++i) {
      sl_var.putTuple(i, unit_cube.interpolate(sl.indexToPhysCoords(i)));
    }
  //System.out.println(sl);
  //System.out.println(sl_var);
    //and modify according to null points in large cube
    //create zero tuple
    TupleDouble zt = new TupleDouble(var.getTupleSize());
    for (int i = 0; i < var.getTupleSize(); ++i) {
      zt.setVal(i, 0);
    }
    if (unit_cube.isNull(0)) {//point 0 in small lattice
      sl_var.putTuple(1, zt);
      sl_var.putTuple(3, zt);
      sl_var.putTuple(4, zt);
      sl_var.putTuple(9, zt);
      sl_var.putTuple(10, zt);
      sl_var.putTuple(12, zt);
    }
    if (unit_cube.isNull(1)) {//point 18 in small lattice
      sl_var.putTuple(9, zt);
      sl_var.putTuple(10, zt);
      sl_var.putTuple(12, zt);
      sl_var.putTuple(19, zt);
      sl_var.putTuple(21, zt);
      sl_var.putTuple(22, zt);
    }
    if (unit_cube.isNull(2)) {//point 6 in small lattice
      sl_var.putTuple(3, zt);
      sl_var.putTuple(4, zt);
      sl_var.putTuple(7, zt);
      sl_var.putTuple(12, zt);
      sl_var.putTuple(15, zt);
      sl_var.putTuple(16, zt);
    }
    if (unit_cube.isNull(3)) {//point 24 in small lattice
      sl_var.putTuple(12, zt);
      sl_var.putTuple(15, zt);
      sl_var.putTuple(16, zt);
      sl_var.putTuple(21, zt);
      sl_var.putTuple(22, zt);
      sl_var.putTuple(25, zt);
    }
    if (unit_cube.isNull(4)) {//point 2 in small lattice
      sl_var.putTuple(1, zt);
      sl_var.putTuple(4, zt);
      sl_var.putTuple(5, zt);
      sl_var.putTuple(10, zt);
      sl_var.putTuple(11, zt);
      sl_var.putTuple(14, zt);
    }
    if (unit_cube.isNull(5)) {//point 20 in small lattice
      sl_var.putTuple(10, zt);
      sl_var.putTuple(11, zt);
      sl_var.putTuple(14, zt);
      sl_var.putTuple(19, zt);
      sl_var.putTuple(22, zt);
      sl_var.putTuple(23, zt);
    }
    if (unit_cube.isNull(6)) {//point 8 in small lattice
      sl_var.putTuple(4, zt);
      sl_var.putTuple(5, zt);
      sl_var.putTuple(7, zt);
      sl_var.putTuple(14, zt);
      sl_var.putTuple(16, zt);
      sl_var.putTuple(17, zt);
    }
    if (unit_cube.isNull(7)) {//point 26 in small lattice
      sl_var.putTuple(14, zt);
      sl_var.putTuple(16, zt);
      sl_var.putTuple(17, zt);
      sl_var.putTuple(22, zt);
      sl_var.putTuple(23, zt);
      sl_var.putTuple(25, zt);
    }
    //centre point must be zero (cos at least one of corners is zero)
    sl_var.putTuple(13, zt);
  //System.out.println(sl);
  //System.out.println(sl_var);
    //find correct cube in small lattice
    TripleDouble pcn = lattice.latticeCoordsToPhysCoords(lattice.nearestLatticeCoords(pc));
    //calculate which octant the point is in
    // Octant depends on relative position of pcn (nearest lattice point) and pc (point of interest)
    // If pc > pcn in a given lattice direction, a + is shown, otherwise a -
    // e.g. point 1.1, 1.1, 0.9 is (+, +, -) relative to point 1, 1, 1 (hence quadrant 2)
    // 1 = (+, +, +)
    // 2 = (+, +, -)
    // 3 = (+, -, +)
    // 4 = (+, -, -)
    // 5 = (-, +, +)
    // 6 = (-, +, -)
    // 7 = (-, -, +)
    // 8 = (-, -, -)
    int octant = 8 - (4 * (pcn.X() > pc.X() ? 0 : 1) + 2 * (pcn.Y() > pc.Y() ? 0 : 1) + (pcn.Z() > pc.Z() ? 0 : 1));
  //System.out.println("octant: " + octant);
    Cube cube = new Cube();
    switch(octant) {
      case 1: cube = this.createCube(new TripleDouble(0.25, 0.25, 0.25), sl, sl_var);//could be any point in the correct quadrant
              break;
      case 2: cube = this.createCube(new TripleDouble(0.25, 0.25, 0.75), sl, sl_var);
              break;
      case 3: cube = this.createCube(new TripleDouble(0.25, 0.75, 0.25), sl, sl_var);
              break;
      case 4: cube = this.createCube(new TripleDouble(0.25, 0.75, 0.75), sl, sl_var);
              break;
      case 5: cube = this.createCube(new TripleDouble(0.75, 0.25, 0.25), sl, sl_var);
              break;
      case 6: cube = this.createCube(new TripleDouble(0.75, 0.25, 0.75), sl, sl_var);
              break;
      case 7: cube = this.createCube(new TripleDouble(0.75, 0.75, 0.25), sl, sl_var);
              break;
      case 8: cube = this.createCube(new TripleDouble(0.75, 0.75, 0.75), sl, sl_var);
              break;
    }
    return cube;
  }

}