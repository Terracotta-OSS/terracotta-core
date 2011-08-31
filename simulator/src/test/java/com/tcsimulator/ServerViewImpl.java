/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tcsimulator.distrunner.ServerSpec;

import java.util.List;

public class ServerViewImpl implements ServerView {

  private final ServerSpec spec;
  private int              isServerRunning;

  public ServerViewImpl(ServerSpec sSpec) {
    spec = sSpec;
  }

  public synchronized int isServerRunning() {
    return this.isServerRunning;
  }

  public synchronized void setServerRunning(int val) {
    this.isServerRunning = val;
  }

  public String getHostName() {
    return this.spec.getHostName();
  }

  public String getTestHome() {
    return this.spec.getTestHome();
  }

  public List getJvmOpts() {
    return this.spec.getJvmOpts();
  }

  public int getJmxPort() {
    return this.spec.getJmxPort();
  }

  public int getDsoPort() {
    return this.spec.getDsoPort();
  }

  public int getCacheCount() {
    return this.spec.getCache();
  }

  public int getType() {
    return this.spec.getType();
  }

  public ServerView copy() {
    return new ServerViewImpl(spec.copy());
  }

}
