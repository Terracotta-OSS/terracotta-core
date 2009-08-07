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
import org.terracotta.dso.editors.xmlbeans.XmlBooleanToggle;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.DsoClientDebugging;
import com.terracottatech.config.RuntimeOutputOptions;

public class RuntimeOutputOptionsPanel extends ConfigurationEditorPanel implements XmlObjectStructureListener {
  private DsoClientDebugging   m_dsoClientDebugging;
  private RuntimeOutputOptions m_runtimeOutputOptions;
  private final Layout         m_layout;

  public RuntimeOutputOptionsPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_runtimeOutputOptions == null) {
      removeListeners();
      m_runtimeOutputOptions = m_dsoClientDebugging.addNewRuntimeOutputOptions();
      updateChildren();
      addListeners();
    }
  }

  public boolean hasAnySet() {
    return m_runtimeOutputOptions != null
           && (m_runtimeOutputOptions.isSetAutoLockDetails() || m_runtimeOutputOptions.isSetCaller() || m_runtimeOutputOptions
               .isSetFullStack());
  }

  private void testRemoveRuntimeOutputOptions() {
    if (!hasAnySet() && m_dsoClientDebugging.getRuntimeOutputOptions() != null) {
      m_dsoClientDebugging.unsetRuntimeOutputOptions();
      m_runtimeOutputOptions = null;
      fireXmlObjectStructureChanged();
    }
    fireClientChanged();
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoClientDebugging);
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    testRemoveRuntimeOutputOptions();
  }

  private void addListeners() {
    ((XmlBooleanToggle) m_layout.m_autoLockDetailsCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_callerCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_fullStackCheck.getData()).addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    ((XmlBooleanToggle) m_layout.m_autoLockDetailsCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_callerCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_fullStackCheck.getData()).removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_layout.setRuntimeOutputOptions(m_runtimeOutputOptions);
  }

  public void setup(DsoClientDebugging dsoClientDebugging) {
    removeListeners();
    setEnabled(true);

    m_dsoClientDebugging = dsoClientDebugging;
    m_runtimeOutputOptions = m_dsoClientDebugging != null ? m_dsoClientDebugging.getRuntimeOutputOptions() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_dsoClientDebugging = null;
    m_runtimeOutputOptions = null;

    m_layout.tearDown();

    setEnabled(false);
  }

  private class Layout {
    private static final String RUNTIME_OUTPUT_OPTIONS = "Runtime Output Options";
    private static final String AUTOLOCK_DETAILS       = "Autolock Details";
    private static final String CALLER                 = "Caller";
    private static final String FULL_STACK             = "Full Stack";

    private final Button        m_autoLockDetailsCheck;
    private final Button        m_callerCheck;
    private final Button        m_fullStackCheck;

    void setRuntimeOutputOptions(RuntimeOutputOptions runtimeOutputOptions) {
      ((XmlBooleanToggle) m_autoLockDetailsCheck.getData()).setup(runtimeOutputOptions);
      ((XmlBooleanToggle) m_callerCheck.getData()).setup(runtimeOutputOptions);
      ((XmlBooleanToggle) m_fullStackCheck.getData()).setup(runtimeOutputOptions);
    }

    void tearDown() {
      ((XmlBooleanToggle) m_autoLockDetailsCheck.getData()).tearDown();
      ((XmlBooleanToggle) m_callerCheck.getData()).tearDown();
      ((XmlBooleanToggle) m_fullStackCheck.getData()).tearDown();
    }

    private Layout(Composite parent) {
      parent.setLayout(new GridLayout());

      Group runtimeOutputOptionsGroup = new Group(parent, SWT.SHADOW_NONE);
      runtimeOutputOptionsGroup.setText(RUNTIME_OUTPUT_OPTIONS);
      GridLayout gridLayout = new GridLayout();
      gridLayout.verticalSpacing = 3;
      runtimeOutputOptionsGroup.setLayout(gridLayout);
      runtimeOutputOptionsGroup
          .setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

      m_autoLockDetailsCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_autoLockDetailsCheck.setText(AUTOLOCK_DETAILS);
      initBooleanField(m_autoLockDetailsCheck, RuntimeOutputOptions.class, "auto-lock-details");

      m_callerCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_callerCheck.setText(CALLER);
      initBooleanField(m_callerCheck, RuntimeOutputOptions.class, "caller");

      m_fullStackCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_fullStackCheck.setText(FULL_STACK);
      initBooleanField(m_fullStackCheck, RuntimeOutputOptions.class, "full-stack");
    }
  }
}
