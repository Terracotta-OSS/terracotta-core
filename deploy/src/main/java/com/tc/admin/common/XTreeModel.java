/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.tree.DefaultTreeModel;

public class XTreeModel extends DefaultTreeModel {
  public XTreeModel() {
    this(new XRootNode("root"));
  }

  public XTreeModel(XRootNode root) {
    super(root);
    ((XRootNode) getRoot()).setModel(this);
  }

  public void tearDown() {
    XRootNode theRoot = (XRootNode) getRoot();
    if (theRoot != null) {
      theRoot.tearDown();
    }
  }
}
