/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

import com.tc.exception.TCException;

public class TCStatisticsBufferException extends TCException {
  public TCStatisticsBufferException(final String message, final Throwable cause) {
    super(message, cause);
  }
}