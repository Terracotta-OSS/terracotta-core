/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IServer;
import com.tc.admin.model.ServerLogListener;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ServerLog extends LogPane {
  private IServer                         m_server;
  private LogListener                     m_logListener;

  private static final SimpleAttributeSet m_errorIconAttrSet    = new SimpleAttributeSet();
  private static final SimpleAttributeSet m_warnIconAttrSet     = new SimpleAttributeSet();
  private static final SimpleAttributeSet m_infoIconAttrSet     = new SimpleAttributeSet();
  private static final SimpleAttributeSet m_blankIconAttrSet    = new SimpleAttributeSet();

  private static final String             LOG_ERROR             = AdminClient.getContext().getMessage("log.error");

  private static final String             LOG_WARN              = AdminClient.getContext().getMessage("log.warn");

  private static final String             LOG_INFO              = AdminClient.getContext().getMessage("log.info");

  private static final int                DEFAULT_MAX_LOG_LINES = 1000;

  private static int                      MAX_LOG_LINES         = Integer.getInteger("com.tc.admin.ServerLog.maxLines",
                                                                                     DEFAULT_MAX_LOG_LINES).intValue();

  static {
    StyleConstants.setIcon(m_errorIconAttrSet, LogHelper.getHelper().getErrorIcon());
    StyleConstants.setIcon(m_warnIconAttrSet, LogHelper.getHelper().getWarningIcon());
    StyleConstants.setIcon(m_infoIconAttrSet, LogHelper.getHelper().getInfoIcon());
    StyleConstants.setIcon(m_blankIconAttrSet, LogHelper.getHelper().getBlankIcon());
  }

  public ServerLog(IServer server) {
    super();
    m_server = server;
    server.addServerLogListener(m_logListener = new LogListener());
    setEditable(false);
  }

  public IServer getServer() {
    return m_server;
  }

  class LogListener implements ServerLogListener {
    public void messageLogged(String logMsg) {
      log(logMsg);
    }
  }

  public void log(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    e.printStackTrace(pw);
    pw.close();

    log(sw.toString());
  }

  public void append(String s) {
    StyledDocument doc = (StyledDocument) getDocument();

    try {
      int length = doc.getLength();
      AttributeSet iconAttrSet = m_blankIconAttrSet;

      if (s.indexOf(LOG_ERROR) != -1) {
        iconAttrSet = m_errorIconAttrSet;
      } else if (s.indexOf(LOG_WARN) != -1) {
        iconAttrSet = m_warnIconAttrSet;
      } else if (s.indexOf(LOG_INFO) != -1) {
        iconAttrSet = m_infoIconAttrSet;
      }

      appendToLog(doc, length, " ", iconAttrSet);
      length++;

      appendToLog(doc, length, s, null);
    } catch (BadLocationException e) {/**/
    }
  }

  private void appendToLog(StyledDocument doc, int offset, String s, AttributeSet attrSet) throws BadLocationException {
    doc.insertString(offset, s, attrSet);

    if (MAX_LOG_LINES > 0) {
      int lineCount;
      int length = doc.getLength();

      s = doc.getText(0, length);

      if ((lineCount = lineCount(s)) > MAX_LOG_LINES) {
        int lines = lineCount - MAX_LOG_LINES;

        offset = 0;

        for (int i = 0; i < lines; i++) {
          offset = s.indexOf(NEWLINE, offset);
          offset++;
        }

        doc.remove(0, offset);
      }
    }
  }

  private static final char NEWLINE = '\n';

  private static int lineCount(String s) {
    int result = 0;
    int offset = 0;

    if (s != null && s.length() > 0) {
      while ((offset = s.indexOf(NEWLINE, offset)) != -1) {
        result++;
        offset++;
      }
    }

    return result;
  }

  public void log(String s) {
    append(s);
    setCaretPosition(getDocument().getLength() - 1);
  }

  public void tearDown() {
    m_server.removeServerLogListener(m_logListener);
    m_logListener = null;
    m_server = null;
  }
}
