/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

public class LogViewer extends XContainer {
  private final IAdminClientContext            adminClientContext;
  private final Map<IClusterModel, ClusterLog> logMap;
  private PagedView                            pagedView;

  public LogViewer(IAdminClientContext adminClientContext) {
    super(new BorderLayout());
    this.adminClientContext = adminClientContext;
    logMap = new HashMap<IClusterModel, ClusterLog>();
    add(pagedView = new PagedView(), BorderLayout.CENTER);
  }

  public void add(IClusterModel clusterModel) {
    ClusterLog clusterLog = new ClusterLog(adminClientContext, clusterModel);
    logMap.put(clusterModel, clusterLog);
    pagedView.addPage(clusterLog);
  }

  public void remove(IClusterModel clusterModel) {
    pagedView.remove(logMap.remove(clusterModel));
  }

  public boolean isEmpty() {
    return logMap.isEmpty();
  }

  public void select(IClusterModel clusterModel) {
    pagedView.setPage(clusterModel.toString());
  }
}
