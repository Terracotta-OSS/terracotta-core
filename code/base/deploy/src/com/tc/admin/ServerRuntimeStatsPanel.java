/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import static com.tc.admin.model.IClusterNode.POLLED_ATTR_CPU_USAGE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_MAX_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FAULT_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FLUSH_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_TRANSACTION_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_USED_MEMORY;
import static com.tc.admin.model.IServer.POLLED_ATTR_CACHE_MISS_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XContainer;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.statistics.StatisticData;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.border.TitledBorder;

public class ServerRuntimeStatsPanel extends BaseRuntimeStatsPanel {
  private IServer                  server;
  private ServerListener           serverListener;

  private TimeSeries               memoryMaxSeries;
  private TimeSeries               memoryUsedSeries;

  private ChartPanel               cpuPanel;
  private TimeSeries[]             cpuTimeSeries;
  private Map<String, TimeSeries>  cpuTimeSeriesMap;

  private TimeSeries               flushRateSeries;
  private TimeSeries               faultRateSeries;
  private TimeSeries               txnRateSeries;
  private TimeSeries               cacheMissRateSeries;

  private static final Set<String> POLLED_ATTRIBUTE_SET = new HashSet(Arrays.asList(POLLED_ATTR_CPU_USAGE,
                                                                                    POLLED_ATTR_USED_MEMORY,
                                                                                    POLLED_ATTR_MAX_MEMORY,
                                                                                    POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                    POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                    POLLED_ATTR_TRANSACTION_RATE,
                                                                                    POLLED_ATTR_CACHE_MISS_RATE));

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
  public void attributesPolled(PolledAttributesResult result) {
    IServer theServer = getServer();
    if (theServer != null) {
      handleDSOStats(result);
      handleSysStats(result);
    }
  }

  private synchronized void handleDSOStats(PolledAttributesResult result) {
    IServer theServer = getServer();
    if (theServer != null) {
      tmpDate.setTime(System.currentTimeMillis());
      updateSeries(flushRateSeries, (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OBJECT_FLUSH_RATE));
      updateSeries(faultRateSeries, (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OBJECT_FAULT_RATE));
      updateSeries(txnRateSeries, (Number) result.getPolledAttribute(theServer, POLLED_ATTR_TRANSACTION_RATE));
      updateSeries(cacheMissRateSeries, (Number) result.getPolledAttribute(theServer, POLLED_ATTR_CACHE_MISS_RATE));
    }
  }

  private synchronized void handleSysStats(PolledAttributesResult result) {
    IServer theServer = getServer();
    if (theServer != null) {
      updateSeries(memoryMaxSeries, (Number) result.getPolledAttribute(theServer, POLLED_ATTR_MAX_MEMORY));
      updateSeries(memoryUsedSeries, (Number) result.getPolledAttribute(theServer, POLLED_ATTR_USED_MEMORY));

      if (cpuTimeSeries != null) {
        StatisticData[] cpuUsageData = (StatisticData[]) result.getPolledAttribute(theServer, POLLED_ATTR_CPU_USAGE);
        if (cpuUsageData != null) {
          for (int i = 0; i < cpuUsageData.length; i++) {
            StatisticData cpuData = cpuUsageData[i];
            TimeSeries timeSeries = cpuTimeSeriesMap.get(cpuData.getElement());
            if (timeSeries != null) {
              updateSeries(timeSeries, (Number) cpuData.getData());
            }
          }
        }
      }
    }
  }

  @Override
  protected synchronized void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(0, 2));
    setupMemoryPanel(chartsPanel);
    setupCpuPanel(chartsPanel);
    setupTxnRatePanel(chartsPanel);
    setupCacheMissRatePanel(chartsPanel);
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
  }

  private void setupFaultRatePanel(XContainer parent) {
    faultRateSeries = createTimeSeries("");
    ChartPanel faultRatePanel = createChartPanel(createChart(faultRateSeries, false));
    parent.add(faultRatePanel);
    faultRatePanel.setPreferredSize(fDefaultGraphSize);
    faultRatePanel.setBorder(new TitledBorder(appContext.getString("server.stats.fault.rate")));
    faultRatePanel.setToolTipText(appContext.getString("server.stats.fault.rate.tip"));
  }

  private void setupTxnRatePanel(XContainer parent) {
    txnRateSeries = createTimeSeries("");
    ChartPanel txnRatePanel = createChartPanel(createChart(txnRateSeries, false));
    parent.add(txnRatePanel);
    txnRatePanel.setPreferredSize(fDefaultGraphSize);
    txnRatePanel.setBorder(new TitledBorder(appContext.getString("server.stats.transaction.rate")));
    txnRatePanel.setToolTipText(appContext.getString("server.stats.transaction.rate.tip"));
  }

  private void setupCacheMissRatePanel(XContainer parent) {
    cacheMissRateSeries = createTimeSeries("");
    ChartPanel cacheMissRatePanel = createChartPanel(createChart(cacheMissRateSeries, false));
    parent.add(cacheMissRatePanel);
    cacheMissRatePanel.setPreferredSize(fDefaultGraphSize);
    cacheMissRatePanel.setBorder(new TitledBorder(appContext.getString("server.stats.cache.miss.rate")));
    cacheMissRatePanel.setToolTipText(appContext.getString("server.stats.cache.miss.rate.tip"));
  }

  private void setupMemoryPanel(XContainer parent) {
    memoryMaxSeries = createTimeSeries(appContext.getString("heap.usage.max"));
    memoryUsedSeries = createTimeSeries(appContext.getString("heap.usage.used"));
    JFreeChart memoryChart = createChart(new TimeSeries[] { memoryMaxSeries, memoryUsedSeries });
    XYPlot plot = (XYPlot) memoryChart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeIncludesZero(true);
    ChartPanel memoryPanel = createChartPanel(memoryChart);
    parent.add(memoryPanel);
    memoryPanel.setPreferredSize(fDefaultGraphSize);
    memoryPanel.setBorder(new TitledBorder(appContext.getString("server.stats.heap.usage")));
    memoryPanel.setToolTipText(appContext.getString("server.stats.heap.usage.tip"));
  }

  private synchronized void setupCpuSeries(int processorCount) {
    cpuTimeSeriesMap = new HashMap<String, TimeSeries>();
    cpuTimeSeries = new TimeSeries[processorCount];
    for (int i = 0; i < processorCount; i++) {
      String cpuName = "cpu " + i;
      cpuTimeSeriesMap.put(cpuName, cpuTimeSeries[i] = createTimeSeries(cpuName));
    }
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

  private class CpuPanelWorker extends BasicWorker<String[]> {
    private CpuPanelWorker() {
      super(new Callable<String[]>() {
        public String[] call() throws Exception {
          IServer theServer = getServer();
          if (theServer != null) { return theServer.getCpuStatNames(); }
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
        String[] cpuNames = getResult();
        if (cpuNames.length > 0) {
          setupCpuSeries(cpuNames.length);
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
      cpuTimeSeriesMap.clear();
      cpuTimeSeriesMap = null;
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
    if (cacheMissRateSeries != null) {
      list.add(cacheMissRateSeries);
      cacheMissRateSeries = null;
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
