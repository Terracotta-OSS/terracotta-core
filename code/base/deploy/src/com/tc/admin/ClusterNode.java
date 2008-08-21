/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.auth.AuthScope;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.dso.ClassesNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.dso.GCStatsNode;
import com.tc.admin.dso.RootsNode;
import com.tc.admin.dso.locks.LocksNode;
import com.tc.admin.model.ClusterModel;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterNode;
import com.tc.admin.model.IServer;
import com.tc.admin.model.Server;
import com.tc.admin.model.ServerVersion;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.management.remote.JMXConnector;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class ClusterNode extends ComponentNode implements ConnectionListener {
  private AdminClientContext  m_acc;
  private ClusterModel        m_clusterModel;
  private ClusterPanel        m_clusterPanel;
  private ConnectDialog       m_connectDialog;
  private JDialog             m_versionMismatchDialog;
  private AtomicBoolean       m_versionCheckOccurred;
  private JPopupMenu          m_popupMenu;
  private ConnectAction       m_connectAction;
  private DisconnectAction    m_disconnectAction;
  private DeleteAction        m_deleteAction;
  private AutoConnectAction   m_autoConnectAction;
  private JCheckBoxMenuItem   m_autoConnectMenuItem;

  private LocksNode           m_locksNode;
  private ClientsNode         m_clientsNode;
  private StatsRecorderNode   m_statsRecorderNode;

  private static final String CONNECT_ACTION      = "Connect";
  private static final String DISCONNECT_ACTION   = "Disconnect";
  private static final String DELETE_ACTION       = "Delete";
  private static final String AUTO_CONNECT_ACTION = "AutoConnect";

  private static final String HOST                = ServersHelper.HOST;
  private static final String PORT                = ServersHelper.PORT;
  private static final String AUTO_CONNECT        = ServersHelper.AUTO_CONNECT;

  ClusterNode() {
    this(ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT, ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  ClusterNode(final String host, final int jmxPort, final boolean autoConnect) {
    super();

    m_acc = AdminClient.getContext();
    m_clusterModel = new ClusterModel(host, jmxPort, autoConnect);

    setLabel(m_acc.getString("cluster.node.label"));
    initMenu(autoConnect);
    setComponent(m_clusterPanel = createClusterPanel());
    setIcon(ServersHelper.getHelper().getServerIcon());
    m_clusterModel.addPropertyChangeListener(new ClusterPropertyChangeListener());
    m_versionCheckOccurred = new AtomicBoolean(false);
  }

  public IClusterModel getClusterModel() {
    return m_clusterModel;
  }

  boolean isDBBackupSupported() {
    return getClusterModel().isDBBackupSupported();
  }

  public IServer[] getClusterServers() throws Exception {
    return m_clusterModel.getClusterServers();
  }

  private class ClusterPropertyChangeRunnable implements Runnable {
    PropertyChangeEvent m_pce;

    ClusterPropertyChangeRunnable(PropertyChangeEvent pce) {
      m_pce = pce;
    }

    public void run() {
      String prop = m_pce.getPropertyName();
      if (IServer.PROP_CONNECTED.equals(prop)) {
        if (m_clusterModel.isConnected()) {
          handleConnected();
        } else {
          m_acc.removeServerLog(m_clusterModel);
        }
      } else if (IClusterNode.PROP_READY.equals(prop)) {
        handleReady();
      } else if (IServer.PROP_CONNECT_ERROR.equals(prop)) {
        handleConnectError();
      } else if (IClusterModel.PROP_ACTIVE_SERVER.equals(prop)) {
        handleActiveServer();
      }
    }
  }

  private class ClusterPropertyChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      Runnable r = new ClusterPropertyChangeRunnable(evt);
      if (SwingUtilities.isEventDispatchThread()) {
        r.run();
      } else {
        SwingUtilities.invokeLater(r);
      }
    }
  }

  private boolean testCheckServerVersion() {
    if (m_versionCheckOccurred.getAndSet(true)) { return true; }
    return m_acc.testServerMatch(this);
  }

  private void handleConnected() {
    if (m_acc == null) return;
    if (!testCheckServerVersion()) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (m_clusterModel.isConnected()) {
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

    m_connectAction.setEnabled(false);
    m_disconnectAction.setEnabled(true);
  }

  private void handleDisconnected() {
    if (m_versionMismatchDialog != null) {
      m_versionMismatchDialog.setVisible(false);
    }
    handleDisconnect();
  }

  private void handleReady() {
    if (m_clusterModel.isReady()) {
      handleConnected();
    }
  }

  private void handleActiveServer() {
    IServer activeServer = m_clusterModel.getActiveServer();
    if (activeServer != null) {
      handleNewActive();
    } else {
      handleDisconnected();
    }
  }

  private void handleConnectError() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        reportConnectError();
      }
    });
  }

  protected ClusterPanel createClusterPanel() {
    return new ClusterPanel(this);
  }

  void handleNewActive() {
    handleActivation();
    m_acc.addServerLog(m_clusterModel);
    m_clusterPanel.reinitialize();
    m_acc.nodeChanged(this);
  }

  String[] getConnectionCredentials() {
    return m_clusterModel.getConnectionCredentials();
  }

  Map<String, Object> getConnectionEnvironment() {
    return m_clusterModel.getConnectionEnvironment();
  }

  public ConnectionContext getConnectionContext() {
    return m_clusterModel.getConnectionContext();
  }

  void setHost(String host) {
    m_clusterModel.setHost(host);
  }

  String getHost() {
    return m_clusterModel.getHost();
  }

  void setPort(int port) {
    m_clusterModel.setPort(port);
  }

  int getPort() {
    return m_clusterModel.getPort();
  }

  boolean isAutoConnect() {
    return m_clusterModel.isAutoConnect();
  }

  void setAutoConnect(boolean autoConnect) {
    m_clusterModel.setAutoConnect(autoConnect);
  }

  Integer getDSOListenPort() throws Exception {
    return m_clusterModel.getDSOListenPort();
  }

  String getStatsExportServletURI() throws Exception {
    return m_clusterModel.getStatsExportServletURI();
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

  boolean isConnected() {
    return m_clusterModel.isConnected();
  }

  boolean isStarted() {
    return m_clusterModel.isStarted();
  }

  boolean isActive() {
    return m_clusterModel.isActive();
  }

  boolean isPassiveUninitialized() {
    return m_clusterModel.isPassiveUninitialized();
  }

  boolean isPassiveStandby() {
    return m_clusterModel.isPassiveStandby();
  }

  boolean hasConnectionException() {
    return m_clusterModel.hasConnectError();
  }

  ConnectDialog getConnectDialog(ConnectionListener listener) {
    if (m_connectDialog == null) {
      Frame frame = (Frame) m_clusterPanel.getAncestorOfClass(java.awt.Frame.class);
      m_connectDialog = new ConnectDialog(frame, m_clusterModel, listener);
    } else {
      m_connectDialog.setServer(m_clusterModel);
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
      m_acc.log(e);
    }
  }

  private void beginConnect() throws Exception {
    ConnectDialog cd = getConnectDialog(this);
    m_clusterModel.refreshCachedCredentials();
    cd.center((Frame) m_clusterPanel.getAncestorOfClass(java.awt.Frame.class));
    cd.setVisible(true);
  }

  /**
   * Messaged by ConnectDialog.
   */
  public void handleConnection() {
    JMXConnector jmxc;
    if ((jmxc = m_connectDialog.getConnector()) != null) {
      try {
        m_clusterModel.setJMXConnector(jmxc);
        m_clusterModel.refreshCachedCredentials();
      } catch (IOException ioe) {
        reportConnectError(ioe);
      }
    }
  }

  /**
   * Messaged by ConnectDialog.
   */
  public void handleException() {
    Exception e = m_connectDialog.getError();
    if (e != null) {
      reportConnectError(e);
    }
  }

  private void reportConnectError() {
    reportConnectError(m_clusterModel.getConnectError());
  }

  private void reportConnectError(Exception error) {
    String msg = Server.getConnectErrorMessage(error, m_clusterModel);

    if (msg != null && m_clusterPanel != null) {
      boolean autoConnect = isAutoConnect();
      if (autoConnect && error instanceof SecurityException) {
        setAutoConnect(autoConnect = false);
        m_autoConnectMenuItem.setSelected(false);
        m_acc.updateServerPrefs();
      }
      if (!autoConnect) {
        m_clusterPanel.setupConnectButton();
      }
      m_clusterPanel.setStatusLabel(msg);
    }
    m_acc.nodeChanged(this);
  }

  /**
   * Called when the user clicks the Disconnect button. Used whether or not auto-connect is enabled.
   */
  void disconnect() {
    disconnect(false);
  }

  /**
   * Called when the user is deleting this node and is currently connected.
   */
  private void disconnectForDelete() {
    disconnect(true);
  }

  private void disconnect(boolean deletingNode) {
    m_acc.setStatus(m_acc.format("disconnecting.from", this));
    if (!deletingNode) {
      setAutoConnect(false);
      m_acc.updateServerPrefs();
      m_autoConnectMenuItem.setSelected(false);
    }
    m_clusterModel.disconnect();
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
      super(m_acc.getString("connect.label"), ServersHelper.getHelper().getConnectIcon());
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
      super(m_acc.getString("disconnect.label"), ServersHelper.getHelper().getDisconnectIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MENU_SHORTCUT_KEY_MASK, true));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      boolean recordingStats = haveActiveRecordingSession();
      boolean profilingLocks = isProfilingLocks();

      if (recordingStats || profilingLocks) {
        String key;
        if (recordingStats && profilingLocks) {
          key = "recording.stats.profiling.locks.msg";
        } else if (recordingStats) {
          key = "recording.stats.msg";
        } else {
          key = "profiling.locks.msg";
        }

        String msg = m_acc.format(key, m_acc.getMessage("disconnect.anyway"));
        Frame frame = (Frame) m_clusterPanel.getAncestorOfClass(Frame.class);
        int answer = JOptionPane.showConfirmDialog(m_clusterPanel, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) { return; }
      }
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
        disconnectForDelete();
      }

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          AdminClientContext acc = AdminClient.getContext();
          acc.setStatus(m_acc.format("deleted.server", ClusterNode.this));
          acc.remove(ClusterNode.this);
          acc.updateServerPrefs();
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

      m_connectAction.setEnabled(!autoConnect);
      setAutoConnect(autoConnect);
      m_clusterPanel.setupConnectButton();
      m_acc.updateServerPrefs();
    }
  }

  private AtomicBoolean m_addingChildren = new AtomicBoolean(false);

  void tryAddChildren() {
    if (!m_clusterModel.isReady()) { return; }
    if (m_addingChildren.get()) { return; }

    try {
      m_addingChildren.set(true);
      if (getChildCount() == 0) {
        addChildren();
        m_acc.nodeStructureChanged(this);
        m_acc.expand(this);
        m_acc.addServerLog(m_clusterModel);
      }
    } finally {
      m_addingChildren.set(false);
    }
  }

  protected void addChildren() {
    add(createRootsNode());
    add(new ClassesNode(this));
    try {
      add(m_locksNode = createLocksNode());
    } catch (Throwable t) {
      // Need a more specific exception but this means we're trying to connect to an
      // older version of the server, that doesn't have the LockMonitorMBean we expect.
    }
    add(createGCStatsNode());
    add(createThreadDumpsNode());
    add(m_statsRecorderNode = createStatsRecorderNode());
    add(m_clientsNode = createClientsNode());
    add(createServersNode());
  }

  protected RootsNode createRootsNode() {
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
      m_acc.remove(m_statsRecorderNode);
      m_statsRecorderNode.tearDown();
      m_statsRecorderNode = null;
    }
  }

  protected GCStatsNode createGCStatsNode() {
    return new GCStatsNode(this);
  }

  protected LocksNode createLocksNode() {
    return new LocksNode(this);
  }

  protected ServersNode createServersNode() {
    return new ServersNode(this);
  }

  protected ClientsNode createClientsNode() {
    return new ClientsNode(this);
  }

  public void selectClientNode(String remoteAddr) {
    m_clientsNode.selectClientNode(remoteAddr);
  }

  void handleStarting() {
    m_acc.nodeChanged(this);
    m_clusterPanel.started();
  }

  void handlePassiveUninitialized() {
    m_acc.nodeChanged(this);
    m_clusterPanel.passiveUninitialized();
  }

  void handlePassiveStandby() {
    m_acc.nodeChanged(this);
    m_clusterPanel.passiveStandby();
  }

  void handleActivation() {
    m_acc.nodeChanged(this);
    tryAddChildren();
    m_clusterPanel.activated();
  }

  public ServerVersion getProductInfo() {
    return m_clusterModel.getProductInfo();
  }

  public String getProductVersion() {
    return getProductInfo().version();
  }

  public String getProductBuildID() {
    return getProductInfo().buildID();
  }

  public String getProductLicense() {
    return getProductInfo().license();
  }

  String getEnvironment() {
    return m_clusterModel.getEnvironment();
  }

  String getConfig() {
    return m_clusterModel.getConfig();
  }

  long getStartTime() {
    return m_clusterModel.getStartTime();
  }

  long getActivateTime() {
    return m_clusterModel.getActivateTime();
  }

  StatisticsLocalGathererMBean getStatisticsGathererMBean() {
    return m_clusterModel.getStatisticsGathererMBean();
  }

  boolean haveActiveRecordingSession() {
    return m_statsRecorderNode != null && m_statsRecorderNode.isRecording();
  }

  boolean isProfilingLocks() {
    return m_locksNode != null ? m_locksNode.isProfiling() : false;
  }

  boolean haveMonitoringActivity() {
    return haveActiveRecordingSession() || isProfilingLocks();
  }

  private ScheduledExecutorService m_scheduledExecutor;
  private ScheduledFuture<?>       m_monitoringTaskFuture;
  private Runnable                 m_monitoringActivityTask;
  private final long               MONITORING_FEEDBACK_MILLIS = 750;

  private synchronized ScheduledExecutorService getScheduledExecutor() {
    if (m_scheduledExecutor == null) {
      m_scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    return m_scheduledExecutor;
  }

  private synchronized Runnable getMonitoringActivityTask() {
    if (m_monitoringActivityTask == null) {
      m_monitoringActivityTask = new MonitoringActivityTask();
    }
    return m_monitoringActivityTask;
  }

  private class MonitoringActivityTask implements Runnable {
    private Icon m_defaultIcon  = ServersHelper.getHelper().getServerIcon();
    private Icon m_activityIcon = ServersHelper.getHelper().getActivityIcon();

    public void run() {
      boolean haveActivity = haveMonitoringActivity();
      setIcon(determineIcon(haveActivity));
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          notifyChanged();
        }
      });
      if (haveActivity) {
        m_monitoringTaskFuture = getScheduledExecutor().schedule(this, MONITORING_FEEDBACK_MILLIS,
                                                                 TimeUnit.MILLISECONDS);
      } else {
        m_monitoringTaskFuture = null;
      }
    }

    private Icon determineIcon(boolean haveActivity) {
      Icon icon = m_defaultIcon;
      if (haveActivity) {
        icon = (getIcon() == m_defaultIcon) ? m_activityIcon : m_defaultIcon;
      }
      return icon;
    }
  }

  private synchronized void testStartMonitoringTask() {
    if (m_monitoringTaskFuture == null) {
      m_monitoringTaskFuture = getScheduledExecutor().schedule(getMonitoringActivityTask(), MONITORING_FEEDBACK_MILLIS,
                                                               TimeUnit.MILLISECONDS);
    }
  }

  private synchronized void testStopMonitoringTask() {
    if (m_scheduledExecutor != null) {
      m_scheduledExecutor.shutdownNow();
      m_scheduledExecutor = null;
      setIcon(ServersHelper.getHelper().getServerIcon());
    }
    m_monitoringTaskFuture = null;
  }

  public void showRecordingStats(boolean recordingStats) {
    testStartMonitoringTask();
  }

  public void showProfilingLocks(boolean profilingLocks) {
    testStartMonitoringTask();
  }

  void handleDisconnect() {
    testStopMonitoringTask();
    m_acc.select(this);

    m_locksNode = null;
    m_clientsNode = null;

    tearDownChildren();
    removeAllChildren();
    m_acc.nodeStructureChanged(this);
    m_clusterPanel.disconnected();
    m_versionCheckOccurred.set(false);
  }

  Color getServerStatusColor() {
    return getServerStatusColor(m_clusterModel);
  }

  static Color getServerStatusColor(Server server) {
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

  ClusterThreadDumpEntry takeThreadDump() {
    ClusterThreadDumpEntry tde = new ClusterThreadDumpEntry();
    Map<IClusterNode, String> map = m_clusterModel.takeThreadDump();
    Iterator<Map.Entry<IClusterNode, String>> iter = map.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<IClusterNode, String> entry = iter.next();
      tde.add(entry.getKey().toString(), entry.getValue());
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
    testStopMonitoringTask();
    if (m_connectDialog != null) {
      m_connectDialog.tearDown();
    }
    m_clusterModel.tearDown();

    m_acc = null;
    m_clusterModel = null;
    m_clusterPanel = null;
    m_connectDialog = null;
    m_popupMenu = null;
    m_connectAction = null;
    m_disconnectAction = null;
    m_deleteAction = null;
    m_autoConnectAction = null;

    m_locksNode = null;
    m_clientsNode = null;

    m_monitoringActivityTask = null;

    super.tearDown();
  }
}
