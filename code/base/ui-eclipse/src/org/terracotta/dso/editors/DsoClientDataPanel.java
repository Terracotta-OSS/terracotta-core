/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.ContainerResource;

import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoClientData;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class DsoClientDataPanel extends ConfigurationEditorPanel
  implements ChangeListener,
             XmlObjectStructureListener
{
  private Client                  m_client;
  private DsoClientData           m_dsoClientData;
  private DsoClientDebuggingPanel m_dsoClientDebugging;

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_dsoClientDebugging =
      (DsoClientDebuggingPanel)findComponent("DsoClientDebugging");
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if(m_dsoClientData == null) {
      removeListeners();
      m_dsoClientData = m_client.addNewDso();
      updateChildren();
      addListeners();
    }
  }
  
  public boolean hasAnySet() {
    return m_dsoClientData != null && m_dsoClientData.isSetDebugging();
  }
  
  public void structureChanged(XmlObjectStructureChangeEvent e) {
    syncModel();
  }

  public void stateChanged(ChangeEvent e) {
    setDirty();
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_client.getDso() != null) {
      m_client.unsetDso();
      m_dsoClientData = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    else {
      setDirty();
    }
  }
  
  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_client);
  }
  
  private void addListeners() {
    m_dsoClientDebugging.addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    m_dsoClientDebugging.removeXmlObjectStructureListener(this);
  }
  
  private void updateChildren() {
    m_dsoClientDebugging.setup(m_dsoClientData);
  }
  
  public void setup(Client client) {
    removeListeners();
    setEnabled(true);
    
    m_client        = client;
    m_dsoClientData = m_client != null ? m_client.getDso() : null;
    
    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();
    m_dsoClientDebugging.tearDown();
    setEnabled(false);
  }
}
