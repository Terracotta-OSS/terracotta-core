/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlObjectHolderHelper;
import org.terracotta.ui.util.SWTLayout;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.BindPort;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class ServersPanel extends ConfigurationEditorPanel
  implements ConfigurationEditorRoot
{
  private IProject              m_project;
  private TcConfig              m_config;
  private Servers               m_servers;

  private Layout                m_layout;

  private TableSelectionHandler m_tableSelectionHandler;
  private AddServerHandler      m_addServerHandler;
  private RemoveServerHandler   m_removeServerHandler;

  private static final int      NAME_INDEX     = 0;
  private static final int      HOST_INDEX     = 1;
  private static final int      DSO_PORT_INDEX = 2;
  private static final int      JMX_PORT_INDEX = 3;

  private static int DEFAULT_JMX_PORT;
  private static int DEFAULT_DSO_PORT;
  
  static {
    XmlObjectHolderHelper xmlHelper = new XmlObjectHolderHelper();
    xmlHelper.init(Server.class, "dso-port");
    DEFAULT_DSO_PORT = xmlHelper.defaultInteger();
    xmlHelper.init(Server.class, "jmx-port");
    DEFAULT_JMX_PORT = xmlHelper.defaultInteger();
  }
  
  public ServersPanel(Composite parent, int style) {
    super(parent, style);

    m_layout = new Layout(this);
    m_tableSelectionHandler = new TableSelectionHandler();
    m_addServerHandler = new AddServerHandler();
    m_removeServerHandler = new RemoveServerHandler();
    
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  private void addListeners() {
    m_layout.m_serverTable.addSelectionListener(m_tableSelectionHandler);
    m_layout.m_addServerButton.addSelectionListener(m_addServerHandler);
    m_layout.m_removeServerButton.addSelectionListener(m_removeServerHandler);
  }

  private void removeListeners() {
    m_layout.m_serverTable.removeSelectionListener(m_tableSelectionHandler);
    m_layout.m_addServerButton.removeSelectionListener(m_addServerHandler);
    m_layout.m_removeServerButton.removeSelectionListener(m_removeServerHandler);
  }
  
  private void updateChildren() {
    m_layout.m_serverTable.removeAll();
    initTableItems();
    testDisableRemoveButton();
    m_layout.m_serverPanel.setVisible(haveAnyServers());
  }
  
  public void setup(IProject project) {
    TcPlugin plugin = TcPlugin.getDefault();
    
    setEnabled(true);
    removeListeners();

    m_project = project;
    m_config = plugin.getConfiguration(m_project);
    m_servers = m_config != null ? m_config.getServers() : null;

    updateChildren();  
    addListeners();
    handleTableSelection();
  }
  
  public IProject getProject() {
    return m_project;
  }

  private Server getSelectedServer() {
    int index = m_layout.m_serverTable.getSelectionIndex();
    if(index != -1) {
      TableItem item = m_layout.m_serverTable.getItem(index);
      return (Server) item.getData();
    } else {
      return null;
    }
  }

  boolean haveAnyServers() {
    return m_servers != null && m_servers.sizeOfServerArray() > 0;
  }
  
  void testRemoveServers() {
    if(m_servers != null && m_servers.sizeOfServerArray() == 0) {
      m_config.unsetServers();
      m_servers = null;
    }
    m_layout.m_serverPanel.setVisible(haveAnyServers());
    fireServersChanged();
    testDisableRemoveButton();
  }

  private void testDisableRemoveButton() {
    m_layout.m_removeServerButton.setEnabled(m_layout.m_serverTable.getSelectionCount()>0);
  }
  
  public void tearDown() {
    removeListeners();

  }
  private void initTableItems() {
    m_layout.m_serverTable.removeAll();
    if (m_servers == null) return;
    Server[] servers = m_servers.getServerArray();
    for (int i = 0; i < servers.length; i++) {
      createTableItem(servers[i]);
    }
    if(servers.length > 0) {
      m_layout.m_serverTable.setSelection(0);
    }
  }

  private void initTableItem(TableItem item, Server server) {
    item.setText(new String[] {
      server.getName(),
      server.getHost(),
      Integer.toString(server.isSetDsoPort() ? server.getDsoPort().getIntValue() : DEFAULT_DSO_PORT),
      Integer.toString(server.isSetJmxPort() ? server.getJmxPort().getIntValue() : DEFAULT_JMX_PORT)});
  }

  private void updateTableItem(int index) {
    TableItem item = m_layout.m_serverTable.getItem(index);
    initTableItem(item, (Server)item.getData());
  }
  
  private TableItem createTableItem(Server server) {
    TableItem item = new TableItem(m_layout.m_serverTable, SWT.NONE);
    initTableItem(item, server);
    item.setData(server);
    return item;
  }

  private void handleTableSelection() {
    Server server = getSelectedServer();
    m_layout.m_removeServerButton.setEnabled(server != null);
    m_layout.setServer(server);
  }

  private static class Layout implements SWTLayout {
    private static final String ADD      = "Add...";
    private static final String REMOVE   = "Remove";
    private static final String SERVERS  = "Servers";
    private static final String NAME     = "Name";
    private static final String HOST     = "Host";
    private static final String DSO_PORT = "DSO Port";
    private static final String JMX_PORT = "JMX Port";

    private Table               m_serverTable;
    private Button              m_addServerButton;
    private Button              m_removeServerButton;
    private ServerPanel         m_serverPanel;

    public void reset() {
      m_serverTable.removeAll();
      setServer(null);
    }

    private void setServer(Server server) {
      m_serverPanel.setVisible(server != null);
      if(server != null) {
        m_serverPanel.setup(server);
      } else {
        m_serverPanel.tearDown();
      }
    }

    private Layout(Composite parent) {
      GridLayout gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 10;
      parent.setLayout(gridLayout);

      createServersGroup(parent);
      createServerGroup(parent);
    }

    private void createServersGroup(Composite parent) {
      Group serversGroup = new Group(parent, SWT.NONE);
      serversGroup.setText(SERVERS);
      GridLayout gridLayout = new GridLayout(2, false);
      serversGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      serversGroup.setLayoutData(gridData);

      m_serverTable = new Table(serversGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_serverTable.setHeaderVisible(true);
      m_serverTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(serversGroup, m_serverTable);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.heightHint = SWTUtil.tableRowsToPixels(m_serverTable, 3);
      m_serverTable.setLayoutData(gridData);

      TableColumn nameCol = new TableColumn(m_serverTable, SWT.NONE, NAME_INDEX);
      nameCol.setResizable(true);
      nameCol.setText(NAME);
      nameCol.pack();

      TableColumn hostCol = new TableColumn(m_serverTable, SWT.NONE, HOST_INDEX);
      hostCol.setResizable(true);
      hostCol.setText(HOST);
      hostCol.pack();

      TableColumn dsoPortCol = new TableColumn(m_serverTable, SWT.NONE, DSO_PORT_INDEX);
      dsoPortCol.setResizable(true);
      dsoPortCol.setText(DSO_PORT);
      dsoPortCol.pack();

      TableColumn jmxPortCol = new TableColumn(m_serverTable, SWT.NONE, JMX_PORT_INDEX);
      jmxPortCol.setResizable(true);
      jmxPortCol.setText(JMX_PORT);
      jmxPortCol.pack();

      Composite buttonPanel = new Composite(serversGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_addServerButton = new Button(buttonPanel, SWT.PUSH);
      m_addServerButton.setText(ADD);
      m_addServerButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addServerButton);

      m_removeServerButton = new Button(buttonPanel, SWT.PUSH);
      m_removeServerButton.setText(REMOVE);
      m_removeServerButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeServerButton);
    }

    private void createServerGroup(Composite parent) {
      m_serverPanel = new ServerPanel(parent, SWT.NONE);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      m_serverPanel.setLayoutData(gridData);
    }
  }
  
  class TableSelectionHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      handleTableSelection();
    }
  }
  
  class AddServerHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_serverTable.setRedraw(false);
      if(m_servers == null) {
        m_servers = m_config.addNewServers();
      }
      
      Server server = m_servers.addNewServer();
      server.setName("localhost");
      server.setHost("localhost");
      
      BindPort dsoPort = BindPort.Factory.newInstance();
      dsoPort.setBind("0.0.0.0");
      dsoPort.setIntValue(9510);
      server.addNewDsoPort();
      server.setDsoPort(dsoPort);
      
      BindPort jmxPort = BindPort.Factory.newInstance();
      jmxPort.setBind("0.0.0.0");
      jmxPort.setIntValue(9520);
      server.addNewJmxPort();
      server.getJmxPort().setIntValue(9520);

      createTableItem(server);
      fireServersChanged();
      m_layout.m_serverTable.setSelection(m_servers.sizeOfServerArray()-1);
      m_layout.m_serverTable.forceFocus();
      handleTableSelection();
      m_layout.m_serverTable.setRedraw(true);
    }
  }

  class RemoveServerHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_serverTable.setRedraw(false);
      m_layout.m_serverTable.forceFocus();
      int index = m_layout.m_serverTable.getSelectionIndex();
      if(index != -1) {
        m_servers.removeServer(index);
        testRemoveServers();
        int count = m_layout.m_serverTable.getItemCount(); 
        if(count > 0) {
          if(index >= count) index = count-1;
          m_layout.m_serverTable.setSelection(index); 
        }
        m_layout.m_serverTable.forceFocus();
        handleTableSelection();
        m_layout.m_serverTable.setRedraw(true);
      }
    }
  }
  
  int indexOf(Server server) {
    TableItem[] items = m_layout.m_serverTable.getItems();
    for(int i = 0; i < items.length; i++) { 
      if(server == items[i].getData()) return i;
    }
    return -1;
  }
  
  void fireServerChanged(Server server) {
    fireServerChanged(indexOf(server));
  }
  
  public void serverChanged(IProject project, int index) {
    if(project.equals(getProject())) {
      updateTableItem(index);
      handleTableSelection();
    }
  }

  public void serversChanged(IProject project) {
    if(project.equals(getProject())) {
      initTableItems();
    }
  }
}
