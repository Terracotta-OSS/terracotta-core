/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.model.IServer;
import com.tc.management.beans.L2MBeanNames;
import com.tc.util.StringUtil;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

public class ServerPanel extends XContainer {
  private ApplicationContext appContext;
  private IServer            server;
  private ServerListener     serverListener;
  private XTabbedPane        tabbedPane;
  private StatusView         statusView;
  private XContainer         restartInfoItem;
  private PropertyTable      propertyTable;
  private XTextArea          environmentTextArea;
  private XTextArea          tcPropertiesTextArea;
  private XTextArea          processArgumentsTextArea;
  private XTextArea          configTextArea;
  private ServerLoggingPanel loggingPanel;

  public ServerPanel(ApplicationContext appContext, IServer server) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.server = server;

    tabbedPane = new XTabbedPane();

    /** Main **/
    XContainer mainPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    XContainer topPanel = new XContainer(new GridBagLayout());
    topPanel.add(statusView = new StatusView(), gbc);
    statusView.setText("Not connected");
    gbc.gridx++;

    // topPanel filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(new XLabel(), gbc);

    gbc.gridx = gbc.gridy = 0;
    mainPanel.add(topPanel, gbc);
    gbc.gridy++;
    gbc.weightx = 1.0;

    mainPanel.add(restartInfoItem = new XContainer(new BorderLayout()), gbc);
    gbc.gridy++;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;

    propertyTable = new PropertyTable();
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
    propertyTable.setDefaultRenderer(Long.class, renderer);
    propertyTable.setDefaultRenderer(Integer.class, renderer);
    mainPanel.add(new XScrollPane(propertyTable), gbc);

    XContainer mainPanelHolder = new XContainer(new BorderLayout());
    mainPanelHolder.add(mainPanel, BorderLayout.NORTH);

    tabbedPane.addTab(appContext.getString("node.main"), mainPanelHolder);

    /** Environment **/
    XContainer envPanel = new XContainer(new BorderLayout());
    environmentTextArea = new XTextArea();
    environmentTextArea.setEditable(false);
    environmentTextArea.setFont((Font) appContext.getObject("textarea.font"));
    envPanel.add(new XScrollPane(environmentTextArea));
    envPanel.add(new SearchPanel(appContext, environmentTextArea), BorderLayout.SOUTH);
    tabbedPane.addTab(appContext.getString("node.environment"), envPanel);

    /** TCProperies **/
    XContainer tcPropsPanel = new XContainer(new BorderLayout());
    tcPropertiesTextArea = new XTextArea();
    tcPropertiesTextArea.setEditable(false);
    tcPropertiesTextArea.setFont((Font) appContext.getObject("textarea.font"));
    tcPropsPanel.add(new XScrollPane(tcPropertiesTextArea));
    tcPropsPanel.add(new SearchPanel(appContext, tcPropertiesTextArea), BorderLayout.SOUTH);
    tabbedPane.addTab(appContext.getString("node.tcProperties"), tcPropsPanel);

    /** Process Arguments **/
    XContainer argsPanel = new XContainer(new BorderLayout());
    processArgumentsTextArea = new XTextArea();
    processArgumentsTextArea.setEditable(false);
    processArgumentsTextArea.setFont((Font) appContext.getObject("textarea.font"));
    argsPanel.add(new XScrollPane(processArgumentsTextArea));
    argsPanel.add(new SearchPanel(appContext, processArgumentsTextArea), BorderLayout.SOUTH);
    tabbedPane.addTab(appContext.getString("node.processArguments"), argsPanel);

    /** Config **/
    XContainer configPanel = new XContainer(new BorderLayout());
    configTextArea = new XTextArea();
    configTextArea.setEditable(false);
    configTextArea.setFont((Font) appContext.getObject("textarea.font"));
    configPanel.add(new XScrollPane(configTextArea));
    configPanel.add(new SearchPanel(appContext, configTextArea), BorderLayout.SOUTH);
    tabbedPane.addTab(appContext.getString("node.config"), configPanel);

