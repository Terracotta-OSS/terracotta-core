package com.tc.management.beans.sessions;

import com.tc.management.TerracottaMBean;


public interface SessionMonitorMBean extends TerracottaMBean {

  public static interface SessionsComptroller {
    boolean killSession(String sessionId);
  }

  int getRequestCount();

  int getRequestRatePerSecond();

  int getCreatedSessionCount();

  int getSessionCreationRatePerMinute();

  int getDestroyedSessionCount();

  int getSessionDestructionRatePerMinute();

  void reset();

  boolean expireSession(String sessionId);

  void registerSessionsController(SessionsComptroller comptroller);

  void sessionCreated();

  void sessionDestroyed();

  void requestProcessed();

}
