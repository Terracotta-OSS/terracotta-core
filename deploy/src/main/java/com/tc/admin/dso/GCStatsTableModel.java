/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XObjectTableModel;
import com.tc.objectserver.api.GCStats;

import java.util.Arrays;
import java.util.Date;

public class GCStatsTableModel extends XObjectTableModel {
  private static final String[] FIELDS   = { "Iteration", "Type", "Status", "StartDate", "BeginObjectCount",
      "PausedStageTime", "MarkStageTime", "ActualGarbageCount", "DeleteStageTime", "ElapsedTime", "EndObjectCount" };

  private final String[]        HEADERS  = { "dso.gcstats.iteration", "dso.gcstats.type", "dso.gcstats.status",
      "dso.gcstats.startTime", "dso.gcstats.beginObjectCount", "dso.gcstats.pausedStageTime",
      "dso.gcstats.markStageTime", "dso.gcstats.actualGarbageCount", "dso.gcstats.deleteStageTime",
      "dso.gcstats.elapsedTime", "dso.gcstats.endObjectCount" };

  private static final int      MAX_SIZE = 1500;

  public GCStatsTableModel(ApplicationContext appContext) {
    super();
    configure(GCStatsWrapper.class, FIELDS, appContext.getMessages(HEADERS));
  }

  public void setGCStats(GCStats[] gcStats) {
    int count = gcStats != null ? gcStats.length : 0;
    GCStatsWrapper[] wrappers = new GCStatsWrapper[count];

    for (int i = 0; i < count; i++) {
      wrappers[i] = new GCStatsWrapper(gcStats[i]);
    }
    Arrays.sort(wrappers);
    set(wrappers);
  }

  public long getFirstStartTime() {
    int rowCount = getRowCount();
    return rowCount > 0 ? ((GCStatsWrapper) getObjectAt(0)).getStartTime() : System.currentTimeMillis();
  }

  public long getFirstEndTime() {
    int rowCount = getRowCount();
    if (rowCount > 0) {
      GCStatsWrapper wrapper = (GCStatsWrapper) getObjectAt(0);
      long elapsed = wrapper.getElapsedTime();
      return elapsed != -1 ? wrapper.getStartTime() + elapsed : -1;
    } else {
      return System.currentTimeMillis();
    }
  }

  public long getLastStartTime() {
    int rowCount = getRowCount();
    return rowCount > 0 ? ((GCStatsWrapper) getObjectAt(rowCount - 1)).getStartTime() : System.currentTimeMillis();
  }

  public long getLastEndTime() {
    int rowCount = getRowCount();
    if (rowCount > 0) {
      GCStatsWrapper wrapper = (GCStatsWrapper) getObjectAt(rowCount - 1);
      long elapsed = wrapper.getElapsedTime();
      return elapsed != -1 ? wrapper.getStartTime() + elapsed : -1;
    } else {
      return System.currentTimeMillis();
    }
  }

  public int iterationRow(int iteration) {
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
  @Override
  public boolean isColumnSortable(int col) {
    return false;
  }

  public String exportAsText() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("Iteration").append(" | ").append("Type").append(" | ").append("Status").append(" | ")
        .append("Start Time").append(" | ").append("Begin Count").append(" | ").append("Pasused stage(ms)")
        .append(" | ").append("Mark Stage(ms.)").append(" | ").append("Garbage Count").append(" | ")
        .append("Delete stage(ms.)").append(" | ").append("Total Elapsed time(ms.)").append(" | ").append("End Count")
        .append("\n");

    for (int i = 0; i < getRowCount(); i++) {
      GCStatsWrapper stat = (GCStatsWrapper) getObjectAt(i);
      strBuilder.append(stat.getIteration()).append(" | ").append(stat.getType()).append(" | ")
          .append(stat.getStatus()).append(" | ").append(new Date(stat.getStartTime())).append(" | ")
          .append(stat.getBeginObjectCount()).append(" | ").append(stat.getPausedStageTime()).append(" | ")
          .append(stat.getMarkStageTime()).append(" | ").append(stat.getActualGarbageCount()).append(" | ")
          .append(stat.getDeleteStageTime()).append(" | ").append(stat.getElapsedTime()).append(" | ")
          .append(stat.getEndObjectCount()).append("\n");
    }
    return strBuilder.toString();
  }
}
