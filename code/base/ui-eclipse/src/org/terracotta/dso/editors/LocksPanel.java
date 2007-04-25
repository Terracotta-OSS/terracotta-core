/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
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
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Locks;
import com.terracottatech.config.NamedLock;

public class LocksPanel extends ConfigurationEditorPanel {

  private final Layout m_layout;
  private State        m_state;

  public LocksPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
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
    initTableItems();
    setActive(true);
  }

  public synchronized void refreshContent() {
    m_layout.reset();
    initTableItems();
  }

  public void detach() {
    m_state.xmlContext.detachComponentModel(this);
  }
  
  // ================================================================================
  // INIT LISTENERS
  // ================================================================================

  private void createContextListeners() {
    m_layout.m_autoLocksTable.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_layout.m_removeAutoLockButton.setEnabled(true);
      }
    });
    m_layout.m_namedLocksTable.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_layout.m_removeNamedLockButton.setEnabled(true);
      }
    });
    // - auto locks table
    m_layout.m_autoLocksTable.addListener(SWT.SetData, new Listener() {
      public void handleEvent(Event event) {
        if (!m_isActive) return;
        TableItem item = (TableItem) event.item;
        Autolock lock = (Autolock) item.getData();
        int type = -1;
        switch (event.index) {
          case Layout.AUTO_METHOD_COLUMN:
            type = XmlConfigEvent.LOCKS_AUTO_METHOD;
            break;
          case Layout.AUTO_LOCK_COLUMN:
            type = XmlConfigEvent.LOCKS_AUTO_LEVEL;
            break;
          default:
            break;
        }
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(item.getText(event.index), null, lock, type));
      }
    });
    m_state.xmlContext.addListener(new AutoLocksTableListener(), XmlConfigEvent.LOCKS_AUTO_METHOD, this);
    m_state.xmlContext.addListener(new AutoLocksTableListener(), XmlConfigEvent.LOCKS_AUTO_LEVEL, this);
    // - named locks table
    m_layout.m_namedLocksTable.addListener(SWT.SetData, new Listener() {
      public void handleEvent(Event event) {
        if (!m_isActive) return;
        TableItem item = (TableItem) event.item;
        NamedLock lock = (NamedLock) item.getData();
        int type = -1;
        switch (event.index) {
          case Layout.NAMED_NAME_COLUMN:
            type = XmlConfigEvent.LOCKS_NAMED_NAME;
            break;
          case Layout.NAMED_METHOD_COLUMN:
            type = XmlConfigEvent.LOCKS_NAMED_METHOD;
            break;
          case Layout.NAMED_LOCK_COLUMN:
            type = XmlConfigEvent.LOCKS_NAMED_LEVEL;
            break;
          default:
            break;
        }
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(item.getText(event.index), null, lock, type));
      }
    });
    m_state.xmlContext.addListener(new NamedLocksTableListener(), XmlConfigEvent.LOCKS_AUTO_METHOD, this);
    m_state.xmlContext.addListener(new NamedLocksTableListener(), XmlConfigEvent.LOCKS_AUTO_LEVEL, this);
    // - remove autolock
    m_layout.m_removeAutoLockButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_autoLocksTable.forceFocus();
        int selected = m_layout.m_autoLocksTable.getSelectionIndex();
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.DELETE_LOCK_AUTO);
        event.index = selected;
        setActive(true);
        m_state.xmlContext.notifyListeners(event);
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        m_layout.m_autoLocksTable.remove(((XmlConfigEvent) e).index);
        m_layout.m_removeAutoLockButton.setEnabled(false);
      }
    }, XmlConfigEvent.REMOVE_LOCK_AUTO, this);
    // - remove named-lock
    m_layout.m_removeNamedLockButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_namedLocksTable.forceFocus();
        int selected = m_layout.m_namedLocksTable.getSelectionIndex();
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.DELETE_LOCK_NAMED);
        event.index = selected;
        setActive(true);
        m_state.xmlContext.notifyListeners(event);
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        m_layout.m_namedLocksTable.remove(((XmlConfigEvent) e).index);
        m_layout.m_removeNamedLockButton.setEnabled(false);
      }
    }, XmlConfigEvent.REMOVE_LOCK_NAMED, this);
    // - add autolock
    m_layout.m_addAutoLockButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_autoLocksTable.forceFocus();
        NavigatorBehavior behavior = new MethodBehavior();
        ExpressionChooser chooser = new ExpressionChooser(getShell(), behavior.getTitle(), MethodBehavior.ADD_MSG,
            m_state.project, behavior);
        chooser.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent updateEvent) {
            String[] items = (String[]) updateEvent.data;
            for (int i = 0; i < items.length; i++) {
              XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.CREATE_LOCK_AUTO);
              event.data = new String[] { items[i], "" };
              m_state.xmlContext.notifyListeners(event);
            }
          }
        });
        setActive(true);
        chooser.open();
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        Autolock lock = (Autolock) castEvent(e).element;
        LockLevel.Enum levelEnum = lock.getLockLevel();
        String level = (levelEnum == null) ? "" : levelEnum.toString();
        createAutolockTableItem(lock, new String[] { lock.getMethodExpression(), level });
      }
    }, XmlConfigEvent.NEW_LOCK_AUTO, this);
    // - add named-lock
    m_layout.m_addNamedLockButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_namedLocksTable.forceFocus();
        NavigatorBehavior behavior = new MethodBehavior();
        ExpressionChooser chooser = new ExpressionChooser(getShell(), behavior.getTitle(), MethodBehavior.ADD_MSG,
            m_state.project, behavior);
        chooser.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent updateEvent) {
            String[] items = (String[]) updateEvent.data;
            for (int i = 0; i < items.length; i++) {
              XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.CREATE_LOCK_NAMED);
              event.data = new String[] { "", items[i], "" };
              m_state.xmlContext.notifyListeners(event);
            }
          }
        });
        setActive(true);
        chooser.open();
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        NamedLock lock = (NamedLock) castEvent(e).element;
        LockLevel.Enum levelEnum = lock.getLockLevel();
        String level = (levelEnum == null) ? "" : levelEnum.toString();
        createNamedLockTableItem(lock, new String[] { lock.getLockName(), lock.getMethodExpression(), level });
      }
    }, XmlConfigEvent.NEW_LOCK_NAMED, this);
  }

  // --------------------------------------------------------------------------------

  private class AutoLocksTableListener implements UpdateEventListener {
    public void handleUpdate(UpdateEvent e) {
      if (!m_isActive) return;
      XmlConfigEvent event = castEvent(e);
      if (event.data == null) event.data = "";
      XmlObject lock = event.element;
      TableItem[] items = m_layout.m_autoLocksTable.getItems();
      for (int i = 0; i < items.length; i++) {
        if (items[i].getData() == lock) {
          int column = -1;
          switch (event.type) {
            case XmlConfigEvent.LOCKS_AUTO_METHOD:
              column = Layout.AUTO_METHOD_COLUMN;
              break;
            case XmlConfigEvent.LOCKS_AUTO_LEVEL:
              column = Layout.AUTO_LOCK_COLUMN;
              break;
            default:
              break;
          }
          items[i].setText(column, (String) event.data);
        }
      }
    }
  }

  // --------------------------------------------------------------------------------

  private class NamedLocksTableListener implements UpdateEventListener {
    public void handleUpdate(UpdateEvent e) {
      if (!m_isActive) return;
      XmlConfigEvent event = castEvent(e);
      if (event.data == null) event.data = "";
      XmlObject lock = event.element;
      TableItem[] items = m_layout.m_namedLocksTable.getItems();
      for (int i = 0; i < items.length; i++) {
        if (items[i].getData() == lock) {
          int column = -1;
          switch (event.type) {
            case XmlConfigEvent.LOCKS_NAMED_NAME:
              column = Layout.NAMED_NAME_COLUMN;
              break;
            case XmlConfigEvent.LOCKS_NAMED_METHOD:
              column = Layout.NAMED_METHOD_COLUMN;
              break;
            case XmlConfigEvent.LOCKS_NAMED_LEVEL:
              column = Layout.NAMED_LOCK_COLUMN;
              break;
            default:
              break;
          }
          items[i].setText(column, (String) event.data);
        }
      }
    }
  }

  // ================================================================================
  // HELPERS
  // ================================================================================

  private void initTableItems() {
    Locks locks = m_state.xmlContext.getParentElementProvider().hasLocks();
    if (locks == null) return;
    String[] autolockLevels = XmlConfigContext.getListDefaults(Autolock.class, XmlConfigEvent.LOCKS_AUTO_LEVEL);
    SWTUtil.makeTableComboItem(m_layout.m_autoLocksTable, Layout.AUTO_LOCK_COLUMN, autolockLevels);
    Autolock[] autolocks = locks.getAutolockArray();
    for (int i = 0; i < autolocks.length; i++) {
      createAutolockTableItem(autolocks[i], new String[] {
        autolocks[i].getMethodExpression(),
        (autolocks[i].isSetLockLevel()) ? autolocks[i].getLockLevel().toString() : "" });
    }
    String[] namedLockLevels = XmlConfigContext.getListDefaults(NamedLock.class, XmlConfigEvent.LOCKS_NAMED_LEVEL);
    SWTUtil.makeTableComboItem(m_layout.m_namedLocksTable, Layout.NAMED_LOCK_COLUMN, namedLockLevels);
    NamedLock[] namedLocks = locks.getNamedLockArray();
    for (int i = 0; i < namedLocks.length; i++) {
      createNamedLockTableItem(namedLocks[i], new String[] {
        namedLocks[i].getLockName(),
        namedLocks[i].getMethodExpression(),
        (namedLocks[i].isSetLockLevel()) ? namedLocks[i].getLockLevel().toString() : "" });
    }
  }

  private void createAutolockTableItem(Autolock lock, String[] elements) {
    TableItem item = new TableItem(m_layout.m_autoLocksTable, SWT.NONE);
    item.setText(Layout.AUTO_METHOD_COLUMN, elements[0]);
    item.setText(Layout.AUTO_LOCK_COLUMN, elements[1]);
    item.setData(lock);
  }

  private void createNamedLockTableItem(NamedLock lock, String[] elements) {
    TableItem item = new TableItem(m_layout.m_namedLocksTable, SWT.NONE);
    item.setText(Layout.NAMED_NAME_COLUMN, elements[0]);
    item.setText(Layout.NAMED_METHOD_COLUMN, elements[1]);
    item.setText(Layout.NAMED_LOCK_COLUMN, elements[2]);
    item.setData(lock);
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

  private static class Layout {

    private static final String AUTO_LOCKS          = "Auto Locks";
    private static final String NAMED_LOCKS         = "Named Locks";
    private static final String LOCK_NAME           = "Lock Name";
    private static final String METHOD_EXPRESSION   = "Method Expression";
    private static final String LOCK_LEVEL          = "Lock Level";
    private static final String ADD                 = "Add...";
    private static final String REMOVE              = "Remove";

    private static final int    AUTO_METHOD_COLUMN  = 0;
    private static final int    AUTO_LOCK_COLUMN    = 1;
    private static final int    NAMED_NAME_COLUMN   = 0;
    private static final int    NAMED_METHOD_COLUMN = 1;
    private static final int    NAMED_LOCK_COLUMN   = 2;

    private Table               m_autoLocksTable;
    private Button              m_addAutoLockButton;
    private Button              m_removeAutoLockButton;
    private Table               m_namedLocksTable;
    private Button              m_addNamedLockButton;
    private Button              m_removeNamedLockButton;

    public void reset() {
      m_removeAutoLockButton.setEnabled(false);
      m_autoLocksTable.removeAll();
      m_removeNamedLockButton.setEnabled(false);
      m_namedLocksTable.removeAll();
    }

    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      comp.setLayout(gridLayout);

      createAutoLocksPanel(comp);
      createNamedLocksPanel(comp);
    }

    private void createAutoLocksPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(AUTO_LOCKS);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_autoLocksTable = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_autoLocksTable.setHeaderVisible(true);
      m_autoLocksTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(tablePanel, m_autoLocksTable, new int[] { 3, 1 });
      SWTUtil.makeTableColumnsEditable(m_autoLocksTable, new int[] { AUTO_METHOD_COLUMN });

      TableColumn methodCol = new TableColumn(m_autoLocksTable, SWT.NONE, AUTO_METHOD_COLUMN);
      methodCol.setResizable(true);
      methodCol.setText(METHOD_EXPRESSION);
      methodCol.pack();

      TableColumn lockCol = new TableColumn(m_autoLocksTable, SWT.NONE, AUTO_LOCK_COLUMN);
      lockCol.setResizable(true);
      lockCol.setText(LOCK_LEVEL);
      lockCol.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
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
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(NAMED_LOCKS);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_namedLocksTable = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_namedLocksTable.setHeaderVisible(true);
      m_namedLocksTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(tablePanel, m_namedLocksTable, new int[] { 1, 2, 1 });
      SWTUtil.makeTableColumnsEditable(m_namedLocksTable, new int[] { NAMED_NAME_COLUMN, NAMED_METHOD_COLUMN });

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
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
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
}
