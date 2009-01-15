/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.apache.commons.io.IOUtils;

import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTextField;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class ErrorDialog extends JDialog {
  private XTextArea  errorText;
  private XTextField messageText;
  private XButton    closeButton;

  public ErrorDialog() {
    super((Frame) null, true);

    setTitle("An Unexpected Error Has Occurred");

    Container cp = getContentPane();
    cp.setLayout(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    messageText = new XTextField();
    messageText.setEditable(false);
    cp.add(messageText, gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    XContainer tracePanel = new XContainer(new BorderLayout());
    errorText = new XTextArea();
    errorText.setEditable(false);
    tracePanel.add(new XScrollPane(errorText));
    tracePanel.setBorder(BorderFactory.createTitledBorder("Stracktrace:"));
    cp.add(tracePanel, gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = gbc.weighty = 0.0;
    closeButton = new XButton("Close");
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setVisible(false);
      }
    });
    cp.add(closeButton, gbc);
  }

  public ErrorDialog(String message, Throwable t) {
    this();

    if (t != null) {
      setError(t);
    }
    setMessage(message);
    pack();
  }

  public void setError(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    t.printStackTrace(pw);
    setErrorText(sw.toString());
    IOUtils.closeQuietly(pw);
  }

  public void setErrorText(String text) {
    errorText.setText(text);
  }

  public void setMessage(String text) {
    messageText.setText(text);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ErrorDialog d = new ErrorDialog("This is an error message", new RuntimeException());
        WindowHelper.center(d);
        d.setVisible(true);
      }
    });
  }
}
