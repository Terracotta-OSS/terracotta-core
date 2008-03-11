/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.AbstractTreeCellRenderer;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.DSOHelper;
import com.tc.config.schema.L2Info;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.stats.DSOMBean;

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
import java.util.ArrayList;

import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.generic.ConnectionClosedException;
import javax.naming.CommunicationException;
import javax.naming.ServiceUnavailableException;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * All connection actions go through the ServerConnectionManager, which calls back through ConnectionListener. The
 * ServerConnectionManager handles auto-connecting, active-connection monitoring, and connection-state messaging. A JMX
 * notification handler (handleNotification) informs when the server goes from started->active state.
 */

public class ServerNode extends ComponentNode implements ConnectionListener, NotificationListener {
  protected AdminClientContext      m_acc;
  protected ServerConnectionManager m_connectManager;
  protected String                  m_host;
  protected int                     m_jmxPort;
  protected Integer                 m_dsoPort;
  protected Exception               m_connectException;
  protected TCServerInfoMBean       m_serverInfoBean;
  protected ProductInfo             m_productInfo;
  protected ServerPanel             m_serverPanel;
  protected ConnectDialog           m_connectDialog;
  protected JDialog                 m_versionMismatchDialog;
  protected JPopupMenu              m_popupMenu;

  ServerNode() {
    this(ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT, ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  ServerNode(final String host, final int jmxPort, final boolean autoConnect) {
    super();

    m_acc = AdminClient.getContext();
    m_host = host;
    m_jmxPort = jmxPort;

    setRenderer(new ServerNodeTreeCellRenderer());
    initMenu(autoConnect);
    setComponent(m_serverPanel = createServerPanel());
    AutoConnectionListener acl = new AutoConnectionListener();
    m_connectManager = new ServerConnectionManager(host, jmxPort, autoConnect, acl);
    if (autoConnect) {
      String[] creds = ServerConnectionManager.getCachedCredentials(m_connectManager);
      if (creds != null) {
        m_connectManager.setCredentials(creds[0], creds[1]);
      }
    }
  }

  protected ServerPanel createServerPanel() {
    return new ServerPanel(this);
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

  ConnectionContext getConnectionContext() {
    return m_connectManager.getConnectionContext();
  }

  void setHost(String host) {
    m_connectManager.setHostname(m_host = host);
  }

  public String getHost() {
    return m_host;
  }

  void setPort(int port) {
    m_connectManager.setJMXPortNumber(m_jmxPort = port);
  }

  public int getPort() {
    return m_jmxPort;
  }

  public Integer getDSOListenPort() {
    if (m_dsoPort != null) return m_dsoPort;
    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();
      m_dsoPort = serverInfo != null ? serverInfo.getDSOListenPort() : -1;
    } catch (Exception e) {
      // connection was probably dropped.  Wait for disconnect message.
      AdminClient.getContext().log(e);
    }
    return m_dsoPort;
  }

  private void testGetDSOListenPort() {
    if (m_dsoPort == null) getDSOListenPort();
  }

  String getStatsExportServletURI(String sessionId) throws Exception {
    Integer dsoPort = getDSOListenPort();
    Object[] args = new Object[] { getHost(), dsoPort.toString(), sessionId };
    return MessageFormat.format("http://{0}:{1}/stats-export?session={2}", args);
  }

  protected void initMenu(boolean autoConnect) {
    m_popupMenu = new JPopupMenu("Server Actions");

  }

  void setVersionMismatchDialog(JDialog dialog) {
    m_versionMismatchDialog = dialog;
  }

  private void setConnected(boolean connected) {
    if (m_acc == null) { return; }
    if (connected) {
      if (m_versionMismatchDialog != null) return;
      if (!m_serverPanel.isProductInfoShowing() && !m_acc.controller.testServerMatch(this)) {
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
      Frame frame = (Frame) m_serverPanel.getAncestorOfClass(java.awt.Frame.class);
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
    Frame frame = (Frame) m_serverPanel.getAncestorOfClass(java.awt.Frame.class);

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
      m_serverPanel.setConnectExceptionMessage(msg);
    }
    m_acc.controller.nodeChanged(ServerNode.this);
  }

  void disconnect() {
    String msg = m_acc.getMessage("disconnecting.from");
    MessageFormat form = new MessageFormat(msg);
    Object[] args = new Object[] { this };

    m_acc.controller.setStatus(form.format(args));
    m_connectManager.setAutoConnect(false);
    m_acc.controller.updateServerPrefs();
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
    AdminClientPanel topPanel = (AdminClientPanel) m_serverPanel.getAncestorOfClass(AdminClientPanel.class);
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
      if (serverInfo != null) {
        serverInfo.shutdown();
      }
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

  public String toString() {
    return m_connectManager != null ? m_connectManager.toString() : "";
  }

  private class ShutdownAction extends XAbstractAction implements Runnable {
    ShutdownAction() {
      super("Shutdown", ServersHelper.getHelper().getShutdownIcon());

      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_SHORTCUT_KEY_MASK, true));
      setEnabled(false);
    }

    public void run() {
      shutdown();
    }

    public void actionPerformed(ActionEvent ae) {
      SwingUtilities.invokeLater(this);
    }
  }

  void handleStarting() {
    testGetDSOListenPort();
    m_acc.controller.nodeChanged(ServerNode.this);
    m_serverPanel.started();
  }

  DSOMBean getDSOBean() {
    try {
      ConnectionContext cc = getConnectionContext();
      ObjectName objectName = DSOHelper.getHelper().getDSOMBean(cc);
      if (objectName == null) return null;
      return (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, objectName, DSOMBean.class, true);
    } catch (Exception ioe) {
      return null;
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
      m_acc.controller.nodeChanged(ServerNode.this);
    } catch (Exception e) {
      m_acc.log(e);
    } finally {
      m_tryAddingChildren.set(false);
    }
  }

  private void addChildren(ConnectionContext cc) throws Exception {
    // There are no longer any children.  Leaving in case someone changes their mind.
  }

  void handlePassiveUninitialized() {
    testGetDSOListenPort();
    try {
      tryAddChildren();
      m_serverPanel.passiveUninitialized();
    } catch (Exception e) {
      // just wait for disconnect message to come in
    }
  }

  void handlePassiveStandby() {
    testGetDSOListenPort();
    try {
      tryAddChildren();
      m_serverPanel.passiveStandby();
    } catch (Exception e) {
      // just wait for disconnect message to come in
    }
  }

  void handleActivation() {
    testGetDSOListenPort();
    try {
      tryAddChildren();
      m_serverPanel.activated();
    } catch (Exception e) {
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
                m_acc.controller.nodeStructureChanged(ServerNode.this);
                m_acc.controller.expand(ServerNode.this);
                m_acc.controller.nodeChanged(ServerNode.this);
              } catch (Exception e) {
                // just wait for disconnect message to come in
              }
            }
          });
        }
      }
    }
  }

  StatisticsManagerMBean getStatisticsManagerMBean() {
    ConnectionContext cc = getConnectionContext();
    return (StatisticsManagerMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, StatisticsMBeanNames.STATISTICS_MANAGER, StatisticsManagerMBean.class, false);
  }

  StatisticsEmitterMBean registerStatisticsEmitterListener(NotificationListener listener) {
    ConnectionContext cc = getConnectionContext();
    StatisticsEmitterMBean stat_emitter = (StatisticsEmitterMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, StatisticsMBeanNames.STATISTICS_EMITTER, StatisticsEmitterMBean.class, false);
    ArrayList dataList = new ArrayList();
    try {
      cc.mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_EMITTER, listener, null, dataList);
    } catch (Exception e) {
      throw new RuntimeException("Registering stats emitter listener", e);
    }
    return stat_emitter;
  }

  L2Info[] getClusterMembers() {
    L2Info[] result = null;

    try {
      TCServerInfoMBean serverInfo = getServerInfoBean();
      result = serverInfo.getL2Info();
    } catch (Exception e) {
      m_acc.log(e);
    }

    return result != null ? result : new L2Info[0];
  }

  private void resetBeanProxies() {
    m_serverInfoBean = null;
    m_productInfo = null;
  }

  void handleDisconnect() {
    resetBeanProxies();

    for (int i = getChildCount() - 1; i >= 0; i--) {
      ((XTreeNode) getChildAt(i)).tearDown();
      remove(i);
    }

    m_serverPanel.disconnected();
    m_acc.controller.nodeStructureChanged(ServerNode.this);
    m_acc.controller.select(this);
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
      m_statusView.setIndicator(ServerNode.this.getServerStatusColor());
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

    super.tearDown();
  }
}
