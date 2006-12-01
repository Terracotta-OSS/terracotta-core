/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.AbstractTableCellRenderer;
import org.dijon.Container;
import org.dijon.Label;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;

import javax.swing.JComponent;
import javax.swing.border.LineBorder;

/**
 * TODO: Extract a StatusView from this so it can be used elsewhere,
 * such as the ServerPanel, to indicate the server status.
 */

public abstract class StatusRenderer extends AbstractTableCellRenderer {
  protected Container m_statusRenderer;
  protected Label     m_label;
  protected Label     m_indicator;
    
  public StatusRenderer() {
    super();
      
    AdminClientContext cntx = AdminClient.getContext();
      
    m_statusRenderer = (Container)cntx.topRes.resolve("StatusRenderer");
    m_label          = (Label)m_statusRenderer.findComponent("StatusLabel");
    m_indicator      = (Label)m_statusRenderer.findComponent("StatusIndicator");

    m_label.setForeground(null);
    m_indicator.setOpaque(true);
    m_indicator.setBorder(LineBorder.createBlackLineBorder());
  }
    
  public JComponent getComponent() {
    return m_statusRenderer;
  }
}
