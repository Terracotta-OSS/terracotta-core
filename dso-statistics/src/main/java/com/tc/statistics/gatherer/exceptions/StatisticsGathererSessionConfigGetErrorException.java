/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

public class StatisticsGathererSessionConfigGetErrorException extends StatisticsGathererConfigErrorException {
  private final String sessionId;
  private final String key;

  public StatisticsGathererSessionConfigGetErrorException(final String sessionId, final String key, final Throwable cause) {
    super("Unexpected exception while retrieving the config value '"+key+"' for session ID '"+sessionId+".'", cause);
    this.sessionId = sessionId;
    this.key = key;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getKey() {
    return key;
  }
}