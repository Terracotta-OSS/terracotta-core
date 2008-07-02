/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IMapEntry;
import com.tc.admin.model.IObject;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

public class MapEntryNode extends XTreeNode implements DSOObjectTreeNode {
  protected IMapEntry m_mapEntry;
  protected XTreeNode m_keyNode;
  protected XTreeNode m_valueNode;

  public MapEntryNode(IMapEntry mapEntry) {
    super(mapEntry);
    m_mapEntry = mapEntry;
    initChildren();
  }

  public IObject getObject() {
    return m_mapEntry;
  }

  protected void initChildren() {
    IObject key = m_mapEntry.getKey();
    IObject value = m_mapEntry.getValue();

    add(newObjectNode(key));
    add(value != null ? newObjectNode(value) : new XTreeNode("value=null"));

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

    m_mapEntry = null;
    m_keyNode = null;
    m_valueNode = null;
  }
}
