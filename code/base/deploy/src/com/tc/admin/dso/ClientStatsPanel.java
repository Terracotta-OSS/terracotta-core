/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Container;
import org.dijon.ContainerResource;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.Poller;
import com.tc.admin.common.RatePanel;
import com.tc.admin.common.XContainer;

import java.awt.BorderLayout;

import javax.management.ObjectName;
import javax.swing.SwingConstants;

public class ClientStatsPanel extends XContainer implements Poller {
  private CacheActivityPanel m_cacheActivity;
  private RatePanel          m_txnRate;

  public ClientStatsPanel(ConnectionContext cc, ObjectName bean) {
    AdminClientContext acc = AdminClient.getContext();

    load((ContainerResource)acc.topRes.getComponent("ClientStatsPanel"));

    m_cacheActivity = new CacheActivityPanel(cc, bean, SwingConstants.VERTICAL);
    addPanel("Panel1", m_cacheActivity);

    String stat   = "TransactionRate";
    String header = acc.getMessage("dso.transaction.rate");
    String xAxis  = null;
    String yAxis  = acc.getMessage("dso.transaction.rate.range.label");
    
    m_txnRate = new RatePanel(cc, bean, stat, header, xAxis, yAxis);
    addPanel("Panel2", m_txnRate);
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
  }

  public void start() {
    if(m_cacheActivity != null)
      m_cacheActivity.start();
    
    if(m_txnRate != null)
      m_txnRate.start();
  }

  public void tearDown() {
    stop();

    super.tearDown();

    m_cacheActivity = null;
    m_txnRate = null;
  }
}
