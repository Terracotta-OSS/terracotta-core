/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.servers;

import org.dijon.Button;
import org.dijon.ComboBox;
import org.dijon.ComboModel;
import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.Frame;
import org.dijon.Label;

import com.tc.admin.common.XObjectTable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;

public class ServersDialog extends Dialog {
  private ServerInfo[]        m_servers;
  private ComboBox            m_selector;
  private ServerEnvTableModel m_envTableModel;
  private XObjectTable        m_envTable;
  private Label               m_errorLabel;
  private Icon                m_errorIcon;
  private Button              m_restoreButton;
  
  public ServersDialog(Frame frame) {
    super(frame, true);
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);
    
    m_selector = (ComboBox)findComponent("ServerSelector");
    m_selector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        finishEditing();
        updateEnvironmentTable();
      }
    });
    
    m_envTable = (XObjectTable)findComponent("ServerEnvironment");
    m_envTable.setModel(m_envTableModel = new ServerEnvTableModel());
    ((JButton)findComponent("CancelButton")).addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setVisible(false);
      }
    });
    m_envTableModel.addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        validateModel();
      }
    });

    m_errorLabel = (Label)findComponent("ErrorLabel");
    m_errorIcon  = m_errorLabel.getIcon();
    m_errorLabel.setIcon(null);
    
    try {
      String  methodName = "setAlwaysOnTop";
      Class[] argTypes   = new Class[] {Boolean.class};
      Method  method     = getClass().getMethod(methodName, argTypes);
      
      if(method != null) {
        method.invoke(this, new Object[] {Boolean.TRUE});
      }
    } catch(Exception e) {/**/}

    m_restoreButton = (Button)findComponent("RestoreButton");
    m_restoreButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int        index  = getSelectedServerIndex();
        ServerInfo server = getSelectedServer();
        Properties props  = ServerSelection.getInstance().getDefaultProperties(index);

        server.setProperties(props);
        m_envTableModel.set(server.getProperties());
      }
    });
  }
  
  public void addAcceptListener(ActionListener listener) {
    ((JButton)findComponent("OKButton")).addActionListener(listener);
  }
  
  public void setSelection(ServerSelection selection) {
    setServers(selection.cloneServers());
    m_selector.setSelectedIndex(selection.getSelectedServerIndex());
  }
  
  private void updateEnvironmentTable() {
    m_envTableModel.set(getSelectedServer().getProperties());
  }
  
  private void setServers(ServerInfo[] servers) {
    m_selector.setModel(new ComboModel(m_servers = servers));
    updateEnvironmentTable();
  }

  public ServerInfo[] getServers() {
    return m_servers;
  }
  
  public int getSelectedServerIndex() {
    return m_selector.getSelectedIndex();
  }
  
  public ServerInfo getSelectedServer() {
    return (ServerInfo)m_selector.getSelectedItem();
  }
  
  public ServerInfo getServer(String name) {
    ComboBoxModel model = m_selector.getModel();
    ServerInfo    server;
    
    for(int i = 0; i < model.getSize(); i++) {
      server = (ServerInfo)model.getElementAt(i);
      
      if(server.getName().equals(name)) {
        return server;
      }
    }
    
    return null;
  }
  
  public Properties getServerProperties(String name) {
    ServerInfo server = getServer(name);
    return server != null ? server.toProperties() : null;
  }

  public void finishEditing() {
    if(m_envTable.isEditing()) {
      TableCellEditor editor = m_envTable.getCellEditor();
      
      if(!editor.stopCellEditing()) {
        editor.cancelCellEditing();
      }
    }
  }
  
  private void validateModel() {
    ServerInfo server   = getSelectedServer();
    String[]   messages = server.validateProperties();
    String     msg      = null;
    Icon       icon     = null;
    
    if(messages != null) {
      msg  = messages[0];
      icon = m_errorIcon;
    }
    
    m_errorLabel.setText(msg);
    m_errorLabel.setIcon(icon);
    
  }
}
