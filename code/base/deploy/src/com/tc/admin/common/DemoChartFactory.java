/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.RangeType;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class DemoChartFactory {
  public static final TickUnitSource DEFAULT_TICKS = createStandardTickUnits();
  public static final TickUnitSource DEFAULT_INTEGER_TICKS = createIntegerTickUnits();
  
  static {
    // ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
    // ChartFactory.setChartTheme(StandardChartTheme.createDarknessTheme());

    StandardChartTheme theme = new StandardChartTheme("Terracotta") {
      public void apply(JFreeChart chart) {
        super.apply(chart);
        chart.setBackgroundPaint(null);
      }
    };
    DefaultDrawingSupplier drawingSupplier = new DefaultDrawingSupplier(
                                                                        DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
                                                                        DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
                                                                        new Stroke[] { new BasicStroke(2.0f) },
                                                                        new Stroke[] { new BasicStroke(0.5f) },
                                                                        DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
    theme.setDrawingSupplier(drawingSupplier);
    theme.setPlotBackgroundPaint(Color.white);
    theme.setDomainGridlinePaint(Color.lightGray);
    theme.setRangeGridlinePaint(Color.lightGray);
    theme.setAxisOffset(new RectangleInsets());
    ChartFactory.setChartTheme(theme);
  }

  public static JFreeChart getXYBarChart(CategoryDataset catagoryDataset, PlotOrientation plotOrientation) {
    return ChartFactory.createBarChart("", "", "", catagoryDataset, plotOrientation, false, true, false);
  }

  public static JFreeChart getXYBarChart(String header, String xLabel, String yLabel, TimeSeries ts) {
    IntervalXYDataset dataset = createTimeSeriesDataset(ts);
    return createXYBarChart(header, xLabel, yLabel, dataset, false);
  }

  public static JFreeChart getXYBarChart(String header, String xLabel, String yLabel, TimeSeries[] timeseries) {
    IntervalXYDataset dataset = createTimeSeriesDataset(timeseries);
    return createXYBarChart(header, xLabel, yLabel, dataset, true);
  }

  private static JFreeChart createXYBarChart(String header, String xLabel, String yLabel, IntervalXYDataset dataset,
                                             boolean legend) {
    JFreeChart chart = ChartFactory.createXYBarChart(header, xLabel, true, yLabel, dataset, PlotOrientation.VERTICAL,
                                                     legend, true, false);

    XYPlot plot = (XYPlot) chart.getPlot();

    DateAxis axis = (DateAxis) plot.getDomainAxis();
    axis.setFixedAutoRange(30000.0);
    axis.setDateFormatOverride(new SimpleDateFormat("kk:mm:ss"));

    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setStandardTickUnits(DEFAULT_TICKS);
    numberAxis.setRangeType(RangeType.POSITIVE);
    numberAxis.setAutoRangeMinimumSize(50.0);

    return chart;
  }

  public static JFreeChart getXYLineChart(String header, String xLabel, String yLabel, TimeSeries ts,
                                          boolean createLegend) {
    return getXYLineChart(header, xLabel, yLabel, new TimeSeries[] { ts }, createLegend);
  }

  public static JFreeChart getXYLineChart(String header, String xLabel, String yLabel, TimeSeries[] timeseries,
                                          boolean createLegend) {
    XYDataset dataset = createTimeSeriesDataset(timeseries);
    return createXYLineChart(header, xLabel, yLabel, dataset, createLegend);
  }

  public static JFreeChart createXYLineChart(String header, String xLabel, String yLabel, XYDataset dataset,
                                             boolean createLegend) {
    JFreeChart chart = ChartFactory.createTimeSeriesChart(header, xLabel, yLabel, dataset, createLegend, true, false);

    XYPlot plot = (XYPlot) chart.getPlot();

    ValueAxis axis = plot.getDomainAxis();
    axis.setFixedAutoRange(30000.0);
    if (axis instanceof DateAxis) {
      ((DateAxis) axis).setDateFormatOverride(new SimpleDateFormat("h:mm:ss"));
    }

    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setRangeType(RangeType.POSITIVE);
    numberAxis.setStandardTickUnits(DEFAULT_TICKS);
    numberAxis.setAutoRangeMinimumSize(50.0);

    return chart;
  }

  public static JFreeChart getXYStepChart(String header, String xLabel, String yLabel, TimeSeries ts) {
    XYDataset dataset = createTimeSeriesDataset(ts);
    return createXYStepChart(header, xLabel, yLabel, dataset, false);
  }

  public static JFreeChart getXYStepChart(String header, String xLabel, String yLabel, TimeSeries[] timeseries) {
    XYDataset dataset = createTimeSeriesDataset(timeseries);
    return createXYStepChart(header, xLabel, yLabel, dataset, true);
  }

  private static JFreeChart createXYStepChart(String header, String xLabel, String yLabel, XYDataset dataset,
                                              boolean legend) {
    JFreeChart chart = ChartFactory.createXYStepChart(header, xLabel, yLabel, dataset, PlotOrientation.VERTICAL,
                                                      legend, true, false);

    XYPlot plot = (XYPlot) chart.getPlot();

    ValueAxis axis = plot.getDomainAxis();
    axis.setFixedAutoRange(30000.0);
    if (axis instanceof DateAxis) {
      ((DateAxis) axis).setDateFormatOverride(new SimpleDateFormat("h:mm:ss"));
    }

    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setRangeType(RangeType.POSITIVE);
    numberAxis.setStandardTickUnits(DEFAULT_TICKS);
    numberAxis.setAutoRangeMinimumSize(50.0);

    return chart;
  }

  private static TimeSeriesCollection createTimeSeriesDataset(TimeSeries s1) {
    return createTimeSeriesDataset(new TimeSeries[] { s1 });
  }

  private static TimeSeriesCollection createTimeSeriesDataset(TimeSeries[] series) {
    TimeSeriesCollection dataset = new TimeSeriesCollection();

    for (int i = 0; i < series.length; i++) {
      dataset.addSeries(series[i]);
    }

    dataset.setDomainIsPointsInTime(true);

    return dataset;
  }

  public static TickUnitSource createStandardTickUnits() {
    TickUnits units = new TickUnits();
    DecimalFormat df0 = new DecimalFormat("0.00000000");
    DecimalFormat df1 = new DecimalFormat("0.0000000");
    DecimalFormat df2 = new DecimalFormat("0.000000");
    DecimalFormat df3 = new DecimalFormat("0.00000");
    DecimalFormat df4 = new DecimalFormat("0.0000");
    DecimalFormat df5 = new DecimalFormat("0.000");
    DecimalFormat df6 = new DecimalFormat("0.00");
    DecimalFormat df7 = new DecimalFormat("0.0");
    DecimalFormat df8 = new DecimalFormat("#,##0");
    // DecimalFormat df9 = new DecimalFormat("#,###,##0");
    // DecimalFormat df10 = new DecimalFormat("#,###,###,##0");
    DecimalFormat tdf = new ThinDecimalFormat();

    // we can add the units in any order, the TickUnits collection will
    // sort them...
    units.add(new NumberTickUnit(0.0000001, df1));
    units.add(new NumberTickUnit(0.000001, df2));
    units.add(new NumberTickUnit(0.00001, df3));
    units.add(new NumberTickUnit(0.0001, df4));
    units.add(new NumberTickUnit(0.001, df5));
    units.add(new NumberTickUnit(0.01, df6));
    units.add(new NumberTickUnit(0.1, df7));
    units.add(new NumberTickUnit(1, df8));
    units.add(new NumberTickUnit(10, df8));
    units.add(new NumberTickUnit(100, df8));
    units.add(new NumberTickUnit(1000, /* df8 */tdf));
    units.add(new NumberTickUnit(10000, /* df8 */tdf));
    units.add(new NumberTickUnit(100000, /* df8 */tdf));
    units.add(new NumberTickUnit(1000000, /* df9 */tdf));
    units.add(new NumberTickUnit(10000000, /* df9 */tdf));
    units.add(new NumberTickUnit(100000000, /* df9 */tdf));
    units.add(new NumberTickUnit(1000000000, /* df10 */tdf));
    units.add(new NumberTickUnit(10000000000.0, /* df10 */tdf));
    units.add(new NumberTickUnit(100000000000.0, /* df10 */tdf));

    units.add(new NumberTickUnit(0.00000025, df0));
    units.add(new NumberTickUnit(0.0000025, df1));
    units.add(new NumberTickUnit(0.000025, df2));
    units.add(new NumberTickUnit(0.00025, df3));
    units.add(new NumberTickUnit(0.0025, df4));
    units.add(new NumberTickUnit(0.025, df5));
    units.add(new NumberTickUnit(0.25, df6));
    units.add(new NumberTickUnit(2.5, df7));
    units.add(new NumberTickUnit(25, df8));
    units.add(new NumberTickUnit(250, df8));
    units.add(new NumberTickUnit(2500, /* df8 */tdf));
    units.add(new NumberTickUnit(25000, /* df8 */tdf));
    units.add(new NumberTickUnit(250000, /* df8 */tdf));
    units.add(new NumberTickUnit(2500000, /* df9 */tdf));
    units.add(new NumberTickUnit(25000000, /* df9 */tdf));
    units.add(new NumberTickUnit(250000000, /* df9 */tdf));
    units.add(new NumberTickUnit(2500000000.0, /* df10 */tdf));
    units.add(new NumberTickUnit(25000000000.0, /* df10 */tdf));
    units.add(new NumberTickUnit(250000000000.0, /* df10 */tdf));

    units.add(new NumberTickUnit(0.0000005, df1));
    units.add(new NumberTickUnit(0.000005, df2));
    units.add(new NumberTickUnit(0.00005, df3));
    units.add(new NumberTickUnit(0.0005, df4));
    units.add(new NumberTickUnit(0.005, df5));
    units.add(new NumberTickUnit(0.05, df6));
    units.add(new NumberTickUnit(0.5, df7));
    units.add(new NumberTickUnit(5L, df8));
    units.add(new NumberTickUnit(50L, df8));
    units.add(new NumberTickUnit(500L, df8));
    units.add(new NumberTickUnit(5000L, /* df8 */tdf));
    units.add(new NumberTickUnit(50000L, /* df8 */tdf));
    units.add(new NumberTickUnit(500000L, /* df8 */tdf));
    units.add(new NumberTickUnit(5000000L, /* df9 */tdf));
    units.add(new NumberTickUnit(50000000L, /* df9 */tdf));
    units.add(new NumberTickUnit(500000000L, /* df9 */tdf));
    units.add(new NumberTickUnit(5000000000L, /* df10 */tdf));
    units.add(new NumberTickUnit(50000000000L, /* df10 */tdf));
    units.add(new NumberTickUnit(500000000000L, /* df10 */tdf));

    return units;
  }

  public static TickUnitSource createIntegerTickUnits() {
    TickUnits units = new TickUnits();
    DecimalFormat df0 = new DecimalFormat("0");
    DecimalFormat df1 = new DecimalFormat("#,##0");
    DecimalFormat tdf = new ThinDecimalFormat();

    units.add(new NumberTickUnit(1, df0));
    units.add(new NumberTickUnit(2, df0));
    units.add(new NumberTickUnit(5, df0));
    units.add(new NumberTickUnit(10, df0));
    units.add(new NumberTickUnit(20, df0));
    units.add(new NumberTickUnit(50, df0));
    units.add(new NumberTickUnit(100, df0));
    units.add(new NumberTickUnit(200, df0));
    units.add(new NumberTickUnit(500, df0));
    units.add(new NumberTickUnit(1000, df1));
    units.add(new NumberTickUnit(2000, df1));
    units.add(new NumberTickUnit(5000, df1));
    units.add(new NumberTickUnit(10000, /* df1 */tdf));
    units.add(new NumberTickUnit(20000, /* df1 */tdf));
    units.add(new NumberTickUnit(50000, /* df1 */tdf));
    units.add(new NumberTickUnit(100000, /* df1 */tdf));
    units.add(new NumberTickUnit(200000, /* df1 */tdf));
    units.add(new NumberTickUnit(500000, /* df1 */tdf));
    units.add(new NumberTickUnit(1000000, /* df1 */tdf));
    units.add(new NumberTickUnit(2000000, /* df1 */tdf));
    units.add(new NumberTickUnit(5000000, /* df1 */tdf));
    units.add(new NumberTickUnit(10000000, /* df1 */tdf));
    units.add(new NumberTickUnit(20000000, /* df1 */tdf));
    units.add(new NumberTickUnit(50000000, /* df1 */tdf));
    units.add(new NumberTickUnit(100000000, /* df1 */tdf));
    units.add(new NumberTickUnit(200000000, /* df1 */tdf));
    units.add(new NumberTickUnit(500000000, /* df1 */tdf));
    units.add(new NumberTickUnit(1000000000, /* df1 */tdf));
    units.add(new NumberTickUnit(2000000000, /* df1 */tdf));
    units.add(new NumberTickUnit(5000000000.0, /* df1 */tdf));
    units.add(new NumberTickUnit(10000000000.0, /* df1 */tdf));

    return units;
  }

}
