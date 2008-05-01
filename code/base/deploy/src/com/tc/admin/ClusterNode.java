/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.auth.AuthScope;
import org.dijon.AbstractTreeCellRenderer;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeCellRenderer;
import com.tc.admin.dso.ClassesNode;
import com.tc.admin.dso.ClientNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.dso.DSOClient;
import com.tc.admin.dso.DSOHelper;
import com.tc.admin.dso.GCStatsNode;
import com.tc.admin.dso.RootsNode;
import com.tc.admin.dso.locks.LocksNode;
import com.tc.config.schema.L2Info;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.util.concurrent.ThreadUtil;

import java.awt.Color;
import java.awt.Component;
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

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.generic.ConnectionClosedException;
import javax.naming.CommunicationException;
import javax.naming.ServiceUnavailableException;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class ClusterNode extends ComponentNode implements ConnectionListener, NotificationListener {
  private AdminClientContext      m_acc;
  private String                  m_baseLabel;
  private String                  m_recordingStatsLabel;
  private ServerConnectionManager m_connectManager;
  private Exception               m_connectException;
  private L2Info[]                m_clusterMembers;
  private TCServerInfoMBean       m_serverInfoBean;
  private ProductInfo             m_productInfo;
  private ClusterPanel            m_clusterPanel;
  private ConnectDialog           m_connectDialog;
  private JDialog                 m_versionMismatchDialog;
  private JPopupMenu              m_popupMenu;
  private ConnectAction           m_connectAction;
  private DisconnectAction        m_disconnectAction;
  private boolean                 m_userDisconnecting;
  private DeleteAction            m_deleteAction;
  private AutoConnectAction       m_autoConnectAction;
  private JCheckBoxMenuItem       m_autoConnectMenuItem;

  private ActiveLocator           m_activeLocator;

  private RootsNode               m_rootsNode;
  private LocksNode               m_locksNode;
  private ServersNode             m_serversNode;
  private ClientsNode             m_clientsNode;
  private GCStatsNode             m_gcStatsNode;
  private ClusterThreadDumpsNode  m_threadDumpsNode;
  private StatsRecorderNode       m_statsRecorderNode;

  private static final String     CONNECT_ACTION      = "Connect";
  private static final String     DISCONNECT_ACTION   = "Disconnect";
  private static final String     DELETE_ACTION       = "Delete";
  private static final String     AUTO_CONNECT_ACTION = "AutoConnect";

  private static final String     HOST                = ServersHelper.HOST;
  private static final String     PORT                = ServersHelper.PORT;
  private static final String     AUTO_CONNECT        = ServersHelper.AUTO_CONNECT;

  ClusterNode() {
    this(ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT, ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  ClusterNode(final String host, final int jmxPort, final boolean autoConnect) {
    super();

    setLabel(m_baseLabel = "Terracotta cluster");
    m_recordingStatsLabel = m_baseLabel + " (recording stats)";
    
    m_acc = AdminClient.getContext();
    AutoConnectionListener acl = new AutoConnectionListener();
    m_connectManager = new ServerConnectionManager(host, jmxPort, autoConnect, acl);
    if (autoConnect) {
      String[] creds = ServerConnectionManager.getCachedCredentials(m_connectManager);
      if (creds != null) {
        m_connectManager.setCredentials(creds);
      }
    }
    initMenu(autoConnect);
    setComponent(m_clusterPanel = createClusterPanel());
    setRenderer(new ClusterNodeRenderer());
  }

  private class ClusterNodeRenderer extends XTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean focused) {
      Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
      if (haveActiveRecordingSession()) {
        m_label.setForeground(sel ? Color.white : Color.red);
        m_label.setText(m_recordingStatsLabel);
      }
      return comp;
    }
  }

  protected ClusterPanel createClusterPanel() {
    return new ClusterPanel(this);
  }

  void handleNewActive(ServerConnectionManager scm) {
    String[] creds = ServerConnectionManager.getCachedCredentials(scm);
    if (creds != null) {
      m_connectManager.setCredentials(creds);
    }

    boolean autoConnect = isAutoConnect();
    m_connectManager.setAutoConnect(false);
    m_connectManager.setL2Info(new L2Info(scm.getL2Info()));
    resetBeanProxies();

    try {
      m_connectManager.setConnected(m_connectManager.testIsConnected());
    } catch (Exception e) {
      m_acc.controller.nodeChanged(ClusterNode.this);
      m_connectManager.setAutoConnect(autoConnect);
      return;
    }

    m_clusterPanel.reinitialize();
    synchronized(this) {
      if (m_rootsNode != null) {
        m_rootsNode.newConnectionContext();
        m_locksNode.newConnectionContext();
        m_gcStatsNode.newConnectionContext();
        if(m_statsRecorderNode != null) {
          m_statsRecorderNode.newConnectionContext();
        }
        m_serversNode.newConnectionContext();
        m_clientsNode.newConnectionContext();
      }
    }
    
    m_acc.controller.nodeChanged(ClusterNode.this);
    m_connectManager.setAutoConnect(autoConnect);
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

  ServerConnectionManager getServerConnectionManager() {
    return m_connectManager;
  }

  String[] getConnectionCredentials() {
    return m_connectManager.getCredentials();
  }
  
  Map<String, Object> getConnectionEnvironment() {
    return m_connectManager.getConnectionEnvironment();
  }
  
  public ConnectionContext getConnectionContext() {
    return m_connectManager.getConnectionContext();
  }

  String getBaseLabel() {
    return m_baseLabel;
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

  Integer getDSOListenPort() throws Exception {
    return ServerHelper.getHelper().getDSOListenPort(getConnectionContext());
  }

  String getStatsExportServletURI() throws Exception {
    Integer dsoPort = getDSOListenPort();
    Object[] args = new Object[] { getHost(), dsoPort.toString() };
    return MessageFormat.format("http://{0}:{1}/statistics-gatherer/retrieveStatistics", args);
  }

  AuthScope getAuthScope() throws Exception {
    return new AuthScope(getHost(), getDSOListenPort());
  }

  private void initMenu(boolean autoConnect) {
    m_popupMenu = new JPopupMenu("Server Actions");

    m_connectAction = new ConnectAction();
    m_disconnectAction = new DisconnectAction();
    m_deleteAction = new DeleteAction();
    m_autoConnectAction = new AutoConnectAction();

    addActionBinding(CONNECT_ACTION, m_connectAction);
    addActionBinding(DISCONNECT_ACTION, m_disconnectAction);
    addActionBinding(DELETE_ACTION, m_deleteAction);
    addActionBinding(AUTO_CONNECT_ACTION, m_autoConnectAction);

    m_popupMenu.add(m_connectAction);
    m_popupMenu.add(m_disconnectAction);
    m_popupMenu.add(new JSeparator());
    m_popupMenu.add(m_deleteAction);
    m_popupMenu.add(new JSeparator());

    m_popupMenu.add(m_autoConnectMenuItem = new JCheckBoxMenuItem(m_autoConnectAction));
    m_autoConnectMenuItem.setSelected(autoConnect);
  }

  void setVersionMismatchDialog(JDialog dialog) {
    m_versionMismatchDialog = dialog;
  }

  private void setConnected(boolean connected) {
    if (m_acc == null) { return; }
    if (connected) {
      if (m_versionMismatchDialog != null) return;
      if (!m_clusterPanel.isProductInfoShowing() && !m_acc.controller.testServerMatch(this)) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (m_connectManager.isConnected()) {
              disconnect();
            }
          }
        });
        return;
      }
      m_acc.controller.block();
      m_connectException = null;
      if (m_connectManager.isConnected()) {
        if (m_connectManager.isActive()) {
          handleActivation();
        } else if (m_connectManager.isPassiveStandby()) {
          handlePassiveStandby();
        } else if (m_connectManager.isPassiveUninitialized()) {
          handlePassiveUninitialized();
        } else if (m_connectManager.isStarted()) {
          handleStarting();
        }
      } else {
        handleDisconnect();
      }
      m_acc.controller.unblock();
    } else {
      if (m_versionMismatchDialog != null) {
        m_versionMismatchDialog.setVisible(false);
      }
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

  boolean isPassiveUninitialized() {
    return m_connectManager != null && m_connectManager.isPassiveUninitialized();
  }

  boolean isPassiveStandby() {
    return m_connectManager != null && m_connectManager.isPassiveStandby();
  }

  boolean hasConnectionException() {
    return m_connectException != null;
  }

  ConnectDialog getConnectDialog(ConnectionListener listener) {
    if (m_connectDialog == null) {
      Frame frame = (Frame) m_clusterPanel.getAncestorOfClass(java.awt.Frame.class);
      m_connectDialog = new ConnectDialog(frame, m_connectManager, listener);
    } else {
      m_connectDialog.setServerConnectionManager(m_connectManager);
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

    ConnectDialog cd = getConnectDialog(this);
    Frame frame = (Frame) m_clusterPanel.getAncestorOfClass(java.awt.Frame.class);

    String[] creds = ServerConnectionManager.getCachedCredentials(getServerConnectionManager());
    if (creds != null) {
      m_connectManager.setCredentials(creds[0], creds[1]);
    }

    cd.center(frame);
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

  public static String getConnectionExceptionString(Exception e, ConnectionContext cntx) {
    AdminClientContext acc = AdminClient.getContext();
    String msg = null;
    Throwable cause = ExceptionHelper.getRootCause(e);
    
    if (cause instanceof ServiceUnavailableException) {
      String tmpl = acc.getMessage("service.unavailable");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { cntx };
      msg = form.format(args);
    } else if (cause instanceof ConnectException) {
      String tmpl = acc.getMessage("cannot.connect.to");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { cntx };
      msg = form.format(args);
    } else if (cause instanceof UnknownHostException || cause instanceof java.rmi.UnknownHostException) {
      String tmpl = acc.getMessage("unknown.host");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { cntx.host };
      msg = form.format(args);
    } else if (cause instanceof CommunicationException) {
      String tmpl = acc.getMessage("cannot.connect.to");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { cntx };
      msg = form.format(args);
    } else {
      msg = cause != null ? cause.getMessage() : e.getMessage();
    }

    return "<html>" + msg + "</html>";
  }

  private void reportConnectionException(Exception e) {
    String msg = getConnectionExceptionString(e, getConnectionContext());

    m_connectException = e;
    if (msg != null && m_clusterPanel != null) {
      boolean autoConnect = isAutoConnect();
      if(autoConnect && e instanceof SecurityException) {
        m_connectManager.setAutoConnect(autoConnect = false);
        m_autoConnectMenuItem.setSelected(false);
        m_acc.controller.updateServerPrefs();
      }
      if (!autoConnect) {
        m_clusterPanel.setupConnectButton();
      }
      m_clusterPanel.setStatusLabel(msg);
    }
    m_acc.controller.nodeChanged(ClusterNode.this);
  }

  /**
   * Called when the user clicks the Disconnect button. Used whether or not auto-connect is enabled.
   */
  private void disconnect() {
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
    AdminClientPanel topPanel = (AdminClientPanel) m_clusterPanel.getAncestorOfClass(AdminClientPanel.class);
    Frame frame = (Frame) topPanel.getAncestorOfClass(java.awt.Frame.class);
    String msg = "Are you sure you want to shutdown '" + this + "'?";

    int result = JOptionPane.showConfirmDialog(frame, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (result == JOptionPane.OK_OPTION) {
      doShutdown();
    }
  }

  void doShutdown() {
    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();
      serverInfo.shutdown();
    } catch (ConnectionClosedException ignore) {
      /* expected */
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

  private class ConnectAction extends XAbstractAction {
    ConnectAction() {
      super("Connect", ServersHelper.getHelper().getConnectIcon());

      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      connect();
    }
  }

  DisconnectAction getDisconnectAction() {
    return m_disconnectAction;
  }

  class DisconnectAction extends XAbstractAction {
    DisconnectAction() {
      super("Disconnect", ServersHelper.getHelper().getDisconnectIcon());

      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MENU_SHORTCUT_KEY_MASK, true));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      boolean recording = haveActiveRecordingSession();
      if (recording) {
        String msg = "There is an active statistic recording sessions.  Quit anyway?";
        Frame frame = (Frame) m_clusterPanel.getAncestorOfClass(Frame.class);
        int answer = JOptionPane.showConfirmDialog(m_clusterPanel, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
          return;
        }
      }
      m_userDisconnecting = true;
      disconnect();
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

          String name = ClusterNode.this.toString();
          MessageFormat form = new MessageFormat(m_acc.getMessage("deleted.server"));

          controller.setStatus(form.format(new Object[] { name }));

          // this must be last because as a side effect this node is tornDown (see tearDown)
          controller.remove(ClusterNode.this);

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

      if (autoConnect) {
        String[] creds = ServerConnectionManager.getCachedCredentials(getServerConnectionManager());
        if (creds != null) {
          m_connectManager.setCredentials(creds);
        }
      }

      m_connectAction.setEnabled(!autoConnect);
      m_connectManager.setAutoConnect(autoConnect);
      m_clusterPanel.setupConnectButton();
      m_acc.controller.updateServerPrefs();
    }
  }

  boolean isAutoConnect() {
    return m_connectManager.isAutoConnect();
  }

  private class ActiveLocator extends Thread {
    private ServerConnectionManager scm  = null;
    private boolean                 stop = false;

    void setStopped() {
      stop = true;
    }

    private ServerConnectionManager getConnectionManager(L2Info l2Info) {
      if (scm == null) {
        scm = new ServerConnectionManager(l2Info, false, null);
      } else {
        scm.setL2Info(l2Info);
      }
      scm.setCredentials(m_connectManager.getCredentials());
      return scm;
    }

    public void run() {
      L2Info[] l2Infos = getClusterMembers();
      if (l2Infos.length > 1) {
        while (true) {
          if (stop) return;
          for (L2Info l2Info : l2Infos) {
            if (l2Info.matches(m_connectManager.getL2Info())) {
              continue;
            }
            ThreadUtil.reallySleep(1000);
            if (stop) {
              if (scm != null) scm.tearDown();
              return;
            }
            scm = getConnectionManager(l2Info);
            try {
              if (scm.testIsConnected()) {
                if (scm.testIsActive()) {
                  m_activeLocator = null;
                  SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                      handleNewActive(scm);
                      scm.tearDown();
                    }
                  });
                  return;
                }
              }
            } catch (Exception e) {
              /**/
            }
          }
        }
      }
      m_activeLocator = null;
    }
  }

  private void cancelActiveLocator() {
    if (m_activeLocator != null) {
      m_activeLocator.setStopped();
      m_activeLocator = null;
    }
  }

  SynchronizedBoolean m_tryAddingChildren = new SynchronizedBoolean(false);

  void tryAddChildren() throws Exception {
    if (m_tryAddingChildren.get()) { return; }

    try {
      m_tryAddingChildren.set(true);
      if (getChildCount() == 0) {
        ConnectionContext cntx = getConnectionContext();

        if (DSOHelper.getHelper().getDSOMBean(cntx) != null) {
          addChildren(cntx);
          m_acc.controller.nodeStructureChanged(this);
          m_acc.controller.expand(this);
        } else {
          ObjectName mbsd = cntx.queryName("JMImplementation:type=MBeanServerDelegate");
          if (mbsd != null) {
            try {
              cntx.removeNotificationListener(mbsd, this);
            } catch (Exception e) {/**/
            }
            cntx.addNotificationListener(mbsd, this);
          }
        }
      }
      m_acc.controller.nodeChanged(ClusterNode.this);
    } catch (Exception e) {
      m_acc.log(e);
    } finally {
      m_tryAddingChildren.set(false);
    }
  }

  private void addChildren(ConnectionContext cc) throws Exception {
    add(m_rootsNode = createRootsNode());
    add(new ClassesNode(this));
    try {
      add(m_locksNode = createLocksNode());
    } catch (Throwable t) {
      // Need a more specific exception but this means we're trying to connect to an
      // older version of the server, that doesn't have the LockMonitorMBean we expect.
    }
    add(m_gcStatsNode = createGCStatsNode());
    add(m_threadDumpsNode = createThreadDumpsNode());
    add(m_statsRecorderNode = createStatsRecorderNode());
    add(m_serversNode = createServersNode());
    add(m_clientsNode = createClientsNode());
  }

  protected RootsNode createRootsNode() throws Exception {
    return new RootsNode(this);
  } 

  protected ClusterThreadDumpsNode createThreadDumpsNode() {
    return new ClusterThreadDumpsNode(this);
  }

  protected StatsRecorderNode createStatsRecorderNode() {
    return new StatsRecorderNode(this);
  }

  void makeStatsRecorderUnavailable() {
    if (m_statsRecorderNode != null) {
      m_acc.controller.remove(m_statsRecorderNode);
      m_statsRecorderNode.tearDown();
      m_statsRecorderNode = null;
    }
  }

  protected GCStatsNode createGCStatsNode() throws Exception {
    return new GCStatsNode(this);
  }

  protected LocksNode createLocksNode() throws Exception {
    return new LocksNode(this);
  }

  protected ServersNode createServersNode() {
    return new ServersNode(this);
  }

  protected ClientsNode createClientsNode() throws Exception {
    return new ClientsNode(this);
  }

  public void selectClientNode(String remoteAddr) {
    m_clientsNode.selectClientNode(remoteAddr);
  }

  private void testStartActiveLocator() {
    if(m_activeLocator == null) {
      m_activeLocator = new ActiveLocator();
      m_activeLocator.start();
    }
  }
  
  void handleStarting() {
    if (m_activeLocator != null) {
      // The ActiveLocator tries to not consider the bootstrap server but the L2Info
      // may be different from the host/port the user enters in the ClusterPanel, so
      // we may be extraneously informed that we're starting.
      return;
    }
    cancelActiveLocator();
    m_acc.controller.nodeChanged(ClusterNode.this);
    m_clusterPanel.started();
    if (!m_connectManager.testIsActive()) {
      m_activeLocator = new ActiveLocator();
      m_activeLocator.start();
    }
  }

  void handlePassiveUninitialized() {
    try {
      tryAddChildren();
      m_clusterPanel.passiveUninitialized();
      testStartActiveLocator();
    } catch (Exception e) {
      // just wait for disconnect message to come in
    }
  }

  void handlePassiveStandby() {
    try {
      tryAddChildren();
      m_clusterPanel.passiveStandby();
      testStartActiveLocator();      
    } catch (Exception e) {
      // just wait for disconnect message to come in
    }
  }

  void handleActivation() {
    if (m_activeLocator != null) {
      cancelActiveLocator();
    }
    try {
      tryAddChildren();
      m_clusterPanel.activated();
    } catch (Exception e) {
      e.printStackTrace();
      // just wait for disconnect message to come in
    }
  }

  TCServerInfoMBean getServerInfoBean() throws Exception {
    if (m_serverInfoBean != null) return m_serverInfoBean;
    m_serverInfoBean = ServerHelper.getHelper().getServerInfoBean(getConnectionContext());
    return m_serverInfoBean;
  }

  public ProductInfo getProductInfo() {
    if (m_productInfo != null) return m_productInfo;

    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();

      String version = serverInfo.getVersion();
      String buildID = serverInfo.getBuildID();
      String license = serverInfo.getDescriptionOfCapabilities();
      String copyright = serverInfo.getCopyright();

      m_productInfo = new ProductInfo(version, buildID, license, copyright);
    } catch (Exception e) {
      m_acc.log(e);
      m_productInfo = new ProductInfo();
    }

    return m_productInfo;
  }

  public String getProductVersion() {
    return getProductInfo().getVersion();
  }

  public String getProductBuildID() {
    return getProductInfo().getBuildID();
  }

  public String getProductLicense() {
    return getProductInfo().getLicense();
  }

  String getEnvironment() {
    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();
      return serverInfo.getEnvironment();
    } catch (Exception e) {
      m_acc.log(e);
      return e.getMessage();
    }
  }

  String getConfig() {
    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();
      return serverInfo.getConfig();
    } catch (Exception e) {
      m_acc.log(e);
      return e.getMessage();
    }
  }

  long getStartTime() {
    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();
      return serverInfo.getStartTime();
    } catch (Exception e) {
      m_acc.log(e);
      return 0L;
    }
  }

  long getActivateTime() {
    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();
      return serverInfo.getActivateTime();
    } catch (Exception e) {
      m_acc.log(e);
      return 0L;
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    if (notification instanceof MBeanServerNotification) {
      MBeanServerNotification mbsn = (MBeanServerNotification) notification;
      String type = notification.getType();
      ObjectName name = mbsn.getMBeanName();

      if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        if (name.getCanonicalName().equals(L2MBeanNames.DSO.getCanonicalName())) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              try {
                addChildren(getConnectionContext());
                m_acc.controller.nodeStructureChanged(ClusterNode.this);
                m_acc.controller.expand(ClusterNode.this);
                m_acc.controller.nodeChanged(ClusterNode.this);
              } catch (Exception e) {
                // just wait for disconnect message to come in
              }
            }
          });
        }
      }
    }
  }

  StatisticsLocalGathererMBean getStatisticsGathererMBean() {
    ConnectionContext cc = getConnectionContext();
    return (StatisticsLocalGathererMBean) MBeanServerInvocationProxy
        .newProxyInstance(cc.mbsc, StatisticsMBeanNames.STATISTICS_GATHERER, StatisticsLocalGathererMBean.class, true);
  }

  boolean haveActiveRecordingSession() {
    return m_statsRecorderNode != null && m_statsRecorderNode.isRecording();
  }

  L2Info[] getClusterMembers() {
    if (m_clusterMembers != null) return m_clusterMembers;

    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();
      m_clusterMembers = serverInfo.getL2Info();
    } catch (Exception e) {
      m_acc.log(e);
    } finally {
      if (m_clusterMembers == null) {
        m_clusterMembers = new L2Info[0];
      }
    }
    return m_clusterMembers;
  }

  class ActiveWaiter implements Runnable {
    public void run() {
      while (true) {
        int count = m_serversNode.getChildCount();
        if (count == 0) break;
        boolean anyStarted = false;
        for (int i = 0; i < count; i++) {
          ServerNode serverNode = (ServerNode) m_serversNode.getChildAt(i);
          final ServerConnectionManager scm = serverNode.getServerConnectionManager();
          if (!scm.equals(m_connectManager) && scm.isActive()) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                handleNewActive(scm);
              }
            });
            return;
          }
          if (scm.isStarted()) {
            anyStarted = true;
          }
        }
        if (!anyStarted) break;
        try {
          Thread.sleep(1000);
        } catch (Exception e) {/**/
        }
      }
      reallyHandleDisconnect();
    }
  }

  void waitForNewActive() {
    Thread waiter = new Thread(new ActiveWaiter());
    waiter.start();
  }

  boolean tryFindNewActive() {
    if (m_serversNode != null && m_serversNode.getChildCount() > 1) {
      int count = m_serversNode.getChildCount();
      for (int i = 0; i < count; i++) {
        ServerNode serverNode = (ServerNode) m_serversNode.getChildAt(i);
        ServerConnectionManager scm = serverNode.getServerConnectionManager();
        if (!scm.equals(m_connectManager)) {
          if (scm.isActive()) {
            handleNewActive(scm);
            return true;
          } else if (scm.isStarted()) {
            waitForNewActive();
            return true;
          }
        }
      }
    }
    return false;
  }

  private void resetBeanProxies() {
    m_serverInfoBean = null;
    m_productInfo = null;
  }

  void handleDisconnect() {
    resetBeanProxies();
    if (!m_userDisconnecting && tryFindNewActive()) { return; }
    reallyHandleDisconnect();
  }

  private synchronized void reallyHandleDisconnect() {
    m_clusterMembers = null;
    m_acc.controller.select(this);

    m_rootsNode = null;
    m_locksNode = null;
    m_serversNode = null;
    m_clientsNode = null;
    m_gcStatsNode = null;
    
    tearDownChildren();
    removeAllChildren();
    m_acc.controller.nodeStructureChanged(this);
    m_clusterPanel.disconnected();
    m_userDisconnecting = false;
  }

  Color getServerStatusColor() {
    return getServerStatusColor(getServerConnectionManager());
  }

  static Color getServerStatusColor(ServerConnectionManager scm) {
    if (scm != null) {
      if (scm.isActive()) {
        return Color.GREEN;
      } else if (scm.isPassiveStandby()) {
        return Color.CYAN;
      } else if (scm.isPassiveUninitialized()) {
        return Color.ORANGE;
      } else if (scm.isStarted()) {
        return Color.YELLOW;
      } else if (scm.getConnectionException() != null) { return Color.RED; }
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
      m_statusView.setIndicator(ClusterNode.this.getServerStatusColor());
      m_statusView.setLabel(value.toString());
    }
  }

  ClusterThreadDumpEntry takeThreadDump() {
    long requestMillis = System.currentTimeMillis();
    ClusterThreadDumpEntry tde = new ClusterThreadDumpEntry();
    int serverCount = m_serversNode.getChildCount();

    for (int i = 0; i < serverCount; i++) {
      ServerNode serverNode = (ServerNode) m_serversNode.getChildAt(i);
      ConnectionContext cc = serverNode.getConnectionContext();
      try {
        String td = ServerHelper.getHelper().takeThreadDump(cc, requestMillis);
        tde.add(serverNode.toString(), td);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    int clientCount = m_clientsNode.getChildCount();
    for (int i = 0; i < clientCount; i++) {
      ClientNode clientNode = (ClientNode) m_clientsNode.getChildAt(i);
      DSOClient client = clientNode.getClient();
      try {
        L1InfoMBean l1Info = client.getL1InfoBean();
        if (l1Info != null) {
          tde.add(client.getRemoteAddress(), l1Info.takeThreadDump(requestMillis));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (m_statsRecorderNode != null) {
      m_statsRecorderNode.testTriggerThreadDumpSRA();
    }

    return tde;
  }

  void notifyChanged() {
    nodeChanged();
  }

  public void tearDown() {
    if (m_connectDialog != null) {
      m_connectDialog.tearDown();
    }
    m_connectManager.tearDown();

    m_acc = null;
    m_connectManager = null;
    m_clusterPanel = null;
    m_connectDialog = null;
    m_popupMenu = null;
    m_connectAction = null;
    m_disconnectAction = null;
    m_deleteAction = null;
    m_autoConnectAction = null;

    m_rootsNode = null;
    m_locksNode = null;
    m_serversNode = null;
    m_clientsNode = null;
    m_gcStatsNode = null;
    m_threadDumpsNode = null;

    super.tearDown();
  }
}
