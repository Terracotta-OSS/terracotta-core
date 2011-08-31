/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.JPanel;

public class XContainer extends JPanel {
  private XTreeNode node;

  public XContainer() {
    super();
  }

  public XContainer(LayoutManager layout) {
    super(layout);
  }

  public XContainer(XTreeNode node) {
    this();
    setNode(node);
  }

  public void setNode(XTreeNode node) {
    this.node = node;
  }

  public XTreeNode getNode() {
    return node;
  }

  public void tearDown() {
    for (Component comp : getComponents()) {
      if (comp instanceof XContainer) {
        ((XContainer) comp).tearDown();
      } else if (comp instanceof XTabbedPane) {
        ((XTabbedPane) comp).tearDown();
      } else if (comp instanceof XSplitPane) {
        ((XSplitPane) comp).tearDown();
      }
    }
    removeAll();
    setNode(null);
  }
}
