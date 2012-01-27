/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JLabel;

public class ArrowLabel extends JLabel {
  public ArrowLabel() {
    super(new ArrowIcon());
    setOpaque(false);
  }

  public void setDirection(int direction) {
    ((ArrowIcon) getIcon()).setDirection(direction);
  }

  public int getDirection() {
    return ((ArrowIcon) getIcon()).getDirection();
  }
}
