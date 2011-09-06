/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.MonitoringNode;
import com.tc.admin.dso.PlatformNode;
import com.tc.admin.model.ClusterModel;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterNode;
import com.tc.admin.model.IClusterStatsListener;
import com.tc.admin.model.IProductVersion;
import com.tc.admin.model.IServer;
import com.tc.admin.options.RuntimeStatsOption;
import com.tc.statistics.retrieval.actions.SRAThreadDump;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
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
  protected final IAdminClientContext adminClientContext;
  protected final IClusterModel       clusterModel;

  private ClusterPanel                clusterPanel;
  private ConnectDialog               connectDialog;
  private JDialog                     versionMismatchDialog;
  private final AtomicBoolean         versionCheckOccurred;
  private JPopupMenu                  popupMenu;
  private RenameAction                renameAction;
  private ConnectAction               connectAction;
  private DisconnectAction            disconnectAction;
  private DeleteAction                deleteAction;
  private AutoConnectAction           autoConnectAction;
  private JCheckBoxMenuItem           autoConnectMenuItem;

  private boolean                     isRecordingClusterStats;
  private boolean                     isProfilingLocks;

  private static final String         CONNECT_ACTION      = "Connect";
  private static final String         DISCONNECT_ACTION   = "Disconnect";
  private static final String         DELETE_ACTION       = "Delete";
  private static final String         AUTO_CONNECT_ACTION = "AutoConnect";

  private static final String         NAME                = ServersHelper.NAME;
  private static final String         HOST                = ServersHelper.HOST;
  private static final String         PORT                = ServersHelper.PORT;
  private static final String         AUTO_CONNECT        = ServersHelper.AUTO_CONNECT;

  ClusterNode(IAdminClientContext adminClientContext) {
    this(adminClientContext, new ClusterModel(ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT,
                                              ConnectionContext.DEFAULT_AUTO_CONNECT),
         ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  public ClusterNode(IAdminClientContext adminClientContext, ClusterModel clusterModel, final boolean autoConnect) {
    super(clusterModel);

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    setClusterName(adminClientContext.getString("cluster.node.label"));
    initMenu(autoConnect);
    setComponent(clusterPanel = createClusterPanel());
    setIcon(ServersHelper.getHelper().getServerIcon());
    clusterModel.addPropertyChangeListener(new ClusterListener(clusterModel));
    versionCheckOccurred = new AtomicBoolean(false);

    RuntimeStatsOption runtimeStatsOption = (RuntimeStatsOption) adminClientContext.getOption(RuntimeStatsOption.NAME);
    if (runtimeStatsOption != null) {
      clusterModel.setPollPeriod(runtimeStatsOption.getPollPeriodSeconds());
      clusterModel.setPollTimeout(runtimeStatsOption.getPollTimeoutSeconds());
    }
    Preferences prefs = adminClientContext.getPrefs().node(RuntimeStatsOption.NAME);
    prefs.addPreferenceChangeListener(this);
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public IServer getActiveCoordinator() {
    return clusterModel.getActiveCoordinator();
  }

  boolean isDBBackupSupported() {
    return false;
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
    protected void handleConnectError(Exception connectError) {
      if (connectDialog != null && connectDialog.isVisible()) { return; }

      reportConnectError(connectError);
      if (connectError instanceof SecurityException) {
        try {
          beginConnect();
        } catch (Exception e) {
          adminClientContext.log(e);
        }
      }
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
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
        handleStarting();
      } else {
        if (versionMismatchDialog != null) {
          versionMismatchDialog.setVisible(false);
        }
        handleDisconnect();
        if (isAutoConnect()) {
          clusterModel.connect();
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
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      adminClientContext.log(e);
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

  void setClusterName(String name) {
    setLabel(name);
    clusterModel.setName(name);
  }

  String getClusterName() {
    return clusterModel.getName();
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

    renameAction = new RenameAction();
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
    popupMenu.add(renameAction);
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
    final JMXConnector jmxc;
    if ((jmxc = connectDialog.getConnector()) != null) {
      clusterPanel.setStatusLabel(adminClientContext.getString("connecting"));
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            clusterModel.setJMXConnector(jmxc);
            clusterModel.refreshCachedCredentials();
          } catch (IOException ioe) {
            reportConnectError(ioe);
          }
        }
      });
    }
  }

  /**
   * Messaged by ConnectDialog.
   */
  public void handleException() {
    Exception e = connectDialog.getError();
    if (e != null) {
      clusterModel.clearConnectionCredentials();
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
    prefs.put(NAME, getClusterName());
    prefs.put(HOST, getHost());
    prefs.putInt(PORT, getPort());
    prefs.putBoolean(AUTO_CONNECT, isAutoConnect());
  }

  private void rename() {
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, clusterPanel);
    String name = (String) JOptionPane.showInputDialog(clusterPanel, "Enter new name:", frame.getTitle(),
                                                       JOptionPane.QUESTION_MESSAGE, null, null, getLabel());
    if (name != null) {
      setClusterName(name);
      adminClientContext.getAdminClientController().updateServerPrefs();
    }
  }

  private class RenameAction extends XAbstractAction {
    RenameAction() {
      super(adminClientContext.getString("rename.label"));
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      rename();
    }
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
    if (addingChildren.getAndSet(true)) { return; }

    try {
      if (getChildCount() == 0) {
        addChildren();
        AdminClientController controller = adminClientContext.getAdminClientController();
        nodeStructureChanged();
        controller.expand(this);
        controller.expand(topologyNode);
        if (featuresNode.getParent() != null) {
          controller.expand(featuresNode);
        }
      }
    } catch (Throwable t) {
      adminClientContext.log(t);
    } finally {
      addingChildren.set(false);
    }
  }

  TopologyNode topologyNode;

  protected void addChildren() {
    featuresNode = createFeaturesNode();
    add(createMonitoringNode());
    add(topologyNode = createTopologyNode());
    add(createPlatformNode());
  }

  private FeaturesNode featuresNode;

  protected FeaturesNode createFeaturesNode() {
    return new FeaturesNode(this, adminClientContext, clusterModel);
  }

  protected PlatformNode createPlatformNode() {
    return new PlatformNode(this, adminClientContext, clusterModel);
  }

  protected MonitoringNode createMonitoringNode() {
    return new MonitoringNode(this, adminClientContext, clusterModel);
  }

  protected TopologyNode createTopologyNode() {
    return new TopologyNode(adminClientContext, clusterModel);
  }

  void handleStarting() {
    nodeChanged();
    tryAddChildren();
    clusterPanel.started();
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.addPropertyChangeListener(ClusterNode.this);
    }
  }

  void handlePassiveUninitialized() {
    nodeChanged();
    tryAddChildren();
    clusterPanel.passiveUninitialized();
  }

  void handlePassiveStandby() {
    nodeChanged();
    tryAddChildren();
    clusterPanel.passiveStandby();
  }

  void handleActivation() {
    nodeChanged();
    tryAddChildren();
    clusterPanel.activated();
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      if (activeCoord.isClusterStatsSupported()) {
        activeCoord.addClusterStatsListener(ClusterNode.this);
      }
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

  synchronized boolean recordingClusterStats() {
    return isRecordingClusterStats;
  }

  synchronized boolean isProfilingLocks() {
    return isProfilingLocks;
  }

  boolean haveMonitoringActivity() {
    return recordingClusterStats() || isProfilingLocks();
  }

  private ScheduledExecutorService scheduledExecutor;
  private ScheduledFuture<?>       monitoringTaskFuture;
  private Runnable                 monitoringActivityTask;
  private static final long        MONITORING_FEEDBACK_MILLIS = 750;

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

  public synchronized void showRecordingStats(boolean recordingStats) {
    isRecordingClusterStats = recordingStats;
    testStartMonitoringTask();
  }

  public synchronized void showProfilingLocks(boolean profilingLocks) {
    isProfilingLocks = profilingLocks;
    testStartMonitoringTask();
  }

  void handleDisconnect() {
    if (getChildCount() > 0) {
      testStopMonitoringTask();
      adminClientContext.getAdminClientController().select(this);

      if (featuresNode != null) {
        featuresNode.setTearDown(true);
        if (featuresNode.getParent() == null) {
          featuresNode.tearDown();
        }
      }
      featuresNode = null;

      tearDownChildren();
      nodeStructureChanged();
      clusterPanel.disconnected();
      versionCheckOccurred.set(false);
    }
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

  public ClusterThreadDumpEntry takeClusterDump() {
    ClusterThreadDumpEntry tde = new ClusterThreadDumpEntry(adminClientContext);
    Map<IClusterNode, Future<String>> map = clusterModel.takeClusterDump();
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
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showRecordingStats(true);
      }
    });
  }

  public void sessionStopped(String sessionId) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showRecordingStats(false);
      }
    });
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

  @Override
  public void tearDown() {
    testStopMonitoringTask();

    Preferences prefs = adminClientContext.getPrefs().node(RuntimeStatsOption.NAME);
    prefs.removePreferenceChangeListener(this);

    if (connectDialog != null) {
      connectDialog.tearDown();
    }

    if (featuresNode != null) {
      featuresNode.setTearDown(true);
      if (featuresNode.getParent() == null) {
        featuresNode.tearDown();
      }
    }

    clusterModel.tearDown();

    super.tearDown();
  }
}
