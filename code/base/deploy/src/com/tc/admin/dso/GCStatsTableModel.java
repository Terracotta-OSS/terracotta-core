/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.objectserver.api.GCStats;

import com.tc.admin.AdminClient;
import com.tc.admin.common.XObjectTableModel;

public class GCStatsTableModel extends XObjectTableModel {

  private static final String[] FIELDS   = { "Iteration", "Type", "Status", "StartDate", "BeginObjectCount",
      "PausedStageTime", "MarkStageTime", "ActualGarbageCount", "DeleteStageTime", "ElapsedTime" };

  private static final String[] HEADERS  = AdminClient.getContext().getMessages(
                                                                                new String[] { "dso.gcstats.iteration",
      "dso.gcstats.type", "dso.gcstats.status", "dso.gcstats.startTime", "dso.gcstats.beginObjectCount",
      "dso.gcstats.pausedStageTime", "dso.gcstats.markStageTime", "dso.gcstats.actualGarbageCount",
      "dso.gcstats.deleteStageTime", "dso.gcstats.elapsedTime"                 });

  private static final int      MAX_SIZE = 1500;

  public GCStatsTableModel() {
    super(GCStatsWrapper.class, FIELDS, HEADERS);
  }

  public void setGCStats(GCStats[] gcStats) {
    int count = gcStats != null ? gcStats.length : 0;
    GCStatsWrapper[] wrappers = new GCStatsWrapper[count];

    for (int i = 0; i < count; i++) {
      wrappers[i] = new GCStatsWrapper(gcStats[i]);
    }

    set(wrappers);
  }

  private int iterationRow(int iteration) {
    int rowCount = getRowCount();
    for (int i = 0; i < rowCount; i++) {
      GCStatsWrapper wrapper = (GCStatsWrapper) getObjectAt(i);
      if (iteration == wrapper.getIteration()) { return i; }
    }
    return -1;
  }

  public void addGCStats(GCStats gcStats) {
    int row = iterationRow(gcStats.getIteration());
    if (row != -1) {
      ((GCStatsWrapper) getObjectAt(row)).set(gcStats);
      fireTableRowsUpdated(row, row);
    } else {
      add(0, new GCStatsWrapper(gcStats));
      fireTableRowsInserted(0, 0);
    }
    testTrimRows();
  }

  private void testTrimRows() {
    int count = getRowCount();
    if (count > MAX_SIZE) {
      int origCount = count;
      while (count > MAX_SIZE) {
        remove(count - 1);
        count--;
      }
      fireTableRowsDeleted(count - 1, origCount - 1);
    }
  }

  // no sorting allowed
  public boolean isColumnSortable(int col) {
    return false;
  }
}
