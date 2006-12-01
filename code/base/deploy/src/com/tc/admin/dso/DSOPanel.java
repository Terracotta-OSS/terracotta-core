/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.ContainerResource;
import org.dijon.Label;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.ProductInfo;
import com.tc.admin.ServerNode;
import com.tc.admin.common.XContainer;

public class DSOPanel extends XContainer {
  private Label m_version;
  private Label m_buildID;

  public DSOPanel(ConnectionContext cc) {
    super();
    
    AdminClientContext cntx = AdminClient.getContext();
    load((ContainerResource)cntx.topRes.getComponent("DSOPanel"));

    ProductInfo info;
    try {
      info = ServerNode.getProductInfo(cc);
    } catch(Exception e) {
      info = new ProductInfo();
    }
    init(info);
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_version = (Label)findComponent("Version");
    m_buildID = (Label)findComponent("BuildID");
  }
  
  public void init(ProductInfo productInfo) {
    m_version.setText(productInfo.getVersion());
    m_buildID.setText(productInfo.getBuildID());
  }
  
  public void tearDown() {
    super.tearDown();
    
    m_version = null;
    m_buildID = null;
  }
}
