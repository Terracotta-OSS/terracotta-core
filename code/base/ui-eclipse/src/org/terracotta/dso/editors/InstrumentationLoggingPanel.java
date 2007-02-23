/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.ContainerResource;

import org.terracotta.dso.editors.xmlbeans.XmlBooleanToggle;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import com.terracottatech.config.DsoClientDebugging;
import com.terracottatech.config.InstrumentationLogging;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InstrumentationLoggingPanel extends ConfigurationEditorPanel
  implements ActionListener,
             XmlObjectStructureListener
{
  private DsoClientDebugging     m_dsoClientDebugging;
  private InstrumentationLogging m_instrumentationLogging;
  private XmlBooleanToggle       m_class;
  private XmlBooleanToggle       m_locks;
  private XmlBooleanToggle       m_transientRoot;
  private XmlBooleanToggle       m_roots;
  private XmlBooleanToggle       m_distributedMethods;

  public InstrumentationLoggingPanel() {
    super();
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_class = (XmlBooleanToggle)findComponent("Class1");
    init(m_class, "class1");

    m_locks = (XmlBooleanToggle)findComponent("Locks");
    init(m_locks, "locks");

    m_transientRoot = (XmlBooleanToggle)findComponent("TransientRoot");
    init(m_transientRoot, "transient-root");

    m_roots = (XmlBooleanToggle)findComponent("Roots");
    init(m_roots, "roots");

    m_distributedMethods =
      (XmlBooleanToggle)findComponent("DistributedMethods");
    init(m_distributedMethods, "distributed-methods");
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if(m_instrumentationLogging == null) {
      removeListeners();
      m_instrumentationLogging =
        m_dsoClientDebugging.addNewInstrumentationLogging();
      updateChildren();
      addListeners();
    }
  }

  public void actionPerformed(ActionEvent ae) {
    setDirty();
  }

  public void structureChanged(XmlObjectStructureChangeEvent ae) {
    syncModel();
  }

  private void syncModel() {
    if(!hasAnySet() &&
       m_dsoClientDebugging.getInstrumentationLogging() != null)
    {
      m_dsoClientDebugging.unsetInstrumentationLogging();
      m_instrumentationLogging = null;
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

  public boolean hasAnySet() {
    return m_instrumentationLogging != null &&
          (m_instrumentationLogging.isSetClass1()         ||
           m_instrumentationLogging.isSetHierarchy()      ||
           m_instrumentationLogging.isSetLocks()          ||
           m_instrumentationLogging.isSetTransientRoot()  ||
           m_instrumentationLogging.isSetRoots()          ||
           m_instrumentationLogging.isSetDistributedMethods());
  }

  private void addListeners() {
    m_class.addActionListener(this);
    m_class.addXmlObjectStructureListener(this);

    m_locks.addActionListener(this);
    m_locks.addXmlObjectStructureListener(this);

    m_transientRoot.addActionListener(this);
    m_transientRoot.addXmlObjectStructureListener(this);

    m_roots.addActionListener(this);
    m_roots.addXmlObjectStructureListener(this);

    m_distributedMethods.addActionListener(this);
    m_distributedMethods.addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    m_class.removeActionListener(this);
    m_class.removeXmlObjectStructureListener(this);

    m_locks.removeActionListener(this);
    m_locks.removeXmlObjectStructureListener(this);

    m_transientRoot.removeActionListener(this);
    m_transientRoot.removeXmlObjectStructureListener(this);

    m_roots.removeActionListener(this);
    m_roots.removeXmlObjectStructureListener(this);

    m_distributedMethods.removeActionListener(this);
    m_distributedMethods.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    setup(m_class);
    setup(m_locks);
    setup(m_transientRoot);
    setup(m_roots);
    setup(m_distributedMethods);
  }

  public void setup(DsoClientDebugging dsoClientDebugging) {
    removeListeners();
    setEnabled(true);

    m_dsoClientDebugging = dsoClientDebugging;
    m_instrumentationLogging =
      m_dsoClientDebugging != null ?
        m_dsoClientDebugging.getInstrumentationLogging() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_dsoClientDebugging     = null;
    m_instrumentationLogging = null;

    m_class.tearDown();
    m_locks.tearDown();
    m_transientRoot.tearDown();
    m_roots.tearDown();
    m_distributedMethods.tearDown();

    setEnabled(false);
  }

  private void init(XmlBooleanToggle toggle, String elementName) {
    toggle.init(InstrumentationLogging.class, elementName);
  }

  private void setup(XmlBooleanToggle toggle) {
    toggle.setup(m_instrumentationLogging);
  }
}
