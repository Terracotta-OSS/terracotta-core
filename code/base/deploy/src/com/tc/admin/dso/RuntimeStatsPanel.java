/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Button;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Spinner;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.ui.RectangleInsets;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextPane;
import com.tc.management.RuntimeStatisticConstants;
import com.tc.stats.statistics.CountStatistic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

public class RuntimeStatsPanel extends XContainer implements RuntimeStatisticConstants {
  protected AdminClientContext  m_acc;
  protected Timer               m_statsGathererTimer;
  protected Container           m_chartsPanel;
  private Button                m_startMonitoringButton;
  private Button                m_stopMonitoringButton;
  private Button                m_clearSamplesButton;
  private Spinner               m_samplePeriodSpinner;
  private Spinner               m_sampleHistorySpinner;
  private boolean               m_shouldAutoStart;

  protected static Dimension    fDefaultGraphSize                       = new Dimension(
                                                                                        ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
                                                                                        ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT);

  private static final int      DEFAULT_POLL_PERIOD_SECS                = 3;
  private static final int      DEFAULT_SAMPLE_HISTORY_MINUTES          = 5;
  private static final int      SAMPLE_SAMPLE_HISTORY_STEP_SIZE         = 1;

  private static final String   DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY    = "poll-periods-seconds";
  private static final String   DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY = "sample-history-minutes";

  private ArrayList<TimeSeries> m_allSeries;
  private ArrayList<JFreeChart> m_allCharts;

  private static final String   HYPERIC_INSTRUCTIONS_URI                = "/com/tc/admin/HypericInstructions.html";

  public RuntimeStatsPanel() {
    super();
    m_acc = AdminClient.getContext();
    m_allSeries = new ArrayList<TimeSeries>();
    m_allCharts = new ArrayList<JFreeChart>();
    m_shouldAutoStart = true;
    load((ContainerResource) AdminClient.getContext().topRes.child("RuntimeStatsPanel"));
  }

  public void load(ContainerResource res) {
    super.load(res);

    m_chartsPanel = (Container) findComponent("ChartsPanel");

    m_startMonitoringButton = (Button) findComponent("StartMonitoringButton");
    m_startMonitoringButton.addActionListener(new StartMonitoringAction());

    m_stopMonitoringButton = (Button) findComponent("StopMonitoringButton");
    m_stopMonitoringButton.addActionListener(new StopMonitoringAction());

    m_clearSamplesButton = (Button) findComponent("ClearSamplesButton");
    m_clearSamplesButton.addActionListener(new ClearSamplesAction());

    m_samplePeriodSpinner = (Spinner) findComponent("SamplePeriodSpinner");
    m_samplePeriodSpinner.setModel(new SpinnerNumberModel(Integer.valueOf(getDefaultPollPeriodSeconds()), Integer
        .valueOf(1), null, Integer.valueOf(1)));
    m_samplePeriodSpinner.addChangeListener(new SamplePeriodChangeHandler());

    m_sampleHistorySpinner = (Spinner) findComponent("SampleHistorySpinner");
    m_sampleHistorySpinner.setModel(new SpinnerNumberModel(Integer.valueOf(getDefaultSampleHistoryMinutes()), Integer
        .valueOf(1), null, Integer.valueOf(SAMPLE_SAMPLE_HISTORY_STEP_SIZE)));
    m_sampleHistorySpinner.addChangeListener(new SampleHistoryChangeHandler());
  }

  protected ChartPanel createChartPanel(JFreeChart chart) {
    boolean useBuffer = false;
    boolean properties = false;
    boolean save = false;
    boolean print = false;
    boolean zoom = false;
    boolean tooltips = true;

    ChartPanel chartPanel = new ChartPanel(chart, ChartPanel.DEFAULT_WIDTH, ChartPanel.DEFAULT_HEIGHT,
                                           ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
                                           ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT,
                                           ChartPanel.DEFAULT_MAXIMUM_DRAW_WIDTH,
                                           ChartPanel.DEFAULT_MAXIMUM_DRAW_HEIGHT, useBuffer, properties, save, print,
                                           zoom, tooltips);
    chartPanel.setRangeZoomable(false);
    chartPanel.setDomainZoomable(false);
    
    return chartPanel;
  }

  private int getDefaultPollPeriodSeconds() {
    return getIntPref(DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY, DEFAULT_POLL_PERIOD_SECS);
  }

