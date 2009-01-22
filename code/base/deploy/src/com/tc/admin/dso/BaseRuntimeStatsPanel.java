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
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.ui.RectangleInsets;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.LinkButton;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextPane;
import com.tc.admin.model.PolledAttributeListener;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.management.RuntimeStatisticConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
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
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

public class BaseRuntimeStatsPanel extends XContainer implements RuntimeStatisticConstants, PolledAttributeListener,
    PreferenceChangeListener, HierarchyListener {
  protected ApplicationContext     appContext;

  protected XContainer             chartsPanel;
  private XButton                  manageMonitoringButton;
  private XButton                  clearSamplesButton;
  private JButton                  configureOptionsButton;
  protected AxisSpace              rangeAxisSpace;
  private boolean                  autoStart;
  private boolean                  hasAutoStarted;
  private boolean                  isMonitoring;

  protected static final Dimension fDefaultGraphSize                       = new Dimension(0, 65);

  private static final int         DEFAULT_POLL_PERIOD_SECS                = 3;
  private static final int         DEFAULT_SAMPLE_HISTORY_MINUTES          = 5;

  private static final String      DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY    = "poll-periods-seconds";
  private static final String      DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY = "sample-history-minutes";

  private static final ImageIcon   fStartIcon;
  private static final ImageIcon   fStopIcon;
  private static final ImageIcon   fClearIcon;

  static {
    fStartIcon = new ImageIcon(BaseRuntimeStatsPanel.class.getResource("/com/tc/admin/icons/resume_co.gif"));
    fStopIcon = new ImageIcon(BaseRuntimeStatsPanel.class.getResource("/com/tc/admin/icons/suspend_co.gif"));
    fClearIcon = new ImageIcon(BaseRuntimeStatsPanel.class.getResource("/com/tc/admin/icons/clear_co.gif"));
  }

  private ArrayList<TimeSeries>    allSeries;
  private ArrayList<JFreeChart>    allCharts;

  private static final String      HYPERIC_INSTRUCTIONS_URI                = "/com/tc/admin/HypericInstructions.html";

  public BaseRuntimeStatsPanel(ApplicationContext appContext) {
    super(new BorderLayout());

    this.appContext = appContext;

    allSeries = new ArrayList<TimeSeries>();
    allCharts = new ArrayList<JFreeChart>();
    autoStart = true;

    add(chartsPanel = new XContainer());

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

    Preferences prefs = appContext.getPrefs().node("RuntimeStats");
    prefs.addPreferenceChangeListener(this);

    addHierarchyListener(this);
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

  private class ConfigureOptionsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      appContext.getApplicationController().showOption("RuntimeStats");
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

  protected TimeSeries createTimeSeries(String name) {
    TimeSeries ts = new TimeSeries(name, Second.class);
    ts.setMaximumItemCount(50);
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

  protected JFreeChart createChart(TimeSeries[] seriesArray, boolean createLegend) {
    JFreeChart chart = DemoChartFactory.getXYLineChart("", "", "", seriesArray, createLegend);
    int sampleHistoryMinutes = getSampleHistoryMinutes();
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);

    int maxSampleCount = (sampleHistoryMinutes * 60) / getPollPeriodSeconds();
    for (TimeSeries series : seriesArray) {
      series.setMaximumItemCount(maxSampleCount);
    }

    allCharts.add(chart);
    return chart;
  }

  public void hierarchyChanged(HierarchyEvent e) {
    long flags = e.getChangeFlags();
    if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (isShowing() && getAutoStart() && !hasAutoStarted) {
        hasAutoStarted = true;
        startMonitoringRuntimeStats();
      }
    }
  }

  public void addNotify() {
    super.addNotify();

    double fixedRangeAxisSpace = 0;
    List<XYPlot> plotList = new ArrayList<XYPlot>();
    java.awt.Component[] chartPanels = chartsPanel.getComponents();
    for (java.awt.Component comp : chartPanels) {
      if (!(comp instanceof ChartPanel)) continue;
      ChartPanel chartPanel = (ChartPanel) comp;
      JFreeChart chart = chartPanel.getChart();
      if (chart == null) continue;
      XYPlot plot = ((XYPlot) chart.getPlot());
      plotList.add(plot);
      double rangeAxisTickWidth = getRangeAxisTickWidth(chartPanel.getGraphics(), plot);
      fixedRangeAxisSpace = Math.max(fixedRangeAxisSpace, rangeAxisTickWidth);
    }

    rangeAxisSpace = new AxisSpace();
    rangeAxisSpace.setLeft(fixedRangeAxisSpace);
    rangeAxisSpace.setRight(5);

    if (plotList.size() > 0) {
      Iterator<XYPlot> plotIter = plotList.iterator();
      while (plotIter.hasNext()) {
        XYPlot plot = plotIter.next();
        plot.setFixedRangeAxisSpace(rangeAxisSpace);
      }
    }
  }

  private double getRangeAxisTickWidth(Graphics graphics, XYPlot plot) {
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    RectangleInsets tickLabelInsets = numberAxis.getTickLabelInsets();
//    NumberTickUnit unit = numberAxis.getTickUnit();
    double upper = 500000000000d;
    NumberTickUnit unit = (NumberTickUnit) DemoChartFactory.DEFAULT_INTEGER_TICKS.getCeilingTickUnit(upper);
    
    // look at lower and upper bounds...
    FontMetrics fm = graphics.getFontMetrics(numberAxis.getTickLabelFont());
    Range range = numberAxis.getRange();
    double lower = range.getLowerBound();
//    double upper = range.getUpperBound();
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
    chartsPanel.setVisible(true);
    isMonitoring = true;
    manageMonitoringButton.setIcon(fStopIcon);
  }

  public void stopMonitoringRuntimeStats() {
    isMonitoring = false;
    manageMonitoringButton.setIcon(fStartIcon);
  }

  private void clearAllRuntimeStatsSamples() {
    Iterator<TimeSeries> iter = allSeries.iterator();
    while (iter.hasNext()) {
      iter.next().clear();
    }
  }

  private int getIntPref(String key, int defaultValue) {
    return appContext.getPrefs().node("RuntimeStats").getInt(key, defaultValue);
  }

  private void setRuntimeStatsPollPeriodSeconds(int seconds) {
    Iterator<JFreeChart> chartIter = allCharts.iterator();
    int sampleHistoryMinutes = getSampleHistoryMinutes();
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;
    while (chartIter.hasNext()) {
      ((XYPlot) chartIter.next().getPlot()).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    }
  }

  private void setRuntimeStatsSampleHistoryMinutes(int sampleHistoryMinutes) {
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;
    int maxSampleCount = (sampleHistoryMinutes * 60) / getPollPeriodSeconds();

    Iterator<TimeSeries> seriesIter = allSeries.iterator();
    while (seriesIter.hasNext()) {
      seriesIter.next().setMaximumItemCount(maxSampleCount);
    }

    Iterator<JFreeChart> chartIter = allCharts.iterator();
    while (chartIter.hasNext()) {
      ((XYPlot) chartIter.next().getPlot()).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    }
  }

  protected Date tmpDate = new Date();

  protected void updateSeries(TimeSeries series, Number value) {
    if (value != null) {
      series.addOrUpdate(new Second(tmpDate), value);
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

  public synchronized void tearDown() {
    removeHierarchyListener(this);
    stopMonitoringRuntimeStats();

    super.tearDown();

    appContext = null;
    chartsPanel = null;
    manageMonitoringButton = null;
    clearSamplesButton = null;
  }

}
