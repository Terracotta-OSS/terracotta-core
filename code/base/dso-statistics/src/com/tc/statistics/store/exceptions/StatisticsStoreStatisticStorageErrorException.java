/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.store.exceptions;

import com.tc.statistics.StatisticData;

public class StatisticsStoreStatisticStorageErrorException extends StatisticsStoreException {
  private final Long          id;
  private final StatisticData data;

  public StatisticsStoreStatisticStorageErrorException(final long id, final StatisticData data, final Throwable cause) {
    super("Unexpected error while storing the statistic with id '" + id + "' and data " + data + ".", cause);
    this.id = Long.valueOf(id);
    this.data = data;
  }

  public StatisticsStoreStatisticStorageErrorException(final StatisticData data, final Throwable cause) {
    super("Unexpected error while storing the statistic data " + data + ".", cause);
    this.id = null;
    this.data = data;
  }

  public Long getId() {
    return id;
  }

  public StatisticData getData() {
    return data;
  }
}
