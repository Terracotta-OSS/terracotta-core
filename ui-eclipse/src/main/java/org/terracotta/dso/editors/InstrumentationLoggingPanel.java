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
import com.terracottatech.config.InstrumentationLogging;

public class InstrumentationLoggingPanel extends ConfigurationEditorPanel implements XmlObjectStructureListener {
  private DsoClientDebugging     m_dsoClientDebugging;
  private InstrumentationLogging m_instrumentationLogging;
  private final Layout           m_layout;

  public InstrumentationLoggingPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_instrumentationLogging == null) {
      removeListeners();
      m_instrumentationLogging = m_dsoClientDebugging.addNewInstrumentationLogging();
      updateChildren();
      addListeners();
    }
  }

  public void structureChanged(XmlObjectStructureChangeEvent ae) {
    testRemoveInstrumenationLogging();
  }

  private void testRemoveInstrumenationLogging() {
    if (!hasAnySet() && m_dsoClientDebugging.getInstrumentationLogging() != null) {
      m_dsoClientDebugging.unsetInstrumentationLogging();
      m_instrumentationLogging = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    fireClientChanged();
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoClientDebugging);
  }

  public boolean hasAnySet() {
    return m_instrumentationLogging != null
           && (m_instrumentationLogging.isSetClass1() || m_instrumentationLogging.isSetHierarchy()
               || m_instrumentationLogging.isSetLocks() || m_instrumentationLogging.isSetTransientRoot()
               || m_instrumentationLogging.isSetRoots() || m_instrumentationLogging.isSetDistributedMethods());
  }

  private void addListeners() {
    ((XmlBooleanToggle) m_layout.m_classCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_hierarchyCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_locksCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_transientRootCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_distributedMethodsCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_rootsCheck.getData()).addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    ((XmlBooleanToggle) m_layout.m_classCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_hierarchyCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_locksCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_transientRootCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_distributedMethodsCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle) m_layout.m_rootsCheck.getData()).removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_layout.setInstrumentationLogging(m_instrumentationLogging);
  }

  public void setup(DsoClientDebugging dsoClientDebugging) {
    removeListeners();
    setEnabled(true);

    m_dsoClientDebugging = dsoClientDebugging;
    m_instrumentationLogging = m_dsoClientDebugging != null ? m_dsoClientDebugging.getInstrumentationLogging() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_dsoClientDebugging = null;
    m_instrumentationLogging = null;

    ((XmlBooleanToggle) m_layout.m_classCheck.getData()).tearDown();
    ((XmlBooleanToggle) m_layout.m_hierarchyCheck.getData()).tearDown();
    ((XmlBooleanToggle) m_layout.m_locksCheck.getData()).tearDown();
    ((XmlBooleanToggle) m_layout.m_transientRootCheck.getData()).tearDown();
    ((XmlBooleanToggle) m_layout.m_distributedMethodsCheck.getData()).tearDown();
    ((XmlBooleanToggle) m_layout.m_rootsCheck.getData()).tearDown();

    setEnabled(false);
  }

  private class Layout {
    private static final String INSTRUMENTATION_LOGGING = "Instrumentation Logging";
    private static final String CLASS                   = "Class";
    private static final String HIERARCHY               = "Hierarchy";
    private static final String LOCKS                   = "Locks";
    private static final String TRANSIENT_ROOT          = "Transient Root";
    private static final String DISTRIBUTED_METHODS     = "Distributed Methods";
    private static final String ROOTS                   = "Roots";

    private final Button        m_classCheck;
    private final Button        m_hierarchyCheck;
    private final Button        m_locksCheck;
    private final Button        m_transientRootCheck;
    private final Button        m_distributedMethodsCheck;
    private final Button        m_rootsCheck;

    void setInstrumentationLogging(InstrumentationLogging instrumentationLogging) {
      ((XmlBooleanToggle) m_classCheck.getData()).setup(instrumentationLogging);
      ((XmlBooleanToggle) m_hierarchyCheck.getData()).setup(instrumentationLogging);
      ((XmlBooleanToggle) m_locksCheck.getData()).setup(instrumentationLogging);
      ((XmlBooleanToggle) m_transientRootCheck.getData()).setup(instrumentationLogging);
      ((XmlBooleanToggle) m_distributedMethodsCheck.getData()).setup(instrumentationLogging);
      ((XmlBooleanToggle) m_rootsCheck.getData()).setup(instrumentationLogging);
    }

    private Layout(Composite parent) {
      parent.setLayout(new GridLayout());

      Group instrumentationLoggingGroup = new Group(parent, SWT.SHADOW_NONE);
      instrumentationLoggingGroup.setText(INSTRUMENTATION_LOGGING);
      GridLayout gridLayout = new GridLayout();
      gridLayout.verticalSpacing = 3;
      instrumentationLoggingGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
      instrumentationLoggingGroup.setLayoutData(gridData);

      m_classCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_classCheck.setText(CLASS);
      initBooleanField(m_classCheck, InstrumentationLogging.class, "class1");

      m_hierarchyCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_hierarchyCheck.setText(HIERARCHY);
      initBooleanField(m_hierarchyCheck, InstrumentationLogging.class, "hierarchy");

      m_locksCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_locksCheck.setText(LOCKS);
      initBooleanField(m_locksCheck, InstrumentationLogging.class, "locks");

      m_transientRootCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_transientRootCheck.setText(TRANSIENT_ROOT);
      initBooleanField(m_transientRootCheck, InstrumentationLogging.class, "transient-root");

      m_distributedMethodsCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_distributedMethodsCheck.setText(DISTRIBUTED_METHODS);
      initBooleanField(m_distributedMethodsCheck, InstrumentationLogging.class, "distributed-methods");

      m_rootsCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_rootsCheck.setText(ROOTS);
      initBooleanField(m_rootsCheck, InstrumentationLogging.class, "roots");
    }
  }
}
