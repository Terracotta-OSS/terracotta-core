/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;

public class XCheckBox extends JCheckBox {
  protected XPopupListener popupListener;

  public XCheckBox() {
    super();
    popupListener = new XPopupListener(this);
    JPopupMenu popup = createPopup();
    if (popup != null) {
      setPopupMenu(popup);
    }
  }

  public XCheckBox(String label) {
    this();
    setText(label);
  }

  protected JPopupMenu createPopup() {
    return null;
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    popupListener.setPopupMenu(popupMenu);
  }

  public JPopupMenu getPopupMenu() {
    return popupListener.getPopupMenu();
  }
}
