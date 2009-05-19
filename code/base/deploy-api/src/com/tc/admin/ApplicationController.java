/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

public interface ApplicationController {
  void setStatus(String msg);

  void clearStatus();

  void log(String msg);

  void log(Throwable t);

  void showOptions();
  
  void showOption(String name);
  
  void block();

  void unblock();
}
