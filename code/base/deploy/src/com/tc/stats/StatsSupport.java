/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.stats.statistics.Statistic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;

public class StatsSupport extends NotificationBroadcasterSupport implements Serializable {
  private final Map              m_stats        = new HashMap();
  private final SynchronizedLong sequenceNumber = new SynchronizedLong(0L);

  public synchronized void addStatistic(String id, Statistic statistic) {
    m_stats.put(id, statistic);
  }

  public synchronized Statistic getStatistic(String id) {
    return (Statistic) m_stats.get(id);
  }

  public synchronized String[] getStatisticNames() {
    return (String[]) m_stats.keySet().toArray(new String[m_stats.size()]);
  }

  public synchronized Statistic[] getStatistics() {
    return (Statistic[]) m_stats.values().toArray(new Statistic[m_stats.size()]);
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    String name = AttributeChangeNotification.class.getName();
    String description = "An attribute of this MBean has changed";

    return new MBeanNotificationInfo[] { new MBeanNotificationInfo(types, name, description) };
  }

  protected void sendNotification(String msg, String attr, String type, Object oldVal, Object newVal) {
    sendNotification(new AttributeChangeNotification(this, sequenceNumber.increment(), System.currentTimeMillis(), msg,
                                                     attr, type, oldVal, newVal));
  }
}
