/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlOptions;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Label;

import com.tc.admin.common.XTextArea;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.Servers;

import javax.swing.border.LineBorder;

public class PersistenceModeWarningPanel extends Container {
  private Label m_warningLabel;

  public PersistenceModeWarningPanel(String warning) {
    super();
    load(AdminClient.getContext().getComponent("PersistenceModeWarningPanel"));
    m_warningLabel.setText(warning);
  }

  public void load(ContainerResource res) {
    super.load(res);

    ((Label) findComponent("IconLabel")).setIcon(LogHelper.getHelper().getAlertIcon());

    m_warningLabel = (Label) findComponent("WarningLabel");
    
    XTextArea configSnippetText = (XTextArea) findComponent("ConfigSnippetText");
    Servers servers = Servers.Factory.newInstance();
    servers.addNewServer().addNewDso().addNewPersistence().setMode(PersistenceMode.PERMANENT_STORE);
    String configText = servers.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(3));
    configSnippetText.setColumns(maxLineWidth(configText));
    configSnippetText.setText(configText);
    configSnippetText.setFont(m_warningLabel.getFont());
    configSnippetText.setBorder(LineBorder.createBlackLineBorder());
  }

  private static int maxLineWidth(String source) {
    int result = 0;
    for(String s : StringUtils.split(source, System.getProperty("line.separator"))) {
      result = Math.max(result, s.length());
    }
    return result;
  }
  
  public void setWarningText(String warning) {
    m_warningLabel.setText(warning);
  }
}
