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
import java.util.Date;

public class DGCIntervalMarker extends IntervalMarker implements ToolTipProvider {
  private final GCStats dgcInfo;
  private String        fToolTip;

  public DGCIntervalMarker(GCStats dgcInfo) {
    super(dgcInfo.getStartTime(), dgcInfo.getStartTime() + dgcInfo.getElapsedTime(), Color.yellow,
          new BasicStroke(0.5f), Color.yellow, new BasicStroke(0.5f), 0.3f);
    this.dgcInfo = dgcInfo;
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

  /**
   * PausedStageTime, MarkStageTime, and DeleteStageTime are new in 2.7. Show these values only if set.
   */
  private void buildToolTip() {
    StringBuffer sb = new StringBuffer();
    sb.append("<html><table cellspacing=1 cellpadding=1>");
    buildRow(sb, "iteration", dgcInfo.getIteration());
    if (dgcInfo.getType() != null) {
      buildRow(sb, "type", dgcInfo.getType());
    }
    buildRow(sb, "start time", DateFormat.getInstance().format(new Date(dgcInfo.getStartTime())));
    buildRow(sb, "total elapsed time", dgcInfo.getElapsedTime());
    buildRow(sb, "begin object count", dgcInfo.getBeginObjectCount());
    if (dgcInfo.getPausedStageTime() != -1) {
      buildRow(sb, "paused stage time", dgcInfo.getPausedStageTime());
    }
    if (dgcInfo.getMarkStageTime() != -1) {
      buildRow(sb, "mark stage time", dgcInfo.getMarkStageTime());
    }
    buildRow(sb, "garbage count", dgcInfo.getActualGarbageCount());
    if (dgcInfo.getDeleteStageTime() != -1) {
      buildRow(sb, "delete stage time", dgcInfo.getDeleteStageTime());
    }
    sb.append("</table></html>");
    fToolTip = sb.toString();
  }

  public String getToolTipText(MouseEvent me) {
    if (fToolTip == null) buildToolTip();
    return fToolTip;
  }
}
