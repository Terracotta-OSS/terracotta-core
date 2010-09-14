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
import static com.tc.admin.model.IServer.POLLED_ATTR_FAULTED_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_FLUSHED_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_LOCK_RECALL_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_OFFHEAP_OBJECT_CACHED_COUNT;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
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
  private XLabel                   clientFlushRateLabel;
  private TimeSeries               clientFaultRateSeries;
  private XLabel                   clientFaultRateLabel;
  private TimeSeries               txnRateSeries;
  private XLabel                   txnRateLabel;
  private TimeSeries               diskFaultRateSeries;
  private StatusView               diskFaultRateLabel;
  private TimeSeries               diskFlushRateSeries;
  private StatusView               diskFlushRateLabel;
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

  protected final String           clientFlushRateLabelFormat = "{0,number,integer} Flushes/sec.";
  protected final String           clientFaultRateLabelFormat = "{0,number,integer} Faults/sec.";
  protected final String           txnRateLabelFormat         = "{0,number,integer} Txns/sec.";
  protected final String           diskFaultRateLabelFormat   = "{0,number,integer} Faults/sec.";
  protected final String           diskFlushRateLabelFormat   = "{0,number,integer} Flushes/sec.";
  protected final String           lockRecallRateLabelFormat  = "{0,number,integer} Recalls/sec.";
  protected final String           broadcastRateLabelFormat   = "{0,number,integer} Broadcasts/sec.";

  private static final Set<String> POLLED_ATTRIBUTE_SET       = new HashSet(
                                                                            Arrays
                                                                                .asList(POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                        POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                        POLLED_ATTR_TRANSACTION_RATE,
                                                                                        POLLED_ATTR_CACHE_MISS_RATE,
                                                                                        POLLED_ATTR_FAULTED_RATE,
                                                                                        POLLED_ATTR_FLUSHED_RATE,
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
  public void attributesPolled(final PolledAttributesResult result) {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          handleDSOStats(result);
        }
      });
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
      long diskFaultedRate = 0;
      long diskFlushedRate = 0;
      long liveObjectCount = 0;
      long cachedObjectCount = 0;
      long offHeapObjectCount = 0;
      long lockRecallRate = 0;
      long broadcastRate = 0;
      Number n;

      for (IServerGroup group : theClusterModel.getServerGroups()) {
        IServer theServer = group.getActiveServer();
        if (theServer.isReady()) {
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OBJECT_FLUSH_RATE);
          if (n != null) {
            if (flush >= 0) {
              flush += n.longValue();
            }
          } else {
            flush = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OBJECT_FAULT_RATE);
          if (n != null) {
            if (fault >= 0) {
              fault += n.longValue();
            }
          } else {
            fault = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_TRANSACTION_RATE);
          if (n != null) {
            if (txn >= 0) {
              txn += n.longValue();
            }
          } else {
            txn = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_FAULTED_RATE);
          if (n != null) {
            if (diskFaultedRate >= 0) {
              diskFaultedRate += n.longValue();
            }
          } else {
            diskFaultedRate = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_FLUSHED_RATE);
          if (n != null) {
            if (diskFlushedRate >= 0) {
              diskFlushedRate += n.longValue();
            }
          } else {
            diskFlushedRate = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_CACHE_MISS_RATE);
          if (n != null) {
            if (cacheMiss >= 0) {
              cacheMiss += n.longValue();
            }
          } else {
            cacheMiss = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_LIVE_OBJECT_COUNT);
          if (n != null) {
            if (liveObjectCount >= 0) {
              liveObjectCount += n.longValue();
            }
          } else {
            liveObjectCount = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_CACHED_OBJECT_COUNT);
          if (n != null) {
            if (cachedObjectCount >= 0) {
              cachedObjectCount += n.longValue();
            }
          } else {
            cachedObjectCount = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OFFHEAP_OBJECT_CACHED_COUNT);
          if (n != null) {
            if (offHeapObjectCount >= 0) {
              offHeapObjectCount += n.longValue();
            }
          } else {
            offHeapObjectCount = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_LOCK_RECALL_RATE);
          if (n != null) {
            if (lockRecallRate >= 0) {
              lockRecallRate += n.longValue();
            }
          } else {
            lockRecallRate = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_BROADCAST_RATE);
          if (n != null) {
            if (broadcastRate >= 0) {
              broadcastRate += n.longValue();
            }
          } else {
            broadcastRate = -1;
          }
        }
      }

      if (flush != -1) {
        updateSeries(clientFlushRateSeries, Long.valueOf(flush));
        clientFlushRateLabel.setText(MessageFormat.format(clientFlushRateLabelFormat, flush));
      }
      if (fault != -1) {
        updateSeries(clientFaultRateSeries, Long.valueOf(fault));
        clientFaultRateLabel.setText(MessageFormat.format(clientFaultRateLabelFormat, fault));
      }
      if (txn != -1) {
        updateSeries(txnRateSeries, Long.valueOf(txn));
        txnRateLabel.setText(MessageFormat.format(txnRateLabelFormat, txn));
      }
      if (diskFaultedRate != -1) {
        updateSeries(diskFaultRateSeries, Long.valueOf(diskFaultedRate));
        diskFaultRateLabel.setText(MessageFormat.format(diskFaultRateLabelFormat, diskFaultedRate));
      }
      if (diskFlushedRate != -1) {
        updateSeries(diskFlushRateSeries, Long.valueOf(diskFlushedRate));
        diskFlushRateLabel.setText(MessageFormat.format(diskFlushRateLabelFormat, diskFlushedRate));
      }
      if (liveObjectCount != -1) {
        updateSeries(liveObjectCountSeries, Long.valueOf(liveObjectCount));
        objectManagerTitle.setTitle(MessageFormat.format(objectManagerTitlePattern, cachedObjectCount,
                                                         offHeapObjectCount, liveObjectCount));
      }
      if (cachedObjectCount != -1) {
        updateSeries(cachedObjectCountSeries, Long.valueOf(cachedObjectCount));
      }
      if (offHeapObjectCount != -1) {
        updateSeries(offHeapObjectCountSeries, Long.valueOf(offHeapObjectCount));
      }
      if (lockRecallRate != -1) {
        updateSeries(lockRecallRateSeries, Long.valueOf(lockRecallRate));
        lockRecallRateLabel.setText(MessageFormat.format(lockRecallRateLabelFormat, lockRecallRate));
      }
      if (broadcastRate != -1) {
        updateSeries(broadcastRateSeries, Long.valueOf(broadcastRate));
        broadcastRateLabel.setText(MessageFormat.format(broadcastRateLabelFormat, broadcastRate));
      }
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
    offHeapObjectCountSeries = createTimeSeries("OffHeap Object Count");
    JFreeChart chart = createChart(new TimeSeries[] { cachedObjectCountSeries, offHeapObjectCountSeries,
        liveObjectCountSeries }, true);
    ChartPanel liveObjectCountPanel = createChartPanel(chart);
    parent.add(liveObjectCountPanel);
    liveObjectCountPanel.setPreferredSize(fDefaultGraphSize);
    String liveObjectCountLabel = appContext.getString("live.object.count");
    objectManagerTitlePattern = liveObjectCountLabel + " (caching: {0}, offheap: {1}, total: {2})";
    objectManagerTitle = BorderFactory.createTitledBorder(liveObjectCountLabel);
    liveObjectCountPanel.setBorder(objectManagerTitle);
    liveObjectCountPanel.setToolTipText("Total/Cached/OffHeap instance counts");
    liveObjectCountPlot = (XYPlot) chart.getPlot();
    XYAreaRenderer areaRenderer2 = new XYAreaRenderer(XYAreaRenderer.AREA,
                                                      StandardXYToolTipGenerator.getTimeSeriesInstance(), null);
    liveObjectCountPlot.setRenderer(areaRenderer2);
    areaRenderer2.setSeriesPaint(0, Color.blue);
    areaRenderer2.setSeriesPaint(1, Color.red);
    areaRenderer2.setSeriesPaint(2, Color.green);
  }

  private void setupLockRecallRatePanel(XContainer parent) {
    lockRecallRateSeries = createTimeSeries("Lock Recall Rate");
    broadcastRateSeries = createTimeSeries("Change Broadcast Rate");
    JFreeChart chart = createChart(new TimeSeries[] { lockRecallRateSeries, broadcastRateSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeMinimumSize(10.0);
    ChartPanel recallRatePanel = createChartPanel(chart);
    parent.add(recallRatePanel);
    recallRatePanel.setPreferredSize(fDefaultGraphSize);
    recallRatePanel.setBorder(new TitledBorder("Lock Recalls/Change Broadcasts"));
    recallRatePanel.setToolTipText("Global Lock Recalls");
    recallRatePanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(lockRecallRateLabel = createStatusLabel(Color.red));
    labelHolder.add(broadcastRateLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    recallRatePanel.add(labelHolder, gbc);
  }

  private void setupFlushRatePanel(XContainer parent) {
    clientFlushRateSeries = createTimeSeries("");
    ChartPanel flushRatePanel = createChartPanel(createChart(clientFlushRateSeries, false));
    parent.add(flushRatePanel);
    flushRatePanel.setPreferredSize(fDefaultGraphSize);
    flushRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.flush.rate")));
    flushRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.flush.rate.tip"));
    flushRatePanel.setLayout(new BorderLayout());
    flushRatePanel.add(clientFlushRateLabel = createOverlayLabel());
  }

  private void setupFaultRatePanel(XContainer parent) {
    clientFaultRateSeries = createTimeSeries("");
    ChartPanel faultRatePanel = createChartPanel(createChart(clientFaultRateSeries, false));
    parent.add(faultRatePanel);
    faultRatePanel.setPreferredSize(fDefaultGraphSize);
    faultRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.fault.rate")));
    faultRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.fault.rate.tip"));
    faultRatePanel.setLayout(new BorderLayout());
    faultRatePanel.add(clientFaultRateLabel = createOverlayLabel());
  }

  private void setupTxnRatePanel(XContainer parent) {
    txnRateSeries = createTimeSeries("");
    ChartPanel txnRatePanel = createChartPanel(createChart(txnRateSeries, false));
    parent.add(txnRatePanel);
    txnRatePanel.setPreferredSize(fDefaultGraphSize);
    txnRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.transaction.rate")));
    txnRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.transaction.rate.tip"));
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
    cacheMissRatePanel.setBorder(new TitledBorder(appContext.getString("aggregate.server.stats.cache-manager")));
    cacheMissRatePanel.setToolTipText(appContext.getString("aggregate.server.stats.cache-manager.tip"));
    cacheMissRatePanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(diskFaultRateLabel = createStatusLabel(Color.red));
    labelHolder.add(diskFlushRateLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    cacheMissRatePanel.add(labelHolder, gbc);
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
    if (diskFaultRateSeries != null) {
      list.add(diskFaultRateSeries);
      diskFaultRateSeries = null;
    }
    if (diskFlushRateSeries != null) {
      list.add(diskFlushRateSeries);
      diskFlushRateSeries = null;
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
