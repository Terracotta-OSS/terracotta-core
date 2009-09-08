/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.HighLowItemLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

@SuppressWarnings("serial")
public class TooltipChartPanel extends ChartPanel {

  public TooltipChartPanel(JFreeChart chart) {
    this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
         DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, DEFAULT_BUFFER_USED, true, // properties
         true, // save
         true, // print
         true, // zoom
         true // tooltips
    );
  }

  public TooltipChartPanel(JFreeChart chart, boolean useBuffer) {
    this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
         DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, useBuffer, true, // properties
         true, // save
         true, // print
         true, // zoom
         true // tooltips
    );
  }

  public TooltipChartPanel(JFreeChart chart, boolean properties, boolean save, boolean print, boolean zoom,
                           boolean tooltips) {
    this(chart, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
         DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, DEFAULT_BUFFER_USED, properties, save, print, zoom,
         tooltips);
  }

  public TooltipChartPanel(JFreeChart chart, int width, int height, int minimumDrawWidth, int minimumDrawHeight,
                           int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer, boolean properties,
                           boolean save, boolean print, boolean zoom, boolean tooltips) {
    super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth, maximumDrawHeight, useBuffer,
          properties, save, print, zoom, tooltips);
  }

  /**
   * Returns a string for the tooltip.
   * 
   * @param e the mouse event.
   * @return A tool tip or <code>null</code> if no tooltip is available.
   */
  @Override
  public String getToolTipText(MouseEvent e) {
    String result = getTooltipAtPoint(e.getPoint());
    if (result != null) return result;
    else return super.getToolTipText(e);
  }

  private static int                       HOTSPOT_SIZE = 5;
  // TODO Add code to set and get these values
  private final HighLowItemLabelGenerator  hiLoTips     = new HighLowItemLabelGenerator();
  private final StandardXYToolTipGenerator xyTips       = new StandardXYToolTipGenerator();

  /**
   * This method attempts to get a tooltip by converting the screen X,Y into Chart Area X,Y and then looking for a data
   * point in a data set that lies inside a hotspot around that value.
   * 
   * @param point The Java 2D point
   * @return A string for the data at the point or null if no data is found.
   */
  protected String getTooltipAtPoint(Point point) {
    String result = null;

    Point2D translatedPoint = this.translateScreenToJava2D(point);
    JFreeChart chart = this.getChart();
    if (chart == null) { return null; }
    Plot plot = chart.getPlot();
    if (plot == null) { return null; }
    PlotRenderingInfo info = this.getChartRenderingInfo().getPlotInfo();
    if (plot instanceof CombinedDomainXYPlot) {
      int index = info.getSubplotIndex(translatedPoint);
      if (index < 0) index = 0;
      plot = (Plot) ((CombinedDomainXYPlot) plot).getSubplots().get(index);
      info = this.getChartRenderingInfo().getPlotInfo().getSubplotInfo(index);
    }
    if (plot != null && plot instanceof XYPlot) {
      XYPlot xyPlot = (XYPlot) plot;
      ValueAxis domainAxis = xyPlot.getDomainAxis();
      ValueAxis rangeAxis = xyPlot.getRangeAxis();
      Rectangle2D screenArea = this.scale(info.getDataArea());

      double hotspotSizeX = HOTSPOT_SIZE * this.getScaleX();
      double hotspotSizeY = HOTSPOT_SIZE * this.getScaleY();
      double x0 = point.getX();
      double y0 = point.getY();
      double x1 = x0 - hotspotSizeX;
      double y1 = y0 + hotspotSizeY;
      double x2 = x0 + hotspotSizeX;
      double y2 = y0 - hotspotSizeY;
      RectangleEdge xEdge = RectangleEdge.BOTTOM;
      RectangleEdge yEdge = RectangleEdge.LEFT;
      // Switch everything for horizontal charts
      if (xyPlot.getOrientation() == PlotOrientation.HORIZONTAL) {
        hotspotSizeX = HOTSPOT_SIZE * this.getScaleY();
        hotspotSizeY = HOTSPOT_SIZE * this.getScaleX();
        x0 = point.getY();
        y0 = point.getX();
        x1 = x0 + hotspotSizeX;
        y1 = y0 - hotspotSizeY;
        x2 = x0 - hotspotSizeX;
        y2 = y0 + hotspotSizeY;
        xEdge = RectangleEdge.LEFT;
        yEdge = RectangleEdge.BOTTOM;
      }

      double ty0 = rangeAxis.java2DToValue(y0, screenArea, yEdge);
      double tx1 = domainAxis.java2DToValue(x1, screenArea, xEdge);
      double ty1 = rangeAxis.java2DToValue(y1, screenArea, yEdge);
      double tx2 = domainAxis.java2DToValue(x2, screenArea, xEdge);
      double ty2 = rangeAxis.java2DToValue(y2, screenArea, yEdge);

      int datasetCount = xyPlot.getDatasetCount();
      XYItemRenderer itemRenderer = xyPlot.getRenderer();
      for (int datasetIndex = 0; datasetIndex < datasetCount; datasetIndex++) {
        XYDataset dataset = xyPlot.getDataset(datasetIndex);
        XYToolTipGenerator tipGenerator = itemRenderer.getSeriesToolTipGenerator(datasetIndex);
        if (tipGenerator == null) tipGenerator = itemRenderer.getBaseToolTipGenerator();
        int seriesCount = dataset.getSeriesCount();
        for (int series = 0; series < seriesCount; series++) {
          int itemCount = dataset.getItemCount(series);
          if (dataset instanceof OHLCDataset) {
            if (tipGenerator == null) tipGenerator = hiLoTips;

            // This could be optimized to use a binary search for x first
            for (int item = 0; item < itemCount; item++) {
              double xValue = dataset.getXValue(series, item);
              double yValueHi = ((OHLCDataset) dataset).getHighValue(series, item);
              double yValueLo = ((OHLCDataset) dataset).getLowValue(series, item);
              // Check hi lo and swap if needed
              if (yValueHi < yValueLo) {
                double temp = yValueHi;
                yValueHi = yValueLo;
                yValueLo = temp;
              }
              // Check if the dataset 'X' value lies between the hotspot (tx1 < xValue < tx2)
              if (tx1 < xValue && xValue < tx2)
              // Check if the cursor 'y' value lies between the high and low (low < ty0 < high)
              if (yValueLo < ty0 && ty0 < yValueHi) { return tipGenerator.generateToolTip(dataset, series, item); }
            }
          } else {
            if (tipGenerator == null) tipGenerator = xyTips;

            // This could be optimized to use a binary search for x first
            for (int item = 0; item < itemCount; item++) {
              double xValue = dataset.getXValue(series, item);
              double yValue = dataset.getYValue(series, item);
              // Check if the dataset 'X' value lies between the hotspot (tx1< xValue < tx2)
              if (tx1 < xValue && xValue < tx2)
              // Check if the dataset 'Y' value lies between the hotspot (ty1 < yValue < ty2)
              if (ty1 < yValue && yValue < ty2) { return tipGenerator.generateToolTip(dataset, series, item); }
            }
          }
        }
      }
    }

    return result;
  }
}