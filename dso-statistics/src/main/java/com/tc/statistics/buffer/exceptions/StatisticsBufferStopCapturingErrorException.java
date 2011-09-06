/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class StatisticsBufferStopCapturingErrorException extends StatisticsBufferException {
  private final String sessionId;

  public StatisticsBufferStopCapturingErrorException(final String sessionId, final Throwable cause) {
    super("The capture session with the cluster-wide ID '" + sessionId + "' could not be stopped.", cause);
    this.sessionId = sessionId;
  }

  public String getSessionId() {
    return sessionId;
  }
}