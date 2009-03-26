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
import static com.tc.admin.model.IServer.POLLED_ATTR_CACHE_MISS_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_LOCK_RECALL_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
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

  private TimeSeries               flushRateSeries;
  private TimeSeries               faultRateSeries;
  private TimeSeries               txnRateSeries;
  private TimeSeries               cacheMissRateSeries;
  private XYPlot                   liveObjectCountPlot;
  private DGCIntervalMarker        currentDGCMarker;
  private String                   liveObjectCountTitlePattern;
  private TitledBorder             liveObjectCountTitle;
  private TimeSeries               liveObjectCountSeries;
  private TimeSeries               lockRecallRateSeries;
  private TimeSeries               broadcastRateSeries;

  private static final Set<String> POLLED_ATTRIBUTE_SET = new HashSet(Arrays.asList(POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                    POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                    POLLED_ATTR_TRANSACTION_RATE,
                                                                                    POLLED_ATTR_CACHE_MISS_RATE,
                                                                                    POLLED_ATTR_LIVE_OBJECT_COUNT,
                                                                                    POLLED_ATTR_LOCK_RECALL_RATE,
                                                                                    POLLED_ATTR_BROADCAST_RATE));

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
      long liveObjectCount = 0;
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

      if (flush != -1) updateSeries(flushRateSeries, Long.valueOf(flush));
      if (fault != -1) updateSeries(faultRateSeries, Long.valueOf(fault));
      if (txn != -1) updateSeries(txnRateSeries, Long.valueOf(txn));
      if (cacheMiss != -1) updateSeries(cacheMissRateSeries, Long.valueOf(cacheMiss));
      if (liveObjectCount != -1) {
        updateSeries(liveObjectCountSeries, Long.valueOf(liveObjectCount));
        liveObjectCountTitle.setTitle(MessageFormat.format(liveObjectCountTitlePattern, liveObjectCount));
      }
      if (lockRecallRate != -1) updateSeries(lockRecallRateSeries, Long.valueOf(lockRecallRate));
      if (broadcastRate != -1) updateSeries(broadcastRateSeries, Long.valueOf(broadcastRate));
    }
  }

  @Override
  protected synchronized void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(0, 2));
    setupTxnRatePanel(chartsPanel);
    setupCacheMissRatePanel(chartsPanel);
    setupFlushRatePanel(chartsPanel);
    setupFaultRatePanel(chartsPanel);
    setupLiveObjectCountPanel(chartsPanel);
    setupLockRecallRatePanel(chartsPanel);
  }

  private void setupLiveObjectCountPanel(XContainer parent) {
    liveObjectCountSeries = createTimeSeries("LiveObjectCount");
    JFreeChart chart = createChart(liveObjectCountSeries, false);
    ChartPanel liveObjectCountPanel = createChartPanel(chart);
    parent.add(liveObjectCountPanel);
    liveObjectCountPanel.setPreferredSize(fDefaultGraphSize);
    liveObjectCountTitlePattern = appContext.getString("dso.client.liveObjectCount") + " ({0})";
    liveObjectCountTitle = BorderFactory.createTitledBorder("Live Object Count");
    liveObjectCountPanel.setBorder(liveObjectCountTitle);
    liveObjectCountPanel.setToolTipText("Total instance count");
    liveObjectCountPlot = (XYPlot) chart.getPlot();
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
    flushRateSeries = createTimeSeries("");
    ChartPanel flushRatePanel = createChartPanel(createChart(flushRateSeries, false));
    parent.add(flushRatePanel);
    flushRatePanel.setPreferredSize(fDefaultGraphSize);
    flushRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.flush.rate")));
    flushRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.flush.rate.tip"));
  }

  private void setupFaultRatePanel(XContainer parent) {
    faultRateSeries = createTimeSeries("");
    ChartPanel faultRatePanel = createChartPanel(createChart(faultRateSeries, false));
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

  private void setupCacheMissRatePanel(XContainer parent) {
    cacheMissRateSeries = createTimeSeries("");
    ChartPanel cacheMissRatePanel = createChartPanel(createChart(cacheMissRateSeries, false));
    parent.add(cacheMissRatePanel);
    cacheMissRatePanel.setPreferredSize(fDefaultGraphSize);
    cacheMissRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.cache.miss.rate")));
    cacheMissRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.cache.miss.rate.tip"));
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
        liveObjectCountPlot.addDomainMarker(currentDGCMarker, Layer.BACKGROUND);
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
    if (liveObjectCountSeries != null) {
      list.add(liveObjectCountSeries);
      liveObjectCountSeries = null;
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
