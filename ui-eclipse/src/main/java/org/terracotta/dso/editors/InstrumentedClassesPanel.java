/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.editors.chooser.ClassBehavior;
import org.terracotta.dso.editors.chooser.ExpressionChooser;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.dso.editors.xmlbeans.XmlBooleanToggle;
import org.terracotta.ui.util.SWTUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.OnLoad;

public class InstrumentedClassesPanel extends ConfigurationEditorPanel {
  private IProject                     m_project;
  private DsoApplication               m_dsoApp;
  private InstrumentedClasses          m_instrumentedClasses;

  private final Layout                 m_layout;

  private TableSelectionListener       m_tableSelectionListener;
  private TableDataListener            m_tableDataListener;
  private AddRuleHandler               m_addRuleHandler;
  private RemoveRuleHandler            m_removeRuleHandler;
  private MoveUpHandler                m_moveUpHandler;
  private MoveDownHandler              m_moveDownHandler;
  private HonorTransientHandler        m_honorTransientHandler;
  private OnLoadDoNothingHandler       m_onLoadDoNothingHandler;
  private OnLoadCallMethodHandler      m_onLoadCallMethodHandler;
  private OnLoadCallMethodTextHandler  m_onLoadCallMethodTextHandler;
  private OnLoadExecuteCodeHandler     m_onLoadExecuteCodeHandler;
  private OnLoadExecuteCodeTextHandler m_onLoadExecuteCodeTextHandler;

  private static final String          INCLUDE = "include";
  private static final String          EXCLUDE = "exclude";

