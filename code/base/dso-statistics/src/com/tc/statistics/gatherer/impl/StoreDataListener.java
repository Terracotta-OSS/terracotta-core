/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.StatisticsStoreException;

import java.util.Iterator;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;

class StoreDataListener implements NotificationListener {
  public void handleNotification(final Notification notification, final Object o) {
    List data = (List)notification.getUserData();
    try {
      for (Iterator it = data.iterator(); it.hasNext(); ) {
        StatisticData stat_data = (StatisticData)it.next();
        ((StatisticsStore)o).storeStatistic(stat_data);
      }
    } catch (StatisticsStoreException e) {
      throw new TCRuntimeException(e);
    }
  }
}