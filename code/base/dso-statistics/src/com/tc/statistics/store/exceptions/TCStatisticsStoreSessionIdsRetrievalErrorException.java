/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.exceptions;

public class TCStatisticsStoreSessionIdsRetrievalErrorException extends TCStatisticsStoreException {
  public TCStatisticsStoreSessionIdsRetrievalErrorException(final Throwable cause) {
    super("Unexpected error while retrieving the available session IDs.", cause);
  }
}