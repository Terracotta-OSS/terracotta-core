/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
