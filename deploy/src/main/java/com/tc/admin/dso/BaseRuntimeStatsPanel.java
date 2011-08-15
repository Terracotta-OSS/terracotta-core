/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicChartPanel;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.FixedTimeSeriesCollection;
import com.tc.admin.common.FixedTimeTableXYDataset;
import com.tc.admin.common.LinkButton;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.SyncHTMLEditorKit;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTextPane;
import com.tc.admin.model.IClusterNode;
import com.tc.admin.model.PolledAttributeListener;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.admin.options.RuntimeStatsOption;
import com.tc.management.RuntimeStatisticConstants;
import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;
import com.tc.util.Conversion;
import com.tc.util.Conversion.MemorySizeUnits;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

public class BaseRuntimeStatsPanel extends XContainer implements RuntimeStatisticConstants, PolledAttributeListener,
    PreferenceChangeListener, HierarchyListener {
  protected final ApplicationContext    appContext;
  protected final ArrayList<TimeSeries> allSeries;
  protected final ArrayList<JFreeChart> allCharts;

  protected XContainer                  chartsPanel;
  private XButton                       manageMonitoringButton;
  private XButton                       clearSamplesButton;
  private JButton                       configureOptionsButton;
  protected AxisSpace                   rangeAxisSpace;
  private volatile boolean              autoStart;
  private volatile boolean              hasAutoStarted;
  private volatile boolean              isMonitoring;

  protected static final Dimension      fDefaultGraphSize                       = new Dimension(0, 65);

  private static final int              DEFAULT_POLL_PERIOD_SECS                = 3;
  private static final int              DEFAULT_SAMPLE_HISTORY_MINUTES          = 5;

  private static final String           DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY    = "poll-periods-seconds";
  private static final String           DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY = "sample-history-minutes";

  private static final ImageIcon        fStartIcon;
  private static final ImageIcon        fStopIcon;
  private static final ImageIcon        fClearIcon;

  protected static final Font           labelFont                               = new Font("DialogInput", Font.PLAIN,
                                                                                           12);

  static {
    fStartIcon = new ImageIcon(BaseRuntimeStatsPanel.class.getResource("/com/tc/admin/icons/resume_co.gif"));
    fStopIcon = new ImageIcon(BaseRuntimeStatsPanel.class.getResource("/com/tc/admin/icons/suspend_co.gif"));
    fClearIcon = new ImageIcon(BaseRuntimeStatsPanel.class.getResource("/com/tc/admin/icons/clear_co.gif"));
  }

  private static final String[]         MEMORY_USAGE_SERIES_NAMES               = { SRAMemoryUsage.DATA_NAME_MAX,
      SRAMemoryUsage.DATA_NAME_USED                                            };

  private final boolean                 showControls                            = false;

  private static final String           HYPERIC_INSTRUCTIONS_URI                = "/com/tc/admin/HypericInstructions.html";

  public BaseRuntimeStatsPanel(ApplicationContext appContext) {
    super(new BorderLayout());

    this.appContext = appContext;

    allSeries = new ArrayList<TimeSeries>();
    allCharts = new ArrayList<JFreeChart>();
    autoStart = true;

    add(chartsPanel = new XContainer());

    if (showControls) {
      XContainer bottomPanel = new XContainer(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = gbc.gridy = 0;
      gbc.insets = new Insets(3, 3, 3, 3);

      configureOptionsButton = LinkButton.makeLink("Configure Runtime Statistics", new ConfigureOptionsAction());
      bottomPanel.add(configureOptionsButton, gbc);
      gbc.gridx++;

      gbc.weightx = 1.0;
      gbc.anchor = GridBagConstraints.EAST;

      manageMonitoringButton = new XButton();
      manageMonitoringButton.setIcon(fStartIcon);
      manageMonitoringButton.addActionListener(new ManageMonitoringAction());
      bottomPanel.add(manageMonitoringButton, gbc);
      gbc.gridx++;

      gbc.weightx = 0.0;
      gbc.anchor = GridBagConstraints.CENTER;

      clearSamplesButton = new XButton();
      clearSamplesButton.setIcon(fClearIcon);
      clearSamplesButton.addActionListener(new ClearSamplesAction());
      bottomPanel.add(clearSamplesButton, gbc);

      add(bottomPanel, BorderLayout.SOUTH);
    }

    Preferences runtimeStatsPrefs = appContext.getPrefs().node(RuntimeStatsOption.NAME);
    runtimeStatsPrefs.addPreferenceChangeListener(this);

    addHierarchyListener(this);
  }

  public XContainer getChartsPanel() {
    return chartsPanel;
  }

  public static ChartPanel createChartPanel(JFreeChart chart) {
    boolean useBuffer = false;
    boolean properties = false;
    boolean save = false;
    boolean print = false;
    boolean zoom = false;
    boolean tooltips = true;

    BasicChartPanel chartPanel = new BasicChartPanel(chart, ChartPanel.DEFAULT_WIDTH, ChartPanel.DEFAULT_HEIGHT,
        ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH, ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT,
        ChartPanel.DEFAULT_MAXIMUM_DRAW_WIDTH, ChartPanel.DEFAULT_MAXIMUM_DRAW_HEIGHT, useBuffer, properties, save,
        print, zoom, tooltips) {
      @Override
      public String getToolTipText(MouseEvent e) {
        String tip = super.getToolTipText(e);
        return tip != null ? tip : getToolTipText();
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        ToolTipManager.sharedInstance().setEnabled(false);
        ToolTipManager.sharedInstance().setEnabled(true);
        super.mouseMoved(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {/**/
      }

      @Override
      public void mouseExited(MouseEvent e) {/**/
      }
    };
    chartPanel.setRangeZoomable(false);
    chartPanel.setDomainZoomable(false);

    return chartPanel;
  }

  public synchronized void setAutoStart(boolean autoStart) {
    this.autoStart = autoStart;
  }

  public synchronized boolean getAutoStart() {
    return autoStart;
  }

  private int getPollPeriodSeconds() {
    return getIntPref(DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY, DEFAULT_POLL_PERIOD_SECS);
  }

  private int getSampleHistoryMinutes() {
    return getIntPref(DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY, DEFAULT_SAMPLE_HISTORY_MINUTES);
  }

  private int getSampleHistorySeconds() {
    return getSampleHistoryMinutes() * 60;
  }

  private int getSampleHistoryMillis() {
    return getSampleHistorySeconds() * 1000;
  }

  private class ConfigureOptionsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      appContext.getApplicationController().showOption(RuntimeStatsOption.NAME);
    }
  }

  private class ManageMonitoringAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      if (isMonitoringRuntimeStats()) {
        stopMonitoringRuntimeStats();
      } else {
        startMonitoringRuntimeStats();
      }
    }
  }

  private class ClearSamplesAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      clearAllRuntimeStatsSamples();
    }
  }

  protected TimeSeries[] createMemoryUsageSeries() {
    String[] names = MEMORY_USAGE_SERIES_NAMES;
    TimeSeries[] result = new TimeSeries[names.length];
    for (int i = 0; i < names.length; i++) {
      result[i] = createTimeSeries(names[i]);
    }
    return result;
  }

  protected TimeSeries[] createCpusSeries(IClusterNode clusterNode) {
    String[] cpus = clusterNode.getCpuStatNames();
    TimeSeries[] result = new TimeSeries[cpus.length];
    for (int i = 0; i < cpus.length; i++) {
      result[i] = createTimeSeries(cpus[i]);
    }
    return result;
  }

  protected FixedTimeSeriesCollection createCpuUsageDataset(IClusterNode clusterNode) {
    String[] cpus = clusterNode.getCpuStatNames();
    return new FixedTimeSeriesCollection(cpus, getSampleHistoryMillis());
  }

  protected TimeSeries createCpuLoadSeries(IClusterNode clusterNode) {
    String[] cpus = clusterNode.getCpuStatNames();
    return cpus.length > 0 ? createTimeSeries("cpu load") : null;
  }

  protected TimeSeries createTimeSeries(String name) {
    TimeSeries ts = new TimeSeries(name, Second.class);
    ts.setMaximumItemAge(getSampleHistorySeconds());
    allSeries.add(ts);
    return ts;
  }

  protected JFreeChart createChart(TimeSeries series) {
    return createChart(new TimeSeries[] { series });
  }

  protected JFreeChart createChart(TimeSeries series, boolean createLegend) {
    return createChart(new TimeSeries[] { series }, createLegend);
  }

  protected JFreeChart createChart(TimeSeries[] seriesArray) {
    return createChart(seriesArray, true);
  }

  protected int getMaxSampleCount() {
    return (getSampleHistoryMinutes() * 60) / getPollPeriodSeconds();
  }

  protected JFreeChart createChart(XYDataset xyDataset, boolean createLegend) {
    JFreeChart chart = DemoChartFactory.createXYLineChart("", "", "", xyDataset, createLegend);
    int sampleHistoryMillis = getSampleHistoryMillis();

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);

    allCharts.add(chart);
    return chart;
  }

  protected JFreeChart createChart(TimeSeries[] seriesArray, boolean createLegend) {
    JFreeChart chart = DemoChartFactory.getXYLineChart("", "", "", seriesArray, createLegend);

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getDomainAxis().setFixedAutoRange(getSampleHistoryMillis());
    ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);

    int sampleHistorySeconds = getSampleHistorySeconds();
    for (TimeSeries series : seriesArray) {
      series.setMaximumItemAge(sampleHistorySeconds);
    }

    allCharts.add(chart);
    return chart;
  }

  protected JFreeChart createStackedXYAreaChart(FixedTimeTableXYDataset dataset, boolean createLegend) {
    JFreeChart chart = DemoChartFactory.createStackedXYAreaChart("", "", "", dataset, PlotOrientation.VERTICAL,
                                                                 createLegend);
    int sampleHistoryMinutes = getSampleHistoryMinutes();
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);

    dataset.setMaximumItemAge(sampleHistoryMillis);

    allCharts.add(chart);
    return chart;
  }

  protected JFreeChart createXYBarChart(TimeSeries ts) {
    return createXYBarChart(new TimeSeries[] { ts }, false);
  }

  protected JFreeChart createXYBarChart(TimeSeries[] seriesArray, boolean createLegend) {
    JFreeChart chart = DemoChartFactory.getXYBarChart("", "", "", seriesArray, createLegend);

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getDomainAxis().setFixedAutoRange(getSampleHistoryMillis());
    ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);

    int sampleHistorySeconds = getSampleHistorySeconds();
    for (TimeSeries series : seriesArray) {
      series.setMaximumItemAge(sampleHistorySeconds);
    }

    allCharts.add(chart);
    return chart;
  }

  protected JFreeChart createXYStepChart(TimeSeries ts) {
    return createXYStepChart(new TimeSeries[] { ts }, false);
  }

  protected JFreeChart createXYStepChart(TimeSeries[] ts) {
    return createXYStepChart(ts, true);
  }

  protected JFreeChart createXYStepChart(TimeSeries[] seriesArray, boolean createLegend) {
    JFreeChart chart = DemoChartFactory.getXYStepChart("", "", "", seriesArray, createLegend);

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getDomainAxis().setFixedAutoRange(getSampleHistoryMillis());
    ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);

    int sampleHistorySeconds = getSampleHistorySeconds();
    for (TimeSeries series : seriesArray) {
      series.setMaximumItemAge(sampleHistorySeconds);
    }

    allCharts.add(chart);
    return chart;
  }

  protected static XLabel createOverlayLabel() {
    XLabel result = new XLabel();
    result.setFont(labelFont);
    result.setHorizontalAlignment(SwingConstants.CENTER);
    result.setForeground(Color.gray);
    return result;
  }

  protected static StatusView createStatusLabel(Color color) {
    StatusView result = new StatusView();
    XLabel label = result.getLabel();
    label.setFont(labelFont);
    label.setForeground(Color.gray);
    result.setIndicator(color);
    result.setOpaque(false);
    return result;
  }

  public void hierarchyChanged(HierarchyEvent e) {
    long flags = e.getChangeFlags();
    if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (isShowing() && !isMonitoringRuntimeStats() && getAutoStart() && !hasAutoStarted) {
        hasAutoStarted = true;
        startMonitoringRuntimeStats();
      }
    }
  }

  protected void updateFixedRangeAxisSpace(XContainer theChartsPanel) {
    double fixedRangeAxisSpace = 0;
    List<XYPlot> plotList = new ArrayList<XYPlot>();
    java.awt.Component[] chartPanels = theChartsPanel.getComponents();
    for (java.awt.Component comp : chartPanels) {
      if (!(comp instanceof ChartPanel)) {
        continue;
      }
      ChartPanel chartPanel = (ChartPanel) comp;
      JFreeChart chart = chartPanel.getChart();
      if (chart == null) {
        continue;
      }
      Plot plot = chart.getPlot();
      if (plot instanceof XYPlot) {
        XYPlot xyPlot = ((XYPlot) chart.getPlot());
        plotList.add(xyPlot);
        if (xyPlot.getRangeAxis().isVisible()) {
          double rangeAxisTickWidth = getRangeAxisTickWidth(chartPanel.getGraphics(), xyPlot);
          fixedRangeAxisSpace = Math.max(fixedRangeAxisSpace, rangeAxisTickWidth);
        }
      }
    }

    rangeAxisSpace = new AxisSpace();
    rangeAxisSpace.setLeft(fixedRangeAxisSpace);
    // rangeAxisSpace.setRight(fixedRangeAxisSpace);

    if (plotList.size() > 0) {
      Iterator<XYPlot> plotIter = plotList.iterator();
      while (plotIter.hasNext()) {
        XYPlot plot = plotIter.next();
        plot.setFixedRangeAxisSpace(rangeAxisSpace);
      }
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    updateFixedRangeAxisSpace(chartsPanel);
  }

  private double getRangeAxisTickWidth(Graphics graphics, XYPlot plot) {
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    RectangleInsets tickLabelInsets = numberAxis.getTickLabelInsets();
    // NumberTickUnit unit = numberAxis.getTickUnit();
    double upper = 500000000000d;
    NumberTickUnit unit = (NumberTickUnit) DemoChartFactory.DEFAULT_INTEGER_TICKS.getCeilingTickUnit(upper);

    // look at lower and upper bounds...
    FontMetrics fm = graphics.getFontMetrics(numberAxis.getTickLabelFont());
    Range range = numberAxis.getRange();
    double lower = range.getLowerBound();
    // double upper = range.getUpperBound();
    String lowerStr = "";
    String upperStr = "";
    NumberFormat formatter = numberAxis.getNumberFormatOverride();
    if (formatter != null) {
      lowerStr = formatter.format(lower);
      upperStr = formatter.format(upper);
    } else {
      lowerStr = unit.valueToString(lower);
      upperStr = unit.valueToString(upper);
    }
    double w1 = fm.stringWidth(lowerStr);
    double w2 = fm.stringWidth(upperStr);
    return Math.max(w1, w2) + tickLabelInsets.getLeft() + tickLabelInsets.getRight();
  }

  protected void setup(XContainer runtimeStatsPanel) {
    /* override this */
  }

  protected boolean isMonitoringRuntimeStats() {
    return isMonitoring;
  }

  public void startMonitoringRuntimeStats() {
    clearAllRuntimeStatsSamples();
    isMonitoring = true;
    if (showControls) {
      manageMonitoringButton.setIcon(fStopIcon);
    }
  }

  public void stopMonitoringRuntimeStats() {
    hasAutoStarted = isMonitoring = false;
    if (showControls) {
      manageMonitoringButton.setIcon(fStartIcon);
    }
  }

  private void clearAllRuntimeStatsSamples() {
    if (allSeries != null) {
      for (TimeSeries ts : allSeries.toArray(new TimeSeries[0])) {
        ts.clear();
      }
    }
  }

  private int getIntPref(String key, int defaultValue) {
    return appContext.getPrefs().node(RuntimeStatsOption.NAME).getInt(key, defaultValue);
  }

  protected void setMaximumItemAge(int maxItemAgeSeconds) {
    Iterator<TimeSeries> seriesIter = allSeries.iterator();
    while (seriesIter.hasNext()) {
      seriesIter.next().setMaximumItemAge(maxItemAgeSeconds);
    }
  }

  protected void setRuntimeStatsPollPeriodSeconds(int seconds) {
    /* ClusterNode handles messaging the clusterModel */
  }

  protected void setRuntimeStatsSampleHistoryMinutes(int sampleHistoryMinutes) {
    int sampleHistorySeconds = sampleHistoryMinutes * 60;
    int sampleHistoryMillis = sampleHistorySeconds * 1000;

    setMaximumItemAge(sampleHistorySeconds);

    Iterator<JFreeChart> chartIter = allCharts.iterator();
    while (chartIter.hasNext()) {
      Plot plot = chartIter.next().getPlot();
      if (plot instanceof XYPlot) {
        ((XYPlot) plot).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
      }
    }
  }

  protected Date tmpDate = new Date();

  protected void updateSeries(TimeSeries series, Number value) {
    if (series != null) {
      series.addOrUpdate(new Second(tmpDate), getValueOrMissing(series, value));
    }
  }

  protected void updateSeries(TimeTableXYDataset dataset, Number value, String seriesName) {
    if (dataset != null && value != null) {
      dataset.add(new Second(tmpDate), value.doubleValue(), seriesName);
    }
  }

  public static String convert(long value) {
    try {
      if (value < MemorySizeUnits.KILO.getInBytes()) {
        return Long.toString(value);
      } else {
        return Conversion.memoryBytesAsSize(value);
      }
    } catch (Exception e) {
      return Long.toString(value);
    }
  }

  private static final boolean SHOW_LAST_ON_MISSING = true;

  protected static Number getValueOrMissing(TableXYDataset dataset, Number value, int series) {
    if (value != null) { return value; }
    if (SHOW_LAST_ON_MISSING) {
      int itemCount = dataset.getItemCount();
      if (itemCount > 0) { return dataset.getY(series, itemCount - 1); }
    }
    return Double.valueOf(0);
  }

  protected static Number getValueOrMissing(TimeSeries timeSeries, Number value) {
    if (value != null) { return value; }
    if (SHOW_LAST_ON_MISSING) {
      int itemCount = timeSeries.getItemCount();
      if (itemCount > 0) { return timeSeries.getValue(itemCount - 1); }
    }
    return Double.valueOf(0);
  }

  protected static Number getValueOrMissing(TimeSeries series, StatisticData[] data, int index) {
    Number result = null;
    if (data != null && data.length > 0) {
      StatisticData seriesData = data[index];
      if (seriesData != null) {
        result = (Number) seriesData.getData();
      }
    }
    if (SHOW_LAST_ON_MISSING) {
      if (result == null) {
        int itemCount = series.getItemCount();
        if (itemCount > 0) {
          result = series.getValue(itemCount - 1);
        }
      }
    }
    if (result == null) {
      result = Double.valueOf(0);
    }
    return result;
  }

  protected void handleTransactionRate(TimeSeries series, Long value) {
    if (series != null) {
      series.addOrUpdate(new Second(tmpDate), getValueOrMissing(series, value));
    }
  }

  protected void handleCpuUsage(TimeSeries series, StatisticData data) {
    if (series != null) {
      series.addOrUpdate(new Second(tmpDate), getValueOrMissing(series, (Number) data.getData()));
    }
  }

  protected void handleCpuUsage(TimeSeries[] series, StatisticData[] data) {
    if (series != null && series.length > 0) {
      Second now = new Second(tmpDate);
      for (int i = 0; i < series.length; i++) {
        series[i].addOrUpdate(now, getValueOrMissing(series[i], data, i));
      }
    }
  }

  protected void handleCpuUsage(FixedTimeSeriesCollection dataset, StatisticData[] data) {
    float[] fa = null;
    if (data != null) {
      fa = new float[data.length];
      for (int i = 0; i < data.length; i++) {
        fa[i] = data[i] != null ? ((Number) data[i].getData()).floatValue() : 0.0f;
      }
    }
    dataset.appendData(new Second(tmpDate), fa);
  }

  protected void handleMemoryUsage(TimeSeries[] series, Long max, Long used) {
    if (series != null) {
      Second now = new Second(tmpDate);
      series[0].addOrUpdate(now, getValueOrMissing(series[0], max));
      series[1].addOrUpdate(now, getValueOrMissing(series[1], used));
    }
  }

  protected void setupHypericInstructions(JComponent comp) {
    comp.removeAll();
    XTextPane textPane = new XTextPane();
    textPane.setEditorKit(new SyncHTMLEditorKit());
    textPane.setEditable(false);
    textPane.setBackground(Color.WHITE);
    textPane.addHyperlinkListener(new HypericInstructionsLinkListener());
    try {
      textPane.setPage(getClass().getResource(HYPERIC_INSTRUCTIONS_URI));
    } catch (IOException ioe) {
      textPane.setText(ioe.getMessage());
    }
    comp.setLayout(new BorderLayout());
    comp.add(new JScrollPane(textPane));
    comp.revalidate();
    comp.repaint();
  }

  private static class HypericInstructionsLinkListener implements HyperlinkListener {
    public void hyperlinkUpdate(HyperlinkEvent e) {
      HyperlinkEvent.EventType type = e.getEventType();
      Element elem = e.getSourceElement();

      if (elem == null || type == HyperlinkEvent.EventType.ENTERED || type == HyperlinkEvent.EventType.EXITED) { return; }

      AttributeSet a = elem.getAttributes();
      AttributeSet anchor = (AttributeSet) a.getAttribute(HTML.Tag.A);
      String href = (String) anchor.getAttribute(HTML.Attribute.HREF);
      BrowserLauncher.openURL(href);
    }
  }

  public void attributesPolled(PolledAttributesResult result) {
    /* override me */
  }

  public void preferenceChange(PreferenceChangeEvent evt) {
    Preferences prefs = evt.getNode();
    String key = evt.getKey();

    if (key.equals(DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY)) {
      setRuntimeStatsPollPeriodSeconds(prefs.getInt(key, DEFAULT_POLL_PERIOD_SECS));
    } else if (key.equals(DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY)) {
      setRuntimeStatsSampleHistoryMinutes(prefs.getInt(key, DEFAULT_SAMPLE_HISTORY_MINUTES));
    }
  }

  @Override
  public void tearDown() {
    Preferences runtimeStatsPrefs = appContext.getPrefs().node(RuntimeStatsOption.NAME);
    runtimeStatsPrefs.removePreferenceChangeListener(this);
    removeHierarchyListener(this);
    stopMonitoringRuntimeStats();

    allSeries.clear();
    allCharts.clear();

    super.tearDown();
  }
}
