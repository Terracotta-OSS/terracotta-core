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
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.dial.DialBackground;
import org.jfree.chart.plot.dial.DialCap;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialTextAnnotation;
import org.jfree.chart.plot.dial.DialValueIndicator;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialRange;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.RangeType;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.ValueDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class DemoChartFactory {
  public static final TickUnitSource DEFAULT_TICKS         = createStandardTickUnits();
  public static final TickUnitSource DEFAULT_MEMORY_TICKS  = createMemoryTickUnits();
  public static final TickUnitSource DEFAULT_INTEGER_TICKS = createIntegerTickUnits();
  public static final ResourceBundle bundle                = ResourceBundle
                                                               .getBundle("com.tc.admin.common.CommonBundle");
  public static final Font           regularFont           = (Font) bundle.getObject("chart.regular.font");

  static {
    StandardChartTheme theme = new StandardChartTheme("Terracotta") {
      @Override
      public void apply(JFreeChart chart) {
        super.apply(chart);
        chart.setBackgroundPaint(null);
      }

      @Override
      public void applyToValueAxis(ValueAxis axis) {
        super.applyToValueAxis(axis);
        axis.setTickLabelFont(regularFont);
        axis.setLabelFont(regularFont);
      }
    };
    DefaultDrawingSupplier drawingSupplier = new DefaultDrawingSupplier(
                                                                        DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
                                                                        DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
                                                                        new Stroke[] { new BasicStroke(1.3f) },
                                                                        new Stroke[] { new BasicStroke(0.5f) },
                                                                        DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
    theme.setDrawingSupplier(drawingSupplier);
    theme.setPlotBackgroundPaint(Color.white);
    theme.setDomainGridlinePaint(Color.lightGray);
    theme.setRangeGridlinePaint(Color.lightGray);
    theme.setAxisOffset(new RectangleInsets());
    theme.setRegularFont(regularFont);
    theme.setLargeFont((Font) bundle.getObject("chart.large.font"));
    theme.setExtraLargeFont((Font) bundle.getObject("chart.extra-large.font"));
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

  public static JFreeChart getXYBarChart(String header, String xLabel, String yLabel, TimeSeries[] timeseries,
                                         boolean legend) {
    IntervalXYDataset dataset = createTimeSeriesDataset(timeseries);
    return createXYBarChart(header, xLabel, yLabel, dataset, legend);
  }

  private static JFreeChart createXYBarChart(String header, String xLabel, String yLabel, IntervalXYDataset dataset,
                                             boolean legend) {
    JFreeChart chart = ChartFactory.createXYBarChart(header, xLabel, true, yLabel, dataset, PlotOrientation.VERTICAL,
                                                     legend, true, false);

    XYPlot plot = (XYPlot) chart.getPlot();
    ((XYBarRenderer) plot.getRenderer()).setDrawBarOutline(false);

    DateAxis axis = (DateAxis) plot.getDomainAxis();
    axis.setFixedAutoRange(30000.0);
    axis.setDateFormatOverride(new SimpleDateFormat("kk:mm:ss"));
    axis.setTickLabelFont(regularFont);
    axis.setLabelFont(regularFont);

    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setStandardTickUnits(DEFAULT_TICKS);
    numberAxis.setRangeType(RangeType.POSITIVE);
    numberAxis.setAutoRangeMinimumSize(10.0);
    numberAxis.setTickLabelFont(regularFont);
    numberAxis.setLabelFont(regularFont);

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
    axis.setTickLabelFont(regularFont);
    axis.setLabelFont(regularFont);
    if (axis instanceof DateAxis) {
      ((DateAxis) axis).setDateFormatOverride(new SimpleDateFormat("h:mm:ss"));
    }

    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setRangeType(RangeType.POSITIVE);
    numberAxis.setStandardTickUnits(DEFAULT_TICKS);
    numberAxis.setAutoRangeMinimumSize(10.0);
    numberAxis.setTickLabelFont(regularFont);
    numberAxis.setLabelFont(regularFont);

    return chart;
  }

  public static JFreeChart createStackedXYAreaChart(String header, String xLabel, String yLabel,
                                                    TableXYDataset dataset, PlotOrientation orientation,
                                                    boolean createLegend) {
    JFreeChart chart = createStackedXYAreaChart(header, xLabel, yLabel, dataset, orientation, createLegend, true, false);

    XYPlot plot = (XYPlot) chart.getPlot();

    ValueAxis axis = plot.getDomainAxis();
    axis.setFixedAutoRange(30000.0);
    axis.setTickLabelFont(regularFont);
    axis.setLabelFont(regularFont);
    if (axis instanceof DateAxis) {
      ((DateAxis) axis).setDateFormatOverride(new SimpleDateFormat("h:mm:ss"));
    }

    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setRangeType(RangeType.POSITIVE);
    numberAxis.setStandardTickUnits(DEFAULT_TICKS);
    numberAxis.setAutoRangeMinimumSize(10.0);
    numberAxis.setTickLabelFont(regularFont);
    numberAxis.setLabelFont(regularFont);

    return chart;
  }

  public static JFreeChart createStackedXYAreaChart(String title, String xAxisLabel, String yAxisLabel,
                                                    TableXYDataset dataset, PlotOrientation orientation,
                                                    boolean legend, boolean tooltips, boolean urls) {

    if (orientation == null) { throw new IllegalArgumentException("Null 'orientation' argument."); }
    DateAxis xAxis = new DateAxis(xAxisLabel);
    xAxis.setLowerMargin(0.02);
    xAxis.setUpperMargin(0.02);
    NumberAxis yAxis = new NumberAxis(yAxisLabel);
    XYToolTipGenerator toolTipGenerator = null;
    if (tooltips) {
      toolTipGenerator = new StandardXYToolTipGenerator();
    }

    XYURLGenerator urlGenerator = null;
    if (urls) {
      urlGenerator = new StandardXYURLGenerator();
    }
    StackedXYAreaRenderer2 renderer = new StackedXYAreaRenderer2(toolTipGenerator, urlGenerator);
    renderer.setOutline(true);
    XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
    plot.setOrientation(orientation);

    plot.setRangeAxis(yAxis); // forces recalculation of the axis range

    JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    ChartFactory.getChartTheme().apply(chart);
    return chart;

  }

  public static JFreeChart getXYStepChart(String header, String xLabel, String yLabel, TimeSeries ts, boolean legend) {
    XYDataset dataset = createTimeSeriesDataset(ts);
    return createXYStepChart(header, xLabel, yLabel, dataset, legend);
  }

  public static JFreeChart getXYStepChart(String header, String xLabel, String yLabel, TimeSeries[] timeseries,
                                          boolean legend) {
    XYDataset dataset = createTimeSeriesDataset(timeseries);
    return createXYStepChart(header, xLabel, yLabel, dataset, legend);
  }

  private static JFreeChart createXYStepChart(String header, String xLabel, String yLabel, XYDataset dataset,
                                              boolean legend) {
    JFreeChart chart = ChartFactory.createXYStepChart(header, xLabel, yLabel, dataset, PlotOrientation.VERTICAL,
                                                      legend, true, false);

    XYPlot plot = (XYPlot) chart.getPlot();

    ValueAxis axis = plot.getDomainAxis();
    axis.setFixedAutoRange(30000.0);
    axis.setTickLabelFont(regularFont);
    axis.setLabelFont(regularFont);
    if (axis instanceof DateAxis) {
      ((DateAxis) axis).setDateFormatOverride(new SimpleDateFormat("h:mm:ss"));
    }

    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setRangeType(RangeType.POSITIVE);
    numberAxis.setStandardTickUnits(DEFAULT_TICKS);
    numberAxis.setAutoRangeMinimumSize(10.0);
    numberAxis.setTickLabelFont(regularFont);
    numberAxis.setLabelFont(regularFont);

    return chart;
  }

  public static StandardDialScale createStandardDialScale(double lowerBound, double upperBound, double startAngle,
                                                          double extent, double majorTickInterval, int minorTickCount) {
    StandardDialScale scale = new StandardDialScale(lowerBound, upperBound, startAngle, extent, majorTickInterval,
                                                    minorTickCount);
    scale.setTickRadius(0.88);
    scale.setTickLabelOffset(0.20);
    scale.setTickLabelFont((Font) bundle.getObject("dial.tick.label.font"));
    scale.setTickLabelFormatter(new DecimalFormat("#,###"));
    return scale;
  }

  public static JFreeChart createDial(String dialLabel, ValueDataset dataset, double lowerBound, double upperBound) {
    return createDial(dialLabel, 12, dataset, lowerBound, upperBound, new StandardDialRange[] {});
  }

  public static JFreeChart createDial(String dialLabel, int baseFont, ValueDataset dataset, double lowerBound,
                                      double upperBound) {
    return createDial(dialLabel, baseFont, dataset, lowerBound, upperBound, new StandardDialRange[] {});
  }

  public static JFreeChart createDial(String dialLabel, ValueDataset dataset, double lowerBound, double upperBound,
                                      StandardDialRange[] ranges) {
    return createDial(dialLabel, 12, dataset, lowerBound, upperBound, ranges);
  }

  public static JFreeChart createDial(String dialLabel, int baseFont, ValueDataset dataset, double lowerBound,
                                      double upperBound, StandardDialRange[] ranges) {
    StandardDialScale scale = createStandardDialScale(lowerBound, upperBound, -140, -260, 1000, 4);
    return createDial(dialLabel, baseFont, dataset, scale, ranges, new Color(240, 0, 0), new Color(218, 0, 0));
  }

  public static JFreeChart createDial(String dialLabel, ValueDataset dataset, StandardDialScale scale,
                                      StandardDialRange[] ranges) {
    return createDial(dialLabel, 12, dataset, scale, ranges, new Color(240, 0, 0), new Color(218, 0, 0));
  }

  public static JFreeChart createDial(String dialLabel, ValueDataset dataset, StandardDialScale scale,
                                      StandardDialRange[] ranges, Paint pointerFillPaint, Paint pointerOutlinePaint) {
    return createDial(dialLabel, 12, dataset, scale, ranges, pointerFillPaint, pointerOutlinePaint);
  }

  public static JFreeChart createDial(String dialLabel, int baseFont, ValueDataset dataset, StandardDialScale scale,
                                      StandardDialRange[] ranges, Paint pointerFillPaint, Paint pointerOutlinePaint) {
    DialPlot plot = new DialPlot();
    plot.setView(0.0, 0.0, 1.0, 1.0);
    plot.setDataset(dataset);
    plot.setDialFrame(new StandardDialFrame());

    // set the text annotation
    DialTextAnnotation annotation1 = new DialTextAnnotation(dialLabel);
    annotation1.setFont(new Font("DialogInput", Font.BOLD, baseFont + 4));
    annotation1.setRadius(0.17);
    plot.addLayer(annotation1);

    // set the value indicator
    DialValueIndicator dvi = new DialValueIndicator(0);
    dvi.setNumberFormat(new DecimalFormat("#"));
    dvi.setFont(new Font("Monospaced", Font.BOLD, baseFont + 4));
    dvi.setRadius(0.68);
    dvi.setOutlinePaint(Color.black);
    dvi.setOutlineStroke(new BasicStroke(2.0f));
    if (scale != null) {
      dvi.setTemplateValue(Double.valueOf(scale.getUpperBound()));
    }
    dvi.setInsets(new RectangleInsets(5, 20, 5, 20));
    plot.addLayer(dvi);

    if (scale != null) {
      plot.addScale(0, scale);
    }

    // set the needle
    DialPointer.Pointer p = new DialPointer.Pointer();
    p.setFillPaint(pointerFillPaint);
    p.setOutlinePaint(pointerOutlinePaint);
    plot.addPointer(p);

    // set the needle cap
    DialCap cap = new DialCap();
    cap.setFillPaint(Color.black);
    plot.setCap(cap);

    plot.setBackground(new DialBackground(Color.white));

    for (StandardDialRange range : ranges) {
      range.setInnerRadius(0.90);
      range.setOuterRadius(0.95);
      plot.addLayer(range);
    }

    return new JFreeChart(plot);
  }

  public static TimeSeriesCollection createTimeSeriesDataset(TimeSeries s1) {
    return createTimeSeriesDataset(new TimeSeries[] { s1 });
  }

  public static TimeSeriesCollection createTimeSeriesDataset(TimeSeries[] series) {
    TimeSeriesCollection dataset = new TimeSeriesCollection();

    for (TimeSeries serie : series) {
      dataset.addSeries(serie);
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
    DecimalFormat tdf = ThinDecimalFormat.INSTANCE;

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
    units.add(new NumberTickUnit(1000, tdf));
    units.add(new NumberTickUnit(10000, tdf));
    units.add(new NumberTickUnit(100000, tdf));
    units.add(new NumberTickUnit(1000000, tdf));
    units.add(new NumberTickUnit(10000000, tdf));
    units.add(new NumberTickUnit(100000000, tdf));
    units.add(new NumberTickUnit(1000000000, tdf));
    units.add(new NumberTickUnit(10000000000.0, tdf));
    units.add(new NumberTickUnit(100000000000.0, tdf));

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
    units.add(new NumberTickUnit(2500, tdf));
    units.add(new NumberTickUnit(25000, tdf));
    units.add(new NumberTickUnit(250000, tdf));
    units.add(new NumberTickUnit(2500000, tdf));
    units.add(new NumberTickUnit(25000000, tdf));
    units.add(new NumberTickUnit(250000000, tdf));
    units.add(new NumberTickUnit(2500000000.0, tdf));
    units.add(new NumberTickUnit(25000000000.0, tdf));
    units.add(new NumberTickUnit(250000000000.0, tdf));

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
    units.add(new NumberTickUnit(5000L, tdf));
    units.add(new NumberTickUnit(50000L, tdf));
    units.add(new NumberTickUnit(500000L, tdf));
    units.add(new NumberTickUnit(5000000L, tdf));
    units.add(new NumberTickUnit(50000000L, tdf));
    units.add(new NumberTickUnit(500000000L, tdf));
    units.add(new NumberTickUnit(5000000000L, tdf));
    units.add(new NumberTickUnit(50000000000L, tdf));
    units.add(new NumberTickUnit(500000000000L, tdf));

    return units;
  }

  public static TickUnitSource createMemoryTickUnits() {
    TickUnits units = new TickUnits();
    DecimalFormat tmf = new ThinMemoryFormat("0");

    long[] bases = { ThinMemoryFormat.BYTE, ThinMemoryFormat.KBYTE, ThinMemoryFormat.MBYTE, ThinMemoryFormat.GBYTE,
        ThinMemoryFormat.TBYTE };

    for (long base : bases) {
      // units.add(new NumberTickUnit(1 * base, tmf));
      // units.add(new NumberTickUnit(8 * base, tmf));
      // units.add(new NumberTickUnit(16 * base, tmf));
      // units.add(new NumberTickUnit(32 * base, tmf));
      // units.add(new NumberTickUnit(64 * base, tmf));
      // units.add(new NumberTickUnit(128 * base, tmf));
      // units.add(new NumberTickUnit(256 * base, tmf));
      // units.add(new NumberTickUnit(512 * base, tmf));

      units.add(new NumberTickUnit(1 * base, tmf));
      units.add(new NumberTickUnit(2 * base, tmf));
      units.add(new NumberTickUnit(5 * base, tmf));
      units.add(new NumberTickUnit(10 * base, tmf));
      units.add(new NumberTickUnit(20 * base, tmf));
      units.add(new NumberTickUnit(50 * base, tmf));
      units.add(new NumberTickUnit(100 * base, tmf));
      units.add(new NumberTickUnit(200 * base, tmf));
      units.add(new NumberTickUnit(500 * base, tmf));
    }

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
    units.add(new NumberTickUnit(10000, tdf));
    units.add(new NumberTickUnit(20000, tdf));
    units.add(new NumberTickUnit(50000, tdf));
    units.add(new NumberTickUnit(100000, tdf));
    units.add(new NumberTickUnit(200000, tdf));
    units.add(new NumberTickUnit(500000, tdf));
    units.add(new NumberTickUnit(1000000, tdf));
    units.add(new NumberTickUnit(2000000, tdf));
    units.add(new NumberTickUnit(5000000, tdf));
    units.add(new NumberTickUnit(10000000, tdf));
    units.add(new NumberTickUnit(20000000, tdf));
    units.add(new NumberTickUnit(50000000, tdf));
    units.add(new NumberTickUnit(100000000, tdf));
    units.add(new NumberTickUnit(200000000, tdf));
    units.add(new NumberTickUnit(500000000, tdf));
    units.add(new NumberTickUnit(1000000000, tdf));
    units.add(new NumberTickUnit(2000000000, tdf));
    units.add(new NumberTickUnit(5000000000.0, tdf));
    units.add(new NumberTickUnit(10000000000.0, tdf));

    return units;
  }
}
