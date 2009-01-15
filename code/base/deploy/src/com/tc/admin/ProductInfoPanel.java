/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XContainer;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * This component is displayed on the ServerPanel after connecting to the associated server.
 */

public class ProductInfoPanel extends XContainer {
  private JLabel  version;
  private JLabel  copyright;
  private boolean visible;

  public ProductInfoPanel() {
    super(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    add(version = new JLabel(), gbc);
    gbc.gridy++;
    add(copyright = new JLabel(), gbc);
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
    if (visible) {
      Color fg = UIManager.getColor("Label.foreground");
      version.setForeground(fg);
      copyright.setForeground(fg);
    } else {
      init("foo", "", "bar");
      Color bg = getBackground();
      version.setForeground(bg);
      copyright.setForeground(bg);
    }
  }

  public void init(String versionText, String patchLevel, String copyrightText) {
    if (patchLevel != null && patchLevel.length() > 0) {
      versionText += ", Patch level " + patchLevel;
    }
    version.setText(versionText);
    copyright.setText(copyrightText);
  }

  public void tearDown() {
    super.tearDown();
    version = null;
    copyright = null;
  }
}
