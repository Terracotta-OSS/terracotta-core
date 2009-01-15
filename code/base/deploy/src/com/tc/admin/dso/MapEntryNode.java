/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IMapEntry;
import com.tc.admin.model.IObject;

import java.util.Vector;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;

public class MapEntryNode extends XTreeNode implements DSOObjectTreeNode {
  protected IMapEntry mapEntry;
  protected XTreeNode keyNode;
  protected XTreeNode valueNode;

  public MapEntryNode(IMapEntry mapEntry) {
    super(mapEntry);
    this.mapEntry = mapEntry;
    init();
  }

  public IObject getObject() {
    return mapEntry;
  }

  protected void init() {
    if (children == null) {
      children = new Vector();
    }
    children.setSize(2);
  }

  public TreeNode getChildAt(int index) {
    if (children != null && children.elementAt(index) == null) {
      fillInChildren();
    }
    return super.getChildAt(index);
  }

  protected void fillInChildren() {
    IObject key = mapEntry.getKey();
    IObject value = mapEntry.getValue();
    XTreeNode child;

    child = newObjectNode(key);
    children.setElementAt(child, 0);
    child.setParent(MapEntryNode.this);

    child = value != null ? newObjectNode(value) : new XTreeNode("value=null");
    children.setElementAt(child, 1);
    child.setParent(MapEntryNode.this);

    if (key == null) {
      SwingUtilities.invokeLater(new AncestorReaper());
    }
  }

  private XTreeNode newObjectNode(IObject object) {
    if (object instanceof IMapEntry) {
      return new MapEntryNode((IMapEntry) object);
    } else if (object instanceof IBasicObject) {
      BasicObjectTreeModel model = (BasicObjectTreeModel) getModel();
      return model.newObjectNode((IBasicObject) object);
    } else {
      return new XTreeNode("NoSuchObject");
    }
  }

  class AncestorReaper implements Runnable {
    public void run() {
      XTreeNode node = (XTreeNode) getParent();
      while (node != null) {
        if (node instanceof BasicObjectNode) {
          BasicObjectNode ftn = (BasicObjectNode) node;
          if (ftn.isObjectValid()) {
            ftn.refreshChildren();
            return;
          }
        }
        node = (XTreeNode) node.getParent();
      }
    }
  }

  public Icon getIcon() {
    return RootsHelper.getHelper().getFieldIcon();
  }

  public void tearDown() {
    super.tearDown();

    mapEntry = null;
    keyNode = null;
    valueNode = null;
  }
}
