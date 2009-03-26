/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;

public class SplashDialog extends JDialog {
  private XButton         importButton;
  private XButton         helpButton;
  private XButton         skipButton;
  private final XCheckBox noSplashToggle;

  public SplashDialog(JFrame parent, boolean modal) {
    super(parent, modal);

    setTitle(parent.getTitle());

    Container cp = getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    XLabel iconLabel = new XLabel();
    iconLabel.setIcon(new ImageIcon(getClass().getResource("/com/tc/admin/icons/logo.png")));
    cp.add(iconLabel, gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 1.0;

    JEditorPane textArea = new JEditorPane();
    try {
      textArea.setPage(getClass().getResource("ConfiguratorIntro.html"));
    } catch (IOException ioe) {
      textArea.setText(ioe.getLocalizedMessage());
    }
    textArea.setPreferredSize(new Dimension(500, 400));
    cp.add(new XScrollPane(textArea), gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;

    XContainer bottomPanel = new XContainer(new BorderLayout());
    XContainer buttonPanel = new XContainer(new GridLayout(0, 1, 1, 3));
    buttonPanel.add(importButton = new XButton("Import..."));
    buttonPanel.add(helpButton = new XButton("Help..."));
    buttonPanel.add(skipButton = new XButton("Skip"));
    bottomPanel.add(buttonPanel, BorderLayout.WEST);

    noSplashToggle = new XCheckBox("Don't show again");
    noSplashToggle.setSelected(false);
    bottomPanel.add(noSplashToggle, BorderLayout.EAST);
    cp.add(bottomPanel, gbc);

    pack();
  }

  public JButton getImportButton() {
    return importButton;
  }

  public JButton getHelpButton() {
    return helpButton;
  }

  public JButton getSkipButton() {
    return skipButton;
  }

  public JCheckBox getNoSplashToggle() {
    return noSplashToggle;
  }

}
