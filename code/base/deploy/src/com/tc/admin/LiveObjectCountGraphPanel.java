package com.tc.admin;

import static com.tc.admin.model.IClusterModel.PollScope.ACTIVE_SERVERS;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_LIVE_OBJECT_COUNT;
import static com.tc.admin.model.IServer.POLLED_ATTR_BROADCAST_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_LOCK_RECALL_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.ui.RectangleInsets;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.dso.DGCIntervalMarker;
import com.tc.admin.model.DGCListener;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributeListener;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.objectserver.api.GCStats;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

class LiveObjectCountGraphPanel extends BaseRuntimeStatsPanel implements PolledAttributeListener, DGCListener {
  private IClusterModel            clusterModel;
  private ClusterListener          clusterListener;

  private XYPlot                   liveObjectCountPlot;
  private TimeSeries               liveObjectCountSeries;
  private JLabel                   liveObjectCountLabel;
  private final String             liveObjectCountLabelPrefix;

  private TimeSeries               lockRecallRateSeries;
  private JLabel                   lockRecallRateLabel;
  private final String             lockRecallRateLabelPrefix;

  private JLabel                   broadcastRateLabel;
  private final String             broadcastRateLabelPrefix = "Broadcast Rate";

  private TimeSeries               broadcastRateSeries;

  public final static Color        LABEL_FG                 = Color.black;
  public final static Font         LABEL_FONT               = new Font("Dialog", Font.PLAIN, 9);

  private static final Set<String> POLLED_ATTRIBUTE_SET     = new HashSet(Arrays.asList(POLLED_ATTR_LIVE_OBJECT_COUNT,
                                                                                        POLLED_ATTR_LOCK_RECALL_RATE,
                                                                                        POLLED_ATTR_BROADCAST_RATE));

  public LiveObjectCountGraphPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(appContext);
    this.clusterModel = clusterModel;

    liveObjectCountLabelPrefix = appContext.getString("dso.client.liveObjectCount");
    lockRecallRateLabelPrefix = appContext.getString("dso.lock-recall.rate");

