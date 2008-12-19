/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.sessions;

/**
 * Clustered HTTP Session monitor interface.  Associated methods are fired on
 * their relevant events.  This interface extends an MBean interface which provides
 * accessors for the relevant data items.  This kills multiple birds with one stone.
 * <ol>
 *   <li>Application logic related calls do not rely on methods declared in bean interfaces.</li>
 *   <li>Methods that shouldn't be seen by the public are not published as bean operations.</li>
 *   <li>We only require one bean implementation and not two.</li>
 * </ol> 
 */
public interface SessionMonitor extends SessionStatisticsMBean {

  /**
   * Interface to use when killing sessions
   */
  public static interface SessionsComptroller {
    /**
     * Kill the specified session
     * @param sessionId Session to kill
     * @return True if killed
     */
    boolean killSession(String sessionId);
  }

  /**
   * Reset sampling
   */
  void reset();

  /**
   * Register a sessions controller
   * @param comptroller Sessions controller
   */
  void registerSessionsController(SessionsComptroller comptroller);

  /**
   * Event indicating that a session was created.
   */
  void sessionCreated();

  /**
   * Event indicating that a session was destroyed.
   */
  void sessionDestroyed();

  /**
   * Event indicating that a request was processed.
   */
  void requestProcessed();

}
