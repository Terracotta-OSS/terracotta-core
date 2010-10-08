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
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_ONHEAP_FAULT_RATE);
          if (n != null) {
            if (onHeapFaultRate >= 0) {
              onHeapFaultRate += n.longValue();
            }
          } else {
            onHeapFaultRate = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_ONHEAP_FLUSH_RATE);
          if (n != null) {
            if (onHeapFlushRate >= 0) {
              onHeapFlushRate += n.longValue();
            }
          } else {
            onHeapFlushRate = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OFFHEAP_FAULT_RATE);
          if (n != null) {
            if (offHeapFaultRate >= 0) {
              offHeapFaultRate += n.longValue();
            }
          } else {
            offHeapFaultRate = -1;
          }
          n = (Number) getPolledAttribute(result, theServer, POLLED_ATTR_OFFHEAP_FLUSH_RATE);
          if (n != null) {
            if (offHeapFlushRate >= 0) {
              offHeapFlushRate += n.longValue();
            }
          } else {
            offHeapFlushRate = -1;
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
        clientFlushRateLabel.setText(MessageFormat.format(clientFlushRateLabelFormat, convert(flush)));
      }
      if (fault != -1) {
        updateSeries(clientFaultRateSeries, Long.valueOf(fault));
        clientFaultRateLabel.setText(MessageFormat.format(clientFaultRateLabelFormat, convert(fault)));
      }
      if (txn != -1) {
        updateSeries(txnRateSeries, Long.valueOf(txn));
        txnRateLabel.setText(MessageFormat.format(txnRateLabelFormat, convert(txn)));
      }
      if (onHeapFaultRate != -1) {
        updateSeries(onHeapFaultRateSeries, Long.valueOf(onHeapFaultRate));
        onHeapFaultRateLabel.setText(MessageFormat.format(onHeapFaultRateLabelFormat, convert(onHeapFaultRate)));
      }
      if (onHeapFlushRate != -1) {
        updateSeries(onHeapFlushRateSeries, Long.valueOf(onHeapFlushRate));
        onHeapFlushRateLabel.setText(MessageFormat.format(onHeapFlushRateLabelFormat, convert(onHeapFlushRate)));
      }
      if (offHeapFaultRate != -1) {
        updateSeries(offHeapFaultRateSeries, Long.valueOf(offHeapFaultRate));
        offHeapFaultRateLabel.setText(MessageFormat.format(offHeapFaultRateLabelFormat, convert(offHeapFaultRate)));
      }
      if (offHeapFlushRate != -1) {
        updateSeries(offHeapFlushRateSeries, Long.valueOf(offHeapFlushRate));
        offHeapFlushRateLabel.setText(MessageFormat.format(offHeapFlushRateLabelFormat, convert(offHeapFlushRate)));
      }
      if (liveObjectCount != -1) {
        updateSeries(liveObjectCountSeries, Long.valueOf(liveObjectCount));
        String cached = convert(cachedObjectCount);
        String offHeap = offHeapObjectCount != -1 ? convert(offHeapObjectCount) : "n/a";
        String live = convert(liveObjectCount);
        objectManagerTitle.setTitle(MessageFormat.format(objectManagerTitlePattern, cached, offHeap, live));
      }
      if (cachedObjectCount != -1) {
        updateSeries(cachedObjectCountSeries, Long.valueOf(cachedObjectCount));
      }
      if (offHeapObjectCount != -1) {
        updateSeries(offHeapObjectCountSeries, Long.valueOf(offHeapObjectCount));
      }
      if (lockRecallRate != -1) {
        updateSeries(lockRecallRateSeries, Long.valueOf(lockRecallRate));
        lockRecallRateLabel.setText(MessageFormat.format(lockRecallRateLabelFormat, convert(lockRecallRate)));
      }
      if (broadcastRate != -1) {
        updateSeries(broadcastRateSeries, Long.valueOf(broadcastRate));
        broadcastRateLabel.setText(MessageFormat.format(broadcastRateLabelFormat, convert(broadcastRate)));
      }
    }
  }

  @Override
  protected synchronized void setup(XContainer chartsPanel) {

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
    if (onHeapFaultRateSeries != null) {
      list.add(onHeapFaultRateSeries);
      onHeapFaultRateSeries = null;
    }
    if (onHeapFlushRateSeries != null) {
      list.add(onHeapFlushRateSeries);
      onHeapFlushRateSeries = null;
    }
    if (offHeapFaultRateSeries != null) {
      list.add(offHeapFaultRateSeries);
      offHeapFaultRateSeries = null;
    }
    if (offHeapFlushRateSeries != null) {
      list.add(offHeapFlushRateSeries);
      offHeapFlushRateSeries = null;
    }
    if (liveObjectCountSeries != null) {
      list.add(liveObjectCountSeries);
      liveObjectCountSeries = null;
    }
    if (offHeapObjectCountSeries != null) {
      list.add(offHeapObjectCountSeries);
      offHeapObjectCountSeries = null;
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
