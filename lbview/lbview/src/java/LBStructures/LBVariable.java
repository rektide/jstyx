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

public class LBVariable {

  //compulsory information
  private char label;
  private TripleInt dimension_map;
  private int tuple;
  private int data_type;
  private String type_string;
  //optional information
  private String description;
  private double last_updated; //this will come from messages for pipe mode only
  
  private Tuple [] data;
  
  public final static int BOOLEAN = 0;
  public final static int INTEGER = 1;
  public final static int DOUBLE  = 2;

  public LBVariable(){}
  
  //constructor (size, label, map, tuple, type, description, last update)
  public LBVariable(int s, char l, TripleInt m, int t, int p, String d, double u) {
    label = l;
    dimension_map = new TripleInt(m);
    tuple = t;
    data_type = p;
    description = d;
    last_updated = u;
    switch(data_type) {
      case DOUBLE:  type_string = "DOUBLE";
                    data = new TupleDouble[s];
                    for (int i = 0; i < s; ++i) data[i] = new TupleDouble(t);
                    break;
      case INTEGER: type_string = "INTEGER";
                    data = new TupleInteger[s];
                    for (int i = 0; i < s; ++i) data[i] = new TupleInteger(t);
                    break;
    }
  }
  
  public Tuple getTuple(int i) {
    if (i < 0) return new Tuple(tuple); //this means that tuple for a point outside the lattice is sought
    return data[i];
  }
  
  public void putTuple(int i, Tuple t) {
    data[i] = t; //this should probably check that it is the right type of tuple
  }
  
  public int getTupleSize() {
    return tuple;
  }
  
  public TripleInt getMap() {
    return dimension_map;
  }
  
  public char getLabel() {return label;}
  
  public int getDataType() {return data_type;}
  
  public String stringVals() {
    String s = "";
    for (int i = 0; i < data.length; ++i) {
      s += data[i].toString() + "\n";
    }
    return s;
  }
  
  public String stringMetaData() {
    String s =  label + ":\n  " + description + 
                "\n  (" + dimension_map.X() + " " + dimension_map.Y() + " " + dimension_map.Z() + ")\n  <" +
                tuple + ">\n  " + type_string;
    return s;
  }
  
  public String toString() {
    return this.stringMetaData() + " {\n" + this.stringVals() + "}";
  }

}