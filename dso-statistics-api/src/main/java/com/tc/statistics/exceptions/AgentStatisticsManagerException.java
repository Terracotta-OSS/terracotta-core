/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.exceptions;

import com.tc.exception.TCException;

public class AgentStatisticsManagerException extends TCException {
  public AgentStatisticsManagerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
