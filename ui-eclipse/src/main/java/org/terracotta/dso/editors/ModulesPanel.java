/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ModuleInfo;
import org.terracotta.dso.ModulesConfiguration;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.dialogs.NewAddModuleDialog;
import org.terracotta.dso.dialogs.RepoLocationDialog;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.Client;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.net.URL;

public class ModulesPanel extends ConfigurationEditorPanel implements XmlObjectStructureListener {
  private Client                       m_dsoClient;
  private Modules                      m_modules;

  private final Layout                 m_layout;

  private final TableSelectionListener m_tableSelectionListener;
  private final TableDataListener      m_tableDataListener;
  private final MouseMoveListener      m_moduleMouseMoveListener;
  private final AddModuleHandler       m_addModuleHandler;
  private final RemoveModuleHandler    m_removeModuleHandler;
  private final AddRepoHandler         m_addRepoHandler;
  private final RemoveRepoHandler      m_removeRepoHandler;

  private static final String          MODULE_DECLARATION    = "Module Declaration";
  private static final String          MODULE_REPO_LOCATION  = "Repository Location";
  private static final String          REPO_DECLARATION      = "Repository Declaration";

  private static final int             MODULE_NAME_INDEX     = 0;
  private static final int             MODULE_GROUP_ID_INDEX = 0;

