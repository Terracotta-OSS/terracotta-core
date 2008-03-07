/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class TCStatisticsBufferDatabaseOpenErrorException extends TCStatisticsBufferException {
  public TCStatisticsBufferDatabaseOpenErrorException(final Throwable cause) {
    super("Unexpected error while opening the buffer database.", cause);
  }
}