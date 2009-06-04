/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

public class FeatureNode extends ComponentNode {
  protected Feature             feature;
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected FeaturePanel        featurePanel;
  protected ClientsNode         clientsNode;

  public FeatureNode(Feature feature, IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(feature.getDisplayName());

    this.feature = feature;
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
  }

  @Override
  public Component getComponent() {
    if (featurePanel == null) {
      adminClientContext.block();
      featurePanel = new FeaturePanel(feature, adminClientContext, clusterModel);
      adminClientContext.unblock();
    }
    return featurePanel;
  }

  public Feature getFeature() {
    return feature;
  }

  @Override
  public void tearDown() {
    if (featurePanel != null) {
      featurePanel.tearDown();
    }

    synchronized (this) {
      feature = null;
      adminClientContext = null;
      clusterModel = null;
      featurePanel = null;
      clientsNode = null;
    }

    super.tearDown();
  }
}
