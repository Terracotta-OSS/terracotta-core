/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.TextComponentHelper;
import com.tc.admin.common.XTextPane;

import java.awt.Font;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class LogPane extends XTextPane {
  private boolean autoScroll = true;

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

  public void setAutoScroll(boolean autoScroll) {
    if ((this.autoScroll = autoScroll) == true) {
      if (getDocument().getLength() > 0) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            // Would like to just use setCaretPosition here but the if
            // the model isn't changed, that will have no affect.
            // We're just adding a space, removing it, and then
            // setting the caret position.  Bummer.
            int end = getDocument().getLength();
            try {
              getDocument().insertString(end, " ", null);
              getDocument().remove(end, 1);
              setCaretPosition(end - 1);
            } catch (Exception e) {/**/
            }
          }
        });
      }
    }
  }

  public boolean getAutoScroll() {
    return autoScroll;
  }

  public void log(String s) {
    boolean doAutoScroll = getAutoScroll();
    append(s);
    if (doAutoScroll) {
      setCaretPosition(getDocument().getLength() - 1);
    }
  }
}
