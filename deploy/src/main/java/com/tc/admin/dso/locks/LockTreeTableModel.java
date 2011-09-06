/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.treetable.AbstractTreeTableModel;
import com.tc.admin.common.treetable.TreeTableModel;
import com.tc.management.lock.stats.LockSpec;
import com.tc.object.locks.LockID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;

public class LockTreeTableModel extends AbstractTreeTableModel {
  private static final String[] cNames      = { "Lock", "<html>Times<br>Requested</html>",
      "<html>Times<br>Hopped</html>", "<html>Average<br>Contenders</html>", "<html>Average<br>Acquire Time</html>",
      "<html>Average<br>Held Time</html>", "<html>Average Nested<br>Lock Depth</html>" };
  private static final Class[]  cTypes      = { TreeTableModel.class, Long.class, Long.class, Long.class, Long.class,
      Long.class, Long.class               };
  private String[]              fColumnTips;

  private RootLockNode          fRoot;

  public LockTreeTableModel(ApplicationContext appContext, Collection<LockSpec> lockInfos) {
    this(appContext, new RootLockNode(lockInfos));
  }

  public LockTreeTableModel(ApplicationContext appContext, RootLockNode root) {
    super(root);
    fRoot = (RootLockNode) getRoot();
    fColumnTips = (String[]) appContext.getObject("dso.locks.column.tips");
  }

  public LockNode getLockNode(LockID lockID) {
    return fRoot.getLockNode(lockID);
  }

  public TreePath getLockNodePath(LockID lockID) {
    LockNode lockNode = getLockNode(lockID);
    if (lockNode != null) { return new TreePath(new Object[] { fRoot, lockNode }); }
    return null;
  }

  public void notifyChanged() {
    fireTreeStructureChanged(this, new TreePath[] { new TreePath(fRoot) }, null, null);
  }

  private void sort(LockNode[] nodes, final int col, final int direction) {
    if (nodes == null || nodes.length == 0) return;

    Comparator c = new Comparator<LockNode>() {
      public int compare(LockNode o1, LockNode o2) {
        Comparable prev = (Comparable) o1.getValueAt(col);
        Object next = o2.getValueAt(col);
        int diff = prev.compareTo(next);
        return (direction == SwingConstants.SOUTH) ? diff : -diff;
      }
    };
    Arrays.sort(nodes, c);

    for (int i = 0; i < nodes.length; i++) {
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

  public String getColumnTip(int column) {
    return fColumnTips[column];
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
