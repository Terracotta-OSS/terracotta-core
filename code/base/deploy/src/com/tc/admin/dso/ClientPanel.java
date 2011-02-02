/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.SearchPanel;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.IServer;
import com.tc.util.StringUtil;

import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import javax.swing.table.DefaultTableCellRenderer;

public class ClientPanel extends XContainer implements PropertyChangeListener {
  protected ApplicationContext appContext;
  protected IClient            client;

  protected XTabbedPane        tabbedPane;
  protected XContainer         controlArea;
  protected PropertyTable      propertyTable;
  protected XTextArea          environmentTextArea;
  protected XTextArea          tcPropertiesTextArea;
  protected XTextArea          processArgumentsTextArea;
  protected XTextArea          configTextArea;
  protected ClientLoggingPanel loggingPanel;

  public ClientPanel(ApplicationContext appContext, IClient client) {
    super(new BorderLayout());

    this.appContext = appContext;

    tabbedPane = new XTabbedPane();

    /** Main **/
    XContainer mainPanel = new XContainer(new BorderLayout());
    mainPanel.add(controlArea = new XContainer(), BorderLayout.NORTH);
    propertyTable = new PropertyTable();
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
    propertyTable.setDefaultRenderer(Long.class, renderer);
    propertyTable.setDefaultRenderer(Integer.class, renderer);
    mainPanel.add(new XScrollPane(propertyTable), BorderLayout.CENTER);
    tabbedPane.addTab(appContext.getString("node.main"), mainPanel);

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
    loggingPanel = createLoggingPanel(appContext, client);
    if (loggingPanel != null) {
      tabbedPane.addTab(appContext.getString("node.logging.settings"), loggingPanel);
    }

    add(tabbedPane, BorderLayout.CENTER);

    setTabbedPaneEnabled(false);
    setClient(client);
  }

  protected ClientLoggingPanel createLoggingPanel(ApplicationContext theAppContext, IClient theClient) {
    return new ClientLoggingPanel(theAppContext, theClient);
  }

  public void setClient(IClient client) {
    this.client = client;

    String patchLevel = client.getProductPatchLevel();
    String[] fields = { "Host", "Port", "ChannelID", "ProductVersion", "ProductBuildID" };
    List<String> fieldList = new ArrayList(Arrays.asList(fields));
    String[] headings = { "Host", "Port", "Client ID", "Version", "Build" };
    List<String> headingList = new ArrayList(Arrays.asList(headings));
    if (patchLevel != null && patchLevel.length() > 0) {
      fieldList.add("ProductPatchVersion");
      headingList.add("Patch");
    }
    fields = fieldList.toArray(new String[fieldList.size()]);
    headings = headingList.toArray(new String[headingList.size()]);
    propertyTable.setModel(new PropertyTableModel(client, fields, headings));

    if (client.isReady()) {
      initClientState();
    } else {
      client.addPropertyChangeListener(this);
    }
  }

  public IClient getClient() {
    return client;
  }

  private static class ClientState {
    private final String   fEnvironment;
    private final String   fTCProperties;
    private final String[] fProcessArguments;
    private final String   fConfig;

    ClientState() {
      this("", "", new String[] {}, "");
    }

    ClientState(String environment, String tcProperties, String[] processArguments, String config) {
      fEnvironment = environment;
      fTCProperties = tcProperties;
      fProcessArguments = processArguments;
      fConfig = config;

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

  private class ClientStateWorker extends BasicWorker<ClientState> {
    private ClientStateWorker() {
      super(new Callable<ClientState>() {
        public ClientState call() throws Exception {
          IClient theClient = getClient();
          IServer theServer = theClient.getClusterModel().getActiveCoordinator();
          if (theServer == null) throw new IllegalStateException("not connected");
          Map<ObjectName, Set<String>> request = new HashMap<ObjectName, Set<String>>();
          ObjectName l1InfoBeanName = theClient.getL1InfoBeanName();
          request.put(l1InfoBeanName,
                      new HashSet(Arrays.asList(new String[] { "Environment", "TCProperties", "ProcessArguments",
                          "Config" })));
          Map<ObjectName, Map<String, Object>> result = theServer.getAttributeMap(request);
          Map<String, Object> values = result.get(l1InfoBeanName);
          if (values != null) {
            String environment = ClientState.safeGetString(values, "Environment");
            String tcProps = ClientState.safeGetString(values, "TCProperties");
            String[] args = ClientState.safeGetStringArray(values, "ProcessArguments");
            String config = ClientState.safeGetString(values, "Config");
            return new ClientState(environment, tcProps, args, config);
          } else {
            return new ClientState();
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
          if (appContext != null) {
            appContext.log(e);
          }
        }
      } else {
        if (!tabbedPane.isEnabled()) {
          ClientState serverState = getResult();
          environmentTextArea.setText(serverState.getEnvironment());
          tcPropertiesTextArea.setText(serverState.getTCProperties());
          processArgumentsTextArea.setText(StringUtil.toString(serverState.getProcessArguments(), "\n", null, null));
          configTextArea.setText(serverState.getConfig());
          setTabbedPaneEnabled(true);
        }
      }
    }
  }

  private void initClientState() {
    appContext.submit(new ClientStateWorker());
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModelElement.PROP_READY.equals(prop)) {
      initClientState();
    }
  }

  private void setTabbedPaneEnabled(boolean enabled) {
    tabbedPane.setEnabled(enabled);
    int tabCount = tabbedPane.getTabCount();
    for (int i = 0; i < tabCount; i++) {
      tabbedPane.setEnabledAt(i, enabled);
    }
    tabbedPane.setSelectedIndex(0);
  }

  @Override
  public void tearDown() {
    super.tearDown();

    appContext = null;
    client = null;
    propertyTable = null;
    environmentTextArea = null;
    tcPropertiesTextArea = null;
    processArgumentsTextArea = null;
    configTextArea = null;
    controlArea = null;
  }
}
