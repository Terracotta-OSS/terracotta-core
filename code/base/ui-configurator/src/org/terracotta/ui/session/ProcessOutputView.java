/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.OutputStreamListener;
import com.tc.admin.common.StreamReader;
import com.tc.admin.common.TextComponentHelper;
import com.tc.admin.common.TextPaneUpdater;
import com.tc.admin.common.XTextPane;

import java.awt.Font;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ProcessOutputView extends XTextPane {
  private StreamReader         errorStreamReader;
  private StreamReader         outputStreamReader;
  private String               listenerTrigger;
  private OutputStreamListener listener;
  
  public ProcessOutputView() {
    super();
    setEditable(false);
    setFont(new Font("monospaced", Font.PLAIN, 11));
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
    this.listener = listener;
    if(errorStreamReader != null) {
      errorStreamReader.setTriggerListener(listener);
    }
    if(outputStreamReader != null) {
      outputStreamReader.setTriggerListener(listener);
    }
  }
  
  public OutputStreamListener getListener() {
    return listener;
  }
  
  public void setListenerTrigger(String trigger) {
    listenerTrigger = trigger;
    if(errorStreamReader != null) {
      errorStreamReader.setTrigger(trigger);
    }
    if(outputStreamReader != null) {
      outputStreamReader.setTrigger(trigger);
    }
  }
  
  public String getListenerTrigger() {
    return listenerTrigger;
  }
  
  public void start(Process process) {
    if(errorStreamReader != null || outputStreamReader != null) {
      stop();
    }
    
    errorStreamReader = new StreamReader(process.getErrorStream(),
                                           new TextPaneUpdater(this),
                                           listener,
                                           listenerTrigger);
    errorStreamReader.start();
    
    outputStreamReader = new StreamReader(process.getInputStream(),
                                            new TextPaneUpdater(this),
                                            listener,
                                            listenerTrigger);
    outputStreamReader.start();
  }
  
  public void stop() {
    errorStreamReader.finish();
    outputStreamReader.finish();
  }

  public void append(String text) {
    SwingUtilities.invokeLater(new TextPaneUpdater(this, text));
  }
  
  public void stopAndClear() {
    stop();
    setText("");
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
