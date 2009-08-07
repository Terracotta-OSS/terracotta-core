/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IMethod;
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
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.chooser.ExpressionChooser;
import org.terracotta.dso.editors.chooser.MethodBehavior;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.DistributedMethods.MethodExpression;

public class DistributedMethodsPanel extends ConfigurationEditorPanel {
  private IProject                     m_project;
  private DsoApplication               m_dsoApp;
  private DistributedMethods           m_distributedMethods;

  private final Layout                 m_layout;

  private final AddHandler             m_addHandler;
  private final RemoveHandler          m_removeHandler;
  private final TableSelectionListener m_tableSelectionListener;
  private final TableDataListener      m_tableDataListener;

  public DistributedMethodsPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    m_addHandler = new AddHandler();
    m_removeHandler = new RemoveHandler();
    m_tableSelectionListener = new TableSelectionListener();
    m_tableDataListener = new TableDataListener();
  }

  public boolean hasAnySet() {
    return m_distributedMethods != null && m_distributedMethods.sizeOfMethodExpressionArray() > 0;
  }

  private DistributedMethods ensureDistributedMethods() {
    if (m_distributedMethods == null) {
      ensureXmlObject();
    }
    return m_distributedMethods;
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_distributedMethods == null) {
      removeListeners();
      m_distributedMethods = m_dsoApp.addNewDistributedMethods();
      updateChildren();
      addListeners();
    }
  }

  private void testRemoveDistributedMethods() {
    if (!hasAnySet() && m_dsoApp.getDistributedMethods() != null) {
      m_dsoApp.unsetDistributedMethods();
      m_distributedMethods = null;
      fireXmlObjectStructureChanged(m_dsoApp);
    }
    fireDistributedMethodsChanged();
    testDisableRemoveButton();
  }

  private void testDisableRemoveButton() {
    m_layout.m_removeButton.setEnabled(m_layout.m_table.getSelectionCount() > 0);
  }

  private void addListeners() {
    m_layout.m_addButton.addSelectionListener(m_addHandler);
    m_layout.m_removeButton.addSelectionListener(m_removeHandler);
    m_layout.m_table.addSelectionListener(m_tableSelectionListener);
    m_layout.m_table.addListener(SWT.SetData, m_tableDataListener);
  }

  private void removeListeners() {
    m_layout.m_addButton.removeSelectionListener(m_addHandler);
    m_layout.m_removeButton.removeSelectionListener(m_removeHandler);
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
    m_distributedMethods = m_dsoApp != null ? m_dsoApp.getDistributedMethods() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_dsoApp = null;
    m_distributedMethods = null;

    setEnabled(false);
  }

  private void initTableItems() {
    m_layout.m_table.removeAll();
    if (m_distributedMethods == null) return;
    MethodExpression[] methods = m_distributedMethods.getMethodExpressionArray();
    for (int i = 0; i < methods.length; i++) {
      createTableItem(methods[i]);
    }
    if (methods.length > 0) {
      m_layout.m_table.setSelection(0);
    }
  }

  private void createTableItem(MethodExpression method) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    item.setText(method.getStringValue());
    item.setData(method);
  }

  public boolean isDistributed(IMethod method) {
    return TcPlugin.getDefault().getConfigurationHelper(m_project).isDistributedMethod(method);
  }

  private void internalAddDistributed(String expr) {
    if (expr != null && expr.length() > 0) {
      DistributedMethods dms = ensureDistributedMethods();
      MethodExpression me = dms.addNewMethodExpression();
      me.setStringValue(expr);
      createTableItem(me);
    }
  }

  private static class Layout {
    private static final String DISTRIBUTED_METHODS = "Distributed Methods";
    private static final String ADD                 = "Add...";
    private static final String REMOVE              = "Remove";

    private final Button        m_addButton;
    private final Button        m_removeButton;
    private final Table         m_table;

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
      label.setText(DISTRIBUTED_METHODS);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_table = new Table(sidePanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_table.setHeaderVisible(true);
      m_table.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(sidePanel, m_table, new int[] { 3, 1 });
      SWTUtil.makeTableColumnsEditable(m_table, new int[] { 0 });
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.heightHint = SWTUtil.tableRowsToPixels(m_table, 3);
      m_table.setLayoutData(gridData);

      TableColumn column = new TableColumn(m_table, SWT.NONE);
      column.setText(DISTRIBUTED_METHODS);
      column.pack();

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

  private class AddHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      NavigatorBehavior behavior = new MethodBehavior();
      ExpressionChooser chooser = new ExpressionChooser(getShell(), behavior.getTitle(), MethodBehavior.ADD_MSG,
                                                        m_project, behavior);
      chooser.addValueListener(new UpdateEventListener() {
        public void handleUpdate(UpdateEvent updateEvent) {
          String[] items = (String[]) updateEvent.data;
          for (int i = 0; i < items.length; i++) {
            internalAddDistributed(items[i]);
          }
          fireDistributedMethodsChanged();
        }
      });
      chooser.open();
    }
  }

  private class RemoveHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      int[] selection = m_layout.m_table.getSelectionIndices();

      for (int i = selection.length - 1; i >= 0; i--) {
        ensureDistributedMethods().removeMethodExpression(selection[i]);
      }
      m_layout.m_table.remove(selection);
      testRemoveDistributedMethods();
    }
  }

  private class TableSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_removeButton.setEnabled(true);
    }
  }

  private class TableDataListener implements Listener {
    public void handleEvent(Event e) {
      TableItem item = (TableItem) e.item;
      MethodExpression expr = (MethodExpression) item.getData();
      expr.setStringValue(item.getText(e.index));
      fireDistributedMethodsChanged();
    }
  }
}
