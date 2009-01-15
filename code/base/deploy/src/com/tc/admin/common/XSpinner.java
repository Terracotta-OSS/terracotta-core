/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;

public class XSpinner extends JSpinner {
  protected XPopupListener popupListener;

  public XSpinner() {
    super();
    popupListener = new XPopupListener(this);
    setPopupMenu(createPopup());
  }

  public void setEditor(JComponent editor) {
    super.setEditor(editor);
    popupListener.setTarget(editor);
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
