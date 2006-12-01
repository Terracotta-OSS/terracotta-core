/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.results;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

import com.tctest.performance.generate.load.Measurement;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JPanel;

/**
 * This class acccepts an {@link com.tctest.performance.results.PerformanceMeasurementMarshaller} data file path as an
 * argument. The system property <tt>-Dclip=true</tt> will clip the workqueue waittime line to allow a more accurate
 * resolution for the other data points.
 */
public class ResultsGraph extends ApplicationFrame {

  private static final Color[] colors = new Color[] { Color.red, Color.blue, Color.green, Color.orange, Color.cyan,
      Color.magenta, Color.yellow, Color.pink };

  public ResultsGraph(PerformanceMeasurementMarshaller.Statistics stats, boolean save) {
    super(stats.header.title + " - duration: " + stats.header.duration + " secs.");
    ChartPanel chartPanel = (ChartPanel) createDemoPanel(stats, save);
    chartPanel.setPreferredSize(new java.awt.Dimension(700, 500));
    chartPanel.setMouseZoomable(true, false);
    setContentPane(chartPanel);
  }

  private static JFreeChart createChart(XYDataset dataset, PerformanceMeasurementMarshaller.Header header) {
    JFreeChart chart = ChartFactory.createXYLineChart(header.title, header.xLabel, header.yLabel, dataset,
                                                      PlotOrientation.VERTICAL, true, true, false);

    chart.setAntiAlias(true);
    chart.setBackgroundPaint(Color.white);
    chart.setPadding(new RectangleInsets(20.0, 20.0, 20.0, 20.0));

    XYPlot plot = (XYPlot) chart.getPlot();
    XYLineAndShapeRenderer xyColors = (XYLineAndShapeRenderer) plot.getRenderer();
    int xyCount = plot.getSeriesCount();
    for (int i = 0; i < xyCount; i++) {
      if (i == xyCount - 1) {
        xyColors.setSeriesPaint(i, Color.LIGHT_GRAY); // set line colors
        break;
      }
      xyColors.setSeriesPaint(i, colors[i]); // set line colors
    }

    plot.setBackgroundPaint(new Color(240, 240, 240));
    plot.setDomainGridlinePaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
    plot.setDomainAxis(new NumberAxis());
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);

    XYItemRenderer r = plot.getRenderer();
    if (r instanceof XYLineAndShapeRenderer) {
      XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
      renderer.setBaseShapesVisible(false);
      renderer.setBaseShapesFilled(true);
    }
    return chart;
  }

  private static XYDataset createDataset(List measurementList, String[] titles) {
    long max = 0; // clip overflow data to fix line scale proportions
    long current;
    int size = measurementList.size();
    if (size > colors.length) throw new RuntimeException("Unable to draw more than " + colors.length + " lines.");
    XYSeriesCollection dataset = new XYSeriesCollection();
    XYSeries xySeries;
    Measurement[] measurements;

    for (int i = size - 1; i >= 0; i--) {
      measurements = (Measurement[]) measurementList.get(i);
      xySeries = new XYSeries(titles[i]);

      for (int j = 0; j < measurements.length; j++) {
        current = measurements[j].y;
        if (i != 0) {
          if (current > max) max = current;
        } else if (System.getProperty("clip", "false").equals("true")) {
          if (current > max * 4) break;
        }
        xySeries.add(measurements[j].x, current);
      }

      dataset.addSeries(xySeries);
    }
    return dataset;
  }

  public static JPanel createDemoPanel(PerformanceMeasurementMarshaller.Statistics stats, boolean save) {
    PerformanceMeasurementMarshaller.Header header = stats.header;
    JFreeChart chart = createChart(createDataset(stats.measurements, stats.lineDescriptions), header);
    if (save) {
      try {
        ChartUtilities.saveChartAsPNG(new File("../results-graph.png"), chart, 700, 500);
      } catch (IOException e) {
        System.out.println("Unable to save graph image to disk");
      }
    }
    return new ChartPanel(chart);
  }

  public static void main(String[] args) throws Exception {
    File datafile = new File(args[0]);
    boolean save = (args.length == 2 && args[1] != null && args[1].equals("save"));
    PerformanceMeasurementMarshaller.Statistics stats = PerformanceMeasurementMarshaller.deMarshall(datafile);
    ResultsGraph graph = new ResultsGraph(stats, save);
    if (save) System.exit(0);
    graph.pack();
    RefineryUtilities.centerFrameOnScreen(graph);
    graph.setVisible(true);
  }
}