/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.SearchPanel;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModelElement;

import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

public class ClientPanel extends XContainer implements PropertyChangeListener {
  protected ApplicationContext appContext;
  protected IClient            client;

  protected XTabbedPane        tabbedPane;
  protected XContainer         controlArea;
  protected PropertyTable      propertyTable;
  protected XTextArea          environmentTextArea;
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
      try {
        setupTunneledBeans();
      } catch (Exception e) {
        appContext.log(e);
      }
    } else {
      client.addPropertyChangeListener(this);
    }
  }

  public IClient getClient() {
    return client;
  }

  private void setupTunneledBeans() throws Exception {
    environmentTextArea.setText(client.getEnvironment());
    configTextArea.setText(client.getConfig());
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModelElement.PROP_READY.equals(prop)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            setupTunneledBeans();
          } catch (Exception e) {
            appContext.log(e);
          }
        }
      });
    }
  }

  @Override
  public void tearDown() {
    super.tearDown();

    appContext = null;
    client = null;
    propertyTable = null;
    environmentTextArea = null;
    configTextArea = null;
    controlArea = null;
  }
}
