/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.AbstractTreeCellRenderer;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IClusterNode;
import com.tc.admin.model.IServer;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

/**
 * All connection actions go through the ServerConnectionManager, which calls back through ConnectionListener. The
 * ServerConnectionManager handles auto-connecting, active-connection monitoring, and connection-state messaging. A JMX
 * notification handler (handleNotification) informs when the server goes from started->active state. TODO: this class
 * and ClusterNode have much in common as ClusterNode was derived from ServerNode. The commonality should be reduced
 * through a refactoring.
 */

public class ServerNode extends ComponentNode {
  protected AdminClientContext           m_acc;
  protected ServersNode                  m_serversNode;
  protected IServer                      m_server;
  protected ServerPropertyChangeListener m_serverPropertyChangeListener;
  protected ServerPanel                  m_serverPanel;
  protected JDialog                      m_versionMismatchDialog;
  protected JPopupMenu                   m_popupMenu;
  protected ServerThreadDumpsNode        m_threadDumpsNode;
  protected ServerRuntimeStatsNode       m_runtimeStatsNode;

  ServerNode(ServersNode serversNode, IServer server) {
    super();

    m_acc = AdminClient.getContext();
    m_serversNode = serversNode;
    m_server = server;

    setRenderer(new ServerNodeTreeCellRenderer());
    initMenu();
    m_server.addPropertyChangeListener(m_serverPropertyChangeListener = new ServerPropertyChangeListener());
  }

  public java.awt.Component getComponent() {
    if (m_serverPanel == null) {
      m_serverPanel = createServerPanel();
      if (m_server.isConnected()) {
        handleConnected();
      }
    }
    return m_serverPanel;
  }

  public IServer getServer() {
    return m_server;
  }

  private class ServerPropertyChangeRunnable implements Runnable {
    PropertyChangeEvent m_pce;

    ServerPropertyChangeRunnable(PropertyChangeEvent pce) {
      m_pce = pce;
    }

    public void run() {
      if (m_server == null) return;
      String prop = m_pce.getPropertyName();
      if (IServer.PROP_CONNECTED.equals(prop)) {
        if (!m_server.isConnected()) {
          handleDisconnected();
        } else {
          handleConnected();
        }
      } else if (IClusterNode.PROP_READY.equals(prop)) {
        if (m_server.isReady()) {
          handleConnected();
        }
      } else if (IServer.PROP_CONNECT_ERROR.equals(prop)) {
        handleConnectError();
      }
    }
  }

