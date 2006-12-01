/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.sessions;

import com.tc.management.beans.sessions.SessionMonitorMBean;

public class SessionMonitorWrapper  {
  private SessionMonitorMBean bean;
  
  private int requestCount;
  private int requestRatePerSecond;
  private int createdSessionCount;
  private int sessionCreationRatePerMinute;
  private int destroyedSessionCount;
  private int sessionDestructionRatePerMinute;
  
  public SessionMonitorWrapper(SessionMonitorMBean bean) {
    this.bean = bean;
    
    requestCount                    = bean.getRequestCount();
    requestRatePerSecond            = bean.getRequestRatePerSecond();
    createdSessionCount             = bean.getCreatedSessionCount();
    sessionCreationRatePerMinute    = bean.getSessionCreationRatePerMinute();
    destroyedSessionCount           = bean.getDestroyedSessionCount();
    sessionDestructionRatePerMinute = bean.getSessionDestructionRatePerMinute();
  }
  
  public int getRequestCount() {
    return requestCount;
  }

  public int getRequestRatePerSecond() {
    return requestRatePerSecond;
  }

  public int getCreatedSessionCount() {
    return createdSessionCount;
  }

  public int getSessionCreationRatePerMinute() {
    return sessionCreationRatePerMinute;
  }

  public int getDestroyedSessionCount() {
    return destroyedSessionCount;
  }

  public int getSessionDestructionRatePerMinute() {
    return sessionDestructionRatePerMinute;
  }

  public boolean expireSession(String sessionId) {
    return bean.expireSession(sessionId);
  }
}
