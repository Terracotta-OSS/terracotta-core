/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;

public class DsoApplicationPanel extends ConfigurationEditorPanel {

  private final Layout m_layout;
  private State        m_state;

  public DsoApplicationPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
  }

  // ================================================================================
  // INTERFACE
  // ================================================================================

  public synchronized void clearState() {
    setActive(false);
    m_state.xmlContext.detachComponentModel(this);
    m_layout.m_bootClasses.clearState();
    m_layout.m_distributedMethods.clearState();
    m_layout.m_instrumentedClasses.clearState();
    m_layout.m_locks.clearState();
    m_layout.m_roots.clearState();
    m_layout.m_transientFields.clearState();
    m_state = null;
  }

  public synchronized void init(Object data) {
    if (m_isActive && m_state.project == (IProject) data) return;
    setActive(false);
    IProject project = (IProject) data;
    m_state = new State(project);
    m_layout.m_bootClasses.init(project);
    m_layout.m_distributedMethods.init(project);
    m_layout.m_instrumentedClasses.init(project);
    m_layout.m_locks.init(project);
    m_layout.m_roots.init(project);
    m_layout.m_transientFields.init(project);
    setActive(true);
  }

  public synchronized void refreshContent() {
    m_layout.m_bootClasses.refreshContent();
    m_layout.m_distributedMethods.refreshContent();
    m_layout.m_instrumentedClasses.refreshContent();
    m_layout.m_locks.refreshContent();
    m_layout.m_roots.refreshContent();
    m_layout.m_transientFields.refreshContent();
  }
  
  public void detach() {
    m_layout.m_bootClasses.detach();
    m_layout.m_distributedMethods.detach();
    m_layout.m_instrumentedClasses.detach();
    m_layout.m_locks.detach();
    m_layout.m_roots.detach();
    m_layout.m_transientFields.detach();
  }

  // ================================================================================
  // STATE
  // ================================================================================

  private class State {
    final IProject             project;
    final XmlConfigContext     xmlContext;
    final XmlConfigUndoContext xmlUndoContext;

    private State(IProject project) {
      this.project = project;
      this.xmlContext = XmlConfigContext.getInstance(project);
      this.xmlUndoContext = XmlConfigUndoContext.getInstance(project);
    }
  }

  // ================================================================================
  // LAYOUT
  // ================================================================================

  private class Layout {

    private static final int         MIN_HEIGHT                = 400;
    private static final String      ROOTS_ICON                = "/com/tc/admin/icons/hierarchicalLayout.gif";
    private static final String      LOCKS_ICON                = "/com/tc/admin/icons/deadlock_view.gif";
    private static final String      TRANSIENT_FIELDS_ICON     = "/com/tc/admin/icons/transient.gif";
    private static final String      INSTRUMENTED_CLASSES_ICON = "/com/tc/admin/icons/class_obj.gif";
    private static final String      DISTRIBUTED_METHODS_ICON  = "/com/tc/admin/icons/jmeth_obj.gif";
    private static final String      BOOT_CLASSES_ICON         = "/com/tc/admin/icons/jar_obj.gif";
    private static final String      ROOTS                     = "Roots";
    private static final String      LOCKS                     = "Locks";
    private static final String      TRANSIENT_FIELDS          = "Transient Fields";
    private static final String      INSTRUMENTED_CLASSES      = "Instrumented Classes";
    private static final String      DISTRIBUTED_METHODS       = "Distributed Methods";
    private static final String      BOOT_CLASSES              = "Boot Classes";
    private RootsPanel               m_roots;
    private LocksPanel               m_locks;
    private TransientFieldsPanel     m_transientFields;
    private InstrumentedClassesPanel m_instrumentedClasses;
    private DistributedMethodsPanel  m_distributedMethods;
    private BootClassesPanel         m_bootClasses;
    private final TabFolder          m_tabFolder;

    public void reset() {
    // not implemented
    }

    private Layout(Composite parent) {
      ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
      m_tabFolder = new TabFolder(scroll, SWT.BORDER);
      scroll.setContent(m_tabFolder);
      scroll.setExpandHorizontal(true);
      scroll.setExpandVertical(true);
      scroll.setMinHeight(MIN_HEIGHT);

      TabItem rootsTab = new TabItem(m_tabFolder, SWT.NONE);
      rootsTab.setText(ROOTS);
      rootsTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(ROOTS_ICON)));
      rootsTab.setControl(m_roots = new RootsPanel(m_tabFolder, SWT.NONE));

      TabItem locksTab = new TabItem(m_tabFolder, SWT.NONE);
      locksTab.setText(LOCKS);
      locksTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(LOCKS_ICON)));
      locksTab.setControl(m_locks = new LocksPanel(m_tabFolder, SWT.NONE));

      TabItem transientFieldsTab = new TabItem(m_tabFolder, SWT.NONE);
      transientFieldsTab.setText(TRANSIENT_FIELDS);
      transientFieldsTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(
          TRANSIENT_FIELDS_ICON)));
      transientFieldsTab.setControl(m_transientFields = new TransientFieldsPanel(m_tabFolder, SWT.NONE));

      TabItem instrumentedClassesTab = new TabItem(m_tabFolder, SWT.NONE);
      instrumentedClassesTab.setText(INSTRUMENTED_CLASSES);
      instrumentedClassesTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(
          INSTRUMENTED_CLASSES_ICON)));
      instrumentedClassesTab.setControl(m_instrumentedClasses = new InstrumentedClassesPanel(m_tabFolder, SWT.NONE));

      TabItem distributedMethodsTab = new TabItem(m_tabFolder, SWT.NONE);
      distributedMethodsTab.setText(DISTRIBUTED_METHODS);
      distributedMethodsTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(
          DISTRIBUTED_METHODS_ICON)));
      distributedMethodsTab.setControl(m_distributedMethods = new DistributedMethodsPanel(m_tabFolder, SWT.NONE));

      TabItem bootClassesTab = new TabItem(m_tabFolder, SWT.NONE);
      bootClassesTab.setText(BOOT_CLASSES);
      bootClassesTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(BOOT_CLASSES_ICON)));
      bootClassesTab.setControl(m_bootClasses = new BootClassesPanel(m_tabFolder, SWT.NONE));

      m_tabFolder.pack();
      m_tabFolder.setSelection(rootsTab);
    }
  }
}
