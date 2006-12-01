/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.ContainerResource;
import org.dijon.Label;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;

import java.awt.Color;

import javax.swing.border.LineBorder;

public class StatusView extends XContainer {
  protected Label m_label;
  protected Label m_indicator;
    
  public StatusView() {
    super();
      
    AdminClientContext cntx = AdminClient.getContext();
    load((ContainerResource)cntx.topRes.getComponent("StatusRenderer"));
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);
    
    m_label     = (Label)findComponent("StatusLabel");
    m_indicator = (Label)findComponent("StatusIndicator");

    m_indicator.setOpaque(true);
    m_indicator.setBorder(LineBorder.createBlackLineBorder());
  }
  
  public void setLabel(String label) {
    m_label.setText(label);
  }
  
  public void setIndicator(Color color) {
    m_indicator.setBackground(color);
  }
  
  public void tearDown() {
    super.tearDown();
    
    m_label     = null;
    m_indicator = null;
  }
}
