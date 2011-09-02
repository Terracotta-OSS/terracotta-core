/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JTextPane;
import javax.swing.text.Document;

public class TextPaneUpdater implements Runnable {
  JTextPane                   view;
  String                      line;

  private static final String LINE_SEP = System.getProperty("line.separator");

  public TextPaneUpdater(JTextPane view) {
    this.view = view;
  }

  public TextPaneUpdater(JTextPane view, String text) {
    this(view);
    setLine(text);
  }

  void setLine(String line) {
    this.line = line;
  }

  public void run() {
    try {
      Document doc = view.getDocument();
      doc.insertString(doc.getLength(), line + LINE_SEP, null);
      view.setCaretPosition(doc.getLength() - 1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
