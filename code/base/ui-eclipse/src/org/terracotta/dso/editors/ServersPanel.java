/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.DictionaryResource;

import org.terracotta.dso.TcPlugin;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XTable.PortNumberRenderer;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.event.ActionEvent;

import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;

public class ServersPanel extends ConfigurationEditorPanel
  implements ConfigurationEditorRoot,
             ListSelectionListener,
             TableModelListener
{
  private static ContainerResource m_res;
  private IProject                 m_project;
  private TcConfig                 m_config;  
  private Servers                  m_servers;
  private ServerTableModel         m_serverTableModel;
  private XObjectTable             m_serverTable;
  private JPopupMenu               m_popupMenu;
  private AddServerAction          m_addServerAction;
  private RemoveServerAction       m_removeServerAction;
  private Button                   m_addServerButton;
  private Button                   m_removeServerButton;
  private ServerPanel              m_serverPanel;

  static {
    TcPlugin           plugin = TcPlugin.getDefault();
    DictionaryResource topRes = plugin.getResources();

    m_res = (ContainerResource)topRes.find("ServersPanel");
  }
  
  public ServersPanel() {
    super();
    if(m_res != null) {
      load(m_res);
    }
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_serverTable = (XObjectTable)findComponent("ServersTable");
    m_serverTable.setModel(m_serverTableModel = new ServerTableModel());
    m_serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    TableColumnModel colModel = m_serverTable.getColumnModel();
    PortNumberRenderer portRenderer = new XObjectTable.PortNumberRenderer();
    colModel.getColumn(2).setCellRenderer(portRenderer);
    colModel.getColumn(3).setCellRenderer(portRenderer);
  
    m_serverPanel = (ServerPanel)findComponent("ServerPanel");
    
    initMenu();
    
    m_addServerButton = (Button)findComponent("AddServerButton");
    m_addServerButton.setAction(m_addServerAction);

    m_removeServerButton = (Button)findComponent("RemoveServerButton");
    m_removeServerButton.setAction(m_removeServerAction);
  }

  private void initMenu() {
    m_popupMenu = new JPopupMenu("ServersPanel Actions");

    m_popupMenu.add(m_addServerAction = new AddServerAction());
    m_popupMenu.add(m_removeServerAction = new RemoveServerAction());
    
    m_serverTable.setPopupMenu(m_popupMenu);
  }

  class AddServerAction extends XAbstractAction {
    public AddServerAction() {
      super("Add");
    }
    
    public void actionPerformed(ActionEvent ae) {
      if(m_servers == null) {
        m_servers = m_config.addNewServers();
      }
      
      int    row    = m_servers.sizeOfServerArray();
      Server server = m_servers.addNewServer();
      
      server.setName("dev");
      server.setHost("localhost");
      server.setDsoPort(9510);
      server.setJmxPort(9520);
      
      m_serverTableModel.add(server);
      m_serverTableModel.fireTableRowsInserted(row, row);
      m_serverTable.setRowSelectionInterval(row, row);
    }
  }
  
  class RemoveServerAction extends XAbstractAction {
    public RemoveServerAction() {
      super("Remove");
      setEnabled(isEnabled());
    }

    public void actionPerformed(ActionEvent ae) {
      int row = m_serverTable.getSelectedRow();
      
      if(row != -1) {
        Server server = m_servers.getServerArray(row);
        
        m_servers.removeServer(row);
        m_serverTableModel.remove(server);
        m_serverTableModel.fireTableRowsDeleted(row, row);

        int count = m_serverTableModel.getRowCount();
        if(count > 0) {
          row = Math.min(count-1, row);
          m_serverTable.setRowSelectionInterval(row, row);
        }
      }
    }
    
    public boolean isEnabled() {
      return !m_serverTable.getSelectionModel().isSelectionEmpty();
    }
  }
  
  private static final String[] FIELDS  = new String[] {
    "Name", "Host", "DsoPort", "JmxPort"
  };
  private static final String[] HEADERS = new String[] {
    "Name", "Host", "DSO port", "JMX port"
  };
  
  class ServerTableModel extends XObjectTableModel {
    public ServerTableModel() {
      super(Server.class, FIELDS, HEADERS);
    }
    
    public void setServers(Servers servers) {
      ServerTableModel.this.clear();
      set(servers.getServerArray());
    }
  }

  public void tableChanged(TableModelEvent e) {
    if(m_serverTableModel.getRowCount() == 0) {
      m_config.unsetServers();
      m_servers = null;
      m_serverPanel.setVisible(false);
    }
    else {
      m_serverPanel.setVisible(true);
    }      
    
    setDirty();
  }
  
  public void valueChanged(ListSelectionEvent e) {
    if(!e.getValueIsAdjusting()) {
      int     row     = m_serverTable.getSelectedRow();
      boolean haveSel = row != -1;
        
      if(haveSel) {
        m_serverPanel.tearDown();
        m_serverPanel.setup((Server)m_serverTableModel.getObjectAt(row));
      }
      m_removeServerAction.setEnabled(haveSel);
    }
  }

  private void addListeners() {
    m_serverTableModel.addTableModelListener(this);
    m_serverTable.getSelectionModel().addListSelectionListener(this);
    
    if(m_serverTableModel.getRowCount() > 0) {
      m_serverTable.setRowSelectionInterval(0, 0);
    }
  }
  
  private void removeListeners() {
    m_serverTableModel.removeTableModelListener(this);
    m_serverTable.getSelectionModel().removeListSelectionListener(this);    
  }
  
  private void updateChildren() {
    m_serverTableModel.clear();
    if(m_servers != null) {
      m_serverTableModel.setServers(m_servers);
    }
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
  }
  
  public IProject getProject() {
    return m_project;
  }
  
  public void tearDown() {
    removeListeners();
    m_serverTableModel.clear();
    setEnabled(false);
  }
}
