/***************************************************************************
 *   From vtk java examples (find proper citation)                         *
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

package vtk.util;

import vtk.vtkPanel;

import java.util.Observable;
import java.util.Observer;

/**
 * Utilities to simplify use of vtkPanel.
 * 
 * @author Jarek Sacha
 * @version $Revision$
 */
public class VtkPanelUtil {

  // Prevent instantiation of this class
  private VtkPanelUtil() {
  }


  /**
   * Set size of vtkPanel so Java JPanel and VTK render window coordinates are
   * synchronised. When a call to vtkPanel.setSize() is made before panel is
   * rendered, panel size is not forwarded to VTK and there must be another
   * call to setSize once the panel finished rendering for the first time.
   * 
   * @param renWin vtkPanel for which to safely set size
   * @param xSize  size in X.
   * @param ySize  size in Y.
   */
  public static void setSize(final vtkPanel renWin,
                             final int xSize,
                             final int ySize) {
    if (renWin == null) {
      throw new IllegalArgumentException("Argument vtkPanel cannot be null");
    }

    // Add observer to update size when first rendering is completed
    // Set it before call to 'setSize' 'isWindowSet' so we do not
    // miss notification if rendering is completed in an other thread.
    final Observer windowSetObserver = new Observer() {
      public void update(Observable o, Object arg) {
        renWin.setSize(xSize, ySize);
        renWin.removeWindowSetObserver(this);
      }
    };
    renWin.addWindowSetObserver(windowSetObserver);

    // Set size here, if needed the 'windowSetObserver' will make sure
    // that another call to 'setSize' is mage when vtkPanel is rendered
    // for the first time.
    renWin.setSize(xSize, ySize);

    // No need to use 'windowSetObserver' is window is already set.
    if (renWin.isWindowSet() == true) {
      renWin.removeWindowSetObserver(windowSetObserver);
    }
  }

}
