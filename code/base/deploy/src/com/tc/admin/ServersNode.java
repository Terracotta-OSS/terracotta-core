/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.config.schema.L2Info;

public class ServersNode extends ComponentNode {
  private ClusterNode m_clusterNode;

  public ServersNode(ClusterNode clusterNode) {
    super();
    m_clusterNode = clusterNode;
    init();
  }

  private void init() {
    AdminClientContext acc = AdminClient.getContext();
    L2Info[] l2s = m_clusterNode.getClusterMembers();
    for (L2Info l2Info : l2s) {
      add(acc.nodeFactory.createServerNode(l2Info.host(), l2Info.jmxPort(), true));
    }
    setComponent(new ServersPanel(this));
    setLabel(AdminClient.getContext().getMessage("servers") + " (" + getChildCount() + ")");
  }

  void newConnectionContext() {
    /**
     * what if the new active has a different config with a different set of cluster members? if so, we need to
     * reconstruct the serversPanel and child nodes. This would be bad.
     */
  }

  /**
   * Return any credentials that were used when the initial cluster server was connected.
   */
  String[] getParentCredentials() {
    return m_clusterNode.getServerConnectionManager().getCredentials();
  }

  public void tearDown() {
    int serverCount = getChildCount();
    for(int i = 0; i < serverCount; i++) {
      ((ServerNode)getChildAt(i)).handleDisconnect();
    }
    super.tearDown();
    m_clusterNode = null;
  }
}
