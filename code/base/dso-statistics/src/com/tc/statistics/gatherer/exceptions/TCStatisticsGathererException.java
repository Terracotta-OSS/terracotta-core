/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

import com.tc.exception.TCException;

public class TCStatisticsGathererException extends TCException {
  private TCStatisticsGathererException next;

  public TCStatisticsGathererException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public TCStatisticsGathererException getNextException() {
    return next;
  }

  public synchronized void setNextException(final TCStatisticsGathererException e) {
    TCStatisticsGathererException last = this;
    while (last.next != null) {
      last = last.next;
    }
    last.next = e;
  }
}