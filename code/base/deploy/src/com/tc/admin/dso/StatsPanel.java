/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Container;
import org.dijon.ContainerResource;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.RatePanel;
import com.tc.admin.common.DoubleRatePanel;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.Poller;

import java.awt.BorderLayout;

import javax.management.ObjectName;

public class StatsPanel extends XContainer implements Poller {
  private CacheActivityPanel m_cacheActivity;
  private RatePanel          m_txnRate;
  private DoubleRatePanel    m_cacheHitRatio;

  public StatsPanel(ConnectionContext cc, ObjectName bean) {
    AdminClientContext acc = AdminClient.getContext();

    load((ContainerResource)acc.topRes.getComponent("StatsPanel"));

    m_cacheActivity = new CacheActivityPanel(cc, bean);
    addPanel("Panel1", m_cacheActivity);

    String stat   = "TransactionRate";
    String header = acc.getMessage("dso.transaction.rate");
    String yAxis  = acc.getMessage("dso.transaction.rate.range.label");
    String xAxis  = null;

    m_txnRate = new RatePanel(cc, bean, stat, header, xAxis, yAxis);
    addPanel("Panel2", m_txnRate);

    stat   = "CacheHitRatio";
    header = acc.getMessage("dso.cache.hit.ratio");
    yAxis  = acc.getMessage("dso.cache.hit.ration.range.label");

    m_cacheHitRatio =
      new DoubleRatePanel(cc, bean, stat, header, xAxis, yAxis);
    m_cacheHitRatio.getRangeAxis().setRange(0d, 1d);

    addPanel("Panel3", m_cacheHitRatio);
  }

  private void addPanel(String parentPanelName, XContainer panel) {
    Container parentPanel = (Container)getChild(parentPanelName);

    parentPanel.setLayout(new BorderLayout());
    parentPanel.add(panel);
  }

  public void stop() {
    if(m_cacheActivity != null)
      m_cacheActivity.stop();
    
    if(m_txnRate != null)
      m_txnRate.stop();
    
    if(m_cacheHitRatio != null)
      m_cacheHitRatio.stop();
  }

  public void start() {
    if(m_cacheActivity != null)
      m_cacheActivity.start();

    if(m_txnRate != null)
      m_txnRate.start();

    if(m_cacheHitRatio != null)
      m_cacheHitRatio.start();
  }

  public void tearDown() {
    stop();

    super.tearDown();

    m_cacheActivity = null;
    m_txnRate = null;
    m_cacheHitRatio = null;
  }
}