  private class ServerPropertyChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      Runnable r = new ServerPropertyChangeRunnable(evt);
      if (SwingUtilities.isEventDispatchThread()) {
        r.run();
      } else {
        SwingUtilities.invokeLater(r);
      }
    }
  }

  void handleConnected() {
    if (m_acc == null) return;
    if (m_versionMismatchDialog != null) return;
    if (m_serverPanel != null && !m_serverPanel.isProductInfoShowing() && !m_acc.testServerMatch(this)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (m_server.isConnected()) {
            disconnect();
          }
        }
      });
      return;
    }
    if (isActive()) {
      handleActivation();
    } else if (isPassiveStandby()) {
      handlePassiveStandby();
    } else if (isPassiveUninitialized()) {
      handlePassiveUninitialized();
    } else if (isStarted()) {
      handleStarting();
    }
  }

  private void handleDisconnected() {
    if (m_versionMismatchDialog != null) {
      m_versionMismatchDialog.setVisible(false);
    }
    handleDisconnect();
  }

  private void handleConnectError() {
    reportConnectError();
  }

  protected ServerPanel createServerPanel() {
    return new ServerPanel(this);
  }

  public String getHost() {
    return m_server.getHost();
  }

  public String getCanonicalHostName() {
    return m_server.getCanonicalHostName();
  }

  public String getHostAddress() {
    return m_server.getHostAddress();
  }

  public int getPort() {
    return m_server.getPort();
  }

  public Integer getDSOListenPort() throws Exception {
    return m_server.getDSOListenPort();
  }

  String getStatsExportServletURI(String sessionId) throws Exception {
    return m_server.getStatsExportServletURI(sessionId);
  }

  protected void initMenu() {
    m_popupMenu = new JPopupMenu("Server Actions");
  }

  void setVersionMismatchDialog(JDialog dialog) {
    m_versionMismatchDialog = dialog;
  }

  boolean isConnected() {
    return m_server.isConnected();
  }

  boolean isStarted() {
    return m_server.isStarted();
  }

  boolean isActive() {
    return m_server.isActive();
  }

  boolean isPassiveUninitialized() {
    return m_server.isPassiveUninitialized();
  }

  boolean isPassiveStandby() {
    return m_server.isPassiveStandby();
  }

  boolean hasConnectError() {
    return m_server.hasConnectError();
  }

  private void reportConnectError() {
    reportConnectError(m_server.getConnectError());
  }

  private void reportConnectError(Exception e) {
    String msg = m_server.getConnectErrorMessage(e);
    if (msg != null && m_serverPanel != null) {
      m_serverPanel.setConnectExceptionMessage(msg);
    }
    m_acc.nodeChanged(this);
  }

  void disconnect() {
    m_acc.setStatus(m_acc.format("disconnecting.from", this));
    m_acc.updateServerPrefs();
    m_server.disconnect();
  }

  public Icon getIcon() {
    return ServersHelper.getHelper().getServerIcon();
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  public String toString() {
    return m_server != null ? m_server.toString() : "";
  }

  void handleStarting() {
    m_acc.nodeChanged(this);
    if (m_serverPanel != null) {
      m_serverPanel.started();
    }
  }

  private AtomicBoolean m_addingChildren = new AtomicBoolean(false);

  void tryAddChildren() {
    if (!m_server.isReady()) { return; }
    if (m_addingChildren.get()) { return; }

    try {
      m_addingChildren.set(true);
      if (getChildCount() == 0) {
        addChildren();
        m_acc.nodeStructureChanged(this);
      }
    } finally {
      m_addingChildren.set(false);
    }
  }

  private void addChildren() {
    add(m_runtimeStatsNode = createRuntimeStatsNode());
    add(m_threadDumpsNode = createThreadDumpsNode());
  }

  protected ServerRuntimeStatsNode createRuntimeStatsNode() {
    return new ServerRuntimeStatsNode(this);
  }

  protected ServerThreadDumpsNode createThreadDumpsNode() {
    return new ServerThreadDumpsNode(this);
  }

  void handlePassiveUninitialized() {
    tryAddChildren();
    if (m_serverPanel != null) {
      m_serverPanel.passiveUninitialized();
    }
    m_acc.nodeChanged(this);
  }

  void handlePassiveStandby() {
    tryAddChildren();
    if (m_serverPanel != null) {
      m_serverPanel.passiveStandby();
    }
    m_acc.nodeChanged(this);
  }

  void handleActivation() {
    tryAddChildren();
    if (m_serverPanel != null) {
      m_serverPanel.activated();
    }
    m_acc.nodeChanged(this);
  }

  void handleDisconnect() {
    m_acc.nodeChanged(this);
    if (m_serverPanel != null) {
      m_serverPanel.disconnected();
    }
    for (int i = getChildCount() - 1; i >= 0; i--) {
      m_acc.remove((XTreeNode) getChildAt(i));
    }
  }

  public String getProductVersion() throws Exception {
    return m_server.getProductVersion();
  }

  public String getProductBuildID() throws Exception {
    return m_server.getProductBuildID();
  }

  public String getProductLicense() throws Exception {
    return m_server.getProductLicense();
  }

  public String getProductCopyright() throws Exception {
    return m_server.getProductCopyright();
  }

  String getEnvironment() throws Exception {
    return m_server.getEnvironment();
  }

  String getConfig() throws Exception {
    return m_server.getConfig();
  }

  long getStartTime() throws Exception {
    return m_server.getStartTime();
  }

  long getActivateTime() throws Exception {
    return m_server.getActivateTime();
  }

  Color getServerStatusColor() {
    return getServerStatusColor(m_server);
  }

  static Color getServerStatusColor(IServer server) {
    if (server != null) {
      if (server.isActive()) {
        return Color.GREEN;
      } else if (server.isPassiveStandby()) {
        return Color.CYAN;
      } else if (server.isPassiveUninitialized()) {
        return Color.ORANGE;
      } else if (server.isStarted()) {
        return Color.YELLOW;
      } else if (server.hasConnectError()) { return Color.RED; }
    }
    return Color.LIGHT_GRAY;
  }

  private class ServerNodeTreeCellRenderer extends AbstractTreeCellRenderer {
    protected StatusView m_statusView;

    public ServerNodeTreeCellRenderer() {
      super();

      m_statusView = new StatusView() {
        public void setForeground(Color fg) {
          super.setForeground(fg);
          if (m_label != null) {
            m_label.setForeground(fg);
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

    public JComponent getComponent() {
      return m_statusView;
    }

    public void setValue(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean focused) {
      m_statusView.setIndicator(ServerNode.this.getServerStatusColor());
      m_statusView.setLabel(value.toString());
    }
  }

  public void tearDown() {
    m_server.removePropertyChangeListener(m_serverPropertyChangeListener);
    
    super.tearDown();

    m_acc = null;
    m_server = null;
    m_serverPanel = null;
    m_popupMenu = null;
    m_threadDumpsNode = null;
  }
}
