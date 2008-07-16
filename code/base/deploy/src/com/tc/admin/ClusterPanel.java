/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.ContainerResource;

import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.ServerVersion;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.concurrent.Callable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextField;

public class ClusterPanel extends XContainer {
  private AdminClientContext m_acc;
  private ClusterNode        m_clusterNode;
  private String             m_originalHost;
  private int                m_originalPort;
  private JTextField         m_hostField;
  private JTextField         m_portField;
  private JButton            m_connectButton;
  static private ImageIcon   m_connectIcon;
  static private ImageIcon   m_disconnectIcon;
  private StatusView         m_statusView;
  private ProductInfoPanel   m_productInfoPanel;

  static {
    m_connectIcon = new ImageIcon(ServerPanel.class.getResource("/com/tc/admin/icons/disconnect_co.gif"));
    m_disconnectIcon = new ImageIcon(ServerPanel.class.getResource("/com/tc/admin/icons/newex_wiz.gif"));
  }

  public ClusterPanel(ClusterNode clusterNode) {
    super(clusterNode);

    m_clusterNode = clusterNode;
    m_acc = AdminClient.getContext();

    load((ContainerResource) m_acc.getComponent("ClusterPanel"));

    m_hostField = (JTextField) findComponent("HostField");
    m_portField = (JTextField) findComponent("PortField");
    m_connectButton = (JButton) findComponent("ConnectButton");
    m_statusView = (StatusView) findComponent("StatusIndicator");
    m_productInfoPanel = (ProductInfoPanel) findComponent("ProductInfoPanel");

    m_statusView.setLabel("Not connected");
    m_productInfoPanel.setVisible(false);

    m_hostField.addActionListener(new HostFieldHandler());
    m_portField.addActionListener(new PortFieldHandler());
    m_connectButton.addActionListener(new ConnectionButtonHandler());

    m_hostField.setText(m_originalHost = m_clusterNode.getHost());
    m_portField.setText(Integer.toString(m_originalPort = m_clusterNode.getPort()));

    setupConnectButton();
  }

  void reinitialize() {
    m_hostField.setText(m_clusterNode.getHost());
    m_portField.setText(Integer.toString(m_clusterNode.getPort()));
  }

  class HostFieldHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String host = m_hostField.getText().trim();

      m_clusterNode.setHost(m_originalHost = host);
      m_acc.nodeChanged(m_clusterNode);
      m_acc.updateServerPrefs();
    }
  }

  class PortFieldHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String port = m_portField.getText().trim();

      try {
        m_clusterNode.setPort(m_originalPort = Integer.parseInt(port));
        m_acc.nodeChanged(m_clusterNode);
        m_acc.updateServerPrefs();
      } catch (Exception e) {
        Toolkit.getDefaultToolkit().beep();
        m_acc.log("'" + port + "' not a number");
        m_portField.setText(Integer.toString(m_clusterNode.getPort()));
      }
    }
  }

  class ConnectionButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      m_connectButton.setEnabled(false);
      if (m_clusterNode.isConnected()) {
        disconnect();
      } else {
        connect();
      }
    }
  }

  void setupConnectButton() {
    String label;
    Icon icon;
    boolean enabled;
    boolean connected = m_clusterNode.isConnected();

    if (connected) {
      label = "Disconnect";
      icon = m_disconnectIcon;
      enabled = true;
    } else {
      label = "Connect...";
      icon = m_connectIcon;
      enabled = !m_clusterNode.isAutoConnect();
    }

    m_connectButton.setText(label);
    m_connectButton.setIcon(icon);
    m_connectButton.setEnabled(enabled);
  }

  JButton getConnectButton() {
    return m_connectButton;
  }

  private void connect() {
    m_clusterNode.connect();
  }

  void activated() {
    m_acc.execute(new ActivatedWorker());
  }

  private class ActivatedWorker extends BasicWorker<Date> {
    private ActivatedWorker() {
      super(new Callable<Date>() {
        public Date call() throws Exception {
          return new Date(m_clusterNode.getActivateTime());
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        m_hostField.setEditable(false);
        m_portField.setEditable(false);
        setupConnectButton();
        setStatusLabel(m_acc.format("server.activated.label", getResult().toString()));
        testShowProductInfo();
      }
    }
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    m_acc.execute(new StartedWorker());
  }

  private class StartedWorker extends BasicWorker<Date> {
    private StartedWorker() {
      super(new Callable<Date>() {
        public Date call() throws Exception {
          return new Date(m_clusterNode.getStartTime());
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        m_hostField.setEditable(false);
        m_portField.setEditable(false);
        setupConnectButton();
        setStatusLabel(m_acc.format("server.started.label", getResult().toString()));
        testShowProductInfo();
      }
    }
  }

  void passiveUninitialized() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);
    setupConnectButton();
    setStatusLabel(m_acc.format("server.initializing.label", new Date().toString()));
    testShowProductInfo();
  }

  void passiveStandby() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);
    setupConnectButton();
    setStatusLabel(m_acc.format("server.standingby.label", new Date().toString()));
    testShowProductInfo();
  }

  private void disconnect() {
    m_clusterNode.getDisconnectAction().actionPerformed(null);
  }

  void disconnected() {
    m_hostField.setEditable(true);
    m_hostField.setText(m_originalHost);
    m_clusterNode.setHost(m_originalHost);

    m_portField.setEditable(true);
    m_portField.setText(Integer.toString(m_originalPort));
    m_clusterNode.setPort(m_originalPort);

    String startTime = new Date().toString();
    setupConnectButton();
    setStatusLabel(m_acc.format("server.disconnected.label", startTime));
    hideProductInfo();

    m_acc.setStatus(m_acc.format("server.disconnected.status", m_clusterNode, startTime));
  }

  void setStatusLabel(String msg) {
    m_statusView.setLabel(msg);
    m_statusView.setIndicator(m_clusterNode.getServerStatusColor());
  }

  boolean isProductInfoShowing() {
    return m_productInfoPanel.isVisible();
  }

  private void testShowProductInfo() {
    if (!isProductInfoShowing()) {
      m_acc.execute(new ProductInfoWorker());
    }
  }

  private class ProductInfoWorker extends BasicWorker<ServerVersion> {
    private ProductInfoWorker() {
      super(new Callable<ServerVersion>() {
        public ServerVersion call() throws Exception {
          return m_clusterNode.getProductInfo();
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        // assume the server went away
      } else {
        showProductInfo(getResult());
      }
    }
  }

  private void showProductInfo(ServerVersion productInfo) {
    m_productInfoPanel.init(productInfo.version(), productInfo.copyright());
    m_productInfoPanel.setVisible(true);
    revalidate();
    repaint();
  }

  private void hideProductInfo() {
    m_productInfoPanel.setVisible(false);
    revalidate();
    repaint();
  }

  public void tearDown() {
    super.tearDown();

    m_statusView.tearDown();
    m_productInfoPanel.tearDown();

    m_acc = null;
    m_clusterNode = null;
    m_hostField = null;
    m_portField = null;
    m_connectButton = null;
    m_statusView = null;
    m_productInfoPanel = null;
  }
}
