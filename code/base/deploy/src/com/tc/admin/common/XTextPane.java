/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.TextPane;

import javax.swing.JPopupMenu;

public class XTextPane extends TextPane {
  protected TextComponentHelper m_helper;
  
  public XTextPane() {
    super();
    m_helper = createHelper();
    setPopupMenu(createPopup());
  }
  
  protected TextComponentHelper createHelper() {
    return new TextComponentHelper(this); 
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
