package com.tc.admin.common;

import javax.swing.tree.DefaultTreeModel;

public class XTreeModel extends DefaultTreeModel {
  public XTreeModel() {
    this(new XRootNode());
  }

  public XTreeModel(XRootNode root) {
    super(root);
    ((XRootNode)getRoot()).setModel(this);
  }
}
