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
import static com.tc.admin.model.IServer.POLLED_ATTR_CACHE_MISS_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_FLUSHED_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_LOCK_RECALL_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.ui.Layer;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.dso.DGCIntervalMarker;
import com.tc.admin.model.DGCListener;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.objectserver.api.GCStats;

import java.awt.GridLayout;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class AggregateServerRuntimeStatsPanel extends BaseRuntimeStatsPanel implements DGCListener {
  private IClusterModel            clusterModel;
  private ClusterListener          clusterListener;

  private TimeSeries               clientFlushRateSeries;
  private TimeSeries               clientFaultRateSeries;
  private TimeSeries               txnRateSeries;
  private TimeSeries               cacheMissRateSeries;
  private TimeSeries               diskFlushedRateSeries;
  private XYPlot                   liveObjectCountPlot;
  private DGCIntervalMarker        currentDGCMarker;
  private String                   objectManagerTitlePattern;
  private TitledBorder             objectManagerTitle;
  private TimeSeries               liveObjectCountSeries;
  private TimeSeries               cachedObjectCountSeries;
  private TimeSeries               lockRecallRateSeries;
  private TimeSeries               broadcastRateSeries;

  private static final Set<String> POLLED_ATTRIBUTE_SET = new HashSet(Arrays.asList(POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                    POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                    POLLED_ATTR_TRANSACTION_RATE,
                                                                                    POLLED_ATTR_CACHE_MISS_RATE,
                                                                                    POLLED_ATTR_FLUSHED_RATE,
                                                                                    POLLED_ATTR_LIVE_OBJECT_COUNT,
                                                                                    POLLED_ATTR_LOCK_RECALL_RATE,
                                                                                    POLLED_ATTR_BROADCAST_RATE,
                                                                                    POLLED_ATTR_CACHED_OBJECT_COUNT));

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
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      if (clusterModel.isReady()) {
        startMonitoringRuntimeStats();
      } else {
        stopMonitoringRuntimeStats();
      }
    }
  }

  synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  private void addPolledAttributeListener() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      theClusterModel.addPolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTRIBUTE_SET, this);
    }
  }

  private void removePolledAttributeListener() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      theClusterModel.removePolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTRIBUTE_SET, this);
    }
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
  public void attributesPolled(PolledAttributesResult result) {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      handleDSOStats(result);
    }
  }

  private Object getPolledAttribute(PolledAttributesResult par, IServer server, String attr) {
    Object result = par.getPolledAttribute(server, attr);
    if (result == null) {
      appContext.log("No poll result for " + server + ": " + attr);
    }
    return result;
  }

  private synchronized void handleDSOStats(PolledAttributesResult result) {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      tmpDate.setTime(System.currentTimeMillis());

      long flush = 0;
      long fault = 0;
      long txn = 0;
      long cacheMiss = 0;
      long diskFlushedRate = 0;
      long liveObjectCount = 0;
      long cachedObjectCount = 0;
      long lockRecallRate = 0;
      long broadcastRate = 0;
      Number n;

      for (IServerGroup group : theClusterModel.getServerGroups()) {
        IServer theServer = group.getActiveServer();
        if (theServer.isReady()) {
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OBJECT_FLUSH_RATE);
          if (n != null) {
            if (flush >= 0) flush += n.longValue();
          } else {
            flush = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OBJECT_FAULT_RATE);
          if (n != null) {
            if (fault >= 0) fault += n.longValue();
          } else {
            fault = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_TRANSACTION_RATE);
          if (n != null) {
            if (txn >= 0) txn += n.longValue();
          } else {
            txn = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_FLUSHED_RATE);
          if (n != null) {
            if (diskFlushedRate >= 0) diskFlushedRate += n.longValue();
          } else {
            diskFlushedRate = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_CACHE_MISS_RATE);
          if (n != null) {
            if (cacheMiss >= 0) cacheMiss += n.longValue();
          } else {
            cacheMiss = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_LIVE_OBJECT_COUNT);
          if (n != null) {
            if (liveObjectCount >= 0) liveObjectCount += n.longValue();
          } else {
            liveObjectCount = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_CACHED_OBJECT_COUNT);
          if (n != null) {
            if (cachedObjectCount >= 0) cachedObjectCount += n.longValue();
          } else {
            cachedObjectCount = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_LOCK_RECALL_RATE);
          if (n != null) {
            if (lockRecallRate >= 0) lockRecallRate += n.longValue();
          } else {
            lockRecallRate = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_BROADCAST_RATE);
          if (n != null) {
            if (broadcastRate >= 0) broadcastRate += n.longValue();
          } else {
            broadcastRate = -1;
          }
        }
      }

      if (flush != -1) updateSeries(clientFlushRateSeries, Long.valueOf(flush));
      if (fault != -1) updateSeries(clientFaultRateSeries, Long.valueOf(fault));
      if (txn != -1) updateSeries(txnRateSeries, Long.valueOf(txn));
      if (cacheMiss != -1) updateSeries(cacheMissRateSeries, Long.valueOf(cacheMiss));
      if (diskFlushedRate != -1) updateSeries(diskFlushedRateSeries, Long.valueOf(diskFlushedRate));
      if (liveObjectCount != -1) {
        updateSeries(liveObjectCountSeries, Long.valueOf(liveObjectCount));
        objectManagerTitle
            .setTitle(MessageFormat.format(objectManagerTitlePattern, cachedObjectCount, liveObjectCount));
      }
      if (cachedObjectCount != -1) {
        updateSeries(cachedObjectCountSeries, Long.valueOf(cachedObjectCount));
      }
      if (lockRecallRate != -1) updateSeries(lockRecallRateSeries, Long.valueOf(lockRecallRate));
      if (broadcastRate != -1) updateSeries(broadcastRateSeries, Long.valueOf(broadcastRate));
    }
  }

  @Override
  protected synchronized void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(0, 2));
    setupTxnRatePanel(chartsPanel);
    setupCacheManagerPanel(chartsPanel);
    setupFlushRatePanel(chartsPanel);
    setupFaultRatePanel(chartsPanel);
    setupObjectManagerPanel(chartsPanel);
    setupLockRecallRatePanel(chartsPanel);
  }

  private void setupObjectManagerPanel(XContainer parent) {
    liveObjectCountSeries = createTimeSeries("Live Object Count");
    cachedObjectCountSeries = createTimeSeries("Cached Object Count");
    JFreeChart chart = createChart(new TimeSeries[] { cachedObjectCountSeries, liveObjectCountSeries }, true);
    ChartPanel liveObjectCountPanel = createChartPanel(chart);
    parent.add(liveObjectCountPanel);
    liveObjectCountPanel.setPreferredSize(fDefaultGraphSize);
    objectManagerTitlePattern = appContext.getString("dso.cluster.objectManager") + " (caching {0} of {1} instances)";
    objectManagerTitle = BorderFactory.createTitledBorder("Object Manager");
    liveObjectCountPanel.setBorder(objectManagerTitle);
    liveObjectCountPanel.setToolTipText("Total/Cached instance counts");
    liveObjectCountPlot = (XYPlot) chart.getPlot();
    XYAreaRenderer areaRenderer2 = new XYAreaRenderer(XYAreaRenderer.AREA, StandardXYToolTipGenerator
        .getTimeSeriesInstance(), null);
    liveObjectCountPlot.setRenderer(0, areaRenderer2);
  }

  private void setupLockRecallRatePanel(XContainer parent) {
    lockRecallRateSeries = createTimeSeries("Lock Recall Rate");
    broadcastRateSeries = createTimeSeries("Change Broadcast Rate");
    JFreeChart chart = createChart(new TimeSeries[] { lockRecallRateSeries, broadcastRateSeries }, true);
    XYPlot plot = (XYPlot) chart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeMinimumSize(10.0);
    ChartPanel recallRatePanel = createChartPanel(chart);
    parent.add(recallRatePanel);
    recallRatePanel.setPreferredSize(fDefaultGraphSize);
    recallRatePanel.setBorder(new TitledBorder("Lock Recalls/Change Broadcasts"));
    recallRatePanel.setToolTipText("Global Lock Recalls");
  }

  private void setupFlushRatePanel(XContainer parent) {
    clientFlushRateSeries = createTimeSeries("");
    ChartPanel flushRatePanel = createChartPanel(createChart(clientFlushRateSeries, false));
    parent.add(flushRatePanel);
    flushRatePanel.setPreferredSize(fDefaultGraphSize);
    flushRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.flush.rate")));
    flushRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.flush.rate.tip"));
  }

  private void setupFaultRatePanel(XContainer parent) {
    clientFaultRateSeries = createTimeSeries("");
    ChartPanel faultRatePanel = createChartPanel(createChart(clientFaultRateSeries, false));
    parent.add(faultRatePanel);
    faultRatePanel.setPreferredSize(fDefaultGraphSize);
    faultRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.fault.rate")));
    faultRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.fault.rate.tip"));
  }

  private void setupTxnRatePanel(XContainer parent) {
    txnRateSeries = createTimeSeries("");
    ChartPanel txnRatePanel = createChartPanel(createChart(txnRateSeries, false));
    parent.add(txnRatePanel);
    txnRatePanel.setPreferredSize(fDefaultGraphSize);
    txnRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.transaction.rate")));
    txnRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.transaction.rate.tip"));
  }

  private void setupCacheManagerPanel(XContainer parent) {
    cacheMissRateSeries = createTimeSeries("Cache Miss Rate");
    diskFlushedRateSeries = createTimeSeries("Disk Flushed Rate");
    ChartPanel cacheMissRatePanel = createChartPanel(createChart(new TimeSeries[] { cacheMissRateSeries,
        diskFlushedRateSeries }, true));
    parent.add(cacheMissRatePanel);
    cacheMissRatePanel.setPreferredSize(fDefaultGraphSize);
    cacheMissRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.cache-manager")));
    cacheMissRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.cache-manager.tip"));
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
      if (gcStats.getElapsedTime() != -1) {
        currentDGCMarker = null;
      }
    }
  }

  private void clearAllTimeSeries() {
    ArrayList<TimeSeries> list = new ArrayList<TimeSeries>();
    if (clientFlushRateSeries != null) {
      list.add(clientFlushRateSeries);
      clientFlushRateSeries = null;
    }
    if (clientFaultRateSeries != null) {
      list.add(clientFaultRateSeries);
      clientFaultRateSeries = null;
    }
    if (txnRateSeries != null) {
      list.add(txnRateSeries);
      txnRateSeries = null;
    }
    if (cacheMissRateSeries != null) {
      list.add(cacheMissRateSeries);
      cacheMissRateSeries = null;
    }
    if (diskFlushedRateSeries != null) {
      list.add(diskFlushedRateSeries);
      diskFlushedRateSeries = null;
    }
    if (liveObjectCountSeries != null) {
      list.add(liveObjectCountSeries);
      liveObjectCountSeries = null;
    }
    if (cachedObjectCountSeries != null) {
      list.add(cachedObjectCountSeries);
      cachedObjectCountSeries = null;
    }
    if (lockRecallRateSeries != null) {
      list.add(lockRecallRateSeries);
      lockRecallRateSeries = null;
    }
    if (broadcastRateSeries != null) {
      list.add(broadcastRateSeries);
      broadcastRateSeries = null;
    }

    Iterator<TimeSeries> iter = list.iterator();
    while (iter.hasNext()) {
      iter.next().clear();
    }
  }

  @Override
  public synchronized void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    stopMonitoringRuntimeStats();
    clusterModel = null;
    clusterListener = null;
    liveObjectCountPlot = null;
    super.tearDown();
    clearAllTimeSeries();
  }
}