  private int getDefaultSampleHistoryMinutes() {
    return getIntPref(DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY, DEFAULT_SAMPLE_HISTORY_MINUTES);
  }

  private class StartMonitoringAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      startMonitoringRuntimeStats();
    }
  }

  private class StopMonitoringAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      stopMonitoringRuntimeStats();
    }
  }

  private class ClearSamplesAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      clearAllRuntimeStatsSamples();
    }
  }

  private class SamplePeriodChangeHandler implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      SpinnerNumberModel model = (SpinnerNumberModel) m_samplePeriodSpinner.getModel();
      Integer i = (Integer) model.getNumber();
      setRuntimeStatsPollPeriodSeconds(i.intValue());
    }
  }

  private class SampleHistoryChangeHandler implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      SpinnerNumberModel model = (SpinnerNumberModel) m_sampleHistorySpinner.getModel();
      Integer i = (Integer) model.getNumber();
      setRuntimeStatsSampleHistoryMinutes(i.intValue());
    }
  }

  protected TimeSeries createTimeSeries(String name) {
    TimeSeries ts = new TimeSeries(name, Second.class);
    ts.setMaximumItemCount(50);
    m_allSeries.add(ts);
    return ts;
  }

  protected JFreeChart createChart(TimeSeries series) {
    JFreeChart chart = DemoChartFactory.getXYLineChart("", "", "", series);
    int sampleHistoryMinutes = getRuntimeStatsSampleHistoryMinutes();
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);

    int maxSampleCount = (sampleHistoryMinutes * 60) / getRuntimeStatsPollPeriodSeconds();
    series.setMaximumItemCount(maxSampleCount);

    m_allCharts.add(chart);
    return chart;
  }

  protected JFreeChart createChart(TimeSeries[] seriesArray) {
    JFreeChart chart = DemoChartFactory.getXYLineChart("", "", "", seriesArray);
    int sampleHistoryMinutes = getRuntimeStatsSampleHistoryMinutes();
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);

    int maxSampleCount = (sampleHistoryMinutes * 60) / getRuntimeStatsPollPeriodSeconds();
    for (TimeSeries series : seriesArray) {
      series.setMaximumItemCount(maxSampleCount);
    }

    m_allCharts.add(chart);
    return chart;
  }

  public void addNotify() {
    super.addNotify();

    double fixedRangeAxisSpace = 0;
    List<XYPlot> plotList = new ArrayList<XYPlot>();
    java.awt.Component[] chartPanels = m_chartsPanel.getComponents();
    for (java.awt.Component comp : chartPanels) {
      if (!(comp instanceof ChartPanel)) continue;
      ChartPanel chartPanel = (ChartPanel) comp;
      JFreeChart chart = chartPanel.getChart();
      if (chart == null) continue;
      XYPlot plot = ((XYPlot) chart.getPlot());
      plotList.add(plot);
      double rangeAxisSpace = getRangeAxisTickWidth(chartPanel.getGraphics(), plot);
      fixedRangeAxisSpace = Math.max(fixedRangeAxisSpace, rangeAxisSpace);
    }

    AxisSpace rangeAxisSpace = new AxisSpace();
    rangeAxisSpace.setLeft(fixedRangeAxisSpace);
    rangeAxisSpace.setRight(10);

    Iterator<XYPlot> plotIter = plotList.iterator();
    while (plotIter.hasNext()) {
      XYPlot plot = plotIter.next();
      plot.setFixedRangeAxisSpace(rangeAxisSpace);
    }

    if (m_shouldAutoStart) {
      startMonitoringRuntimeStats();
      m_shouldAutoStart = false;
    }
  }

  private double getRangeAxisTickWidth(Graphics graphics, XYPlot plot) {
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    RectangleInsets tickLabelInsets = numberAxis.getTickLabelInsets();
    NumberTickUnit unit = numberAxis.getTickUnit();

    // look at lower and upper bounds...
    FontMetrics fm = graphics.getFontMetrics(numberAxis.getTickLabelFont());
    Range range = numberAxis.getRange();
    double lower = range.getLowerBound();
    double upper = range.getUpperBound();
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

  protected void setup(Container runtimeStatsPanel) {
    /* override this */
  }

  public void startMonitoringRuntimeStats() {
    testStartStatsGatherer();
    m_chartsPanel.setVisible(true);
    m_startMonitoringButton.setEnabled(false);
    m_stopMonitoringButton.setEnabled(true);
  }

  public void stopMonitoringRuntimeStats() {
    if (m_statsGathererTimer != null) {
      m_statsGathererTimer.stop();
      m_startMonitoringButton.setEnabled(true);
      m_stopMonitoringButton.setEnabled(false);
    }
  }

  private void clearAllRuntimeStatsSamples() {
    boolean monitoring = false;
    if (m_statsGathererTimer != null && m_statsGathererTimer.isRunning()) {
      monitoring = true;
      m_statsGathererTimer.stop();
    }

    Iterator<TimeSeries> iter = m_allSeries.iterator();
    while (iter.hasNext()) {
      iter.next().clear();
    }

    if (monitoring) {
      m_statsGathererTimer.start();
    }
  }

  private int getIntPref(String key, int defaultValue) {
    Preferences prefs = m_acc.prefs.node("RuntimeStats");
    return prefs.getInt(key, defaultValue);
  }

  private void putIntPref(String key, int value) {
    Preferences prefs = m_acc.prefs.node("RuntimeStats");
    prefs.putInt(key, value);
    try {
      prefs.flush();
    } catch (Exception e) {/**/
    }
  }

  private void setRuntimeStatsPollPeriodSeconds(int seconds) {
    putIntPref(DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY, seconds);

    if (m_statsGathererTimer != null) {
      int pollMillis = seconds * 1000;
      m_statsGathererTimer.setInitialDelay(pollMillis);
      m_statsGathererTimer.restart();

      Iterator<JFreeChart> chartIter = m_allCharts.iterator();
      int sampleHistoryMinutes = getRuntimeStatsSampleHistoryMinutes();
      int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;
      while (chartIter.hasNext()) {
        ((XYPlot) chartIter.next().getPlot()).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
      }
    }
  }

  protected int getRuntimeStatsPollPeriodSeconds() {
    SpinnerNumberModel model = (SpinnerNumberModel) m_samplePeriodSpinner.getModel();
    Integer i = (Integer) model.getNumber();
    return i.intValue();
  }

  private int getRuntimeStatsPollPeriodMillis() {
    return getRuntimeStatsPollPeriodSeconds() * 1000;
  }

  private int getRuntimeStatsSampleHistoryMinutes() {
    SpinnerNumberModel model = (SpinnerNumberModel) m_sampleHistorySpinner.getModel();
    Integer i = (Integer) model.getNumber();
    return i.intValue();
  }

  private void setRuntimeStatsSampleHistoryMinutes(int sampleHistoryMinutes) {
    putIntPref(DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY, sampleHistoryMinutes);

    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;
    int maxSampleCount = (sampleHistoryMinutes * 60) / getRuntimeStatsPollPeriodSeconds();

    Iterator<TimeSeries> seriesIter = m_allSeries.iterator();
    while (seriesIter.hasNext()) {
      seriesIter.next().setMaximumItemCount(maxSampleCount);
    }

    Iterator<JFreeChart> chartIter = m_allCharts.iterator();
    while (chartIter.hasNext()) {
      ((XYPlot) chartIter.next().getPlot()).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    }
  }

  protected void testStartStatsGatherer() {
    if (m_statsGathererTimer == null) {
      int pollMillis = getRuntimeStatsPollPeriodMillis();
      m_statsGathererTimer = new Timer(pollMillis, new StatisticsRetrievalAction());
      m_statsGathererTimer.setRepeats(false);
    }
    if (!m_statsGathererTimer.isRunning()) {
      m_statsGathererTimer.start();
    }
  }

  private Date m_tmpDate = new Date();

  protected void updateSeries(TimeSeries series, CountStatistic value) {
    m_tmpDate.setTime(value.getLastSampleTime());
    series.addOrUpdate(new Second(m_tmpDate), value.getCount());
  }

  protected void retrieveStatistics() {
    /* override this */
  }

  private class StatisticsRetrievalAction implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      retrieveStatistics();
    }
  }

  protected void setupHypericInstructions(JComponent comp) {
    comp.removeAll();
    XTextPane textPane = new XTextPane();
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

  public void tearDown() {
    m_acc = null;
    if (m_statsGathererTimer != null && m_statsGathererTimer.isRunning()) {
      m_statsGathererTimer.stop();
    }
    m_statsGathererTimer = null;

    super.tearDown();

    m_chartsPanel = null;
    m_startMonitoringButton = null;
    m_stopMonitoringButton = null;
    m_clearSamplesButton = null;
    m_samplePeriodSpinner = null;
    m_sampleHistorySpinner = null;

  }
}
