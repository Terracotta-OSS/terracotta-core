/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Panel;

/**
 * Heavyweight java.awt.Panel that is the root for all our Swing-in-SWT objects.
 * This is different from its base type in that it doesn't clear the background
 * before painting.
 */
public class RootPanel extends Panel {
  public RootPanel() {
    super(new BorderLayout());
  }

  public void update(Graphics g) {
    if(isShowing()) {
      paint(g);
    }
  }
}
