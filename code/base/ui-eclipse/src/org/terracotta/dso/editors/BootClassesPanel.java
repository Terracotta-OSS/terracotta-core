/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import org.eclipse.ui.dialogs.SelectionDialog;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.QualifiedClassName;

public class BootClassesPanel extends ConfigurationEditorPanel {

  private static final String CLASS_SELECT_TITLE   = "DSO Application Configuration";
  private static final String CLASS_SELECT_MESSAGE = "Select system classes to add to DSO Boot Jar";
  private final Layout        m_layout;
  private State               m_state;

  public BootClassesPanel(Composite parent, int style) {
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
    m_layout.m_table.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_layout.m_removeButton.setEnabled(true);
      }
    });
    m_layout.m_table.addListener(SWT.SetData, new Listener() {
      public void handleEvent(Event e) {
        if (!m_isActive) return;
        TableItem item = (TableItem) e.item;
        XmlConfigEvent event = new XmlConfigEvent(item.getText(e.index), null, null, XmlConfigEvent.BOOT_CLASS);
        event.index = m_layout.m_table.getSelectionIndex();
        m_state.xmlContext.notifyListeners(event);
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        if (event.data == null) return;
        TableItem[] items = m_layout.m_table.getItems();
        for (int i = 0; i < items.length; i++) {
          if (items[i].getText().equals(event.data)) {
            items[i].setText((String) event.data);
          }
        }
      }
    }, XmlConfigEvent.BOOT_CLASS, this);
    // - remove class
    m_layout.m_removeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_table.forceFocus();
        int selected = m_layout.m_table.getSelectionIndex();
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.DELETE_BOOT_CLASS);
        event.index = selected;
        setActive(true);
        m_state.xmlContext.notifyListeners(event);
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        m_layout.m_table.remove(((XmlConfigEvent) e).index);
        m_layout.m_removeButton.setEnabled(false);
      }
    }, XmlConfigEvent.REMOVE_BOOT_CLASS, this);
    // - add class
    m_layout.m_addButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_table.forceFocus();
        setActive(true);
        IJavaProject javaProject = JavaCore.create(m_state.project);
        int filter = IJavaSearchScope.SYSTEM_LIBRARIES;
        IJavaElement[] elements = new IJavaElement[] { javaProject };
        IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements, filter);
        int style = IJavaElementSearchConstants.CONSIDER_ALL_TYPES;
        SelectionDialog dialog;
        try {
          dialog = JavaUI.createTypeDialog(null, null, scope, style, true);
        } catch (JavaModelException jme) {
          jme.printStackTrace();
          return;
        }
        dialog.setTitle(CLASS_SELECT_TITLE);
        dialog.setMessage(CLASS_SELECT_MESSAGE);
        dialog.open();
        Object[] items = dialog.getResult();
        if (items == null) return;
        for (int i = 0; i < items.length; i++) {
          XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.CREATE_BOOT_CLASS);
          event.data = ((IType) items[i]).getFullyQualifiedName();
          m_state.xmlContext.notifyListeners(event);
        }
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        QualifiedClassName include = (QualifiedClassName) castEvent(e).element;
        createTableItem(include);
      }
    }, XmlConfigEvent.NEW_BOOT_CLASS, this);
  }

  // ================================================================================
  // HELPERS
  // ================================================================================

  private void initTableItems() {
    AdditionalBootJarClasses bootClasses = m_state.xmlContext.getParentElementProvider().hasAdditionalBootJarClasses();
    if (bootClasses == null) return;
    XmlObject[] includes = bootClasses.selectPath("*");
    for (int i = 0; i < includes.length; i++) {
      createTableItem((QualifiedClassName) includes[i]);
    }
  }

  private void createTableItem(QualifiedClassName element) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    item.setText(element.getStringValue());
    item.setData(element);
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

    private static final String BOOT_CLASSES = "Boot Classes";
    private static final String ADD          = "Add...";
    private static final String REMOVE       = "Remove";

    private Button              m_addButton;
    private Button              m_removeButton;
    private Table               m_table;

    public void reset() {
      m_removeButton.setEnabled(false);
      m_table.removeAll();
    }

    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(BOOT_CLASSES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_table = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_table.setHeaderVisible(false);
      m_table.setLinesVisible(true);
      SWTUtil.makeTableColumnsEditable(m_table, new int[] { 0 });

      TableColumn column = new TableColumn(m_table, SWT.NONE);
      column.setText(BOOT_CLASSES);
      column.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
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
}
