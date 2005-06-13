package uk.ac.rdg.resc.jstyx.gridservice.client.lbview;
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

import java.util.Observable;
import vtk.*;
 
class LBData extends Observable {

  private int [] dims;
  private int size;
  private double dx;
  private vtkIntArray type;
  private vtkDoubleArray vels;
  private vtkDoubleArray scalar_vels;
  private vtkStructuredGrid grid;
  LBVTKPanel panel;
  
  public LBData(LBVTKPanel panel) {
    this.panel = panel;
  }
  
  public void setGrid(int lx, int ly, int lz, double d_x) {
    grid = new vtkStructuredGrid();
    grid.SetDimensions(lx, ly, lz);
    dims = new int[3];
    dims[0] = lx;
    dims[1] = ly;
    dims[2] = lz;
    size = lx * ly * lz;
    dx = d_x;
    vtkPoints points = new vtkPoints();
    points.SetNumberOfPoints(size);
    for (int i = 0; i < size; ++i) {
      points.SetPoint(i, i % dims[0] * dx - ((dims[0] - 1) * dx / 2),
      ((i / dims[0]) % dims[1]) * dx - ((dims[1] - 1) * dx / 2),
      (i / (dims[0] * dims[1])) * dx - ((dims[2] - 1) * dx / 2));
    }
    grid.SetPoints(points);
    this.panel.gui.observe(this);
    this.panel.observe(this);
    setChanged();
    notifyObservers(new Character('o'));//notify that outline is now available
  }
  
  //should I create a new vtkIntArray each time here? or is it held in memory. if so, would I be creating a new
  //one each time and wasting memory, or would it be deleted when points were reset?
  public void setTypeArray(int [] types) {
    if (type == null) {
      type = new vtkIntArray();
      type.SetNumberOfValues(size);
      type.SetName("type");
    }
    for (int i = 0; i < size; ++i) {
      type.SetValue(i, (types[i] == 0) ? 0 : 1);
    }
    grid.GetPointData().AddArray(type);
    setChanged();
    notifyObservers(new Character('t'));//notify that type information is now available
  }
  
  public void setVelArray(double [][] vel) {
    if (vels == null) {
      vels = new vtkDoubleArray();
      vels.SetNumberOfComponents(3);
      vels.SetNumberOfTuples(size);
      vels.SetName("vels");
    }
    if (scalar_vels == null) {
      scalar_vels = new vtkDoubleArray();
      scalar_vels.SetNumberOfValues(size);
      scalar_vels.SetName("scalar_vels");
    }
    for (int i = 0; i < size; ++i) {
      vels.SetTuple3(i, vel[i][0], vel[i][1], vel[i][2]);
      scalar_vels.SetValue(i, Math.sqrt(Math.pow(vel[i][0], 2) + Math.pow(vel[i][2], 2) + Math.pow(vel[i][2], 2)));
    }
    grid.GetPointData().SetVectors(vels);
    grid.GetPointData().SetScalars(scalar_vels);
    setChanged();
    notifyObservers(new Character('V'));
  }
  
  public vtkStructuredGrid getGrid() {
    return grid;
  }
  
  public int getSize() {
    return size;
  }
  
  public double getDx() {
    return dx;
  }
  
  public boolean gridHasChanged(int [] d, double d_x) {
    if (grid == null) return true;
    return (!(d[0] == dims[0] && d[1] == dims[1] && d[2] == dims[2] && d_x == dx));
  }

}