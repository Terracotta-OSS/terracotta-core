/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.ContainerResource;

import org.terracotta.dso.editors.xmlbeans.XmlBooleanToggle;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import com.terracottatech.config.DsoClientDebugging;
import com.terracottatech.config.RuntimeLogging;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RuntimeLoggingPanel extends ConfigurationEditorPanel
  implements ActionListener,
             XmlObjectStructureListener
{
  private DsoClientDebugging m_dsoClientDebugging;
  private RuntimeLogging     m_runtimeLogging;
  private XmlBooleanToggle   m_lockDebug;
  private XmlBooleanToggle   m_waitNotifyDebug;
  private XmlBooleanToggle   m_distributedMethodDebug;
  private XmlBooleanToggle   m_newObjectDebug;

  public RuntimeLoggingPanel() {
    super();
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_lockDebug = (XmlBooleanToggle)findComponent("LockDebug");
    init(m_lockDebug, "lock-debug");

    m_waitNotifyDebug = (XmlBooleanToggle)findComponent("WaitNotifyDebug");
    init(m_waitNotifyDebug, "wait-notify-debug");

    m_distributedMethodDebug =
      (XmlBooleanToggle)findComponent("DistributedMethodDebug");
    init(m_distributedMethodDebug, "distributed-method-debug");

    m_newObjectDebug = (XmlBooleanToggle)findComponent("NewObjectDebug");
    init(m_newObjectDebug, "new-object-debug");
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
    syncModel();
  }

  public void actionPerformed(ActionEvent ae) {
    setDirty();
  }

  public boolean hasAnySet() {
    return m_runtimeLogging != null &&
          (m_runtimeLogging.isSetLockDebug()              ||
           m_runtimeLogging.isSetWaitNotifyDebug()        ||
           m_runtimeLogging.isSetDistributedMethodDebug() ||
           m_runtimeLogging.isSetNewObjectDebug());
  }

  private void syncModel() {
    if(!hasAnySet() && m_dsoClientDebugging.getRuntimeLogging() != null) {
      m_dsoClientDebugging.unsetRuntimeLogging();
      m_runtimeLogging = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    else {
      setDirty();
    }
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoClientDebugging);
  }

  private void addListeners() {
    m_lockDebug.addActionListener(this);
    m_lockDebug.addXmlObjectStructureListener(this);

    m_waitNotifyDebug.addActionListener(this);
    m_waitNotifyDebug.addXmlObjectStructureListener(this);

    m_distributedMethodDebug.addActionListener(this);
    m_distributedMethodDebug.addXmlObjectStructureListener(this);

    m_newObjectDebug.addActionListener(this);
    m_newObjectDebug.addXmlObjectStructureListener(this);
}

  private void removeListeners() {
    m_lockDebug.removeActionListener(this);
    m_lockDebug.removeXmlObjectStructureListener(this);

    m_waitNotifyDebug.removeActionListener(this);
    m_waitNotifyDebug.removeXmlObjectStructureListener(this);

    m_distributedMethodDebug.removeActionListener(this);
    m_distributedMethodDebug.removeXmlObjectStructureListener(this);

    m_newObjectDebug.removeActionListener(this);
    m_newObjectDebug.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    setup(m_lockDebug);
    setup(m_waitNotifyDebug);
    setup(m_distributedMethodDebug);
    setup(m_newObjectDebug);
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

    m_lockDebug.tearDown();
    m_waitNotifyDebug.tearDown();
    m_distributedMethodDebug.tearDown();
    m_newObjectDebug.tearDown();

    setEnabled(false);
  }

  private void init(XmlBooleanToggle toggle, String elementName) {
    toggle.init(RuntimeLogging.class, elementName);
  }

  private void setup(XmlBooleanToggle toggle) {
    toggle.setup(m_runtimeLogging);
  }
}
