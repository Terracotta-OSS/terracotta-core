/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.terracotta.dso.editors.xmlbeans.XmlIntegerSpinner;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.Client;
import com.terracottatech.config.DsoClientData;

public class DsoClientDataPanel extends ConfigurationEditorPanel implements XmlObjectStructureListener {
  private Client        m_client;
  private DsoClientData m_dsoClientData;
  private final Layout  m_layout;

  public DsoClientDataPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_dsoClientData == null) {
      removeListeners();
      m_dsoClientData = m_client.addNewDso();
      updateChildren();
      addListeners();
    }
  }

  public boolean hasAnySet() {
    return m_dsoClientData != null && m_dsoClientData.isSetDebugging()
           || ((XmlIntegerSpinner) m_layout.m_faultCountSpinner.getData()).isSet();
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    testUnsetDsoClientData();
  }

  private void testUnsetDsoClientData() {
    if (!hasAnySet() && m_client.getDso() != null) {
      m_client.unsetDso();
      m_dsoClientData = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    } else {
      fireClientChanged();
    }
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_client);
  }

  private void addListeners() {
    m_layout.m_dsoClientDebugging.addXmlObjectStructureListener(this);
    ((XmlIntegerSpinner) m_layout.m_faultCountSpinner.getData()).addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    m_layout.m_dsoClientDebugging.removeXmlObjectStructureListener(this);
    ((XmlIntegerSpinner) m_layout.m_faultCountSpinner.getData()).removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_layout.setDsoClientData(m_dsoClientData);
  }

  public void setup(Client client) {
    removeListeners();
    setEnabled(true);

    m_client = client;
    m_dsoClientData = m_client != null ? m_client.getDso() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();
    m_layout.m_dsoClientDebugging.tearDown();
    ((XmlIntegerSpinner) m_layout.m_faultCountSpinner.getData()).tearDown();
    setEnabled(false);
  }

  private class Layout {
    private static final String           DSO_CLIENT_DATA = "Dso Client Data";
    private static final String           FAULT_COUNT     = "Fault Count";

    private final DsoClientDebuggingPanel m_dsoClientDebugging;
    private Spinner                       m_faultCountSpinner;

    void setDsoClientData(DsoClientData dsoClientData) {
      m_dsoClientDebugging.setup(dsoClientData);
      ((XmlIntegerSpinner) m_faultCountSpinner.getData()).setup(dsoClientData);
    }

    private Layout(Composite parent) {
      parent.setLayout(new GridLayout());

      Group panel = new Group(parent, SWT.SHADOW_NONE);
      panel.setText(DSO_CLIENT_DATA);
      GridLayout gridLayout = new GridLayout();
      panel.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      panel.setLayoutData(gridData);

      m_dsoClientDebugging = new DsoClientDebuggingPanel(panel, SWT.NONE);
      m_dsoClientDebugging.setLayoutData(new GridData(GridData.FILL_BOTH));
      createFaultCountPane(panel);
    }

    private void createFaultCountPane(Composite parent) {
      Composite faultCountPane = new Composite(parent, SWT.NONE);
      faultCountPane.setLayout(new GridLayout(2, false));
      faultCountPane.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
      Label faultCountLabel = new Label(faultCountPane, SWT.NONE);
      faultCountLabel.setText(FAULT_COUNT);
      faultCountLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
      m_faultCountSpinner = new Spinner(faultCountPane, SWT.BORDER);
      m_faultCountSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
      initIntegerSpinnerField(m_faultCountSpinner, DsoClientData.class, "fault-count");
    }
  }
}
