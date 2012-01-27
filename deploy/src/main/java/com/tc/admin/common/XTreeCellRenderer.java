/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class XTreeCellRenderer extends DefaultTreeCellRenderer {
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
                                                int row, boolean focused) {
    Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
    if (value instanceof XTreeNode) {
      setIcon(((XTreeNode) value).getIcon());
    }
    return comp;
  }
}
