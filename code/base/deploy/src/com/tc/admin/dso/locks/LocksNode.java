/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;

import javax.swing.Icon;

public class LocksNode extends ComponentNode {
  private ConnectionContext m_cc;
  private String m_baseLabel;

  public LocksNode(ConnectionContext cc) {
    super();

    m_cc = cc;

    setLabel(m_baseLabel = AdminClient.getContext().getMessage("dso.locks"));
    setComponent(new LocksPanel(m_cc, this));
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
    m_cc = null;
  }
}
