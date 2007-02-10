/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.ContainerResource;

import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import com.terracottatech.configV2.DsoClientData;
import com.terracottatech.configV2.DsoClientDebugging;

public class DsoClientDebuggingPanel extends ConfigurationEditorPanel
  implements XmlObjectStructureListener
{
  private DsoClientData               m_dsoClientData;
  private DsoClientDebugging          m_dsoClientDebugging;
  private InstrumentationLoggingPanel m_instrumentationLoggingPanel;
  private RuntimeLoggingPanel         m_runtimeLoggingPanel;
  private RuntimeOutputOptionsPanel   m_runtimeOutputOptionsPanel;
  
  public DsoClientDebuggingPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_instrumentationLoggingPanel = (InstrumentationLoggingPanel)
      findComponent("InstrumentationLogging");
    m_runtimeLoggingPanel = (RuntimeLoggingPanel)
      findComponent("RuntimeLogging");
    m_runtimeOutputOptionsPanel = (RuntimeOutputOptionsPanel)
      findComponent("RuntimeOutputOptions");
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();
    
    if(m_dsoClientDebugging == null) {
      removeListeners();
      m_dsoClientDebugging = m_dsoClientData.addNewDebugging();
      updateChildren();
      addListeners();
    }
  }
  
  public boolean hasAnySet() {
    return m_dsoClientDebugging != null &&
      (m_dsoClientDebugging.isSetInstrumentationLogging() ||
       m_dsoClientDebugging.isSetRuntimeLogging()         ||
       m_dsoClientDebugging.isSetRuntimeOutputOptions());
  }
  
  private void addListeners() {
    m_instrumentationLoggingPanel.addXmlObjectStructureListener(this);
    m_runtimeLoggingPanel.addXmlObjectStructureListener(this);
    m_runtimeOutputOptionsPanel.addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    m_instrumentationLoggingPanel.removeXmlObjectStructureListener(this);
    m_runtimeLoggingPanel.removeXmlObjectStructureListener(this);
    m_runtimeOutputOptionsPanel.removeXmlObjectStructureListener(this);
  }
  
  public void structureChanged(XmlObjectStructureChangeEvent e) {
    if(!hasAnySet() && m_dsoClientData.getDebugging() != null){
      m_dsoClientData.unsetDebugging();
      m_dsoClientDebugging = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    else {
      setDirty();
    }
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoClientData);
  }
    
  private void updateChildren() {
    m_instrumentationLoggingPanel.setup(m_dsoClientDebugging);
    m_runtimeLoggingPanel.setup(m_dsoClientDebugging);
    m_runtimeOutputOptionsPanel.setup(m_dsoClientDebugging);
  }
  
  public void setup(DsoClientData dsoClientData) {
    removeListeners();
    setEnabled(true);
    
    m_dsoClientData      = dsoClientData;
    m_dsoClientDebugging = m_dsoClientData != null ?
                           m_dsoClientData.getDebugging() : null;
    
    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();
    
    m_dsoClientData      = null;
    m_dsoClientDebugging = null;

    m_instrumentationLoggingPanel.tearDown();
    m_runtimeLoggingPanel.tearDown();
    m_runtimeOutputOptionsPanel.tearDown();

    setEnabled(false);
  }
}
