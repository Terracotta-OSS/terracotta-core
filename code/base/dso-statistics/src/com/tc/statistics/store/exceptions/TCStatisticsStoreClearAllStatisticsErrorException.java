/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.exceptions;

public class TCStatisticsStoreClearAllStatisticsErrorException extends TCStatisticsStoreException {
  public TCStatisticsStoreClearAllStatisticsErrorException(final Throwable cause) {
    super("Unexpected error while clearing all the statistics.", cause);
   }
}