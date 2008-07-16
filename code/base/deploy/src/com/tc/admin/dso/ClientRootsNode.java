/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

public class ClientRootsNode extends RootsNode {
  protected ClientNode m_clientNode;

  public ClientRootsNode(ClientNode clientNode) {
    super(clientNode.getClientsNode().getClusterNode());
    setLabel(m_acc.getMessage("dso.client.roots"));
    m_clientNode = clientNode;
  }

  protected RootsPanel createRootsPanel() {
    return new RootsPanel(m_clientNode.getClient(), m_roots);
  }
}
