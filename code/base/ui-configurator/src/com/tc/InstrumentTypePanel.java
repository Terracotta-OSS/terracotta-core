/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.dijon.ButtonGroup;
import org.dijon.CheckBox;
import org.dijon.Component;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Label;
import org.dijon.List;
import org.dijon.ListModel;
import org.dijon.RadioButton;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InstrumentTypePanel extends Container {
  private Label       m_messageLabel;
  private ButtonGroup m_buttonGroup;
  private RadioButton m_classButton;
  private RadioButton m_packageButton;
  private CheckBox    m_restartToggle;
  private String      m_className;
  private String      m_packageName;
  private Container   m_superTypesPanel;
  private List        m_superTypesList;
  private Container   m_superTypesStandin;
  private Container   m_bootTypesPanel;
  private List        m_bootTypesList;
  private Container   m_bootTypesStandin;
  
  private static final String INCLUDE_ALL_PATTERN = "*..*";
  
  public InstrumentTypePanel(ContainerResource res) {
    super(res);
  }

  public void load(ContainerResource res) {
    super.load(res);
    
    m_messageLabel      = (Label)findComponent("MessageLabel");
    m_classButton       = (RadioButton)findComponent("ClassButton");
    m_buttonGroup       = (ButtonGroup)findComponent("ButtonGroup");
    m_packageButton     = (RadioButton)findComponent("PackageButton");
    m_superTypesPanel   = (Container)findComponent("SuperTypesPanel");
    m_superTypesStandin = new Container();
    m_superTypesList    = (List)m_superTypesPanel.findComponent("SuperTypesList");
    m_bootTypesPanel    = (Container)findComponent("BootTypesPanel");
    m_bootTypesStandin  = new Container();
    m_bootTypesList     = (List)m_bootTypesPanel.findComponent("BootTypesList");
    m_restartToggle     = (CheckBox)findComponent("RestartToggle");
    
    m_buttonGroup.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if(m_superTypesPanel.getParent() != null) {
          m_superTypesPanel.setEnabled(m_buttonGroup.getSelectedIndex() != 2);
        }
      }
    });
    
    int buttonCount = m_buttonGroup.getChildCount();
    for(int i = 0; i < buttonCount; i++) {
      Component comp   = m_buttonGroup.getChildAt(i);
      Insets    margin = new Insets(0,0,0,0);
      
      if(comp instanceof RadioButton) {
        ((RadioButton)comp).setMargin(margin);
      }
    }
  }
  
  public void setup(String msg,
                    String className,
                    java.util.List bootTypes,
                    java.util.List superTypes)
  {
    m_messageLabel.setText(msg);
    
    m_className   = className;
    m_packageName = className.substring(0, className.lastIndexOf('.'));
    
    m_classButton.setText("Type '"+className+"'");
    m_packageButton.setText("All types in package '"+m_packageName+"'");
    m_buttonGroup.setSelected("ClassButton");
    
    if(superTypes.size() > 0) {
      if(m_superTypesPanel.getParent() == null) {
        replaceChild(m_superTypesStandin, m_superTypesPanel);
      }
      m_superTypesList.setModel(new ListModel(superTypes));
    }
    else if(m_superTypesPanel.getParent() != null) {
      replaceChild(m_superTypesPanel, m_superTypesStandin);
    }

    if(bootTypes.size() > 0) {
      if(m_bootTypesPanel.getParent() == null) {
        replaceChild(m_bootTypesStandin, m_bootTypesPanel);
      }
      m_bootTypesList.setModel(new ListModel(bootTypes));
    }
    else if(m_bootTypesPanel.getParent() != null) {
      replaceChild(m_bootTypesPanel, m_bootTypesStandin);
    }
    
    m_restartToggle.setSelected(true);
  }
  
  public boolean instrumentClass() {
    return m_classButton.isSelected();
  }
  
  public String getPattern() {
    switch(m_buttonGroup.getSelectedIndex()) {
      case 0:  return m_className;
      case 1:  return m_packageName+".*";
      default: return INCLUDE_ALL_PATTERN;
    }
  }
  
  public boolean includeAll() {
    return getPattern().equals(INCLUDE_ALL_PATTERN);
  }
  
  public boolean restartSystem() {
    return m_restartToggle.isSelected();
  }
}
