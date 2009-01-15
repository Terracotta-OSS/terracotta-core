/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import java.util.EventListener;

public interface IClusterStatsListener extends EventListener {
  void connected();
  
  void disconnected();
  
  void reinitialized();

  void sessionCreated(String sessionId);

  void sessionStarted(String sessionId);

  void sessionStopped(String sessionId);

  void sessionCleared(String sessionId);

  void allSessionsCleared();
}
