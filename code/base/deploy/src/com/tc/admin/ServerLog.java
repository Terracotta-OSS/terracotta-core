/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
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
  private IServer                         server;
  private ServerListener                  serverListener;
  private LogListener                     logListener;

  private static final SimpleAttributeSet errorIconAttrSet      = new SimpleAttributeSet();
  private static final SimpleAttributeSet warnIconAttrSet       = new SimpleAttributeSet();
  private static final SimpleAttributeSet infoIconAttrSet       = new SimpleAttributeSet();
  private static final SimpleAttributeSet blankIconAttrSet      = new SimpleAttributeSet();

  private final String                    error;
  private final String                    warn;
  private final String                    info;

  private static final int                DEFAULT_MAX_LOG_LINES = 1000;

  private static int                      MAX_LOG_LINES         = Integer.getInteger("com.tc.admin.ServerLog.maxLines",
                                                                                     DEFAULT_MAX_LOG_LINES).intValue();

  static {
    StyleConstants.setIcon(errorIconAttrSet, LogHelper.getHelper().getErrorIcon());
    StyleConstants.setIcon(warnIconAttrSet, LogHelper.getHelper().getWarningIcon());
    StyleConstants.setIcon(infoIconAttrSet, LogHelper.getHelper().getInfoIcon());
    StyleConstants.setIcon(blankIconAttrSet, LogHelper.getHelper().getBlankIcon());
  }

  public ServerLog(ApplicationContext appContext, IServer server) {
    super();
    error = appContext.getString("log.error");
    warn = appContext.getString("log.warn");
    info = appContext.getString("log.info");
    this.server = server;
    if (server.isReady()) {
      server.addServerLogListener(logListener = new LogListener());
    }
    server.addPropertyChangeListener(serverListener = new ServerListener(server));
    setEditable(false);
    setName(server.toString());
  }

  private class ServerListener extends AbstractServerListener {
    private ServerListener(IServer server) {
      super(server);
    }

    @Override
    protected void handleReady() {
      if (server.isReady()) {
        if (logListener == null) {
          logListener = new LogListener();
        }
        server.addServerLogListener(logListener);
      } else {
        // server.removeServerLogListener(logListener);
      }
    }
  }

  public IServer getServer() {
    return server;
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

  @Override
  public void append(String s) {
    StyledDocument doc = (StyledDocument) getDocument();

    try {
      int length = doc.getLength();
      AttributeSet iconAttrSet = blankIconAttrSet;

      if (s.indexOf(error) != -1) {
        iconAttrSet = errorIconAttrSet;
      } else if (s.indexOf(warn) != -1) {
        iconAttrSet = warnIconAttrSet;
      } else if (s.indexOf(info) != -1) {
        iconAttrSet = infoIconAttrSet;
      }

      appendToLog(doc, length, " ", iconAttrSet);
      length++;

      appendToLog(doc, length, s, null);
    } catch (BadLocationException e) {/**/
    }
  }

  private void appendToLog(StyledDocument doc, int offset, String s, AttributeSet attrSet) throws BadLocationException {
    doc.insertString(offset, s, attrSet);

    if (MAX_LOG_LINES > 0 && getAutoScroll()) {
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

  public void tearDown() {
    server.removePropertyChangeListener(serverListener);
    server.removeServerLogListener(logListener);

    synchronized (this) {
      serverListener = null;
      logListener = null;
      server = null;
    }
  }
}
