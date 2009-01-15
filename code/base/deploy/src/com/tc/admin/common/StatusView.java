/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.border.LineBorder;

public class StatusView extends XContainer {
  protected XLabel label;
  protected XLabel indicator;

  public StatusView() {
    super(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 3, 1, 3);
    gbc.anchor = GridBagConstraints.WEST;

    indicator = new XLabel();
    indicator.setOpaque(true);
    indicator.setBorder(LineBorder.createBlackLineBorder());
    indicator.setMinimumSize(new Dimension(10, 10));
    indicator.setPreferredSize(new Dimension(10, 10));
    add(indicator, gbc);
    gbc.gridx++;

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    add(label = new XLabel(), gbc);
  }

  public XLabel getLabel() {
    return label;
  }

  public XLabel getIndicator() {
    return indicator;
  }

  public void setForeground(Color fg) {
    if(label != null) {
      label.setForeground(fg);
    }
    super.setForeground(fg);
  }
  
  public void setText(String text) {
    label.setText(text);
    revalidate();
    repaint();
  }

  public void setIndicator(Color color) {
    indicator.setBackground(color);
    indicator.setOpaque(true);
    indicator.setMinimumSize(new Dimension(10, 10));
    indicator.setPreferredSize(new Dimension(10, 10));
    revalidate();
    repaint();
  }

  public void tearDown() {
    super.tearDown();
    label = null;
    indicator = null;
  }
}
