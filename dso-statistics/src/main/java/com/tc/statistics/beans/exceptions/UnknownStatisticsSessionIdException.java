/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.beans.exceptions;

public class UnknownStatisticsSessionIdException extends RuntimeException {
  private final String nodeName;
  private final String sessionId;

  public UnknownStatisticsSessionIdException(final String nodeName, final String sessionId, final Throwable e) {
    super("Unknown cluster-wide session ID '"+sessionId+"' on node '"+nodeName+"'.", e);
    this.nodeName = nodeName;
    this.sessionId = sessionId;
  }

  public String getNodeName() {
    return nodeName;
  }

  public String getSessionId() {
    return sessionId;
  }
}
