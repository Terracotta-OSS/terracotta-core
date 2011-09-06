/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

public class XTextArea extends JTextArea {
  protected TextComponentHelper helper;

  public XTextArea() {
    super();
    helper = createHelper();
  }

  protected TextComponentHelper createHelper() {
    return new TextComponentHelper(this);
  }

  public JPopupMenu createPopup() {
    return helper.createPopup();
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    helper.setPopupMenu(popupMenu);
  }

  public JPopupMenu getPopupMenu() {
    return helper.getPopupMenu();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (getPopupMenu() == null) {
      setPopupMenu(createPopup());
    }
  }

  @Override
  public void setText(String text) {
    super.setText(text);
    setCaretPosition(0);
    moveCaretPosition(0);
  }
}
