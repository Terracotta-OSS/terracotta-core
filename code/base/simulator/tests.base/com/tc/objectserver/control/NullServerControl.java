/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.control;


public class NullServerControl implements ServerControl {

  private boolean isRunning;
  
  public synchronized void attemptShutdown() throws Exception {
    isRunning = false;
  }

  public synchronized void shutdown() throws Exception {
     isRunning = false;
  }

  public synchronized void crash() throws Exception {
    isRunning = false;
  }

  public synchronized void start(long timeout) throws Exception {
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

}
