/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;

public class XComboBox extends JComboBox {
  protected XPopupListener popupListener;

  public XComboBox() {
    super();
    popupListener = new XPopupListener(this);
    setPopupMenu(createPopup());
  }

  public XComboBox(ComboBoxModel aModel) {
    this();
    setModel(aModel);
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
