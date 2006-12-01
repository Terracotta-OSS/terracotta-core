/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import org.dijon.ContainerResource;
import org.dijon.Label;

import com.tc.admin.common.NullListSelectionModel;
import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.admin.common.XContainer;

/**
 * This component is displayed on the ServerPanel after connecting to the
 * associated server.
 */

public class AltProductInfoPanel extends XContainer {
  private PropertyTable      m_infoTable;
  private PropertyTableModel m_propertyModel;
  private Label              m_copyrightLabel;

  static final String[] PROPERTY_FIELDS = {
   "Version",
   "BuildID",
   "License"
  };
                                
  // TODO: i18n
  static final String[] PROPERTY_HEADERS = {
    "Version",
    "Build",
    "License"
  };
                                                              
  public AltProductInfoPanel() {
    super();
    
    AdminClientContext cc = AdminClient.getContext();
    load(cc.topRes.getComponent("AltProductInfoPanel"));

    m_infoTable.setSelectionModel(new NullListSelectionModel());
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_infoTable = (PropertyTable)findComponent("ProductInfoTable");
    m_propertyModel = m_infoTable.getPropertyModel();
    m_propertyModel.init(ProductInfo.class, PROPERTY_FIELDS, PROPERTY_HEADERS);
    
    m_copyrightLabel = (Label)findComponent("CopyrightLabel");
  }
  
  public void init(ProductInfo productInfo) {
    m_propertyModel.setInstance(productInfo);
    m_copyrightLabel.setText(productInfo.getCopyright());
  }
}
