/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A Button that paints its border when the mouse flies over. 
 */

public class RolloverButton extends XButton {
  public RolloverButton() {
    super();
    setBorderPainted(false);
    addMouseListener(new FlyoverListener());
  }
  
  class FlyoverListener extends MouseAdapter {
    public void mouseEntered(MouseEvent me) {
      setBorderPainted(true);
    }
    public void mouseExited(MouseEvent me) {
      setBorderPainted(false);
    }
  }
}
