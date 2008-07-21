/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.Component;
import java.util.concurrent.Callable;

public class ServersNode extends ComponentNode {
  protected AdminClientContext m_acc;
  protected ClusterNode        m_clusterNode;
  protected ServersPanel       m_serversPanel;

  public ServersNode(ClusterNode clusterNode) {
    super();
    m_acc = AdminClient.getContext();
    m_clusterNode = clusterNode;
    init();
  }

  IClusterModel getClusterModel() {
    return m_clusterNode != null ? m_clusterNode.getClusterModel() : null;
  }

  private void init() {
    setLabel(m_acc.getMessage("servers"));
    for (int i = getChildCount() - 1; i >= 0; i--) {
      m_acc.remove((XTreeNode) getChildAt(i));
    }
    m_acc.execute(new InitWorker());
  }

  private class InitWorker extends BasicWorker<IServer[]> {
    private InitWorker() {
      super(new Callable<IServer[]>() {
        public IServer[] call() throws Exception {
          return m_clusterNode.getClusterServers();
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        IServer[] clusterServers = getResult();
        for (IServer server : clusterServers) {
          ServerNode serverNode = m_acc.getNodeFactory().createServerNode(ServersNode.this, server);
          add(serverNode);
          serverNode.handleConnected();
        }
        setLabel(m_acc.getMessage("servers") + " (" + getChildCount() + ")");
        m_acc.nodeChanged(ServersNode.this);
      }
    }
  }

  protected ServersPanel createServersPanel() {
    return new ServersPanel(ServersNode.this);
  }

  public Component getComponent() {
    if (m_serversPanel == null) {
      m_serversPanel = createServersPanel();
    }
    return m_serversPanel;
  }

  public ConnectionContext getConnectionContext() {
    return m_clusterNode.getConnectionContext();
  }

  void selectClientNode(String remoteAddr) {
    m_clusterNode.selectClientNode(remoteAddr);
  }

  /**
   * Return any credentials that were used when the initial cluster server was connected.
   */
  String[] getParentCredentials() {
    return m_clusterNode.getConnectionCredentials();
  }

  public void tearDown() {
    super.tearDown();
    m_acc = null;
    m_clusterNode = null;
  }
}
