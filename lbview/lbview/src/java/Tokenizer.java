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

public class Tokenizer {

  Link token;
  String message = "";
  Pattern delims;
  private Scanner input;
  int line_num;
  
  public Tokenizer(Scanner sc, String delimiters) {
    input = sc;
    delims = Pattern.compile(delimiters);
    line_num = 0;
    this.refill();
  }
  
  //public member functions
  
  public boolean hasNext() {return (token != null || input.hasNext());}
  
  public boolean hasMessage() {return !(message.matches(""));}
  
  public boolean hasMore() {
    return (this.hasNext() || this.hasMessage());
  }
  
  public String getMessage() {
    String msg = this.message;
    this.message = "";
    this.refill();
    return msg;
  }
  
  public String nextToken() {
    while (token == null || token.toString().matches("")) this.readLine();
    //if (token == null || token.toString().matches("")) this.readLine();
    String t = token.toString();
    token = token.next();
    this.refill();
    return t;
  }
  
  private void refill() {
    while (this.hasNext() && (token == null || token.toString().matches("")) && !this.hasMessage()) this.readLine();
  }
  
  public int nextUIntToken() throws IncorrectInputException {
    Pattern uint_regex = Pattern.compile("\\d+");
    String t = this.nextToken();
    if (!uint_regex.matcher(t).matches()) throw new IncorrectInputException("Expecting uint, found: " + t);
    return Integer.parseInt(t);
  }
  
  public int nextIntToken() throws IncorrectInputException {
    Pattern int_regex = Pattern.compile("[+|-]?\\d+");
    String t = this.nextToken();
    if (!int_regex.matcher(t).matches()) throw new IncorrectInputException("Expecting int, found: " + t);
    return Integer.parseInt(t);
  }
  
  public double nextDoubleToken() throws IncorrectInputException {
    Pattern double_regex = Pattern.compile("[+|-]?((\\d+(\\.\\d*)?)|(\\.\\d+))([e|E][+|-]?\\d+)?");
    String t = this.nextToken();
    if (!double_regex.matcher(t).matches()) throw new IncorrectInputException("Expecting double, found: " + t);
    return Double.parseDouble(t);
  }
  
  public char nextTypeChar() throws IncorrectInputException {
    Pattern typechar_regex = Pattern.compile("[a-zA-Z0-9]|\\}"); //any type char (or })
    String t = this.nextToken();
    if (!typechar_regex.matcher(t).matches()) throw new IncorrectInputException("Expecting type char character, found: " + t);
    return t.charAt(0);
  }
  
  public String nextQuotedString() throws IncorrectInputException {
    Pattern quoted_regex = Pattern.compile("\".+\"");
    String t = this.nextToken();
    if (!quoted_regex.matcher(t).matches()) throw new IncorrectInputException("Expecting quoted string, found: " +t);
    return t;
  }
  
  public String nextTokenMustBe(String s) throws IncorrectInputException {
    String t = this.nextToken();
    if (!t.matches(s)) throw new IncorrectInputException("Expecting: " + s.replaceFirst("\\\\", "") + " found: " + t);
    return t;
  }
  
  //private member functions, used internally
  
  private void readLine() {
    line_num++;
    message = "";
    token = new Link(input.nextLine());
    this.stripComments();
    this.breakAtWhitespace();
    this.breakAtDelims();
    if (token.toString().length() == 0) {
      if (token.hasNext()) token = token.next();
      else token = null;
      //else this.readLine();
    }
  }
  
  private void stripComments() {
    Matcher comment_match = Pattern.compile("//").matcher(token.toString());
    if (comment_match.find()) {
      message = token.toString().substring(comment_match.end());
      token.truncateEnd(comment_match.start());
    }
  }
  
  private void breakAtWhitespace() {
    Matcher space_match = Pattern.compile("(\\s+)|(\")").matcher("");
    Matcher quote_match = Pattern.compile("\"[^\"]+\"").matcher("");
    Link this_token = token;
    while (this_token != null) {
      space_match.reset(this_token.toString());
      if (space_match.find()) {
        if (quote_match.reset(this_token.toString()).find() && quote_match.start() == 0) {
          this_token = this_token.chop(quote_match.end());
          continue;
        }
        if (space_match.start() > 0) this_token = this_token.chop(space_match.start());
        else this_token = this_token.truncateStart(space_match.end()); //in case whitespace is first in token
      }
      else this_token = this_token.next();
    }
  }
  
  private void breakAtDelims() {
    Matcher delim_match = delims.matcher("");
    Link this_token = token;
    while (this_token != null) {
      delim_match.reset(this_token.toString());
      if (delim_match.find()) {
        if (delim_match.start() > 0) this_token = this_token.chop(delim_match.start()).chop(delim_match.end() - delim_match.start());
        else this_token = this_token.chop(delim_match.end()); //in case delim is first in token
      }
      else this_token = this_token.next();
    }
  }
  
  public String toString() {
    String s = token.toString();
    while (token.hasNext()) {
      token = token.next();
      s += "\n" + token.toString();
    }
    s += "\nMessage: " + message;
    return s;
  }
  
  //class to hold chain of strings after (and during) chopping around delimiters
  public class Link {
  
    private Link next;
    private String str;
    
    public Link(String s) {str = s;}
    
    public Link(String s, Link l) {
      str = s;
      next = l;
    }
    
    public boolean hasNext() {return (next != null);}
    
    public void setString(String s) {str = s;}
    
    public Link chop(int i) {
      if (str.length() > i) {//do only if delim is not last in token
        next = new Link(str.substring(i), next);
        str = str.substring(0, i);
      }
      return next;
    }
    
    public Link truncateEnd(int i) {
      str = str.substring(0, i);
      return this;
    }
    
    public Link truncateStart(int i) {
      str = str.substring(i);
      return this;
    }
    
    public Link next() {return next;}
    
    public void removeNext() {next = null;}
    
    public String toString() {return str;}
  
  }
  
  //Exceptions
  public class IncorrectInputException extends Exception {
    public IncorrectInputException() {}
    public IncorrectInputException(String s) {
      //can I add the position in the file from scanner????
      super("Incorrect input at line: " + line_num + ". " + s);
    }
  }

}