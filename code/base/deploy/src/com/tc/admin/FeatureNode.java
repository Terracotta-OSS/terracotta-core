/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

import javax.swing.JComponent;

public class FeatureNode extends ComponentNode {
  protected Feature             feature;
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected JComponent          featurePanel;
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
      featurePanel = new FeaturePanel(feature, adminClientContext, clusterModel);
    }
    return featurePanel;
  }

  public Feature getFeature() {
    return feature;
  }
}
