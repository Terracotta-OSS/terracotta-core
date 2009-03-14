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
import org.jfree.chart.plot.dial.DialBackground;
import org.jfree.chart.plot.dial.DialCap;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialTextAnnotation;
import org.jfree.chart.plot.dial.DialValueIndicator;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialRange;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.RectangleInsets;

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
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

class DashboardPanel extends BaseRuntimeStatsPanel implements PolledAttributeListener {
  private IClusterModel            clusterModel;
  private ClusterListener          clusterListener;

  private final int                dialRangeScaleFactor;

  private DialInfo                 txnRateDialInfo;
  private DialInfo                 creationRateDialInfo;
  private DialInfo                 broadcastRateDialInfo;
  private DialInfo                 lockRecallRateDialInfo;
  private DialInfo                 flushRateDialInfo;
  private DialInfo                 faultRateDialInfo;
  private DialInfo                 txnSizeRateDialInfo;
  private DialInfo                 pendingTxnsDialInfo;

  private long                     lastObjectCount;
  private long                     lastObjectCountTime   = -1;

  private final XContainer         messagePanel;
  private XLabel                   messageLabel;

  private static final int         HISTORY_ITERATION_MAX = Integer
                                                             .getInteger(
                                                                         "com.tc.admin.dashboard.dial-history-iterations",
                                                                         3);

