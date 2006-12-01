/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import org.dijon.ContainerResource;
import org.dijon.Label;

import com.tc.admin.common.XContainer;

/**
 * This component is displayed on the ServerPanel after connecting to the
 * associated server.
 */

public class ProductInfoPanel extends XContainer {
  private Label m_version;
  private Label m_copyright;

  public ProductInfoPanel() {
    super();
    
    AdminClientContext cc = AdminClient.getContext();
    load(cc.topRes.getComponent("ProductInfoPanel"));
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_version   = (Label)findComponent("Version");
    m_copyright = (Label)findComponent("Copyright");
  }
  
  public void init(ProductInfo productInfo) {
    m_version.setText(productInfo.getVersion());
    m_copyright.setText(productInfo.getCopyright());
  }
  
  public void tearDown() {
    super.tearDown();
    
    m_version   = null;
    m_copyright = null;
  }
}
