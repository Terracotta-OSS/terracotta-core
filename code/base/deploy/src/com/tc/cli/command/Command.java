/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.cli.command;

public interface Command {
  
  public String name();
  
  public String description();
  
  public String optionName();
  
  public void printUsage();

  public void execute(String [] args);
  
}
