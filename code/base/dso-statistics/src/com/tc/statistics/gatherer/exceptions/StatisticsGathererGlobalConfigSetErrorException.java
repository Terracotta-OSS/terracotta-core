/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

public class StatisticsGathererGlobalConfigSetErrorException extends StatisticsGathererConfigErrorException {
  private final String key;
  private final Object value;
  
  public StatisticsGathererGlobalConfigSetErrorException(final String key, final Object value, final Throwable cause) {
    super("Unexpected exception while setting the global config parameter '"+key+"' to value '"+value+"'.'", cause);
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }
}