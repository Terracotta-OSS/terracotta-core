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
import static com.tc.admin.model.IServer.POLLED_ATTR_CACHE_MISS_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_FLUSHED_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.DemoChartFactory;
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

  private TimeSeries               memoryMaxSeries;
  private StatusView               memoryMaxLabel;
  private TimeSeries               memoryUsedSeries;
  private StatusView               memoryUsedLabel;
  private TimeSeries               offHeapMaxSeries;
  private TimeSeries               offHeapUsedSeries;

  private ChartPanel               cpuPanel;
  private TimeSeries[]             cpuTimeSeries;

  private TimeSeries               flushRateSeries;
  private XLabel                   flushRateLabel;
  private TimeSeries               faultRateSeries;
  private XLabel                   faultRateLabel;
  private TimeSeries               txnRateSeries;
  private XLabel                   txnRateLabel;
  private TimeSeries               diskFaultRateSeries;
  private StatusView               diskFaultRateLabel;
  private TimeSeries               diskFlushRateSeries;
  private StatusView               diskFlushRateLabel;

  protected final String           flushRateLabelFormat     = "{0,number,integer} Flushes/sec.";
  protected final String           faultRateLabelFormat     = "{0,number,integer} Faults/sec.";
  protected final String           txnRateLabelFormat       = "{0,number,integer} Txns/sec.";
  protected final String           diskFaultRateLabelFormat = "{0,number,integer} Faults/sec.";
  protected final String           diskFlushRateLabelFormat = "{0,number,integer} Flushes/sec.";
  protected final String           memoryUsedLabelFormat    = "{0,number,integer} Heap Used";
  protected final String           memoryMaxLabelFormat     = "{0,number,integer} Heap Max";
  protected final String           offHeapUsedLabelFormat   = "{0,number,integer} OffHeap Used";
  protected final String           offHeapMaxLabelFormat    = "{0,number,integer} OffHeap Max";

  private static final Set<String> POLLED_ATTRIBUTE_SET     = new HashSet(
                                                                          Arrays
                                                                              .asList(POLLED_ATTR_CPU_USAGE,
                                                                                      POLLED_ATTR_USED_MEMORY,
                                                                                      POLLED_ATTR_MAX_MEMORY,
                                                                                      POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                      POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                      POLLED_ATTR_TRANSACTION_RATE,
                                                                                      POLLED_ATTR_CACHE_MISS_RATE,
                                                                                      POLLED_ATTR_FLUSHED_RATE,
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

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_CACHE_MISS_RATE);
      updateSeries(diskFaultRateSeries, n);
      if (n != null) {
        diskFaultRateLabel.setText(MessageFormat.format(diskFaultRateLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_FLUSHED_RATE);
      updateSeries(diskFlushRateSeries, n);
      if (n != null) {
        diskFlushRateLabel.setText(MessageFormat.format(diskFlushRateLabelFormat, n));
      }
    }
  }

  private synchronized void handleSysStats(PolledAttributesResult result) {
    IServer theServer = getServer();
    if (theServer != null) {
      Number n;

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_MAX_MEMORY);
      updateSeries(memoryMaxSeries, n);
      if (n != null) {
        memoryMaxLabel.setText(MessageFormat.format(memoryMaxLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_USED_MEMORY);
      updateSeries(memoryUsedSeries, n);
      if (n != null) {
        memoryUsedLabel.setText(MessageFormat.format(memoryUsedLabelFormat, n));
      }

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OFFHEAP_MAX_MEMORY);
      updateSeries(offHeapMaxSeries, n);

      n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OFFHEAP_USED_MEMORY);
      updateSeries(offHeapUsedSeries, n);

      if (cpuTimeSeries != null) {
        StatisticData[] cpuUsageData = (StatisticData[]) result.getPolledAttribute(theServer, POLLED_ATTR_CPU_USAGE);
        handleCpuUsage(cpuTimeSeries, cpuUsageData);
      }
    }
  }

  @Override
  protected synchronized void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(0, 2));
    setupMemoryPanel(chartsPanel);
    setupCpuPanel(chartsPanel);
    setupTxnRatePanel(chartsPanel);
    setupCacheManagerPanel(chartsPanel);
    setupFlushRatePanel(chartsPanel);
    setupFaultRatePanel(chartsPanel);
  }

  private void setupFlushRatePanel(XContainer parent) {
    flushRateSeries = createTimeSeries("");
    ChartPanel flushRatePanel = createChartPanel(createChart(flushRateSeries, false));
    parent.add(flushRatePanel);
    flushRatePanel.setPreferredSize(fDefaultGraphSize);
    flushRatePanel.setBorder(new TitledBorder(appContext.getString("server.stats.flush.rate")));
    flushRatePanel.setToolTipText(appContext.getString("server.stats.flush.rate.tip"));
    flushRatePanel.setLayout(new BorderLayout());
    flushRatePanel.add(flushRateLabel = createOverlayLabel());
  }

  private void setupFaultRatePanel(XContainer parent) {
    faultRateSeries = createTimeSeries("");
    ChartPanel faultRatePanel = createChartPanel(createChart(faultRateSeries, false));
    parent.add(faultRatePanel);
    faultRatePanel.setPreferredSize(fDefaultGraphSize);
    faultRatePanel.setBorder(new TitledBorder(appContext.getString("server.stats.fault.rate")));
    faultRatePanel.setToolTipText(appContext.getString("server.stats.fault.rate.tip"));
    faultRatePanel.setLayout(new BorderLayout());
    faultRatePanel.add(faultRateLabel = createOverlayLabel());
  }

  private void setupTxnRatePanel(XContainer parent) {
    txnRateSeries = createTimeSeries("");
    ChartPanel txnRatePanel = createChartPanel(createChart(txnRateSeries, false));
    parent.add(txnRatePanel);
    txnRatePanel.setPreferredSize(fDefaultGraphSize);
    txnRatePanel.setBorder(new TitledBorder(appContext.getString("server.stats.transaction.rate")));
    txnRatePanel.setToolTipText(appContext.getString("server.stats.transaction.rate.tip"));
    txnRatePanel.setLayout(new BorderLayout());
    txnRatePanel.add(txnRateLabel = createOverlayLabel());
  }

  private void setupCacheManagerPanel(XContainer parent) {
    diskFaultRateSeries = createTimeSeries(appContext.getString("dso.disk.fault.rate"));
    diskFlushRateSeries = createTimeSeries(appContext.getString("dso.disk.flush.rate"));
    ChartPanel cacheMissRatePanel = createChartPanel(createChart(new TimeSeries[] { diskFaultRateSeries,
        diskFlushRateSeries }, false));
    parent.add(cacheMissRatePanel);
    cacheMissRatePanel.setPreferredSize(fDefaultGraphSize);
    cacheMissRatePanel.setBorder(new TitledBorder(appContext.getString("server.stats.cache-manager")));
    cacheMissRatePanel.setToolTipText(appContext.getString("server.stats.cache-manager.tip"));
    cacheMissRatePanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(diskFaultRateLabel = createStatusLabel(Color.red));
    labelHolder.add(diskFlushRateLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    cacheMissRatePanel.add(labelHolder, gbc);
  }

  private void setupMemoryPanel(XContainer parent) {
    memoryMaxSeries = createTimeSeries(appContext.getString("heap.usage.max"));
    memoryUsedSeries = createTimeSeries(appContext.getString("heap.usage.used"));
    JFreeChart memoryChart = createChart(new TimeSeries[] { memoryMaxSeries, memoryUsedSeries }, false);
    XYPlot plot = (XYPlot) memoryChart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeIncludesZero(true);
    ChartPanel memoryPanel = createChartPanel(memoryChart);
    parent.add(memoryPanel);
    memoryPanel.setPreferredSize(fDefaultGraphSize);
    memoryPanel.setBorder(new TitledBorder(appContext.getString("server.stats.heap.usage")));
    memoryPanel.setToolTipText(appContext.getString("server.stats.heap.usage.tip"));
    memoryPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(memoryMaxLabel = createStatusLabel(Color.red));
    labelHolder.add(memoryUsedLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    memoryPanel.add(labelHolder, gbc);

    offHeapUsedSeries = createTimeSeries("OffHeap Used");
    int maxItemCount = memoryMaxSeries.getMaximumItemCount();
    offHeapUsedSeries.setMaximumItemCount(maxItemCount);
    offHeapMaxSeries = createTimeSeries("OffHeap Max");
    offHeapMaxSeries.setMaximumItemCount(maxItemCount);
    plot.setDataset(1,
                    DemoChartFactory.createTimeSeriesDataset(new TimeSeries[] { offHeapMaxSeries, offHeapUsedSeries }));
    NumberAxis axis2 = createNumberAxis("");
    plot.setRangeAxis(1, axis2);
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
    plot.setRenderer(1, renderer);
    renderer.setSeriesToolTipGenerator(1, StandardXYToolTipGenerator.getTimeSeriesInstance());
    plot.mapDatasetToRangeAxis(1, 1);
  }

  private static NumberAxis createNumberAxis(String name) {
    NumberAxis result = new NumberAxis(name);
    result.setRangeType(RangeType.POSITIVE);
    result.setStandardTickUnits(DemoChartFactory.DEFAULT_TICKS);
    result.setAutoRangeMinimumSize(10.0);
    result.setTickLabelFont(DemoChartFactory.regularFont);
    result.setLabelFont(DemoChartFactory.regularFont);
    return result;
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
    if (memoryMaxSeries != null) {
      list.add(memoryMaxSeries);
      memoryMaxSeries = null;
    }
    if (memoryUsedSeries != null) {
      list.add(memoryUsedSeries);
      memoryUsedSeries = null;
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
    if (diskFaultRateSeries != null) {
      list.add(diskFaultRateSeries);
      diskFaultRateSeries = null;
    }
    if (diskFlushRateSeries != null) {
      list.add(diskFlushRateSeries);
      diskFlushRateSeries = null;
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
