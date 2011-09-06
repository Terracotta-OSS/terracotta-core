/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * A utility helper for components that want to have a context menu.
 */

public class XPopupListener extends MouseAdapter {
  protected JComponent target;
  protected JPopupMenu popupMenu;

  public XPopupListener() {
    super();
  }

  public XPopupListener(JComponent target) {
    this();
    this.target = target;
  }

  public void setTarget(JComponent target) {
    if (target != null) {
      if (popupMenu != null) {
        target.removeMouseListener(this);
        target.setComponentPopupMenu(null);
      }
    }

    if ((this.target = target) != null) {
      if (popupMenu != null) {
        target.setComponentPopupMenu(popupMenu);
        target.addMouseListener(this);
      }
    }
  }

  public void mousePressed(MouseEvent e) {
    testPopup(e);
  }

  public void mouseReleased(MouseEvent e) {
    testPopup(e);
  }

  public void testPopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
      popupMenu.show(target, e.getX(), e.getY());
    }
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    if (popupMenu != null) {
      if (target != null) {
        target.removeMouseListener(this);
        target.remove(popupMenu);
      }
    }

    if ((this.popupMenu = popupMenu) != null) {
      if (target != null) {
        target.add(popupMenu);
        target.addMouseListener(this);
      }
    }
  }

  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }
}
