/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

import com.tc.exception.TCException;

public class StatisticsGathererException extends TCException {
  private StatisticsGathererException next;

  public StatisticsGathererException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public StatisticsGathererException getNextException() {
    return next;
  }

  public synchronized void setNextException(final StatisticsGathererException e) {
    StatisticsGathererException last = this;
    while (last.next != null) {
      last = last.next;
    }
    last.next = e;
  }
}