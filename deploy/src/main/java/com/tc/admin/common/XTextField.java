/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JPopupMenu;
import javax.swing.JTextField;

public class XTextField extends JTextField {
  protected TextComponentHelper helper;

  public XTextField() {
    super();
    helper = createHelper();
  }

  public XTextField(String text) {
    this();
    setText(text);
  }

  protected TextComponentHelper createHelper() {
    return new TextComponentHelper(this);
  }

  protected JPopupMenu createPopup() {
    return helper.createPopup();
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    helper.setPopupMenu(popupMenu);
  }

  public JPopupMenu getPopupMenu() {
    return helper.getPopupMenu();
  }

  public void addNotify() {
    super.addNotify();
    if (getPopupMenu() == null) {
      setPopupMenu(createPopup());
    }
  }
}
