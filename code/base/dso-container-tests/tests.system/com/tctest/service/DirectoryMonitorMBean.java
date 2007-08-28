/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.service;

public interface DirectoryMonitorMBean {
  public void setScanRate(long rate);

  public long getScanRate();

  public void setDirectory(String dir);

  public String getDirectory();

  public void setExtensionList(String list);

  public String getExtensionList();

  // life cycles
  public void start();

  public void stop();

  public boolean isStarted();
}