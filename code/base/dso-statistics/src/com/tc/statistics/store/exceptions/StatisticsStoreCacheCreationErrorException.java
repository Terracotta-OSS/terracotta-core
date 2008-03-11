/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.exceptions;

public class StatisticsStoreCacheCreationErrorException extends StatisticsStoreException {
  public StatisticsStoreCacheCreationErrorException(final Throwable cause) {
    super("Unable to create teh data caches in the data store.", cause);
  }
}