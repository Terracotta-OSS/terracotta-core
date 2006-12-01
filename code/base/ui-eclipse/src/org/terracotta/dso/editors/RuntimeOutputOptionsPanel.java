/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.ContainerResource;

import org.terracotta.dso.editors.xmlbeans.XmlBooleanToggle;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import com.terracottatech.configV2.DsoClientDebugging;
import com.terracottatech.configV2.RuntimeOutputOptions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RuntimeOutputOptionsPanel extends ConfigurationEditorPanel
  implements ActionListener,
             XmlObjectStructureListener
{
  private DsoClientDebugging   m_dsoClientDebugging;
  private RuntimeOutputOptions m_runtimeOutputOptions;
  private XmlBooleanToggle     m_autoLockDetails;
  private XmlBooleanToggle     m_caller;
  private XmlBooleanToggle     m_fullStack;
  
  public RuntimeOutputOptionsPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_autoLockDetails = (XmlBooleanToggle)findComponent("AutoLockDetails");
    init(m_autoLockDetails, "auto-lock-details");

    m_caller = (XmlBooleanToggle)findComponent("Caller");
    init(m_caller, "caller");

    m_fullStack = (XmlBooleanToggle)findComponent("FullStack");
    init(m_fullStack, "full-stack");
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();
    
    if(m_runtimeOutputOptions == null) {
      removeListeners();
      m_runtimeOutputOptions =
        m_dsoClientDebugging.addNewRuntimeOutputOptions();
      updateChildren();
      addListeners();
    }
  }
  
  public boolean hasAnySet() {
    return m_runtimeOutputOptions != null &&
          (m_runtimeOutputOptions.isSetAutoLockDetails() ||
           m_runtimeOutputOptions.isSetCaller()          ||
           m_runtimeOutputOptions.isSetFullStack());
  }

  private void syncModel() {
    if(!hasAnySet() &&
       m_dsoClientDebugging.getRuntimeOutputOptions() != null)
    {
      m_dsoClientDebugging.unsetRuntimeOutputOptions();
      m_runtimeOutputOptions = null;
      fireXmlObjectStructureChanged();     
    }
    else {
      setDirty();
    }
  }
  
  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoClientDebugging);
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    syncModel();
  }
  
  public void actionPerformed(ActionEvent ae) {
    setDirty();
  }

  private void addListeners() {
    m_autoLockDetails.addActionListener(this);
    m_autoLockDetails.addXmlObjectStructureListener(this);

    m_caller.addActionListener(this);
    m_caller.addXmlObjectStructureListener(this);

    m_fullStack.addActionListener(this);
    m_fullStack.addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    m_autoLockDetails.removeActionListener(this);
    m_autoLockDetails.removeXmlObjectStructureListener(this);

    m_caller.removeActionListener(this);
    m_caller.removeXmlObjectStructureListener(this);

    m_fullStack.removeActionListener(this);
    m_fullStack.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    setup(m_autoLockDetails);
    setup(m_caller);
    setup(m_fullStack);
  }
  
  public void setup(DsoClientDebugging dsoClientDebugging) {
    removeListeners();
    setEnabled(true);

    m_dsoClientDebugging   = dsoClientDebugging;
    m_runtimeOutputOptions = m_dsoClientDebugging != null ?
                             m_dsoClientDebugging.getRuntimeOutputOptions() :
                             null;   

    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();

    m_dsoClientDebugging   = null;
    m_runtimeOutputOptions = null;   
    
    m_autoLockDetails.tearDown();
    m_caller.tearDown();
    m_fullStack.tearDown();
    
    setEnabled(false);
  }
  
  private void init(XmlBooleanToggle toggle, String elementName) {
    toggle.init(RuntimeOutputOptions.class, elementName);
  }

  private void setup(XmlBooleanToggle toggle) {
    toggle.setup(m_runtimeOutputOptions);
  }
}