  public ModulesPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);

    m_tableSelectionListener = new TableSelectionListener();
    m_tableDataListener = new TableDataListener();
    m_moduleMouseMoveListener = new ModuleMouseMoveListener();
    m_addModuleHandler = new AddModuleHandler();
    m_removeModuleHandler = new RemoveModuleHandler();
    m_addRepoHandler = new AddRepoHandler();
    m_removeRepoHandler = new RemoveRepoHandler();

    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  private Modules ensureModules() {
    if (m_modules == null) {
      ensureXmlObject();
    }
    return m_modules;
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();
    if (m_modules == null) {
      removeListeners();
      m_modules = m_dsoClient.addNewModules();
      updateChildren();
      addListeners();
    }
  }

  private void updateChildren() {
    initModuleRepositories();
    initModules();
  }

  public void setup(Client dsoClient) {
    setEnabled(true);
    removeListeners();
    m_dsoClient = dsoClient;
    m_modules = (m_dsoClient != null) ? m_dsoClient.getModules() : null;
    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();
    m_dsoClient = null;
    setEnabled(false);
  }

  private void addListeners() {
    m_layout.m_moduleRepoTable.addSelectionListener(m_tableSelectionListener);
    m_layout.m_moduleRepoTable.addListener(SWT.SetData, m_tableDataListener);
    m_layout.m_moduleTable.addSelectionListener(m_tableSelectionListener);
    m_layout.m_moduleTable.addListener(SWT.SetData, m_tableDataListener);
    m_layout.m_moduleTable.addMouseMoveListener(m_moduleMouseMoveListener);
    m_layout.m_addModule.addSelectionListener(m_addModuleHandler);
    m_layout.m_removeModule.addSelectionListener(m_removeModuleHandler);
    m_layout.m_addModuleRepo.addSelectionListener(m_addRepoHandler);
    m_layout.m_removeModuleRepo.addSelectionListener(m_removeRepoHandler);
  }

  private void removeListeners() {
    m_layout.m_moduleRepoTable.removeSelectionListener(m_tableSelectionListener);
    m_layout.m_moduleRepoTable.removeListener(SWT.SetData, m_tableDataListener);
    m_layout.m_moduleTable.removeSelectionListener(m_tableSelectionListener);
    m_layout.m_moduleTable.removeListener(SWT.SetData, m_tableDataListener);
    m_layout.m_moduleTable.removeMouseMoveListener(m_moduleMouseMoveListener);
    m_layout.m_addModule.removeSelectionListener(m_addModuleHandler);
    m_layout.m_removeModule.removeSelectionListener(m_removeModuleHandler);
    m_layout.m_addModuleRepo.removeSelectionListener(m_addRepoHandler);
    m_layout.m_removeModuleRepo.removeSelectionListener(m_removeRepoHandler);
  }

  private void testRemoveModules() {
    if (!hasAnySet() && m_dsoClient.getModules() != null) {
      m_dsoClient.unsetModules();
      m_modules = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_dsoClient);
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    testRemoveModules();
  }

  public boolean hasAnySet() {
    return m_modules != null && (m_modules.sizeOfRepositoryArray() > 0 || m_modules.sizeOfModuleArray() > 0);
  }

  private void createModuleTableItem(Module module) {
    TableItem item = new TableItem(m_layout.m_moduleTable, SWT.NONE);
    String groupId = module.isSetGroupId() ? module.getGroupId() : "org.terracotta.modules";
    item.setText(new String[] { module.getName(), groupId, module.getVersion() });
    item.setData(module);
  }

  private void initModules() {
    m_layout.m_moduleTable.removeAll();
    if (m_modules == null) return;
    Module[] modules = m_modules.getModuleArray();
    for (Module module : modules) {
      createModuleTableItem(module);
    }
    if (modules.length > 0) {
      m_layout.m_moduleTable.setSelection(0);
    }
  }

  private void internalAddModuleRepo(String location) {
    ensureModules().addRepository(location);
    createModuleRepoTableItem(location);
  }

  private void createModuleRepoTableItem(String repo) {
    TableItem item = new TableItem(m_layout.m_moduleRepoTable, SWT.NONE);
    item.setText(repo);
  }

  private void initModuleRepositories() {
    m_layout.m_moduleRepoTable.removeAll();
    if (m_modules == null) return;
    String[] repos = m_modules.getRepositoryArray();
    for (String repo : repos) {
      createModuleRepoTableItem(repo);
    }
    if (repos.length > 0) {
      m_layout.m_moduleRepoTable.setSelection(0);
    }
  }

  private void testEnableRemove() {
    m_layout.m_removeModuleRepo.setEnabled(m_layout.m_moduleRepoTable.getSelectionCount() > 0);
    m_layout.m_removeModule.setEnabled(m_layout.m_moduleTable.getSelectionCount() > 0);
  }

  private static class Layout {
    private static final String MODULE_REPOSITORIES = "Module Repositories";
    private static final String MODULES             = "Modules";
    private static final String LOCATION            = "Location";
    private static final String NAME                = "Name";
    private static final String GROUP_ID            = "Group Identifier";
    private static final String VERSION             = "Version";
    private static final String ADD                 = "Add...";
    private static final String REMOVE              = "Remove";

    private Table               m_moduleRepoTable;
    private Button              m_addModuleRepo;
    private Button              m_removeModuleRepo;
    private Table               m_moduleTable;
    private Button              m_addModule;
    private Button              m_removeModule;

    private Layout(Composite parent) {
      parent.setLayout(new GridLayout());
      createModuleRepositoriesPanel(parent);
      createModulesPanel(parent);
    }

    private void createModuleRepositoriesPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      comp.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      comp.setLayoutData(gridData);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(MODULE_REPOSITORIES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_moduleRepoTable = new Table(sidePanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_moduleRepoTable.setHeaderVisible(true);
      m_moduleRepoTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(sidePanel, m_moduleRepoTable);
      SWTUtil.makeTableColumnsEditable(m_moduleRepoTable, new int[] { 0 });
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.heightHint = SWTUtil.tableRowsToPixels(m_moduleRepoTable, 0);
      m_moduleRepoTable.setLayoutData(gridData);

      TableColumn column0 = new TableColumn(m_moduleRepoTable, SWT.NONE);
      column0.setResizable(true);
      column0.setText(LOCATION);
      column0.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
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
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      comp.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      comp.setLayoutData(gridData);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(MODULES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_moduleTable = new Table(sidePanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_moduleTable.setHeaderVisible(true);
      m_moduleTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(sidePanel, m_moduleTable);
      SWTUtil.makeTableColumnsEditable(m_moduleTable, new int[] { 0, 1, 2 });
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.heightHint = SWTUtil.tableRowsToPixels(m_moduleTable, 0);
      m_moduleTable.setLayoutData(gridData);

      TableColumn column0 = new TableColumn(m_moduleTable, SWT.NONE);
      column0.setResizable(true);
      column0.setText(NAME);
      column0.pack();

      TableColumn column1 = new TableColumn(m_moduleTable, SWT.NONE);
      column1.setResizable(true);
      column1.setText(GROUP_ID);
      column1.pack();

      TableColumn column2 = new TableColumn(m_moduleTable, SWT.NONE);
      column2.setResizable(true);
      column2.setText(VERSION);
      column2.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
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

  class TableSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      Table table = (Table) e.widget;
      boolean removeEnabled = table.getItemCount() > 0;
      Button button = (table == m_layout.m_moduleRepoTable) ? m_layout.m_removeModuleRepo : m_layout.m_removeModule;
      button.setEnabled(removeEnabled);
    }
  }

  class TableDataListener implements Listener {
    public void handleEvent(Event event) {
      TableItem item = (TableItem) event.item;
      String text = item.getText(event.index);
      Table table = (Table) event.widget;
      int index = table.getSelectionIndex();

      if (event.widget == m_layout.m_moduleRepoTable) {
        m_modules.setRepositoryArray(index, text);
        fireModuleRepoChanged(index);
      } else {
        Module module = (Module) item.getData();
        if (event.index == MODULE_NAME_INDEX) {
          module.setName(text);
        } else if (event.index == MODULE_GROUP_ID_INDEX) {
          module.setGroupId(text);
        } else {
          module.setVersion(text);
        }
        fireModuleChanged(index);
      }
      table.setSelection(index);
    }
  }

  class ModuleMouseMoveListener implements MouseMoveListener {
    public void mouseMove(MouseEvent e) {
      String tip = null;
      TableItem item = m_layout.m_moduleTable.getItem(new Point(e.x, e.y));
      if (item != null) {
        Module module = (Module) item.getData();
        TcPlugin plugin = TcPlugin.getDefault();
        ModulesConfiguration modulesConfig = plugin.getModulesConfiguration(getProject());
        if (modulesConfig != null) {
          ModuleInfo moduleInfo = modulesConfig.getModuleInfo(module);
          if (moduleInfo != null) {
            Exception error = moduleInfo.getError();
            if (error != null) {
              tip = error.getMessage();
            } else {
              URL loc = moduleInfo.getLocation();
              if (loc != null) {
                tip = loc.toExternalForm();
              }
            }
          }
        }
      }

      final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window instanceof ApplicationWindow) {
        ((ApplicationWindow) window).setStatus(tip);
      }
    }
  }

  class AddModuleHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_moduleTable.forceFocus();
      NewAddModuleDialog dialog = new NewAddModuleDialog(getShell(), MODULE_DECLARATION, MODULE_DECLARATION, m_modules);
      dialog.addValueListener(new NewAddModuleDialog.ValueListener() {
        public void setValue(Modules modules) {
          m_dsoClient.setModules(modules);
          fireModulesChanged();
          fireModuleReposChanged();
        }
      });
      dialog.open();
    }
  }

  class RemoveModuleHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_moduleTable.forceFocus();
      int[] selection = m_layout.m_moduleTable.getSelectionIndices();
      Modules modules = ensureModules();
      for (int i = selection.length - 1; i >= 0; i--) {
        modules.removeModule(selection[i]);
      }
      m_layout.m_moduleTable.remove(selection);
      testEnableRemove();
      testRemoveModules();
      fireModulesChanged();
    }
  }

  class AddRepoHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_moduleRepoTable.forceFocus();
      RepoLocationDialog dialog = new RepoLocationDialog(getShell(), REPO_DECLARATION, MODULE_REPO_LOCATION);
      dialog.addValueListener(new RepoLocationDialog.ValueListener() {
        public void setValues(String repoLocation) {
          if (repoLocation != null && !(repoLocation = repoLocation.trim()).equals("")) {
            internalAddModuleRepo(repoLocation);
            testRemoveModules();
            fireModuleReposChanged();
          }
        }
      });
      dialog.open();
    }
  }

  class RemoveRepoHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_moduleRepoTable.forceFocus();
      int[] selection = m_layout.m_moduleRepoTable.getSelectionIndices();
      Modules modules = ensureModules();
      for (int i = selection.length - 1; i >= 0; i--) {
        modules.removeRepository(selection[i]);
      }
      m_layout.m_moduleRepoTable.remove(selection);
      testEnableRemove();
      testRemoveModules();
      fireModuleReposChanged();
    }
  }
}
