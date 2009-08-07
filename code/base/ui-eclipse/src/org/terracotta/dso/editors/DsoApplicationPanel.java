/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;

import com.terracottatech.config.Application;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class DsoApplicationPanel extends ConfigurationEditorPanel implements ConfigurationEditorRoot,
    XmlObjectStructureListener {
  private IProject       m_project;
  private TcConfig       m_config;
  private Application    m_application;
  private DsoApplication m_dsoApp;
  private final Layout   m_layout;

  public DsoApplicationPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {/**/
  }

  private void addListeners() {
    m_layout.m_instrumentedClasses.addXmlObjectStructureListener(this);
    m_layout.m_transientFields.addXmlObjectStructureListener(this);
    m_layout.m_locks.addXmlObjectStructureListener(this);
    m_layout.m_roots.addXmlObjectStructureListener(this);
    m_layout.m_distributedMethods.addXmlObjectStructureListener(this);
    m_layout.m_bootClasses.addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    m_layout.m_instrumentedClasses.removeXmlObjectStructureListener(this);
    m_layout.m_transientFields.removeXmlObjectStructureListener(this);
    m_layout.m_locks.removeXmlObjectStructureListener(this);
    m_layout.m_roots.removeXmlObjectStructureListener(this);
    m_layout.m_distributedMethods.removeXmlObjectStructureListener(this);
    m_layout.m_bootClasses.removeXmlObjectStructureListener(this);
  }

  @Override
  public void ensureXmlObject() {
    if (m_dsoApp == null) {
      removeListeners();
      if (m_application == null) {
        m_application = m_config.addNewApplication();
      }
      m_dsoApp = m_application.addNewDso();
      initPanels();
      addListeners();
    }
  }

  public void setupInternal() {
    TcPlugin plugin = TcPlugin.getDefault();

    m_config = plugin.getConfiguration(m_project);
    m_application = m_config != null ? m_config.getApplication() : null;
    m_dsoApp = m_application != null ? m_application.getDso() : null;

    initPanels();
  }

  private void initPanels() {
    m_layout.m_instrumentedClasses.setup(m_project, m_dsoApp);
    m_layout.m_transientFields.setup(m_project, m_dsoApp);
    m_layout.m_locks.setup(m_project, m_dsoApp);
    m_layout.m_roots.setup(m_project, m_dsoApp);
    m_layout.m_distributedMethods.setup(m_project, m_dsoApp);
    m_layout.m_bootClasses.setup(m_project, m_dsoApp);
  }

  public void updateInstrumentedClassesPanel() {
    m_layout.m_instrumentedClasses.updateModel();
  }

  public void updateTransientsPanel() {
    m_layout.m_transientFields.updateModel();
  }

  public void updateLocksPanel() {
    m_layout.m_locks.updateModel();
  }

  public void updateRootsPanel() {
    m_layout.m_roots.updateModel();
  }

  public void updateDistributedMethodsPanel() {
    m_layout.m_distributedMethods.updateModel();
  }

  public void updateBootClassesPanel() {
    m_layout.m_bootClasses.updateModel();
  }

  public void setup(IProject project) {
    m_project = project;

    setEnabled(true);
    removeListeners();
    setupInternal();
    addListeners();
  }

  @Override
  public IProject getProject() {
    return m_project;
  }

  public void tearDown() {
    removeListeners();

    m_layout.m_instrumentedClasses.tearDown();
    m_layout.m_transientFields.tearDown();
    m_layout.m_locks.tearDown();
    m_layout.m_roots.tearDown();
    m_layout.m_distributedMethods.tearDown();
    m_layout.m_bootClasses.tearDown();

    setEnabled(false);
  }

  private static class Layout {
    private static final String            ROOTS_ICON                = "/com/tc/admin/icons/hierarchicalLayout.gif";
    private static final String            LOCKS_ICON                = "/com/tc/admin/icons/deadlock_view.gif";
    private static final String            TRANSIENT_FIELDS_ICON     = "/com/tc/admin/icons/transient.gif";
    private static final String            INSTRUMENTED_CLASSES_ICON = "/com/tc/admin/icons/class_obj.gif";
    private static final String            DISTRIBUTED_METHODS_ICON  = "/com/tc/admin/icons/jmeth_obj.gif";
    private static final String            BOOT_CLASSES_ICON         = "/com/tc/admin/icons/jar_obj.gif";
    private static final String            ROOTS                     = "Roots";
    private static final String            LOCKS                     = "Locks";
    private static final String            TRANSIENT_FIELDS          = "Transient Fields";
    private static final String            INSTRUMENTED_CLASSES      = "Instrumented Classes";
    private static final String            DISTRIBUTED_METHODS       = "Distributed Methods";
    private static final String            BOOT_CLASSES              = "Boot Classes";
    private final RootsPanel               m_roots;
    private final LocksPanel               m_locks;
    private final TransientFieldsPanel     m_transientFields;
    private final InstrumentedClassesPanel m_instrumentedClasses;
    private final DistributedMethodsPanel  m_distributedMethods;
    private final BootClassesPanel         m_bootClasses;
    private final TabFolder                m_tabFolder;

    private Layout(Composite parent) {
      m_tabFolder = new TabFolder(parent, SWT.BORDER);

      TabItem rootsTab = new TabItem(m_tabFolder, SWT.NONE);
      rootsTab.setText(ROOTS);
      rootsTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(ROOTS_ICON)));
      ScrolledComposite scroll = new ScrolledComposite(m_tabFolder, SWT.V_SCROLL);
      m_roots = new RootsPanel(scroll, SWT.NONE);
      rootsTab.setControl(scroll);
      scroll.setContent(m_roots);
      scroll.setExpandHorizontal(true);
      scroll.setExpandVertical(true);
      scroll.setMinSize(m_roots.computeSize(SWT.DEFAULT, SWT.DEFAULT));

      TabItem locksTab = new TabItem(m_tabFolder, SWT.NONE);
      locksTab.setText(LOCKS);
      locksTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(LOCKS_ICON)));
      scroll = new ScrolledComposite(m_tabFolder, SWT.V_SCROLL);
      m_locks = new LocksPanel(scroll, SWT.NONE);
      locksTab.setControl(scroll);
      scroll.setContent(m_locks);
      scroll.setExpandHorizontal(true);
      scroll.setExpandVertical(true);
      scroll.setMinSize(m_locks.computeSize(SWT.DEFAULT, SWT.DEFAULT));

      TabItem transientFieldsTab = new TabItem(m_tabFolder, SWT.NONE);
      transientFieldsTab.setText(TRANSIENT_FIELDS);
      transientFieldsTab.setImage(new Image(parent.getDisplay(), this.getClass()
          .getResourceAsStream(TRANSIENT_FIELDS_ICON)));
      scroll = new ScrolledComposite(m_tabFolder, SWT.V_SCROLL);
      m_transientFields = new TransientFieldsPanel(scroll, SWT.NONE);
      transientFieldsTab.setControl(scroll);
      scroll.setContent(m_transientFields);
      scroll.setExpandHorizontal(true);
      scroll.setExpandVertical(true);
      scroll.setMinSize(m_transientFields.computeSize(SWT.DEFAULT, SWT.DEFAULT));

      TabItem instrumentedClassesTab = new TabItem(m_tabFolder, SWT.NONE);
      instrumentedClassesTab.setText(INSTRUMENTED_CLASSES);
      instrumentedClassesTab.setImage(new Image(parent.getDisplay(), this.getClass()
          .getResourceAsStream(INSTRUMENTED_CLASSES_ICON)));
      scroll = new ScrolledComposite(m_tabFolder, SWT.V_SCROLL);
      m_instrumentedClasses = new InstrumentedClassesPanel(scroll, SWT.NONE);
      instrumentedClassesTab.setControl(scroll);
      scroll.setContent(m_instrumentedClasses);
      scroll.setExpandHorizontal(true);
      scroll.setExpandVertical(true);
      scroll.setMinSize(m_instrumentedClasses.computeSize(SWT.DEFAULT, SWT.DEFAULT));

      TabItem distributedMethodsTab = new TabItem(m_tabFolder, SWT.NONE);
      distributedMethodsTab.setText(DISTRIBUTED_METHODS);
      distributedMethodsTab.setImage(new Image(parent.getDisplay(), this.getClass()
          .getResourceAsStream(DISTRIBUTED_METHODS_ICON)));
      scroll = new ScrolledComposite(m_tabFolder, SWT.V_SCROLL);
      m_distributedMethods = new DistributedMethodsPanel(scroll, SWT.NONE);
      distributedMethodsTab.setControl(scroll);
      scroll.setContent(m_distributedMethods);
      scroll.setExpandHorizontal(true);
      scroll.setExpandVertical(true);
      scroll.setMinSize(m_distributedMethods.computeSize(SWT.DEFAULT, SWT.DEFAULT));

      TabItem bootClassesTab = new TabItem(m_tabFolder, SWT.NONE);
      bootClassesTab.setText(BOOT_CLASSES);
      bootClassesTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(BOOT_CLASSES_ICON)));
      scroll = new ScrolledComposite(m_tabFolder, SWT.V_SCROLL);
      m_bootClasses = new BootClassesPanel(scroll, SWT.NONE);
      bootClassesTab.setControl(scroll);
      scroll.setContent(m_bootClasses);
      scroll.setExpandHorizontal(true);
      scroll.setExpandVertical(true);
      scroll.setMinSize(m_bootClasses.computeSize(SWT.DEFAULT, SWT.DEFAULT));

      m_tabFolder.pack();
      m_tabFolder.setSelection(rootsTab);
    }
  }

  @Override
  public void transientFieldsChanged(IProject project) {
    if (project.equals(getProject())) {
      m_layout.m_transientFields.setup(m_project, m_dsoApp);
    }
  }

  @Override
  public void bootClassesChanged(IProject project) {
    if (project.equals(getProject())) {
      m_layout.m_bootClasses.setup(m_project, m_dsoApp);
    }
  }

  @Override
  public void distributedMethodsChanged(IProject project) {
    if (project.equals(getProject())) {
      m_layout.m_distributedMethods.setup(m_project, m_dsoApp);
    }
  }

  @Override
  public void instrumentationRulesChanged(IProject project) {
    if (project.equals(getProject())) {
      m_layout.m_instrumentedClasses.setup(m_project, m_dsoApp);
    }
  }

  @Override
  public void namedLocksChanged(IProject project) {
    if (project.equals(getProject())) {
      m_layout.m_locks.setup(m_project, m_dsoApp);
    }
  }

  @Override
  public void autolocksChanged(IProject project) {
    if (project.equals(getProject())) {
      m_layout.m_locks.setup(m_project, m_dsoApp);
    }
  }

  @Override
  public void rootsChanged(IProject project) {
    if (project.equals(getProject())) {
      m_layout.m_roots.setup(m_project, m_dsoApp);
    }
  }

}
