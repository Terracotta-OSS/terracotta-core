/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

public class ProgressDialog extends JDialog {
  public ProgressDialog(Frame owner, String title, String msg) {
    super(owner, title, false);

    Container cp = getContentPane();
    cp.setLayout(new GridBagLayout());
//    ((JComponent) cp).setBorder(BorderFactory.createLoweredBevelBorder());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);

    JLabel label = new JLabel(msg);
    label.setFont(UIManager.getFont("TextPane.font"));
    label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    add(label, gbc);
    gbc.gridy++;

    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    add(progressBar, gbc);

    setAlwaysOnTop(true);
  }
}
