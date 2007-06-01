/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
import com.terracottatech.config.RuntimeLogging;

public class RuntimeLoggingPanel extends ConfigurationEditorPanel
  implements XmlObjectStructureListener
{
  private DsoClientDebugging m_dsoClientDebugging;
  private RuntimeLogging     m_runtimeLogging;
  private Layout             m_layout;

  public RuntimeLoggingPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if(m_runtimeLogging == null) {
      removeListeners();
      m_runtimeLogging = m_dsoClientDebugging.addNewRuntimeLogging();
      updateChildren();
      addListeners();
    }
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    testRemoveRuntimeLogging();
  }

  public boolean hasAnySet() {
    return m_runtimeLogging != null &&
          (m_runtimeLogging.isSetLockDebug()              ||
           m_runtimeLogging.isSetWaitNotifyDebug()        ||
           m_runtimeLogging.isSetDistributedMethodDebug() ||
           m_runtimeLogging.isSetNewObjectDebug());
  }

  private void testRemoveRuntimeLogging() {
    if(!hasAnySet() && m_dsoClientDebugging.getRuntimeLogging() != null) {
      m_dsoClientDebugging.unsetRuntimeLogging();
      m_runtimeLogging = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    fireClientChanged();
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoClientDebugging);
  }

  private void addListeners() {
    ((XmlBooleanToggle)m_layout.m_lockDebugCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_distributedMethodDebugCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_fieldChangeDebugCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_nonPortableDumpCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_waitNotifyDebugCheck.getData()).addXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_newObjectDebugCheck.getData()).addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    ((XmlBooleanToggle)m_layout.m_lockDebugCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_distributedMethodDebugCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_fieldChangeDebugCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_nonPortableDumpCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_waitNotifyDebugCheck.getData()).removeXmlObjectStructureListener(this);
    ((XmlBooleanToggle)m_layout.m_newObjectDebugCheck.getData()).removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_layout.setRuntimeLogging(m_runtimeLogging);
  }

  public void setup(DsoClientDebugging dsoClientDebugging) {
    removeListeners();
    setEnabled(true);

    m_dsoClientDebugging = dsoClientDebugging;
    m_runtimeLogging     = m_dsoClientDebugging != null ?
                           m_dsoClientDebugging.getRuntimeLogging() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_dsoClientDebugging = null;
    m_runtimeLogging = null;

    ((XmlBooleanToggle)m_layout.m_lockDebugCheck.getData()).tearDown();
    ((XmlBooleanToggle)m_layout.m_distributedMethodDebugCheck.getData()).tearDown();
    ((XmlBooleanToggle)m_layout.m_fieldChangeDebugCheck.getData()).tearDown();
    ((XmlBooleanToggle)m_layout.m_nonPortableDumpCheck.getData()).tearDown();
    ((XmlBooleanToggle)m_layout.m_waitNotifyDebugCheck.getData()).tearDown();
    ((XmlBooleanToggle)m_layout.m_newObjectDebugCheck.getData()).tearDown();

    setEnabled(false);
  }

  private class Layout {
    private static final String RUNTIME_LOGGING          = "Runtime Logging";
    private static final String LOCK_DEBUG               = "Lock Debug";
    private static final String DISTRIBUTED_METHOD_DEBUG = "Distributed Method Debug";
    private static final String FIELD_CHANGE_DEBUG       = "Field Change Debug";
    private static final String NON_PORTABLE_DUMP        = "Non-portable Dump";
    private static final String WAIT_NOTIFY_DEBUG        = "Wait Notify Debug";
    private static final String NEW_OBJECT_DEBUG         = "New Object Debug";

    private Button              m_lockDebugCheck;
    private Button              m_distributedMethodDebugCheck;
    private Button              m_fieldChangeDebugCheck;
    private Button              m_nonPortableDumpCheck;
    private Button              m_waitNotifyDebugCheck;
    private Button              m_newObjectDebugCheck;

    public void reset() {
      m_lockDebugCheck.setSelection(false);
      m_lockDebugCheck.setEnabled(false);
      m_distributedMethodDebugCheck.setSelection(false);
      m_distributedMethodDebugCheck.setEnabled(false);
      m_fieldChangeDebugCheck.setSelection(false);
      m_fieldChangeDebugCheck.setEnabled(false);
      m_nonPortableDumpCheck.setSelection(false);
      m_nonPortableDumpCheck.setEnabled(false);
      m_waitNotifyDebugCheck.setSelection(false);
      m_waitNotifyDebugCheck.setEnabled(false);
      m_newObjectDebugCheck.setSelection(false);
      m_newObjectDebugCheck.setEnabled(false);
    }

    void setRuntimeLogging(RuntimeLogging runtimeLogging) {
      ((XmlBooleanToggle)m_lockDebugCheck.getData()).setup(runtimeLogging);
      ((XmlBooleanToggle)m_distributedMethodDebugCheck.getData()).setup(runtimeLogging);
      ((XmlBooleanToggle)m_fieldChangeDebugCheck.getData()).setup(runtimeLogging);
      ((XmlBooleanToggle)m_nonPortableDumpCheck.getData()).setup(runtimeLogging);
      ((XmlBooleanToggle)m_waitNotifyDebugCheck.getData()).setup(runtimeLogging);
      ((XmlBooleanToggle)m_newObjectDebugCheck.getData()).setup(runtimeLogging);
    }

    private Layout(Composite parent) {
      parent.setLayout(new GridLayout());
      
      Group runtimeLoggingGroup = new Group(parent, SWT.SHADOW_NONE);
      runtimeLoggingGroup.setText(RUNTIME_LOGGING);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      runtimeLoggingGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
      runtimeLoggingGroup.setLayoutData(gridData);

      m_lockDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_lockDebugCheck.setText(LOCK_DEBUG);
      initBooleanField(m_lockDebugCheck, RuntimeLogging.class, "lock-debug");
      
      m_distributedMethodDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_distributedMethodDebugCheck.setText(DISTRIBUTED_METHOD_DEBUG);
      initBooleanField(m_distributedMethodDebugCheck, RuntimeLogging.class, "distributed-method-debug");

      m_fieldChangeDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_fieldChangeDebugCheck.setText(FIELD_CHANGE_DEBUG);
      initBooleanField(m_fieldChangeDebugCheck, RuntimeLogging.class, "field-change-debug");
      
      m_nonPortableDumpCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_nonPortableDumpCheck.setText(NON_PORTABLE_DUMP);
      initBooleanField(m_nonPortableDumpCheck, RuntimeLogging.class, "non-portable-dump");
      
      m_waitNotifyDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_waitNotifyDebugCheck.setText(WAIT_NOTIFY_DEBUG);
      initBooleanField(m_waitNotifyDebugCheck, RuntimeLogging.class, "wait-notify-debug");
      
      m_newObjectDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_newObjectDebugCheck.setText(NEW_OBJECT_DEBUG);
      initBooleanField(m_newObjectDebugCheck, RuntimeLogging.class, "new-object-debug");
    }
  }
}
