/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XTextPane;
import com.tc.management.beans.L2MBeanNames;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ServerLog extends XTextPane {
  private ConnectionContext   m_cc;
  private ObjectName          m_logger;
  private LogListener         m_logListener;
  private Icon                m_errorIcon;
  private Icon                m_warnIcon;
  private Icon                m_infoIcon;
  private Icon                m_blankIcon;
  private SimpleAttributeSet  m_errorIconAttrSet;
  private SimpleAttributeSet  m_warnIconAttrSet;
  private SimpleAttributeSet  m_infoIconAttrSet;
  private SimpleAttributeSet  m_blankIconAttrSet;

  private static final String LOG_ERROR             = AdminClient.getContext().getMessage("log.error");

  private static final String LOG_WARN              = AdminClient.getContext().getMessage("log.warn");

  private static final String LOG_INFO              = AdminClient.getContext().getMessage("log.info");

  private static final int    DEFAULT_MAX_LOG_LINES = 1000;

  private static int          MAX_LOG_LINES         = Integer.getInteger("com.tc.admin.ServerLog.maxLines",
                                                                         DEFAULT_MAX_LOG_LINES).intValue();

  public ServerLog(ConnectionContext cc) {
    super();

    m_cc = cc;
    m_logListener = new LogListener();
    m_errorIcon = LogHelper.getHelper().getErrorIcon();
    m_warnIcon = LogHelper.getHelper().getWarningIcon();
    m_infoIcon = LogHelper.getHelper().getInfoIcon();
    m_blankIcon = LogHelper.getHelper().getBlankIcon();

    m_errorIconAttrSet = new SimpleAttributeSet();
    StyleConstants.setIcon(m_errorIconAttrSet, m_errorIcon);

    m_warnIconAttrSet = new SimpleAttributeSet();
    StyleConstants.setIcon(m_warnIconAttrSet, m_warnIcon);

    m_infoIconAttrSet = new SimpleAttributeSet();
    StyleConstants.setIcon(m_infoIconAttrSet, m_infoIcon);

    m_blankIconAttrSet = new SimpleAttributeSet();
    StyleConstants.setIcon(m_blankIconAttrSet, m_blankIcon);

    try {
      m_logger = m_cc.queryName(L2MBeanNames.LOGGER.getCanonicalName());
      m_cc.addNotificationListener(m_logger, m_logListener);
    } catch (Exception e) {
      AdminClient.getContext().log(e);
    }

    setEditable(false);
  }

  public ConnectionContext getConnectionContext() {
    return m_cc;
  }

  class LogListener implements NotificationListener {
    public void handleNotification(Notification notice, Object handback) {
      log(notice.getMessage());
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
}
