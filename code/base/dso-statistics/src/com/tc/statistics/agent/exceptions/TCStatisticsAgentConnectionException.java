/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent.exceptions;

import com.tc.exception.TCException;

public class TCStatisticsAgentConnectionException extends TCException {
  public TCStatisticsAgentConnectionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}