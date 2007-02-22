/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.AbstractTreeCellRenderer;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.DSOHelper;
import com.tc.admin.dso.DSONode;
import com.tc.config.schema.L2Info;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.naming.CommunicationException;
import javax.naming.ServiceUnavailableException;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * All connection actions go through the ServerConnectionManager, which calls back through ConnectionListener. The
 * ServerConnectionManager handles auto-connecting, active-connection monitoring, and connection-state messaging. A JMX
 * notification handler (handleNotification) informs when the server goes from started->active state.
 */

public class ServerNode extends ComponentNode implements ConnectionListener {
  private AdminClientContext      m_acc;
  private ServerConnectionManager m_connectManager;
  private Exception               m_connectException;
  private ServerPanel             m_serverPanel;
  private ConnectDialog           m_connectDialog;
  private JPopupMenu              m_popupMenu;
  private ConnectAction           m_connectAction;
  private DisconnectAction        m_disconnectAction;
  private DeleteAction            m_deleteAction;
  private AutoConnectAction       m_autoConnectAction;
  private JCheckBoxMenuItem       m_autoConnectMenuItem;
  private ShutdownAction          m_shutdownAction;

  private static final String     CONNECT_ACTION                 = "Connect";
  private static final String     DISCONNECT_ACTION              = "Disconnect";
  private static final String     DELETE_ACTION                  = "Delete";
  private static final String     AUTO_CONNECT_ACTION            = "AutoConnect";

  private static final String     HOST                           = ServersHelper.HOST;
  private static final String     PORT                           = ServersHelper.PORT;
  private static final String     AUTO_CONNECT                   = ServersHelper.AUTO_CONNECT;

  private static final long       DEFAULT_CONNECT_TIMEOUT_MILLIS = 8000;

  private static final long       CONNECT_TIMEOUT_MILLIS         = Long
                                                                     .getLong(
                                                                              "com.tc.admin.ServerNode.connect-timeout",
                                                                              DEFAULT_CONNECT_TIMEOUT_MILLIS)
                                                                     .longValue();

