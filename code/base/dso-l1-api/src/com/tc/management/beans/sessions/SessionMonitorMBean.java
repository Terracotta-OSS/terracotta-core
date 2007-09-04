/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.sessions;

import com.tc.management.TerracottaMBean;

/**
 * MBean for session monitoring of Terracotta-clustered sessions.  This MBean tracks 
 * session creation, session destruction, and requests processed.
 */
public interface SessionMonitorMBean extends TerracottaMBean {

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
   * Get count of total requests in sample
   * @return Total requests in sample
   */
  int getRequestCount();

  /**
   * @return Requests per second in sample
   */
  int getRequestRatePerSecond();

  /**
   * @return Sessions created in sample
   */
  int getCreatedSessionCount();

  /**
   * @return Session creation rate in sample
   */
  int getSessionCreationRatePerMinute();

  /**
   * @return Sessions destroyed in sample
   */
  int getDestroyedSessionCount();

  /**
   * @return Sessions destroyed rate in sample
   */
  int getSessionDestructionRatePerMinute();

  /**
   * Reset sampling
   */
  void reset();

  /**
   * Force session to expire
   * @param sessionId Session to expire
   * @return True if expired
   */
  boolean expireSession(String sessionId);

  /**
   * Register a sessions controller
   * @param comptroller Sessions controller
   */
  void registerSessionsController(SessionsComptroller comptroller);

  /**
   * Event indicating to mbean that a session was created.
   */
  void sessionCreated();

  /**
   * Event indicating to mbean that a session was destroyed.
   */
  void sessionDestroyed();

  /**
   * Event indicating to mbean that a request was processed.
   */
  void requestProcessed();

}
