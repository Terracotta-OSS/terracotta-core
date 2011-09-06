/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTextField;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

public class ConnectPanel extends XContainer {
  private XTextField hostField;
  private XTextField portField;
  private XCheckBox  autoConnectToggle;
  private XButton    connectButton;

  public ConnectPanel(ApplicationContext appContext) {
    super(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);

    add(new XLabel(new ImageIcon(getClass().getResource("/com/tc/admin/icons/logo.png"))), gbc);
    gbc.gridy++;

    XContainer fieldPanel = new XContainer(new GridBagLayout());
    GridBagConstraints fgbc = new GridBagConstraints();
    fgbc.gridx = fgbc.gridy = 0;
    fgbc.insets = new Insets(3, 3, 3, 3);
    fgbc.fill = GridBagConstraints.HORIZONTAL;
    fieldPanel.add(new XLabel("Server host:", SwingConstants.RIGHT), fgbc);
    fgbc.gridx++;

    fieldPanel.add(hostField = new XTextField(), fgbc);
    hostField.setName("HostField");
    hostField.setColumns(22);
    fgbc.gridx = 0;
    fgbc.gridy++;

    fieldPanel.add(new XLabel("JMX port:", SwingConstants.RIGHT), fgbc);
    fgbc.gridx++;

    fieldPanel.add(portField = new XTextField(), fgbc);
    portField.setName("PortField");
    portField.setColumns(22);
    fgbc.gridy++;

    fieldPanel.add(autoConnectToggle = new XCheckBox("Connect automatically"), fgbc);
    autoConnectToggle.setName("AutoConnectToggle");
    fgbc.gridy++;

    fgbc.fill = GridBagConstraints.NONE;
    fgbc.anchor = GridBagConstraints.WEST;
    fieldPanel.add(connectButton = new XButton(appContext.getString("connect.elipses")), fgbc);
    connectButton.setName("ConnectButton");
    
    gbc.anchor = GridBagConstraints.WEST;
    add(fieldPanel, gbc);
  }

  public XTextField getHostField() {
    return hostField;
  }

  public XTextField getPortField() {
    return portField;
  }

  public XCheckBox getAutoConnectToggle() {
    return autoConnectToggle;
  }
  
  public XButton getConnectButton() {
    return connectButton;
  }

  public void setup(String host, int port, boolean autoConnect) {
    hostField.setText(host);
    portField.setText(Integer.toString(port));
    autoConnectToggle.setSelected(autoConnect);
    connectButton.setEnabled(true);
  }
}
