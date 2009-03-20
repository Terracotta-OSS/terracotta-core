/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;

import com.tc.management.TerracottaMBean;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;

import javax.management.NotCompliantMBeanException;

public class LocalDGCStats extends AbstractNotifyingMBean implements DGCMBean, TerracottaMBean {

  private final GCStatsEventPublisher gcStatsPublisher;

  public LocalDGCStats(GCStatsEventPublisher gcStatsPublisher) throws NotCompliantMBeanException {
    super(DGCMBean.class);
    this.gcStatsPublisher = gcStatsPublisher;
  }

  public GCStats[] getGarbageCollectorStats() {
    return this.gcStatsPublisher.getGarbageCollectorStats();
  }

  public long getLastCollectionGarbageCount() {
    GCStats gcStats = gcStatsPublisher.getLastGarbageCollectorStats();
    return gcStats != null ? gcStats.getActualGarbageCount() : -1;
  }

  public long getLastCollectionElapsedTime() {
    GCStats gcStats = gcStatsPublisher.getLastGarbageCollectorStats();
    return gcStats != null ? gcStats.getElapsedTime() : -1;
  }

  public void reset() {
    // TODO: implement this?
  }
}
