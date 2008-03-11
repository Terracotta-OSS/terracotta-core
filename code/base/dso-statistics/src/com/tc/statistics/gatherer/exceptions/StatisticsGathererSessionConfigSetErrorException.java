/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

public class StatisticsGathererSessionConfigSetErrorException extends StatisticsGathererConfigErrorException {
  private final String sessionId;
  private final String key;
  private final Object value;

  public StatisticsGathererSessionConfigSetErrorException(final String sessionId, final String key, final Object value, final Throwable cause) {
    super("Unexpected exception while setting the config parameter '"+key+"' to value '"+value+"' for session ID '"+sessionId+".'", cause);
    this.sessionId = sessionId;
    this.key = key;
    this.value = value;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }
}