/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;


public class NullServerControl implements ServerControl {

  private boolean isRunning;

  @Override
  public synchronized void attemptForceShutdown() throws Exception {
    isRunning = false;
  }

  @Override
  public synchronized void shutdown() throws Exception {
    isRunning = false;
  }

  @Override
  public synchronized void crash() throws Exception {
    isRunning = false;
  }

  @Override
  public synchronized void start() throws Exception {
    this.isRunning = true;
  }

  @Override
  public void startWithoutWait() throws Exception {
    this.isRunning = true;
  }

  @Override
  public synchronized boolean isRunning() {
    return isRunning;
  }

  @Override
  public void mergeSTDOUT() {
    return;
  }

  @Override
  public void mergeSTDERR() {
    return;
  }

  @Override
  public void waitUntilShutdown() {
    return;
  }

  @Override
  public int getTsaPort() {
    return 0;
  }

  @Override
  public int getAdminPort() {
    return 0;
  }

  @Override
  public int waitFor() throws Exception {
    return 1;
  }

  @Override
  public void waitUntilL2IsActiveOrPassive() throws Exception {
    //
  }

  @Override
  public void pauseServer(long pauseTimeMillis) throws InterruptedException {
    //
  }

  @Override
  public void pauseServer() throws InterruptedException {

  }

  @Override
  public void unpauseServer() throws InterruptedException {

  }
}
