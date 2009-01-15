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

public class LiveObjectCountViewer extends XContainer {
  private ApplicationContext                            appContext;
  private Map<IClusterModel, LiveObjectCountGraphPanel> graphPanelMap;
  private PagedView                                     pagedView;

  public LiveObjectCountViewer(ApplicationContext appContext) {
    super(new BorderLayout());
    this.appContext = appContext;
    graphPanelMap = new HashMap<IClusterModel, LiveObjectCountGraphPanel>();
    add(pagedView = new PagedView(), BorderLayout.CENTER);
  }

  public void add(IClusterModel clusterModel) {
    LiveObjectCountGraphPanel graphPanel = new LiveObjectCountGraphPanel(appContext, clusterModel);
    graphPanelMap.put(clusterModel, graphPanel);
    pagedView.addPage(graphPanel);
  }

  public void remove(IClusterModel clusterModel) {
    pagedView.addPage(graphPanelMap.remove(clusterModel));
  }

  public void select(IClusterModel clusterModel) {
    pagedView.setPage(clusterModel.toString());
  }

  public boolean isEmpty() {
    return graphPanelMap.isEmpty();
  }

  @Override
  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      appContext = null;
      graphPanelMap.clear();
      graphPanelMap = null;
      pagedView = null;
    }
  }
}
