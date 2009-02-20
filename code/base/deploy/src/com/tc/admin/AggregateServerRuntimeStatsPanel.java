/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import static com.tc.admin.model.IClusterModel.PollScope.ACTIVE_SERVERS;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FAULT_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_OBJECT_FLUSH_RATE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_TRANSACTION_RATE;
import static com.tc.admin.model.IServer.POLLED_ATTR_CACHE_MISS_RATE;

import org.jfree.chart.ChartPanel;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.PolledAttributesResult;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.border.TitledBorder;

public class AggregateServerRuntimeStatsPanel extends BaseRuntimeStatsPanel {
  private IClusterModel            clusterModel;

  private TimeSeries               flushRateSeries;
  private TimeSeries               faultRateSeries;
  private TimeSeries               txnRateSeries;
  private TimeSeries               cacheMissRateSeries;

  private static final Set<String> POLLED_ATTRIBUTE_SET = new HashSet(Arrays.asList(POLLED_ATTR_OBJECT_FLUSH_RATE,
                                                                                    POLLED_ATTR_OBJECT_FAULT_RATE,
                                                                                    POLLED_ATTR_TRANSACTION_RATE,
                                                                                    POLLED_ATTR_CACHE_MISS_RATE));

  public AggregateServerRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(appContext);
    this.clusterModel = clusterModel;
    setup(chartsPanel);
    setName(clusterModel.toString());
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
    addPolledAttributeListener();
    super.startMonitoringRuntimeStats();
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

  private synchronized void handleDSOStats(PolledAttributesResult result) {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      tmpDate.setTime(System.currentTimeMillis());

      long flush = 0L;
      long fault = 0L;
      long txn = 0L;
      long cacheMiss = 0L;
      Number n;

      for (IServerGroup group : theClusterModel.getServerGroups()) {
        for (IServer theServer : group.getMembers()) {
          if (theServer.isReady()) {
            if ((n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OBJECT_FLUSH_RATE)) != null) {
              flush += n.longValue();
            }
            if ((n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_OBJECT_FAULT_RATE)) != null) {
              fault += n.longValue();
            }
            if ((n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_TRANSACTION_RATE)) != null) {
              txn += n.longValue();
            }
            if ((n = (Number) result.getPolledAttribute(theServer, POLLED_ATTR_CACHE_MISS_RATE)) != null) {
              cacheMiss += n.longValue();
            }
          }
        }
      }

      updateSeries(flushRateSeries, Long.valueOf(flush));
      updateSeries(faultRateSeries, Long.valueOf(fault));
      updateSeries(txnRateSeries, Long.valueOf(txn));
      updateSeries(cacheMissRateSeries, Long.valueOf(cacheMiss));
    }
  }

  @Override
  protected synchronized void setup(XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(0, 2));
    setupTxnRatePanel(chartsPanel);
    setupCacheMissRatePanel(chartsPanel);
    setupFlushRatePanel(chartsPanel);
    setupFaultRatePanel(chartsPanel);
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

    Iterator<TimeSeries> iter = list.iterator();
    while (iter.hasNext()) {
      iter.next().clear();
    }
  }

  @Override
  public synchronized void tearDown() {
    stopMonitoringRuntimeStats();
    clusterModel = null;
    super.tearDown();
    clearAllTimeSeries();
  }
}
