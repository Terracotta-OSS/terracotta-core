/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.common.treetable.AbstractTreeTableModel;
import com.tc.admin.common.treetable.TreeTableModel;
import com.tc.management.beans.LockStatisticsMonitorMBean;

import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;

public class LockTreeTableModel extends AbstractTreeTableModel {
  static protected String[] cNames = { "Lock", "<html>Times<br>Requested</html>", "<html>Times<br>Hopped</html>",
      "<html>Average<br>Contenders</html>", "<html>Average<br>Acquire Time</html>",
      "<html>Average<br>Held Time</html>", "<html>Average Nested<br>Lock Depth</html>" };
  static protected Class[]  cTypes = { TreeTableModel.class, Long.class, Long.class, Long.class, Long.class,
      Long.class, Long.class      };

  private RootLockNode      fRoot;

  public LockTreeTableModel(LockStatisticsMonitorMBean lockStats) {
    this(new RootLockNode(lockStats));
  }

  public LockTreeTableModel(RootLockNode root) {
    super(root);
    fRoot = (RootLockNode) getRoot();
  }

  public void init() {
    fRoot.init();
    fireTreeStructureChanged(this, new TreePath[] { new TreePath(fRoot) }, null, null);
  }

  /**
   * All the elements of LockNode are either String or Long, which are comparable's.
   */
  private boolean compareAdjacentNodes(LockNode[] nodes, int direction, int row, int col) {
    Comparable prev = (Comparable) nodes[row - 1].getValueAt(col);
    Object next = nodes[row].getValueAt(col);
    int diff = prev.compareTo(next);

    return (direction == SwingConstants.SOUTH) ? (diff > 0) : (diff < 0);
  }

  private void sort(LockNode[] nodes, int col, int direction) {
    int count = nodes.length;

    for (int i = 0; i < count; i++) {
      for (int j = i; j > 0 && compareAdjacentNodes(nodes, direction, j, col); j--) {
        LockNode tmp = nodes[j];
        nodes[j] = nodes[j - 1];
        nodes[j - 1] = tmp;
      }
    }

    for (int i = 0; i < count; i++) {
      sort(nodes[i].children(), col, direction);
    }
  }

  public void sort(int col, int direction) {
    sort(fRoot.children(), col, direction);
    fireTreeStructureChanged(this, new Object[] { fRoot }, new int[0], null);
  }

  public boolean isRootVisible() {
    return false;
  }

  public int getColumnCount() {
    return cNames.length;
  }

  public String getColumnName(int column) {
    return cNames[column];
  }

  public Class getColumnClass(int column) {
    return cTypes[column];
  }

  public Object getValueAt(Object node, int column) {
    return ((LockNode) node).getValueAt(column);
  }

  protected Object[] getChildren(Object node) {
    LockNode lockNode = ((LockNode) node);
    return lockNode.children();
  }

  public int getChildCount(Object node) {
    Object[] children = getChildren(node);
    return (children == null) ? 0 : children.length;
  }

  public Object getChild(Object node, int i) {
    return getChildren(node)[i];
  }

}
