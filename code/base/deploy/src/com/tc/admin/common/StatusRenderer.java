/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.AbstractTableCellRenderer;
import org.dijon.Container;
import org.dijon.Label;

import com.tc.admin.AdminClient;

import javax.swing.JComponent;
import javax.swing.border.LineBorder;

public abstract class StatusRenderer extends AbstractTableCellRenderer {
  protected Container m_statusRenderer;
  protected Label     m_label;
  protected Label     m_indicator;

  public StatusRenderer() {
    super();

    m_statusRenderer = (Container) AdminClient.getContext().resolveResource("StatusRenderer");
    m_label = (Label) m_statusRenderer.findComponent("StatusLabel");
    m_indicator = (Label) m_statusRenderer.findComponent("StatusIndicator");

    m_label.setForeground(null);
    m_indicator.setOpaque(true);
    m_indicator.setBorder(LineBorder.createBlackLineBorder());
  }

  public JComponent getComponent() {
    return m_statusRenderer;
  }
}