    /** Logging **/
    loggingPanel = createLoggingPanel(appContext, server);
    if (loggingPanel != null) {
      tabbedPane.addTab(appContext.getString("node.logging.settings"), loggingPanel);
    }

    hideInfoContent();

    add(tabbedPane, BorderLayout.CENTER);

    serverListener = new ServerListener(server);
    serverListener.startListening();
  }

  synchronized IServer getServer() {
    return server;
  }

  synchronized ApplicationContext getApplicationContext() {
    return appContext;
  }

  protected ServerLoggingPanel createLoggingPanel(ApplicationContext theAppContext, IServer theServer) {
    return new ServerLoggingPanel(theAppContext, theServer);
  }

  protected class ServerListener extends AbstractServerListener {
    public ServerListener(IServer server) {
      super(server);
    }

    @Override
    protected void handleConnectError() {
      IServer theServer = getServer();
      if (theServer != null) {
        Exception e = theServer.getConnectError();
        String msg = e != null ? theServer.getConnectErrorMessage(e) : "unknown error";
        if (msg != null) {
          setConnectExceptionMessage(msg);
        }
      }
    }

    /**
     * The only differences between activated() and started() is the status message and the serverlog is only added in
     * activated() under the presumption that a non-active server won't be saying anything.
     */

    @Override
    protected void handleStarting() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext != null) {
        theAppContext.execute(new StartedWorker());
      }
    }

    @Override
    protected void handleActivation() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext != null) {
        theAppContext.execute(new ActivatedWorker());
      }
    }

    @Override
    protected void handlePassiveStandby() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext != null) {
        theAppContext.execute(new PassiveStandbyWorker());
      }
    }

    @Override
    protected void handlePassiveUninitialized() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext != null) {
        theAppContext.execute(new PassiveUninitializedWorker());
      }
    }

    @Override
    protected void handleDisconnected() {
      disconnected();
    }

  }

  protected void storePreferences() {
    ApplicationContext theAppContext = getApplicationContext();
    if (theAppContext != null) {
      theAppContext.storePrefs();
    }
  }

  private static class ServerState {
    private final Date     fStartDate;
    private final Date     fActivateDate;
    private final String   fEnvironment;
    private final String   fTCProperties;
    private final String[] fProcessArguments;
    private final String   fConfig;

    ServerState() {
      this(new Date(), new Date(), "", "", new String[] {}, "");
    }

    ServerState(Date startDate, Date activateDate, String environment, String tcProperties, String[] processArguments,
                String config) {
      fStartDate = startDate;
      fActivateDate = activateDate;
      fEnvironment = environment;
      fTCProperties = tcProperties;
      fProcessArguments = processArguments;
      fConfig = config;

    }

    Date getStartDate() {
      return fStartDate;
    }

    Date getActivateDate() {
      return fActivateDate;
    }

    String getEnvironment() {
      return fEnvironment;
    }

    String getTCProperties() {
      return fTCProperties;
    }

    String[] getProcessArguments() {
      return fProcessArguments;
    }

    String getConfig() {
      return fConfig;
    }

    private static long safeGetLong(Map<String, Object> values, String key) {
      Object val = values.get(key);
      if (val instanceof Long) { return ((Long) val).longValue(); }
      return 0;
    }

    private static String safeGetString(Map<String, Object> values, String key) {
      Object val = values.get(key);
      if (val != null) { return val.toString(); }
      return "";
    }

    private static String[] safeGetStringArray(Map<String, Object> values, String key) {
      Object val = values.get(key);
      if (val != null && val.getClass().isArray() && val.getClass().getComponentType() == String.class) { return (String[]) val; }
      return new String[] {};
    }
  }

  /**
   * TODO: grab all of these in one-shot.
   */
  private class ServerStateWorker extends BasicWorker<ServerState> {
    private ServerStateWorker() {
      super(new Callable<ServerState>() {
        public ServerState call() throws Exception {
          IServer theServer = getServer();
          if (theServer == null) throw new IllegalStateException("not connected");
          Map<ObjectName, Set<String>> request = new HashMap<ObjectName, Set<String>>();
          request.put(L2MBeanNames.TC_SERVER_INFO,
                      new HashSet(Arrays.asList(new String[] { "StartTime", "ActivateTime", "Environment",
                          "TCProperties", "ProcessArguments", "Config" })));
          Map<ObjectName, Map<String, Object>> result = theServer.getAttributeMap(request);
          Map<String, Object> values = result.get(L2MBeanNames.TC_SERVER_INFO);
          if (values != null) {
            Date startDate = new Date(ServerState.safeGetLong(values, "StartTime"));
            Date activateDate = new Date(ServerState.safeGetLong(values, "ActivateTime"));
            String environment = ServerState.safeGetString(values, "Environment");
            String tcProps = ServerState.safeGetString(values, "TCProperties");
            String[] args = ServerState.safeGetStringArray(values, "ProcessArguments");
            String config = ServerState.safeGetString(values, "Config");
            return new ServerState(startDate, activateDate, environment, tcProps, args, config);
          } else {
            return new ServerState();
          }
        }
      }, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          ApplicationContext theAppContext = getApplicationContext();
          if (theAppContext != null) {
            theAppContext.log(e);
          }
        }
      } else {
        if (!tabbedPane.isEnabled()) { // showInfoContent enables tabbedPane
          ServerState serverState = getResult();
          showInfoContent();
          environmentTextArea.setText(serverState.getEnvironment());
          tcPropertiesTextArea.setText(serverState.getTCProperties());
          processArgumentsTextArea.setText(StringUtil.toString(serverState.getProcessArguments(), "\n", null, null));
          configTextArea.setText(serverState.getConfig());
          if (loggingPanel != null) {
            loggingPanel.setupLoggingControls();
          }
        }
      }
    }
  }

  private class StartedWorker extends ServerStateWorker {
    @Override
    protected void finished() {
      IServer theServer = getServer();
      if (theServer == null) return;
      super.finished();
      if (getException() == null) {
        ServerState serverState = getResult();
        String startTime = serverState.getStartDate().toString();
        setStatusLabel(appContext.format("server.started.label", startTime));
        appContext.setStatus(appContext.format("server.started.status", theServer, startTime));
      } else {
        appContext.log(getException());
      }
    }
  }

  private class ActivatedWorker extends ServerStateWorker {
    @Override
    protected void finished() {
      IServer theServer = getServer();
      if (theServer == null) return;
      super.finished();
      if (getException() == null) {
        ServerState serverState = getResult();
        String activateTime = serverState.getActivateDate().toString();
        setStatusLabel(appContext.format("server.activated.label", activateTime));
        appContext.setStatus(appContext.format("server.activated.status", theServer, activateTime));
      } else {
        appContext.log(getException());
      }
    }
  }

  private class PassiveUninitializedWorker extends ServerStateWorker {
    @Override
    protected void finished() {
      IServer theServer = getServer();
      if (theServer == null) return;
      super.finished();
      if (getException() == null) {
        String startTime = new Date().toString();
        setStatusLabel(appContext.format("server.initializing.label", startTime));
        appContext.setStatus(appContext.format("server.initializing.status", theServer, startTime));
      }
    }
  }

  private class PassiveStandbyWorker extends ServerStateWorker {
    @Override
    protected void finished() {
      IServer theServer = getServer();
      if (theServer == null) return;
      super.finished();
      if (getException() == null) {
        String startTime = new Date().toString();
        setStatusLabel(appContext.format("server.standingby.label", startTime));
        appContext.setStatus(appContext.format("server.standingby.status", theServer, startTime));
      }
    }
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    if (appContext != null) {
      appContext.execute(new StartedWorker());
    }
  }

  void activated() {
    if (appContext != null) {
      appContext.execute(new ActivatedWorker());
    }
  }

  void passiveUninitialized() {
    if (appContext != null) {
      appContext.execute(new PassiveUninitializedWorker());
    }
  }

  void passiveStandby() {
    if (appContext != null) {
      appContext.execute(new PassiveStandbyWorker());
    }
  }

  private void testShowRestartInfoItem() {
    IServer theServer = getServer();
    if (theServer == null) return;
    if (!theServer.getPersistenceMode().equals("permanent-store")) {
      String warning = appContext.getString("server.non-restartable.warning");
      restartInfoItem.add(new PersistenceModeWarningPanel(appContext, warning));
    } else {
      restartInfoItem.removeAll();
    }
  }

  protected void showInfoContent() {
    testShowRestartInfoItem();
    showProductInfo();
  }

  protected void hideInfoContent() {
    hideProductInfo();
    hideRestartInfo();
  }

  private void hideRestartInfo() {
    restartInfoItem.removeAll();
    restartInfoItem.revalidate();
    restartInfoItem.repaint();
  }

  void disconnected() {
    IServer theServer = getServer();
    if (theServer == null) return;
    String startTime = new Date().toString();
    setStatusLabel(appContext.format("server.disconnected.label", startTime));
    appContext.setStatus(appContext.format("server.disconnected.status", theServer, startTime));
    hideInfoContent();
  }

  private void setTabbedPaneEnabled(boolean enabled) {
    tabbedPane.setEnabled(enabled);
    int tabCount = tabbedPane.getTabCount();
    for (int i = 0; i < tabCount; i++) {
      tabbedPane.setEnabledAt(i, enabled);
    }
    tabbedPane.setSelectedIndex(0);
  }

  void setConnectExceptionMessage(String msg) {
    IServer theServer = getServer();
    if (theServer != null) {
      setStatusLabel(msg);
      setTabbedPaneEnabled(false);
    }
  }

  void setStatusLabel(String text) {
    IServer theServer = getServer();
    if (theServer == null) return;
    statusView.setText(text);
    ServerHelper.getHelper().setStatusView(theServer, statusView);
    statusView.revalidate();
    statusView.repaint();
  }

  /**
   * The fields listed below are on IServer. If those methods change, so will these fields need to change. PropertyTable
   * uses reflection to access values to display. TODO: i18n
   */
  private void showProductInfo() {
    String[] fields = { "Name", "CanonicalHostName", "HostAddress", "Port", "DSOListenPort", "ProductVersion",
        "ProductBuildID", "ProductLicense", "PersistenceMode", "FailoverMode" };
    String[] headings = { "Name", "Host", "Address", "JMX port", "DSO port", "Version", "Build", "License",
        "Persistence mode", "Failover mode" };
    List<String> fieldList = new ArrayList(Arrays.asList(fields));
    List<String> headingList = new ArrayList(Arrays.asList(headings));
    String patch = server.getProductPatchLevel();
    if (patch != null && patch.length() > 0) {
      fieldList.add(fieldList.indexOf("ProductLicense"), "ProductPatchVersion");
      headingList.add(headingList.indexOf("License"), "Patch");
    }
    fields = fieldList.toArray(new String[fieldList.size()]);
    headings = headingList.toArray(new String[headingList.size()]);
    propertyTable.setModel(new PropertyTableModel(server, fields, headings));
    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, propertyTable);
    if (scrollPane != null) {
      scrollPane.setVisible(true);
    }

    setTabbedPaneEnabled(true);

    revalidate();
    repaint();
  }

  private void hideProductInfo() {
    propertyTable.setModel(new PropertyTableModel());
    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, propertyTable);
    if (scrollPane != null) {
      scrollPane.setVisible(false);
    }
    tabbedPane.setSelectedIndex(0);
    tabbedPane.setEnabled(false);

    revalidate();
    repaint();
  }

  @Override
  public synchronized void tearDown() {
    server.removePropertyChangeListener(serverListener);
    serverListener.tearDown();
    statusView.tearDown();

    super.tearDown();

    appContext = null;
    server = null;
    serverListener = null;
    propertyTable = null;
    statusView = null;
    tabbedPane = null;
    environmentTextArea = null;
    tcPropertiesTextArea = null;
    processArgumentsTextArea = null;
    configTextArea = null;
    loggingPanel = null;
  }
}
