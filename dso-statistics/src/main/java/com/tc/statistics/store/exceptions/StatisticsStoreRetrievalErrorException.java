/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.exceptions;

import com.tc.statistics.store.StatisticsRetrievalCriteria;

public class StatisticsStoreRetrievalErrorException extends StatisticsStoreException {
  private final StatisticsRetrievalCriteria criteria;

  public StatisticsStoreRetrievalErrorException(final StatisticsRetrievalCriteria criteria, final Throwable cause) {
    super("Unexpected error while retrieving the statistic data for criteria '" + criteria + "'.", cause);

    this.criteria = criteria;
  }

  public StatisticsRetrievalCriteria getCriteria() {
    return criteria;
  }
}