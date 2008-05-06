/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.dso.editors.xmlbeans.XmlStringEnumCombo;
import org.terracotta.ui.util.SWTLayout;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.Persistence;

public class PersistencePanel extends ConfigurationEditorPanel
  implements XmlObjectStructureListener
{
  private DsoServerData m_dsoServerData;
  private Persistence   m_persistence;
  private Layout        m_layout;
  
  public PersistencePanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if(m_persistence == null) {
      removeListeners();
      m_persistence = m_dsoServerData.addNewPersistence();
      updateChildren();
      addListeners();
    }
  }
  
  public boolean hasAnySet() {
    return m_persistence != null && m_persistence.isSetMode();
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    syncModel();
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_dsoServerData.getPersistence() != null) {
      m_dsoServerData.unsetPersistence();
      m_persistence = null;
//      fireXmlObjectStructureChanged();
//      updateChildren();
    }
    fireXmlObjectStructureChanged();
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoServerData);    
  }
  
  private void addListeners() {
    ((XmlStringEnumCombo)m_layout.m_persistenceModeCombo.getData()).addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    ((XmlStringEnumCombo)m_layout.m_persistenceModeCombo.getData()).removeXmlObjectStructureListener(this);
  }
  
  private void updateChildren() {
    m_layout.setPersistence(m_persistence);
  }

  public void setup(DsoServerData dsoServerData) {
    setEnabled(true);
    removeListeners();

    m_dsoServerData = dsoServerData;
    m_persistence   = m_dsoServerData != null ?
                      m_dsoServerData.getPersistence() : null;
    
    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();

    m_dsoServerData = null;
    m_persistence   = null;
    
    ((XmlStringEnumCombo)m_layout.m_persistenceModeCombo.getData()).tearDown();

    setEnabled(false);
  }
  
  private class Layout implements SWTLayout {
    private static final String PERSISTENCE_MODE   = "Mode";

    private Combo               m_persistenceModeCombo;

    public void reset() {
      resetServerFields(false);
    }

    private void setPersistence(Persistence persistence) {
      ((XmlStringEnumCombo)m_persistenceModeCombo.getData()).setup(persistence);
    }

    private void resetServerFields(boolean enabled) {
      m_persistenceModeCombo.deselectAll();
      m_persistenceModeCombo.setEnabled(enabled);
    }

    private Layout(Composite parent) {
      GridLayout gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      parent.setLayout(gridLayout);
      
      Group panel = new Group(parent, SWT.NONE);
      panel.setText("Persistence");
      gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 10;
      panel.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      panel.setLayoutData(gridData);

      Label persistenceModeLabel = new Label(panel, SWT.NONE);
      persistenceModeLabel.setText(PERSISTENCE_MODE);

      m_persistenceModeCombo = new Combo(panel, SWT.BORDER | SWT.READ_ONLY);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      m_persistenceModeCombo.setLayoutData(gridData);
      initStringEnumCombo(m_persistenceModeCombo, Persistence.class, "mode");
    }
  }
}
