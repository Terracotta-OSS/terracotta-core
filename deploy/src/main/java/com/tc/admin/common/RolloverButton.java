/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;

/**
 * A Button that paints its border when the mouse flies over.
 */

public class RolloverButton extends XButton {
  public RolloverButton() {
    super();
    setBorderPainted(false);
    setContentAreaFilled(false);
    addMouseListener(new FlyoverListener());
  }

  public RolloverButton(String text) {
    this();
    setText(text);
  }

  public RolloverButton(String text, Icon icon) {
    this();
    setText(text);
    setIcon(icon);
  }
  
  class FlyoverListener extends MouseAdapter {
    public void mouseEntered(MouseEvent me) {
      setBorderPainted(true);
      setContentAreaFilled(true);
    }

    public void mouseExited(MouseEvent me) {
      setBorderPainted(false);
      setContentAreaFilled(false);
    }
  }
}
