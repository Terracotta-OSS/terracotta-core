/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.TextArea;

import javax.swing.JPopupMenu;

public class XTextArea extends TextArea {
  protected TextComponentHelper m_helper;
  
  public XTextArea() {
    super();
    m_helper = new TextComponentHelper(this);
    setPopupMenu(createPopup());
  }
  
  private JPopupMenu createPopup() {
    return m_helper.createPopup();
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    m_helper.setPopupMenu(popupMenu);
  }

  public JPopupMenu getPopupMenu() {
    return m_helper.getPopupMenu();
  }
}
