/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlOptions;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTextArea;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.Servers;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.border.LineBorder;

public class PersistenceModeWarningPanel extends XContainer {
  private XLabel warningLabel;

  public PersistenceModeWarningPanel(ApplicationContext appContext, String warning) {
    super(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.NORTHWEST;

    XLabel iconLabel = new XLabel();
    iconLabel.setIcon(LogHelper.getHelper().getAlertIcon());
    add(iconLabel, gbc);
    gbc.gridx++;

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    add(warningLabel = new XLabel(warning), gbc);
    gbc.gridy++;
    gbc.anchor = GridBagConstraints.CENTER;

    XTextArea configSnippetText = new XTextArea();
    Servers servers = Servers.Factory.newInstance();
    servers.addNewServer().addNewDso().addNewPersistence().setMode(PersistenceMode.PERMANENT_STORE);
    String configText = servers.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(3));
    configSnippetText.setColumns(maxLineWidth(configText));
    configSnippetText.setEditable(false);
    configSnippetText.setText(configText);
    configSnippetText.setFont(warningLabel.getFont());
    configSnippetText.setBorder(LineBorder.createBlackLineBorder());
    add(configSnippetText, gbc);
  }

  private static int maxLineWidth(String source) {
    int result = 0;
    for (String s : StringUtils.split(source, System.getProperty("line.separator"))) {
      result = Math.max(result, s.length());
    }
    return result;
  }

  public void setWarningText(String warning) {
    warningLabel.setText(warning);
  }
}
