/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.List;
import org.dijon.ScrollPane;
import org.dijon.SplitPane;
import org.dijon.TabbedPane;

import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextArea;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.stats.DSOMBean;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

public class ServerPanel extends XContainer {
  private AdminClientContext      m_acc;
  private ServerNode              m_serverNode;
  private DSOMBean                m_dsoBean;
  private PropertyTable           m_propertyTable;
  
  private StatusView              m_statusView;
  private ProductInfoPanel        m_productInfoPanel;

  private TabbedPane              m_tabbedPane;

  private XTextArea               m_environmentTextArea;
  private XTextArea               m_configTextArea;

  private Button                  m_threadDumpButton;
  private SplitPane               m_threadDumpsSplitter;
  private Integer                 m_dividerLoc;
  private DividerListener         m_dividerListener;
  private List                    m_threadDumpList;
  private DefaultListModel        m_threadDumpListModel;
  private XTextArea               m_threadDumpTextArea;
  private ScrollPane              m_threadDumpTextScroller;
  private ThreadDumpEntry         m_lastSelectedEntry;

  private TCServerInfoMBean       m_serverInfoBean;

  private ServerRuntimeStatsPanel m_runtimeStatsPanel;

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

    m_threadDumpButton = (Button) findComponent("TakeThreadDumpButton");
    m_threadDumpButton.addActionListener(new ThreadDumpButtonHandler());

    m_threadDumpsSplitter = (SplitPane) findComponent("ServerThreadDumpsSplitter");
    m_dividerLoc = new Integer(getThreadDumpSplitPref());
    m_dividerListener = new DividerListener();

    m_threadDumpList = (List) findComponent("ThreadDumpList");
    m_threadDumpList.setModel(m_threadDumpListModel = new DefaultListModel());
    m_threadDumpList.addListSelectionListener(new ThreadDumpListSelectionListener());
    m_threadDumpTextArea = (XTextArea) findComponent("ThreadDumpTextArea");
    m_threadDumpTextScroller = (ScrollPane) findComponent("ThreadDumpTextScroller");

    m_runtimeStatsPanel = (ServerRuntimeStatsPanel) findComponent("RuntimeStatsPanel");
    m_runtimeStatsPanel.setServerPanel(this);
    
