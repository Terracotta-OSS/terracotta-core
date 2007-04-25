/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.dialogs.AddModuleDialog;
import org.terracotta.dso.dialogs.RepoLocationDialog;
import org.terracotta.dso.editors.chooser.FolderBehavior;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.dso.editors.chooser.PackageNavigator;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

public final class ClientsPanel extends ConfigurationEditorPanel {

  private static final String MODULE_DECLARATION   = "Module Declaration";
  private static final String MODULE_REPO_LOCATION = "Repository Location (URL)";
  private static final String REPO_DECLARATION     = "Repository Declaration";
  private final Layout        m_layout;
  private State               m_state;

  public ClientsPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  // ================================================================================
  // INTERFACE
  // ================================================================================

  public synchronized void clearState() {
    setActive(false);
    m_layout.reset();
    m_state.xmlContext.detachComponentModel(this);
    m_state = null;
  }

  public synchronized void init(Object data) {
    if (m_isActive && m_state.project == (IProject) data) return;
    setActive(false);
    m_state = new State((IProject) data);
    createContextListeners();
    setActive(true);
    updateClientListeners();
    Modules modules = m_state.xmlContext.getParentElementProvider().hasModules();
    initModuleRepositories(modules);
    initModules(modules);
  }
  
  public synchronized void refreshContent() {
    m_layout.reset();
    updateClientListeners();
    Modules modules = m_state.xmlContext.getParentElementProvider().hasModules();
    initModuleRepositories(modules);
    initModules(modules);
  }
  
  public void detach() {
    m_state.xmlContext.detachComponentModel(this);
  }

  // ================================================================================
  // INIT LISTENERS
  // ================================================================================

