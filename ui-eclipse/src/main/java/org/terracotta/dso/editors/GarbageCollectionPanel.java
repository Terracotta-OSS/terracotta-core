/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.terracotta.dso.editors.xmlbeans.XmlBooleanToggle;
import org.terracotta.dso.editors.xmlbeans.XmlIntegerSpinner;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.ui.util.SWTLayout;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.GarbageCollection;

public class GarbageCollectionPanel extends ConfigurationEditorPanel
  implements XmlObjectStructureListener
{
  private DsoServerData     m_dsoServerData;
  private GarbageCollection m_garbageCollection;
  private Layout            m_layout;

  public GarbageCollectionPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
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
  
  public void structureChanged(XmlObjectStructureChangeEvent e) {
    testUnsetGarbageCollection();
  }
  
  private void testUnsetGarbageCollection() {
    if(!hasAnySet() && m_dsoServerData.getGarbageCollection() != null) {
      m_dsoServerData.unsetGarbageCollection();
      m_garbageCollection = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    else {
      fireServerChanged();
    }
  }

  void fireServerChanged() {
    DsoServerDataPanel panel = (DsoServerDataPanel)SWTUtil.getAncestorOfClass(DsoServerDataPanel.class, this);
    panel.fireServerChanged();
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoServerData);    
  }
  
  private void addListeners() {
    ((XmlBooleanToggle)m_layout.m_gcCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlIntegerSpinner)m_layout.m_gcIntervalSpinner.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_verboseCheck.getData()).addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    ((XmlBooleanToggle)m_layout.m_gcCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlIntegerSpinner)m_layout.m_gcIntervalSpinner.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_verboseCheck.getData()).removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_layout.setGarbageCollection(m_garbageCollection);    
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
    
    m_layout.tearDown();

    setEnabled(false);
  }

  private class Layout implements SWTLayout {
    private static final String GARBAGE_COLLECTION = "Garbage Collection";
    private static final String GC_ENABLED         = "Enabled";
    private static final String VERBOSE            = "Verbose";
    private static final String GC_INTERVAL        = "GC Interval (seconds)";

    private Button              m_gcCheck;
    private Spinner             m_gcIntervalSpinner;
    private Button              m_verboseCheck;

    public void reset() {
      setGarbageCollection(null);
    }

    private void setGarbageCollection(GarbageCollection gc) {
      ((XmlBooleanToggle)m_gcCheck.getData()).setup(gc);
      ((XmlIntegerSpinner)m_gcIntervalSpinner.getData()).setup(gc);
      ((XmlBooleanToggle)m_verboseCheck.getData()).setup(gc);
    }

    void tearDown() {
      ((XmlBooleanToggle)m_gcCheck.getData()).tearDown();
      ((XmlIntegerSpinner)m_gcIntervalSpinner.getData()).tearDown();
      ((XmlBooleanToggle)m_verboseCheck.getData()).tearDown();
    }
    
    private Layout(Composite parent) {
      GridLayout gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      parent.setLayout(gridLayout);
      
      Group panel = new Group(parent, SWT.NONE);
      panel.setText(GARBAGE_COLLECTION);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 10;
      panel.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      panel.setLayoutData(gridData);

      m_gcCheck = new Button(panel, SWT.CHECK);
      m_gcCheck.setText(GC_ENABLED);
      initBooleanField(m_gcCheck, GarbageCollection.class, "enabled");

      Composite intervalPanel = new Composite(panel, SWT.NONE);
      gridLayout = new GridLayout(2, false);
      intervalPanel.setLayout(gridLayout);
      Label gcIntervalLabel = new Label(intervalPanel, SWT.NONE);
      gcIntervalLabel.setText(GC_INTERVAL);

      m_gcIntervalSpinner = new Spinner(intervalPanel, SWT.BORDER);
      gridData = new GridData();
      m_gcIntervalSpinner.setLayoutData(gridData);
      initIntegerSpinnerField(m_gcIntervalSpinner, GarbageCollection.class, "interval");

      m_verboseCheck = new Button(panel, SWT.CHECK);
      m_verboseCheck.setText(VERBOSE);
      initBooleanField(m_verboseCheck, GarbageCollection.class, "verbose");
    }
  }
}
