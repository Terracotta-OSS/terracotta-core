/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;


public interface ServerControl {

  public void mergeSTDOUT();

  public void mergeSTDERR();

  /**
   * Starts the shutdown sequence, but doesn't block.
   */
  public void attemptForceShutdown() throws Exception;

  /**
   * Starts the shutdown sequence, blocking until isRunning() is false.
   */
  public void shutdown() throws Exception;

  /**
   * Forces the server to exit, blocking until isRunning() is false.
   */
  public void crash() throws Exception;

  /**
   * Starts the server, blocking until isRunning() is true.
   */
  public void start() throws Exception;

  /**
   * Starts the server without waiting for it to start
   */
  public void startWithoutWait() throws Exception;

  /**
   * wait for the process termination
   */
  public int waitFor() throws Exception;

  /**
   * Returns true if the server responds.
   */
  public boolean isRunning();

  public void waitUntilShutdown() throws Exception;

  public int getDsoPort();

  public int getAdminPort();

}