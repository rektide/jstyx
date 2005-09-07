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

import vtk.*;
import javax.swing.*;
import java.awt.*;
import vtk.util.VtkPanelUtil;
import LBStructures.*;

//TODO: make the outline the right size (add half a dx to each dimension) if this is possible

public class LBVTKPanel extends vtkPanel {

  private vtkStructuredGrid grid;
  private vtkDoubleArray vels;
  private vtkActor actor_vels;
  
  private vtkGlyph3D glyph;
  private vtkPolyDataMapper glyph_mapper;
  
  private vtkActor actor_outline;
  private vtkActor dummy = new vtkActor();
  
  private LBDomain domain;
  
  private double scale_factor = 1;
  
  private boolean scale_vels_to_fit = true;
  
  public LBVTKPanel(Dimension scrsize, LBDomain dom) {
    this.domain = dom;
    VtkPanelUtil.setSize(this, scrsize.width / 3, scrsize.height / 2);
    this.setGrid(this.domain.getLattice());
  }
  
  //save the image as a jpeg file
  public void saveImageJPG(String image_file, int magnification, int quality) {
    super.Lock();
    vtkWindowToImageFilter image_filter = new vtkWindowToImageFilter();
    image_filter.SetInput(super.GetRenderWindow());
    image_filter.SetMagnification(magnification);
    vtkJPEGWriter image_writer = new vtkJPEGWriter();
    image_writer.SetFileName(image_file);
    image_writer.SetInput(image_filter.GetOutput());
    image_writer.SetQuality(quality);
    image_writer.ProgressiveOff();
    image_writer.Write();
    super.UnLock();
  }
  
  //save the image as a postscript file - this is a bit naff
  public void saveImagePostScript(String image_file) {
    super.Lock();
    vtkWindowToImageFilter image_filter = new vtkWindowToImageFilter();
    image_filter.SetInput(super.GetRenderWindow());
    vtkPostScriptWriter image_writer = new vtkPostScriptWriter();
    image_writer.SetFileName(image_file);
    image_writer.SetInput(image_filter.GetOutput());
    image_writer.Write();
    super.UnLock();
  }
  
  //save the image as a vector stl output. this needs work cos it needs a polydata input
  public void saveImageSTL(String image_file) {
    super.Lock();
    vtkWindowToImageFilter image_filter = new vtkWindowToImageFilter();
    image_filter.SetInput(super.GetRenderWindow());
    vtkSTLWriter image_writer = new vtkSTLWriter();
    image_writer.SetFileName(image_file);
    image_writer.SetInput(image_filter.GetOutput());
    image_writer.Write();
    super.UnLock();
  }
  
  public void domainUpdate(LBDomain dom) {
    this.domain = dom;
    try {
      this.setVels();
    } catch (Exception e) {
      System.out.println("LBVTKPanel.domainUpdate()");
      e.printStackTrace();
    }
    
    this.grid.Modified();
    super.repaint();
  }
  
  private void setGrid(Lattice l) {
    this.grid = new vtkStructuredGrid();
    this.grid.SetDimensions(l.getDimensions().X(), l.getDimensions().Y(), l.getDimensions().Z());
    vtkPoints points = new vtkPoints();
    points.SetNumberOfPoints(l.getSize());
    for (int i = 0; i < l.getSize(); ++i) {
      points.SetPoint(i, l.indexToPhysCoords(i).toArray());
    }
    this.grid.SetPoints(points);
    this.createOutlineActor();
  }
  
  public void setVels() {
    LBVariable v = new LBVariable();
    try {
      v = this.domain.getVariable('V');
    } catch (Exception e) {
      System.out.println("LBVTKPanel.setVels()");
      e.printStackTrace();
    }
    if (this.vels == null) {
      this.vels = new vtkDoubleArray();
      this.vels.SetNumberOfComponents(3);
      this.vels.SetNumberOfTuples(this.grid.GetPoints().GetNumberOfPoints());
      this.vels.SetName("vels");
    }
    for (int i = 0; i < this.grid.GetPoints().GetNumberOfPoints(); ++i) {
      this.vels.SetTuple3(i, v.getTuple(i).getVal(0), v.getTuple(i).getVal(1), v.getTuple(i).getVal(2));
    }
    this.vels.Modified();
    this.grid.GetPointData().SetVectors(vels);
    if (this.actor_vels == null) createVelsActor();
    if (this.scale_vels_to_fit) this.scaleVelsToFit(this.scale_factor);
    this.grid.Modified();
    super.repaint();
  }
  
  private void createOutlineActor() {
    if (this.grid != null) {
      vtkOutlineFilter outline = new vtkOutlineFilter();
      outline.SetInput(this.grid);
      vtkPolyDataMapper mapOutline = new vtkPolyDataMapper();
      mapOutline.SetInput(outline.GetOutput());
      actor_outline = new vtkActor();
      actor_outline.SetMapper(mapOutline);
      actor_outline.GetProperty().SetColor(1,1,1);
      super.GetRenderer().AddActor(actor_outline);
      super.repaint();
    }
  }
  
