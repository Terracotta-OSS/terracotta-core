/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.commons.lang.StringUtils;
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
import org.terracotta.dso.editors.chooser.FieldBehavior;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;

public class RootsPanel extends ConfigurationEditorPanel {
  private IProject                     m_project;
  private DsoApplication               m_dsoApp;
  private Roots                        m_roots;

  private final Layout                 m_layout;

  private final AddRootHandler         m_addRootHandler;
  private final RemoveRootHandler      m_removeRootHandler;
  private final TableSelectionListener m_tableSelectionListener;
  private final TableDataListener      m_tableDataListener;

  private static final int             FIELD_COLUMN = 0;
  private static final int             NAME_COLUMN  = 1;

  public RootsPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    m_addRootHandler = new AddRootHandler();
    m_removeRootHandler = new RemoveRootHandler();
    m_tableSelectionListener = new TableSelectionListener();
    m_tableDataListener = new TableDataListener();
  }

  public boolean hasAnySet() {
    return m_roots != null && m_roots.sizeOfRootArray() > 0;
  }

  private Roots ensureRoots() {
    if (m_roots == null) {
      ensureXmlObject();
    }
    return m_roots;
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_roots == null) {
      removeListeners();
      m_roots = m_dsoApp.addNewRoots();
      updateChildren();
      addListeners();
    }
  }

  private void syncModel() {
    if (!hasAnySet() && m_dsoApp.getRoots() != null) {
      m_dsoApp.unsetRoots();
      m_roots = null;
      fireXmlObjectStructureChanged(m_dsoApp);
    }
    fireRootsChanged();
    testDisableRemoveButton();
  }

  private void testDisableRemoveButton() {
    m_layout.m_removeButton.setEnabled(m_layout.m_table.getSelectionCount() > 0);
  }

  private void addListeners() {
    m_layout.m_addButton.addSelectionListener(m_addRootHandler);
    m_layout.m_removeButton.addSelectionListener(m_removeRootHandler);
    m_layout.m_table.addSelectionListener(m_tableSelectionListener);
    m_layout.m_table.addListener(SWT.SetData, m_tableDataListener);
  }

  private void removeListeners() {
    m_layout.m_addButton.removeSelectionListener(m_addRootHandler);
    m_layout.m_removeButton.removeSelectionListener(m_removeRootHandler);
    m_layout.m_table.removeSelectionListener(m_tableSelectionListener);
    m_layout.m_table.removeListener(SWT.SetData, m_tableDataListener);
  }

  public void updateChildren() {
    initTableItems();
    testDisableRemoveButton();
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
    m_roots = m_dsoApp != null ? m_dsoApp.getRoots() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();
    clearTableItems();

    m_project = null;
    m_dsoApp = null;
    m_roots = null;

    setEnabled(false);
  }

  private void clearTableItems() {
    m_layout.m_table.removeAll();
  }

  private void initTableItems() {
    clearTableItems();
    if (m_roots == null) return;
    Root[] roots = m_roots.getRootArray();
    for (int i = 0; i < roots.length; i++) {
      createTableItem(roots[i]);
    }
    if (roots.length > 0) {
      m_layout.m_table.setSelection(0);
    }
  }

  private void initTableItem(TableItem item, Root root) {
    String fieldNameOrExpression;
    if (root.isSetFieldName()) {
      fieldNameOrExpression = root.getFieldName();
    } else {
      fieldNameOrExpression = root.getFieldExpression();
    }
    item.setText(new String[] { fieldNameOrExpression, root.getRootName() });
  }

  private void updateTableItem(int index) {
    TableItem item = m_layout.m_table.getItem(index);
    initTableItem(item, (Root) item.getData());
  }

  private void createTableItem(Root root) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    initTableItem(item, root);
    item.setData(root);
  }

  private void internalSetRoot(Root root, String fieldNameOrExpression) {
    fieldNameOrExpression = fieldNameOrExpression.trim();
    String sansWhitespace = StringUtils.deleteWhitespace(fieldNameOrExpression);
    if (fieldNameOrExpression.length() != sansWhitespace.length()) {
      root.setFieldExpression(fieldNameOrExpression);
      if (root.isSetFieldName()) {
        root.unsetFieldName();
      }
    } else {
      root.setFieldName(fieldNameOrExpression);
      if (root.isSetFieldExpression()) {
        root.unsetFieldExpression();
      }
    }
  }

  private void internalAddRoot(String fieldNameOrExpression) {
    Root root = ensureRoots().addNewRoot();
    internalSetRoot(root, fieldNameOrExpression);
    createTableItem(root);

    int row = m_layout.m_table.getItemCount() - 1;
    m_layout.m_table.setSelection(row);
  }

  private static class Layout {
    private static final String ROOTS  = "Roots";
    private static final String FIELD  = "Field/Expression";
    private static final String NAME   = "Name";
    private static final String ADD    = "Add...";
    private static final String REMOVE = "Remove";

    private final Table         m_table;
    private final Button        m_addButton;
    private final Button        m_removeButton;

    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 10;
      comp.setLayout(gridLayout);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(ROOTS);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_table = new Table(sidePanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_table.setHeaderVisible(true);
      m_table.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(sidePanel, m_table, new int[] { 2, 1 });
      SWTUtil.makeTableColumnsEditable(m_table, new int[] { 0, 1 });
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.heightHint = SWTUtil.tableRowsToPixels(m_table, 3);
      m_table.setLayoutData(gridData);

      TableColumn fieldCol = new TableColumn(m_table, SWT.NONE);
      fieldCol.setResizable(true);
      fieldCol.setText(FIELD);
      fieldCol.pack();

      TableColumn nameCol = new TableColumn(m_table, SWT.NONE);
      nameCol.setResizable(true);
      nameCol.setText(NAME);
      nameCol.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addButton = new Button(buttonPanel, SWT.PUSH);
      m_addButton.setText(ADD);
      m_addButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addButton);

      m_removeButton = new Button(buttonPanel, SWT.PUSH);
      m_removeButton.setText(REMOVE);
      m_removeButton.setEnabled(false);
      m_removeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeButton);
    }
  }

  class AddRootHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      NavigatorBehavior behavior = new FieldBehavior();
      ExpressionChooser chooser = new ExpressionChooser(getShell(), behavior.getTitle(), FieldBehavior.ADD_MSG,
                                                        m_project, behavior);
      chooser.addValueListener(new UpdateEventListener() {
        public void handleUpdate(UpdateEvent updateEvent) {
          String[] items = (String[]) updateEvent.data;
          for (int i = 0; i < items.length; i++) {
            internalAddRoot(items[i]);
          }
          fireRootsChanged();
        }
      });
      chooser.open();
    }
  }

  class RemoveRootHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      int[] selection = m_layout.m_table.getSelectionIndices();
      for (int i = selection.length - 1; i >= 0; i--) {
        ensureRoots().removeRoot(selection[i]);
      }
      m_layout.m_table.remove(selection);
      syncModel();
    }
  }

  class TableSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_removeButton.setEnabled(true);
    }
  }

  class TableDataListener implements Listener {
    public void handleEvent(Event e) {
      TableItem item = (TableItem) e.item;
      String fieldNameOrExpression = item.getText(e.index);
      Root root = (Root) item.getData();

      if (e.index == FIELD_COLUMN) {
        if (fieldNameOrExpression.length() == 0) {
          int index = m_layout.m_table.indexOf(item);
          ensureRoots().removeRoot(index);
          m_layout.m_table.remove(index);
          syncModel();
          return;
        } else {
          internalSetRoot(root, fieldNameOrExpression);
        }
      } else if (e.index == NAME_COLUMN) {
        root.setRootName(fieldNameOrExpression);
      }
      fireRootChanged(m_layout.m_table.indexOf(item));
    }
  }

  @Override
  public void rootChanged(IProject project, int index) {
    if (project.equals(getProject())) {
      updateTableItem(index);
    }
  }
}
