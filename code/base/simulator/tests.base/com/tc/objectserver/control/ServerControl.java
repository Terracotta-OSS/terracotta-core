/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

public interface ServerControl {

  public void mergeSTDOUT();

  public void mergeSTDERR();

  /**
   * Starts the shutdown sequence, but doesn't block.
   */
  public void attemptShutdown() throws Exception;

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
   * Returns true if the server responds.
   */
  public boolean isRunning();

  public void clean();

  public void waitUntilShutdown() throws Exception;

  public int getDsoPort();

  public int getAdminPort();

}