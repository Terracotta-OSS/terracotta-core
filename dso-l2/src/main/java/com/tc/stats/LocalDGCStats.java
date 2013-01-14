/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;

import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.stats.api.DGCMBean;

import javax.management.NotCompliantMBeanException;

public class LocalDGCStats extends AbstractNotifyingMBean implements DGCMBean {

  private final GCStatsEventPublisher gcStatsPublisher;

  public LocalDGCStats(GCStatsEventPublisher gcStatsPublisher) throws NotCompliantMBeanException {
    super(DGCMBean.class);
    this.gcStatsPublisher = gcStatsPublisher;
  }

  @Override
  public GCStats[] getGarbageCollectorStats() {
    return this.gcStatsPublisher.getGarbageCollectorStats();
  }

  @Override
  public long getLastCollectionGarbageCount() {
    GCStats gcStats = gcStatsPublisher.getLastGarbageCollectorStats();
    return gcStats != null ? gcStats.getActualGarbageCount() : -1;
  }

  @Override
  public long getLastCollectionElapsedTime() {
    GCStats gcStats = gcStatsPublisher.getLastGarbageCollectorStats();
    return gcStats != null ? gcStats.getElapsedTime() : -1;
  }

  @Override
  public void reset() {
    // TODO: implement this?
  }
}
