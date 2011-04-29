/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

public class DashboardViewer extends XContainer {
  private final ApplicationContext                 appContext;
  private final Map<IClusterModel, DashboardPanel> panelMap;
  private final PagedView                          pagedView;

  private static final String                      EMPTY_PAGE = "EmptyPage";

  public DashboardViewer(ApplicationContext appContext) {
    super(new BorderLayout());
    this.appContext = appContext;
    panelMap = new HashMap<IClusterModel, DashboardPanel>();
    add(pagedView = new PagedView(), BorderLayout.CENTER);

    XLabel nilPage = new XLabel();
    nilPage.setName(EMPTY_PAGE);
    pagedView.addPage(nilPage);
  }

  public void add(IClusterModel clusterModel) {
    DashboardPanel dashboardPanel = new DashboardPanel(appContext, clusterModel);
    panelMap.put(clusterModel, dashboardPanel);
    pagedView.addPage(dashboardPanel);
    select(clusterModel);
  }

  public void remove(IClusterModel clusterModel) {
    pagedView.addPage(panelMap.remove(clusterModel));
  }

  public void select(IClusterModel clusterModel) {
    String pageName = clusterModel.getName();
    if (!pagedView.hasPage(pageName)) {
      pageName = EMPTY_PAGE;
    }
    pagedView.setPage(pageName);
  }

  public boolean isEmpty() {
    return panelMap.isEmpty();
  }

  @Override
  public void tearDown() {
    panelMap.clear();
    super.tearDown();
  }
}
