/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.ContainerResource;
import org.dijon.ScrollPane;
import org.dijon.TabbedPane;

import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextArea;

import java.util.Date;
import java.util.concurrent.Callable;

import javax.swing.table.DefaultTableCellRenderer;

public class ServerPanel extends XContainer {
  private AdminClientContext      m_acc;
  private ServerNode              m_serverNode;
  private PropertyTable           m_propertyTable;
  private StatusView              m_statusView;
  private ProductInfoPanel        m_productInfoPanel;
  private TabbedPane              m_tabbedPane;
  private XTextArea               m_environmentTextArea;
  private XTextArea               m_configTextArea;

  public ServerPanel(ServerNode serverNode) {
    super(serverNode);

    m_serverNode = serverNode;
    m_acc = AdminClient.getContext();

    load((ContainerResource) m_acc.topRes.getComponent("ServerPanel"));

    m_tabbedPane = (TabbedPane) findComponent("TabbedPane");

    m_propertyTable = (PropertyTable) findComponent("ServerInfoTable");
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
    m_propertyTable.setDefaultRenderer(Long.class, renderer);
    m_propertyTable.setDefaultRenderer(Integer.class, renderer);

    m_statusView = (StatusView) findComponent("StatusIndicator");
    m_productInfoPanel = (ProductInfoPanel) findComponent("ProductInfoPanel");

    m_statusView.setLabel("Not connected");
    m_productInfoPanel.setVisible(false);

    m_environmentTextArea = (XTextArea) findComponent("EnvironmentTextArea");
    m_configTextArea = (XTextArea) findComponent("ConfigTextArea");

    hideProductInfo();
  }

  protected void storePreferences() {
    AdminClientContext acc = AdminClient.getContext();
    acc.client.storePrefs();
  }

  class ServerState {
    private Date        fStartDate;
    private Date        fActivateDate;
    private ProductInfo fProductInfo;
    private String      fEnvironment;
    private String      fConfig;
    private Integer     fDSOListenPort;

    ServerState(Date startDate, Date activateDate, ProductInfo productInfo, String environment, String config, Integer dsoListenPort) {
      fStartDate = startDate;
      fActivateDate = activateDate;
      fProductInfo = productInfo;
      fEnvironment = environment;
      fConfig = config;
    }

    Date getStartDate() {
      return fStartDate;
    }

    Date getActivateDate() {
      return fActivateDate;
    }

    ProductInfo getProductInfo() {
      return fProductInfo;
    }

    String getEnvironment() {
      return fEnvironment;
    }

    String getConfig() {
      return fConfig;
    }
    
    Integer getDSOListenPort() {
      return fDSOListenPort;
    }
  }

