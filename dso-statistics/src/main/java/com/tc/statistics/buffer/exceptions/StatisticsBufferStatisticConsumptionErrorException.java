/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class StatisticsBufferStatisticConsumptionErrorException extends StatisticsBufferException {
  private final String sessionId;

  public StatisticsBufferStatisticConsumptionErrorException(final String sessionId, final Throwable cause) {
    super("Unexpected error while consuming the statistic data for session with cluster-wide ID '" + sessionId + "'.", cause);
    this.sessionId = sessionId;
  }

  public String getSessionId() {
    return sessionId;
  }
}