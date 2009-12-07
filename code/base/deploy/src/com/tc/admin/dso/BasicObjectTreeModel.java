/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IObject;
import com.tc.object.ObjectID;
import com.tc.util.Assert;

public class BasicObjectTreeModel extends XTreeModel {
  private final IAdminClientContext adminClientContext;
  private final IClient             client;

  public BasicObjectTreeModel(IAdminClientContext adminClientContext, IBasicObject[] roots) {
    this(adminClientContext, null, roots);
  }

  public BasicObjectTreeModel(IAdminClientContext adminClientContext, IClient client, IBasicObject[] roots) {
    super();

    this.adminClientContext = adminClientContext;
    this.client = client;

    XTreeNode rootNode = (XTreeNode) getRoot();
    if (roots != null && roots.length > 0) {
      for (IBasicObject object : roots) {
        rootNode.add(newObjectNode(object));
      }
    }
  }

  private static boolean isResidentOnClient(IClient client, IObject object) {
    Assert.assertNotNull(client);
    while (object != null) {
      ObjectID oid = object.getObjectID();
      if (oid != null) { return client.isResident(oid); }
      object = object.getParent();
    }
    return false;
  }

  public static BasicObjectNode newObjectNode(IAdminClientContext adminClientContext, IClient client,
                                              IBasicObject object) {
    object = object.newCopy();
    BasicObjectNode objectNode = new BasicObjectNode(adminClientContext, object);
    objectNode.setResident(client != null ? isResidentOnClient(client, object) : true);
    return objectNode;
  }

  public BasicObjectNode newObjectNode(IBasicObject object) {
    return newObjectNode(adminClientContext, client, object);
  }

  public void refresh() {
    XTreeNode rootNode = (XTreeNode) getRoot();
    for (int i = rootNode.getChildCount() - 1; i >= 0; i--) {
      ((BasicObjectNode) getChild(rootNode, i)).refresh();
    }
  }

  public BasicObjectNode add(IBasicObject object) {
    XTreeNode parentNode = (XTreeNode) getRoot();
    int index = parentNode.getChildCount();
    BasicObjectNode newNode = newObjectNode(object);
    insertNodeInto(newNode, parentNode, index);
    if (parentNode.getChildCount() == 1) {
      reload(); // Huh? Display doesn't update when first child is added? Force it to.
    }
    return newNode;
  }
}
