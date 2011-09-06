/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.util.ProductInfo;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;

/**
 * TODO: i18n
 */

public class AboutDialog extends JDialog implements ActionListener {
  private JButton                okButton;
  private JLabel                 versionLabel;
  private JTextArea              sysInfoTextArea;

  private static final ImageIcon tcIcon = new ImageIcon(AboutDialog.class.getResource("/com/tc/admin/icons/logo.png"));

  public AboutDialog(Frame parent) {
    super(parent, true);

    setTitle("About " + parent.getTitle());
    setResizable(false);

    Container cp = getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);

    add(new XLabel(tcIcon), gbc);
    gbc.gridy++;
    add(versionLabel = new XLabel(), gbc);
    gbc.gridy++;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(5, 10, 5, 10);
    XContainer sysInfoPanel = new XContainer(new BorderLayout());
    sysInfoPanel.setBorder(BorderFactory.createTitledBorder("System Information"));
    sysInfoPanel.add(new XScrollPane(sysInfoTextArea = new XTextArea()));
    sysInfoTextArea.setEditable(false);
    add(sysInfoPanel, gbc);
    gbc.gridy++;
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    add(new XLabel("Copyright Terracotta, Inc. All rights reserved."), gbc);
    gbc.gridy++;
    add(okButton = new XButton("OK"), gbc);
    getRootPane().setDefaultButton(okButton);
    okButton.addActionListener(this);

    init(ProductInfo.getInstance());
  }

  public void actionPerformed(ActionEvent ae) {
    setVisible(false);
  }

  private String versionText(ProductInfo productInfo) {
    StringBuffer sb = new StringBuffer("<html><p>");
    sb.append(productInfo.toLongString());
    if (productInfo.isPatched()) {
      sb.append("<p style=\"text-align:center\">");
      sb.append(productInfo.toLongPatchString());
    }
    sb.append("</html>");
    return sb.toString();
  }

  private void init(ProductInfo productInfo) {
    String newLine = System.getProperty("line.separator");
    versionLabel.setText(versionText(productInfo));
    String osInfo = System.getProperty("os.name") + " (" + System.getProperty("os.version") + "/"
                    + System.getProperty("os.arch") + ")";
    String javaVersion = "Java " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
    String javaHomeDir = System.getProperty("java.home");
    String javaVMInfo = System.getProperty("java.vm.name") + ", " + System.getProperty("java.vm.version") + " ["
                        + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB]";
    sysInfoTextArea.setText(osInfo + newLine + javaVersion + newLine + javaHomeDir + newLine + javaVMInfo);
  }

}
