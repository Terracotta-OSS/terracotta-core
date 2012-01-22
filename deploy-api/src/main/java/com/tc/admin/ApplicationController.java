/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
