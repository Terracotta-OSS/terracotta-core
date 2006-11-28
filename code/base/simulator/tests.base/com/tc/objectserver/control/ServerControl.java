/*
 * Created on Apr 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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
  public void start(long timeout) throws Exception;

  /**
   * Returns true if the server responds.
   */
  public boolean isRunning();

  public void clean();

  public void waitUntilShutdown() throws Exception;

  public int getDsoPort();

}