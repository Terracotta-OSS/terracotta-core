/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.Item;
import org.dijon.Label;

import com.tc.admin.AdminClient;

import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

public class ProgressDialog extends Dialog {
  public ProgressDialog(Frame owner, String title, String msg) {
    super(owner, title, false);
    load((DialogResource) AdminClient.getContext().childResource("ProgressDialog"));
    setTitle(title);
    Label msgLabel = (Label) findComponent("MessageLabel");
    msgLabel.setFont(UIManager.getFont("TextPane.font"));
    msgLabel.setText(msg);
    msgLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    setUndecorated(true);
    setAlwaysOnTop(true);
    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    ((Item) findComponent("ProgressBarHolder")).add(progressBar);
  }
}
