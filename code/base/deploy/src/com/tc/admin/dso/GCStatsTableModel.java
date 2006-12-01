/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.objectserver.api.GCStats;

import com.tc.admin.AdminClient;
import com.tc.admin.common.XObjectTableModel;

public class GCStatsTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = {
    "Iteration",
    "StartDate",
    "ElapsedTime",
    "BeginObjectCount",
    "CandidateGarbageCount",
    "ActualGarbageCount"
  };

  private static final String[] HEADERS =
    AdminClient.getContext().getMessages(
      new String[] {
        "dso.gcstats.iteration",
        "dso.gcstats.startTime",
        "dso.gcstats.elapsedTime",
        "dso.gcstats.beginObjectCount",
        "dso.gcstats.candidateGarbageCount",
        "dso.gcstats.actualGarbageCount"
      });

  public GCStatsTableModel() {
    super(GCStatsWrapper.class, FIELDS, HEADERS);
  }

  public void setGCStats(GCStats[] gcStats) {
    int              count    = gcStats != null ? gcStats.length : 0;
    GCStatsWrapper[] wrappers = new GCStatsWrapper[count];

    for(int i = 0; i < count; i++) {
      wrappers[i] = new GCStatsWrapper(gcStats[i]);
    }

    set(wrappers);
  }

  public void addGCStats(GCStats gcStats) {
    add(0, new GCStatsWrapper(gcStats));
    fireTableRowsInserted(0, 0);
  }
}
