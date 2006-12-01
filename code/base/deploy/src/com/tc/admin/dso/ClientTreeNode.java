/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.PollerNode;
import com.tc.admin.common.RatePanel;

import javax.management.ObjectName;

public class ClientTreeNode extends ComponentNode {
  private DSOClient          m_client;
  private CacheActivityPanel m_cacheActivity;
  private RatePanel          m_txnRate;
  private ClientStatsPanel   m_clientStats;

  public ClientTreeNode(ConnectionContext cc, DSOClient client) {
    super(client.getRemoteAddress());
    setComponent(new ClientPanel(m_client = client));

    AdminClientContext acc  = AdminClient.getContext();
    int                i    = 0;
    ObjectName         bean = client.getObjectName();
    ComponentNode      node;

    m_cacheActivity = new CacheActivityPanel(cc, bean);
    node  = new PollerNode(acc.getMessage("dso.cache.activity"),
                           m_cacheActivity);
    
    m_cacheActivity.setNode(node);
    insert(node, i++);

    String stat   = "TransactionRate";
    String header = acc.getMessage("dso.transaction.rate");
    String xAxis  = null;
    String yAxis  = acc.getMessage("dso.transaction.rate.range.label");
    
    m_txnRate = new RatePanel(cc, bean, stat, header, xAxis, yAxis);
    m_txnRate.setNode(node = new PollerNode(header, m_txnRate));
    insert(node, i++);

    m_clientStats = new ClientStatsPanel(cc, bean);
    node = new PollerNode(acc.getMessage("dso.all.statistics"),
                          m_clientStats);
    m_clientStats.setNode(node);
    insert(node, i++);
  }

  public DSOClient getClient() {
    return m_client;
  }

  public void tearDown() {
    m_cacheActivity.stop();
    m_txnRate.stop();
    m_clientStats.stop();

    super.tearDown();

    m_cacheActivity = null;
    m_txnRate = null;
    m_clientStats = null;
  }
}
