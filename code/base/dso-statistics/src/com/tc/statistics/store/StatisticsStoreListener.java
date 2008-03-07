/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

public interface StatisticsStoreListener {
  public void opened();

  public void closed();

  public void sessionCleared(String sessionId);

  public void allSessionsCleared();
}