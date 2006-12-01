/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.Spinner;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

public class XSpinner extends Spinner {
  protected XPopupListener m_popupListener;
  
  public XSpinner() {
    super();
    m_popupListener = new XPopupListener(this);
    setPopupMenu(createPopup());
  }
  
  public void setEditor(JComponent editor) {
    super.setEditor(editor);
    m_popupListener.setTarget(editor);    
  }
  
  protected JPopupMenu createPopup() {
    return null;
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    m_popupListener.setPopupMenu(popupMenu);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupListener.getPopupMenu();
  }
}
