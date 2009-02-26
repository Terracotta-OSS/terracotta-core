package com.tc.admin;

import static com.tc.admin.model.IClusterModel.PollScope.ACTIVE_SERVERS;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_LIVE_OBJECT_COUNT;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FAULT_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FLUSH_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_TRANSACTION_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_BROADCAST_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_LOCK_RECALL_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_PENDING_TRANSACTIONS_COUNT;
import static com.tc.admin.model.IServer.POLLED_ATTR_TRANSACTION_SIZE_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.dial.StandardDialRange;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.data.general.DefaultValueDataset;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.XContainer;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributeListener;
import com.tc.admin.model.PolledAttributesResult;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;

class DashboardPanel extends BaseRuntimeStatsPanel implements PolledAttributeListener {
  private IClusterModel            clusterModel;
  private ClusterListener          clusterListener;

  private DefaultValueDataset      txnRateDataset;
  private DefaultValueDataset      creationRateDataset;
  private DefaultValueDataset      broadcastRateDataset;
  private DefaultValueDataset      lockRecallRateDataset;
  private DefaultValueDataset      flushRateDataset;
  private DefaultValueDataset      faultRateDataset;
  private DefaultValueDataset      txnSizeRateDataset;
  private DefaultValueDataset      pendingTxnsDataset;

  private long                     lastObjectCount;
  private long                     lastObjectCountTime  = -1;

  private static final Set<String> POLLED_ATTRIBUTE_SET = new HashSet(Arrays
                                                            .asList(POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                    POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                    POLLED_ATTR_LIVE_OBJECT_COUNT,
                                                                    POLLED_ATTR_LOCK_RECALL_RATE,
                                                                    POLLED_ATTR_TRANSACTION_RATE,
                                                                    POLLED_ATTR_TRANSACTION_SIZE_RATE,
                                                                    POLLED_ATTR_BROADCAST_RATE,
                                                                    POLLED_ATTR_PENDING_TRANSACTIONS_COUNT));

  public DashboardPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(appContext);

    this.clusterModel = clusterModel;

    txnRateDataset = new DefaultValueDataset();
    creationRateDataset = new DefaultValueDataset();
    broadcastRateDataset = new DefaultValueDataset();
    lockRecallRateDataset = new DefaultValueDataset();
    flushRateDataset = new DefaultValueDataset();
    faultRateDataset = new DefaultValueDataset();
    txnSizeRateDataset = new DefaultValueDataset();
    pendingTxnsDataset = new DefaultValueDataset();

