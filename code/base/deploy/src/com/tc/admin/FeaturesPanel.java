/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XList;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultListModel;

public class FeaturesPanel extends XContainer {
  private IAdminClientContext adminClientContext;
  private IClusterModel       clusterModel;
  private XList               featureList;
  private DefaultListModel    featureListModel;

  public FeaturesPanel(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    add(new XScrollPane(featureList = new XList()));
    featureList.setModel(featureListModel = new DefaultListModel());
  }

  public void init(Map<String, Feature> featureMap) {
    Iterator<Map.Entry<String, Feature>> iter = featureMap.entrySet().iterator();
    while (iter.hasNext()) {
      add(iter.next().getValue());
    }
  }

  public void add(Feature feature) {
    if (!featureListModel.contains(feature)) {
      featureListModel.addElement(feature);
    }
  }

  public void remove(Feature feature) {
    featureListModel.removeElement(feature);
  }

  protected IAdminClientContext getAdminClientContext() {
    return adminClientContext;
  }

  protected IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      featureList.setModel(null);
      featureList = null;
      featureListModel.clear();
      featureListModel = null;
    }
  }
}