  private static final Set<String> POLLED_ATTRIBUTE_SET  = new HashSet(Arrays
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

    dialRangeScaleFactor = Integer.getInteger("com.tc.admin.dashboard.dial-range-scale-factor", clusterModel
        .getServerGroups().length);

    txnRateDialInfo = new DialInfo("Transaction Rate", scale(5000));
    creationRateDialInfo = new DialInfo("Object Creation Rate", scale(7000));
    broadcastRateDialInfo = new DialInfo("Change Broadcast Rate", scale(5000));
    lockRecallRateDialInfo = new DialInfo("Lock Recall Rate", scale(5000));
    flushRateDialInfo = new DialInfo("Flush Rate", scale(5000));
    faultRateDialInfo = new DialInfo("Fault Rate", scale(5000));
    txnSizeRateDialInfo = new DialInfo("Transaction Volume Rate", scale(100));
    pendingTxnsDialInfo = new DialInfo("Unacknowledged Client Transactions", scale(100));

    messagePanel = createMessagePanel();

    setName(clusterModel.toString());
    setup(chartsPanel);

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      startMonitoringRuntimeStats();
    } else {
      remove(chartsPanel);
      messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
      add(messagePanel);
    }
  }

  private int scale(int value) {
    return value * dialRangeScaleFactor;
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(appContext.getString("initializing"));
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) appContext.getObject("message.label.font"));
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
        chartsPanel.revalidate();
        startMonitoringRuntimeStats();
      } else {
        stopMonitoringRuntimeStats();
        messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
        add(messagePanel);
        messagePanel.revalidate();
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
        IClusterModel theClusterModel = getClusterModel();
        if (theClusterModel == null) { return; }

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
              if (txnRate >= 0) txnRate += nodeValue.intValue();
            } else {
              txnRate = -1;
            }
            nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_LIVE_OBJECT_COUNT);
            if (nodeValue != null) {
              if (liveObjectCount >= 0) liveObjectCount += nodeValue.intValue();
            } else {
              liveObjectCount = -1;
            }
            nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_LOCK_RECALL_RATE);
            if (nodeValue != null) {
              if (lockRecallRate >= 0) lockRecallRate += nodeValue.intValue();
            } else {
              lockRecallRate = -1;
            }
            nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_BROADCAST_RATE);
            if (nodeValue != null) {
              if (broadcastRate >= 0) broadcastRate += nodeValue.intValue();
            } else {
              broadcastRate = -1;
            }
            nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_OBJECT_FLUSH_RATE);
            if (nodeValue != null) {
              if (flushRate >= 0) flushRate += nodeValue.intValue();
            } else {
              flushRate = -1;
            }
            nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_OBJECT_FAULT_RATE);
            if (nodeValue != null) {
              if (faultRate >= 0) faultRate += nodeValue.intValue();
            } else {
              faultRate = -1;
            }
            nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_TRANSACTION_SIZE_RATE);
            if (nodeValue != null) {
              if (txnSizeRate >= 0) txnSizeRate += nodeValue.intValue();
            } else {
              txnSizeRate = -1;
            }
            nodeValue = (Number) result.getPolledAttribute(activeServer, POLLED_ATTR_PENDING_TRANSACTIONS_COUNT);
            if (nodeValue != null) {
              if (pendingTxnsCount >= 0) pendingTxnsCount += nodeValue.intValue();
            } else {
              pendingTxnsCount = -1;
            }
          }
        }

        double creationRate = -1d;
        long now = System.currentTimeMillis();
        if (lastObjectCountTime != -1 && liveObjectCount != -1) {
          double newObjectsCount = liveObjectCount - lastObjectCount;
          if (newObjectsCount >= 0) {
            double timeDiff = now - (double) lastObjectCountTime;
            creationRate = (newObjectsCount / timeDiff) * 1000;
          }
        }
        if (liveObjectCount >= 0) {
          lastObjectCount = liveObjectCount;
        }
        lastObjectCountTime = now;

        if (txnRate != -1) txnRateDialInfo.setValue(Double.valueOf(txnRate));
        if (creationRate != -1) creationRateDialInfo.setValue(Double.valueOf(creationRate));
        if (broadcastRate != -1) broadcastRateDialInfo.setValue(Double.valueOf(broadcastRate));
        if (lockRecallRate != -1) lockRecallRateDialInfo.setValue(Double.valueOf(lockRecallRate));
        if (faultRate != -1) faultRateDialInfo.setValue(Double.valueOf(faultRate));
        if (flushRate != -1) flushRateDialInfo.setValue(Double.valueOf(flushRate));
        if (txnSizeRate != -1) txnSizeRateDialInfo.setValue(Double.valueOf(txnSizeRate / 1000d));
        if (pendingTxnsCount != -1) pendingTxnsDialInfo.setValue(Integer.valueOf(pendingTxnsCount));

        // chartsPanel.revalidate();
        // chartsPanel.repaint();
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

  private class DialChartPanel extends ChartPanel {
    private final DialInfo      dialInfo;
    private final Insets        myInsets  = new Insets(0, 0, 0, 0);
    private final Dimension     mySize    = new Dimension();
    private final MyEmptyBorder border    = new MyEmptyBorder();
    private final String        tipFormat = appContext.getString("dashboard.dial.tip.format");

    private DialChartPanel(JFreeChart chart, DialInfo dialInfo) {
      super(chart, ChartPanel.DEFAULT_WIDTH, ChartPanel.DEFAULT_HEIGHT, ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
            ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT, ChartPanel.DEFAULT_MAXIMUM_DRAW_WIDTH,
            ChartPanel.DEFAULT_MAXIMUM_DRAW_HEIGHT, false, false, false, false, false, true);
      setBorder(border);
      this.dialInfo = dialInfo;
    }

    public void updateMyInsets() {
      Dimension origSize = getParent().getSize();

      mySize.width = Math.min(origSize.width, origSize.height);
      mySize.height = Math.min(mySize.width, origSize.height);

      myInsets.left = myInsets.right = (origSize.width - mySize.width) / 2;
      myInsets.top = myInsets.bottom = (origSize.height - mySize.height) / 2;

      border.updateInsets();
    }

    @Override
    public void paintComponent(Graphics g) {
      updateMyInsets();
      super.paintComponent(g);
    }

    @Override
    public String getToolTipText(MouseEvent me) {
      long max = dialInfo.getMax();
      long average = (long) dialInfo.getAverage();
      return MessageFormat.format(tipFormat, dialInfo.name, max, average);
    }

    private class MyEmptyBorder extends EmptyBorder {
      private MyEmptyBorder() {
        super(0, 0, 0, 0);
      }

      private void updateInsets() {
        this.top = myInsets.top;
        this.right = myInsets.right;
        this.bottom = myInsets.bottom;
        this.left = myInsets.left;
      }
    }
  }

  public JComponent createDialChartPanel(JFreeChart chart, DialInfo dialInfo) {
    DialChartPanel chartPanel = new DialChartPanel(chart, dialInfo);
    XContainer chartHolder = new XContainer(new BorderLayout());
    chartHolder.add(chartPanel);
    return chartHolder;
  }

  public static JFreeChart createDial(String dialLabel, DialInfo dialInfo, StandardDialScale scale,
                                      StandardDialRange[] ranges) {
    DialPlot plot = new DialPlot();
    plot.setView(0.0, 0.0, 1.0, 1.0);
    plot.setDialFrame(new StandardDialFrame());

    plot.setDataset(0, dialInfo.maxDataset);
    DialPointer.Pin pin = new DialPointer.Pin(0);
    pin.setPaint(Color.gray);
    pin.setRadius(0.7);
    plot.addPointer(pin);

    plot.setDataset(1, dialInfo.pinnedDataset);
    DialPointer.Pointer pointer = new DialPointer.Pointer(1);
    pointer.setFillPaint(Color.black);
    pointer.setOutlinePaint(Color.black);
    plot.addPointer(pointer);

    DialTextAnnotation text = new DialTextAnnotation(dialLabel);
    text.setFont(new Font("DialogInput", Font.PLAIN, 14));
    text.setRadius(0.17);
    plot.addLayer(text);

    plot.setDataset(2, dialInfo.dataset);

    DialValueIndicator dvi = new DialValueIndicator(2);
    dvi.setNumberFormat(new DecimalFormat("#,###"));
    dvi.setFont(new Font("Monospaced", Font.PLAIN, 14));
    dvi.setRadius(0.68);
    dvi.setOutlinePaint(Color.black);
    if (scale != null) {
      dvi.setTemplateValue(Double.valueOf(scale.getUpperBound()));
    }
    dvi.setInsets(new RectangleInsets(5, 20, 5, 20));
    plot.addLayer(dvi);
    dialInfo.valueIndicator = dvi;

    if (scale != null) {
      plot.addScale(0, scale);
    }

    DialCap cap = new DialCap();
    cap.setFillPaint(Color.black);
    plot.setCap(cap);

    plot.setBackground(new DialBackground(Color.white));

    for (StandardDialRange range : ranges) {
      range.setInnerRadius(0.90);
      range.setOuterRadius(0.95);
      plot.addLayer(range);
    }

    return new JFreeChart(plot);
  }

  @Override
  protected void setup(XContainer runtimeStatsPanel) {
    JFreeChart chart;
    StandardDialRange[] ranges;
    double startAngle = -140, extent = -260;
    StandardDialScale defScale = DemoChartFactory.createStandardDialScale(0, scale(5000), startAngle, extent,
                                                                          scale(1000), 4);
    StandardDialScale scale;
    Font labelFont = (Font) appContext.getObject("header.label.font");

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 1.0;
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 1, 1, 1);

    runtimeStatsPanel.setLayout(new GridBagLayout());

    ranges = new StandardDialRange[] { new StandardDialRange(scale(4000), scale(4500), Color.orange),
        new StandardDialRange(scale(4500), scale(5000), Color.red) };
    chart = createDial(appContext.getString("dashboard.txn-rate"), txnRateDialInfo, defScale, ranges);
    DialPlot plot = (DialPlot) chart.getPlot();
    DialPointer.Pointer p = (DialPointer.Pointer) plot.getPointerForDataset(1);
    p.setFillPaint(Color.red);
    p.setOutlinePaint(Color.red);
    runtimeStatsPanel.add(createDialChartPanel(chart, txnRateDialInfo), gbc);
    gbc.gridx++;

    ranges = new StandardDialRange[] { new StandardDialRange(scale(5000), scale(6000), Color.orange),
        new StandardDialRange(scale(6000), scale(7000), Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, scale(7000), startAngle, extent, scale(1000), 4);
    chart = createDial(appContext.getString("dashboard.object-creation-rate"), creationRateDialInfo, scale, ranges);
    runtimeStatsPanel.add(createDialChartPanel(chart, creationRateDialInfo), gbc);
    gbc.gridx++;

    ranges = new StandardDialRange[] { new StandardDialRange(scale(4000), scale(4500), Color.orange),
        new StandardDialRange(scale(4500), scale(5000), Color.red) };
    chart = createDial(appContext.getString("dashboard.lock-recall-rate"), lockRecallRateDialInfo, defScale, ranges);
    runtimeStatsPanel.add(createDialChartPanel(chart, lockRecallRateDialInfo), gbc);
    gbc.gridx++;

    ranges = new StandardDialRange[] { new StandardDialRange(scale(4000), scale(4500), Color.orange),
        new StandardDialRange(scale(4500), scale(5000), Color.red) };
    chart = createDial(appContext.getString("dashboard.broadcast-rate"), broadcastRateDialInfo, defScale, ranges);
    runtimeStatsPanel.add(createDialChartPanel(chart, broadcastRateDialInfo), gbc);
    gbc.gridx++;

    ranges = new StandardDialRange[] { new StandardDialRange(scale(4000), scale(4500), Color.orange),
        new StandardDialRange(scale(4500), scale(5000), Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, scale(5000), startAngle, extent, scale(1000), 4);
    chart = createDial(appContext.getString("dashboard.fault-rate"), faultRateDialInfo, scale, ranges);
    runtimeStatsPanel.add(createDialChartPanel(chart, faultRateDialInfo), gbc);
    gbc.gridx++;

    chart = createDial(appContext.getString("dashboard.flush-rate"), flushRateDialInfo, scale, ranges);
    runtimeStatsPanel.add(createDialChartPanel(chart, flushRateDialInfo), gbc);
    gbc.gridx++;

    ranges = new StandardDialRange[] { new StandardDialRange(scale(60), scale(80), Color.orange),
        new StandardDialRange(scale(80), scale(100), Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, scale(100), startAngle, extent, scale(10), 4);
    chart = createDial(appContext.getString("dashboard.txn-size-rate"), txnSizeRateDialInfo, scale, ranges);
    runtimeStatsPanel.add(createDialChartPanel(chart, txnSizeRateDialInfo), gbc);
    gbc.gridx++;

    ranges = new StandardDialRange[] { new StandardDialRange(scale(60), scale(90), Color.orange),
        new StandardDialRange(scale(90), scale(100), Color.red) };
    scale = DemoChartFactory.createStandardDialScale(0, scale(100), startAngle, extent, scale(10), 4);
    chart = createDial(appContext.getString("dashboard.unacked-txns"), pendingTxnsDialInfo, scale, ranges);
    runtimeStatsPanel.add(createDialChartPanel(chart, pendingTxnsDialInfo), gbc);

    gbc.gridx = 0;
    gbc.gridy++;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.ipadx = 0;

    XLabel label = new XLabel(appContext.getString("dashboard.transactions"));
    label.setFont(labelFont);
    label.setBorder(BorderFactory.createEtchedBorder());
    label.setHorizontalAlignment(SwingConstants.CENTER);
    runtimeStatsPanel.add(label, gbc);

    gbc.gridx++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.ipadx = 0;

    label = new XLabel(appContext.getString("dashboard.impeding-factors"));
    label.setFont(labelFont);
    label.setBorder(BorderFactory.createEtchedBorder());
    label.setHorizontalAlignment(SwingConstants.CENTER);
    runtimeStatsPanel.add(label, gbc);
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  private static class DialInfo {
    String              name;
    Number              maxValue;
    Number              localMaxValue     = Double.valueOf(0);
    DefaultValueDataset dataset           = new DefaultValueDataset(0);
    DefaultValueDataset pinnedDataset     = new DefaultValueDataset(0);
    DefaultValueDataset maxDataset        = new DefaultValueDataset(0);
    int                 maxIterationCount = 0;
    DialValueIndicator  valueIndicator;
    long                runningTotal;
    long                sampleCount;
    long                max;

    DialInfo(String name, Number maxValue) {
      this.name = name;
      this.maxValue = maxValue;
    }

    void setValue(Number value) {
      dataset.setValue(value);
      pinnedDataset.setValue(value.doubleValue() <= maxValue.doubleValue() ? value : maxValue);
      if (value.doubleValue() > maxDataset.getValue().doubleValue()) {
        setMaxValue(value);
      } else {
        if (maxIterationCount >= HISTORY_ITERATION_MAX) {
          setMaxValue(localMaxValue);
        } else {
          localMaxValue = Math.max(localMaxValue.doubleValue(), value.doubleValue());
          maxIterationCount++;
        }
      }
      boolean isMaxed = dataset.getValue().doubleValue() > maxValue.doubleValue();
      valueIndicator.setPaint(isMaxed ? Color.red : Color.black);

      runningTotal += dataset.getValue().longValue();
      sampleCount++;
      max = Math.max(max, dataset.getValue().longValue());
    }

    void setMaxValue(Number value) {
      maxDataset.setValue(Math.min(value.doubleValue(), maxValue.doubleValue()));
      maxIterationCount = 0;
      localMaxValue = Double.valueOf(0);
    }

    double getAverage() {
      return runningTotal / ((double) sampleCount);
    }

    long getMax() {
      return max;
    }
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

      txnRateDialInfo = null;
      creationRateDialInfo = null;
      broadcastRateDialInfo = null;
      lockRecallRateDialInfo = null;
      flushRateDialInfo = null;
      faultRateDialInfo = null;
      txnSizeRateDialInfo = null;
      pendingTxnsDialInfo = null;
    }
  }
}