    setName(clusterModel.toString());
    setup(chartsPanel);

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
        oldActive.removeDGCListener(LiveObjectCountGraphPanel.this);
      }
      if (newActive != null) {
        newActive.addDGCListener(LiveObjectCountGraphPanel.this);
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
  }

  /**
   * Implementation of PolledAttributeListener.
   */
  @Override
  public void attributesPolled(PolledAttributesResult result) {
    int liveObjectCount = 0;
    int lockRecallRate = 0;
    int broadcastRate = 0;
    for (IServerGroup group : clusterModel.getServerGroups()) {
      IServer activeServer = group.getActiveServer();
      if (activeServer != null) {
        Number nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_LIVE_OBJECT_COUNT);
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
      }
    }
    final int liveObjectCountValue = liveObjectCount;
    final int lockRecallRateValue = lockRecallRate;
    final int broadcastRateValue = broadcastRate;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (liveObjectCountSeries != null) {
          liveObjectCountSeries.addOrUpdate(new Second(), liveObjectCountValue);
          liveObjectCountLabel.setText(liveObjectCountLabelPrefix + " = " + liveObjectCountValue + " instances");
        }
        if (lockRecallRateSeries != null) {
          lockRecallRateSeries.addOrUpdate(new Second(), lockRecallRateValue);
          lockRecallRateLabel.setText(lockRecallRateLabelPrefix + " = " + lockRecallRateValue + " recalls/s.");
        }
        if (broadcastRateSeries != null) {
          broadcastRateSeries.addOrUpdate(new Second(), broadcastRateValue);
          broadcastRateLabel.setText(broadcastRateLabelPrefix + " = " + broadcastRateValue + " broacasts/s.");
        }
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

  @Override
  protected void setup(XContainer runtimeStatsPanel) {
    runtimeStatsPanel.setLayout(new GridLayout(0, 1));

    JFreeChart chart = createChart(liveObjectCountSeries = createTimeSeries("LiveObjectCount"), false);
    chart.setPadding(RectangleInsets.ZERO_INSETS);
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.getRangeAxis().setVisible(false);
    plot.getDomainAxis().setVisible(false);
    plot.setFixedDomainAxisSpace(new AxisSpace());
    plot.setFixedRangeAxisSpace(new AxisSpace());
    liveObjectCountPlot = plot;

    ChartPanel chartPanel = createChartPanel(chart);
    chartPanel.setMinimumSize(new Dimension(0, 0));
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setLayout(new BorderLayout());
    liveObjectCountLabel = new JLabel();
    liveObjectCountLabel.setForeground(LABEL_FG);
    liveObjectCountLabel.setFont(LABEL_FONT);
    liveObjectCountLabel.setBorder(new EmptyBorder(6, 14, 0, 0));
    chartPanel.add(liveObjectCountLabel, BorderLayout.NORTH);
    liveObjectCountLabel.setHorizontalAlignment(SwingConstants.LEFT);
    liveObjectCountLabel.setVerticalAlignment(SwingConstants.TOP);
    chartPanel.setOpaque(false);

    runtimeStatsPanel.add(chartPanel);

    lockRecallRateSeries = createTimeSeries("LockRecallRate");
    broadcastRateSeries = createTimeSeries("BroadcastRate");

    chart = createXYBarChart(new TimeSeries[] { lockRecallRateSeries, broadcastRateSeries }, false);
    chart.setPadding(RectangleInsets.ZERO_INSETS);
    plot = (XYPlot) chart.getPlot();
    plot.setRenderer(new ClusteredXYBarRenderer());
    plot.getRangeAxis().setVisible(false);
    plot.getDomainAxis().setVisible(false);
    plot.setFixedDomainAxisSpace(new AxisSpace());
    plot.setFixedRangeAxisSpace(new AxisSpace());
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setAutoRangeMinimumSize(10.0);

    XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
    renderer.setDrawBarOutline(false);
    renderer.setShadowVisible(false);

    renderer.setSeriesPaint(0, Color.blue);
    renderer.setSeriesPaint(1, Color.red);

    chartPanel = createChartPanel(chart);
    chartPanel.setMinimumSize(new Dimension(0, 0));
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setLayout(new BorderLayout());
    JPanel labelPanel = new JPanel(new GridLayout(0, 1));
    lockRecallRateLabel = new JLabel();
    lockRecallRateLabel.setForeground(Color.blue);
    lockRecallRateLabel.setFont(LABEL_FONT);
    lockRecallRateLabel.setHorizontalAlignment(SwingConstants.LEFT);
    lockRecallRateLabel.setVerticalAlignment(SwingConstants.TOP);
    labelPanel.add(lockRecallRateLabel);
    broadcastRateLabel = new JLabel();
    broadcastRateLabel.setForeground(Color.red);
    broadcastRateLabel.setFont(LABEL_FONT);
    broadcastRateLabel.setHorizontalAlignment(SwingConstants.LEFT);
    broadcastRateLabel.setVerticalAlignment(SwingConstants.TOP);
    labelPanel.add(broadcastRateLabel);
    labelPanel.setBorder(new EmptyBorder(6, 14, 0, 0));
    labelPanel.setOpaque(false);
    chartPanel.add(labelPanel, BorderLayout.NORTH);

    runtimeStatsPanel.add(chartPanel);
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
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
      if (gcStats.getElapsedTime() != -1) {
        liveObjectCountPlot.addDomainMarker(new DGCIntervalMarker(gcStats));
      }
    }
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterModel.removePolledAttributeListener(ACTIVE_SERVERS, POLLED_ATTRIBUTE_SET, this);

    super.tearDown();

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      liveObjectCountSeries.clear();
      liveObjectCountSeries = null;
      liveObjectCountLabel = null;
      clusterListener = null;
    }
  }
}
