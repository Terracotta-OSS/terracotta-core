/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.editors.xmlbeans.XmlIntegerField;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.dso.editors.xmlbeans.XmlStringField;
import org.terracotta.ui.util.SWTLayout;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.Server;

public class ServerPanel extends ConfigurationEditorPanel implements XmlObjectStructureListener {
  private Server                           m_server;
  private Layout                           m_layout;

  private LogsBrowseSelectionHandler       m_logsBrowseSelectionHandler;
  private DataBrowseSelectionHandler       m_dataBrowseSelectionHandler;
  private StatisticsBrowseSelectionHandler m_statisticsBrowseSelectionHandler;

  public ServerPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);

    m_logsBrowseSelectionHandler = new LogsBrowseSelectionHandler();
    m_dataBrowseSelectionHandler = new DataBrowseSelectionHandler();
    m_statisticsBrowseSelectionHandler = new StatisticsBrowseSelectionHandler();

    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    fireChanged();
  }

  void fireChanged() {
    ServersPanel serversPanel = (ServersPanel) SWTUtil.getAncestorOfClass(ServersPanel.class, this);
    serversPanel.fireServerChanged(m_server);
  }

  public boolean hasAnySet() {
    return m_server != null
           && (m_server.isSetData() || m_server.isSetLogs() || m_server.isSetStatistics() || m_server.isSetDsoPort()
               || m_server.isSetDso() || m_server.isSetJmxPort() || m_server.isSetAuthentication()
               || m_server.isSetName() || m_server.isSetHost() || m_server.isSetL2GroupPort());
  }

  private void addListeners() {
    ((XmlStringField) m_layout.m_nameField.getData()).addXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_hostField.getData()).addXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_dsoPortField.getData()).addXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_jmxPortField.getData()).addXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_dataLocation.getData()).addXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_logsLocation.getData()).addXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_statisticsLocation.getData()).addXmlObjectStructureListener(this);
    m_layout.m_dataBrowse.addSelectionListener(m_dataBrowseSelectionHandler);
    m_layout.m_logsBrowse.addSelectionListener(m_logsBrowseSelectionHandler);
    m_layout.m_statisticsBrowse.addSelectionListener(m_statisticsBrowseSelectionHandler);
    m_layout.m_dsoServerDataPanel.addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    ((XmlStringField) m_layout.m_nameField.getData()).removeXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_hostField.getData()).removeXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_dsoPortField.getData()).removeXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_jmxPortField.getData()).removeXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_dataLocation.getData()).removeXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_logsLocation.getData()).removeXmlObjectStructureListener(this);
    ((XmlStringField) m_layout.m_statisticsLocation.getData()).removeXmlObjectStructureListener(this);
    m_layout.m_dataBrowse.removeSelectionListener(m_dataBrowseSelectionHandler);
    m_layout.m_logsBrowse.removeSelectionListener(m_logsBrowseSelectionHandler);
    m_layout.m_statisticsBrowse.removeSelectionListener(m_statisticsBrowseSelectionHandler);
    m_layout.m_dsoServerDataPanel.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_layout.setServer(m_server);
  }

  public void setup(Server server) {
    setEnabled(true);
    removeListeners();

    if ((m_server = server) != null) {
      updateChildren();
      addListeners();
    } else {
      setEnabled(false);
    }
  }

  public void tearDown() {
    removeListeners();
    m_server = null;
    m_layout.tearDown();
    setEnabled(false);
  }

  private class Layout implements SWTLayout {
    private static final String BROWSE     = "Browse...";
    private static final String NAME       = "Name";
    private static final String HOST       = "Host";
    private static final String DSO_PORT   = "DSO Port";
    private static final String JMX_PORT   = "JMX Port";
    private static final String SERVER     = "Server";
    private static final String DATA       = "Data";
    private static final String LOGS       = "Logs";
    private static final String STATISTICS = "Statistics";

    private Text                m_nameField;
    private Text                m_hostField;
    private Text                m_dsoPortField;
    private Text                m_jmxPortField;
    private Text                m_dataLocation;
    private Text                m_logsLocation;
    private Text                m_statisticsLocation;
    private Button              m_logsBrowse;
    private Button              m_dataBrowse;
    private Button              m_statisticsBrowse;
    private Group               m_serverGroup;
    private DsoServerDataPanel  m_dsoServerDataPanel;

    public void reset() {
      resetServerFields(false);
    }

    void tearDown() {
      ((XmlStringField) m_nameField.getData()).tearDown();
      ((XmlStringField) m_hostField.getData()).tearDown();
      ((XmlIntegerField) m_dsoPortField.getData()).tearDown();
      ((XmlIntegerField) m_jmxPortField.getData()).tearDown();
      ((XmlStringField) m_dataLocation.getData()).tearDown();
      ((XmlStringField) m_logsLocation.getData()).tearDown();
      ((XmlStringField) m_statisticsLocation.getData()).tearDown();

      m_dsoServerDataPanel.tearDown();
    }

    private void setServer(Server server) {
      ((XmlStringField) m_nameField.getData()).setup(server);
      ((XmlStringField) m_hostField.getData()).setup(server);
      ((XmlIntegerField) m_dsoPortField.getData()).setup(server);
      ((XmlIntegerField) m_jmxPortField.getData()).setup(server);
      ((XmlStringField) m_dataLocation.getData()).setup(server);
      ((XmlStringField) m_logsLocation.getData()).setup(server);
      ((XmlStringField) m_statisticsLocation.getData()).setup(server);

      m_dsoServerDataPanel.setup(server);
    }

    private void resetServerFields(boolean enabled) {
      m_nameField.setText("");
      m_nameField.setEnabled(enabled);

      m_hostField.setText("");
      m_hostField.setEnabled(enabled);

      m_dsoPortField.setText("");
      m_dsoPortField.setEnabled(enabled);

      m_jmxPortField.setText("");
      m_jmxPortField.setEnabled(enabled);

      m_dataLocation.setText("");
      m_dataLocation.setEnabled(enabled);
      m_dataBrowse.setEnabled(enabled);

      m_logsLocation.setText("");
      m_logsLocation.setEnabled(enabled);
      m_logsBrowse.setEnabled(enabled);

      m_statisticsLocation.setText("");
      m_statisticsLocation.setEnabled(enabled);
      m_statisticsBrowse.setEnabled(enabled);
    }

    private Layout(Composite parent) {
      GridLayout gridLayout = new GridLayout();
      parent.setLayout(gridLayout);

      m_serverGroup = new Group(parent, SWT.SHADOW_NONE);
      m_serverGroup.setText(SERVER);
      m_serverGroup.setLayout(new GridLayout(2, true));
      m_serverGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite fieldGroup = new Composite(m_serverGroup, SWT.NONE);
      gridLayout = new GridLayout(5, false);
      fieldGroup.setLayout(gridLayout);
      fieldGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true));

      Label nameLabel = new Label(fieldGroup, SWT.NONE);
      nameLabel.setText(NAME);
      m_nameField = new Text(fieldGroup, SWT.BORDER);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.minimumWidth = SWTUtil.textColumnsToPixels(m_nameField, 10);
      m_nameField.setLayoutData(gridData);
      initStringField(m_nameField, Server.class, "name");

      new Label(fieldGroup, SWT.NONE); // space

      Label dsoPortLabel = new Label(fieldGroup, SWT.NONE);
      dsoPortLabel.setText(DSO_PORT);
      m_dsoPortField = new Text(fieldGroup, SWT.BORDER);
      SWTUtil.makeIntField(m_dsoPortField);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.minimumWidth = SWTUtil.textColumnsToPixels(m_dsoPortField, 6);
      m_dsoPortField.setLayoutData(gridData);
      initIntegerField(m_dsoPortField, Server.class, "dso-port");

      Label hostLabel = new Label(fieldGroup, SWT.NONE);
      hostLabel.setText(HOST);
      m_hostField = new Text(fieldGroup, SWT.BORDER);
      m_hostField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      initStringField(m_hostField, Server.class, "host");

      new Label(fieldGroup, SWT.NONE); // space

      Label jmxPortLabel = new Label(fieldGroup, SWT.NONE);
      jmxPortLabel.setText(JMX_PORT);
      m_jmxPortField = new Text(fieldGroup, SWT.BORDER);
      SWTUtil.makeIntField(m_jmxPortField);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.minimumWidth = SWTUtil.textColumnsToPixels(m_jmxPortField, 6);
      m_jmxPortField.setLayoutData(gridData);
      initIntegerField(m_jmxPortField, Server.class, "jmx-port");

      Label dataLabel = new Label(fieldGroup, SWT.NONE);
      dataLabel.setText(DATA);

      Composite dataPanel = new Composite(fieldGroup, SWT.NONE);
      gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      dataPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 4;
      dataPanel.setLayoutData(gridData);

      m_dataLocation = new Text(dataPanel, SWT.BORDER);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      m_dataLocation.setLayoutData(gridData);
      initStringField(m_dataLocation, Server.class, "data");

      m_dataBrowse = new Button(dataPanel, SWT.PUSH);
      m_dataBrowse.setText(BROWSE);
      SWTUtil.applyDefaultButtonSize(m_dataBrowse);

      Label logsLabel = new Label(fieldGroup, SWT.NONE);
      logsLabel.setText(LOGS);

      Composite logsPanel = new Composite(fieldGroup, SWT.NONE);
      gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      logsPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 4;
      logsPanel.setLayoutData(gridData);

      m_logsLocation = new Text(logsPanel, SWT.BORDER);
      m_logsLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      initStringField(m_logsLocation, Server.class, "logs");

      m_logsBrowse = new Button(logsPanel, SWT.PUSH);
      m_logsBrowse.setText(BROWSE);
      SWTUtil.applyDefaultButtonSize(m_logsBrowse);

      Label statisticsLabel = new Label(fieldGroup, SWT.NONE);
      statisticsLabel.setText(STATISTICS);

      Composite statisticsPanel = new Composite(fieldGroup, SWT.NONE);
      gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      statisticsPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 4;
      statisticsPanel.setLayoutData(gridData);

      m_statisticsLocation = new Text(statisticsPanel, SWT.BORDER);
      m_statisticsLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      initStringField(m_statisticsLocation, Server.class, "statistics");

      m_statisticsBrowse = new Button(statisticsPanel, SWT.PUSH);
      m_statisticsBrowse.setText(BROWSE);
      SWTUtil.applyDefaultButtonSize(m_statisticsBrowse);

      m_dsoServerDataPanel = new DsoServerDataPanel(m_serverGroup, SWT.NONE);
      m_dsoServerDataPanel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true));
    }
  }

  private class LogsBrowseSelectionHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      IFolder folder = SWTUtil.openSelectFolderDialog(getProject(), "Select logs folder",
                                                      "Choose a folder for the server log area");
      if (folder != null) {
        setStringField(m_layout.m_logsLocation, folder.getProjectRelativePath().toString());
      }
    }
  }

  private class DataBrowseSelectionHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      IFolder folder = SWTUtil.openSelectFolderDialog(getProject(), "Select data folder",
                                                      "Choose a folder for the server database area");
      if (folder != null) {
        setStringField(m_layout.m_dataLocation, folder.getProjectRelativePath().toString());
      }
    }
  }

  private class StatisticsBrowseSelectionHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      IFolder folder = SWTUtil.openSelectFolderDialog(getProject(), "Select statistics folder",
                                                      "Choose a folder for the server statistics database area");
      if (folder != null) {
        setStringField(m_layout.m_statisticsLocation, folder.getProjectRelativePath().toString());
      }
    }
  }
}
