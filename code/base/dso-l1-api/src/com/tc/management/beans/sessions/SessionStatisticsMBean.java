/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.sessions;

import com.tc.management.TerracottaMBean;

/**
 * MBean for session monitoring of Terracotta-clustered sessions.  This MBean tracks 
 * session creation, session destruction, and requests processed.
 */
public interface SessionStatisticsMBean extends TerracottaMBean {

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
   * Force session to expire
   * @param sessionId Session to expire
   * @return True if expired
   */
  boolean expireSession(String sessionId);

  
//  /**
//   * Reset sampling
//   */
//  void reset();
}
