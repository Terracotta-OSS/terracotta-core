/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class XTextPane extends JTextPane {
  protected TextComponentHelper helper;

  public XTextPane() {
    super();
    helper = createHelper();
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

  public void append(String s) {
    Document doc = getDocument();
    if (doc != null) {
      try {
        if (s != null && s.length() > 0) {
          int len = doc.getLength();
          doc.insertString(len, s, null);
        }
      } catch (BadLocationException e) {
        UIManager.getLookAndFeel().provideErrorFeedback(this);
      }
    }
  }

  protected void testInitPopupMenu() {
    if (getPopupMenu() == null) {
      setPopupMenu(createPopup());
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    testInitPopupMenu();
  }
}