  public InstrumentedClassesPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    m_tableSelectionListener = new TableSelectionListener();
    m_tableDataListener = new TableDataListener();
    m_addRuleHandler = new AddRuleHandler();
    m_removeRuleHandler = new RemoveRuleHandler();
    m_moveUpHandler = new MoveUpHandler();
    m_moveDownHandler = new MoveDownHandler();
    m_honorTransientHandler = new HonorTransientHandler();
    m_onLoadDoNothingHandler = new OnLoadDoNothingHandler();
    m_onLoadCallMethodHandler = new OnLoadCallMethodHandler();
    m_onLoadCallMethodTextHandler = new OnLoadCallMethodTextHandler();
    m_onLoadExecuteCodeHandler = new OnLoadExecuteCodeHandler();
    m_onLoadExecuteCodeTextHandler = new OnLoadExecuteCodeTextHandler();
  }

  public boolean hasAnySet() {
    return m_instrumentedClasses != null
        && (m_instrumentedClasses.sizeOfExcludeArray() > 0 || m_instrumentedClasses.sizeOfIncludeArray() > 0);
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_instrumentedClasses == null) {
      removeListeners();
      m_instrumentedClasses = m_dsoApp.addNewInstrumentedClasses();
      updateChildren();
      addListeners();
    }
  }
  
  private void testInstrumentedClasses() {
    if (!hasAnySet() && m_dsoApp.getInstrumentedClasses() != null) {
      m_dsoApp.unsetInstrumentedClasses();
      m_instrumentedClasses = null;
      fireXmlObjectStructureChanged(m_dsoApp);
    }
    handleTableSelection();
  }

  private void addListeners() {
    m_layout.m_table.addSelectionListener(m_tableSelectionListener);
    m_layout.m_table.addListener(SWT.SetData, m_tableDataListener);
    m_layout.m_addButton.addSelectionListener(m_addRuleHandler);
    m_layout.m_removeButton.addSelectionListener(m_removeRuleHandler);
    m_layout.m_moveUpButton.addSelectionListener(m_moveUpHandler);
    m_layout.m_moveDownButton.addSelectionListener(m_moveDownHandler);
    m_layout.m_honorTransientCheck.addSelectionListener(m_honorTransientHandler);
    m_layout.m_doNothingRadio.addSelectionListener(m_onLoadDoNothingHandler);
    m_layout.m_callAMethodRadio.addSelectionListener(m_onLoadCallMethodHandler);
    m_layout.m_callAMethodText.addFocusListener(m_onLoadCallMethodTextHandler);
    m_layout.m_executeCodeRadio.addSelectionListener(m_onLoadExecuteCodeHandler);
    m_layout.m_executeCodeText.addFocusListener(m_onLoadExecuteCodeTextHandler);
  }

  private void removeListeners() {
    m_layout.m_table.removeSelectionListener(m_tableSelectionListener);
    m_layout.m_table.removeListener(SWT.SetData, m_tableDataListener);
    m_layout.m_addButton.removeSelectionListener(m_addRuleHandler);
    m_layout.m_removeButton.removeSelectionListener(m_removeRuleHandler);
    m_layout.m_moveUpButton.removeSelectionListener(m_moveUpHandler);
    m_layout.m_moveDownButton.removeSelectionListener(m_moveDownHandler);
    m_layout.m_honorTransientCheck.removeSelectionListener(m_honorTransientHandler);
    m_layout.m_doNothingRadio.removeSelectionListener(m_onLoadDoNothingHandler);
    m_layout.m_callAMethodRadio.removeSelectionListener(m_onLoadCallMethodHandler);
    m_layout.m_callAMethodText.removeFocusListener(m_onLoadCallMethodTextHandler);
    m_layout.m_executeCodeRadio.removeSelectionListener(m_onLoadExecuteCodeHandler);
    m_layout.m_executeCodeText.removeFocusListener(m_onLoadExecuteCodeTextHandler);
  }

  public void updateChildren() {
    initTableItems();
    handleTableSelection();
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
    m_instrumentedClasses = m_dsoApp != null ? m_dsoApp.getInstrumentedClasses() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_dsoApp = null;
    m_instrumentedClasses = null;

    setEnabled(false);
  }

  private void initTableItems() {
    m_layout.m_table.removeAll();
    if (m_instrumentedClasses == null) return;
    SWTUtil.makeTableComboItem(m_layout.m_table, 0, new String[] { INCLUDE, EXCLUDE });
    updateRules();
    if (m_layout.m_table.getItemCount() > 0) {
      m_layout.m_table.setSelection(0);
    }
  }

  private TableItem createIncludeTableItem(Include include) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    item.setText(Layout.RULE_COLUMN, INCLUDE);
    item.setText(Layout.EXPRESSION_COLUMN, include.getClassExpression() + "");
    item.setData(include);
    return item;
  }

  private TableItem createExcludeTableItem(ClassExpression exclude) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    item.setText(Layout.RULE_COLUMN, EXCLUDE);
    item.setText(Layout.EXPRESSION_COLUMN, exclude.getStringValue());
    item.setData(exclude);
    return item;
  }

  private void updateRules() {
    m_layout.m_table.removeAll();
    if(m_instrumentedClasses != null) {
      XmlObject[] classes = m_instrumentedClasses.selectPath("*");
      for (int i = 0; i < classes.length; i++) {
        if (classes[i] instanceof Include) {
          createIncludeTableItem((Include) classes[i]);
        } else {
          createExcludeTableItem((ClassExpression) classes[i]);
        }
      }
    }
  }

  private void internalAddInclude(String classExpr) {
    ensureXmlObject();
    Include include = m_instrumentedClasses.addNewInclude();
    include.setClassExpression(classExpr);
    createIncludeTableItem(include);
    m_layout.m_table.select(m_layout.m_table.getItemCount() - 1);
  }

  XmlObject getSelectedRule() {
    int i = m_layout.m_table.getSelectionIndex();
    return (i != -1) ? getRuleAt(i) : null;
  }

  OnLoad ensureSelectedOnLoad() {
    XmlObject rule = getSelectedRule();
    OnLoad onLoad = null;

    if (rule instanceof Include) {
      Include include = (Include) rule;
      if ((onLoad = include.getOnLoad()) == null) {
        onLoad = include.addNewOnLoad();
      }
    }
    return onLoad;
  }

  XmlObject getRuleAt(int index) {
    TableItem item = m_layout.m_table.getItem(index);
    return (XmlObject) item.getData();
  }

  void setRuleAt(int index, XmlObject rule) {
    try {
      TableItem item = m_layout.m_table.getItem(index);
      item.setData(rule);
      if (rule instanceof Include) {
        Include include = (Include) rule;
        item.setText(Layout.RULE_COLUMN, INCLUDE);
        item.setText(Layout.EXPRESSION_COLUMN, include.getClassExpression() + "");
      } else {
        ClassExpression exclude = (ClassExpression) rule;
        item.setText(Layout.RULE_COLUMN, EXCLUDE);
        item.setText(Layout.EXPRESSION_COLUMN, exclude.getStringValue());
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  String getExpressionAt(int index) {
    XmlObject rule = getRuleAt(index);

    if (rule instanceof Include) {
      return ((Include) rule).getClassExpression();
    } else {
      return ((ClassExpression) rule).getStringValue();
    }
  }

  int indexOfInclude(Include include) {
    int index = -1;
    if (m_instrumentedClasses != null) {
      Include[] includes = m_instrumentedClasses.getIncludeArray();
      for (int i = 0; i < includes.length; i++) {
        if (includes[i] == include) return i;
      }
    }
    return index;
  }

  int indexOfExclude(ClassExpression exclude) {
    int index = -1;
    if (m_instrumentedClasses != null) {
      ClassExpression[] excludes = m_instrumentedClasses.xgetExcludeArray();
      for (int i = 0; i < excludes.length; i++) {
        if (excludes[i] == exclude) return i;
      }
    }
    return index;
  }

  public void removeRule(int index) {
    XmlObject rule = getRuleAt(index);
    Node ruleNode = rule.getDomNode();
    Node topNode = m_instrumentedClasses.getDomNode();

    topNode.removeChild(ruleNode);
  }

  public void moveRuleUp(int index) {
    XmlObject rule = getRuleAt(index);
    Node ruleNode = rule.getDomNode();
    Node topNode = m_instrumentedClasses.getDomNode();
    NodeList topNodeList = topNode.getChildNodes();
    int listSize = topNodeList.getLength();
    Node prevNode;

    for (int i = 0; i < listSize; i++) {
      if (ruleNode == topNodeList.item(i)) {
        while (--i >= 0) {
          prevNode = topNodeList.item(i);
          if (prevNode.getNodeType() == Node.ELEMENT_NODE) {
            topNode.removeChild(ruleNode);
            topNode.insertBefore(ruleNode, prevNode);
            fireInstrumentationRulesChanged();
            return;
          }
        }
      }
    }
  }

  public void moveRuleDown(int index) {
    XmlObject rule = getRuleAt(index);
    Node ruleNode = rule.getDomNode();
    Node topNode = m_instrumentedClasses.getDomNode();
    NodeList topNodeList = topNode.getChildNodes();
    int listSize = topNodeList.getLength();
    Node nextNode;

    for (int i = 0; i < listSize; i++) {
      if (ruleNode == topNodeList.item(i)) {
        while (++i < listSize) {
          nextNode = topNodeList.item(i);
          if (nextNode.getNodeType() == Node.ELEMENT_NODE) {
            while (++i < listSize) {
              nextNode = topNodeList.item(i);
              if (nextNode.getNodeType() == Node.ELEMENT_NODE) {
                topNode.removeChild(ruleNode);
                topNode.insertBefore(ruleNode, nextNode);
                fireInstrumentationRulesChanged();
                return;
              }
            }
            topNode.removeChild(ruleNode);
            topNode.appendChild(ruleNode);
            updateRules();
            return;
          }
        }
      }
    }
  }

  public void replace(int index, XmlObject newRule) {
    Node newRuleNode = newRule.getDomNode();
    XmlObject rule = getRuleAt(index);
    Node ruleNode = rule.getDomNode();
    Node topNode = m_instrumentedClasses.getDomNode();

    topNode.replaceChild(newRuleNode, ruleNode);
    fireInstrumentationRulesChanged();
  }

  public void toggleRuleType(int index) {
    String expr = getExpressionAt(index);
    XmlObject xmlObj = getRuleAt(index);

    if (xmlObj instanceof Include) {
      ClassExpression ce = m_instrumentedClasses.addNewExclude();
      ce.setStringValue(expr);
      xmlObj = ce;
    } else {
      Include include = m_instrumentedClasses.addNewInclude();
      include.setClassExpression(expr);
      xmlObj = include;
    }

    replace(index, xmlObj);
  }

  private void handleTableSelection() {
    int index = m_layout.m_table.getSelectionIndex();
    if (index == -1) {
      m_layout.m_removeButton.setEnabled(false);
      m_layout.m_moveUpButton.setEnabled(false);
      m_layout.m_moveDownButton.setEnabled(false);
      m_layout.resetIncludeAttributes();
      return;
    } else {
      int count = m_layout.m_table.getItemCount();
      m_layout.m_removeButton.setEnabled(true);
      if (index == 0) {
        m_layout.m_moveUpButton.setEnabled(false);
        m_layout.m_moveDownButton.setEnabled(count > 1);
      } else if (index == count - 1) {
        m_layout.m_moveUpButton.setEnabled(true);
        m_layout.m_moveDownButton.setEnabled(false);
      } else {
        m_layout.m_moveUpButton.setEnabled(true);
        m_layout.m_moveDownButton.setEnabled(true);
      }
      TableItem item = m_layout.m_table.getItem(index);
      if (item.getText(Layout.RULE_COLUMN).equals(INCLUDE)) {
        initIncludeAttributes();
      } else if (item.getText(Layout.RULE_COLUMN).equals(EXCLUDE)) {
        m_layout.resetIncludeAttributes();
      }
    }
  }

  private void initIncludeAttributes() {
    int selected = m_layout.m_table.getSelectionIndex();
    XmlObject rule = (XmlObject) m_layout.m_table.getItem(selected).getData();

    if (rule instanceof Include) {
      Include include = (Include) rule;
      m_layout.resetIncludeAttributes();
      m_layout.enableIncludeAttributes();
      m_layout.setInclude(include);
    }
  }

  private void handleRemoveOnLoad() {
    m_layout.m_callAMethodText.setText("");
    m_layout.m_callAMethodText.setEnabled(false);
    m_layout.m_executeCodeText.setText("");
    m_layout.m_executeCodeText.setEnabled(false);

    Include include = (Include) getSelectedRule();
    include.unsetOnLoad();
    fireIncludeRuleChanged(indexOfInclude(include));
  }

  private class Layout {
    private static final int    RULE_COLUMN           = 0;
    private static final int    EXPRESSION_COLUMN     = 1;
    private static final String UP                    = "/com/tc/admin/icons/view_menu.gif";
    private static final String DOWN                  = "/com/tc/admin/icons/hide_menu.gif";
    private static final String INSTRUMENTATION_RULES = "Instrumentation Rules";
    private static final String RULE                  = "Rule";
    private static final String EXPRESSION            = "Expression";
    private static final String DETAILS               = "Details";
    private static final String ADD                   = "Add...";
    private static final String REMOVE                = "Remove";
    private static final String RAISE_PRIORITY        = "Raise priority";
    private static final String LOWER_PRIORITY        = "Lower priority";
    private static final String HONOR_TRANSIENT       = "Honor Transient";
    private static final String ON_LOAD               = "On Load Behavior";
    private static final String DO_NOTHING            = "Do Nothing";
    private static final String CALL_A_METHOD         = "Call a Method";
    private static final String EXECUTE_CODE          = "Execute Code";

    private Table               m_table;
    private Button              m_honorTransientCheck;
    private Button              m_doNothingRadio;
    private Button              m_callAMethodRadio;
    private Text                m_callAMethodText;
    private Button              m_executeCodeRadio;
    private Text                m_executeCodeText;
    private Button              m_addButton;
    private Button              m_removeButton;
    private Button              m_moveUpButton;
    private Button              m_moveDownButton;
    private Group               m_onLoadGroup;
    private Group               m_detailGroup;

    private void resetIncludeAttributes() {
      m_detailGroup.setEnabled(false);
      m_onLoadGroup.setEnabled(false);
      m_honorTransientCheck.setSelection(false);
      m_honorTransientCheck.setEnabled(false);
      m_doNothingRadio.setSelection(false);
      m_doNothingRadio.setEnabled(false);
      m_callAMethodRadio.setSelection(false);
      m_callAMethodRadio.setEnabled(false);
      m_callAMethodText.setText("");
      m_callAMethodText.setEnabled(false);
      m_executeCodeRadio.setSelection(false);
      m_executeCodeRadio.setEnabled(false);
      m_executeCodeText.setText("");
      m_executeCodeText.setEnabled(false);
    }

    private void enableIncludeAttributes() {
      m_detailGroup.setEnabled(true);
      m_onLoadGroup.setEnabled(true);
      m_honorTransientCheck.setEnabled(true);
      m_doNothingRadio.setEnabled(true);
      m_callAMethodRadio.setEnabled(true);
      m_executeCodeRadio.setEnabled(true);
    }

    private void setInclude(Include include) {
      ((XmlBooleanToggle) m_honorTransientCheck.getData()).setup(include);

      if (include != null) {
        OnLoad onLoad = include.getOnLoad();
        if (onLoad != null) {
          if (onLoad.isSetExecute()) {
            m_executeCodeRadio.setSelection(true);
            m_executeCodeText.setText(onLoad.getExecute());
            m_executeCodeText.setEnabled(true);
            m_callAMethodText.setEnabled(false);
          } else if (onLoad.isSetMethod()) {
            m_callAMethodRadio.setSelection(true);
            m_callAMethodText.setText(onLoad.getMethod());
            m_executeCodeText.setEnabled(false);
            m_callAMethodText.setEnabled(true);
          }
        } else {
          m_doNothingRadio.setSelection(true);
        }
      }
    }

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
      label.setText(INSTRUMENTATION_RULES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_table = new Table(sidePanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_table.setHeaderVisible(true);
      m_table.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(sidePanel, m_table, new int[] { 1, 4 });
      SWTUtil.makeTableColumnsEditable(m_table, new int[] { 1 });
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.heightHint = SWTUtil.tableRowsToPixels(m_table, 3);
      m_table.setLayoutData(gridData);

      TableColumn ruleColumn = new TableColumn(m_table, SWT.NONE, RULE_COLUMN);
      ruleColumn.setResizable(true);
      ruleColumn.setText(RULE);
      ruleColumn.pack();

      TableColumn expressionColumn = new TableColumn(m_table, SWT.NONE, EXPRESSION_COLUMN);
      expressionColumn.setResizable(true);
      expressionColumn.setText(EXPRESSION);
      expressionColumn.pack();

      m_detailGroup = new Group(sidePanel, SWT.SHADOW_NONE);
      m_detailGroup.setText(DETAILS);
      m_detailGroup.setEnabled(false);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 5;
      m_detailGroup.setLayout(gridLayout);
      m_detailGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      m_honorTransientCheck = new Button(m_detailGroup, SWT.CHECK);
      m_honorTransientCheck.setText(HONOR_TRANSIENT);
      m_honorTransientCheck.setEnabled(false);
      initBooleanField(m_honorTransientCheck, Include.class, "honor-transient");

      //new Label(m_detailGroup, SWT.NONE); // filler

      m_onLoadGroup = new Group(m_detailGroup, SWT.SHADOW_NONE);
      m_onLoadGroup.setText(ON_LOAD);
      m_onLoadGroup.setEnabled(false);
      gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 5;
      m_onLoadGroup.setLayout(gridLayout);
      m_onLoadGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

      m_doNothingRadio = new Button(m_onLoadGroup, SWT.RADIO);
      m_doNothingRadio.setText(DO_NOTHING);
      m_doNothingRadio.setEnabled(false);
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      m_doNothingRadio.setLayoutData(gridData);

      m_callAMethodRadio = new Button(m_onLoadGroup, SWT.RADIO);
      m_callAMethodRadio.setText(CALL_A_METHOD);
      m_callAMethodRadio.setEnabled(false);

      m_callAMethodText = new Text(m_onLoadGroup, SWT.BORDER);
      m_callAMethodText.setEnabled(false);
      int width = SWTUtil.textColumnsToPixels(m_callAMethodText, 50);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.minimumWidth = width;
      m_callAMethodText.setLayoutData(gridData);

      m_executeCodeRadio = new Button(m_onLoadGroup, SWT.RADIO);
      m_executeCodeRadio.setEnabled(false);
      m_executeCodeRadio.setText(EXECUTE_CODE);
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      m_executeCodeRadio.setLayoutData(gridData);

      m_executeCodeText = new Text(m_onLoadGroup, SWT.BORDER | SWT.MULTI);
      m_executeCodeText.setEnabled(false);
      gridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
      gridData.horizontalSpan = 2;
      gridData.minimumHeight = SWTUtil.textRowsToPixels(m_executeCodeText, 4);
      m_executeCodeText.setLayoutData(gridData);

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addButton = new Button(buttonPanel, SWT.PUSH);
      m_addButton.setText(ADD);
      m_addButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_addButton);

      m_removeButton = new Button(buttonPanel, SWT.PUSH);
      m_removeButton.setText(REMOVE);
      m_removeButton.setEnabled(false);
      m_removeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeButton);

      new Label(buttonPanel, SWT.NONE); // filler

      m_moveUpButton = new Button(buttonPanel, SWT.PUSH);
      m_moveUpButton.setText(LOWER_PRIORITY);
      m_moveUpButton.setEnabled(false);
      m_moveUpButton.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(UP)));
      m_moveUpButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_moveUpButton);

      m_moveDownButton = new Button(buttonPanel, SWT.PUSH);
      m_moveDownButton.setText(RAISE_PRIORITY);
      m_moveDownButton.setEnabled(false);
      m_moveDownButton.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(DOWN)));
      m_moveDownButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_moveDownButton);
    }
  }

  class TableSelectionListener extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      handleTableSelection();
    }
  }

  class TableDataListener implements Listener {
    public void handleEvent(Event event) {
      TableItem item = (TableItem) event.item;
      int index = m_layout.m_table.indexOf(item);

      if (event.index == Layout.RULE_COLUMN) {
        toggleRuleTypeLater(index);
      } else {
        XmlObject xmlObj = (XmlObject) item.getData();
        String text = item.getText(event.index).trim();
        
        if (xmlObj instanceof ClassExpression) {
          ClassExpression exclude = (ClassExpression) xmlObj;
          
          if(text.length() == 0) {
            item.setText(exclude.getStringValue());
            removeRuleLater(index);
          } else {
            exclude.setStringValue(text);
            fireExcludeRuleChanged(indexOfExclude(exclude));
          }
        } else {
          Include include = (Include) xmlObj;
          
          if(text.length() == 0) {
            item.setText(include.getClassExpression());
            removeRuleLater(index);
          } else {
            include.setClassExpression(text);
            fireIncludeRuleChanged(indexOfInclude(include));
          }
        }
      }
    }
 
    private void toggleRuleTypeLater(final int index) {
      getDisplay().asyncExec(new Runnable() {
        public void run() {
          m_layout.m_table.setRedraw(false);
          try {
            toggleRuleType(index);
            m_layout.m_table.setSelection(index);
            handleTableSelection();
          } finally {
            m_layout.m_table.setRedraw(true);
          }
        }
      });
    }
    
    private void removeRuleLater(final int index) {
      getDisplay().asyncExec(new Runnable() {
        public void run() {
          removeRule(index);
          fireInstrumentationRulesChanged();
          testInstrumentedClasses();
        }
      });
    }
  }

  class AddRuleHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      NavigatorBehavior behavior = new ClassBehavior();
      final ExpressionChooser chooser = new ExpressionChooser(getShell(), behavior.getTitle(), ClassBehavior.ADD_MSG,
          m_project, behavior);
      chooser.addValueListener(new UpdateEventListener() {
        public void handleUpdate(UpdateEvent updateEvent) {
          String[] items = (String[]) updateEvent.data;
          for (int i = 0; i < items.length; i++) {
            internalAddInclude(items[i]);
          }
          fireInstrumentationRulesChanged();
        }
      });
      chooser.open();
    }
  }

  class RemoveRuleHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      int[] selection = m_layout.m_table.getSelectionIndices();
      for (int i = selection.length - 1; i >= 0; i--) {
        removeRule(selection[i]);
      }
      fireInstrumentationRulesChanged();
      testInstrumentedClasses();
    }
  }

  class MoveUpHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      int index = m_layout.m_table.getSelectionIndex();
      moveRuleUp(index);
      m_layout.m_table.setSelection(index - 1);
    }
  }

  class MoveDownHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_table.forceFocus();
      int index = m_layout.m_table.getSelectionIndex();
      moveRuleDown(index);
      m_layout.m_table.setSelection(index - 1);
    }
  }

  class HonorTransientHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      fireIncludeRuleChanged(indexOfInclude((Include) getSelectedRule()));
    }
  }

  class OnLoadDoNothingHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      handleRemoveOnLoad();
    }
  }

  class OnLoadCallMethodHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_callAMethodText.setEnabled(true);
      m_layout.m_executeCodeText.setText("");
      m_layout.m_executeCodeText.setEnabled(false);

      ensureSelectedOnLoad().unsetExecute();
      fireIncludeRuleChanged(indexOfInclude((Include) getSelectedRule()));
    }
  }

  class OnLoadExecuteCodeHandler extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      m_layout.m_executeCodeText.setEnabled(true);
      m_layout.m_callAMethodText.setText("");
      m_layout.m_callAMethodText.setEnabled(false);

      ensureSelectedOnLoad().unsetMethod();
      fireIncludeRuleChanged(indexOfInclude((Include) getSelectedRule()));
    }
  }

  class OnLoadExecuteCodeTextHandler extends FocusAdapter {
    public void focusLost(FocusEvent e) {
      ensureSelectedOnLoad().setExecute(m_layout.m_executeCodeText.getText());
      fireIncludeRuleChanged(indexOfInclude((Include) getSelectedRule()));
    }
  }

  class OnLoadCallMethodTextHandler extends FocusAdapter {
    public void focusLost(FocusEvent e) {
      ensureSelectedOnLoad().setMethod(m_layout.m_callAMethodText.getText());
      fireIncludeRuleChanged(indexOfInclude((Include) getSelectedRule()));
    }
  }

  public void includeRuleChanged(IProject project, int index) {
    if (project.equals(getProject())) {
      int selIndex = m_layout.m_table.getSelectionIndex();
      updateRules();
      if (selIndex != -1) {
        m_layout.m_table.setSelection(selIndex);
        handleTableSelection();
      }
    }
  }

  public void excludeRulesChanged(IProject project) {
    if (project.equals(getProject())) {
      initTableItems();
    }
  }

  public void excludeRuleChanged(IProject project, int index) {
    if (project.equals(getProject())) {
      int selIndex = m_layout.m_table.getSelectionIndex();
      updateRules();
      if (selIndex != -1) {
        m_layout.m_table.setSelection(selIndex);
        handleTableSelection();
      }
    }
  }

  public void includeRulesChanged(IProject project) {
//    if (project.equals(getProject())) {
//      initTableItems();
//    }
  }
}
