/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;

public class StatsRecorderNode extends ComponentNode {
  private ClusterNode        m_clusterNode;
  private StatsRecorderPanel m_statsRecorderPanel;

  public StatsRecorderNode(ClusterNode clusterNode) {
    super();
    m_clusterNode = clusterNode;
    setLabel(AdminClient.getContext().getMessage("stats.recorder.node.label"));
    setIcon(ServerHelper.getHelper().getStatsRecorderIcon());
    setComponent(m_statsRecorderPanel = new StatsRecorderPanel(this));
  }

  boolean isRecording() {
    return m_statsRecorderPanel.isRecording();
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

  void notifyChanged() {
    nodeChanged();
    m_clusterNode.notifyChanged();
  }
}
