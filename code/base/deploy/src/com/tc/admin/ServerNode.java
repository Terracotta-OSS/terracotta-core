/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.AbstractTreeCellRenderer;
import com.tc.admin.common.StatusView;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;

public class ServerNode extends ClusterElementNode {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected IServer             server;
  protected ServerListener      serverListener;
  protected ServerPanel         serverPanel;
  protected JPopupMenu          popupMenu;

  ServerNode(IAdminClientContext adminClientContext, IClusterModel clusterModel, IServer server) {
    super(server);

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.server = server;

    setRenderer(new ServerNodeTreeCellRenderer());
    initMenu();

    serverListener = createServerListener(server);
    serverListener.startListening();
  }

  public java.awt.Component getComponent() {
    if (serverPanel == null) {
      adminClientContext.block();
      serverPanel = createServerPanel();
      adminClientContext.unblock();
    }
    return serverPanel;
  }

  public synchronized IServer getServer() {
    return server;
  }

  protected ServerListener createServerListener(IServer theServer) {
    return new ServerListener(theServer);
  }

  protected class ServerListener extends AbstractServerListener {
    public ServerListener(IServer server) {
      super(server);
    }

    protected void handleConnectError() {
      if (adminClientContext != null) {
        nodeChanged();
      }
    }

    protected void handleConnected() {
      if (adminClientContext != null) {
        nodeChanged();
      }
    }
  }

  protected ServerPanel createServerPanel() {
    return new ServerPanel(adminClientContext, getServer());
  }

  protected void initMenu() {
    popupMenu = new JPopupMenu("Server Actions");
  }

  public Icon getIcon() {
    return ServersHelper.getHelper().getServerIcon();
  }

  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  public String toString() {
    return server != null ? server.toString() : "";
  }

  private class ServerNodeTreeCellRenderer extends AbstractTreeCellRenderer {
    protected StatusView statusView;

    public ServerNodeTreeCellRenderer() {
      super();

      statusView = new StatusView() {
        public void setForeground(Color fg) {
          super.setForeground(fg);
          if (label != null) {
            label.setForeground(fg);
          }
        }

        public void paint(Graphics g) {
          super.paint(g);
          if (hasFocus) {
            paintFocus(g, 0, 0, getWidth(), getHeight());
          }
        }
      };
    }

    @Override
    public JComponent getComponent() {
      return statusView;
    }

    @Override
    public void setValue(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean focused) {
      statusView.setIndicator(ServerHelper.getHelper().getServerStatusColor(server));
      statusView.setText(value.toString());
    }
  }

  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      server = null;
      serverPanel = null;
      popupMenu = null;
    }
  }
}
