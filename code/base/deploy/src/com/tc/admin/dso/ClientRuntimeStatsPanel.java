/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import static com.tc.admin.model.IClient.POLLED_ATTR_PENDING_TRANSACTIONS_COUNT;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_CPU_USAGE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_MAX_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FAULT_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FLUSH_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_TRANSACTION_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_USED_MEMORY;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.FixedTimeSeriesCollection;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class ClientRuntimeStatsPanel extends BaseRuntimeStatsPanel {
  private IClient                   client;
  private TimeSeries                memoryMaxSeries;
  private StatusView                memoryMaxLabel;
  private TimeSeries                memoryUsedSeries;
  private StatusView                memoryUsedLabel;
  private ChartPanel                cpuPanel;
  private FixedTimeSeriesCollection cpuDataset;
  private TimeSeries                flushRateSeries;
  private XLabel                    flushRateLabel;
  private TimeSeries                faultRateSeries;
  private XLabel                    faultRateLabel;
  private TimeSeries                txnRateSeries;
  private XLabel                    txnRateLabel;
  private TimeSeries                pendingTxnsSeries;
  private XLabel                    pendingTxnsLabel;

  protected final String            flushRateLabelFormat   = "{0} Flushes/sec.";
  protected final String            faultRateLabelFormat   = "{0} Faults/sec.";
  protected final String            txnRateLabelFormat     = "{0} Txns/sec.";
  protected final String            pendingTxnsLabelFormat = "{0} Pending Txns/sec.";
  protected final String            memoryUsedLabelFormat  = "{0} Used";
  protected final String            memoryMaxLabelFormat   = "{0} Max";

  private static final Set<String>  POLLED_ATTRIBUTE_SET   = new HashSet(
                                                                         Arrays
                                                                             .asList(POLLED_ATTR_CPU_USAGE,
                                                                                     POLLED_ATTR_USED_MEMORY,
                                                                                     POLLED_ATTR_MAX_MEMORY,
                                                                                     POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                     POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                     POLLED_ATTR_TRANSACTION_RATE,
                                                                                     POLLED_ATTR_PENDING_TRANSACTIONS_COUNT));

  public ClientRuntimeStatsPanel(ApplicationContext appContext, IClient client) {
    super(appContext);
    this.client = client;
    setup(chartsPanel);
    setName(client.toString());
  }

  private synchronized IClient getClient() {
    return client;
  }

  private void addPolledAttributeListener() {
    IClient theClient = getClient();
    if (theClient != null) {
      theClient.addPolledAttributeListener(POLLED_ATTRIBUTE_SET, this);
    }
  }

  private void removePolledAttributeListener() {
    IClient theClient = getClient();
    if (theClient != null) {
      theClient.removePolledAttributeListener(POLLED_ATTRIBUTE_SET, this);
    }
  }

  @Override
  public void startMonitoringRuntimeStats() {
    IClient theClient = getClient();
    if (theClient != null) {
      addPolledAttributeListener();
    }
    super.startMonitoringRuntimeStats();
  }

  @Override
  public void stopMonitoringRuntimeStats() {
    removePolledAttributeListener();
    super.stopMonitoringRuntimeStats();
  }

  @Override
  protected void setMaximumItemCount(int maxItemCount) {
    super.setMaximumItemCount(maxItemCount);
    cpuDataset.setMaximumItemCount(maxItemCount);
  }

  @Override
  public void attributesPolled(PolledAttributesResult result) {
    if (tornDown.get()) { return; }
    IClient theClient = getClient();
    if (theClient != null) {
      handleDSOStats(result);
      handleSysStats(result);
    }
  }

  private void handleDSOStats(PolledAttributesResult result) {
    IClient theClient = getClient();
    if (theClient != null) {
      IClusterModel theClusterModel = client.getClusterModel();
      if (theClusterModel != null) {
        tmpDate.setTime(System.currentTimeMillis());

        long flush = 0;
        long fault = 0;
        long txn = 0;
        long pendingTxn = 0;
        Number n;

        for (IServerGroup group : theClusterModel.getServerGroups()) {
          for (IServer server : group.getMembers()) {
            if (server.isStarted()) {
              Map<ObjectName, Map<String, Object>> nodeMap = result.getAttributeMap(server);
              Map<String, Object> map = nodeMap != null ? nodeMap.get(theClient.getBeanName()) : null;
              if (map != null) {
                n = (Number) map.get(POLLED_ATTR_TRANSACTION_RATE);
                if (n != null && txn >= 0) {
                  txn += n.longValue();
                }
                n = (Number) map.get(POLLED_ATTR_OBJECT_FLUSH_RATE);
                if (n != null && flush >= 0) {
                  flush += n.longValue();
                }
                n = (Number) map.get(POLLED_ATTR_OBJECT_FAULT_RATE);
                if (n != null && fault >= 0) {
                  fault += n.longValue();
                }
                n = (Number) map.get(POLLED_ATTR_PENDING_TRANSACTIONS_COUNT);
                if (n != null && pendingTxn >= 0) {
                  pendingTxn += n.longValue();
                }
              }
            }
          }
        }

        final long theFlushRate = flush;
        final long theFaultRate = fault;
        final long theTxnRate = txn;
        final long thePendingTxnRate = pendingTxn;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (!tornDown.get()) {
              updateAllDSOSeries(theFlushRate, theFaultRate, theTxnRate, thePendingTxnRate);
            }
          }
        });
      }
    }
  }

  private void updateAllDSOSeries(long flush, long fault, long txn, long pendingTxn) {
    if (tornDown.get()) { return; }

    {
      updateSeries(flushRateSeries, Long.valueOf(flush));
      flushRateLabel.setText(MessageFormat.format(flushRateLabelFormat, convert(flush)));
    }
    {
      updateSeries(faultRateSeries, Long.valueOf(fault));
      faultRateLabel.setText(MessageFormat.format(faultRateLabelFormat, convert(fault)));
    }
    {
      updateSeries(txnRateSeries, Long.valueOf(txn));
      txnRateLabel.setText(MessageFormat.format(txnRateLabelFormat, convert(txn)));
    }
    {
      updateSeries(pendingTxnsSeries, Long.valueOf(pendingTxn));
      pendingTxnsLabel.setText(MessageFormat.format(pendingTxnsLabelFormat, convert(pendingTxn)));
    }
  }

  private void handleSysStats(final PolledAttributesResult result) {
    IClient theClient = getClient();
    if (theClient == null) { return; }

    final Number memoryMax = (Number) result.getPolledAttribute(theClient, POLLED_ATTR_MAX_MEMORY);
    final Number memoryUsed = (Number) result.getPolledAttribute(theClient, POLLED_ATTR_USED_MEMORY);
    final StatisticData[] cpuUsageData = (StatisticData[]) result.getPolledAttribute(theClient, POLLED_ATTR_CPU_USAGE);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (memoryMax != null) {
          memoryMaxLabel.setText(MessageFormat.format(memoryMaxLabelFormat, convert(memoryMax.longValue())));
        }
        if (memoryUsed != null) {
          memoryUsedLabel.setText(MessageFormat.format(memoryUsedLabelFormat, convert(memoryUsed.longValue())));
        }
        updateSeries(memoryMaxSeries, memoryMax);
        updateSeries(memoryUsedSeries, memoryUsed);

        if (cpuDataset != null) {
          handleCpuUsage(cpuDataset, cpuUsageData);
        }
      }
    });
  }

  @Override
  protected void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(0, 2));
    setupMemoryPanel(chartsPanel);
    setupCpuPanel(chartsPanel);
    setupTxnRatePanel(chartsPanel);
    setupPendingTxnsPanel(chartsPanel);
    setupFlushRatePanel(chartsPanel);
    setupFaultRatePanel(chartsPanel);
  }

  private void setupFlushRatePanel(XContainer parent) {
    flushRateSeries = createTimeSeries("");
    ChartPanel flushRatePanel = createChartPanel(createChart(flushRateSeries, false));
    parent.add(flushRatePanel);
    flushRatePanel.setPreferredSize(fDefaultGraphSize);
    flushRatePanel.setBorder(new TitledBorder(appContext.getString("client.stats.flush.rate")));
    flushRatePanel.setToolTipText(appContext.getString("client.stats.flush.rate.tip"));
    flushRatePanel.setLayout(new BorderLayout());
    flushRatePanel.add(flushRateLabel = createOverlayLabel());
  }

  private void setupFaultRatePanel(XContainer parent) {
    faultRateSeries = createTimeSeries("");
    ChartPanel faultRatePanel = createChartPanel(createChart(faultRateSeries, false));
    parent.add(faultRatePanel);
    faultRatePanel.setPreferredSize(fDefaultGraphSize);
    faultRatePanel.setBorder(new TitledBorder(appContext.getString("client.stats.fault.rate")));
    faultRatePanel.setToolTipText(appContext.getString("client.stats.fault.rate.tip"));
    faultRatePanel.setLayout(new BorderLayout());
    faultRatePanel.add(faultRateLabel = createOverlayLabel());
  }

  private void setupTxnRatePanel(XContainer parent) {
    txnRateSeries = createTimeSeries("");
    ChartPanel txnRatePanel = createChartPanel(createChart(txnRateSeries, false));
    parent.add(txnRatePanel);
    txnRatePanel.setPreferredSize(fDefaultGraphSize);
    txnRatePanel.setBorder(new TitledBorder(appContext.getString("client.stats.transaction.rate")));
    txnRatePanel.setToolTipText(appContext.getString("client.stats.transaction.rate.tip"));
    txnRatePanel.setLayout(new BorderLayout());
    txnRatePanel.add(txnRateLabel = createOverlayLabel());
  }

  private void setupPendingTxnsPanel(XContainer parent) {
    pendingTxnsSeries = createTimeSeries("");
    ChartPanel pendingTxnsPanel = createChartPanel(createChart(pendingTxnsSeries, false));
    parent.add(pendingTxnsPanel);
    pendingTxnsPanel.setPreferredSize(fDefaultGraphSize);
    pendingTxnsPanel.setBorder(new TitledBorder(appContext.getString("client.stats.pending.transactions")));
    pendingTxnsPanel.setToolTipText(appContext.getString("client.stats.pending.transactions.tip"));
    pendingTxnsPanel.setLayout(new BorderLayout());
    pendingTxnsPanel.add(pendingTxnsLabel = createOverlayLabel());
  }

  private void setupMemoryPanel(XContainer parent) {
    memoryMaxSeries = createTimeSeries(appContext.getString("heap.usage.max"));
    memoryUsedSeries = createTimeSeries(appContext.getString("heap.usage.used"));
    JFreeChart chart = createChart(new TimeSeries[] { memoryMaxSeries, memoryUsedSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    XYItemRenderer renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, Color.red);
    renderer.setSeriesPaint(1, Color.blue);
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeIncludesZero(true);
    ChartPanel memoryPanel = createChartPanel(chart);
    parent.add(memoryPanel);
    memoryPanel.setPreferredSize(fDefaultGraphSize);
    memoryPanel.setBorder(new TitledBorder(appContext.getString("client.stats.heap.usage")));
    memoryPanel.setToolTipText(appContext.getString("client.stats.heap.usage.tip"));
    memoryPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(memoryMaxLabel = createStatusLabel(Color.red));
    labelHolder.add(memoryUsedLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    memoryPanel.add(labelHolder, gbc);
  }

  private void setupCpuSeries(FixedTimeSeriesCollection cpuDataset) {
    this.cpuDataset = cpuDataset;
    JFreeChart cpuChart = createChart(cpuDataset, false);
    XYPlot plot = (XYPlot) cpuChart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setRange(0.0, 1.0);
    cpuPanel.setChart(cpuChart);
    cpuPanel.setDomainZoomable(false);
    cpuPanel.setRangeZoomable(false);

    if (cpuDataset.getSeriesCount() == 0) {
      cpuPanel.setLayout(new BorderLayout());
      XLabel label = createOverlayLabel();
      label.setText("Sigar is disabled or missing");
      cpuPanel.add(label);
    }
  }

  private class CpuPanelWorker extends BasicWorker<FixedTimeSeriesCollection> {
    private CpuPanelWorker() {
      super(new Callable<FixedTimeSeriesCollection>() {
        public FixedTimeSeriesCollection call() throws Exception {
          final IClient theClient = getClient();
          if (theClient != null) { return createCpuDataset(theClient); }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      if (tornDown.get()) { return; }
      Exception e = getException();
      if (e != null) {
        appContext.log(e);
        setupInstructions();
      } else {
        setupCpuSeries(getResult());
      }
    }
  }

  private void setupInstructions() {
    setupHypericInstructions(cpuPanel);
  }

  private void setupCpuPanel(XContainer parent) {
    parent.add(cpuPanel = createChartPanel(null));
    cpuPanel.setPreferredSize(fDefaultGraphSize);
    cpuPanel.setBorder(new TitledBorder(appContext.getString("client.stats.cpu.usage")));
    cpuPanel.setToolTipText(appContext.getString("client.stats.cpu.usage.tip"));
    appContext.execute(new CpuPanelWorker());
  }

  private void clearAllTimeSeries() {
    ArrayList<TimeSeries> list = new ArrayList<TimeSeries>();

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
    if (pendingTxnsSeries != null) {
      list.add(pendingTxnsSeries);
      pendingTxnsSeries = null;
    }

    Iterator<TimeSeries> iter = list.iterator();
    while (iter.hasNext()) {
      iter.next().clear();
    }

    if (cpuDataset != null) {
      cpuDataset.clear();
    }
  }

  private final AtomicBoolean tornDown = new AtomicBoolean(false);

  @Override
  public synchronized void tearDown() {
    if (!tornDown.compareAndSet(false, true)) { return; }

    stopMonitoringRuntimeStats();
    client = null;

    super.tearDown();

    clearAllTimeSeries();
    cpuPanel = null;
  }
}
