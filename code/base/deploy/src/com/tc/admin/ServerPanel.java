/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.TextField;

import com.tc.admin.common.StatusRenderer;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.config.schema.L2Info;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumnModel;

public class ServerPanel extends XContainer {
  private AdminClientContext      m_acc;
  private ServerNode              m_serverNode;
  private JTextField              m_hostField;
  private JTextField              m_portField;
  private JPasswordField          m_passwordField;
  private JTextField              m_usernameField;
  private JButton                 m_connectButton;
  static private ImageIcon        m_connectIcon;
  static private ImageIcon        m_disconnectIcon;
  private Container               m_runtimeInfoPanel;
  private StatusView              m_statusView;
  private ProductInfoPanel        m_productInfoPanel;
  private ProductInfoPanel        m_altProductInfoPanel;    // Displayed if the RuntimeInfoPanel is not.
  private XObjectTable            m_clusterMemberTable;
  private ClusterMemberTableModel m_clusterMemberTableModel;
  private ClusterMemberListener   m_clusterMemberListener;

  static {
    m_connectIcon = new ImageIcon(ServerPanel.class.getResource("/com/tc/admin/icons/disconnect_co.gif"));
    m_disconnectIcon = new ImageIcon(ServerPanel.class.getResource("/com/tc/admin/icons/newex_wiz.gif"));
  }