  private void createVelsActor() {
    vtkConeSource cone = new vtkConeSource();
    cone.SetRadius(0.3);
    cone.SetCenter(cone.GetHeight() / 2., 0., 0.);
    cone.SetResolution(8);

    this.glyph = new vtkGlyph3D();
    this.glyph.SetInput(grid);
    this.glyph.SetSource(cone.GetOutput());
    this.glyph.OrientOn();
    this.glyph.SetScaleModeToScaleByVector();
    this.glyph.SetColorModeToColorByVector();

    vtkLookupTable lut = new vtkLookupTable();
    lut.SetHueRange(0.667, 0.0);
    lut.SetNumberOfColors(256);
    lut.Build();
    
    this.glyph_mapper = new vtkPolyDataMapper();
    this.glyph_mapper.SetInput(this.glyph.GetOutput());
    this.glyph_mapper.SetScalarRange(this.grid.GetPointData().GetVectors().GetRange(-1));
    this.glyph_mapper.SetLookupTable(lut);
    this.glyph_mapper.Update();
    
    actor_vels = new vtkActor();
    actor_vels.SetMapper(this.glyph_mapper);
  }
  
  public void toggleOutline() {
    if (super.GetRenderer().HasProp(this.actor_outline) != 0) {
      this.removeActor(this.actor_outline);
    }
    else super.GetRenderer().AddActor(actor_outline);
    super.repaint();
  }
  
  public void toggleVels() {
    if (super.GetRenderer().HasProp(this.actor_vels) != 0) {
      this.removeActor(this.actor_vels);
    }
    else {
      if (this.actor_vels == null) setVels(); 
      super.GetRenderer().AddActor(actor_vels);
    }
    super.repaint();
  }
  
  public void setVectorLength(double l) {
    if (this.glyph != null) {
      if (this.scale_vels_to_fit) this.scaleVelsToFit(l);
      else {
        this.glyph.SetScaleFactor(l);
        super.repaint();
      }
    }
  }
  
  public double toggleScaleToFit(double factor) {
    this.scale_vels_to_fit = this.scale_vels_to_fit ? false : true;
    double [] range = new double[2];
    this.grid.GetPointData().GetVectors().GetRange(range, -1);
    if (this.scale_vels_to_fit) {
      this.scale_factor = factor * range[1] / this.domain.getLattice().getDX() ;
    }
    else {
      this.scale_factor = factor * this.domain.getLattice().getDX() / range[1];
    }
    this.setVectorLength(this.scale_factor);
    super.repaint();
    return this.scale_factor;
  }
  
  private void scaleVelsToFit(double factor) {
    this.scale_factor = factor;
    if (this.glyph != null && this.grid != null) {
      double [] range = new double[2];
      this.grid.GetPointData().GetVectors().GetRange(range, -1);
      this.glyph.SetScaleFactor(this.domain.getLattice().getDX() * this.scale_factor / range[1]);
      this.glyph.Modified();
      //update colour map too
      this.glyph_mapper.SetScalarRange(this.grid.GetPointData().GetVectors().GetRange(-1));
      this.glyph_mapper.Modified();
    }
    super.repaint();
  }
  
  public double getVectorLength() {
    if (this.glyph != null) {
      if (this.scale_vels_to_fit) return this.scale_factor;
      else return this.glyph.GetScaleFactor();
    }
    return 0;
  }
  
  private void removeActor(vtkActor actor) {
    //should check if there are no more actors and add in a dummy if so (otherwise won't rerender)
    super.GetRenderer().RemoveActor(actor);
    if (super.GetRenderer().VisibleActorCount() == 0) {
      super.GetRenderer().AddActor(dummy);
    }
    super.repaint();
  }
  
  public double [] getCameraPosition() {
    double [] p = new double[10];
    double [] up = super.GetRenderer().GetActiveCamera().GetViewUp();
    double [] pos = super.GetRenderer().GetActiveCamera().GetPosition();
    double [] focus = super.GetRenderer().GetActiveCamera().GetFocalPoint();
    p[0] = super.GetRenderer().GetActiveCamera().GetDistance();
    p[1] = up[0]; p[2] = up[1]; p[3] = up[2];
    p[4] = (pos[0] - focus[0]) / p[0];
    p[5] = (pos[1] - focus[1]) / p[0];
    p[6] = (pos[2] - focus[2]) / p[0];
    p[7] = focus[0]; p[8] = focus[1]; p[9] = focus[2];
    return p;
  }
  
  public void setCameraPosition(double [] p) {
    double dist = Math.sqrt(Math.pow(p[4], 2) +  Math.pow(p[5], 2) + Math.pow(p[6], 2));
    p[4] = p[4] * p[0] / dist + p[7];
    p[5] = p[5] * p[0] / dist + p[8];
    p[6] = p[6] * p[0] / dist + p[9];
    super.GetRenderer().GetActiveCamera().SetViewUp(p[1], p[2], p[3]);
    super.GetRenderer().GetActiveCamera().SetPosition(p[4], p[5], p[6]);
    super.GetRenderer().GetActiveCamera().SetFocalPoint(p[7], p[8], p[9]);
    super.UpdateLight();
    super.resetCamera();
    super.Render();
  }
  

}