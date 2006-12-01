/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import com.tc.admin.common.OutputStreamListener;
import com.tc.admin.common.StreamReader;
import com.tc.admin.common.TextComponentHelper;
import com.tc.admin.common.TextPaneUpdater;
import com.tc.admin.common.XTextPane;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ProcessOutputView extends XTextPane {
  private StreamReader         m_errorStreamReader;
  private StreamReader         m_outputStreamReader;
  private String               m_listenerTrigger;
  private OutputStreamListener m_listener;
  
  public ProcessOutputView() {
    super();
  }

  protected TextComponentHelper createHelper() {
    return new Helper();
  }

  class Helper extends TextComponentHelper {
    Helper() {
      super(ProcessOutputView.this);
    }
    protected void addCutAction(JPopupMenu popup) {/**/}
    protected void addPasteAction(JPopupMenu popup) {/**/}
  }
  
  public void setListener(OutputStreamListener listener) {
    m_listener = listener;
    
    if(m_errorStreamReader != null) {
      m_errorStreamReader.setTriggerListener(m_listener);
    }
    if(m_outputStreamReader != null) {
      m_outputStreamReader.setTriggerListener(m_listener);
    }
  }
  
  public OutputStreamListener getListener() {
    return m_listener;
  }
  
  public void setListenerTrigger(String trigger) {
    m_listenerTrigger = trigger;
    
    if(m_errorStreamReader != null) {
      m_errorStreamReader.setTrigger(trigger);
    }
    if(m_outputStreamReader != null) {
      m_outputStreamReader.setTrigger(trigger);
    }
  }
  
  public String getListenerTrigger() {
    return m_listenerTrigger;
  }
  
  public void start(Process process) {
    if(m_errorStreamReader != null || m_outputStreamReader != null) {
      stop();
    }
    
    m_errorStreamReader = new StreamReader(process.getErrorStream(),
                                           new TextPaneUpdater(this),
                                           m_listener,
                                           m_listenerTrigger);
    m_errorStreamReader.start();
    
    m_outputStreamReader = new StreamReader(process.getInputStream(),
                                            new TextPaneUpdater(this),
                                            m_listener,
                                            m_listenerTrigger);
    m_outputStreamReader.start();
  }
  
  public void stop() {
    m_errorStreamReader.finish();
    m_outputStreamReader.finish();
  }

  public void append(String text) {
    SwingUtilities.invokeLater(new TextPaneUpdater(this, text));
  }
  
  public void stopAndClear() {
    stop();
    setContent("");
  }
  
  class TextClearer implements Runnable {
    public void run() {
      ProcessOutputView.this.setText("");
    }
  }

  public void setEnabled(boolean enabled) {
    String key = enabled ? "TextPane.foreground" : "TextPane.inactiveForeground";
    setForeground(UIManager.getColor(key));
  }
  
  public boolean isEnabled() {
    return true;
  }
}
