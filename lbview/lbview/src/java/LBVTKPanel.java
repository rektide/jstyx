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
  
  private vtkIntArray type;
  private vtkActor actor_surface;
  
  private vtkActor actor_streamlines;
  private vtkStreamLine streams;
  private vtkPolyDataMapper streamMapper;
  private vtkPlaneSource plane;
  
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
    vtkLight light1 = new vtkLight();
    light1.SetPosition(1, 1, 1);
    vtkLight light2 = new vtkLight();
    light2.SetPosition(1, 1, 1);
    super.GetRenderer().GetLights().AddItem(light1);
    super.GetRenderer().GetLights().AddItem(light2);
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
      this.setType();
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
    if (this.actor_vels == null) this.createVelsActor();
    if (this.scale_vels_to_fit) this.scaleVelsToFit(this.scale_factor);
    if (this.actor_streamlines == null) this.createStreamlinesActor();
    this.scaleStreamlines();
    this.grid.Modified();
    super.repaint();
  }
  
  public void setType() {
    LBVariable v = new LBVariable();
    try {
      v = this.domain.getVariable('t');
    } catch (Exception e) {
      System.out.println("LBVTKPanel.setType()");
      e.printStackTrace();
    }
    if (this.type == null)  {
      this.type = new vtkIntArray();
      this.type.SetNumberOfValues(this.grid.GetPoints().GetNumberOfPoints());
      this.type.SetName("type");
    }
    for (int i = 0; i < this.grid.GetPoints().GetNumberOfPoints(); ++i) {
      this.type.SetValue(i, ((int)(v.getTuple(i).getVal(0)) == 0) ? 0 : 1);
    }
    this.type.Modified();
    this.grid.GetPointData().AddArray(this.type);
    if (this.actor_surface == null) this.createSurfaceActor();
    this.grid.Modified();
    this.repaint();
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
    this.glyph.SetInput(this.grid);
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
    
    this.actor_vels = new vtkActor();
    this.actor_vels.SetMapper(this.glyph_mapper);
  }
  
  private void createStreamlinesActor() {
    double [] dims = new double[6];
    this.grid.GetBounds(dims);
    
    this.plane = new vtkPlaneSource();
    this.plane.SetOrigin(dims[0], dims[2], dims[4]);
    this.plane.SetPoint1(dims[0], dims[2], dims[5]);
    this.plane.SetPoint2(dims[0], dims[3], dims[4]);
    this.plane.SetResolution(1, 20);
    
    this.streams = new vtkStreamLine();
    this.streams.SetInput(this.grid);
    this.streams.SetSource(plane.GetOutput());
    this.streams.SetIntegrationStepLength(0.5);
    this.streams.SetIntegrationDirectionToIntegrateBothDirections();
    this.streams.SpeedScalarsOn();
    this.streams.VorticityOn();
    vtkRungeKutta4 integ = new vtkRungeKutta4();
    this.streams.SetIntegrator(integ);
    
    vtkRibbonFilter ribbon = new vtkRibbonFilter();
    ribbon.SetInput(this.streams.GetOutput());
    ribbon.SetWidth(this.domain.getLattice().getDX() / 4);
    
    vtkLookupTable lut = new vtkLookupTable();
    lut.SetHueRange(0.667, 0.0);
    lut.SetNumberOfColors(256);
    lut.Build();
    this.streamMapper = new vtkPolyDataMapper();
    this.streamMapper.SetInput(ribbon.GetOutput());
    this.streamMapper.SetLookupTable(lut);

    this.actor_streamlines = new vtkActor();
    this.actor_streamlines.SetMapper(this.streamMapper);
  }
  
  private void createSurfaceActor() {
    vtkContourFilter contour = new vtkContourFilter();
    //if (lb_data.getGrid().GetPointData().SetActiveScalars("type") == -1) System.out.println("no type data");
    contour.SetInput(this.grid);
    contour.SelectInputScalars("type");
    contour.ComputeNormalsOff();
    contour.SetValue(0, 0.5);
    vtkPolyDataMapper mapcontour = new vtkPolyDataMapper();
    mapcontour.SetInput(contour.GetOutput());
    this.actor_surface = new vtkActor();
    this.actor_surface.SetMapper(mapcontour);
    this.actor_surface.GetProperty().SetOpacity(0.2);
    super.repaint();
  }
  
  public void toggleOutline() {
    if (super.GetRenderer().HasProp(this.actor_outline) != 0) {
      this.removeActor(this.actor_outline);
    }
    else super.GetRenderer().AddActor(this.actor_outline);
    super.repaint();
  }
  
  public void toggleVels() {
    if (super.GetRenderer().HasProp(this.actor_vels) != 0) {
      this.removeActor(this.actor_vels);
    }
    else {
      if (this.actor_vels == null) setVels(); 
      super.GetRenderer().AddActor(this.actor_vels);
    }
    super.repaint();
  }
  
  public void toggleStreamlines() {
    if (super.GetRenderer().HasProp(this.actor_streamlines) != 0) {
      this.removeActor(this.actor_streamlines);
    }
    else {
      if (this.actor_streamlines == null) setVels(); 
      super.GetRenderer().AddActor(this.actor_streamlines);
    }
    super.repaint();
  }
  
  public void toggleSurface() {
    if (super.GetRenderer().HasProp(this.actor_surface) != 0) {
      this.removeActor(this.actor_surface);
    }
    else {
      if (this.actor_surface == null) this.setType(); 
      super.GetRenderer().AddActor(this.actor_surface);
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
      this.glyph_mapper.SetScalarRange(range);
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
  
  private void scaleStreamlines() {
    if (this.actor_streamlines != null) {
      double length = this.grid.GetLength(); //length of diagonal of bounding box
      double [] range = new double[2];
      this.grid.GetPointData().GetVectors().GetRange(range, -1);
      double time = length / range[1];
      this.streams.SetMaximumPropagationTime(time * 10);
      this.streams.SetStepLength(time / 20);
      this.streamMapper.SetScalarRange(range);
    }
  }
  
  public void setStreamPos(int p) {
    if (this.actor_streamlines != null) {
      double [] center = new double[3];
      center = this.plane.GetCenter();
      double [] dims = new double[6];
      this.grid.GetBounds(dims);
      this.plane.SetCenter(dims[0] + (dims[1] - dims[0]) * p / 100, center[1], center[2]);
      this.plane.Modified();
      super.repaint();
    }
  }
  
  public void setSurfaceOpacity(int o) {
    if (this.actor_surface != null) this.actor_surface.GetProperty().SetOpacity((double)(o) / 100);
    super.repaint();
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