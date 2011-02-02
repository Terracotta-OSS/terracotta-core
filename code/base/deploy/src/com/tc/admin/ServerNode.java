/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.AbstractTreeCellRenderer;
import com.tc.admin.common.ApplicationContext;
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
  protected ApplicationContext appContext;
  protected IClusterModel      clusterModel;
  protected IServer            server;
  protected ServerListener     serverListener;
  protected ServerPanel        serverPanel;
  protected JPopupMenu         popupMenu;

  ServerNode(ApplicationContext appContext, IClusterModel clusterModel, IServer server) {
    super(server);

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.server = server;

    setRenderer(new ServerNodeTreeCellRenderer());
    initMenu();

    serverListener = createServerListener(server);
    serverListener.startListening();
  }

  @Override
  public java.awt.Component getComponent() {
    if (serverPanel == null) {
      appContext.block();
      serverPanel = createServerPanel();
      appContext.unblock();
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

    @Override
    protected void handleConnectError() {
      if (appContext != null) {
        nodeChanged();
      }
    }

    @Override
    protected void handleConnected() {
      if (appContext != null) {
        nodeChanged();
      }
    }
  }

  protected ServerPanel createServerPanel() {
    return new ServerPanel(appContext, getServer());
  }

  protected void initMenu() {
    popupMenu = new JPopupMenu("Server Actions");
  }

  @Override
  public Icon getIcon() {
    return ServersHelper.getHelper().getServerIcon();
  }

  @Override
  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  @Override
  public String toString() {
    return server != null ? server.getName() : "";
  }

  private class ServerNodeTreeCellRenderer extends AbstractTreeCellRenderer {
    protected StatusView statusView;

    public ServerNodeTreeCellRenderer() {
      super();

      statusView = new StatusView() {
        @Override
        public void setForeground(Color fg) {
          super.setForeground(fg);
          if (label != null) {
            label.setForeground(fg);
          }
        }

        @Override
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
      ServerHelper.getHelper().setStatusView(server, statusView);
      statusView.setText(value.toString());
    }
  }

  @Override
  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      server = null;
      if (serverPanel != null) {
        serverPanel.tearDown();
        serverPanel = null;
      }
      popupMenu = null;
    }
  }
}
