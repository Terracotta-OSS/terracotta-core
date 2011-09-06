/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JComponent;

public class LogsPanel extends XContainer {
  private final IAdminClientContext adminClientContext;
  private XTabbedPane               tabbedPane;
  private DashboardViewer           dashboardViewer;
  private LogViewer                 logViewer;

  public LogsPanel(IAdminClientContext adminClientContext) {
    super(new BorderLayout());
    this.adminClientContext = adminClientContext;
    add(tabbedPane = new XTabbedPane());
  }

  void select(IClusterModel clusterModel) {
    if (dashboardViewer != null) {
      dashboardViewer.select(clusterModel);
    }
    if (logViewer != null) {
      logViewer.select(clusterModel);
    }
  }

  public void add(String label, JComponent comp) {
    int index = tabbedPane.getTabCount();
    tabbedPane.addTab(label, (Icon) null, comp, null);
    tabbedPane.setSelectedIndex(index);
  }

  public void add(IClusterModel clusterModel) {
    if (logViewer == null) {
      add("Logs", logViewer = new LogViewer(adminClientContext));
    }
    logViewer.add(clusterModel);

    if (dashboardViewer == null) {
      add(adminClientContext.getString("cluster.dashboard"), dashboardViewer = new DashboardViewer(adminClientContext));
    }
    dashboardViewer.add(clusterModel);
  }

  public void remove(IClusterModel clusterModel) {
    if (logViewer != null) {
      logViewer.remove(clusterModel);
      if (logViewer.isEmpty()) {
        tabbedPane.remove(logViewer);
        logViewer.tearDown();
        logViewer = null;
      }
    }

    if (dashboardViewer != null) {
      dashboardViewer.remove(clusterModel);
      if (dashboardViewer.isEmpty()) {
        tabbedPane.remove(dashboardViewer);
        dashboardViewer.tearDown();
        dashboardViewer = null;
      }
    }
  }

  public void clear() {
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      Component comp = tabbedPane.getComponentAt(i);
      if (comp instanceof XContainer) {
        ((XContainer) comp).tearDown();
      }
    }
    tabbedPane.removeAll();
  }
}
