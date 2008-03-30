/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.Component;

import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XTreeNode;
import com.tc.config.schema.L2Info;

import java.util.concurrent.Callable;

public class ServersNode extends ComponentNode {
  protected AdminClientContext m_acc;
  protected ClusterNode m_clusterNode;
  protected ServersPanel m_serversPanel;

  public ServersNode(ClusterNode clusterNode) {
    super();
    m_acc = AdminClient.getContext();
    m_clusterNode = clusterNode;
    init();
  }

  private void init() {
    setLabel(m_acc.getMessage("servers"));
    for (int i = getChildCount() - 1; i >= 0; i--) {
      m_acc.controller.remove((XTreeNode) getChildAt(i));
    }
    m_acc.executorService.execute(new InitWorker());
  }

  private class InitWorker extends BasicWorker<L2Info[]> {
    private InitWorker() {
      super(new Callable<L2Info[]>() {
        public L2Info[] call() throws Exception {
          return m_clusterNode.getClusterMembers();
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        L2Info[] clusterMembers = getResult();
        for (L2Info member : clusterMembers) {
          add(m_acc.nodeFactory.createServerNode(ServersNode.this, member));
        }
        setLabel(m_acc.getMessage("servers") + " (" + getChildCount() + ")");
        m_acc.controller.nodeChanged(ServersNode.this);
      }
    }
  }
  
  protected ServersPanel createServersPanel() {
    return new ServersPanel(ServersNode.this);
  }
  
  public Component getComponent() {
    if(m_serversPanel== null) {
      m_serversPanel = createServersPanel();
    }
    return m_serversPanel;
  }

  public ConnectionContext getConnectionContext() {
    return m_clusterNode.getConnectionContext();
  }
  
  void newConnectionContext() {
    /**
     * what if the new active has a different config with a different set of cluster members? if so, we need to
     * reconstruct the serversPanel and child nodes. This would be bad.
     */
  }

  void serverStateChanged(ServerNode serverNode) {
    if(m_serversPanel != null) {
      m_serversPanel.serverStateChanged(serverNode);
    }
  }

  void selectClientNode(String remoteAddr) {
    m_clusterNode.selectClientNode(remoteAddr);
  }
  
  /**
   * Return any credentials that were used when the initial cluster server was connected.
   */
  String[] getParentCredentials() {
    return m_clusterNode.getServerConnectionManager().getCredentials();
  }

  public void tearDown() {
    int serverCount = getChildCount();
    for(int i = 0; i < serverCount; i++) {
      ((ServerNode)getChildAt(i)).handleDisconnect();
    }
    super.tearDown();
    m_acc = null;
    m_clusterNode = null;
  }
}