    setName(clusterModel.toString());
    setup(chartsPanel);

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      startMonitoringRuntimeStats();
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    public void handleReady() {
      if (clusterModel.isReady()) {
        startMonitoringRuntimeStats();
      } else {
        stopMonitoringRuntimeStats();
      }
    }
  }

  /**
   * Implementation of PolledAttributeListener.
   */
  @Override
  public void attributesPolled(PolledAttributesResult result) {
    int liveObjectCount = 0;
    int txnRate = 0;
    int lockRecallRate = 0;
    int broadcastRate = 0;
    int flushRate = 0;
    int faultRate = 0;
    int txnSizeRate = 0;
    int pendingTxnsCount = 0;

    for (IServerGroup group : clusterModel.getServerGroups()) {
      IServer activeServer = group.getActiveServer();
      if (activeServer != null) {
        Number nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_TRANSACTION_RATE);
        if (nodeValue != null) {
          txnRate += nodeValue.intValue();
        }
        nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_LIVE_OBJECT_COUNT);
        if (nodeValue != null) {
          liveObjectCount += nodeValue.intValue();
        }
        nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_LOCK_RECALL_RATE);
        if (nodeValue != null) {
          lockRecallRate += nodeValue.intValue();
        }
        nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_BROADCAST_RATE);
        if (nodeValue != null) {
          broadcastRate += nodeValue.intValue();
        }
        nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_OBJECT_FLUSH_RATE);
        if (nodeValue != null) {
          flushRate += nodeValue.intValue();
        }
        nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_OBJECT_FAULT_RATE);
        if (nodeValue != null) {
          faultRate += nodeValue.intValue();
        }
        nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_TRANSACTION_SIZE_RATE);
        if (nodeValue != null) {
          txnSizeRate += nodeValue.intValue();
        }
        nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_PENDING_TRANSACTIONS_COUNT);
        if (nodeValue != null) {
          pendingTxnsCount += nodeValue.intValue();
        }
      }
    }

    double creationRate = 0;
    long now = System.currentTimeMillis();
    if (lastObjectCountTime != -1) {
      double newObjectsCount = liveObjectCount - lastObjectCount;
      double timeDiff = now - (double) lastObjectCountTime;
      creationRate = (newObjectsCount / timeDiff) * 1000;
    }
    lastObjectCount = liveObjectCount;
    lastObjectCountTime = now;

    txnRateDataset.setValue(Integer.valueOf(txnRate));
    creationRateDataset.setValue(Double.valueOf(creationRate));
    broadcastRateDataset.setValue(Integer.valueOf(broadcastRate));
    lockRecallRateDataset.setValue(Integer.valueOf(lockRecallRate));
    faultRateDataset.setValue(Integer.valueOf(faultRate));
    flushRateDataset.setValue(Integer.valueOf(flushRate));
    txnSizeRateDataset.setValue(Double.valueOf(txnSizeRate / 1000d));
    pendingTxnsDataset.setValue(Integer.valueOf(pendingTxnsCount));
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
  protected void setup(XContainer runtimeStatsPanel) {
    runtimeStatsPanel.setLayout(new GridLayout(1, 0));

    JFreeChart chart;
    StandardDialRange[] ranges;
    double startAngle = -140, extent = -260;
    StandardDialScale defScale = DemoChartFactory.createStandardDialScale(0, 5000, startAngle, extent, 1000, 4);
    StandardDialScale scale;
    ChartPanel chartPanel;
    Dimension majorPrefSize = new Dimension(160, 160);
    Dimension minorPrefSize = new Dimension(130, 130);
    Paint minorPointerFillPaint = Color.black;
    Paint minorPointerOutlinePaint = Color.black;

    ranges = new StandardDialRange[] { new StandardDialRange(4000, 4500, Color.orange),
        new StandardDialRange(4500, 5000, Color.red) };
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.txn-rate"), txnRateDataset, defScale, ranges);
    runtimeStatsPanel.add(chartPanel = createChartPanel(chart));
    chartPanel.setPreferredSize(majorPrefSize);
    chartPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

    ranges = new StandardDialRange[] { new StandardDialRange(4000, 4500, Color.orange),
        new StandardDialRange(4500, 5000, Color.red) };
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.lock-recall-rate"), lockRecallRateDataset,
                                        defScale, ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    runtimeStatsPanel.add(chartPanel = createChartPanel(chart));
    chartPanel.setPreferredSize(minorPrefSize);

    ranges = new StandardDialRange[] { new StandardDialRange(400, 450, Color.orange),
        new StandardDialRange(450, 500, Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, 500, startAngle, extent, 100, 4);
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.object-creation-rate"), creationRateDataset,
                                        scale, ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    runtimeStatsPanel.add(chartPanel = createChartPanel(chart));
    chartPanel.setPreferredSize(minorPrefSize);

    ranges = new StandardDialRange[] { new StandardDialRange(4000, 4500, Color.orange),
        new StandardDialRange(4500, 5000, Color.red) };
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.broadcast-rate"), broadcastRateDataset,
                                        defScale, ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    runtimeStatsPanel.add(chartPanel = createChartPanel(chart));
    chartPanel.setPreferredSize(minorPrefSize);

    ranges = new StandardDialRange[] { new StandardDialRange(80, 90, Color.orange),
        new StandardDialRange(90, 100, Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, 100, startAngle, extent, 10, 4);
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.fault-rate"), faultRateDataset, scale, ranges,
                                        minorPointerFillPaint, minorPointerOutlinePaint);
    runtimeStatsPanel.add(chartPanel = createChartPanel(chart));
    chartPanel.setPreferredSize(minorPrefSize);

    chart = DemoChartFactory.createDial(appContext.getString("dashboard.flush-rate"), flushRateDataset, scale, ranges,
                                        minorPointerFillPaint, minorPointerOutlinePaint);
    runtimeStatsPanel.add(chartPanel = createChartPanel(chart));
    chartPanel.setPreferredSize(minorPrefSize);

    ranges = new StandardDialRange[] { new StandardDialRange(200, 400, Color.orange),
        new StandardDialRange(400, 500, Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, 500, startAngle, extent, 100, 4);
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.txn-size-rate"), txnSizeRateDataset, scale,
                                        ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    runtimeStatsPanel.add(chartPanel = createChartPanel(chart));
    chartPanel.setPreferredSize(minorPrefSize);

    ranges = new StandardDialRange[] { new StandardDialRange(80, 90, Color.orange),
        new StandardDialRange(90, 100, Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, 100, startAngle, extent, 10, 4);
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.unacked-txns"), pendingTxnsDataset, scale,
                                        ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    runtimeStatsPanel.add(chartPanel = createChartPanel(chart));
    chartPanel.setPreferredSize(minorPrefSize);
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterModel.removePolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTRIBUTE_SET, this);

    super.tearDown();

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      clusterListener = null;

      txnRateDataset = null;
      creationRateDataset = null;
      broadcastRateDataset = null;
      lockRecallRateDataset = null;
      flushRateDataset = null;
      faultRateDataset = null;
      txnSizeRateDataset = null;
      pendingTxnsDataset = null;
    }
  }
}
