/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

/**
 * This is a TreeCellRenderer that can delegate to either a node-specific renderer or to the default TreeCellRenderer.
 */

public class XTreeCellRendererDelegate implements TreeCellRenderer {
  protected XTreeCellRenderer defaultRenderer;

  public XTreeCellRendererDelegate() {
    defaultRenderer = new XTreeCellRenderer();
  }

  protected TreeCellRenderer getNodeRenderer(Object value) {
    TreeCellRenderer nodeRenderer = null;
    if (value instanceof XTreeNode) {
      nodeRenderer = ((XTreeNode) value).getRenderer();
    }
    return nodeRenderer != null ? nodeRenderer : defaultRenderer;
  }

  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
                                                int row, boolean focused) {
    Component c = getNodeRenderer(value).getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
    if (value instanceof XTreeNode) {
      c.setEnabled(((XTreeNode) value).isEnabled());
    }
    return c;
  }
}
