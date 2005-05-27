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

import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.*;
import java.io.*;

class InputParser implements Runnable {

  private Scanner input;
  private LBData lb_data;
  private LBMessages lb_m;
  
  private Pattern msg;
  private Pattern domain;
  private Pattern simulation;
  private Pattern monitor;
  
  private Matcher matcher_msg;
  private Matcher matcher_domain;
  private Matcher matcher_simulation;
  private Matcher matcher_monitor;
  
  private String token = "";
  
  private Thread dom_parse;
  //private SimulationParser sim_parse;

  public InputParser() {
    input = new Scanner(System.in);
    initParser();
  }
  
  public InputParser(File file) {
    try{
      input = new Scanner(file);
    } catch(FileNotFoundException e) {
    
    }
    initParser();
  }
  
  public InputParser(InputStream is) {
    input = new Scanner(is);
    initParser();
  }
  
  private void initParser() {
    msg = Pattern.compile("//.");
    domain = Pattern.compile("domain\\s*\\{\\s*");
    simulation = Pattern.compile("simulation\\s*\\{\\s*");
    monitor = Pattern.compile("monitor\\s*\\{\\s*");
    matcher_msg = msg.matcher("");
    matcher_domain = domain.matcher("");
    matcher_simulation = simulation.matcher("");
    matcher_monitor = monitor.matcher("");
    if (JLB.getGUI() == null) System.out.println("no gui");
    if (JLB.getGUI().getVTKPanel() == null) System.out.println("no panel");
    if (JLB.getGUI().getVTKPanel().getLBData() == null) System.out.println("no lbdata");
    lb_data = JLB.getGUI().getVTKPanel().getLBData();
  }
  
  public void run() {
    int i = 0;
    while(input.hasNext()) {
      token = input.next();
      //check if a keyword is found
      matcher_msg.reset(token);
      if (matcher_msg.find()) {
        parseMessage();
        Thread.yield();
        continue;
      }
      matcher_domain.reset(token);
      if (matcher_domain.find()) {
        parseDomain();
        Thread.yield();
        continue;
      }
      matcher_simulation.reset(token);
      if (matcher_simulation.find()) {
        parseSimulation();
        Thread.yield();
        continue;
      }
      matcher_monitor.reset(token);
      if (matcher_monitor.find()) {
        parseMonitor();
        Thread.yield();
        continue;
      }
      //give other threads a chance to catch up
      Thread.yield();
    }
    // Need to close the InputStream
    input.close();
  }
  
  private void parseMessage() {
    if (lb_m == null) {
      lb_m = new LBMessages();
    }
    lb_m.addMessage(token.replaceFirst("//", "") + input.nextLine());
  }
  
  private void parseDomain() {
    dom_parse = new Thread(new DomainParser(input, lb_data));
    dom_parse.start();
    //wait until the domain parser thread has terminated before carrying on (may throw exception)
    try {
      dom_parse.join();
    } catch (InterruptedException e) {
      System.out.println(e);
    } 
  }
  
  private void parseSimulation() {
  
  }
  
  private void parseMonitor() {
  
  }

}