/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

public class ClientTreeModel extends XTreeModel {
  private DSOClient[] clients;

  public ClientTreeModel(ConnectionContext cc) {
    super();

    try {
      clients = ClientsHelper.getHelper().getClients(cc);
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
    }

    XTreeNode rootNode = (XTreeNode)getRoot();

    for(int i = 0; i < clients.length; i++) {
      insertNodeInto(new ClientTreeNode(cc, clients[i]), rootNode, i);
    }
  }
}
