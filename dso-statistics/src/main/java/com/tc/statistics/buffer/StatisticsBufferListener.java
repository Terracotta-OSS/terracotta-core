/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer;

public interface StatisticsBufferListener {
  public void capturingStarted(String sessionId);
  public void capturingStopped(String sessionId);
  public void opened();
  public void closing();
  public void closed();
}