  private void createContextListeners() {
    registerFieldBehavior(XmlConfigEvent.CLIENT_LOGS, m_layout.m_logsLocation);
    registerCheckBehavior(XmlConfigEvent.CLIENT_CLASS, m_layout.m_classCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_HIERARCHY, m_layout.m_hierarchyCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_LOCKS, m_layout.m_locksCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_TRANSIENT_ROOT, m_layout.m_transientRootCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_DISTRIBUTED_METHODS, m_layout.m_distributedMethodsCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_ROOTS, m_layout.m_rootsCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_LOCK_DEBUG, m_layout.m_lockDebugCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_DISTRIBUTED_METHOD_DEBUG, m_layout.m_distributedMethodDebugCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_FIELD_CHANGE_DEBUG, m_layout.m_fieldChangeDebugCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_NON_PORTABLE_DUMP, m_layout.m_nonPortableDumpCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_WAIT_NOTIFY_DEBUG, m_layout.m_waitNotifyDebugCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_NEW_OBJECT_DEBUG, m_layout.m_newObjectDebugCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_AUTOLOCK_DETAILS, m_layout.m_autoLockDetailsCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_CALLER, m_layout.m_callerCheck);
    registerCheckBehavior(XmlConfigEvent.CLIENT_FULL_STACK, m_layout.m_fullStackCheck);
    registerFaultCountBehavior(XmlConfigEvent.CLIENT_FAULT_COUNT, m_layout.m_faultCountSpinner);
    m_layout.m_logsBrowse.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        NavigatorBehavior behavior = new FolderBehavior();
        PackageNavigator dialog = new PackageNavigator(getShell(), behavior.getTitle(), m_state.project, behavior);
        dialog.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent event) {
            m_state.xmlContext.notifyListeners(new XmlConfigEvent(event.data, null, null, XmlConfigEvent.CLIENT_LOGS));
          }
        });
        dialog.open();
      }
    });
    // - modules behavior
    m_layout.m_addModule.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        AddModuleDialog dialog = new AddModuleDialog(getShell(), MODULE_DECLARATION, MODULE_DECLARATION);
        dialog.addValueListener(new AddModuleDialog.ValueListener() {
          public void setValues(String name, String version) {
            if (!name.trim().equals("") || !version.trim().equals("")) {
              String[] values = new String[] { name, version };
              m_state.xmlContext.notifyListeners(new XmlConfigEvent(values, null, null,
                  XmlConfigEvent.CREATE_CLIENT_MODULE));
            }
          }
        });
        dialog.open();
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        TableItem item = new TableItem(m_layout.m_moduleTable, SWT.NONE, 0);
        item.setText((String[]) e.data);
        m_layout.m_moduleTable.setSelection(item);
        m_layout.m_removeModule.setEnabled(true);
      }
    }, XmlConfigEvent.NEW_CLIENT_MODULE, this);
    m_layout.m_removeModule.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        int selected = m_layout.m_moduleTable.getSelectionIndex();
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.DELETE_CLIENT_MODULE);
        event.index = selected;
        m_state.xmlContext.notifyListeners(event);
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        m_layout.m_moduleTable.remove(((XmlConfigEvent) e).index);
        m_layout.m_removeModule.setEnabled(false);
      }
    }, XmlConfigEvent.REMOVE_CLIENT_MODULE, this);
    m_layout.m_moduleTable.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_layout.m_removeModule.setEnabled(true);
      }
    });
    // - modules repo behavior
    m_layout.m_addModuleRepo.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        RepoLocationDialog dialog = new RepoLocationDialog(getShell(), REPO_DECLARATION, MODULE_REPO_LOCATION);
        dialog.addValueListener(new RepoLocationDialog.ValueListener() {
          public void setValues(String repoLocation) {
            if (repoLocation != null && !(repoLocation = repoLocation.trim()).equals("")) {
              m_state.xmlContext.notifyListeners(new XmlConfigEvent(repoLocation, null, null,
                  XmlConfigEvent.CREATE_CLIENT_MODULE_REPO));
            }
          }
        });
        dialog.open();
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        TableItem item = new TableItem(m_layout.m_moduleRepoTable, SWT.NONE, 0);
        item.setText((String) e.data);
        m_layout.m_moduleRepoTable.setSelection(item);
        m_layout.m_removeModuleRepo.setEnabled(true);
      }
    }, XmlConfigEvent.NEW_CLIENT_MODULE_REPO, this);
    m_layout.m_removeModuleRepo.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        int selected = m_layout.m_moduleRepoTable.getSelectionIndex();
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.DELETE_CLIENT_MODULE_REPO);
        event.index = selected;
        m_state.xmlContext.notifyListeners(event);
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        m_layout.m_moduleRepoTable.remove(((XmlConfigEvent) e).index);
        m_layout.m_removeModuleRepo.setEnabled(false);
      }
    }, XmlConfigEvent.REMOVE_CLIENT_MODULE_REPO, this);
    m_layout.m_moduleRepoTable.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_layout.m_removeModuleRepo.setEnabled(true);
      }
    });
  }

  // - check box listeners
  private void registerCheckBehavior(final int type, final Button check) {
    check.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_state.xmlContext.notifyListeners(new XmlConfigEvent("" + check.getSelection(), (UpdateEventListener) check
            .getData(), null, type));
      }
    });
    UpdateEventListener checkListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        boolean select = Boolean.parseBoolean((String) event.data);
        check.setEnabled(true);
        check.setSelection(select);
      }
    };
    check.setData(checkListener);
    m_state.xmlContext.addListener(checkListener, type, this);
  }

  // - field listeners
  private void registerFieldBehavior(int type, final Text text) {
    registerFieldNotificationListener(type, text);
    UpdateEventListener textListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        if (event.data == null) event.data = "";
        text.setEnabled(true);
        text.setText((String) event.data);
      }
    };
    text.setData(textListener);
    m_state.xmlContext.addListener(textListener, type, this);
  }

  // - handle field notifications
  private void registerFieldNotificationListener(final int type, final Text text) {
    text.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        handleFieldEvent(text, type);
      }
    });
    text.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.Selection) {
          handleFieldEvent(text, type);
        }
      }
    });
  }

  // - spinner behavior
  private void registerFaultCountBehavior(final int type, final Spinner spinner) {
    spinner.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        handleSpinnerEvent(spinner, type);
      }
    });
    spinner.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.Selection) {
          spinner.getParent().forceFocus();
          spinner.forceFocus();
        }
      }
    });
    spinner.addMouseListener(new MouseAdapter() {
      public void mouseUp(MouseEvent mouseevent) {
        handleSpinnerEvent(spinner, type);
      }
    });
    UpdateEventListener spinnerListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        if (event.data == null) event.data = "0";
        spinner.setEnabled(true);
        spinner.setCapture(true);
        spinner.setIncrement(50);
        spinner.setMaximum(1000000);
        spinner.setSelection(Integer.parseInt((String) event.data));
      }
    };
    spinner.setData(spinnerListener);
    m_state.xmlContext.addListener(spinnerListener, type, this);
  }

  // ================================================================================
  // HELPERS
  // ================================================================================

  private void updateClientListeners() {
    updateListeners(XmlConfigEvent.CLIENT_LOGS);
    updateListeners(XmlConfigEvent.CLIENT_CLASS);
    updateListeners(XmlConfigEvent.CLIENT_HIERARCHY);
    updateListeners(XmlConfigEvent.CLIENT_LOCKS);
    updateListeners(XmlConfigEvent.CLIENT_TRANSIENT_ROOT);
    updateListeners(XmlConfigEvent.CLIENT_DISTRIBUTED_METHODS);
    updateListeners(XmlConfigEvent.CLIENT_ROOTS);
    updateListeners(XmlConfigEvent.CLIENT_LOCK_DEBUG);
    updateListeners(XmlConfigEvent.CLIENT_DISTRIBUTED_METHOD_DEBUG);
    updateListeners(XmlConfigEvent.CLIENT_FIELD_CHANGE_DEBUG);
    updateListeners(XmlConfigEvent.CLIENT_NON_PORTABLE_DUMP);
    updateListeners(XmlConfigEvent.CLIENT_WAIT_NOTIFY_DEBUG);
    updateListeners(XmlConfigEvent.CLIENT_NEW_OBJECT_DEBUG);
    updateListeners(XmlConfigEvent.CLIENT_AUTOLOCK_DETAILS);
    updateListeners(XmlConfigEvent.CLIENT_CALLER);
    updateListeners(XmlConfigEvent.CLIENT_FULL_STACK);
    updateListeners(XmlConfigEvent.CLIENT_FAULT_COUNT);
  }

  private void initModuleRepositories(Modules modules) {
    if (modules == null) return;
    TableItem item;
    String[] repos = modules.getRepositoryArray();
    for (int i = 0; i < repos.length; i++) {
      item = new TableItem(m_layout.m_moduleRepoTable, SWT.NONE);
      item.setText(repos[i]);
    }
  }

  private void initModules(Modules modulesElement) {
    if (modulesElement == null) return;
    TableItem item;
    Module[] modules = modulesElement.getModuleArray();
    for (int i = 0; i < modules.length; i++) {
      item = new TableItem(m_layout.m_moduleTable, SWT.NONE);
      item.setText(new String[] { modules[i].getName(), modules[i].getVersion() });
    }
  }

  private void updateListeners(int event) {
    m_state.xmlContext.updateListeners(new XmlConfigEvent(event));
  }

  private void handleFieldEvent(Text text, int type) {
    if (!m_isActive) return;
    m_state.xmlContext.notifyListeners(new XmlConfigEvent(text.getText(), (UpdateEventListener) text.getData(), null,
        type));
  }

  private void handleSpinnerEvent(Spinner spinner, int type) {
    if (!m_isActive) return;
    m_state.xmlContext.notifyListeners(new XmlConfigEvent("" + spinner.getSelection(), (UpdateEventListener) spinner
        .getData(), null, type));
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

    private static final int    WIDTH_HINT               = 500;
    private static final int    HEIGHT_HINT              = 600;
    private static final String LOGS_LOCATION            = "Logs Location";
    private static final String BROWSE                   = "Browse...";
    private static final String DSO_CLIENT_DATA          = "Dso Client Data";
    private static final String INSTRUMENTATION_LOGGING  = "Instrumentation Logging";
    private static final String RUNTIME_LOGGING          = "Runtime Logging";
    private static final String RUNTIME_OUTPUT_OPTIONS   = "Runtime Output Options";
    private static final String CLASS                    = "Class";
    private static final String HIERARCHY                = "Hierarchy";
    private static final String LOCKS                    = "Locks";
    private static final String TRANSIENT_ROOT           = "Transient Root";
    private static final String DISTRIBUTED_METHODS      = "Distributed Methods";
    private static final String ROOTS                    = "Roots";
    private static final String LOCK_DEBUG               = "Lock Debug";
    private static final String DISTRIBUTED_METHOD_DEBUG = "Distributed Method Debug";
    private static final String FIELD_CHANGE_DEBUG       = "Field Change Debug";
    private static final String NON_PORTABLE_DUMP        = "Non-portable Dump";
    private static final String WAIT_NOTIFY_DEBUG        = "Wait Notify Debug";
    private static final String NEW_OBJECT_DEBUG         = "New Object Debug";
    private static final String AUTOLOCK_DETAILS         = "Autolock Details";
    private static final String CALLER                   = "Caller";
    private static final String FULL_STACK               = "Full Stack";
    private static final String MODULE_REPOSITORIES      = "Module Repositories";
    private static final String MODULES                  = "Modules";
    private static final String LOCATION                 = "Location";
    private static final String NAME                     = "Name";
    private static final String VERSION                  = "Version";
    private static final String ADD                      = "Add...";
    private static final String REMOVE                   = "Remove";
    private static final String FAULT_COUNT              = "Fault Count";

    private Button              m_logsBrowse;
    private Text                m_logsLocation;
    private Button              m_classCheck;
    private Button              m_hierarchyCheck;
    private Button              m_locksCheck;
    private Button              m_transientRootCheck;
    private Button              m_distributedMethodsCheck;
    private Button              m_rootsCheck;
    private Button              m_lockDebugCheck;
    private Button              m_distributedMethodDebugCheck;
    private Button              m_fieldChangeDebugCheck;
    private Button              m_nonPortableDumpCheck;
    private Button              m_waitNotifyDebugCheck;
    private Button              m_newObjectDebugCheck;
    private Button              m_autoLockDetailsCheck;
    private Button              m_callerCheck;
    private Button              m_fullStackCheck;
    private Spinner             m_faultCountSpinner;
    private Table               m_moduleRepoTable;
    private Button              m_addModuleRepo;
    private Button              m_removeModuleRepo;
    private Table               m_moduleTable;
    private Button              m_addModule;
    private Button              m_removeModule;

    public void reset() {
      m_logsLocation.setText("");
      m_logsLocation.setEnabled(false);
      m_classCheck.setSelection(false);
      m_classCheck.setEnabled(false);
      m_hierarchyCheck.setSelection(false);
      m_hierarchyCheck.setEnabled(false);
      m_locksCheck.setSelection(false);
      m_locksCheck.setEnabled(false);
      m_transientRootCheck.setSelection(false);
      m_transientRootCheck.setEnabled(false);
      m_distributedMethodsCheck.setSelection(false);
      m_distributedMethodsCheck.setEnabled(false);
      m_rootsCheck.setSelection(false);
      m_rootsCheck.setEnabled(false);
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
      m_autoLockDetailsCheck.setSelection(false);
      m_autoLockDetailsCheck.setEnabled(false);
      m_callerCheck.setSelection(false);
      m_callerCheck.setEnabled(false);
      m_fullStackCheck.setSelection(false);
      m_fullStackCheck.setEnabled(false);
      m_moduleRepoTable.removeAll();
      m_removeModuleRepo.setEnabled(false);
      m_moduleTable.removeAll();
      m_removeModule.setEnabled(false);
    }

    private Layout(Composite parent) {
      Composite panel = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      panel.setLayout(gridLayout);

      Composite comp = new Composite(panel, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 3;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;

      GridData gridData = new GridData();
      gridData.widthHint = WIDTH_HINT;
      gridData.heightHint = HEIGHT_HINT;
      gridData.horizontalAlignment = GridData.CENTER;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.verticalAlignment = GridData.BEGINNING;
      comp.setLayout(gridLayout);
      comp.setLayoutData(gridData);

      Label logsLabel = new Label(comp, SWT.NONE);
      logsLabel.setText(LOGS_LOCATION);

      m_logsLocation = new Text(comp, SWT.BORDER);
      m_logsLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      m_logsBrowse = new Button(comp, SWT.PUSH);
      m_logsBrowse.setText(BROWSE);
      SWTUtil.applyDefaultButtonSize(m_logsBrowse);

      Group dsoClientDataGroup = new Group(comp, SWT.BORDER);
      dsoClientDataGroup.setText(DSO_CLIENT_DATA);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 3;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      dsoClientDataGroup.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      dsoClientDataGroup.setLayoutData(gridData);

      createInstrumentationLoggingGroup(dsoClientDataGroup);
      createRuntimeLoggingGroup(dsoClientDataGroup);
      createRuntimeOuputOptionsGroup(dsoClientDataGroup);
      createFaultCountPane(dsoClientDataGroup);
      createModuleRepositoriesPanel(dsoClientDataGroup);
      createModulesPanel(dsoClientDataGroup);
    }

    private void createInstrumentationLoggingGroup(Composite parent) {
      Group instrumentationLoggingGroup = new Group(parent, SWT.BORDER);
      instrumentationLoggingGroup.setText(INSTRUMENTATION_LOGGING);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      instrumentationLoggingGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
      gridData.verticalSpan = 2;
      instrumentationLoggingGroup.setLayoutData(gridData);

      m_classCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_classCheck.setText(CLASS);
      m_hierarchyCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_hierarchyCheck.setText(HIERARCHY);
      m_locksCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_locksCheck.setText(LOCKS);
      m_transientRootCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_transientRootCheck.setText(TRANSIENT_ROOT);
      m_distributedMethodsCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_distributedMethodsCheck.setText(DISTRIBUTED_METHODS);
      m_rootsCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_rootsCheck.setText(ROOTS);
    }

    private void createRuntimeLoggingGroup(Composite parent) {
      Group runtimeLoggingGroup = new Group(parent, SWT.BORDER);
      runtimeLoggingGroup.setText(RUNTIME_LOGGING);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      runtimeLoggingGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
      gridData.verticalSpan = 2;
      runtimeLoggingGroup.setLayoutData(gridData);

      m_lockDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_lockDebugCheck.setText(LOCK_DEBUG);
      m_distributedMethodDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_distributedMethodDebugCheck.setText(DISTRIBUTED_METHOD_DEBUG);
      m_fieldChangeDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_fieldChangeDebugCheck.setText(FIELD_CHANGE_DEBUG);
      m_nonPortableDumpCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_nonPortableDumpCheck.setText(NON_PORTABLE_DUMP);
      m_waitNotifyDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_waitNotifyDebugCheck.setText(WAIT_NOTIFY_DEBUG);
      m_newObjectDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_newObjectDebugCheck.setText(NEW_OBJECT_DEBUG);
    }

    private void createRuntimeOuputOptionsGroup(Composite parent) {
      Group runtimeOutputOptionsGroup = new Group(parent, SWT.BORDER);
      runtimeOutputOptionsGroup.setText(RUNTIME_OUTPUT_OPTIONS);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      runtimeOutputOptionsGroup.setLayout(gridLayout);
      runtimeOutputOptionsGroup
          .setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

      m_autoLockDetailsCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_autoLockDetailsCheck.setText(AUTOLOCK_DETAILS);
      m_callerCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_callerCheck.setText(CALLER);
      m_fullStackCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_fullStackCheck.setText(FULL_STACK);
    }

    private void createFaultCountPane(Composite parent) {
      Composite faultCountPane = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      faultCountPane.setLayout(gridLayout);
      faultCountPane.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
      Label faultCountLabel = new Label(faultCountPane, SWT.NONE);
      faultCountLabel.setText(FAULT_COUNT);
      faultCountLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
      m_faultCountSpinner = new Spinner(faultCountPane, SWT.BORDER);
      m_faultCountSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
    }

    private void createModuleRepositoriesPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      comp.setLayoutData(gridData);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(MODULE_REPOSITORIES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_moduleRepoTable = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_moduleRepoTable.setHeaderVisible(true);
      m_moduleRepoTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(tablePanel, m_moduleRepoTable);

      TableColumn column0 = new TableColumn(m_moduleRepoTable, SWT.NONE);
      column0.setResizable(true);
      column0.setText(LOCATION);
      column0.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addModuleRepo = new Button(buttonPanel, SWT.PUSH);
      m_addModuleRepo.setText(ADD);
      m_addModuleRepo.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addModuleRepo);

      m_removeModuleRepo = new Button(buttonPanel, SWT.PUSH);
      m_removeModuleRepo.setText(REMOVE);
      m_removeModuleRepo.setEnabled(false);
      m_removeModuleRepo.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeModuleRepo);
    }

    private void createModulesPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      comp.setLayoutData(gridData);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(MODULES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_moduleTable = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_moduleTable.setHeaderVisible(true);
      m_moduleTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(tablePanel, m_moduleTable);

      TableColumn column0 = new TableColumn(m_moduleTable, SWT.NONE);
      column0.setResizable(true);
      column0.setText(NAME);
      column0.pack();

      TableColumn column1 = new TableColumn(m_moduleTable, SWT.NONE);
      column1.setResizable(true);
      column1.setText(VERSION);
      column1.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addModule = new Button(buttonPanel, SWT.PUSH);
      m_addModule.setText(ADD);
      m_addModule.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addModule);

      m_removeModule = new Button(buttonPanel, SWT.PUSH);
      m_removeModule.setText(REMOVE);
      m_removeModule.setEnabled(false);
      m_removeModule.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeModule);
    }
  }
}
