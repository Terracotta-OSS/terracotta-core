/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

public class LogViewer extends XContainer {
  private final IAdminClientContext            adminClientContext;
  private final Map<IClusterModel, ClusterLog> logMap;
  private PagedView                            pagedView;

  private static final String                  EMPTY_PAGE = "EmptyPage";

  public LogViewer(IAdminClientContext adminClientContext) {
    super(new BorderLayout());
    this.adminClientContext = adminClientContext;
    logMap = new HashMap<IClusterModel, ClusterLog>();
    add(pagedView = new PagedView(), BorderLayout.CENTER);

    XLabel nilPage = new XLabel();
    nilPage.setName(EMPTY_PAGE);
    pagedView.addPage(nilPage);
  }

  public void add(IClusterModel clusterModel) {
    ClusterLog clusterLog = new ClusterLog(adminClientContext, clusterModel);
    logMap.put(clusterModel, clusterLog);
    // clusterLog.setName(clusterModel.toString());
    pagedView.addPage(clusterLog);
    select(clusterModel);
  }

  public void remove(IClusterModel clusterModel) {
    pagedView.remove(logMap.remove(clusterModel));
  }

  public boolean isEmpty() {
    return logMap.isEmpty();
  }

  public void select(IClusterModel clusterModel) {
    String pageName = clusterModel.toString();
    if (!pagedView.hasPage(pageName)) {
      pageName = EMPTY_PAGE;
    }
    pagedView.setPage(pageName);
  }
}
