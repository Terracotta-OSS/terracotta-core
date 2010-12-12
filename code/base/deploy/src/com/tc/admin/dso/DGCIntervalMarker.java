/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso;

import org.jfree.chart.plot.IntervalMarker;

import com.tc.admin.common.ToolTipProvider;
import com.tc.objectserver.api.GCStats;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

public class DGCIntervalMarker extends IntervalMarker implements ToolTipProvider {
  private GCStats dgcInfo;
  private String  fToolTip;

  public DGCIntervalMarker(GCStats dgcInfo) {
    super(dgcInfo.getStartTime(), dgcInfo.getStartTime() + dgcInfo.getElapsedTime(), Color.yellow,
          new BasicStroke(0.5f), Color.yellow, new BasicStroke(0.5f), 0.3f);
    setGCStats(dgcInfo);
  }

  public void setGCStats(GCStats dgcStats) {
    this.dgcInfo = dgcStats;
    double endValue;
    long elapsedTime = dgcInfo.getElapsedTime();
    if (dgcInfo.getStatus().equals(GCStats.GC_CANCELED.getName())) {
      if (elapsedTime == -1) {
        elapsedTime = System.currentTimeMillis();
      }
      endValue = dgcInfo.getStartTime() + elapsedTime;
    } else if (elapsedTime == -1) {
      endValue = dgcInfo.getStartTime() + 100000000;
    } else {
      endValue = dgcInfo.getStartTime() + elapsedTime;
    }
    setEndValue(endValue);
    fToolTip = null;
  }

  public GCStats getGCStats() {
    return dgcInfo;
  }

  private void buildRow(StringBuffer sb, String name, Object value) {
    sb.append("<tr><td>");
    sb.append(name);
    sb.append("</td><td>");
    sb.append(value);
    sb.append("</td></tr>");
  }

  private void buildToolTip() {
    StringBuffer sb = new StringBuffer();
    NumberFormat numFormat = NumberFormat.getNumberInstance();
    DateFormat dateFormat = DateFormat.getInstance();
    sb.append("<html><table cellspacing=1 cellpadding=1>");
    buildRow(sb, "iteration", numFormat.format(dgcInfo.getIteration()));
    if (dgcInfo.getType() != null) {
      buildRow(sb, "type", dgcInfo.getType());
    }
    buildRow(sb, "start time", dateFormat.format(new Date(dgcInfo.getStartTime())));
    if (dgcInfo.getElapsedTime() != -1) {
      buildRow(sb, "total elapsed time (ms.)", numFormat.format(dgcInfo.getElapsedTime()));
    }
    if (dgcInfo.getBeginObjectCount() != -1) {
      buildRow(sb, "begin object count", numFormat.format(dgcInfo.getBeginObjectCount()));
    }
    if (dgcInfo.getPausedStageTime() != -1) {
      buildRow(sb, "paused stage time (ms.)", numFormat.format(dgcInfo.getPausedStageTime()));
    }
    if (dgcInfo.getMarkStageTime() != -1) {
      buildRow(sb, "mark stage time (ms.)", numFormat.format(dgcInfo.getMarkStageTime()));
    }
    if (dgcInfo.getActualGarbageCount() != -1) {
      buildRow(sb, "garbage count", numFormat.format(dgcInfo.getActualGarbageCount()));
    }
    if (dgcInfo.getDeleteStageTime() != -1) {
      buildRow(sb, "delete stage time (ms.)", numFormat.format(dgcInfo.getDeleteStageTime()));
    }
    if (dgcInfo.getEndObjectCount() != -1) {
      buildRow(sb, "end object count", numFormat.format(dgcInfo.getEndObjectCount()));
    }
    sb.append("</table></html>");
    fToolTip = sb.toString();
  }

  public String getToolTipText(MouseEvent me) {
    if (fToolTip == null) buildToolTip();
    return fToolTip;
  }
}
