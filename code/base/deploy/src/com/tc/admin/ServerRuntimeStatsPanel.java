/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import static com.tc.admin.model.IClusterNode.POLLED_ATTR_CPU_USAGE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_MAX_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FAULT_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FLUSH_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OFFHEAP_MAX_MEMORY;
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
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.statistics.StatisticData;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class ServerRuntimeStatsPanel extends BaseRuntimeStatsPanel {
  private IServer                  server;
  private ServerListener           serverListener;

  private TimeSeries               onHeapMaxSeries;
  private StatusView               onHeapMaxLabel;
  private TimeSeries               onHeapUsedSeries;
  private StatusView               onHeapUsedLabel;
  private TimeSeries               offHeapMaxSeries;
  private StatusView               offHeapMaxLabel;
  private TimeSeries               offHeapUsedSeries;
  private StatusView               offHeapUsedLabel;

  private ChartPanel               cpuPanel;
  private TimeSeries[]             cpuTimeSeries;

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

  private final String             flushRateLabelFormat        = "{0,number,integer} Flushes/sec.";
  private final String             faultRateLabelFormat        = "{0,number,integer} Faults/sec.";
  private final String             txnRateLabelFormat          = "{0,number,integer} Txns/sec.";
  private final String             onHeapFaultRateLabelFormat  = "{0,number,integer} OnHeap Faults/sec.";
  private final String             onHeapFlushRateLabelFormat  = "{0,number,integer} OnHeap Flushes/sec.";
  private final String             offHeapFaultRateLabelFormat = "{0,number,integer} OffHeap Faults/sec.";
  private final String             offHeapFlushRateLabelFormat = "{0,number,integer} OffHeap Flushes/sec.";
  private final String             onHeapUsedLabelFormat       = "{0,number,integer} OnHeap Used";
  private final String             onHeapMaxLabelFormat        = "{0,number,integer} OnHeap Max";
  private final String             offHeapUsedLabelFormat      = "{0,number,integer} OffHeap Used";
  private final String             offHeapMaxLabelFormat       = "{0,number,integer} OffHeap Max";

  private static final Set<String> POLLED_ATTRIBUTE_SET        = new HashSet(
                                                                             Arrays
                                                                                 .asList(POLLED_ATTR_CPU_USAGE,
                                                                                         POLLED_ATTR_USED_MEMORY,
                                                                                         POLLED_ATTR_MAX_MEMORY,
                                                                                         POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                         POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                         POLLED_ATTR_TRANSACTION_RATE,
                                                                                         POLLED_ATTR_ONHEAP_FLUSH_RATE,
                                                                                         POLLED_ATTR_ONHEAP_FAULT_RATE,
                                                                                         POLLED_ATTR_OFFHEAP_FLUSH_RATE,
                                                                                         POLLED_ATTR_OFFHEAP_FAULT_RATE,
                                                                                         POLLED_ATTR_OFFHEAP_MAX_MEMORY,
                                                                                         POLLED_ATTR_OFFHEAP_USED_MEMORY));

  public ServerRuntimeStatsPanel(ApplicationContext appContext, IServer server) {
    super(appContext);
    this.server = server;
    setup(chartsPanel);
    setName(server.toString());
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

  synchronized IServer getServer() {
    return server;
  }

  private void addPolledAttributeListener() {
    IServer theServer = getServer();
    if (theServer != null) {
      theServer.addPolledAttributeListener(POLLED_ATTRIBUTE_SET, this);
    }
  }

  private void removePolledAttributeListener() {
    IServer theServer = getServer();
    if (theServer != null) {
      theServer.removePolledAttributeListener(POLLED_ATTRIBUTE_SET, this);
    }
  }

  @Override
  public void startMonitoringRuntimeStats() {
    addPolledAttributeListener();
    super.startMonitoringRuntimeStats();
  }

  @Override
  public void stopMonitoringRuntimeStats() {
    removePolledAttributeListener();
    super.stopMonitoringRuntimeStats();
  }

  @Override
  public void attributesPolled(final PolledAttributesResult result) {
    IServer theServer = getServer();
    if (theServer != null) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          handleDSOStats(result);
          handleSysStats(result);
        }
      });
    }
  }

  private synchronized void handleDSOStats(PolledAttributesResult result) {
    IServer theServer = getServer();
    if (theServer != null) {
      tmpDate.setTime(System.currentTimeMillis());
      Number n;

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OBJECT_FLUSH_RATE);
      updateSeries(flushRateSeries, n);
      if (n != null) {
        flushRateLabel.setText(MessageFormat.format(flushRateLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OBJECT_FAULT_RATE);
      updateSeries(faultRateSeries, n);
      if (n != null) {
        faultRateLabel.setText(MessageFormat.format(faultRateLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_TRANSACTION_RATE);
      updateSeries(txnRateSeries, n);
      if (n != null) {
        txnRateLabel.setText(MessageFormat.format(txnRateLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_ONHEAP_FAULT_RATE);
      updateSeries(onHeapFaultRateSeries, n);
      if (n != null) {
        onHeapFaultRateLabel.setText(MessageFormat.format(onHeapFaultRateLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_ONHEAP_FLUSH_RATE);
      updateSeries(onHeapFlushRateSeries, n);
      if (n != null) {
        onHeapFlushRateLabel.setText(MessageFormat.format(onHeapFlushRateLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OFFHEAP_FAULT_RATE);
      updateSeries(offHeapFaultRateSeries, n);
      if (n != null) {
        offHeapFaultRateLabel.setText(MessageFormat.format(offHeapFaultRateLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OFFHEAP_FLUSH_RATE);
      updateSeries(offHeapFlushRateSeries, n);
      if (n != null) {
        offHeapFlushRateLabel.setText(MessageFormat.format(offHeapFlushRateLabelFormat, n));
      }
    }
  }

  private synchronized void handleSysStats(PolledAttributesResult result) {
    IServer theServer = getServer();
    if (theServer != null) {
      Number n;

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_MAX_MEMORY);
      updateSeries(onHeapMaxSeries, n);
      if (n != null) {
        onHeapMaxLabel.setText(MessageFormat.format(onHeapMaxLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_USED_MEMORY);
      updateSeries(onHeapUsedSeries, n);
      if (n != null) {
        onHeapUsedLabel.setText(MessageFormat.format(onHeapUsedLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OFFHEAP_MAX_MEMORY);
      updateSeries(offHeapMaxSeries, n);
      if (n != null) {
        offHeapMaxLabel.setText(MessageFormat.format(offHeapMaxLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OFFHEAP_USED_MEMORY);
      updateSeries(offHeapUsedSeries, n);
      if (n != null) {
        offHeapUsedLabel.setText(MessageFormat.format(offHeapUsedLabelFormat, n));
      }

      if (cpuTimeSeries != null) {
        StatisticData[] cpuUsageData = (StatisticData[]) result.getPolledAttribute(theServer, POLLED_ATTR_CPU_USAGE);
        handleCpuUsage(cpuTimeSeries, cpuUsageData);
      }
    }
  }

  @Override
  protected synchronized void setup(XContainer chartsPanel) {
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
    ChartPanel chartPanel = createChartPanel(createChart(new TimeSeries[] { onHeapFaultRateSeries,
        onHeapFlushRateSeries }, false));
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
    ChartPanel chartPanel = createChartPanel(createChart(new TimeSeries[] { offHeapFaultRateSeries,
        offHeapFlushRateSeries }, false));
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

  private void setupOnHeapPanel(XContainer parent) {
    onHeapMaxSeries = createTimeSeries(appContext.getString("onheap.usage.max"));
    onHeapUsedSeries = createTimeSeries(appContext.getString("onheap.usage.used"));
    JFreeChart chart = createChart(new TimeSeries[] { onHeapMaxSeries, onHeapUsedSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeIncludesZero(true);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("server.stats.onheap.usage")));
    chartPanel.setToolTipText(appContext.getString("server.stats.onheap.usage.tip"));
    chartPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(onHeapMaxLabel = createStatusLabel(Color.red));
    labelHolder.add(onHeapUsedLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, gbc);
  }

  private void setupOffHeapPanel(XContainer parent) {
    offHeapMaxSeries = createTimeSeries(appContext.getString("offheap.usage.max"));
    offHeapUsedSeries = createTimeSeries(appContext.getString("offheap.usage.used"));
    JFreeChart chart = createChart(new TimeSeries[] { offHeapMaxSeries, offHeapUsedSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeIncludesZero(true);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("server.stats.offheap.usage")));
    chartPanel.setToolTipText(appContext.getString("server.stats.offheap.usage.tip"));
    chartPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(offHeapMaxLabel = createStatusLabel(Color.red));
    labelHolder.add(offHeapUsedLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, gbc);
  }

  private synchronized void setupCpuSeries(TimeSeries[] cpuTimeSeries) {
    this.cpuTimeSeries = cpuTimeSeries;
    JFreeChart cpuChart = createChart(cpuTimeSeries);
    XYPlot plot = (XYPlot) cpuChart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setRange(0.0, 1.0);
    if (rangeAxisSpace != null) {
      plot.setFixedRangeAxisSpace(rangeAxisSpace);
    }
    cpuPanel.setChart(cpuChart);
    cpuPanel.setDomainZoomable(false);
    cpuPanel.setRangeZoomable(false);
  }

  private class CpuPanelWorker extends BasicWorker<TimeSeries[]> {
    private CpuPanelWorker() {
      super(new Callable<TimeSeries[]>() {
        public TimeSeries[] call() throws Exception {
          IServer theServer = getServer();
          if (theServer != null) { return createCpusSeries(theServer); }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        setupInstructions();
      } else {
        TimeSeries[] cpus = getResult();
        if (cpus.length > 0) {
          setupCpuSeries(cpus);
        } else {
          setupInstructions();
        }
      }
    }
  }

  private synchronized void setupInstructions() {
    setupHypericInstructions(cpuPanel);
  }

  private void setupCpuPanel(XContainer parent) {
    cpuPanel = createChartPanel(null);
    parent.add(cpuPanel);
    cpuPanel.setPreferredSize(fDefaultGraphSize);
    cpuPanel.setBorder(new TitledBorder(appContext.getString("server.stats.cpu.usage")));
    cpuPanel.setToolTipText(appContext.getString("server.stats.cpu.usage.tip"));
    appContext.execute(new CpuPanelWorker());
  }

  private void clearAllTimeSeries() {
    ArrayList<TimeSeries> list = new ArrayList<TimeSeries>();
    if (cpuTimeSeries != null) {
      list.addAll(Arrays.asList(cpuTimeSeries));
      cpuTimeSeries = null;
    }
    if (onHeapMaxSeries != null) {
      list.add(onHeapMaxSeries);
      onHeapMaxSeries = null;
    }
    if (onHeapUsedSeries != null) {
      list.add(onHeapUsedSeries);
      onHeapUsedSeries = null;
    }
    if (offHeapMaxSeries != null) {
      list.add(offHeapMaxSeries);
      offHeapMaxSeries = null;
    }
    if (offHeapUsedSeries != null) {
      list.add(offHeapUsedSeries);
      offHeapUsedSeries = null;
    }
    if (flushRateSeries != null) {
      list.add(flushRateSeries);
      flushRateSeries = null;
    }
    if (faultRateSeries != null) {
      list.add(faultRateSeries);
      faultRateSeries = null;
    }
    if (txnRateSeries != null) {
      list.add(txnRateSeries);
      txnRateSeries = null;
    }
    if (onHeapFaultRateSeries != null) {
      list.add(onHeapFaultRateSeries);
      onHeapFaultRateSeries = null;
    }
    if (onHeapFlushRateSeries != null) {
      list.add(onHeapFlushRateSeries);
      onHeapFlushRateSeries = null;
    }

    Iterator<TimeSeries> iter = list.iterator();
    while (iter.hasNext()) {
      iter.next().clear();
    }
  }

  @Override
  public void tearDown() {
    server.removePropertyChangeListener(serverListener);
    serverListener.tearDown();

    clearAllTimeSeries();

    synchronized (this) {
      server = null;
      serverListener = null;
      cpuPanel = null;
    }

    super.tearDown();
  }
}