  public ServerPanel(ServerNode serverNode) {
    super(serverNode);

    m_serverNode = serverNode;
    m_acc = AdminClient.getContext();

    load((ContainerResource) m_acc.topRes.getComponent("ServerPanel"));

    m_hostField = (JTextField) findComponent("HostField");
    m_portField = (JTextField) findComponent("PortField");
    m_connectButton = (JButton) findComponent("ConnectButton");
    m_usernameField = (JTextField) findComponent("UsernameField");
    m_runtimeInfoPanel = (Container) findComponent("RuntimeInfoPanel");
    m_statusView = (StatusView) findComponent("StatusIndicator");
    m_productInfoPanel = (ProductInfoPanel) findComponent("ProductInfoPanel");
    m_clusterMemberTable = (XObjectTable) findComponent("ClusterMembersTable");
    m_clusterMemberTableModel = new ClusterMemberTableModel();
    m_clusterMemberListener = new ClusterMemberListener();

    Container credentialsPanel = (Container) findComponent("CredentialsPanel");
    TitledBorder border = BorderFactory.createTitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Credentials");
    credentialsPanel.setBorder(border);
    
    TextField passwordField = (TextField) findComponent("PasswordField");
    Container passwdHolder = new Container();
    passwdHolder.setLayout(new BorderLayout());
    passwdHolder.add(m_passwordField = new JPasswordField());
    credentialsPanel.replaceChild(passwordField, passwdHolder);

    m_clusterMemberTable.setModel(m_clusterMemberTableModel);
    TableColumnModel colModel = m_clusterMemberTable.getColumnModel();
    colModel.getColumn(0).setCellRenderer(new ClusterMemberStatusRenderer());
    colModel.getColumn(2).setCellRenderer(new XObjectTable.PortNumberRenderer());

    m_statusView.setLabel("Not connected");
    m_runtimeInfoPanel.setVisible(false);

    m_hostField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String host = m_hostField.getText().trim();

        m_serverNode.setHost(host);
        m_acc.controller.nodeChanged(m_serverNode);
        m_acc.controller.updateServerPrefs();
      }
    });

    m_portField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String port = m_portField.getText().trim();

        try {
          m_serverNode.setPort(Integer.parseInt(port));
          m_acc.controller.nodeChanged(m_serverNode);
          m_acc.controller.updateServerPrefs();
        } catch (Exception e) {
          Toolkit.getDefaultToolkit().beep();
          m_acc.controller.log("'" + port + "' not a number");
          m_portField.setText(Integer.toString(m_serverNode.getPort()));
        }
      }
    });
    
    m_usernameField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String username = m_usernameField.getText().trim();
        
        m_serverNode.setUsername(username);
        m_acc.controller.nodeChanged(m_serverNode);
        m_acc.controller.updateServerPrefs();
      }
    });
    
    m_passwordField.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        String password = new String(m_passwordField.getPassword()).trim();

        m_serverNode.setPassword(password);
        m_acc.controller.nodeChanged(m_serverNode);
        m_acc.controller.updateServerPrefs();
      }
    });

    m_connectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if (m_serverNode.isConnected()) {
          disconnect();
        } else {
          connect();
        }
      }
    });

    m_hostField.setText(m_serverNode.getHost());
    m_portField.setText(Integer.toString(m_serverNode.getPort()));

    setupConnectButton();
  }

  void setupConnectButton() {
    String label;
    Icon icon;
    boolean enabled;

    if (m_serverNode.isConnected()) {
      label = "Disconnect";
      icon = m_disconnectIcon;
      enabled = true;
    } else {
      label = "Connect...";
      icon = m_connectIcon;
      enabled = !m_serverNode.isAutoConnect();
    }

    m_connectButton.setText(label);
    m_connectButton.setIcon(icon);
    m_connectButton.setEnabled(enabled);
  }

  JButton getConnectButton() {
    return m_connectButton;
  }

  private void connect() {
    m_serverNode.connect();
  }

  void activated() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);
    m_usernameField.setEditable(false);
    m_passwordField.setEditable(false);

    setupConnectButton();

    Date activateDate = new Date(m_serverNode.getActivateTime());
    String activateTime = DateFormat.getTimeInstance().format(activateDate);
    String statusMsg = "Activated at " + activateTime;

    setStatusLabel(statusMsg);
    m_acc.controller.addServerLog(m_serverNode.getConnectionContext());
    showRuntimeInfo();

    m_acc.controller.setStatus(m_serverNode + " activated at " + activateTime);
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);
    m_usernameField.setEditable(false);
    m_passwordField.setEditable(false);

    Date startDate = new Date(m_serverNode.getStartTime());
    String startTime = DateFormat.getTimeInstance().format(startDate);
    String statusMsg = "Started at " + startTime;

    setupConnectButton();
    setStatusLabel(statusMsg);
    showRuntimeInfo();

    m_acc.controller.setStatus("Started " + m_serverNode + " at " + startTime);
  }

  private void disconnect() {
    m_serverNode.disconnect();
  }

  void disconnected() {
    m_hostField.setEditable(true);
    m_portField.setEditable(true);
    m_usernameField.setEditable(true);
    m_passwordField.setEditable(true);

    String startTime = DateFormat.getTimeInstance().format(new Date());
    String statusMsg = "Disconnected at " + startTime;

    setupConnectButton();
    setStatusLabel(statusMsg);
    hideRuntimeInfo();

    m_acc.controller.removeServerLog(m_serverNode.getConnectionContext());
    m_acc.controller.setStatus(m_serverNode + " disconnected at " + startTime);
  }

  void setStatusLabel(String msg) {
    m_statusView.setLabel(msg);
    m_statusView.setIndicator(getServerStatusColor());
  }

  private Color getServerStatusColor() {
    Color color = Color.LIGHT_GRAY;

    if (m_serverNode.isActive()) {
      color = Color.GREEN;
    } else if (m_serverNode.isStarted()) {
      color = Color.YELLOW;
    } else if (m_serverNode.hasConnectionException()) {
      color = Color.RED;
    }

    return color;
  }

  private void showRuntimeInfo() {
    L2Info[] clusterMembers = m_serverNode.getClusterMembers();

    m_clusterMemberTableModel.clear();

    if (clusterMembers.length > 1) {
      Container parent;

      if (m_altProductInfoPanel != null && (parent = (Container) m_altProductInfoPanel.getParent()) != null) {
        parent.replaceChild(m_altProductInfoPanel, m_runtimeInfoPanel);
      }

      m_productInfoPanel.init(m_serverNode.getProductInfo());
      m_runtimeInfoPanel.setVisible(true);

      for (int i = 0; i < clusterMembers.length; i++) {
        addClusterMember(clusterMembers[i]);
      }
      m_clusterMemberTableModel.fireTableDataChanged();
    } else {
      if (m_altProductInfoPanel == null) {
        m_altProductInfoPanel = new ProductInfoPanel();
      }

      Container parent;
      if ((parent = (Container) m_runtimeInfoPanel.getParent()) != null) {
        parent.replaceChild(m_runtimeInfoPanel, m_altProductInfoPanel);
      }

      m_altProductInfoPanel.init(m_serverNode.getProductInfo());
      m_altProductInfoPanel.setVisible(true);
    }

    revalidate();
    repaint();
  }

  private void hideRuntimeInfo() {
    m_runtimeInfoPanel.setVisible(false);
    revalidate();
    repaint();
  }

  private class ClusterMemberStatusRenderer extends StatusRenderer {
    ClusterMemberStatusRenderer() {
      super();
    }

    public void setValue(JTable table, int row, int col) {
      ServerConnectionManager member = m_clusterMemberTableModel.getClusterMemberAt(row);

      m_label.setText(member.getName());

      Color bg = Color.LIGHT_GRAY;
      if (member.isActive()) {
        bg = Color.GREEN;
      } else if (member.isStarted()) {
        bg = Color.YELLOW;
      } else if (member.getConnectionException() != null) {
        bg = Color.RED;
      }
      m_indicator.setBackground(bg);
    }
  }

  private class ClusterMemberListener implements ConnectionListener {
    public void handleConnection() {
      if (m_clusterMemberTableModel != null) {
        int count = m_clusterMemberTableModel.getRowCount();
        m_clusterMemberTableModel.fireTableRowsUpdated(0, count - 1);
      }
    }

    public void handleException() {
      if (m_clusterMemberTableModel != null) {
        m_clusterMemberTableModel.fireTableDataChanged();
      }
    }
  }

  void addClusterMember(L2Info clusterMember) {
    String host = clusterMember.host();

    if (host.equals("localhost") || host.equals(L2Info.IMPLICIT_L2_NAME)) {
      clusterMember = new L2Info(clusterMember.name(), m_serverNode.getHost(), clusterMember.jmxPort());
    }

    ServerConnectionManager scm = new ServerConnectionManager(clusterMember, true, m_clusterMemberListener);
    m_clusterMemberTableModel.addClusterMember(scm);
  }

  public void tearDown() {
    super.tearDown();

    m_statusView.tearDown();
    m_productInfoPanel.tearDown();
    m_clusterMemberTableModel.tearDown();

    m_acc = null;
    m_serverNode = null;
    m_hostField = null;
    m_portField = null;
    m_connectButton = null;
    m_runtimeInfoPanel = null;
    m_statusView = null;
    m_productInfoPanel = null;
    m_clusterMemberTable = null;
    m_clusterMemberTableModel = null;
    m_clusterMemberListener = null;
  }
}