  private class ServerStateWorker extends BasicWorker<ServerState> {
    private ServerStateWorker() {
      super(new Callable<ServerState>() {
        public ServerState call() {
          Date startDate = new Date(m_serverNode.getStartTime());
          Date activateDate = new Date(m_serverNode.getActivateTime());
          ProductInfo productInfo = m_serverNode.getProductInfo();
          String environment = m_serverNode.getEnvironment();
          String config = m_serverNode.getConfig();
          Integer dsoListenPort = m_serverNode.getDSOListenPort();
          
          return new ServerState(startDate, activateDate, productInfo, environment, config, dsoListenPort);
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        ServerState serverState = getResult();
        showProductInfo(serverState.getProductInfo());
        m_environmentTextArea.setText(serverState.getEnvironment());
        m_configTextArea.setText(serverState.getConfig());
      }
    }
  }

  private class StartedWorker extends ServerStateWorker {
    protected void finished() {
      super.finished();
      if (getException() == null) {
        ServerState serverState = getResult();
        String startTime = serverState.getStartDate().toString();
        setStatusLabel(m_acc.format("server.started.label", new Object[] { startTime }));
        m_acc.controller.setStatus(m_acc.format("server.started.status", new Object[] { m_serverNode, startTime }));
      }
    }
  }

  private class ActivatedWorker extends ServerStateWorker {
    protected void finished() {
      super.finished();
      if (getException() == null) {
        ServerState serverState = getResult();
        String activateTime = serverState.getActivateDate().toString();

        setStatusLabel(m_acc.format("server.activated.label", new Object[] { activateTime }));
        m_acc.controller.addServerLog(m_serverNode.getConnectionContext());
        m_acc.controller
            .setStatus(m_acc.format("server.activated.status", new Object[] { m_serverNode, activateTime }));
      }
    }
  }

  private class PassiveUninitializedWorker extends ServerStateWorker {
    protected void finished() {
      super.finished();
      if (getException() == null) {
        String startTime = new Date().toString();
        setStatusLabel(m_acc.format("server.initializing.label", new Object[] { startTime }));
        m_acc.controller
            .setStatus(m_acc.format("server.initializing.status", new Object[] { m_serverNode, startTime }));
      }
    }
  }

  private class PassiveStandbyWorker extends ServerStateWorker {
    protected void finished() {
      super.finished();
      if (getException() == null) {
        String startTime = new Date().toString();
        setStatusLabel(m_acc.format("server.standingby.label", new Object[] { startTime }));
        m_acc.controller.setStatus(m_acc.format("server.standingby.status", new Object[] { m_serverNode, startTime }));
      }
    }
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    m_acc.executorService.execute(new StartedWorker());
  }

  void activated() {
    m_acc.executorService.execute(new ActivatedWorker());
  }

  void passiveUninitialized() {
    m_acc.executorService.execute(new PassiveUninitializedWorker());
  }

  void passiveStandby() {
    m_acc.executorService.execute(new PassiveStandbyWorker());
  }

  void disconnected() {
    String startTime = new Date().toString();

    setStatusLabel(m_acc.format("server.disconnected.label", new Object[] { startTime }));
    hideProductInfo();

    m_acc.controller.removeServerLog(m_serverNode.getConnectionContext());
    m_acc.controller.setStatus(m_acc.format("server.disconnected.status", new Object[] { m_serverNode, startTime }));
  }

  private void setTabbedPaneEnabled(boolean enabled) {
    m_tabbedPane.setEnabled(enabled);
    int tabCount = m_tabbedPane.getTabCount();
    for (int i = 0; i < tabCount; i++) {
      m_tabbedPane.setEnabledAt(i, enabled);
    }
    m_tabbedPane.setSelectedIndex(0);
  }

  void setConnectExceptionMessage(String msg) {
    setStatusLabel(msg);
    setTabbedPaneEnabled(false);
  }

  void setStatusLabel(String msg) {
    m_statusView.setLabel(msg);
    m_statusView.setIndicator(m_serverNode.getServerStatusColor());
  }

  boolean isProductInfoShowing() {
    return m_productInfoPanel.isVisible();
  }

  /**
   * The fields listed below are on ProductNode. If those methods change, so will these fields need to change.
   * PropertyTable uses reflection to access values to display.
   * TODO: i18n
   */
  private void showProductInfo(ProductInfo productInfo) {
    String[] fields = { "CanonicalHostName", "HostAddress", "Port", "DSOListenPort", "ProductVersion", "ProductBuildID", "ProductLicense" };
    String[] headings = { "Host", "Address", "JMX port", "DSO port", "Version", "Build", "License" };
    m_propertyTable.setModel(new PropertyTableModel(m_serverNode, fields, headings));
    m_propertyTable.getAncestorOfClass(ScrollPane.class).setVisible(true);

    m_productInfoPanel.init(productInfo);
    m_productInfoPanel.setVisible(true);
    setTabbedPaneEnabled(true);

    revalidate();
    repaint();
  }

  private void hideProductInfo() {
    m_propertyTable.setModel(new PropertyTableModel());
    m_propertyTable.getAncestorOfClass(ScrollPane.class).setVisible(false);
    m_productInfoPanel.setVisible(false);
    m_tabbedPane.setSelectedIndex(0);
    m_tabbedPane.setEnabled(false);

    revalidate();
    repaint();
  }

  public void tearDown() {
    m_statusView.tearDown();
    m_productInfoPanel.tearDown();

    super.tearDown();

    m_acc = null;
    m_serverNode = null;
    m_propertyTable = null;
    m_statusView = null;
    m_productInfoPanel = null;
    m_tabbedPane = null;
    m_environmentTextArea = null;
    m_configTextArea = null;
 }
}
