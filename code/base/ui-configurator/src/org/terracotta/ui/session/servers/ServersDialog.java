/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session.servers;

import org.terracotta.ui.session.SessionIntegratorContext;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XScrollPane;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;

public class ServersDialog extends JDialog {
  private ServerSelection     selection;
  private ServerInfo[]        servers;
  private JComboBox           selector;
  private ServerEnvTableModel envTableModel;
  private XObjectTable        envTable;
  private XLabel              errorLabel;
  private Icon                errorIcon;
  private XButton             okButton;
  private XButton             cancelButton;
  private XButton             restoreButton;

  public ServersDialog(JFrame frame, SessionIntegratorContext sessionIntegratorContext) {
    super(frame, true);

    selection = new ServerSelection(sessionIntegratorContext);

    setTitle(frame.getTitle());

    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.WEST;

    topPanel.add(new XLabel(sessionIntegratorContext.getString("current.webserver")), gbc);
    gbc.gridx++;
    topPanel.add(selector = new JComboBox(), gbc);
    selector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        finishEditing();
        updateEnvironmentTable();
      }
    });
    cp.add(topPanel, BorderLayout.NORTH);

    envTable = new ServerEnvTable();
    envTable.setModel(envTableModel = new ServerEnvTableModel());
    envTableModel.addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        validateModel();
      }
    });

    XContainer centerPanel = new XContainer(new BorderLayout());
    centerPanel.add(new XScrollPane(envTable), BorderLayout.CENTER);
    centerPanel.add(errorLabel = new XLabel(), BorderLayout.SOUTH);
    errorIcon = new ImageIcon(getClass().getResource("/com/tc/admin/icons/error12x12.gif"));
    errorLabel.setHorizontalAlignment(SwingConstants.LEFT);
    cp.add(centerPanel, BorderLayout.CENTER);

    XContainer bottomPanel = new XContainer(new BorderLayout());
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    XContainer buttonPanel = new XContainer(new GridLayout(1, 0));
    buttonPanel.add(okButton = new XButton("OK"));
    buttonPanel.add(cancelButton = new XButton("Cancel"));
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setVisible(false);
      }
    });
    buttonPanel.add(restoreButton = new XButton("Restore Defaults"));
    restoreButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int index = getSelectedServerIndex();
        ServerInfo server = getSelectedServer();
        Properties props = selection.getDefaultProperties(index);

        server.setProperties(props);
        envTableModel.set(server.getProperties());
      }
    });

    bottomPanel.add(buttonPanel, BorderLayout.EAST);
    cp.add(bottomPanel, BorderLayout.SOUTH);

    try {
      String methodName = "setAlwaysOnTop";
      Class[] argTypes = new Class[] { Boolean.class };
      Method method = getClass().getMethod(methodName, argTypes);

      if (method != null) {
        method.invoke(this, new Object[] { Boolean.TRUE });
      }
    } catch (Exception e) {/**/
    }

    pack();
  }

  public void addAcceptListener(ActionListener listener) {
    okButton.addActionListener(listener);
  }

  public void setSelection(ServerSelection selection) {
    setServers(selection.cloneServers());
    selector.setSelectedIndex(selection.getSelectedServerIndex());
  }

  private void updateEnvironmentTable() {
    envTableModel.set(getSelectedServer().getProperties());
  }

  private void setServers(ServerInfo[] servers) {
    selector.setModel(new DefaultComboBoxModel(this.servers = servers));
    updateEnvironmentTable();
  }

  public ServerInfo[] getServers() {
    return servers;
  }

  public int getSelectedServerIndex() {
    return selector.getSelectedIndex();
  }

  public ServerInfo getSelectedServer() {
    return (ServerInfo) selector.getSelectedItem();
  }

  public ServerInfo getServer(String name) {
    ComboBoxModel model = selector.getModel();
    ServerInfo server;

    for (int i = 0; i < model.getSize(); i++) {
      server = (ServerInfo) model.getElementAt(i);

      if (server.getName().equals(name)) { return server; }
    }

    return null;
  }

  public Properties getServerProperties(String name) {
    ServerInfo server = getServer(name);
    return server != null ? server.toProperties() : null;
  }

  public void finishEditing() {
    if (envTable.isEditing()) {
      TableCellEditor editor = envTable.getCellEditor();

      if (!editor.stopCellEditing()) {
        editor.cancelCellEditing();
      }
    }
  }

  private void validateModel() {
    ServerInfo server = getSelectedServer();
    String[] messages = server.validateProperties();
    String msg = null;
    Icon icon = null;

    if (messages != null) {
      msg = messages[0];
      icon = errorIcon;
    }

    errorLabel.setText(msg);
    errorLabel.setIcon(icon);

  }
}
