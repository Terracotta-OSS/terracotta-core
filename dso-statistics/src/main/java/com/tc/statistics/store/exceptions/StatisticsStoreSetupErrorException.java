/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.exceptions;

public class StatisticsStoreSetupErrorException extends StatisticsStoreException {
  public StatisticsStoreSetupErrorException(final Throwable cause) {
    super("Unexpected error while preparing the statements for the H2 statistics store.", cause);
  }
}