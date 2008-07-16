/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.TextField;

import javax.swing.JPopupMenu;

public class XTextField extends TextField {
  protected TextComponentHelper m_helper;

  public XTextField() {
    super();
    m_helper = createHelper();
  }

  protected TextComponentHelper createHelper() {
    return new TextComponentHelper(this);
  }

  protected JPopupMenu createPopup() {
    return m_helper.createPopup();
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    m_helper.setPopupMenu(popupMenu);
  }

  public JPopupMenu getPopupMenu() {
    return m_helper.getPopupMenu();
  }

  public void addNotify() {
    super.addNotify();
    if (getPopupMenu() == null) {
      setPopupMenu(createPopup());
    }
  }
}
