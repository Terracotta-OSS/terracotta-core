/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.TextComponentHelper;
import com.tc.admin.common.XTextPane;

import java.awt.Font;

import javax.swing.JPopupMenu;

public class LogPane extends XTextPane {
  public LogPane() {
    super();
    setEditable(false);
    setFont(Font.getFont("helvetica-plain-12"));
  }

  protected TextComponentHelper createHelper() {
    return new LogPaneHelper();
  }

  class LogPaneHelper extends TextComponentHelper {
    LogPaneHelper() {
      super(LogPane.this);
    }

    public JPopupMenu createPopup() {
      JPopupMenu popup = super.createPopup();
      addClearAction(popup);
      return popup;
    }
  }

}
