/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.IAdminClientContext;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;

public class ClientRootsNode extends RootsNode {
  protected IClient client;

  public ClientRootsNode(IAdminClientContext adminClientContext, IClusterModel clusterModel, IClient client) {
    super(adminClientContext, clusterModel);
    this.client = client;
  }

  String getBaseLabel() {
    return adminClientContext.getMessage("dso.client.roots");
  }

  protected RootsPanel createRootsPanel() {
    RootsPanel panel = new RootsPanel(adminClientContext, clusterModel, client, client, roots);
    panel.setExplainationText(adminClientContext.getMessage("resident.object.message"));
    return panel;
  }
}
