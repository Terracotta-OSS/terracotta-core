/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class StatisticsBufferStopCapturingSessionNotFoundException extends StatisticsBufferException {
  private final String sessionId;

  public StatisticsBufferStopCapturingSessionNotFoundException(final String sessionId) {
    super("The capture session with the cluster-wide ID '" + sessionId + "' could not be stopped since the session couldn't be found.", null);
    this.sessionId = sessionId;
  }

  public String getSessionId() {
    return sessionId;
  }
}