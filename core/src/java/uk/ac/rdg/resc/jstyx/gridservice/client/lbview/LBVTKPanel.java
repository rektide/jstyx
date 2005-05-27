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

import java.awt.*;
import javax.swing.*;
import java.util.Observable;
import java.util.Observer;
import vtk.*;

public class LBVTKPanel extends vtkPanel implements Observer {

  LBData lb_data;

  boolean show_outline = true;
  boolean show_type = false;
  boolean show_vels = false;
  boolean show_streamlines = false;
  boolean show_surface = false;
  
  boolean dummy_set = false;

  vtkActor actor_outline;
  vtkActor actor_surface;
  vtkActor actor_streamlines;
  
  public LBVTKPanel(Dimension scrsize) {
    VtkPanelUtil.setSize(this, scrsize.width / 3, scrsize.height / 2);
    lb_data = new LBData();
  }

  public void observe(LBData lbd) {
    lb_data = lbd;
    lb_data.addObserver(this);
  }

  //must implement update():
  public void update(Observable lb_data, Object o) {
    if (show_outline && actor_outline == null) {
      createOutlineActor();
      GetRenderer().AddActor(actor_outline);
    }
    repaint();
  }
  
  public LBData getLBData() {
    return lb_data;
  }
  
  public void destroyVisualization() {
    actor_outline = null;
    actor_surface = null;
    actor_streamlines = null;
    lb_data = null;
    repaint();
  }
  
  public void toggleOutline() {
    show_outline = show_outline ? false : true;
    if (show_outline) {
      if (actor_outline == null) createOutlineActor();
      GetRenderer().AddActor(actor_outline);
    }
    else if (actor_outline != null) {
      removeActor(actor_outline);
    }
    repaint();
  }
  
  private void createOutlineActor() {
    if (lb_data.getGrid() != null) {
      vtkOutlineFilter outline = new vtkOutlineFilter();
      outline.SetInput(lb_data.getGrid());
      vtkPolyDataMapper mapOutline = new vtkPolyDataMapper();
      mapOutline.SetInput(outline.GetOutput());
      actor_outline = new vtkActor();
      actor_outline.SetMapper(mapOutline);
      actor_outline.GetProperty().SetColor(1,1,1);
    }
  }
  
  public void toggleSurface() {
    show_surface = show_surface ? false : true;
    if (show_surface) {
      if (actor_surface == null) createSurfaceActor();
      GetRenderer().AddActor(actor_surface);
    }
    else if (actor_surface != null) {
      removeActor(actor_surface);
    }
    repaint();
  }
  
  private void createSurfaceActor() {
    //check that type is set by checking lb_data.getGrid().GetPointData().SetActiveScalar("type") != -1
    vtkContourFilter contour = new vtkContourFilter();
    //if (lb_data.getGrid().GetPointData().SetActiveScalars("type") == -1) System.out.println("no type data");
    contour.SetInput(lb_data.getGrid());
    contour.SetInputArrayToProcess(0, 0, 0, 0, "type");
    contour.ComputeNormalsOff();
    contour.SetValue(0, 0.5);
    vtkPolyDataMapper mapcontour = new vtkPolyDataMapper();
    mapcontour.SetInput(contour.GetOutput());
    actor_surface = new vtkActor();
    actor_surface.SetMapper(mapcontour);
    actor_surface.GetProperty().SetOpacity(JLB.getGUI().getSliderSurfaceValue() / 100);
  }
  
  public void setSurfaceOpacity(int o) {
    if (actor_surface != null) actor_surface.GetProperty().SetOpacity((double)(o) / 100);
    repaint();
  }
  
  public void toggleStreamlines() {
    show_streamlines = show_streamlines ? false : true;
    if (show_streamlines) {
      if (actor_streamlines == null) createStreamlinesActor();
      GetRenderer().AddActor(actor_streamlines);
    }
    else if (actor_streamlines != null) {
      removeActor(actor_streamlines);
    }
    repaint();
  }
  
  private void createStreamlinesActor() {
    double [] dims = new double[6];
    lb_data.getGrid().GetBounds(dims);
    
    vtkPlaneSource plane = new vtkPlaneSource();
    
    plane.SetOrigin(dims[0], dims[2], dims[4]);
    plane.SetPoint1(dims[0], dims[2], dims[5]);
    plane.SetPoint2(dims[0], dims[3], dims[4]);
    plane.SetResolution(10, 10);
    
    vtkRungeKutta4 integ = new vtkRungeKutta4();
    vtkStreamLine streams = new vtkStreamLine();
    streams.SetInput(lb_data.getGrid());
    streams.SetSource(plane.GetOutput());
    
    streams.SetMaximumPropagationTime(500);
    streams.SetStepLength(5);
    //streams.SetIntegrationStepLength(0.5);
    //streams.SetStartPosition(0, 0.6, 0.75);
    //streams.SetStepLength(ob_dom.getDx() / 4);
    streams.SetIntegrationDirectionToIntegrateBothDirections();
    streams.SetIntegrator(integ);
    
    //streams.SetIntegrationStepLength(0.0005);
    //streams.Update();
    
    vtkRibbonFilter ribbon = new vtkRibbonFilter();
    ribbon.SetInput(streams.GetOutput());
    ribbon.SetWidth(lb_data.getDx() / 3);
    
    
    //if (lb_data.getGrid().GetPointData().SetActiveScalars("scalar_vels") == -1) System.out.println("no scalar_vels data");
    vtkLookupTable lut = new vtkLookupTable();
    lut.SetHueRange(0.667, 0.0);
    lut.SetNumberOfColors(256);
    lut.Build();
    vtkPolyDataMapper streamMapper = new vtkPolyDataMapper();
    streamMapper.SetInput(ribbon.GetOutput());
    //streamMapper.SetScalarRange(-0.01, 0.01);
    streamMapper.SetScalarRange(lb_data.getGrid().GetPointData().GetScalars().GetRange());
    streamMapper.SetLookupTable(lut);

    actor_streamlines = new vtkActor();
    actor_streamlines.SetMapper(streamMapper);
    
    GetRenderer().AddActor(actor_streamlines);
  }
  
  private void removeActor(vtkActor actor) {
    //should check if there are no more actors and add in a dummy if so (otherwise won't rerender)
    GetRenderer().RemoveActor(actor);
    if (GetRenderer().VisibleActorCount() == 0) {
      vtkActor dummy = new vtkActor();
      GetRenderer().AddActor(dummy);
    }
  }

}