/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.awt.Color;
import java.text.SimpleDateFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

public class DemoChartFactory {
  public static JFreeChart getChart(TimeSeries ts) {
    XYDataset dataset = createDataset(ts);
    return createChart(dataset);
  }

  private static JFreeChart createChart(XYDataset dataset) {
    JFreeChart chart =
      ChartFactory.createTimeSeriesChart(
	      "Transaction Rate",
	      "Time",
	      "Txn Per Second",
	      dataset,
	      true,
	      true,
	      false);

    chart.setBackgroundPaint(Color.white);

    XYPlot plot = (XYPlot)chart.getPlot();

    plot.setBackgroundPaint(Color.lightGray);
    plot.setDomainGridlinePaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);

    XYItemRenderer r = plot.getRenderer();

    if(r instanceof XYLineAndShapeRenderer) {
      XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)r;

      renderer.setBaseShapesVisible(true);
      renderer.setBaseShapesFilled(true);
    }

    DateAxis axis = (DateAxis) plot.getDomainAxis();
    axis.setDateFormatOverride(new SimpleDateFormat("kk:mm:ss"));

    return chart;
  }

  private static XYDataset createDataset(TimeSeries s1) {
    TimeSeriesCollection dataset = new TimeSeriesCollection();

    dataset.addSeries(s1);
    
    dataset.setDomainIsPointsInTime(true);

    return dataset;
  }
}
