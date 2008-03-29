/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.auth.AuthScope;

import com.tc.admin.common.ComponentNode;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;

public class StatsRecorderNode extends ComponentNode {
  private ClusterNode        m_clusterNode;
  private StatsRecorderPanel m_statsRecorderPanel;
  private AuthScope          m_authScope;

  public StatsRecorderNode(ClusterNode clusterNode) {
    super();
    m_clusterNode = clusterNode;
    setLabel(AdminClient.getContext().getMessage("stats.recorder.node.label"));
    setIcon(ServerHelper.getHelper().getStatsRecorderIcon());
    setComponent(m_statsRecorderPanel = new StatsRecorderPanel(this));
  }

  void makeUnavailable() {
    m_clusterNode.makeStatsRecorderUnavailable();
  }

  boolean isRecording() {
    return m_statsRecorderPanel != null && m_statsRecorderPanel.isRecording();
  }

  void testTriggerThreadDumpSRA() {
    m_statsRecorderPanel.testTriggerThreadDumpSRA();
  }

  ConnectionContext getConnectionContext() {
    return m_clusterNode.getConnectionContext();
  }

  StatisticsLocalGathererMBean getStatisticsGathererMBean() {
    return m_clusterNode.getStatisticsGathererMBean();
  }

  String getStatsExportServletURI() throws Exception {
    return m_clusterNode.getStatsExportServletURI();
  }

  AuthScope getAuthScope() throws Exception {
    if (m_authScope != null) return m_authScope;
    return m_authScope = m_clusterNode.getAuthScope();
  }

  String getActiveServerAddress() throws Exception {
    return m_clusterNode.getHost()+":"+m_clusterNode.getDSOListenPort();
  }
  
  void notifyChanged() {
    nodeChanged();
    m_clusterNode.notifyChanged();
  }

  public void tearDown() {
    super.tearDown();
    m_clusterNode = null;
    m_statsRecorderPanel = null;
    m_authScope = null;
  }
}
