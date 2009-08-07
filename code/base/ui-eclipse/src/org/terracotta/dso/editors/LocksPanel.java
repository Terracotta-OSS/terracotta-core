/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.terracotta.dso.editors.chooser.ExpressionChooser;
import org.terracotta.dso.editors.chooser.MethodBehavior;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Locks;
import com.terracottatech.config.NamedLock;

public class LocksPanel extends ConfigurationEditorPanel {
  private IProject                     m_project;
  private DsoApplication               m_dsoApp;
  private Locks                        m_locks;

  private final Layout                 m_layout;

  private final TableSelectionListener m_tableSelectionListener;
  private final TableDataListener      m_tableDataListener;
  private final AddLockHandler         m_addLockHandler;
  private final RemoveLockHandler      m_removeLockHandler;

  public LocksPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    m_tableSelectionListener = new TableSelectionListener();
    m_tableDataListener = new TableDataListener();
    m_addLockHandler = new AddLockHandler();
    m_removeLockHandler = new RemoveLockHandler();
  }

  public boolean hasAnySet() {
    return m_locks != null && (m_locks.sizeOfAutolockArray() > 0 || m_locks.sizeOfNamedLockArray() > 0);
  }

  private Locks ensureLocks() {
    if (m_locks == null) {
      ensureXmlObject();
    }
    return m_locks;
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_locks == null) {
      removeListeners();
      m_locks = m_dsoApp.addNewLocks();
      updateChildren();
      addListeners();
    }
  }

  private void testRemoveLocks() {
    if (!hasAnySet() && m_dsoApp.getLocks() != null) {
      m_dsoApp.unsetLocks();
      m_locks = null;
      fireXmlObjectStructureChanged(m_dsoApp);
    }
    testDisableRemoveButtons();
  }

  private void testDisableRemoveButtons() {
    m_layout.m_removeAutoLockButton.setEnabled(m_layout.m_autoLocksTable.getSelectionCount() > 0);
    m_layout.m_removeNamedLockButton.setEnabled(m_layout.m_namedLocksTable.getSelectionCount() > 0);
  }

  private void addListeners() {
    m_layout.m_addAutoLockButton.addSelectionListener(m_addLockHandler);
    m_layout.m_removeAutoLockButton.addSelectionListener(m_removeLockHandler);
    m_layout.m_autoLocksTable.addSelectionListener(m_tableSelectionListener);
    m_layout.m_autoLocksTable.addListener(SWT.SetData, m_tableDataListener);

    m_layout.m_addNamedLockButton.addSelectionListener(m_addLockHandler);
    m_layout.m_removeNamedLockButton.addSelectionListener(m_removeLockHandler);
    m_layout.m_namedLocksTable.addSelectionListener(m_tableSelectionListener);
    m_layout.m_namedLocksTable.addListener(SWT.SetData, m_tableDataListener);
  }

  private void removeListeners() {
    m_layout.m_addAutoLockButton.removeSelectionListener(m_addLockHandler);
    m_layout.m_removeAutoLockButton.removeSelectionListener(m_removeLockHandler);
    m_layout.m_autoLocksTable.removeSelectionListener(m_tableSelectionListener);
    m_layout.m_autoLocksTable.removeListener(SWT.SetData, m_tableDataListener);

    m_layout.m_addNamedLockButton.removeSelectionListener(m_addLockHandler);
    m_layout.m_removeNamedLockButton.removeSelectionListener(m_removeLockHandler);
    m_layout.m_namedLocksTable.removeSelectionListener(m_tableSelectionListener);
    m_layout.m_namedLocksTable.removeListener(SWT.SetData, m_tableDataListener);
  }

  public void updateChildren() {
    initTableItems();
    testDisableRemoveButtons();
  }

  public void updateModel() {
    removeListeners();
    updateChildren();
    addListeners();
  }

  public void setup(IProject project, DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();

    m_project = project;
    m_dsoApp = dsoApp;
    m_locks = m_dsoApp != null ? m_dsoApp.getLocks() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_project = null;
    m_dsoApp = null;
    m_locks = null;

    setEnabled(false);
  }

  private void initTableItems() {
    initAutolockTableItems();
    initNamedLockTableItems();
  }

  private void initAutolockTableItems() {
    m_layout.m_autoLocksTable.removeAll();

    if (m_locks == null) return;
    String[] autolockLevels = getListDefaults(Autolock.class, "lock-level");
    SWTUtil.makeTableComboItem(m_layout.m_autoLocksTable, Layout.AUTO_LOCK_COLUMN, autolockLevels);
    SWTUtil.makeTableComboItem(m_layout.m_autoLocksTable, Layout.AUTO_SYNCHRONIZED_COLUMN, new String[] { "true",
        "false" });
    Autolock[] autolocks = m_locks.getAutolockArray();
    for (int i = 0; i < autolocks.length; i++) {
      createAutolockTableItem(autolocks[i]);
    }
    if (autolocks.length > 0) {
      m_layout.m_autoLocksTable.setSelection(0);
    }
  }

  private void initNamedLockTableItems() {
    m_layout.m_namedLocksTable.removeAll();

    if (m_locks == null) return;
    String[] namedLockLevels = getListDefaults(NamedLock.class, "lock-level");
    SWTUtil.makeTableComboItem(m_layout.m_namedLocksTable, Layout.NAMED_LOCK_COLUMN, namedLockLevels);
    NamedLock[] namedLocks = m_locks.getNamedLockArray();
    for (int i = 0; i < namedLocks.length; i++) {
      createNamedLockTableItem(namedLocks[i]);
    }
    if (namedLocks.length > 0) {
      m_layout.m_namedLocksTable.setSelection(0);
    }
  }

  private void initAutolockTableItem(TableItem item, Autolock lock) {
    item.setText(Layout.AUTO_METHOD_COLUMN, lock.getMethodExpression());
    item.setText(Layout.AUTO_LOCK_COLUMN, lock.isSetLockLevel() ? lock.getLockLevel().toString() : "");
    item.setText(Layout.AUTO_SYNCHRONIZED_COLUMN, lock.isSetAutoSynchronized() ? Boolean.toString(lock
        .getAutoSynchronized()) : "false");
  }

  private void updateAutolockTableItem(int index) {
    TableItem item = m_layout.m_autoLocksTable.getItem(index);
    initAutolockTableItem(item, (Autolock) item.getData());
  }

  private void createAutolockTableItem(Autolock lock) {
    TableItem item = new TableItem(m_layout.m_autoLocksTable, SWT.NONE);
    initAutolockTableItem(item, lock);
    item.setData(lock);
  }

  private void initNamedLockTableItem(TableItem item, NamedLock lock) {
    item.setText(Layout.NAMED_NAME_COLUMN, lock.getLockName());
    item.setText(Layout.NAMED_METHOD_COLUMN, lock.getMethodExpression());
    item.setText(Layout.NAMED_LOCK_COLUMN, lock.isSetLockLevel() ? lock.getLockLevel().toString() : "");
  }

  private void updateNamedLockTableItem(int index) {
    TableItem item = m_layout.m_namedLocksTable.getItem(index);
    initNamedLockTableItem(item, (NamedLock) item.getData());
  }

  private void createNamedLockTableItem(NamedLock lock) {
    TableItem item = new TableItem(m_layout.m_namedLocksTable, SWT.NONE);
    initNamedLockTableItem(item, lock);
    item.setData(lock);
  }

  private void internalAddAutolock(String expression) {
    internalAddAutolock(expression, LockLevel.WRITE);
  }

  private void internalAddAutolock(String expression, LockLevel.Enum level) {
    Locks locks = ensureLocks();
    Autolock lock = locks.addNewAutolock();

    lock.setMethodExpression(expression);
    lock.setLockLevel(level);
    createAutolockTableItem(lock);

    int row = m_layout.m_autoLocksTable.getItemCount() - 1;
    m_layout.m_autoLocksTable.select(row);
  }

  private void internalAddNamedLock(String expression) {
    internalAddNamedLock(expression, "NewLock", LockLevel.WRITE);
  }

  private void internalAddNamedLock(String expression, String name, LockLevel.Enum level) {
    Locks locks = ensureLocks();
    NamedLock lock = locks.addNewNamedLock();

    lock.setMethodExpression(expression);
    lock.setLockName(name);
    lock.setLockLevel(level);
    createNamedLockTableItem(lock);

    int row = m_layout.m_namedLocksTable.getItemCount() - 1;
    m_layout.m_namedLocksTable.select(row);
  }

  private static class Layout {

    private static final String AUTO_LOCKS               = "Auto Locks";
    private static final String NAMED_LOCKS              = "Named Locks";
    private static final String LOCK_NAME                = "Lock Name";
    private static final String METHOD_EXPRESSION        = "Method Expression";
    private static final String LOCK_LEVEL               = "Lock Level";
    private static final String AUTO_SYNCHRONIZED        = "Auto Synchronized";
    private static final String ADD                      = "Add...";
    private static final String REMOVE                   = "Remove";

    private static final int    AUTO_METHOD_COLUMN       = 0;
    private static final int    AUTO_LOCK_COLUMN         = 1;
    private static final int    AUTO_SYNCHRONIZED_COLUMN = 2;

    private static final int    NAMED_NAME_COLUMN        = 0;
    private static final int    NAMED_METHOD_COLUMN      = 1;
    private static final int    NAMED_LOCK_COLUMN        = 2;

    private Table               m_autoLocksTable;
    private Button              m_addAutoLockButton;
    private Button              m_removeAutoLockButton;
    private Table               m_namedLocksTable;
    private Button              m_addNamedLockButton;
    private Button              m_removeNamedLockButton;

    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      comp.setLayout(gridLayout);

      createAutoLocksPanel(comp);
      createNamedLocksPanel(comp);
    }

    private void createAutoLocksPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 10;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(AUTO_LOCKS);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_autoLocksTable = new Table(sidePanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_autoLocksTable.setHeaderVisible(true);
      m_autoLocksTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(sidePanel, m_autoLocksTable, new int[] { 3, 1, 1 });
      SWTUtil.makeTableColumnsEditable(m_autoLocksTable, new int[] { AUTO_METHOD_COLUMN, AUTO_SYNCHRONIZED_COLUMN });
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.heightHint = SWTUtil.tableRowsToPixels(m_autoLocksTable, 3);
      m_autoLocksTable.setLayoutData(gridData);

      TableColumn methodCol = new TableColumn(m_autoLocksTable, SWT.NONE, AUTO_METHOD_COLUMN);
      methodCol.setResizable(true);
      methodCol.setText(METHOD_EXPRESSION);
      methodCol.pack();

      TableColumn lockCol = new TableColumn(m_autoLocksTable, SWT.NONE, AUTO_LOCK_COLUMN);
      lockCol.setResizable(true);
      lockCol.setText(LOCK_LEVEL);
      lockCol.pack();

      TableColumn autoSyncCol = new TableColumn(m_autoLocksTable, SWT.NONE, AUTO_SYNCHRONIZED_COLUMN);
      autoSyncCol.setResizable(true);
      autoSyncCol.setText(AUTO_SYNCHRONIZED);
      autoSyncCol.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addAutoLockButton = new Button(buttonPanel, SWT.PUSH);
      m_addAutoLockButton.setText(ADD);
      m_addAutoLockButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addAutoLockButton);

      m_removeAutoLockButton = new Button(buttonPanel, SWT.PUSH);
      m_removeAutoLockButton.setText(REMOVE);
      m_removeAutoLockButton.setEnabled(false);
      m_removeAutoLockButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeAutoLockButton);
    }

    private void createNamedLocksPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 10;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(NAMED_LOCKS);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_namedLocksTable = new Table(sidePanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_namedLocksTable.setHeaderVisible(true);
      m_namedLocksTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(sidePanel, m_namedLocksTable, new int[] { 1, 2, 1 });
      SWTUtil.makeTableColumnsEditable(m_namedLocksTable, new int[] { NAMED_NAME_COLUMN, NAMED_METHOD_COLUMN });
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.heightHint = SWTUtil.tableRowsToPixels(m_namedLocksTable, 3);
      m_namedLocksTable.setLayoutData(gridData);

      TableColumn lockNameCol = new TableColumn(m_namedLocksTable, SWT.NONE, NAMED_NAME_COLUMN);
      lockNameCol.setResizable(true);
      lockNameCol.setText(LOCK_NAME);
      lockNameCol.pack();

      TableColumn methodCol = new TableColumn(m_namedLocksTable, SWT.NONE, NAMED_METHOD_COLUMN);
      methodCol.setResizable(true);
      methodCol.setText(METHOD_EXPRESSION);
      methodCol.pack();

      TableColumn lockLevelCol = new TableColumn(m_namedLocksTable, SWT.NONE, NAMED_LOCK_COLUMN);
      lockLevelCol.setResizable(true);
      lockLevelCol.setText(LOCK_LEVEL);
      lockLevelCol.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addNamedLockButton = new Button(buttonPanel, SWT.PUSH);
      m_addNamedLockButton.setText(ADD);
      m_addNamedLockButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addNamedLockButton);

      m_removeNamedLockButton = new Button(buttonPanel, SWT.PUSH);
      m_removeNamedLockButton.setText(REMOVE);
      m_removeNamedLockButton.setEnabled(false);
      m_removeNamedLockButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeNamedLockButton);
    }
  }

  class AddLockHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      if (e.widget == m_layout.m_addAutoLockButton) {
        m_layout.m_autoLocksTable.forceFocus();
        NavigatorBehavior behavior = new MethodBehavior();
        ExpressionChooser chooser = new ExpressionChooser(getShell(), behavior.getTitle(), MethodBehavior.ADD_MSG,
                                                          m_project, behavior);
        chooser.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent updateEvent) {
            String[] items = (String[]) updateEvent.data;
            for (int i = 0; i < items.length; i++) {
              internalAddAutolock(items[i]);
            }
            fireAutolocksChanged();
          }
        });
        chooser.open();
      } else if (e.widget == m_layout.m_addNamedLockButton) {
        m_layout.m_namedLocksTable.forceFocus();
        NavigatorBehavior behavior = new MethodBehavior();
        ExpressionChooser chooser = new ExpressionChooser(getShell(), behavior.getTitle(), MethodBehavior.ADD_MSG,
                                                          m_project, behavior);
        chooser.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent updateEvent) {
            String[] items = (String[]) updateEvent.data;
            for (int i = 0; i < items.length; i++) {
              internalAddNamedLock(items[i]);
            }
            fireNamedLocksChanged();
          }
        });
        chooser.open();
      }
    }
  }

  class RemoveLockHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      if (e.widget == m_layout.m_removeAutoLockButton) {
        m_layout.m_autoLocksTable.forceFocus();
        int[] selection = m_layout.m_autoLocksTable.getSelectionIndices();

        for (int i = selection.length - 1; i >= 0; i--) {
          m_locks.removeAutolock(selection[i]);
        }
        m_layout.m_autoLocksTable.remove(selection);
        fireAutolocksChanged();
      } else if (e.widget == m_layout.m_removeNamedLockButton) {
        m_layout.m_namedLocksTable.forceFocus();
        int[] selection = m_layout.m_namedLocksTable.getSelectionIndices();

        for (int i = selection.length - 1; i >= 0; i--) {
          m_locks.removeNamedLock(selection[i]);
        }
        m_layout.m_namedLocksTable.remove(selection);
        fireNamedLocksChanged();
      }
      testRemoveLocks();
    }
  }

  class TableSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      if (e.widget == m_layout.m_autoLocksTable) {
        m_layout.m_removeAutoLockButton.setEnabled(true);
      } else if (e.widget == m_layout.m_namedLocksTable) {
        m_layout.m_removeNamedLockButton.setEnabled(true);
      }
    }
  }

  class TableDataListener implements Listener {
    public void handleEvent(Event e) {
      TableItem item = (TableItem) e.item;
      String text = item.getText(e.index).trim();
      int index = ((Table) e.widget).getSelectionIndex();

      if (e.widget == m_layout.m_autoLocksTable) {
        Autolock lock = (Autolock) item.getData();

        if (e.index == Layout.AUTO_SYNCHRONIZED_COLUMN) {
          boolean autoSync = Boolean.parseBoolean(text);
          if (autoSync) lock.setAutoSynchronized(true);
          else lock.unsetAutoSynchronized();
        } else if (e.index == Layout.AUTO_METHOD_COLUMN) {
          if (text.length() == 0) {
            item.setText(lock.getMethodExpression());
            removeAutolockLater(index);
            return;
          } else {
            lock.setMethodExpression(text);
          }
        } else {
          lock.xgetLockLevel().setStringValue(text);
        }
        fireAutolockChanged(index);
      } else if (e.widget == m_layout.m_namedLocksTable) {
        NamedLock lock = (NamedLock) item.getData();

        if (e.index == Layout.NAMED_NAME_COLUMN) {
          if (text.length() == 0) {
            item.setText(lock.getLockName());
            removeNamedLockLater(index);
            return;
          } else {
            lock.setLockName(text);
          }
        } else if (e.index == Layout.NAMED_METHOD_COLUMN) {
          if (text.length() == 0) {
            item.setText(lock.getMethodExpression());
            removeNamedLockLater(index);
            return;
          } else {
            lock.setMethodExpression(text);
          }
        } else {
          lock.xgetLockLevel().setStringValue(text);
        }
        fireNamedLockChanged(index);
      }
    }
  }

  private void removeAutolockLater(final int index) {
    getDisplay().asyncExec(new Runnable() {
      public void run() {
        m_locks.removeAutolock(index);
        m_layout.m_autoLocksTable.remove(index);
        fireAutolocksChanged();
        testRemoveLocks();
      }
    });
  }

  private void removeNamedLockLater(final int index) {
    getDisplay().asyncExec(new Runnable() {
      public void run() {
        m_locks.removeNamedLock(index);
        m_layout.m_namedLocksTable.remove(index);
        fireAutolocksChanged();
        testRemoveLocks();
      }
    });
  }

  @Override
  public void namedLockChanged(IProject project, int index) {
    if (project.equals(getProject())) {
      updateNamedLockTableItem(index);
    }
  }

  @Override
  public void autolockChanged(IProject project, int index) {
    if (project.equals(getProject())) {
      updateAutolockTableItem(index);
    }
  }
}
