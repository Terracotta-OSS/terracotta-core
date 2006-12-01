/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JTextPane;
import javax.swing.text.Document;

public class TextPaneUpdater implements Runnable {
  JTextPane m_view;
  String    m_line;

  static final String LINE_SEP = System.getProperty("line.separator"); 
    
  public TextPaneUpdater(JTextPane view) {
    m_view = view;
  }
    
  public TextPaneUpdater(JTextPane view, String text) {
    this(view);
    setLine(text);
  }
  
  void setLine(String line) {
    m_line = line;
  }
    
  public void run() {
    Document doc = m_view.getDocument();
      
    try {
      doc.insertString(doc.getLength(), m_line+LINE_SEP, null);
      m_view.setCaretPosition(doc.getLength()-1);
    } catch(Exception e) {/**/}
  }
}
