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
  private IAdminClientContext   adminClientContext;
  private XTabbedPane           tabbedPane;
  private LiveObjectCountViewer liveObjectCountViewer;
  private LogViewer             logViewer;

  public LogsPanel(IAdminClientContext adminClientContext) {
    super(new BorderLayout());
    this.adminClientContext = adminClientContext;
    add(tabbedPane = new XTabbedPane());
  }

  void select(IClusterModel clusterModel) {
    if (liveObjectCountViewer != null) {
      liveObjectCountViewer.select(clusterModel);
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

    if (liveObjectCountViewer == null) {
      add("LiveObjectCount", liveObjectCountViewer = new LiveObjectCountViewer(adminClientContext));
    }
    liveObjectCountViewer.add(clusterModel);
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

    if (liveObjectCountViewer != null) {
      liveObjectCountViewer.remove(clusterModel);
      if (liveObjectCountViewer.isEmpty()) {
        tabbedPane.remove(liveObjectCountViewer);
        liveObjectCountViewer.tearDown();
        liveObjectCountViewer = null;
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
