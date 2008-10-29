/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Label;

import com.tc.admin.AdminClient;

public class ClientRootsNode extends RootsNode {
  protected ClientNode m_clientNode;

  public ClientRootsNode(ClientNode clientNode) {
    super(clientNode.getClientsNode().getClusterNode());
    m_clientNode = clientNode;
  }

  String getBaseLabel() {
    return m_acc.getMessage("dso.client.roots");
  }
  
  protected RootsPanel createRootsPanel() {
    RootsPanel panel = new RootsPanel(m_clientNode.getClientsNode().getClusterModel(), m_clientNode.getClient(),
                                      m_roots);
    Label explainationLabel = (Label) panel.findComponent("ExplainationLabel");
    explainationLabel.setText(AdminClient.getContext().getMessage("resident.object.message"));
    return panel;
  }
}
