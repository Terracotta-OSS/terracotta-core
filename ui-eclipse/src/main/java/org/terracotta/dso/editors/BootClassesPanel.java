/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
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
import org.eclipse.ui.dialogs.SelectionDialog;
import org.terracotta.dso.PatternHelper;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.QualifiedClassName;

public class BootClassesPanel extends ConfigurationEditorPanel {
  private IProject                     m_project;
  private DsoApplication               m_dsoApp;
  private AdditionalBootJarClasses     m_bootClasses;

  private final Layout                 m_layout;

  private final AddHandler             m_addHandler;
  private final RemoveHandler          m_removeHandler;
  private final TableSelectionListener m_tableSelectionListener;
  private final TableDataListener      m_tableDataListener;

  private static final String          CLASS_SELECT_TITLE   = "DSO Application Configuration";
  private static final String          CLASS_SELECT_MESSAGE = "Select system classes to add to DSO Boot Jar";

  public BootClassesPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    m_addHandler = new AddHandler();
    m_removeHandler = new RemoveHandler();
    m_tableSelectionListener = new TableSelectionListener();
    m_tableDataListener = new TableDataListener();
  }

  public boolean hasAnySet() {
    return m_bootClasses != null && m_bootClasses.sizeOfIncludeArray() > 0;
  }

  private AdditionalBootJarClasses ensureAdditionalBootClasses() {
    if (m_bootClasses == null) {
      ensureXmlObject();
    }
    return m_bootClasses;
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_bootClasses == null) {
      removeListeners();
      m_bootClasses = m_dsoApp.addNewAdditionalBootJarClasses();
      updateChildren();
      addListeners();
    }
  }

  private void testRemoveBootClasses() {
    if (!hasAnySet() && m_dsoApp.getAdditionalBootJarClasses() != null) {
      m_dsoApp.unsetAdditionalBootJarClasses();
      m_bootClasses = null;
      fireXmlObjectStructureChanged(m_dsoApp);
    }
    fireBootClassesChanged();
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
    m_bootClasses = m_dsoApp != null ? m_dsoApp.getAdditionalBootJarClasses() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_dsoApp = null;
    m_bootClasses = null;

    setEnabled(false);
  }

  private void initTableItems() {
    m_layout.m_table.removeAll();
    if (m_bootClasses == null) return;
    XmlObject[] includes = m_bootClasses.selectPath("*");
    for (int i = 0; i < includes.length; i++) {
      createTableItem((QualifiedClassName) includes[i]);
    }
    if (includes.length > 0) {
      m_layout.m_table.setSelection(0);
    }
  }

  private void createTableItem(QualifiedClassName element) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    item.setText(element.getStringValue());
    item.setData(element);
  }

  private void internalAddBootClass(String typeName) {
    AdditionalBootJarClasses bootClasses = ensureAdditionalBootClasses();
    bootClasses.addInclude(typeName);
    createTableItem(bootClasses.xgetIncludeArray(bootClasses.sizeOfIncludeArray() - 1));

    int row = m_layout.m_table.getItemCount() - 1;
    m_layout.m_table.setSelection(row);
  }

  public boolean isBootClass(String typeName) {
    AdditionalBootJarClasses bootClasses = ensureAdditionalBootClasses();

    for (int i = 0; i < bootClasses.sizeOfIncludeArray(); i++) {
      if (typeName.equals(bootClasses.getIncludeArray(i))) { return true; }
    }

    return false;
  }

  private static class Layout {
    private static final String BOOT_CLASSES = "Boot Classes";
    private static final String ADD          = "Add...";
    private static final String REMOVE       = "Remove";

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
      label.setText(BOOT_CLASSES);
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
      column.setText(BOOT_CLASSES);
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

  class AddHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      IJavaProject javaProject = JavaCore.create(m_project);
      int filter = IJavaSearchScope.SYSTEM_LIBRARIES;
      IJavaElement[] elements = new IJavaElement[] { javaProject };
      IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements, filter);
      int style = IJavaElementSearchConstants.CONSIDER_ALL_TYPES;
      SelectionDialog dialog;
      try {
        dialog = JavaUI.createTypeDialog(getShell(), null, scope, style, true);
      } catch (JavaModelException jme) {
        jme.printStackTrace();
        return;
      }
      dialog.setTitle(CLASS_SELECT_TITLE);
      dialog.setMessage(CLASS_SELECT_MESSAGE);
      dialog.open();
      Object[] items = dialog.getResult();
      if (items != null) {
        for (int i = 0; i < items.length; i++) {
          internalAddBootClass(PatternHelper.getFullyQualifiedName((IType) items[i]));
        }
        fireBootClassesChanged();
      }
    }
  }

  class RemoveHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      int[] selection = m_layout.m_table.getSelectionIndices();

      for (int i = selection.length - 1; i >= 0; i--) {
        ensureAdditionalBootClasses().removeInclude(selection[i]);
      }
      m_layout.m_table.remove(selection);
      testRemoveBootClasses();
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
      QualifiedClassName qcn = (QualifiedClassName) item.getData();
      qcn.setStringValue(item.getText(e.index));
      fireBootClassesChanged();
    }
  }
}
