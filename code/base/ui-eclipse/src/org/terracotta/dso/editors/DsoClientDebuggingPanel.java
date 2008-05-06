/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.DsoClientDebugging;

public class DsoClientDebuggingPanel extends ConfigurationEditorPanel
  implements XmlObjectStructureListener
{
  private DsoClientData      m_dsoClientData;
  private DsoClientDebugging m_dsoClientDebugging;
  private Layout             m_layout;

  public DsoClientDebuggingPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
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
    m_layout.m_instrumentationLoggingPanel.addXmlObjectStructureListener(this);
    m_layout.m_runtimeLoggingPanel.addXmlObjectStructureListener(this);
    m_layout.m_runtimeOutputOptionsPanel.addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    m_layout.m_instrumentationLoggingPanel.removeXmlObjectStructureListener(this);
    m_layout.m_runtimeLoggingPanel.removeXmlObjectStructureListener(this);
    m_layout.m_runtimeOutputOptionsPanel.removeXmlObjectStructureListener(this);
  }
  
  public void structureChanged(XmlObjectStructureChangeEvent e) {
    if(!hasAnySet() && m_dsoClientData.getDebugging() != null){
      m_dsoClientData.unsetDebugging();
      m_dsoClientDebugging = null;
      fireXmlObjectStructureChanged();
    }
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoClientData);
  }
    
  private void updateChildren() {
    m_layout.setup(m_dsoClientDebugging);
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

    m_layout.m_instrumentationLoggingPanel.tearDown();
    m_layout.m_runtimeLoggingPanel.tearDown();
    m_layout.m_runtimeOutputOptionsPanel.tearDown();

    setEnabled(false);
  }
  
  private class Layout {
    private InstrumentationLoggingPanel m_instrumentationLoggingPanel;
    private RuntimeLoggingPanel         m_runtimeLoggingPanel;
    private RuntimeOutputOptionsPanel   m_runtimeOutputOptionsPanel;

    void setup(DsoClientDebugging dsoClientDebugging) {
      m_instrumentationLoggingPanel.setup(m_dsoClientDebugging);
      m_runtimeLoggingPanel.setup(m_dsoClientDebugging);
      m_runtimeOutputOptionsPanel.setup(m_dsoClientDebugging);
    }
    
    private Layout(Composite parent) {
      parent.setLayout(new GridLayout());
      
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout(3, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));
      
      m_instrumentationLoggingPanel = new InstrumentationLoggingPanel(comp, SWT.NONE);
      m_instrumentationLoggingPanel.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, false));
      
      m_runtimeLoggingPanel = new RuntimeLoggingPanel(comp, SWT.NONE);
      m_runtimeLoggingPanel.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));
      
      m_runtimeOutputOptionsPanel = new RuntimeOutputOptionsPanel(comp, SWT.NONE);
      m_runtimeOutputOptionsPanel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));
    }
  }
}
