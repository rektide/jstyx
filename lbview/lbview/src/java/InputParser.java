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

import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;
import LBStructures.*;
import java.util.Observable;
import java.util.Observer;
import java.util.NoSuchElementException;

class InputParser extends Observable implements Terminable, Runnable {

  private Tokenizer t;
  private LBDomain dom;
  private LBMessage message;

  public InputParser(File file, Observer o) {
    this.addObserver(o);
    try{
      t = new Tokenizer(new Scanner(file), "(\\{)|(\\})|(\\<)|(\\>)|(\\()|(\\))");
    } catch(Exception e) {
      System.out.println("InputParser()");
      e.printStackTrace();
    }
  }

  public InputParser(InputStream is, Observer o) {
    this.addObserver(o);
    t = new Tokenizer(new Scanner(is), "(\\{)|(\\})|(\\<)|(\\>)|(\\()|(\\))");
  }
  
  //must implement run
  public void run() {
    String token = "";
    try{
      while (t.hasMore()) {
        if (t.hasMessage()) {
          token = t.getMessage();
          this.parseMessage(token);
        }
        else {
          token = t.nextToken();
          if (token.matches("domain")) {
            this.parseDomain();
          }
          else t.nextTokenMustBe("\\}");
          //other matches here (message etc)
        }
      }
    }
    catch(NoSuchElementException e) {
      System.out.println("InputParser.run()#1");
      return;
    }
    catch(Exception e) {
      System.out.println("InputParser.run()#2");
      e.printStackTrace();
    }
    Thread.yield();
  }
  
  public LBDomain getDomain() {return dom;}
  
  public LBMessage getMessage() {return message;}
  
  private void parseDomain() {
    try {
      //read in the dimensions data
      t.nextTokenMustBe("\\{");
      t.nextTokenMustBe("dimensions");
      t.nextTokenMustBe("\\{");
      TripleInt dims = new TripleInt(t.nextUIntToken(), t.nextUIntToken(), t.nextUIntToken());
      double dx = t.nextDoubleToken();
      TripleDouble origin = new TripleDouble(t.nextDoubleToken(), t.nextDoubleToken(), t.nextDoubleToken());
      t.nextTokenMustBe("\\}");
      //read in the mapping data
      t.nextTokenMustBe("mapping");
      t.nextTokenMustBe("\\{");
      TripleDouble mapping = new TripleDouble(t.nextDoubleToken(), t.nextDoubleToken(), t.nextDoubleToken());
      t.nextTokenMustBe("\\}");
      //create new domain if needed
      boolean domain_is_new = false;
      if (dom == null || !dom.isCongruent(dims, dx, origin, mapping)) {
        domain_is_new = true;
        dom = new LBDomain(dims, dx, origin, mapping);
      }
      //read various node data in
      //first, set up variables that will be read in
      Character var_name;
      String description;
      TripleInt map;
      int tuple;
      int type;
      //keep reading variables until we hit the end of the domain
      while (!(var_name = t.nextTypeChar()).toString().matches("\\}")) {
        //read in description
        description = t.nextQuotedString();
        //read in mapping
        t.nextTokenMustBe("\\(");
        map = new TripleInt(t.nextIntToken(), t.nextIntToken(), t.nextIntToken());
        t.nextTokenMustBe("\\)");
        //read in tuple
        t.nextTokenMustBe("\\<");
        tuple = t.nextIntToken();
        t.nextTokenMustBe("\\>");
        //read in data type
        String type_string = t.nextToken();
        if (type_string.matches("BOOLEAN")) type = LBVariable.BOOLEAN;
        else if (type_string.matches("INTEGER")) type = LBVariable.INTEGER;
        else if (type_string.matches("DOUBLE")) type = LBVariable.DOUBLE;
        else throw new IncorrectInputException("Expecting: data type, found: " + type_string);
        //create variable in domain and put reference to it in this_var
        //final arg will be time of last update which will come from the messages of pipe mode input
        LBVariable this_var = dom.addVariableToMap(var_name, map, tuple, type, description, 0);
        //ready to read in data
        double scale = dom.getMapping().simToPhys(1, map);
        t.nextTokenMustBe("\\{");
        Tuple this_tuple;
        for (int i = 0; i < dom.getDimensions().getSize(); ++i) {
          this_tuple = this_var.getTuple(i);
          for (int j = 0; j < tuple; ++j) {
            if (!this_tuple.setVal(j, t.nextToken(), scale)) break;
          }
        }
        //done
        t.nextTokenMustBe("\\}");
      }
      super.setChanged();
      if (domain_is_new) super.notifyObservers("NEW_DOMAIN");
      else super.notifyObservers("DOMAIN");
    }
    catch(NoSuchElementException e) {
      System.out.println("InputParser.parseDomain()#1");
      return;
    }
    catch(Exception e) {
      System.out.println("InputParser.parseDomain()#2");
      e.printStackTrace();
    }
  }
  
  private void parseMessage(String token) {
    if (message == null) {
      message = new LBMessage(token);
      super.setChanged();
      super.notifyObservers("NEW_MESSAGE");
    }
    else message.addMessage(token);
  }
  
  public class IncorrectInputException extends Exception {
    public IncorrectInputException() {}
    public IncorrectInputException(String s) {
      //can I add the position in the file from scanner????
      super("Incorrect input. " + s);
    }
  }
  
  //must implement terminate() and isTerminated()
  public void terminate() {
    //System.out.println("input parser dying");
    if (this.dom != null) {
      //sort out saving domain and then destroying domain if necessary
    }
  }
  public boolean isTerminated() {
    return true;
  }

}