    hideRuntimeInfo();    
  }

  TCServerInfoMBean getServerInfoBean() {
    if (m_serverInfoBean != null) return m_serverInfoBean;

    try {
      ConnectionContext cc = m_serverNode.getConnectionContext();
      m_serverInfoBean = ServerHelper.getHelper().getServerInfoBean(cc);
      return m_serverInfoBean;
    } catch (Exception e) {
      return null;
    }
  }

  DSOMBean getDSOBean() {
    if (m_dsoBean == null) {
      m_dsoBean = m_serverNode.getDSOBean();
    }
    return m_dsoBean;
  }

  class ThreadDumpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        long requestMillis = System.currentTimeMillis();
        ConnectionContext cc = m_serverNode.getConnectionContext();
        ThreadDumpEntry tde = new ThreadDumpEntry(ServerHelper.getHelper().takeThreadDump(cc, requestMillis));
        m_threadDumpListModel.addElement(tde);
        m_threadDumpList.setSelectedIndex(m_threadDumpListModel.getSize() - 1);
      } catch (Exception e) {
        AdminClient.getContext().log(e);
      }
    }
  }

  class ThreadDumpListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent lse) {
      if (lse.getValueIsAdjusting()) return;
      if (m_lastSelectedEntry != null) {
        m_lastSelectedEntry.setViewPosition(m_threadDumpTextScroller.getViewport().getViewPosition());
      }
      ThreadDumpEntry tde = (ThreadDumpEntry) m_threadDumpList.getSelectedValue();
      m_threadDumpTextArea.setText(tde.getThreadDumpText());
      final Point viewPosition = tde.getViewPosition();
      if (viewPosition != null) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            m_threadDumpTextScroller.getViewport().setViewPosition(viewPosition);
          }
        });
      }
      m_lastSelectedEntry = tde;
    }
  }

  public void addNotify() {
    super.addNotify();
    m_threadDumpsSplitter.addPropertyChangeListener(m_dividerListener);
  }

  public void removeNotify() {
    m_threadDumpsSplitter.removePropertyChangeListener(m_dividerListener);
    super.removeNotify();
  }

  public void doLayout() {
    super.doLayout();

    if (m_dividerLoc != null) {
      m_threadDumpsSplitter.setDividerLocation(m_dividerLoc.intValue());
    } else {
      m_threadDumpsSplitter.setDividerLocation(0.7);
    }
  }

  private int getThreadDumpSplitPref() {
    Preferences prefs = getPreferences();
    Preferences splitPrefs = prefs.node(m_threadDumpsSplitter.getName());
    return splitPrefs.getInt("Split", -1);
  }

  protected Preferences getPreferences() {
    AdminClientContext acc = AdminClient.getContext();
    return acc.prefs.node("ServerPanel");
  }

  protected void storePreferences() {
    AdminClientContext acc = AdminClient.getContext();
    acc.client.storePrefs();
  }

  private class DividerListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent pce) {
      JSplitPane splitter = (JSplitPane) pce.getSource();
      String propName = pce.getPropertyName();

      if (splitter.isShowing() == false || JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(propName) == false) { return; }

      int divLoc = splitter.getDividerLocation();
      Integer divLocObj = new Integer(divLoc);
      Preferences prefs = getPreferences();
      String name = splitter.getName();
      Preferences node = prefs.node(name);

      node.putInt("Split", divLoc);
      storePreferences();

      m_dividerLoc = divLocObj;
    }
  }

  void activated() {
    Date activateDate = new Date(m_serverNode.getActivateTime());
    String activateTime = activateDate.toString();

    setStatusLabel(m_acc.format("server.activated.label", new Object[] { activateTime }));
    m_acc.controller.addServerLog(m_serverNode.getConnectionContext());
    if (!isProductInfoShowing()) {
      showProductInfo();
    }

    testSetEnvironment();
    testSetConfig();

    m_acc.controller.setStatus(m_acc.format("server.activated.status", new Object[] { m_serverNode, activateTime }));
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    Date startDate = new Date(m_serverNode.getStartTime());
    String startTime = startDate.toString();

    setStatusLabel(m_acc.format("server.started.label", new Object[] { startTime }));
    if (!isProductInfoShowing()) {
      showProductInfo();
    }

    testSetEnvironment();
    testSetConfig();

    m_acc.controller.setStatus(m_acc.format("server.started.status", new Object[] { m_serverNode, startTime }));
  }

  private void testSetEnvironment() {
    if (m_environmentTextArea.getDocument().getLength() > 0) return;
    m_environmentTextArea.setText(m_serverNode.getEnvironment());
  }

  private void testSetConfig() {
    if (m_configTextArea.getDocument().getLength() > 0) return;
    m_configTextArea.setText(m_serverNode.getConfig());
  }

  void passiveUninitialized() {
    String startTime = new Date().toString();

    setStatusLabel(m_acc.format("server.initializing.label", new Object[] { startTime }));
    if (!isProductInfoShowing()) {
      showProductInfo();
    }

    testSetEnvironment();
    testSetConfig();

    m_acc.controller.setStatus(m_acc.format("server.initializing.status", new Object[] { m_serverNode, startTime }));
  }

  void passiveStandby() {
    String startTime = new Date().toString();

    setStatusLabel(m_acc.format("server.standingby.label", new Object[] { startTime }));
    if (!isProductInfoShowing()) {
      showProductInfo();
    }

    testSetEnvironment();
    testSetConfig();

    m_acc.controller.setStatus(m_acc.format("server.standingby.status", new Object[] { m_serverNode, startTime }));
  }

  void disconnected() {
    String startTime = new Date().toString();

    setStatusLabel(m_acc.format("server.disconnected.label", new Object[] { startTime }));
    hideRuntimeInfo();
    m_runtimeStatsPanel.stopMonitoringRuntimeStats();

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
  private void showProductInfo() {
    String[] fields = { "Host", "Port", "DSOListenPort", "ProductVersion", "ProductBuildID", "ProductLicense" };
    String[] headings = { "Host", "JMX port", "DSO port", "Version", "Build", "License" };
    m_propertyTable.setModel(new PropertyTableModel(m_serverNode, fields, headings));
    m_propertyTable.getAncestorOfClass(ScrollPane.class).setVisible(true);
    
    m_productInfoPanel.init(m_serverNode.getProductInfo());
    m_productInfoPanel.setVisible(true);
    setTabbedPaneEnabled(true);

    revalidate();
    repaint();
  }

  private void hideRuntimeInfo() {
    m_propertyTable.setModel(new PropertyTableModel());
    m_propertyTable.getAncestorOfClass(ScrollPane.class).setVisible(false);
    m_productInfoPanel.setVisible(false);
    m_tabbedPane.setSelectedIndex(0);
    m_tabbedPane.setEnabled(false);

    revalidate();
    repaint();
  }

  public void tearDown() {
    super.tearDown();

    m_statusView.tearDown();
    m_productInfoPanel.tearDown();
    m_runtimeStatsPanel.tearDown();

    m_acc = null;
    m_serverNode = null;
    m_statusView = null;
    m_productInfoPanel = null;
    m_runtimeStatsPanel = null;
  }
}
