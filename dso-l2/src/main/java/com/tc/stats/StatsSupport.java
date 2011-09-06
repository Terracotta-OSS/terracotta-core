/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import com.tc.stats.statistics.Statistic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;

// TODO: remove me

public class StatsSupport extends NotificationBroadcasterSupport implements Serializable {
  private final Map        statMap        = new HashMap();
  private final AtomicLong sequenceNumber = new AtomicLong();

  public synchronized void addStatistic(String id, Statistic statistic) {
    statMap.put(id, statistic);
  }

  public synchronized Statistic getStatistic(String id) {
    return (Statistic) statMap.get(id);
  }

  public synchronized String[] getStatisticNames() {
    return (String[]) statMap.keySet().toArray(new String[statMap.size()]);
  }

  public synchronized Statistic[] getStatistics() {
    return (Statistic[]) statMap.values().toArray(new Statistic[statMap.size()]);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    String name = AttributeChangeNotification.class.getName();
    String description = "An attribute of this MBean has changed";

    return new MBeanNotificationInfo[] { new MBeanNotificationInfo(types, name, description) };
  }

  protected void sendNotification(String msg, String attr, String type, Object oldVal, Object newVal) {
    sendNotification(new AttributeChangeNotification(this, sequenceNumber.getAndIncrement(),
                                                     System.currentTimeMillis(), msg, attr, type, oldVal, newVal));
  }
}
