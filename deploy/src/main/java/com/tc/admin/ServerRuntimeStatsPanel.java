/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import static com.tc.admin.model.IClusterNode.POLLED_ATTR_MAX_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FAULT_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FLUSH_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OFFHEAP_MAP_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OFFHEAP_MAX_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OFFHEAP_OBJECT_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OFFHEAP_USED_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_TRANSACTION_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_USED_MEMORY;
import static com.tc.admin.model.IServer.POLLED_ATTR_OFFHEAP_FAULT_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_OFFHEAP_FLUSH_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_ONHEAP_FAULT_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_ONHEAP_FLUSH_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYAreaRenderer2;
import org.jfree.data.RangeType;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.FixedTimeTableXYDataset;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.dso.ClusterNodeRuntimeStatsPanel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.PolledAttributesResult;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class ServerRuntimeStatsPanel extends ClusterNodeRuntimeStatsPanel {
  private final IServer            server;
  private final ServerListener     serverListener;

  private FixedTimeTableXYDataset  offHeapDataset;
  private String                   offHeapMapUsedSeries;
  private String                   offHeapObjectUsedSeries;

  private TimeSeries               flushRateSeries;
  private XLabel                   flushRateLabel;
  private TimeSeries               faultRateSeries;
  private XLabel                   faultRateLabel;
  private TimeSeries               txnRateSeries;
  private XLabel                   txnRateLabel;
  private TimeSeries               onHeapFaultRateSeries;
  private StatusView               onHeapFaultRateLabel;
  private TimeSeries               onHeapFlushRateSeries;
  private StatusView               onHeapFlushRateLabel;
  private TimeSeries               offHeapFaultRateSeries;
  private StatusView               offHeapFaultRateLabel;
  private TimeSeries               offHeapFlushRateSeries;
  private StatusView               offHeapFlushRateLabel;
  private String                   offHeapUsageTitlePattern;
  private TitledBorder             offHeapUsageTitle;
  private NumberAxis               offHeapValueAxis;

  private static final String      FLUSH_RATE_LABEL_FORMAT         = "{0} Flushes/sec.";
  private static final String      FAULT_RATE_LABEL_FORMAT         = "{0} Faults/sec.";
  private static final String      TXN_RATE_LABEL_FORMAT           = "{0} Txns/sec.";
  private static final String      ONHEAP_FAULT_RATE_LABEL_FORMAT  = "{0} OnHeap Faults/sec.";
  private static final String      ONHEAP_FLUSH_RATE_LABEL_FORMAT  = "{0} OnHeap Flushes/sec.";
  private static final String      OFFHEAP_FAULT_RATE_LABLE_FORMAT = "{0} OffHeap Faults/sec.";
  private static final String      OFFHEAP_FLUSH_RATE_LABEL_FORMAT = "{0} OffHeap Flushes/sec.";

  private static final Set<String> POLLED_ATTRIBUTE_SET            = new HashSet(
                                                                                 Arrays
                                                                                     .asList(POLLED_ATTR_USED_MEMORY,
                                                                                             POLLED_ATTR_MAX_MEMORY,
                                                                                             POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                             POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                             POLLED_ATTR_TRANSACTION_RATE,
                                                                                             POLLED_ATTR_ONHEAP_FLUSH_RATE,
                                                                                             POLLED_ATTR_ONHEAP_FAULT_RATE,
                                                                                             POLLED_ATTR_OFFHEAP_FLUSH_RATE,
                                                                                             POLLED_ATTR_OFFHEAP_FAULT_RATE,
                                                                                             POLLED_ATTR_OFFHEAP_MAX_MEMORY,
                                                                                             POLLED_ATTR_OFFHEAP_USED_MEMORY,
                                                                                             POLLED_ATTR_OFFHEAP_OBJECT_MEMORY,
                                                                                             POLLED_ATTR_OFFHEAP_MAP_MEMORY));

  public ServerRuntimeStatsPanel(ApplicationContext appContext, IServer server) {
    super(appContext, server);
    this.server = server;
    setup(chartsPanel);
    server.addPropertyChangeListener(serverListener = new ServerListener(server));
  }

  private class ServerListener extends AbstractServerListener {
    private ServerListener(IServer server) {
      super(server);
    }

    @Override
    protected void handleReady() {
      if (!server.isReady() && isMonitoringRuntimeStats()) {
        stopMonitoringRuntimeStats();
      } else if (server.isReady() && isShowing() && getAutoStart()) {
        startMonitoringRuntimeStats();
      }
    }
  }

  @Override
  protected void addPolledAttributeListener() {
    server.addPolledAttributeListener(POLLED_ATTRIBUTE_SET, this);
  }

  @Override
  protected void removePolledAttributeListener() {
    super.removePolledAttributeListener();
    server.removePolledAttributeListener(POLLED_ATTRIBUTE_SET, this);
  }

  @Override
  public void startMonitoringRuntimeStats() {
    if (server.isReady()) {
      super.startMonitoringRuntimeStats();
    }
  }

  @Override
  protected void setMaximumItemAge(int maxItemAge) {
    super.setMaximumItemAge(maxItemAge);
    offHeapDataset.setMaximumItemAge(maxItemAge);
  }

  @Override
  public void attributesPolled(final PolledAttributesResult result) {
    handleDSOStats(result);
    handleSysStats(result);
  }

  private void handleDSOStats(PolledAttributesResult result) {
    tmpDate.setTime(System.currentTimeMillis());

    final Number flushRate = (Number) result.getPolledAttribute(server, POLLED_ATTR_OBJECT_FLUSH_RATE);
    final Number faultRate = (Number) result.getPolledAttribute(server, POLLED_ATTR_OBJECT_FAULT_RATE);
    final Number txnRate = (Number) result.getPolledAttribute(server, POLLED_ATTR_TRANSACTION_RATE);
    final Number onHeapFaultRate = (Number) result.getPolledAttribute(server, POLLED_ATTR_ONHEAP_FAULT_RATE);
    final Number onHeapFlushRate = (Number) result.getPolledAttribute(server, POLLED_ATTR_ONHEAP_FLUSH_RATE);
    final Number offHeapFaultRate = (Number) result.getPolledAttribute(server, POLLED_ATTR_OFFHEAP_FAULT_RATE);
    final Number offHeapFlushRate = (Number) result.getPolledAttribute(server, POLLED_ATTR_OFFHEAP_FLUSH_RATE);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        updateSeries(flushRateSeries, flushRate);
        if (flushRate != null) {
          flushRateLabel.setText(MessageFormat.format(FLUSH_RATE_LABEL_FORMAT, convert(flushRate.longValue())));
        }

        updateSeries(faultRateSeries, faultRate);
        if (faultRate != null) {
          faultRateLabel.setText(MessageFormat.format(FAULT_RATE_LABEL_FORMAT, convert(faultRate.longValue())));
        }

        updateSeries(txnRateSeries, txnRate);
        if (txnRate != null) {
          txnRateLabel.setText(MessageFormat.format(TXN_RATE_LABEL_FORMAT, convert(txnRate.longValue())));
        }

        if (onHeapFaultRate != null) {
          onHeapFaultRateLabel.setText(MessageFormat.format(ONHEAP_FAULT_RATE_LABEL_FORMAT,
                                                            convert(onHeapFaultRate.longValue())));
        }
        if (onHeapFlushRate != null) {
          onHeapFlushRateLabel.setText(MessageFormat.format(ONHEAP_FLUSH_RATE_LABEL_FORMAT,
                                                            convert(onHeapFlushRate.longValue())));
        }
        updateSeries(onHeapFaultRateSeries, onHeapFaultRate);
        updateSeries(onHeapFlushRateSeries, onHeapFlushRate);

        if (offHeapFaultRate != null) {
          offHeapFaultRateLabel.setText(MessageFormat.format(OFFHEAP_FAULT_RATE_LABLE_FORMAT,
                                                             convert(offHeapFaultRate.longValue())));
        }
        if (offHeapFlushRate != null) {
          offHeapFlushRateLabel.setText(MessageFormat.format(OFFHEAP_FLUSH_RATE_LABEL_FORMAT,
                                                             convert(offHeapFlushRate.longValue())));
        }
        updateSeries(offHeapFaultRateSeries, offHeapFaultRate);
        updateSeries(offHeapFlushRateSeries, offHeapFlushRate);
      }
    });
  }

  @Override
  protected void handleSysStats(PolledAttributesResult result) {
    super.handleSysStats(result);

    final Number offHeapMaxMemory = (Number) result.getPolledAttribute(server, POLLED_ATTR_OFFHEAP_MAX_MEMORY);
    final Number offHeapMapMemory = (Number) result.getPolledAttribute(server, POLLED_ATTR_OFFHEAP_MAP_MEMORY);
    final Number offHeapObjectMemory = (Number) result.getPolledAttribute(server, POLLED_ATTR_OFFHEAP_OBJECT_MEMORY);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        long offHeapMax = 0;
        if (offHeapMaxMemory != null) {
          if ((offHeapMax = offHeapMaxMemory.longValue()) > 0) {
            offHeapValueAxis.setUpperBound(offHeapMax);
          }
        }

        updateSeries(offHeapDataset, offHeapMapMemory, offHeapMapUsedSeries);
        updateSeries(offHeapDataset, offHeapObjectMemory, offHeapObjectUsedSeries);

        long mapOffHeapUsedLong = 0;
        if (offHeapMapMemory != null) {
          mapOffHeapUsedLong = offHeapMapMemory.longValue();
        }

        long objectOffHeapUsed = 0;
        if (offHeapObjectMemory != null) {
          objectOffHeapUsed = offHeapObjectMemory.longValue();
        }

        if (offHeapMaxMemory != null) {
          offHeapUsageTitle.setTitle(MessageFormat.format(offHeapUsageTitlePattern, convertBytes(offHeapMax),
                                                          convertBytes(mapOffHeapUsedLong),
                                                          convertBytes(objectOffHeapUsed)));
        }
      }
    });
  }

  @Override
  protected void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(0, 2));
    setupOnHeapPanel(chartsPanel);
    setupOffHeapPanel(chartsPanel);
    setupCpuPanel(chartsPanel);
    setupTxnRatePanel(chartsPanel);
    setupOnHeapFaultFlushPanel(chartsPanel);
    setupOffHeapFaultFlushPanel(chartsPanel);
    setupFlushRatePanel(chartsPanel);
    setupFaultRatePanel(chartsPanel);
  }

  private void setupFlushRatePanel(XContainer parent) {
    flushRateSeries = createTimeSeries("");
    ChartPanel chartPanel = createChartPanel(createChart(flushRateSeries, false));
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("server.stats.flush.rate")));
    chartPanel.setToolTipText(appContext.getString("server.stats.flush.rate.tip"));
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(flushRateLabel = createOverlayLabel());
  }

  private void setupFaultRatePanel(XContainer parent) {
    faultRateSeries = createTimeSeries("");
    ChartPanel chartPanel = createChartPanel(createChart(faultRateSeries, false));
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("server.stats.fault.rate")));
    chartPanel.setToolTipText(appContext.getString("server.stats.fault.rate.tip"));
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(faultRateLabel = createOverlayLabel());
  }

  private void setupTxnRatePanel(XContainer parent) {
    txnRateSeries = createTimeSeries("");
    ChartPanel chartPanel = createChartPanel(createChart(txnRateSeries, false));
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("server.stats.transaction.rate")));
    chartPanel.setToolTipText(appContext.getString("server.stats.transaction.rate.tip"));
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(txnRateLabel = createOverlayLabel());
  }

  private void setupOnHeapFaultFlushPanel(XContainer parent) {
    onHeapFaultRateSeries = createTimeSeries(appContext.getString("dso.onheap.fault.rate"));
    onHeapFlushRateSeries = createTimeSeries(appContext.getString("dso.onheap.flush.rate"));
    JFreeChart chart = createChart(new TimeSeries[] { onHeapFaultRateSeries, onHeapFlushRateSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getRenderer().setSeriesPaint(0, Color.red);
    plot.getRenderer().setSeriesPaint(1, Color.blue);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("server.stats.onheap.flushfault")));
    chartPanel.setToolTipText(appContext.getString("server.stats.onheap.flushfault.tip"));
    chartPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(onHeapFaultRateLabel = createStatusLabel(Color.red));
    labelHolder.add(onHeapFlushRateLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, gbc);
  }

  private void setupOffHeapFaultFlushPanel(XContainer parent) {
    offHeapFaultRateSeries = createTimeSeries(appContext.getString("dso.offheap.fault.rate"));
    offHeapFlushRateSeries = createTimeSeries(appContext.getString("dso.offheap.flush.rate"));
    JFreeChart chart = createChart(new TimeSeries[] { offHeapFaultRateSeries, offHeapFlushRateSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getRenderer().setSeriesPaint(0, Color.red);
    plot.getRenderer().setSeriesPaint(1, Color.blue);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("server.stats.offheap.flushfault")));
    chartPanel.setToolTipText(appContext.getString("server.stats.offheap.flushfault.tip"));
    chartPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(offHeapFaultRateLabel = createStatusLabel(Color.red));
    labelHolder.add(offHeapFlushRateLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, gbc);
  }

  private void setupOffHeapPanel(XContainer parent) {
    offHeapDataset = new FixedTimeTableXYDataset();
    offHeapDataset.setDomainIsPointsInTime(true);
    offHeapMapUsedSeries = appContext.getString("offheap.map.usage");
    offHeapObjectUsedSeries = appContext.getString("offheap.object.usage");
    JFreeChart chart = createStackedXYAreaChart(offHeapDataset, true);
    XYPlot plot = (XYPlot) chart.getPlot();
    XYAreaRenderer2 areaRenderer = new StackedXYAreaRenderer2(StandardXYToolTipGenerator.getTimeSeriesInstance(), null);
    plot.setRenderer(areaRenderer);
    areaRenderer.setSeriesPaint(0, (Color) appContext.getObject("chart.color.1"));
    areaRenderer.setSeriesPaint(1, (Color) appContext.getObject("chart.color.2"));
    offHeapValueAxis = (NumberAxis) plot.getRangeAxis();
    offHeapValueAxis.setAutoRangeIncludesZero(true);
    offHeapValueAxis.setRangeType(RangeType.POSITIVE);
    offHeapValueAxis.setStandardTickUnits(DemoChartFactory.DEFAULT_MEMORY_TICKS);
    ChartPanel chartPanel = createChartPanel(chart);
    String offHeapUsageLabel = appContext.getString("server.stats.offheap.usage");
    offHeapUsageTitlePattern = offHeapUsageLabel + " (max: {0}, map: {1}, object: {2})";
    offHeapUsageTitle = BorderFactory.createTitledBorder(offHeapUsageLabel);
    chartPanel.setBorder(offHeapUsageTitle);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setToolTipText(appContext.getString("server.stats.offheap.usage.tip"));
  }

  @Override
  protected void clearAllTimeSeries() {
    if (flushRateSeries != null) {
      flushRateSeries.clear();
    }
    if (faultRateSeries != null) {
      faultRateSeries.clear();
    }
    if (txnRateSeries != null) {
      txnRateSeries.clear();
    }
    if (onHeapFaultRateSeries != null) {
      onHeapFaultRateSeries.clear();
    }
    if (onHeapFlushRateSeries != null) {
      onHeapFlushRateSeries.clear();
    }
    if (offHeapDataset != null) {
      offHeapDataset.clear();
    }
  }

  @Override
  public void tearDown() {
    server.removePropertyChangeListener(serverListener);
    serverListener.tearDown();
    super.tearDown();
  }
}
