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
import com.tc.admin.common.XLabel;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributeListener;
import com.tc.admin.model.PolledAttributesResult;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Paint;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

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

  private final XContainer         messagePanel;
  private XLabel                   messageLabel;

  private static final String      NOT_READY_MESSAGE    = "Cluster is not yet ready for action.  Are all the mirror groups active?";
  private static final String      INITIALIZING_MESSAGE = "Initializing...";

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

    messagePanel = createMessagePanel();

    setName(clusterModel.toString());
    setup(chartsPanel);

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      startMonitoringRuntimeStats();
    } else {
      remove(chartsPanel);
      messageLabel.setText(NOT_READY_MESSAGE);
      add(messagePanel);
    }
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(INITIALIZING_MESSAGE);
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
    return panel;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    public void handleReady() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      removeAll();
      if (clusterModel.isReady()) {
        add(chartsPanel);
        startMonitoringRuntimeStats();
      } else {
        stopMonitoringRuntimeStats();
        messageLabel.setText(NOT_READY_MESSAGE);
        add(messagePanel);
      }
      revalidate();
      repaint();
    }
  }

  /**
   * Implementation of PolledAttributeListener.
   */
  @Override
  public void attributesPolled(final PolledAttributesResult result) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
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

        txnRateDataset.setValue(Double.valueOf(txnRate));
        creationRateDataset.setValue(Double.valueOf(creationRate));
        broadcastRateDataset.setValue(Double.valueOf(broadcastRate));
        lockRecallRateDataset.setValue(Double.valueOf(lockRecallRate));
        faultRateDataset.setValue(Double.valueOf(faultRate));
        flushRateDataset.setValue(Double.valueOf(flushRate));
        txnSizeRateDataset.setValue(Double.valueOf(txnSizeRate / 1000d));
        pendingTxnsDataset.setValue(Integer.valueOf(pendingTxnsCount));

        revalidate();
        repaint();
      }
    });
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

  public static ChartPanel createDialChartPanel(JFreeChart chart) {
    boolean useBuffer = false;
    boolean properties = false;
    boolean save = false;
    boolean print = false;
    boolean zoom = false;
    boolean tooltips = true;

    ChartPanel chartPanel = new ChartPanel(chart, ChartPanel.DEFAULT_WIDTH, ChartPanel.DEFAULT_HEIGHT,
        ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH, ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT,
        ChartPanel.DEFAULT_MAXIMUM_DRAW_WIDTH, ChartPanel.DEFAULT_MAXIMUM_DRAW_HEIGHT, useBuffer, properties, save,
        print, zoom, tooltips) {
      @Override
      public Dimension getPreferredSize() {
        Dimension dim = getParent().getSize();
        dim.width = Math.min(dim.width, dim.height);
        dim.height = Math.min(dim.width, dim.height);
        return dim;
      }

      @Override
      public Dimension getMaximumSize() {
        return getPreferredSize();
      }

      @Override
      public Dimension getMinimumSize() {
        return getPreferredSize();
      }
    };
    return chartPanel;
  }

  @Override
  protected void setup(XContainer runtimeStatsPanel) {
    // runtimeStatsPanel.setLayout(new FlowLayout());
    runtimeStatsPanel.setLayout(new GridLayout(1, 0));

    JFreeChart chart;
    StandardDialRange[] ranges;
    double startAngle = -140, extent = -260;
    StandardDialScale defScale = DemoChartFactory.createStandardDialScale(0, 5000, startAngle, extent, 1000, 4);
    StandardDialScale scale;
    XContainer chartHolder;
    Paint minorPointerFillPaint = Color.black;
    Paint minorPointerOutlinePaint = Color.black;
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;

    ranges = new StandardDialRange[] { new StandardDialRange(4000, 4500, Color.orange),
        new StandardDialRange(4500, 5000, Color.red) };
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.txn-rate"), txnRateDataset, defScale, ranges);
    chartHolder = new XContainer(new GridBagLayout());
    chartHolder.add(createDialChartPanel(chart), gbc);
    runtimeStatsPanel.add(chartHolder);

    ranges = new StandardDialRange[] { new StandardDialRange(4000, 4500, Color.orange),
        new StandardDialRange(4500, 5000, Color.red) };
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.lock-recall-rate"), lockRecallRateDataset,
                                        defScale, ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    chartHolder = new XContainer(new GridBagLayout());
    chartHolder.add(createDialChartPanel(chart), gbc);
    runtimeStatsPanel.add(chartHolder);

    ranges = new StandardDialRange[] { new StandardDialRange(5000, 6000, Color.orange),
        new StandardDialRange(6000, 7000, Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, 7000, startAngle, extent, 1000, 4);
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.object-creation-rate"), creationRateDataset,
                                        scale, ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    chartHolder = new XContainer(new GridBagLayout());
    chartHolder.add(createDialChartPanel(chart), gbc);
    runtimeStatsPanel.add(chartHolder);

    ranges = new StandardDialRange[] { new StandardDialRange(4000, 4500, Color.orange),
        new StandardDialRange(4500, 5000, Color.red) };
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.broadcast-rate"), broadcastRateDataset,
                                        defScale, ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    chartHolder = new XContainer(new GridBagLayout());
    chartHolder.add(createDialChartPanel(chart), gbc);
    runtimeStatsPanel.add(chartHolder);

    ranges = new StandardDialRange[] { new StandardDialRange(4000, 4500, Color.orange),
        new StandardDialRange(4500, 5000, Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, 5000, startAngle, extent, 1000, 4);
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.fault-rate"), faultRateDataset, scale, ranges,
                                        minorPointerFillPaint, minorPointerOutlinePaint);
    chartHolder = new XContainer(new GridBagLayout());
    chartHolder.add(createDialChartPanel(chart), gbc);
    runtimeStatsPanel.add(chartHolder);

    chart = DemoChartFactory.createDial(appContext.getString("dashboard.flush-rate"), flushRateDataset, scale, ranges,
                                        minorPointerFillPaint, minorPointerOutlinePaint);
    chartHolder = new XContainer(new GridBagLayout());
    chartHolder.add(createDialChartPanel(chart), gbc);
    runtimeStatsPanel.add(chartHolder);

    ranges = new StandardDialRange[] { new StandardDialRange(60, 80, Color.orange),
        new StandardDialRange(80, 100, Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, 100, startAngle, extent, 10, 4);
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.txn-size-rate"), txnSizeRateDataset, scale,
                                        ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    chartHolder = new XContainer(new GridBagLayout());
    chartHolder.add(createDialChartPanel(chart), gbc);
    runtimeStatsPanel.add(chartHolder);

    ranges = new StandardDialRange[] { new StandardDialRange(60, 90, Color.orange),
        new StandardDialRange(90, 100, Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, 100, startAngle, extent, 10, 4);
    chart = DemoChartFactory.createDial(appContext.getString("dashboard.unacked-txns"), pendingTxnsDataset, scale,
                                        ranges, minorPointerFillPaint, minorPointerOutlinePaint);
    chartHolder = new XContainer(new GridBagLayout());
    chartHolder.add(createDialChartPanel(chart), gbc);
    runtimeStatsPanel.add(chartHolder);
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

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