  ServerNode() {
    this(ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT, ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  ServerNode(final String host, final int port, final boolean autoConnect) {
    super();

    m_acc = AdminClient.getContext();
    setRenderer(new ServerNodeTreeCellRenderer());
    AutoConnectionListener acl = new AutoConnectionListener();
    m_connectManager = new ServerConnectionManager(host, port, autoConnect, acl);
    initMenu(autoConnect);
    setComponent(m_serverPanel = new ServerPanel(this));
  }

  /**
   * We need to use invokeLater here because this is being called from a background thread and all Swing stuff has to be
   * done in the primary event loop.
   */
  private class AutoConnectionListener implements ConnectionListener {
    public void handleConnection() {
      if (m_connectManager != null) {
        final boolean isConnected = m_connectManager.isConnected();

        if (SwingUtilities.isEventDispatchThread()) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              if (m_connectManager != null) {
                setConnected(isConnected);
              }
            }
          });
        } else {
          try {
            SwingUtilities.invokeAndWait(new Runnable() {
              public void run() {
                if (m_connectManager != null) {
                  setConnected(isConnected);
                }
              }
            });
          } catch (InterruptedException ex) {/**/
          } catch (InvocationTargetException ite) {
            m_acc.log(ite);
          }
        }
      }
    }

    public void handleException() {
      if (m_connectManager != null) {
        final Exception e = m_connectManager.getConnectionException();

        if (SwingUtilities.isEventDispatchThread()) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              if (m_connectManager != null) {
                reportConnectionException(e);
              }
            }
          });
        } else {
          try {
            SwingUtilities.invokeAndWait(new Runnable() {
              public void run() {
                if (m_connectManager != null) {
                  reportConnectionException(e);
                }
              }
            });
          } catch (InterruptedException ex) {/**/
          } catch (InvocationTargetException ite) {
            m_acc.log(ite);
          }
        }
      }
    }
  }

  ConnectionContext getConnectionContext() {
    return m_connectManager.getConnectionContext();
  }

  void setHost(String host) {
    m_connectManager.setHostname(host);
  }

  String getHost() {
    return m_connectManager.getHostname();
  }

  void setPort(int port) {
    m_connectManager.setJMXPortNumber(port);
  }

  int getPort() {
    return m_connectManager.getJMXPortNumber();
  }

  private void initMenu(boolean autoConnect) {
    m_popupMenu = new JPopupMenu("Server Actions");

    m_connectAction = new ConnectAction();
    m_disconnectAction = new DisconnectAction();
    m_shutdownAction = new ShutdownAction();
    m_deleteAction = new DeleteAction();
    m_autoConnectAction = new AutoConnectAction();

    addActionBinding(CONNECT_ACTION, m_connectAction);
    addActionBinding(DISCONNECT_ACTION, m_disconnectAction);
    addActionBinding(DELETE_ACTION, m_deleteAction);
    addActionBinding(AUTO_CONNECT_ACTION, m_autoConnectAction);

    m_connectManager.addToggleAutoConnectListener(new ServerConnectionManager.AutoConnectListener() {
      public void handleEvent() {
        m_autoConnectMenuItem.setSelected(false);
        Thread reActivator = new Thread() {
          public void run() {
            boolean ready = false;
            try {
              while (!ready) {
                Thread.sleep(500);
                if (m_serverPanel != null && m_serverPanel.getConnectButton() != null && m_acc.controller != null) {
                  ready = true;
                }
              }
            } catch (InterruptedException e) {
              try {
                Thread.sleep(2000);
              } catch (InterruptedException ie) {
                ie.printStackTrace();
                System.exit(0);
                // let's hope it never comes to this
              }
            }
            m_serverPanel.getConnectButton().setEnabled(true);
            m_acc.controller.updateServerPrefs();
          }
        };
        reActivator.start();
      }
    });

    m_popupMenu.add(m_connectAction);
    m_popupMenu.add(m_disconnectAction);
    m_popupMenu.add(new JSeparator());
    // m_popupMenu.add(m_shutdownAction);
    m_popupMenu.add(m_deleteAction);
    m_popupMenu.add(new JSeparator());

    m_popupMenu.add(m_autoConnectMenuItem = new JCheckBoxMenuItem(m_autoConnectAction));
    m_autoConnectMenuItem.setSelected(autoConnect);
  }

  private void setConnected(boolean connected) {
    if (m_acc == null) { return; }
    if (connected) {
      m_acc.controller.block();

      m_connectException = null;
      if (m_connectManager.isActive()) {
        handleActivation();
      } else if (m_connectManager.isStarted()) {
        handleStarting();
      }

      m_acc.controller.unblock();
    } else {
      handleDisconnect();
    }

    m_connectAction.setEnabled(!connected);
    m_disconnectAction.setEnabled(connected);
  }

  boolean isConnected() {
    return m_connectManager != null && m_connectManager.isConnected();
  }

  boolean isStarted() {
    return m_connectManager != null && m_connectManager.isStarted();
  }

  boolean isActive() {
    return m_connectManager != null && m_connectManager.isActive();
  }

  boolean hasConnectionException() {
    return m_connectException != null;
  }

  private ConnectDialog getConnectDialog(JMXServiceURL url, Map env, long timeout, ConnectionListener listener) {
    if (m_connectDialog == null) {
      m_connectDialog = new ConnectDialog((Frame) m_serverPanel.getAncestorOfClass(java.awt.Frame.class), url, env,
                                          timeout, listener);
    } else {
      m_connectDialog.setServiceURL(url);
      m_connectDialog.setEnvironment(env);
      m_connectDialog.setTimeout(timeout);
      m_connectDialog.setConnectionListener(listener);
    }

    return m_connectDialog;
  }

  /**
   * Called when the user clicks the Connect button. Not used when auto-connect is enabled.
   */
  void connect() {
    try {
      beginConnect();
    } catch (Exception e) {
      m_acc.controller.log(e);
    }
  }

  private void beginConnect() throws Exception {
    m_acc.controller.block();

    m_connectException = null;

    JMXServiceURL url = m_connectManager.getJMXServiceURL();
    Map env = m_connectManager.getConnectionEnvironment();
    ConnectDialog cd = getConnectDialog(url, env, CONNECT_TIMEOUT_MILLIS, this);

    AdminClientPanel topPanel = (AdminClientPanel) SwingUtilities.getAncestorOfClass(AdminClientPanel.class,
                                                                                     m_serverPanel);

    cd.center(topPanel);
    cd.setVisible(true);
  }

  /**
   * Callback from the ConnectDialog.
   */
  public void handleConnection() {
    JMXConnector jmxc;
    if ((jmxc = m_connectDialog.getConnector()) != null) {
      try {
        m_connectManager.setJMXConnector(jmxc);
      } catch (IOException ioe) {
        reportConnectionException(ioe);
      }
    }

    m_acc.controller.unblock();
  }

  /**
   * Callback from the ConnectDialog.
   */
  public void handleException() {
    Exception e = m_connectDialog.getError();

    if (e != null) {
      reportConnectionException(e);
    }

    m_acc.controller.unblock();
  }

  public static String getConnectionExceptionString(Exception e, Object connectionObject) {
    AdminClientContext acc = AdminClient.getContext();
    String msg = null;

    if (e instanceof ServiceUnavailableException || e.getCause() instanceof ServiceUnavailableException) {
      String tmpl = acc.getMessage("service.unavailable");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { connectionObject };

      msg = form.format(args);
    } else if (e.getCause() instanceof ConnectException) {
      String tmpl = acc.getMessage("cannot.connect.to");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { connectionObject };

      msg = form.format(args);
    } else if (e.getCause() instanceof UnknownHostException
               || (e.getCause() != null && e.getCause().getCause() instanceof java.rmi.UnknownHostException)) {
      String tmpl = acc.getMessage("unknown.host");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { connectionObject };

      msg = form.format(args);
    } else if (e.getCause() instanceof CommunicationException) {
      String tmpl = acc.getMessage("cannot.connect.to");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { connectionObject };

      msg = form.format(args);

    } else {
      msg = e.getMessage();
    }

    return "<html>" + msg + "</html>";
  }

  private void reportConnectionException(Exception e) {
    String msg = getConnectionExceptionString(e, this);

    m_connectException = e;
    if (msg != null && m_serverPanel != null) {
      m_serverPanel.setStatusLabel(msg);
    }
    m_acc.controller.nodeChanged(ServerNode.this);
  }

  /**
   * Called when the user clicks the Disconnect button. Used whether or not auto-connect is enabled.
   */
  void disconnect() {
    String msg = m_acc.getMessage("disconnecting.from");
    MessageFormat form = new MessageFormat(msg);
    Object[] args = new Object[] { this };

    m_acc.controller.setStatus(form.format(args));
    m_connectManager.setAutoConnect(false);
    m_acc.controller.updateServerPrefs();
    m_autoConnectMenuItem.setSelected(false);
    m_connectManager.setConnected(false);
  }

  void disconnectOnExit() {
    String msg = m_acc.getMessage("disconnecting.from");
    MessageFormat form = new MessageFormat(msg);
    Object[] args = new Object[] { this };

    m_acc.controller.setStatus(form.format(args));
    m_connectManager.disconnectOnExit();
  }

  void shutdown() {
    try {
      ConnectionContext cntx = getConnectionContext();
      ObjectName serverInfo = getServerInfo(cntx);

      cntx.invoke(serverInfo, "stop", new Object[] {}, new String[] {});
    } catch (Exception e) {
      m_acc.log(e);
    }
  }

  public Icon getIcon() {
    return ServersHelper.getHelper().getServerIcon();
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  public void setPreferences(Preferences prefs) {
    prefs.put(HOST, getHost());
    prefs.putInt(PORT, getPort());
    prefs.putBoolean(AUTO_CONNECT, isAutoConnect());
  }

  public String toString() {
    return m_connectManager.toString();
  }

  private class ConnectAction extends XAbstractAction {
    ConnectAction() {
      super("Connect", ServersHelper.getHelper().getConnectIcon());

      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      connect();
    }
  }

  private class DisconnectAction extends XAbstractAction {
    DisconnectAction() {
      super("Disconnect", ServersHelper.getHelper().getDisconnectIcon());

      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MENU_SHORTCUT_KEY_MASK, true));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      disconnect();
    }
  }

  private class ShutdownAction extends XAbstractAction {
    ShutdownAction() {
      super("Shutdown", ServersHelper.getHelper().getShutdownIcon());

      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_SHORTCUT_KEY_MASK, true));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      shutdown();
    }
  }

  private class DeleteAction extends XAbstractAction {
    DeleteAction() {
      super("Delete", ServersHelper.getHelper().getDeleteIcon());

      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      if (isConnected()) {
        disconnectOnExit();
      }

      // Need to allow possible disconnect message from ServerConnectionManager to
      // come through.

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          AdminClientController controller = m_acc.controller;

          String name = ServerNode.this.toString();
          MessageFormat form = new MessageFormat(m_acc.getMessage("deleted.server"));

          controller.setStatus(form.format(new Object[] { name }));

          // this must be last because as a side effect this node is tornDown (see tearDown)
          controller.remove(ServerNode.this);

          // Update the prefs after removing this node or it'll keep coming back.
          // We saved off the controller above because it gets nulled out in tearDown.
          controller.updateServerPrefs();
        }
      });
    }
  }

  private class AutoConnectAction extends XAbstractAction {
    AutoConnectAction() {
      super("Auto-connect");
      setShortDescription("Attempt to connect automatically");
    }

    public void actionPerformed(ActionEvent ae) {
      JCheckBoxMenuItem menuitem = (JCheckBoxMenuItem) ae.getSource();
      boolean autoConnect = menuitem.isSelected();

      m_connectManager.setAutoConnect(autoConnect);
      m_serverPanel.setupConnectButton();
      m_acc.controller.updateServerPrefs();
    }
  }

  boolean isAutoConnect() {
    return m_connectManager.isAutoConnect();
  }

  void handleStarting() {
    m_acc.controller.nodeChanged(ServerNode.this);
    m_serverPanel.started();
    m_shutdownAction.setEnabled(false);
  }

  static ObjectName getServerInfo(ConnectionContext cntx) throws Exception {
    return ServerHelper.getHelper().getServerInfoMBean(cntx);
  }

  ProductInfo getProductInfo() {
    ProductInfo info;

    try {
      info = getProductInfo(getConnectionContext());
    } catch (Exception e) {
      m_acc.log(e);
      info = new ProductInfo();
    }

    return info;
  }

  public static ProductInfo getProductInfo(ConnectionContext cntx) throws Exception {
    ObjectName serverInfo = getServerInfo(cntx);

    String version = cntx.getStringAttribute(serverInfo, "Version");
    String buildID = cntx.getStringAttribute(serverInfo, "BuildID");
    String license = cntx.getStringAttribute(serverInfo, "DescriptionOfLicense");
    String copyright = cntx.getStringAttribute(serverInfo, "Copyright");

    return new ProductInfo(version, buildID, license, copyright);
  }

  long getStartTime() {
    try {
      ConnectionContext cntx = getConnectionContext();
      ObjectName serverInfo = getServerInfo(cntx);

      return cntx.getLongAttribute(serverInfo, "StartTime");
    } catch (Exception e) {
      m_acc.log(e);
      return 0L;
    }
  }

  long getActivateTime() {
    try {
      ConnectionContext cntx = getConnectionContext();
      ObjectName serverInfo = getServerInfo(cntx);

      return cntx.getLongAttribute(serverInfo, "ActivateTime");
    } catch (Exception e) {
      m_acc.log(e);
      return 0L;
    }
  }

  void handleActivation() {
    ConnectionContext cntx = getConnectionContext();
    DSONode dsoNode = null;

    if (DSOHelper.getHelper().getDSOMBean(cntx) != null) {
      add(dsoNode = new DSONode(cntx));
    }

    /*
     * ObjectName[] beanNames = SessionsHelper.getHelper().getSessionsProductMBeans(cntx); if(beanNames != null &&
     * beanNames.length > 0) { add(new SessionsNode(cntx, beanNames)); }
     */

    m_acc.controller.nodeStructureChanged(this);

    if (dsoNode != null) {
      m_acc.controller.expand(dsoNode);
    }

    m_serverPanel.activated();
    m_shutdownAction.setEnabled(true);
  }

  L2Info[] getClusterMembers() {
    ConnectionContext cc = getConnectionContext();
    L2Info[] result = null;

    try {
      result = (L2Info[]) cc.getAttribute(getServerInfo(cc), "L2Info");
    } catch (Exception e) {
      m_acc.log(e);
    }

    return result != null ? result : new L2Info[0];
  }

  void handleDisconnect() {
    for (int i = getChildCount() - 1; i >= 0; i--) {
      ((XTreeNode) getChildAt(i)).tearDown();
      remove(i);
    }

    m_serverPanel.disconnected();
    m_acc.controller.nodeStructureChanged(ServerNode.this);
    m_acc.controller.select(this);
    m_shutdownAction.setEnabled(false);
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
      Color bg = Color.LIGHT_GRAY;

      if (isActive()) {
        bg = Color.GREEN;
      } else if (isStarted()) {
        bg = Color.YELLOW;
      } else if (hasConnectionException()) {
        bg = Color.RED;
      }

      m_statusView.setIndicator(bg);
      m_statusView.setLabel(value.toString());
    }
  }

  public void tearDown() {
    if (m_connectDialog != null) {
      m_connectDialog.tearDown();
    }
    m_connectManager.tearDown();

    m_acc = null;
    m_connectManager = null;
    m_serverPanel = null;
    m_connectDialog = null;
    m_popupMenu = null;
    m_connectAction = null;
    m_disconnectAction = null;
    m_shutdownAction = null;
    m_deleteAction = null;
    m_autoConnectAction = null;

    super.tearDown();
  }
}
