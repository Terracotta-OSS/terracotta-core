/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer;

public interface StatisticsGathererListener {
  public void connected(String managerHostName, int managerPort);

  public void disconnected();

  public void reinitialized();

  public void capturingStarted(String sessionId);

  public void capturingStopped(String sessionId);

  public void sessionCreated(String sessionId);

  public void sessionClosed(String sessionId);

  public void statisticsEnabled(String[] names);
}