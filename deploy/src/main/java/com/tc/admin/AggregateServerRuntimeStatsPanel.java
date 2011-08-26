/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import static com.tc.admin.model.IClusterModel.PollScope.ACTIVE_SERVERS;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_LIVE_OBJECT_COUNT;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FAULT_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FLUSH_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_TRANSACTION_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_BROADCAST_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_CACHED_OBJECT_COUNT;
import static com.tc.admin.model.IServer.POLLED_ATTR_LOCK_RECALL_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_OFFHEAP_FAULT_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_OFFHEAP_FLUSH_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_OFFHEAP_OBJECT_CACHED_COUNT;
import static com.tc.admin.model.IServer.POLLED_ATTR_ONHEAP_FAULT_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_ONHEAP_FLUSH_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.ui.Layer;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.dso.DGCIntervalMarker;
import com.tc.admin.model.DGCListener;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.objectserver.api.GCStats;

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

public class AggregateServerRuntimeStatsPanel extends BaseRuntimeStatsPanel implements DGCListener {
  private final IClusterModel      clusterModel;
  private ClusterListener          clusterListener;

  private TimeSeries               clientFlushRateSeries;
  private XLabel                   clientFlushRateLabel;
  private TimeSeries               clientFaultRateSeries;
  private XLabel                   clientFaultRateLabel;
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
  private XYPlot                   liveObjectCountPlot;
  private DGCIntervalMarker        currentDGCMarker;
  private String                   objectManagerTitlePattern;
  private TitledBorder             objectManagerTitle;
  private TimeSeries               liveObjectCountSeries;
  private TimeSeries               cachedObjectCountSeries;
  private TimeSeries               offHeapObjectCountSeries;
  private TimeSeries               lockRecallRateSeries;
  private StatusView               lockRecallRateLabel;
  private TimeSeries               broadcastRateSeries;
  private StatusView               broadcastRateLabel;

  protected final String           clientFlushRateLabelFormat  = "{0} Flushes/sec.";
  protected final String           clientFaultRateLabelFormat  = "{0} Faults/sec.";
  protected final String           txnRateLabelFormat          = "{0} Txns/sec.";
  protected final String           onHeapFaultRateLabelFormat  = "{0} OnHeap Faults/sec.";
  protected final String           onHeapFlushRateLabelFormat  = "{0} OnHeap Flushes/sec.";
  protected final String           offHeapFaultRateLabelFormat = "{0} OffHeap Faults/sec.";
  protected final String           offHeapFlushRateLabelFormat = "{0} OffHeap Flushes/sec.";
  protected final String           lockRecallRateLabelFormat   = "{0} Recalls/sec.";
  protected final String           broadcastRateLabelFormat    = "{0} Broadcasts/sec.";

  private static final Set<String> POLLED_ATTRIBUTE_SET        = new HashSet(
                                                                             Arrays
                                                                                 .asList(POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                         POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                         POLLED_ATTR_TRANSACTION_RATE,
                                                                                         POLLED_ATTR_ONHEAP_FAULT_RATE,
                                                                                         POLLED_ATTR_ONHEAP_FLUSH_RATE,
                                                                                         POLLED_ATTR_OFFHEAP_FAULT_RATE,
                                                                                         POLLED_ATTR_OFFHEAP_FLUSH_RATE,
                                                                                         POLLED_ATTR_LIVE_OBJECT_COUNT,
                                                                                         POLLED_ATTR_LOCK_RECALL_RATE,
                                                                                         POLLED_ATTR_BROADCAST_RATE,
                                                                                         POLLED_ATTR_CACHED_OBJECT_COUNT,
                                                                                         POLLED_ATTR_OFFHEAP_OBJECT_CACHED_COUNT));

