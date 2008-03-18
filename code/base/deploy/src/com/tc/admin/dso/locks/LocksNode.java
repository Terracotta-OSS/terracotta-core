/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.AdminClient;
import com.tc.admin.ClusterNode;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;

import javax.swing.Icon;

public class LocksNode extends ComponentNode {
  private ClusterNode       m_clusterNode;
  private String            m_baseLabel;

  public LocksNode(ClusterNode clusterNode) {
    super();
    m_clusterNode = clusterNode;
    setLabel(m_baseLabel = AdminClient.getContext().getMessage("dso.locks"));
    setComponent(new LocksPanel(this));
  }

  ConnectionContext getConnectionContext() {
    return m_clusterNode.getConnectionContext();
  }

  public void newConnectionContext() {
    ((LocksPanel)getComponent()).newConnectionContext();
  }
  
  public Icon getIcon() {
    return LocksHelper.getHelper().getLocksIcon();
  }

  public String getBaseLabel() {
    return m_baseLabel;
  }

  void notifyChanged() {
    nodeChanged();
  }

  public void tearDown() {
    super.tearDown();
    m_clusterNode = null;
    m_baseLabel = null;
  }
}
