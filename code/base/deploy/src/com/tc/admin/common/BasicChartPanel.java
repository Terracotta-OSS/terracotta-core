/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.common;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.Layer;

import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class BasicChartPanel extends TooltipChartPanel {
  public BasicChartPanel(JFreeChart chart) {
    super(chart, ChartPanel.DEFAULT_WIDTH, ChartPanel.DEFAULT_HEIGHT, ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
          ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT, ChartPanel.DEFAULT_MAXIMUM_DRAW_WIDTH,
          ChartPanel.DEFAULT_MAXIMUM_DRAW_HEIGHT, true, false, false, false, false, true);
  }

  public BasicChartPanel(JFreeChart chart, boolean useBuffer) {
    this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
         DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, useBuffer, true, // properties
         true, // save
         true, // print
         true, // zoom
         true // tooltips
    );
  }

  public BasicChartPanel(JFreeChart chart, boolean properties, boolean save, boolean print, boolean zoom,
                         boolean tooltips) {
    this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
         DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, DEFAULT_BUFFER_USED, properties, save, print, zoom,
         tooltips);
  }

  public BasicChartPanel(JFreeChart chart, int width, int height, int minimumDrawWidth, int minimumDrawHeight,
                         int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer, boolean properties,
                         boolean save, boolean print, boolean zoom, boolean tooltips) {
    super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth, maximumDrawHeight, useBuffer,
          properties, save, print, zoom, tooltips);
  }

  @Override
  public String getToolTipText(MouseEvent me) {
    String toolTip = null;
    JFreeChart chart = getChart();
    if (chart != null) {
      Plot plot = chart.getXYPlot();
      if (plot instanceof XYPlot) {
        XYPlot xyPlot = (XYPlot) plot;
        Collection<?> domainMarkers = new HashSet();
        Collection<?> fgDomainMarkers = xyPlot.getDomainMarkers(Layer.FOREGROUND);
        if (fgDomainMarkers != null) {
          domainMarkers.addAll(new HashSet(fgDomainMarkers));
        }
        Collection<?> bgDomainMarkers = xyPlot.getDomainMarkers(Layer.BACKGROUND);
        if (bgDomainMarkers != null) {
          domainMarkers.addAll(new HashSet(bgDomainMarkers));
        }
        PlotRenderingInfo info = getChartRenderingInfo().getPlotInfo();
        Insets insets = getInsets();
        double x = (me.getX() - insets.left) / getScaleX();
        double xx = xyPlot.getDomainAxis().java2DToValue(x, info.getDataArea(), xyPlot.getDomainAxisEdge());
        Iterator<?> domainMarkerIter = domainMarkers.iterator();
        while (domainMarkerIter.hasNext()) {
          IntervalMarker marker = (IntervalMarker) domainMarkerIter.next();
          if (marker instanceof ToolTipProvider) {
            if (xx >= marker.getStartValue() && xx <= marker.getEndValue()) {
              toolTip = ((ToolTipProvider) marker).getToolTipText(me);
            }
          }
        }
      }
    }
    return toolTip != null ? toolTip : super.getToolTipText(me);
  }
}