  public AggregateServerRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(appContext);
    this.clusterModel = clusterModel;
    setup(chartsPanel);
    setName(clusterModel.getName());
    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      IServer activeCoord = clusterModel.getActiveCoordinator();
      if (activeCoord != null) {
        activeCoord.addDGCListener(this);
        startMonitoringRuntimeStats();
      }
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    public void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeDGCListener(AggregateServerRuntimeStatsPanel.this);
      }
      if (newActive != null) {
        newActive.addDGCListener(AggregateServerRuntimeStatsPanel.this);
      }
    }

    @Override
    public void handleReady() {
      if (clusterModel.isReady()) {
        startMonitoringRuntimeStats();
      } else {
        stopMonitoringRuntimeStats();
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      if (appContext != null) {
        appContext.log(e);
      } else {
        super.handleUncaughtError(e);
      }
    }
  }

  private void addPolledAttributeListener() {
    clusterModel.addPolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTRIBUTE_SET, this);
  }

  private void removePolledAttributeListener() {
    clusterModel.removePolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTRIBUTE_SET, this);
  }

  @Override
  public void startMonitoringRuntimeStats() {
    if (clusterModel.isReady()) {
      addPolledAttributeListener();
      super.startMonitoringRuntimeStats();
    }
  }

  @Override
  public void stopMonitoringRuntimeStats() {
    removePolledAttributeListener();
    super.stopMonitoringRuntimeStats();
  }

  @Override
  public void attributesPolled(final PolledAttributesResult result) {
    handleDSOStats(result);
  }

  private Object getPolledAttribute(PolledAttributesResult par, IServer server, String attr) {
    return par.getPolledAttribute(server, attr);
  }

  private void handleDSOStats(PolledAttributesResult result) {
    tmpDate.setTime(System.currentTimeMillis());

    long flush = 0;
    long fault = 0;
    long txn = 0;
    long onHeapFaultRate = 0;
    long onHeapFlushRate = 0;
    long offHeapFaultRate = 0;
    long offHeapFlushRate = 0;
    long liveObjectCount = 0;
    long cachedObjectCount = 0;
    long offHeapObjectCount = 0;
    long lockRecallRate = 0;
    long broadcastRate = 0;
    Number n;

    for (IServerGroup group : clusterModel.getServerGroups()) {
      IServer theServer = group.getActiveServer();
      if (theServer != null && theServer.isReady()) {
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OBJECT_FLUSH_RATE);
        if (n != null) {
          flush += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OBJECT_FAULT_RATE);
        if (n != null) {
          fault += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_TRANSACTION_RATE);
        if (n != null) {
          txn += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_ONHEAP_FAULT_RATE);
        if (n != null) {
          onHeapFaultRate += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_ONHEAP_FLUSH_RATE);
        if (n != null) {
          onHeapFlushRate += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OFFHEAP_FAULT_RATE);
        if (n != null) {
          offHeapFaultRate += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OFFHEAP_FLUSH_RATE);
        if (n != null) {
          offHeapFlushRate += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_LIVE_OBJECT_COUNT);
        if (n != null) {
          liveObjectCount += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_CACHED_OBJECT_COUNT);
        if (n != null) {
          cachedObjectCount += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OFFHEAP_OBJECT_CACHED_COUNT);
        if (n != null) {
          offHeapObjectCount += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_LOCK_RECALL_RATE);
        if (n != null) {
          lockRecallRate += n.longValue();
        }
        n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_BROADCAST_RATE);
        if (n != null) {
          broadcastRate += n.longValue();
        }
      }
    }

    final long theFlush = flush;
    final long theFault = fault;
    final long theTxn = txn;
    final long theOnHeapFaultRate = onHeapFaultRate;
    final long theOnHeapFlushRate = onHeapFlushRate;
    final long theOffHeapFaultRate = offHeapFaultRate;
    final long theOffHeapFlushRate = offHeapFlushRate;
    final long theLiveObjectCount = liveObjectCount;
    final long theCachedObjectCount = cachedObjectCount;
    final long theOffHeapObjectCount = offHeapObjectCount;
    final long theLockRecallRate = lockRecallRate;
    final long theBroadcastRate = broadcastRate;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        {
          updateSeries(clientFlushRateSeries, Long.valueOf(theFlush));
          clientFlushRateLabel.setText(MessageFormat.format(clientFlushRateLabelFormat, convert(theFlush)));
        }
        {
          updateSeries(clientFaultRateSeries, Long.valueOf(theFault));
          clientFaultRateLabel.setText(MessageFormat.format(clientFaultRateLabelFormat, convert(theFault)));
        }
        {
          updateSeries(txnRateSeries, Long.valueOf(theTxn));
          txnRateLabel.setText(MessageFormat.format(txnRateLabelFormat, convert(theTxn)));
        }
        {
          updateSeries(onHeapFaultRateSeries, Long.valueOf(theOnHeapFaultRate));
          onHeapFaultRateLabel.setText(MessageFormat.format(onHeapFaultRateLabelFormat, convert(theOnHeapFaultRate)));
        }
        {
          updateSeries(onHeapFlushRateSeries, Long.valueOf(theOnHeapFlushRate));
          onHeapFlushRateLabel.setText(MessageFormat.format(onHeapFlushRateLabelFormat, convert(theOnHeapFlushRate)));
        }
        {
          updateSeries(offHeapFaultRateSeries, Long.valueOf(theOffHeapFaultRate));
          offHeapFaultRateLabel.setText(MessageFormat.format(offHeapFaultRateLabelFormat, convert(theOffHeapFaultRate)));
        }
        {
          updateSeries(offHeapFlushRateSeries, Long.valueOf(theOffHeapFlushRate));
          offHeapFlushRateLabel.setText(MessageFormat.format(offHeapFlushRateLabelFormat, convert(theOffHeapFlushRate)));
        }
        {
          updateSeries(liveObjectCountSeries, Long.valueOf(theLiveObjectCount));
          String cached = convert(theCachedObjectCount);
          String offHeap = theOffHeapObjectCount != -1 ? convert(theOffHeapObjectCount) : "n/a";
          String live = convert(theLiveObjectCount);
          objectManagerTitle.setTitle(MessageFormat.format(objectManagerTitlePattern, cached, offHeap, live));
        }
        {
          updateSeries(cachedObjectCountSeries, Long.valueOf(theCachedObjectCount));
        }
        {
          updateSeries(offHeapObjectCountSeries, Long.valueOf(theOffHeapObjectCount));
        }
        {
          updateSeries(lockRecallRateSeries, Long.valueOf(theLockRecallRate));
          lockRecallRateLabel.setText(MessageFormat.format(lockRecallRateLabelFormat, convert(theLockRecallRate)));
        }
        {
          updateSeries(broadcastRateSeries, Long.valueOf(theBroadcastRate));
          broadcastRateLabel.setText(MessageFormat.format(broadcastRateLabelFormat, convert(theBroadcastRate)));
        }
      }
    });
  }

  @Override
  protected void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.weightx = gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    setupTxnRatePanel(chartsPanel, gbc);
    gbc.gridx++;
    setupLockRecallRatePanel(chartsPanel, gbc);
    gbc.gridx--;
    gbc.gridy++;
    setupOnHeapFaultFlushPanel(chartsPanel, gbc);
    gbc.gridx++;
    setupOffHeapFaultFlushPanel(chartsPanel, gbc);
    gbc.gridx--;
    gbc.gridy++;
    setupFlushRatePanel(chartsPanel, gbc);
    gbc.gridx++;
    setupFaultRatePanel(chartsPanel, gbc);
    gbc.gridx--;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    setupObjectManagerPanel(chartsPanel, gbc);
  }

  private void setupObjectManagerPanel(XContainer parent, GridBagConstraints gbc) {
    liveObjectCountSeries = createTimeSeries("Live Object Count");
    cachedObjectCountSeries = createTimeSeries("Cached Object Count");
    offHeapObjectCountSeries = createTimeSeries("OffHeap Object Count");
    JFreeChart chart = createChart(new TimeSeries[] { cachedObjectCountSeries, offHeapObjectCountSeries,
        liveObjectCountSeries }, true);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel, gbc);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    String liveObjectCountLabel = appContext.getString("live.object.count");
    objectManagerTitlePattern = liveObjectCountLabel + " (caching: {0}, offheap: {1}, total: {2})";
    objectManagerTitle = BorderFactory.createTitledBorder(liveObjectCountLabel);
    chartPanel.setBorder(objectManagerTitle);
    chartPanel.setToolTipText("Cached/OffHeap/Total instance counts");
    liveObjectCountPlot = (XYPlot) chart.getPlot();
    XYAreaRenderer areaRenderer2 = new XYAreaRenderer(XYAreaRenderer.AREA,
                                                      StandardXYToolTipGenerator.getTimeSeriesInstance(), null);
    liveObjectCountPlot.setRenderer(areaRenderer2);
    areaRenderer2.setSeriesPaint(0, (Color) appContext.getObject("chart.color.2"));
    areaRenderer2.setSeriesPaint(1, (Color) appContext.getObject("chart.color.3"));
    areaRenderer2.setSeriesPaint(2, (Color) appContext.getObject("chart.color.1"));
  }

  private void setupLockRecallRatePanel(XContainer parent, GridBagConstraints gbc) {
    lockRecallRateSeries = createTimeSeries("Lock Recall Rate");
    broadcastRateSeries = createTimeSeries("Change Broadcast Rate");
    JFreeChart chart = createChart(new TimeSeries[] { lockRecallRateSeries, broadcastRateSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeMinimumSize(10.0);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel, gbc);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder("Lock Recalls/Change Broadcasts"));
    chartPanel.setToolTipText("Global Lock Recalls");
    chartPanel.setLayout(new GridBagLayout());

    Color color1 = (Color) appContext.getObject("chart.color.1");
    Color color2 = (Color) appContext.getObject("chart.color.2");

    XYItemRenderer renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, color1);
    renderer.setSeriesPaint(1, color2);

    GridBagConstraints waterMarkConstraint = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(lockRecallRateLabel = createStatusLabel(color1));
    labelHolder.add(broadcastRateLabel = createStatusLabel(color2));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, waterMarkConstraint);
  }

  private void setupFlushRatePanel(XContainer parent, GridBagConstraints gbc) {
    clientFlushRateSeries = createTimeSeries("");
    JFreeChart chart = createChart(clientFlushRateSeries, false);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel, gbc);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.flush.rate")));
    chartPanel.setToolTipText(appContext.getString("aggregate.server.stats.flush.rate.tip"));
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(clientFlushRateLabel = createOverlayLabel());

    XYPlot plot = (XYPlot) chart.getPlot();
    XYItemRenderer renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, (Color) appContext.getObject("chart.color.1"));
  }

  private void setupFaultRatePanel(XContainer parent, GridBagConstraints gbc) {
    clientFaultRateSeries = createTimeSeries("");
    JFreeChart chart = createChart(clientFaultRateSeries, false);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel, gbc);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.fault.rate")));
    chartPanel.setToolTipText(appContext.getString("aggregate.server.stats.fault.rate.tip"));
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(clientFaultRateLabel = createOverlayLabel());

    XYPlot plot = (XYPlot) chart.getPlot();
    XYItemRenderer renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, (Color) appContext.getObject("chart.color.1"));
  }

  private void setupTxnRatePanel(XContainer parent, GridBagConstraints gbc) {
    txnRateSeries = createTimeSeries("");
    JFreeChart chart = createChart(txnRateSeries, false);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel, gbc);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.transaction.rate")));
    chartPanel.setToolTipText(appContext.getString("aggregate.server.stats.transaction.rate.tip"));
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(txnRateLabel = createOverlayLabel());

    XYPlot plot = (XYPlot) chart.getPlot();
    XYItemRenderer renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, (Color) appContext.getObject("chart.color.1"));
  }

  private void setupOnHeapFaultFlushPanel(XContainer parent, GridBagConstraints gbc) {
    onHeapFaultRateSeries = createTimeSeries(appContext.getString("dso.onheap.fault.rate"));
    onHeapFlushRateSeries = createTimeSeries(appContext.getString("dso.onheap.flush.rate"));
    JFreeChart chart = createChart(new TimeSeries[] { onHeapFaultRateSeries, onHeapFlushRateSeries }, false);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel, gbc);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.onheap.flushfault")));
    chartPanel.setToolTipText(appContext.getString("aggregate.server.stats.onheap.flushfault.tip"));
    chartPanel.setLayout(new GridBagLayout());

    Color color1 = (Color) appContext.getObject("chart.color.1");
    Color color2 = (Color) appContext.getObject("chart.color.2");

    XYPlot plot = (XYPlot) chart.getPlot();
    XYItemRenderer renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, color1);
    renderer.setSeriesPaint(1, color2);

    GridBagConstraints waterMarkConstraint = new GridBagConstraints();
    waterMarkConstraint.anchor = GridBagConstraints.WEST;
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(onHeapFaultRateLabel = createStatusLabel(color1));
    labelHolder.add(onHeapFlushRateLabel = createStatusLabel(color2));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, waterMarkConstraint);
  }

  private void setupOffHeapFaultFlushPanel(XContainer parent, GridBagConstraints gbc) {
    offHeapFaultRateSeries = createTimeSeries(appContext.getString("dso.offheap.fault.rate"));
    offHeapFlushRateSeries = createTimeSeries(appContext.getString("dso.offheap.flush.rate"));
    JFreeChart chart = createChart(new TimeSeries[] { offHeapFaultRateSeries, offHeapFlushRateSeries }, false);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel, gbc);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.offheap.flushfault")));
    chartPanel.setToolTipText(appContext.getString("aggregate.server.stats.offheap.flushfault.tip"));
    chartPanel.setLayout(new GridBagLayout());

    Color color1 = (Color) appContext.getObject("chart.color.1");
    Color color2 = (Color) appContext.getObject("chart.color.2");

    XYPlot plot = (XYPlot) chart.getPlot();
    XYItemRenderer renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, color1);
    renderer.setSeriesPaint(1, color2);

    GridBagConstraints waterMarkConstraint = new GridBagConstraints();
    waterMarkConstraint.anchor = GridBagConstraints.WEST;
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(offHeapFaultRateLabel = createStatusLabel(color1));
    labelHolder.add(offHeapFlushRateLabel = createStatusLabel(color2));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, waterMarkConstraint);
  }

  public void statusUpdate(GCStats gcStats) {
    SwingUtilities.invokeLater(new ModelUpdater(gcStats));
  }

  private class ModelUpdater implements Runnable {
    private final GCStats gcStats;

    private ModelUpdater(GCStats gcStats) {
      this.gcStats = gcStats;
    }

    public void run() {
      if (currentDGCMarker == null) {
        currentDGCMarker = new DGCIntervalMarker(gcStats);
        liveObjectCountPlot.addDomainMarker(currentDGCMarker, Layer.FOREGROUND);
      } else {
        currentDGCMarker.setGCStats(gcStats);
      }
      if (gcStats.getStatus().equals(GCStats.GC_COMPLETE.getName())
          || gcStats.getStatus().equals(GCStats.GC_CANCELED.getName())) {
        currentDGCMarker = null;
      }
    }
  }

  private void clearAllTimeSeries() {
    if (clientFlushRateSeries != null) {
      clientFlushRateSeries.clear();
    }
    if (clientFaultRateSeries != null) {
      clientFaultRateSeries.clear();
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
    if (offHeapFaultRateSeries != null) {
      offHeapFaultRateSeries.clear();
    }
    if (offHeapFlushRateSeries != null) {
      offHeapFlushRateSeries.clear();
    }
    if (liveObjectCountSeries != null) {
      liveObjectCountSeries.clear();
    }
    if (offHeapObjectCountSeries != null) {
      offHeapObjectCountSeries.clear();
    }
    if (cachedObjectCountSeries != null) {
      cachedObjectCountSeries.clear();
    }
    if (lockRecallRateSeries != null) {
      lockRecallRateSeries.clear();
    }
    if (broadcastRateSeries != null) {
      broadcastRateSeries.clear();
    }
  }

  @Override
  public void tearDown() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeDGCListener(this);
    }
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    stopMonitoringRuntimeStats();
    clearAllTimeSeries();

    super.tearDown();
  }
}
