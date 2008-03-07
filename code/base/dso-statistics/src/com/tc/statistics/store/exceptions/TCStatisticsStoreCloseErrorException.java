/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.exceptions;

public class TCStatisticsStoreCloseErrorException extends TCStatisticsStoreException {
  public TCStatisticsStoreCloseErrorException(final Throwable cause) {
    super("Unexpected error while closing the store database.", cause);
  }
}