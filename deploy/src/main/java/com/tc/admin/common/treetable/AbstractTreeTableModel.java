package com.tc.admin.common.treetable;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

public abstract class AbstractTreeTableModel implements TreeTableModel {
  protected Object            root;
  protected EventListenerList listenerList = new EventListenerList();

  public AbstractTreeTableModel(Object root) {
    this.root = root;
  }

  public Object getRoot() {
    return root;
  }

  public boolean isRootVisible() {
    return false;
  }

  public boolean isLeaf(Object node) {
    return getChildCount(node) == 0;
  }

  public void valueForPathChanged(TreePath path, Object newValue) {/**/
  }

  // This is not called in the JTree's default mode: use a naive implementation.
  public int getIndexOfChild(Object parent, Object child) {
    for (int i = 0; i < getChildCount(parent); i++) {
      if (getChild(parent, i).equals(child)) { return i; }
    }
    return -1;
  }

  public void addTreeModelListener(TreeModelListener l) {
    listenerList.add(TreeModelListener.class, l);
  }

  public void removeTreeModelListener(TreeModelListener l) {
    listenerList.remove(TreeModelListener.class, l);
  }

  /*
   * Notify all listeners that have registered interest for notification on this event type. The event instance is
   * lazily created using the parameters passed into the fire method.
   * @see EventListenerList
   */
  protected void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
      }
    }
  }

  /*
   * Notify all listeners that have registered interest for notification on this event type. The event instance is
   * lazily created using the parameters passed into the fire method.
   * @see EventListenerList
   */
  protected void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices, Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
      }
    }
  }

  /*
   * Notify all listeners that have registered interest for notification on this event type. The event instance is
   * lazily created using the parameters passed into the fire method.
   * @see EventListenerList
   */
  protected void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices, Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
      }
    }
  }

  /*
   * Notify all listeners that have registered interest for notification on this event type. The event instance is
   * lazily created using the parameters passed into the fire method.
   * @see EventListenerList
   */
  protected void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
      }
    }
  }

  public Class getColumnClass(int column) {
    return Object.class;
  }

  /**
   * By default, make the column with the Tree in it the only editable one. Making this column editable causes the
   * JTable to forward mouse and keyboard events in the Tree column to the underlying JTree.
   */
  public boolean isCellEditable(Object node, int column) {
    return getColumnClass(column) == TreeTableModel.class;
  }

  public void setValueAt(Object aValue, Object node, int column) {/**/
  }
}
