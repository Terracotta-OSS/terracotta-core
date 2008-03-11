/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.exceptions;

import com.tc.statistics.StatisticData;

public class StatisticDataInjectionErrorException extends AgentStatisticsManagerException {
  private final String sessionId;
  private final StatisticData data;

  public StatisticDataInjectionErrorException(final String sessionId, final StatisticData data, final Throwable cause) {
    super("Unexpected error while injecting data '" + data + "' for session ID '" + sessionId + "'.", cause);
    this.sessionId = sessionId;
    this.data = data;
  }

  public String getSessionId() {
    return sessionId;
  }

  public StatisticData getData() {
    return data;
  }
}