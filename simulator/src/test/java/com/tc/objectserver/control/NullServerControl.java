/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.stats.api.DSOMBean;

public class NullServerControl implements ServerControl {

  private boolean isRunning;

  public synchronized void attemptForceShutdown() {
    isRunning = false;
  }

  public synchronized void shutdown() {
    isRunning = false;
  }

  public synchronized void crash() {
    isRunning = false;
  }

  public synchronized void start() {
    this.isRunning = true;
  }

  public void startWithoutWait() {
    this.isRunning = true;
  }

  public synchronized boolean isRunning() {
    return isRunning;
  }

  public void clean() {
    return;
  }

  public void mergeSTDOUT() {
    return;
  }

  public void mergeSTDERR() {
    return;
  }

  public void waitUntilShutdown() {
    return;
  }

  public int getDsoPort() {
    return 0;
  }

  public int getAdminPort() {
    return 0;
  }

  public int waitFor() {
    return 1;
  }

  public DSOMBean getDSOMBean() {
    return null;
  }

  public L2DumperMBean getL2DumperMBean() {
    return null;
  }

  public TCServerInfoMBean getTCServerInfoMBean() {
    return null;
  }

  public void waitUntilL2IsActiveOrPassive() {
    //
  }
}
