/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.ContainerResource;
import org.dijon.Label;

import org.terracotta.dso.editors.xmlbeans.XmlBooleanToggle;
import org.terracotta.dso.editors.xmlbeans.XmlIntegerSpinner;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.GarbageCollection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GarbageCollectionPanel extends ConfigurationEditorPanel
  implements ActionListener,
             ChangeListener,
             XmlObjectStructureListener
{
  private DsoServerData     m_dsoServerData;
  private GarbageCollection m_garbageCollection;
  private XmlBooleanToggle  m_isGcEnabled;
  private XmlBooleanToggle  m_isVerboseGc;
  private XmlIntegerSpinner m_gcInterval;
  private Label             m_gcIntervalLabel;

  public GarbageCollectionPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);
    

    m_isGcEnabled = (XmlBooleanToggle)findComponent("IsGCEnabled");
    m_isGcEnabled.init(GarbageCollection.class, "enabled");

    m_isVerboseGc = (XmlBooleanToggle)findComponent("IsVerboseGC");
    m_isVerboseGc.init(GarbageCollection.class, "verbose");

    m_gcInterval = (XmlIntegerSpinner)findComponent("GCInterval");
    m_gcInterval.init(GarbageCollection.class, "interval");    
    
    m_gcIntervalLabel = (Label)findComponent("GCIntervalLabel");
    m_gcIntervalLabel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if(me.getClickCount() == 1) {
          m_gcInterval.unset();
        }
      }
    });
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if(m_garbageCollection == null) {
      removeListeners();
      m_garbageCollection = m_dsoServerData.addNewGarbageCollection();
      updateChildren();
      addListeners();
    }
  }
  
  public boolean hasAnySet() {
    return m_garbageCollection != null &&
          (m_garbageCollection.isSetEnabled() ||
           m_garbageCollection.isSetVerbose() ||
           m_garbageCollection.isSetInterval());
  }
  
  public void actionPerformed(ActionEvent ae) {
    setDirty();
  }
  
  public void stateChanged(ChangeEvent e) {
    setDirty();    
  }
  
  public void structureChanged(XmlObjectStructureChangeEvent e) {
    syncModel();
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_dsoServerData.getGarbageCollection() != null) {
      m_dsoServerData.unsetGarbageCollection();
      m_garbageCollection = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    else {
      setDirty();
    }
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoServerData);    
  }
  
  private void addListeners() {
    m_isGcEnabled.addActionListener(this);
    m_isGcEnabled.addXmlObjectStructureListener(this);

    m_isVerboseGc.addActionListener(this);
    m_isVerboseGc.addXmlObjectStructureListener(this);

    m_gcInterval.addChangeListener(this);
    m_gcInterval.addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    m_isGcEnabled.removeActionListener(this);
    m_isGcEnabled.removeXmlObjectStructureListener(this);

    m_isVerboseGc.removeActionListener(this);
    m_isVerboseGc.removeXmlObjectStructureListener(this);

    m_gcInterval.removeChangeListener(this);
    m_gcInterval.removeXmlObjectStructureListener(this);
  }
  

  private void updateChildren() {
    m_isGcEnabled.setup(m_garbageCollection);
    m_isVerboseGc.setup(m_garbageCollection);
    m_gcInterval.setup(m_garbageCollection);    
  }

  public void setup(DsoServerData dsoServerData) {
    setEnabled(true);
    removeListeners();

    m_dsoServerData     = dsoServerData;
    m_garbageCollection = m_dsoServerData != null ?
                          m_dsoServerData.getGarbageCollection() :
                          null;
    
    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();

    m_dsoServerData     = null;
    m_garbageCollection = null;
    
    m_isGcEnabled.tearDown();
    m_isVerboseGc.tearDown();
    m_gcInterval.tearDown();
    
    setEnabled(false);
  }
}
