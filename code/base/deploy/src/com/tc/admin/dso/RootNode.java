/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.IComponentProvider;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterNode;

import java.awt.Component;

public class RootNode extends BasicObjectNode implements IComponentProvider {
  protected IClusterNode m_clusterNode;
  private BasicObjectSetPanel m_rootsPanel;

  public RootNode(IClusterNode clusterNode, IBasicObject root) {
    super(root);
    m_clusterNode = clusterNode;
  }

  protected void init() {
    /**/
  }

  public int getChildCount() {
    return 0;
  }
  
  public Component getComponent() {
    if (m_rootsPanel == null) {
      m_rootsPanel = new BasicObjectSetPanel(m_clusterNode, new IBasicObject[] { m_object });
      m_rootsPanel.setNode(this);
    }

    return m_rootsPanel;
  }

  public void tearDown() {
    super.tearDown();

    if (m_rootsPanel != null) {
      m_rootsPanel.tearDown();
      m_rootsPanel = null;
    }
  }
}
