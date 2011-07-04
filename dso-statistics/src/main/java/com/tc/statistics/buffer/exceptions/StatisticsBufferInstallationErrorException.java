/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class StatisticsBufferInstallationErrorException extends StatisticsBufferException {
  public StatisticsBufferInstallationErrorException(final Throwable cause) {
    super("Unable to install the H2 database table structure.", cause);
  }
}
