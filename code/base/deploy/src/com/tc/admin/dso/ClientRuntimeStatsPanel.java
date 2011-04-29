/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import static com.tc.admin.model.IClient.POLLED_ATTR_PENDING_TRANSACTIONS_COUNT;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_MAX_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FAULT_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FLUSH_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_TRANSACTION_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_USED_MEMORY;

import org.jfree.chart.ChartPanel;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributesResult;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public class ClientRuntimeStatsPanel extends ClusterNodeRuntimeStatsPanel {
  private final IClient            client;

  private TimeSeries               flushRateSeries;
  private XLabel                   flushRateLabel;
  private TimeSeries               faultRateSeries;
  private XLabel                   faultRateLabel;
  private TimeSeries               txnRateSeries;
  private XLabel                   txnRateLabel;
  private TimeSeries               pendingTxnsSeries;
  private XLabel                   pendingTxnsLabel;

  protected final String           flushRateLabelFormat   = "{0} Flushes/sec.";
  protected final String           faultRateLabelFormat   = "{0} Faults/sec.";
  protected final String           txnRateLabelFormat     = "{0} Txns/sec.";
  protected final String           pendingTxnsLabelFormat = "{0} Pending Txns/sec.";

  private static final Set<String> POLLED_ATTRIBUTE_SET   = new HashSet(
                                                                        Arrays
                                                                            .asList(POLLED_ATTR_USED_MEMORY,
                                                                                    POLLED_ATTR_MAX_MEMORY,
                                                                                    POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                    POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                    POLLED_ATTR_TRANSACTION_RATE,
                                                                                    POLLED_ATTR_PENDING_TRANSACTIONS_COUNT));

  public ClientRuntimeStatsPanel(ApplicationContext appContext, IClient client) {
    super(appContext, client);
    this.client = client;
    setup(chartsPanel);
  }

  @Override
  protected void addPolledAttributeListener() {
    client.addPolledAttributeListener(POLLED_ATTRIBUTE_SET, this);
  }

  @Override
  protected void removePolledAttributeListener() {
    super.removePolledAttributeListener();
    client.removePolledAttributeListener(POLLED_ATTRIBUTE_SET, this);
  }

  @Override
  public void attributesPolled(PolledAttributesResult result) {
    handleDSOStats(result);
    handleSysStats(result);
  }

  private void handleDSOStats(PolledAttributesResult result) {
    tmpDate.setTime(System.currentTimeMillis());

    long flush = 0;
    long fault = 0;
    long txn = 0;
    long pendingTxn = 0;
    Number n;

    for (IServerGroup group : client.getClusterModel().getServerGroups()) {
      for (IServer server : group.getMembers()) {
        if (server.isStarted()) {
          Map<ObjectName, Map<String, Object>> nodeMap = result.getAttributeMap(server);
          Map<String, Object> map = nodeMap != null ? nodeMap.get(client.getBeanName()) : null;
          if (map != null) {
            n = (Number) map.get(POLLED_ATTR_TRANSACTION_RATE);
            if (n != null) {
              txn += n.longValue();
            }
            n = (Number) map.get(POLLED_ATTR_OBJECT_FLUSH_RATE);
            if (n != null) {
              flush += n.longValue();
            }
            n = (Number) map.get(POLLED_ATTR_OBJECT_FAULT_RATE);
            if (n != null) {
              fault += n.longValue();
            }
            n = (Number) map.get(POLLED_ATTR_PENDING_TRANSACTIONS_COUNT);
            if (n != null) {
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
        updateAllDSOSeries(theFlushRate, theFaultRate, theTxnRate, thePendingTxnRate);
      }
    });
  }

  private void updateAllDSOSeries(long flush, long fault, long txn, long pendingTxn) {
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

  @Override
  protected void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(0, 2));
    setupOnHeapPanel(chartsPanel);
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

  @Override
  protected void clearAllTimeSeries() {
    if (flushRateSeries != null) {
      flushRateSeries.clear();
      flushRateSeries = null;
    }
    if (faultRateSeries != null) {
      faultRateSeries.clear();
      faultRateSeries = null;
    }
    if (txnRateSeries != null) {
      txnRateSeries.clear();
      txnRateSeries = null;
    }
    if (pendingTxnsSeries != null) {
      pendingTxnsSeries.clear();
      pendingTxnsSeries = null;
    }
  }
}
