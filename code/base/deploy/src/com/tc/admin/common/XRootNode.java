/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

public class XRootNode extends XTreeNode {
  private XTreeModel m_model;

  public XRootNode() {
    super();
  }

  public XRootNode(Object userData) {
    super(userData);
  }

  public void setModel(XTreeModel model) {
    m_model = model;
  }

  public XTreeModel getModel() {
    return m_model;
  }
}
