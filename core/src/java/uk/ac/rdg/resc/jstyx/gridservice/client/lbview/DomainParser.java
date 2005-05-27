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

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

class DomainParser implements Runnable {

  private Scanner input;
  private LBData lb_data;
  private String token = "";

  public DomainParser(Scanner in, LBData lb) {
    input = in;
    lb_data = lb;
  }
  
  public void run() {
    try {
      try {
        nextTokenMustBe("dimensions\\{");
        int [] dims = new int[3];
        dims[0] = input.nextInt();
        dims[1] = input.nextInt();
        dims[2] = input.nextInt();
        double dx = input.nextDouble();
        //if the grid has changed, reset it (might need to delete all existing data - but should never happen)
        if (lb_data == null) System.out.println("lb_data is null");
        if (lb_data.gridHasChanged(dims, dx)) lb_data.setGrid(dims[0], dims[1], dims[2], dx);
        nextTokenMustBe("\\}");
        while (nextTokenIs(".\\{")) {
          token = token.replaceFirst("\\{", "");
          switch(token.charAt(0)) {
            case 't': parseType();
                      Thread.yield();
                      break;
            case 'V': parseVels();
                      Thread.yield();
                      break;
            default:  System.err.println("incorrect input");
                      Thread.yield();
                      break;
          }
        }
        tokenMustBe("\\}");
      } catch (InputMismatchException e) {
        throw new IncorrectInputException("Wrong type.");
      }
    } catch (IncorrectInputException e) {
      System.out.println(e);
    }
  }
  
  private void nextTokenMustBe(String s) throws IncorrectInputException {
    if (!input.next().matches(s)) throw new IncorrectInputException("Expecting: " + s.replaceFirst("\\\\", "")); 
  }
  
  private void tokenMustBe(String s) throws IncorrectInputException {
    if (!token.matches(s)) throw new IncorrectInputException("Expecting: " + s.replaceFirst("\\\\", "")); 
  }
  
  private boolean nextTokenIs(String s) {
    token = input.next();
    return (token.matches(s));
  }
  
  //bit slower to read in values then pass them to lb_data and copy them but it means that if there
  //is some kind of input exception, it doesn't leave the data corrupted
  private void parseType() {
    try {//put this in another try block to catch InputMismatchExceptions?
      int [] type = new int[lb_data.getSize()];//should put exception here to deal with NullPointerException
      for (int i = 0; i < lb_data.getSize(); ++i) {
        type[i] = input.nextInt();
      }
      lb_data.setTypeArray(type);
      nextTokenMustBe("\\}");
    } catch (IncorrectInputException e) {
      System.out.println(e);
    }
  }
  
  private void parseVels() {
    try {
      double [][] vels = new double[lb_data.getSize()][3];
      for (int i = 0; i < lb_data.getSize(); ++i) {
        vels[i][0] = input.nextDouble();
        vels[i][1] = input.nextDouble();
        vels[i][2] = input.nextDouble();
      }
      lb_data.setVelArray(vels);
      nextTokenMustBe("\\}");
    } catch (IncorrectInputException e) {
      System.out.println(e);
    }
  }

}