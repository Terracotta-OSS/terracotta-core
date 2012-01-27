/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

public class XRootNode extends XTreeNode {
  private XTreeModel model;

  public XRootNode() {
    super();
  }

  public XRootNode(Object userData) {
    super(userData);
  }

  public void setModel(XTreeModel model) {
    this.model = model;
  }

  public XTreeModel getModel() {
    return model;
  }
}
