/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.io.IOUtils;

import org.dijon.Dialog;
import org.dijon.DialogResource;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTextField;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDialog extends Dialog {
  private static DialogResource m_dialogRes;
  
  private XTextArea  m_errorText;
  private XTextField m_messageText;
  private XButton    m_closeButton;
  
  static {
    m_dialogRes =
      TcPlugin.getDefault().getResources().findDialog("ExceptionDialog"); 
  }
  
  public ExceptionDialog() {
    super(m_dialogRes);
    
    m_errorText   = (XTextArea)findComponent("ErrorText");
    m_messageText = (XTextField)findComponent("MessageText");
    m_closeButton = (XButton)findComponent("CloseButton");
    
    m_closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setVisible(false);
      }
    });
  }
  
  public ExceptionDialog(String message, Throwable t) {
    this();

    if(t != null) {
      setError(t);
    }
    setMessage(message);
  }

  public void setError(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter  pw = new PrintWriter(sw);
    
    t.printStackTrace(pw);
    setErrorText(sw.toString());
    IOUtils.closeQuietly(pw);
  }
  
  public void setErrorText(String errorText) {
    m_errorText.setText(errorText);
  }
  
  public void setMessage(String message) {
    m_messageText.setText(message);
  }
}
