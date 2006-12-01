/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.dijon.CheckBox;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Label;
import org.dijon.List;
import org.dijon.ListModel;

public class InstrumentSuperTypesPanel extends Container {
  private Label     m_messageArea;
  private Container m_superTypesPanel;
  private Container m_superTypesStandin;
  private List      m_superTypesList;
  private Container m_bootTypesPanel;
  private Container m_bootTypesStandin;
  private List      m_bootTypesList;
  private CheckBox  m_restartToggle;
  
  public InstrumentSuperTypesPanel(ContainerResource res) {
    super(res);
  }

  public void load(ContainerResource res) {
    super.load(res);
    
    m_messageArea       = (Label)findComponent("MessageArea");
    m_superTypesPanel   = (Container)findComponent("SuperTypesPanel");
    m_superTypesList    = (List)m_superTypesPanel.findComponent("SuperTypesList");
    m_superTypesStandin = new Container();
    m_bootTypesPanel    = (Container)findComponent("BootTypesPanel");
    m_bootTypesList     = (List)m_bootTypesPanel.findComponent("BootTypesList");
    m_bootTypesStandin  = new Container();
    m_restartToggle     = (CheckBox)findComponent("RestartToggle");
  }
  
  public void setup(String         msg,
                    String         className,
                    java.util.List bootTypes,
                    java.util.List superTypes)
  {
    m_messageArea.setText(msg);
    
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
  
  public boolean restartSystem() {
    return m_restartToggle.isSelected();
  }
}
