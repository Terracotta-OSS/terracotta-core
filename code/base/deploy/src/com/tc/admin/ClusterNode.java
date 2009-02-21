/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.ClusteredHeapNode;
import com.tc.admin.dso.DiagnosticsNode;
import com.tc.admin.model.ClusterModel;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterNode;
import com.tc.admin.model.IClusterStatsListener;
import com.tc.admin.model.IProductVersion;
import com.tc.admin.model.IServer;
import com.tc.statistics.retrieval.actions.SRAThreadDump;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
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

public class ClusterNode extends ClusterElementNode implements ConnectionListener, ClusterThreadDumpProvider,
    IClusterStatsListener, PropertyChangeListener, PreferenceChangeListener {
  protected IAdminClientContext adminClientContext;
  private ClusterModel          clusterModel;
  private ClusterPanel          clusterPanel;
  private ConnectDialog         connectDialog;
  private JDialog               versionMismatchDialog;
  private final AtomicBoolean   versionCheckOccurred;
  private JPopupMenu            popupMenu;
  private ConnectAction         connectAction;
  private DisconnectAction      disconnectAction;
  private DeleteAction          deleteAction;
  private AutoConnectAction     autoConnectAction;
  private JCheckBoxMenuItem     autoConnectMenuItem;

  private boolean               isRecordingClusterStats;
  private boolean               isProfilingLocks;

  private static final String   CONNECT_ACTION      = "Connect";
  private static final String   DISCONNECT_ACTION   = "Disconnect";
  private static final String   DELETE_ACTION       = "Delete";
  private static final String   AUTO_CONNECT_ACTION = "AutoConnect";

  private static final String   HOST                = ServersHelper.HOST;
  private static final String   PORT                = ServersHelper.PORT;
  private static final String   AUTO_CONNECT        = ServersHelper.AUTO_CONNECT;

  ClusterNode(IAdminClientContext adminClientContext) {
    this(adminClientContext, ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT,
         ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  ClusterNode(IAdminClientContext adminClientContext, final String host, final int jmxPort, final boolean autoConnect) {
    super(new ClusterModel(host, jmxPort, autoConnect));

    this.adminClientContext = adminClientContext;
    clusterModel = (ClusterModel) getClusterElement();

    setLabel(adminClientContext.getString("cluster.node.label"));
    initMenu(autoConnect);
    setComponent(clusterPanel = createClusterPanel());
    setIcon(ServersHelper.getHelper().getServerIcon());
    clusterModel.addPropertyChangeListener(new ClusterListener(clusterModel));
    versionCheckOccurred = new AtomicBoolean(false);

    Preferences prefs = adminClientContext.getPrefs().node("RuntimeStats");
    prefs.addPreferenceChangeListener(this);
  }

  public synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  public synchronized IServer getActiveCoordinator() {
    IClusterModel theClusterModel = getClusterModel();
    return theClusterModel != null ? theClusterModel.getActiveCoordinator() : null;
  }

  boolean isDBBackupSupported() {
    IServer activeCoord = getActiveCoordinator();
    return activeCoord != null ? activeCoord.isDBBackupSupported() : false;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected PropertyChangeRunnable createPropertyChangeRunnable(PropertyChangeEvent evt) {
      return new PropertyChangeRunnable(evt) {
        @Override
        public void run() {
          super.run();
          if (IClusterModel.PROP_AUTO_CONNECT.equals(pce.getPropertyName())) {
            autoConnectMenuItem.setSelected(clusterModel.isAutoConnect());
          }
        }
      };
    }

    @Override
    public void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClusterStatsListener(ClusterNode.this);
        oldActive.removePropertyChangeListener(ClusterNode.this);
      }
      if (newActive != null) {
        if (newActive.isClusterStatsSupported()) {
          newActive.addClusterStatsListener(ClusterNode.this);
        }
        newActive.addPropertyChangeListener(ClusterNode.this);
      }
    }

    @Override
    protected void handleConnected() {
      if (clusterModel.isConnected()) {
        connectAction.setEnabled(false);
      } else {
        if (versionMismatchDialog != null) {
          versionMismatchDialog.setVisible(false);
        }
        handleDisconnect();
        if (clusterModel.hasConnectError()) {
          reportConnectError(clusterModel.getConnectError());
        }
      }
    }

    @Override
    protected void handleReady() {
      if (clusterModel.isReady()) {
        if (adminClientContext == null) return;
        if (!testCheckServerVersion()) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              if (clusterModel.isConnected()) {
                disconnect();
              }
            }
          });
          return;
        }
        handleActivation();
        clusterPanel.reinitialize();
        nodeChanged();
        connectAction.setEnabled(false);
        disconnectAction.setEnabled(true);
        adminClientContext.getAdminClientController().setStatus("Ready");
      } else {
        adminClientContext.getAdminClientController().setStatus("Not ready");
      }
    }
  }

  private boolean testCheckServerVersion() {
    if (versionCheckOccurred.getAndSet(true)) { return true; }
    return adminClientContext.getAdminClientController().testServerMatch(this);
  }

  protected ClusterPanel createClusterPanel() {
    return new ClusterPanel(adminClientContext, this);
  }

  String[] getConnectionCredentials() {
    return clusterModel.getConnectionCredentials();
  }

  Map<String, Object> getConnectionEnvironment() {
    return clusterModel.getConnectionEnvironment();
  }

  void setHost(String host) {
    clusterModel.setHost(host);
  }

  String getHost() {
    return clusterModel.getHost();
  }

  void setPort(int port) {
    clusterModel.setPort(port);
  }

  int getPort() {
    return clusterModel.getPort();
  }

  private void initMenu(boolean autoConnect) {
    popupMenu = new JPopupMenu("Server Actions");

    connectAction = new ConnectAction();
    disconnectAction = new DisconnectAction();
    deleteAction = new DeleteAction();
    autoConnectAction = new AutoConnectAction();

    addActionBinding(CONNECT_ACTION, connectAction);
    addActionBinding(DISCONNECT_ACTION, disconnectAction);
    addActionBinding(DELETE_ACTION, deleteAction);
    addActionBinding(AUTO_CONNECT_ACTION, autoConnectAction);

    popupMenu.add(connectAction);
    popupMenu.add(disconnectAction);
    popupMenu.add(new JSeparator());
    popupMenu.add(deleteAction);
    popupMenu.add(new JSeparator());

    popupMenu.add(autoConnectMenuItem = new JCheckBoxMenuItem(autoConnectAction));
    autoConnectMenuItem.setSelected(autoConnect);
  }

  void setVersionMismatchDialog(JDialog dialog) {
    versionMismatchDialog = dialog;
  }

  boolean isConnected() {
    return clusterModel.isConnected();
  }

  boolean isReady() {
    return clusterModel.isReady();
  }

  ConnectDialog getConnectDialog(ConnectionListener listener) {
    if (connectDialog == null) {
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, clusterPanel);
      connectDialog = new ConnectDialog(adminClientContext, frame, clusterModel, listener);
    } else {
      connectDialog.setClusterModel(clusterModel);
      connectDialog.setConnectionListener(listener);
    }

    return connectDialog;
  }

  /**
   * Called when the user clicks the Connect button. Not used when auto-connect is enabled.
   */
  void connect() {
    try {
      clusterModel.connect();
      beginConnect();
    } catch (Exception e) {
      adminClientContext.log(e);
    }
  }

  private void beginConnect() throws Exception {
    ConnectDialog cd = getConnectDialog(this);
    clusterModel.refreshCachedCredentials();
    cd.center();
    cd.setVisible(true);
  }

  /**
   * Messaged by ConnectDialog.
   */
  public void handleConnection() {
    JMXConnector jmxc;
    if ((jmxc = connectDialog.getConnector()) != null) {
      try {
        clusterModel.setJMXConnector(jmxc);
        clusterModel.refreshCachedCredentials();
      } catch (IOException ioe) {
        reportConnectError(ioe);
      }
    }
  }

  /**
   * Messaged by ConnectDialog.
   */
  public void handleException() {
    Exception e = connectDialog.getError();
    if (e != null) {
      reportConnectError(e);
    }
  }

  private void reportConnectError(Exception error) {
    String msg = clusterModel.getConnectErrorMessage(error);

    if (msg != null && clusterPanel != null) {
      boolean autoConnect = isAutoConnect();
      if (autoConnect && error instanceof SecurityException) {
        setAutoConnect(autoConnect = false);
        autoConnectMenuItem.setSelected(false);
        adminClientContext.getAdminClientController().updateServerPrefs();
      }
      if (!autoConnect) {
        clusterPanel.setupConnectButton();
      }
      clusterPanel.setStatusLabel(msg);
    }
    nodeChanged();
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
    adminClientContext.setStatus(adminClientContext.format("disconnecting.from", this));
    if (!deletingNode) {
      setAutoConnect(false);
      adminClientContext.getAdminClientController().updateServerPrefs();
      autoConnectMenuItem.setSelected(false);
    }
    clusterModel.disconnect();
  }

  @Override
  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  public void setPreferences(Preferences prefs) {
    prefs.put(HOST, getHost());
    prefs.putInt(PORT, getPort());
    prefs.putBoolean(AUTO_CONNECT, isAutoConnect());
  }

  private class ConnectAction extends XAbstractAction {
    ConnectAction() {
      super(adminClientContext.getString("connect.label"), ServersHelper.getHelper().getConnectIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      connect();
    }
  }

  DisconnectAction getDisconnectAction() {
    return disconnectAction;
  }

  class DisconnectAction extends XAbstractAction {
    DisconnectAction() {
      super(adminClientContext.getString("disconnect.label"), ServersHelper.getHelper().getDisconnectIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MENU_SHORTCUT_KEY_MASK, true));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      boolean recordingStats = recordingClusterStats();
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

        String msg = adminClientContext.format(key, adminClientContext.getMessage("disconnect.anyway"));
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, clusterPanel);
        int answer = JOptionPane.showConfirmDialog(clusterPanel, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) { return; }
      }
      disconnect();
    }
  }

  private class DeleteAction extends XAbstractAction implements Runnable {
    DeleteAction() {
      super("Delete", ServersHelper.getHelper().getDeleteIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      if (isConnected()) {
        disconnectForDelete();
      }
      SwingUtilities.invokeLater(this);
    }

    public void run() {
      AdminClientController controller = adminClientContext.getAdminClientController();
      adminClientContext.setStatus(adminClientContext.format("deleted.server", ClusterNode.this));
      ((XTreeNode) getParent()).removeChild(ClusterNode.this);
      controller.updateServerPrefs();
    }
  }

  boolean isAutoConnect() {
    return clusterModel.isAutoConnect();
  }

  void setAutoConnect(boolean autoConnect) {
    if (autoConnect && !clusterModel.isConnected()) {
      clusterModel.connect();
    }
    clusterModel.setAutoConnect(autoConnect);
    adminClientContext.getAdminClientController().updateServerPrefs();
  }

  private class AutoConnectAction extends XAbstractAction implements Runnable {
    AutoConnectAction() {
      super("Auto-connect");
      setShortDescription("Attempt to connect automatically");
    }

    public void actionPerformed(ActionEvent ae) {
      SwingUtilities.invokeLater(this);
    }

    public void run() {
      boolean autoConnect = autoConnectMenuItem.isSelected();
      connectAction.setEnabled(!autoConnect);
      setAutoConnect(autoConnect);
      clusterPanel.setupConnectButton();
      adminClientContext.getAdminClientController().updateServerPrefs();
    }
  }

  private final AtomicBoolean addingChildren = new AtomicBoolean(false);

  void tryAddChildren() {
    if (!clusterModel.isReady()) { return; }
    if (addingChildren.get()) { return; }

    try {
      addingChildren.set(true);
      if (getChildCount() == 0) {
        addChildren();
        AdminClientController controller = adminClientContext.getAdminClientController();
        nodeStructureChanged();
        controller.expand(this);

        Enumeration theChildren = children();
        while (theChildren.hasMoreElements()) {
          controller.expand((XTreeNode) theChildren.nextElement());
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      addingChildren.set(false);
    }
  }

  protected void addChildren() {
    add(createClusteredHeapNode());
    add(createDiagnosticsNode());
    add(createTopologyNode());
  }

  protected ClusteredHeapNode createClusteredHeapNode() {
    return new ClusteredHeapNode(adminClientContext, getClusterModel());
  }

  protected DiagnosticsNode createDiagnosticsNode() {
    return new DiagnosticsNode(adminClientContext, getClusterModel(), this);
  }

  protected TopologyNode createTopologyNode() {
    return new TopologyNode(adminClientContext, getClusterModel());
  }

  void handleStarting() {
    nodeChanged();
    clusterPanel.started();
  }

  void handlePassiveUninitialized() {
    nodeChanged();
    clusterPanel.passiveUninitialized();
  }

  void handlePassiveStandby() {
    nodeChanged();
    clusterPanel.passiveStandby();
  }

  void handleActivation() {
    nodeChanged();
    tryAddChildren();
    clusterPanel.activated();
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.addPropertyChangeListener(ClusterNode.this);
    }
  }

  public IProductVersion getProductInfo() {
    IServer activeCoord = getActiveCoordinator();
    return activeCoord != null ? activeCoord.getProductInfo() : null;
  }

  public String getProductVersion() {
    IProductVersion info = getProductInfo();
    return info != null ? info.version() : "";
  }

  public String getProductBuildID() {
    IProductVersion info = getProductInfo();
    return info != null ? info.buildID() : "";
  }

  public String getProductLicense() {
    IProductVersion info = getProductInfo();
    return info != null ? info.license() : "";
  }

  long getStartTime() {
    IServer activeCoord = getActiveCoordinator();
    return activeCoord != null ? activeCoord.getStartTime() : -1;
  }

  long getActivateTime() {
    IServer activeCoord = getActiveCoordinator();
    return activeCoord != null ? activeCoord.getActivateTime() : -1;
  }

  boolean recordingClusterStats() {
    return isRecordingClusterStats;
  }

  boolean isProfilingLocks() {
    return isProfilingLocks;
  }

  boolean haveMonitoringActivity() {
    return recordingClusterStats() || isProfilingLocks();
  }

  private ScheduledExecutorService scheduledExecutor;
  private ScheduledFuture<?>       monitoringTaskFuture;
  private Runnable                 monitoringActivityTask;
  private final long               MONITORING_FEEDBACK_MILLIS = 750;

  private synchronized ScheduledExecutorService getScheduledExecutor() {
    if (scheduledExecutor == null) {
      scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    return scheduledExecutor;
  }

  private synchronized Runnable getMonitoringActivityTask() {
    if (monitoringActivityTask == null) {
      monitoringActivityTask = new MonitoringActivityTask();
    }
    return monitoringActivityTask;
  }

  private class MonitoringActivityTask implements Runnable {
    private final Icon defaultIcon  = ServersHelper.getHelper().getServerIcon();
    private final Icon activityIcon = ServersHelper.getHelper().getActivityIcon();

    public void run() {
      boolean haveActivity = haveMonitoringActivity();
      setIcon(determineIcon(haveActivity));
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          nodeChanged();
        }
      });
      if (haveActivity) {
        monitoringTaskFuture = getScheduledExecutor().schedule(this, MONITORING_FEEDBACK_MILLIS, TimeUnit.MILLISECONDS);
      } else {
        monitoringTaskFuture = null;
      }
    }

    private Icon determineIcon(boolean haveActivity) {
      Icon icon = defaultIcon;
      if (haveActivity) {
        icon = (getIcon() == defaultIcon) ? activityIcon : defaultIcon;
      }
      return icon;
    }
  }

  private synchronized void testStartMonitoringTask() {
    if (monitoringTaskFuture == null) {
      monitoringTaskFuture = getScheduledExecutor().schedule(getMonitoringActivityTask(), MONITORING_FEEDBACK_MILLIS,
                                                             TimeUnit.MILLISECONDS);
    }
  }

  private synchronized void testStopMonitoringTask() {
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdownNow();
      scheduledExecutor = null;
      setIcon(ServersHelper.getHelper().getServerIcon());
    }
    monitoringTaskFuture = null;
  }

  public void showRecordingStats(boolean recordingStats) {
    isRecordingClusterStats = recordingStats;
    testStartMonitoringTask();
  }

  public void showProfilingLocks(boolean profilingLocks) {
    isProfilingLocks = profilingLocks;
    testStartMonitoringTask();
  }

  void handleDisconnect() {
    if (getChildCount() > 0) {
      testStopMonitoringTask();
      adminClientContext.getAdminClientController().select(this);

      tearDownChildren();
      removeAllChildren();
      nodeStructureChanged();
      clusterPanel.disconnected();
      versionCheckOccurred.set(false);
    }
  }

  Color getServerStatusColor() {
    return ServerHelper.getHelper().getServerStatusColor(getActiveCoordinator());
  }

  public ClusterThreadDumpEntry takeThreadDump() {
    ClusterThreadDumpEntry tde = new ClusterThreadDumpEntry(adminClientContext);
    Map<IClusterNode, Future<String>> map = clusterModel.takeThreadDump();
    Iterator<Map.Entry<IClusterNode, Future<String>>> iter = map.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<IClusterNode, Future<String>> entry = iter.next();
      tde.add(entry.getKey().toString(), entry.getValue());
    }
    testTriggerThreadDumpSRA();
    return tde;
  }

  void testTriggerThreadDumpSRA() {
    final IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null && activeCoord.isActiveClusterStatsSession()) {
      adminClientContext.submit(new Runnable() {
        public void run() {
          activeCoord.captureClusterStat(SRAThreadDump.ACTION_NAME);
        }
      });
    }
  }

  @Override
  public void tearDown() {
    testStopMonitoringTask();
    if (connectDialog != null) {
      connectDialog.tearDown();
    }
    clusterModel.tearDown();

    super.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      clusterPanel = null;
      connectDialog = null;
      popupMenu = null;
      connectAction = null;
      disconnectAction = null;
      deleteAction = null;
      autoConnectAction = null;
      monitoringActivityTask = null;
    }
  }

  public void allSessionsCleared() {
    /**/
  }

  public void connected() {
    /**/
  }

  public void disconnected() {
    /**/
  }

  public void reinitialized() {
    /**/
  }

  public void sessionCleared(String sessionId) {
    /**/
  }

  public void sessionCreated(String sessionId) {
    /**/
  }

  public void sessionStarted(String sessionId) {
    showRecordingStats(true);
  }

  public void sessionStopped(String sessionId) {
    showRecordingStats(false);
  }

  public void propertyChange(PropertyChangeEvent evt) {
    if (IServer.PROP_LOCK_STATS_ENABLED.equals(evt.getPropertyName())) {
      showProfilingLocks((Boolean) evt.getNewValue());
    }
  }

  public void preferenceChange(PreferenceChangeEvent evt) {
    Preferences prefs = evt.getNode();
    String key = evt.getKey();

    if (key.equals("poll-periods-seconds")) {
      clusterModel.setPollPeriod(prefs.getInt(key, 3));
    }
  }
}
