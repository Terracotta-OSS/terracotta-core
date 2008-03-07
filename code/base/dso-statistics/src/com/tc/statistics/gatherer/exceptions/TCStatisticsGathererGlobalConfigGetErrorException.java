/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

public class TCStatisticsGathererGlobalConfigGetErrorException extends TCStatisticsGathererConfigErrorException {
  private final String key;

  public TCStatisticsGathererGlobalConfigGetErrorException(final String key, final Throwable cause) {
    super("Unexpected exception while retrieving the global config value '"+key+"'.'", cause);
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}