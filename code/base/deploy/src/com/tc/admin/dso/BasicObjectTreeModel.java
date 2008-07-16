/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterNode;
import com.tc.admin.model.IObject;
import com.tc.object.ObjectID;

public class BasicObjectTreeModel extends XTreeModel {
  private IClusterNode m_clusterNode;

  public BasicObjectTreeModel(IClusterNode clusterNode, IBasicObject[] roots) {
    super();

    m_clusterNode = clusterNode;
    XTreeNode rootNode = (XTreeNode) getRoot();
    if (roots != null && roots.length > 0) {
      for (IBasicObject object : roots) {
        rootNode.add(newObjectNode(object));
      }
    }
  }

  private static boolean isResident(IClusterNode clusterNode, IObject object) {
    while (object != null) {
      ObjectID oid = object.getObjectID();
      if (oid != null) { return clusterNode.isResident(oid); }
      object = object.getParent();
    }
    return false;
  }

  public static BasicObjectNode newObjectNode(IClusterNode clusterNode, IBasicObject object) {
    BasicObjectNode objectNode = new BasicObjectNode(object);
    objectNode.setResident(isResident(clusterNode, object));
    return objectNode;
  }

  public BasicObjectNode newObjectNode(IBasicObject object) {
    return newObjectNode(m_clusterNode, object);
  }

  public void refresh() {
    XTreeNode rootNode = (XTreeNode) getRoot();
    for (int i = rootNode.getChildCount() - 1; i >= 0; i--) {
      ((BasicObjectNode) getChild(rootNode, i)).refresh();
    }
  }

  public void add(IBasicObject object) {
    XTreeNode parentNode = (XTreeNode) getRoot();
    int index = parentNode.getChildCount();

    insertNodeInto(newObjectNode(object), parentNode, index);
  }
}
