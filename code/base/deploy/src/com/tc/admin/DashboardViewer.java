/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

public class DashboardViewer extends XContainer {
  private ApplicationContext                            appContext;
  private Map<IClusterModel, DashboardPanel> panelMap;
  private PagedView                                     pagedView;

  public DashboardViewer(ApplicationContext appContext) {
    super(new BorderLayout());
    this.appContext = appContext;
    panelMap = new HashMap<IClusterModel, DashboardPanel>();
    add(pagedView = new PagedView(), BorderLayout.CENTER);
  }

  public void add(IClusterModel clusterModel) {
    DashboardPanel graphPanel = new DashboardPanel(appContext, clusterModel);
    panelMap.put(clusterModel, graphPanel);
    pagedView.addPage(graphPanel);
  }

  public void remove(IClusterModel clusterModel) {
    pagedView.addPage(panelMap.remove(clusterModel));
  }

  public void select(IClusterModel clusterModel) {
    pagedView.setPage(clusterModel.toString());
  }

  public boolean isEmpty() {
    return panelMap.isEmpty();
  }

  @Override
  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      appContext = null;
      panelMap.clear();
      panelMap = null;
      pagedView = null;
    }
  }
}
