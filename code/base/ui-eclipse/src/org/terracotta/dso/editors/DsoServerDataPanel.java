/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.ContainerResource;

import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import com.terracottatech.configV2.DsoServerData;
import com.terracottatech.configV2.Server;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class DsoServerDataPanel extends ConfigurationEditorPanel
  implements ChangeListener,
             XmlObjectStructureListener
{
  private Server                 m_server;
  private DsoServerData          m_dsoServerData;
  private GarbageCollectionPanel m_garbageCollectionPanel; 
  private PersistencePanel       m_persistencePanel;
  
  public DsoServerDataPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);
    
    m_garbageCollectionPanel =
      (GarbageCollectionPanel)findComponent("GarbageCollectionPanel");
    m_persistencePanel =
      (PersistencePanel)findComponent("PersistencePanel");
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if(m_dsoServerData == null) {
      removeListeners();
      m_dsoServerData = m_server.addNewDso();
      updateChildren();
      addListeners();
    }
  }
  
  public boolean hasAnySet() {
    return m_dsoServerData != null &&
          (m_dsoServerData.isSetGarbageCollection() ||
           m_dsoServerData.isSetPersistence());
  }
  
  public void stateChanged(ChangeEvent e) {
    setDirty();    
  }
  
  public void structureChanged(XmlObjectStructureChangeEvent e) {
    syncModel();
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_server.getDso() != null) {
      m_server.unsetDso();
      m_dsoServerData = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    else {
      setDirty();
    }
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_server);    
  }
  
  private void addListeners() {
    m_garbageCollectionPanel.addXmlObjectStructureListener(this);
    m_persistencePanel.addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    m_garbageCollectionPanel.removeXmlObjectStructureListener(this);
    m_persistencePanel.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_garbageCollectionPanel.setup(m_dsoServerData);
    m_persistencePanel.setup(m_dsoServerData);
  }

  public void setup(Server server) {
    setEnabled(true);
    removeListeners();

    m_server        = server;
    m_dsoServerData = m_server != null ? m_server.getDso() : null;
    
    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();

    m_server        = null;
    m_dsoServerData = null;
    
    m_garbageCollectionPanel.tearDown();
    m_persistencePanel.tearDown();
    
    setEnabled(false);
  }
}
