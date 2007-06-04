/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.MultiChangeSignaller;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.ConfigurationEditor;
import org.terracotta.ui.util.EmptyIterator;
import org.terracotta.ui.util.SWTUtil;

import com.tc.admin.common.NonPortableMessages;
import com.tc.admin.common.NonPortableResolutionAction;
import com.tc.admin.common.NonPortableWorkState;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.NonPortableObjectState;
import com.terracottatech.config.Include;
import com.terracottatech.config.OnLoad;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class NonPortableObjectDialog extends MessageDialog {
  private IJavaProject                      fJavaProject;
  private NonPortableObjectEvent            fEvent;
  private Table                             fIssueTable;
  private Tree                              fObjectTree;
  private CLabel                            fSummaryLabel;
  private Text                              fIssueDescription;
  private Group                             fDetailsPanel;
  private Group                             fResolutionsPanel;
  private CheckboxTreeViewer                fActionTreeViewer;
  private Composite                         fActionPanel;
  private StackLayout                       fActionStackLayout;
  private IncludeRulePanel                  fIncludeRuleView;
  private BootTypesPanel                    fBootTypesView;
  private Label                             fNoActionView;
  private Button                            fPreviousIssueButton, fNextIssueButton, fApplyButton;
  private TcConfig                          fNewConfig;
  private ConfigurationHelper               fConfigHelper;

  private static final Image                NOT_PORTABLE_ICON     = JavaPluginImages
                                                                      .get(JavaPluginImages.IMG_FIELD_PRIVATE);
  private static final Image                NEVER_PORTABLE_ICON   = JavaPluginImages
                                                                      .get(JavaPluginImages.IMG_FIELD_PRIVATE);
  private static final Image                TRANSIENT_ICON        = JavaPluginImages
                                                                      .get(JavaPluginImages.IMG_FIELD_PUBLIC);
  private static final Image                PORTABLE_ICON         = JavaPluginImages
                                                                      .get(JavaPluginImages.IMG_FIELD_DEFAULT);
  private static final Image                PRE_INSTRUMENTED_ICON = JavaPluginImages
                                                                      .get(JavaPluginImages.IMG_FIELD_PROTECTED);
  private static final Image                OBJ_CYCLE_ICON        = TcPlugin
                                                                      .createImage("/images/eclipse/obj_cycle.gif");   //$NON-NLS-1$
  private static final Image                RESOLVED_ICON         = TcPlugin.createImage("/images/eclipse/nature.gif"); //$NON-NLS-1$
  private static final Image                BLANK_ICON            = TcPlugin.createImage("/images/eclipse/blank.gif"); //$NON-NLS-1$

  private static final String               EMPTY_STRING          = "";                                                //$NON-NLS-1$

  private static final Iterator             EMPTY_ITERATOR        = new EmptyIterator();

  private static final MultiChangeSignaller NULL_SIGNALLER        = new MultiChangeSignaller() {
                                                                    public void signal(IProject project) {/**/}
                                                                  };

  public NonPortableObjectDialog(Shell parentShell, NonPortableObjectEvent event) {
    super(
        parentShell,
        NonPortableMessages.getString("PROBLEM_SHARING_DATA"), null, event.getReason().getMessage(), MessageDialog.ERROR, new String[] { //$NON-NLS-1$
        NonPortableMessages.getString("PREVIOUS_ISSUE"), //$NON-NLS-1$
          NonPortableMessages.getString("NEXT_ISSUE"), //$NON-NLS-1$
          NonPortableMessages.getString("APPLY"), //$NON-NLS-1$
          IDialogConstants.CANCEL_LABEL }, 1);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    fEvent = event;
    fJavaProject = getJavaProject(fEvent);
    fNewConfig = (TcConfig) TcPlugin.getDefault().getConfiguration(fJavaProject.getProject()).copy();
    fConfigHelper = new ConfigHelper();
  }

  protected IDialogSettings getDialogBoundsSettings() {
    return getDialogSettings();
  }
  
  protected IDialogSettings getDialogSettings() {
    IDialogSettings settings = TcPlugin.getDefault().getDialogSettings();
    IDialogSettings section = settings.getSection(getDialogSettingsSectionName());
    if (section == null) {
      section = settings.addNewSection(getDialogSettingsSectionName());
    } 
    return section;
  }

  protected String getDialogSettingsSectionName() {
    return TcPlugin.PLUGIN_ID + ".NON_PORTABLE_DIALOG_SECTION"; //$NON-NLS-1$
  } 

  private IJavaProject getJavaProject(NonPortableObjectEvent event) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    String name = event.getContext().getProjectName();
    return JavaCore.create(workspaceRoot.getProject(name));
  }

  class ConfigHelper extends ConfigurationHelper {
    ConfigHelper() {
      super(fJavaProject.getProject());
    }

    public TcConfig getConfig() {
      return fNewConfig;
    }
  }

  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    fPreviousIssueButton = getButton(0);
    fNextIssueButton = getButton(1);
    fApplyButton = getButton(2);
    fPreviousIssueButton.setEnabled(getPreviousIssue() != null);
    fNextIssueButton.setEnabled(getNextIssue() != null);
    fApplyButton.setEnabled(false);
  }

  protected Control createCustomArea(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    composite.setLayout(new GridLayout());

    Composite topPanel = new Composite(composite, SWT.NONE);
    topPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridLayout gridLayout = new GridLayout(2, true);
    gridLayout.marginWidth = 0;
    topPanel.setLayout(gridLayout);

    Group listGroup = new Group(topPanel, SWT.SHADOW_NONE);
    listGroup.setText(NonPortableMessages.getString("ISSUES")); //$NON-NLS-1$
    listGroup.setLayout(new GridLayout());
    listGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    fIssueTable = new Table(listGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.SINGLE);
    fIssueTable.addSelectionListener(new IssueTableSelectionListener());
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.widthHint = SWTUtil.textColumnsToPixels(fIssueTable, 60);
    gridData.heightHint = SWTUtil.tableRowsToPixels(fIssueTable, 10);
    fIssueTable.setLayoutData(gridData);

    Group treePanel = new Group(topPanel, SWT.SHADOW_NONE);
    treePanel.setText(NonPortableMessages.getString("OBJECT_BROWSER")); //$NON-NLS-1$
    treePanel.setLayout(new GridLayout());
    treePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    fObjectTree = new Tree(treePanel, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    fObjectTree.setLayoutData(new GridData(GridData.FILL_BOTH));
    fObjectTree.addSelectionListener(new IssueTreeSelectionListener());

    fDetailsPanel = new Group(composite, SWT.SHADOW_NONE);
    fDetailsPanel.setText(NonPortableMessages.getString("ISSUE_DETAILS")); //$NON-NLS-1$
    fDetailsPanel.setLayout(new GridLayout());
    fDetailsPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    fSummaryLabel = new CLabel(fDetailsPanel, SWT.NONE);
    fSummaryLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fIssueDescription = new Text(fDetailsPanel, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER | SWT.READ_ONLY);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.widthHint = SWTUtil.textColumnsToPixels(fIssueDescription, 80);
    gridData.heightHint = SWTUtil.textRowsToPixels(fIssueDescription, 8);
    fIssueDescription.setLayoutData(gridData);

    fResolutionsPanel = new Group(composite, SWT.SHADOW_NONE);
    fResolutionsPanel.setText(NonPortableMessages.getString("RESOLUTIONS")); //$NON-NLS-1$
    fResolutionsPanel.setLayout(new GridLayout(2, false));
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    fResolutionsPanel.setLayoutData(gridData);

    Group actionGroup = new Group(fResolutionsPanel, SWT.NONE);
    actionGroup.setText(NonPortableMessages.getString("ACTIONS"));
    actionGroup.setLayout(new GridLayout());
    actionGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    fActionTreeViewer = new CheckboxTreeViewer(actionGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
    fActionTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
    fActionTreeViewer.addCheckStateListener(new ActionSelectionHandler());
    fActionTreeViewer.addSelectionChangedListener(new ActionSelectionChangedHandler());
    fActionTreeViewer.setContentProvider(new ActionTreeContentProvider());
    fActionTreeViewer.setLabelProvider(new ActionLabelProvider());

    Group actionPanelGroup = new Group(fResolutionsPanel, SWT.SHADOW_NONE);
    actionPanelGroup.setText(NonPortableMessages.getString("SELECTED_ACTION")); //$NON-NLS-1$
    actionPanelGroup.setLayout(new GridLayout());
    actionPanelGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    fActionPanel = new Composite(actionPanelGroup, SWT.NONE);
    fActionPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    fActionPanel.setLayout(fActionStackLayout = new StackLayout());

    fIncludeRuleView = new IncludeRulePanel(fActionPanel);
    fBootTypesView = new BootTypesPanel(fActionPanel);
    fNoActionView = new Label(fActionPanel, SWT.NONE);

    fActionStackLayout.topControl = fNoActionView;

    populateTree();

    initIssueList();
    if (fIssueTable.getItemCount() > 0) {
      fIssueTable.setSelection(0);
      handleTableSelectionChange();
    }

    return composite;
  }

  protected void buttonPressed(int buttonId) {
    switch (buttonId) {
      case 0:
        gotoPreviousIssue();
        break;
      case 1:
        gotoNextIssue();
        break;
      case 2:
        apply();
        okPressed();
        break;
      case 3:
        cancelPressed();
        break;
    }
  }

  NonPortableResolutionAction[] getResolutionActions(TreeItem item) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) item.getData();
    Object userObject = node.getUserObject();

    if (userObject instanceof NonPortableWorkState) {
      NonPortableWorkState workState = (NonPortableWorkState) userObject;
      return workState.getActions();
    }

    return null;
  }

  private boolean hasSelectedActions(TreeItem item) {
    NonPortableResolutionAction[] actions = getResolutionActions(item);

    if (actions != null && actions.length > 0) {
      for (int i = 0; i < actions.length; i++) {
        if (actions[i].isSelected()) { return true; }
      }
    }

    return false;
  }

  private Iterator selectedActions() {
    ArrayList list = new ArrayList();

    for (int i = 0; i < fIssueTable.getItemCount(); i++) {
      TableItem tableItem = fIssueTable.getItem(i);
      TreeItem treeItem = (TreeItem) tableItem.getData();
      NonPortableResolutionAction[] actions = getResolutionActions(treeItem);

      if (actions != null && actions.length > 0) {
        for (int j = 0; j < actions.length; j++) {
          if (actions[j].isSelected()) {
            list.add(actions[j]);
          }
        }
      }
    }

    return list.size() > 0 ? list.iterator() : EMPTY_ITERATOR;
  }

  private void apply() {
    TcPlugin plugin = TcPlugin.getDefault();
    XmlOptions xmlOpts = plugin.getXmlOptions();
    TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();

    configDoc.setTcConfig(fNewConfig);
    String xmlText = configDoc.xmlText(xmlOpts);

    try {
      IProject project = fJavaProject.getProject();
      plugin.setConfigurationFromString(project, xmlText);
      ConfigurationEditor configEditor = plugin.getConfigurationEditor(project);
      if (configEditor == null) {
        plugin.saveConfiguration(project);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private boolean anySelectedActions() {
    return selectedActions().hasNext();
  }

  private void gotoPreviousIssue() {
    TreeItem item = getPreviousIssue();
    if (item != null) {
      fObjectTree.setSelection(item);
      handleTreeSelectionChange();
    }
  }

  private TreeItem getPreviousIssue(TreeItem item) {
    while ((item = getPreviousItem(item)) != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) item.getData();
      Object userObject = node.getUserObject();

      if (userObject instanceof NonPortableWorkState) {
        NonPortableWorkState workState = (NonPortableWorkState) userObject;
        if (testIsIssue(workState)) { return item; }
      }
    }

    return null;
  }

  private TreeItem getPreviousIssue() {
    TreeItem[] selection = fObjectTree.getSelection();

    if (selection != null && selection.length > 0) { return getPreviousIssue(selection[0]); }

    return null;
  }

  private void gotoNextIssue() {
    TreeItem item = getNextIssue();
    if (item != null) {
      fObjectTree.setSelection(item);
      handleTreeSelectionChange();
    }
  }

  private TreeItem getNextItem(TreeItem item, boolean includeChildren) {
    if (item == null) { return null; }
    if (includeChildren) {
      TreeItem[] children = item.getItems();
      if (children != null && children.length > 0) { return children[0]; }
    }

    TreeItem parent = item.getParentItem();
    if (parent == null) { return null; }
    TreeItem[] siblings = parent.getItems();
    if (siblings != null) {
      if (siblings.length <= 1) { return getNextItem(parent, false); }

      for (int i = 0; i < siblings.length; i++) {
        if (siblings[i] == item && i < (siblings.length - 1)) { return siblings[i + 1]; }
      }
    }
    return getNextItem(parent, false);
  }

  private TreeItem getPreviousItem(TreeItem item) {
    TreeItem parent = item.getParentItem();
    if (parent == null) { return null; }
    TreeItem[] siblings = parent.getItems();
    if (siblings.length == 0 || siblings[0] == item) { return parent; }
    TreeItem previous = siblings[0];
    for (int i = 1; i < siblings.length; i++) {
      if (siblings[i] == item) { return rightMostDescendent(previous); }
      previous = siblings[i];
    }
    return null;
  }

  private TreeItem rightMostDescendent(TreeItem item) {
    TreeItem[] children = item.getItems();
    if (children != null && children.length > 0) { return rightMostDescendent(children[children.length - 1]); }
    return item;
  }

  private TreeItem getNextIssue(TreeItem item) {
    while ((item = getNextItem(item, true)) != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) item.getData();
      Object userObject = node.getUserObject();

      if (userObject instanceof NonPortableWorkState) {
        NonPortableWorkState workState = (NonPortableWorkState) userObject;
        if (testIsIssue(workState)) { return item; }
      }
    }

    return null;
  }

  private TreeItem getNextIssue() {
    TreeItem[] selection = fObjectTree.getSelection();

    if (selection != null && selection.length > 0) { return getNextIssue(selection[0]); }

    return null;
  }

  static Image imageFor(NonPortableWorkState workState) {
    if (workState.isRepeated()) {
      return OBJ_CYCLE_ICON;
    } else if (workState.isPreInstrumented()) {
      return PRE_INSTRUMENTED_ICON;
    } else if (workState.isNeverPortable()) {
      return NEVER_PORTABLE_ICON;
    } else if (!workState.isPortable()) {
      return NOT_PORTABLE_ICON;
    } else if (workState.isTransient()) {
      return TRANSIENT_ICON;
    } else {
      return PORTABLE_ICON;
    }
  }

  void initTreeItem(TreeItem item, DefaultMutableTreeNode node) {
    item.setData(node);

    String text = null;
    Image image = PORTABLE_ICON;

    Object userObject = node.getUserObject();
    if (userObject instanceof NonPortableObjectState) {
      userObject = new NonPortableWorkState((NonPortableObjectState) userObject);
      node.setUserObject(userObject);
    }
    if (userObject instanceof NonPortableWorkState) {
      NonPortableWorkState workState = (NonPortableWorkState) userObject;
      text = workState.getLabel();
      image = imageFor(workState);
    }

    item.setText(text != null ? text : userObject.toString());
    item.setImage(image);

    for (int i = 0; i < node.getChildCount(); i++) {
      TreeItem childItem = new TreeItem(item, SWT.NONE);
      DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
      initTreeItem(childItem, childNode);
    }
  }

  void populateTree() {
    DefaultTreeModel treeModel = fEvent.getContext().getTreeModel();

    if (treeModel != null) {
      DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
      DefaultMutableTreeNode node;
      TreeItem item;

      for (int i = 0; i < root.getChildCount(); i++) {
        item = new TreeItem(fObjectTree, SWT.NONE);
        node = (DefaultMutableTreeNode) root.getChildAt(i);
        initTreeItem(item, node);
      }
    }
  }

  boolean testIsIssue(NonPortableWorkState workState) {
    String fieldName = workState.getFieldName();
    boolean isNull = workState.isNull();
    boolean isTransientField = fieldName != null && (fConfigHelper.isTransient(fieldName) || workState.isTransient());

    if (workState.isNeverPortable() && !isTransientField && !isNull) { return true; }

    return !workState.isPortable()
        && !(fConfigHelper.isAdaptable(workState.getTypeName()) || isTransientField || isNull);
  }

  boolean checkAddToIssueList(NonPortableWorkState workState) {
    if (workState.hasSelectedActions()) { return true; }
    return testIsIssue(workState);
  }

  void initIssueList() {
    TreeItem treeItem = fObjectTree.getItem(0);

    fIssueTable.setRedraw(false);
    fIssueTable.setItemCount(0);
    while (treeItem != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeItem.getData();
      Object userObject = node.getUserObject();

      if (userObject instanceof NonPortableWorkState) {
        NonPortableWorkState workState = (NonPortableWorkState) userObject;

        if (checkAddToIssueList(workState)) {
          TableItem tableItem = new TableItem(fIssueTable, SWT.NONE);

          tableItem.setData(treeItem);
          tableItem.setImage(hasSelectedActions(treeItem) ? RESOLVED_ICON : BLANK_ICON);
          tableItem.setText(workState.shortSummary());
        }
      }

      treeItem = getNextItem(treeItem, true);
    }
    fIssueTable.setRedraw(true);
  }

  void handleTableSelectionChange() {
    TableItem[] selection = fIssueTable.getSelection();

    if (selection.length > 0) {
      TreeItem treeItem = (TreeItem) selection[0].getData();
      fObjectTree.setSelection(treeItem);
      handleTreeSelectionChange();
    }
  }

  void syncTableAndTree() {
    TreeItem[] selection = fObjectTree.getSelection();
    TreeItem treeItem = selection[0];
    int tableCount = fIssueTable.getItemCount();
    TableItem tableItem;

    for (int i = 0; i < tableCount; i++) {
      tableItem = fIssueTable.getItem(i);
      if (tableItem.getData() == treeItem) {
        fIssueTable.setSelection(i);
        return;
      }
    }

    fIssueTable.deselectAll();
  }

  void handleTreeSelectionChange() {
    TreeItem[] selection = fObjectTree.getSelection();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selection[0].getData();
    Object userObject = node.getUserObject();

    syncTableAndTree();
    if (userObject instanceof NonPortableWorkState) {
      setWorkState((NonPortableWorkState) userObject);
    } else {
      hideResolutionsPanel();
    }
    resetSearchButtons();
  }

  private void resetSearchButtons() {
    if (fPreviousIssueButton != null) {
      fPreviousIssueButton.setEnabled(getPreviousIssue() != null);
      fNextIssueButton.setEnabled(getNextIssue() != null);
    }
  }
  
  class IssueTableSelectionListener extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      handleTableSelectionChange();
    }
  }

  class IssueTreeSelectionListener extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      handleTreeSelectionChange();
    }
  }

  void hideResolutionsPanel() {
  fResolutionsPanel.setVisible(false);
  }

  void showResolutionsPanel() {
    fResolutionsPanel.setVisible(true);
  }

  void setWorkState(NonPortableWorkState workState) {
    fActionTreeViewer.getTree().setRedraw(false);
    showResolutionsPanel();
    hideActionPanel();

    fSummaryLabel.setText(workState.summary());
    fSummaryLabel.setImage(imageFor(workState));
    fIssueDescription.setText(workState.descriptionFor());
    fActionTreeViewer.setInput(workState);

    NonPortableResolutionAction[] actions = getActions(workState);
    if (actions != null && actions.length > 0) {
      boolean showComponent = true;

      for (int i = 0; i < actions.length; i++) {
        NonPortableResolutionAction action = actions[i];

        if (action.isEnabled()) {
          boolean isSelected = action.isSelected();

          fActionTreeViewer.setChecked(action, isSelected);
          if(isSelected) {
            if(showComponent) {
              fActionTreeViewer.setSelection(new StructuredSelection(action));
              action.showControl(this);
              showComponent = false;
            }
          }
        }
      }
      fActionTreeViewer.expandAll();
    }

    setNoAction(!workState.hasSelectedActions());

    if (!haveAnyActions()) {
      hideResolutionsPanel();
    }

    fActionTreeViewer.getTree().setRedraw(true);
  }

  NonPortableResolutionAction[] createActions(NonPortableWorkState workState) {
    ArrayList list = new ArrayList();

    if (workState.isNeverPortable() || workState.isPortable()) {
      if (workState.getFieldName() != null && !workState.isTransient()) {
        list.add(new MakeTransientAction(workState));
      }
    } else if (!workState.isPortable()) {
      list.add(new IncludeTypeAction(workState));
      list.add(new IncludePackageAction(workState));

      if (workState.isSystemType()) {
        list.add(new AddToBootJarAction(workState));
      }

      if (workState.getFieldName() != null && !workState.isTransient()) {
        list.add(new MakeTransientAction(workState));
      }
    }

    return (NonPortableResolutionAction[]) list.toArray(new NonPortableResolutionAction[0]);
  }

  NonPortableResolutionAction[] getActions(NonPortableWorkState state) {
    NonPortableResolutionAction[] actions = state.getActions();

    if (actions == null) {
      state.setActions(actions = createActions(state));
    }

    return actions;
  }

  class ActionSelectionChangedHandler implements ISelectionChangedListener {
    public void selectionChanged(SelectionChangedEvent event) {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      Object obj = selection.getFirstElement();

      if (obj == NO_ACTION_ITEM) {
        hideActionPanel();
      } else {
        NonPortableResolutionAction action = (NonPortableResolutionAction) obj;
        if (action != null) {
          if (action.isSelected()) {
            action.showControl(this);
          } else {
            hideActionPanel();
          }
        }
      }
    }
  }

  private boolean isSelected(Object element) {
    IStructuredSelection selection = (IStructuredSelection) fActionTreeViewer.getSelection();
    return !selection.isEmpty() && (selection.getFirstElement() == element);
  }

  private boolean haveAnyActions() {
    ActionTreeContentProvider contentProvider = (ActionTreeContentProvider) fActionTreeViewer.getContentProvider();
    NonPortableResolutionAction[] actions = contentProvider.getEnabledActions();
    return actions != null && actions.length > 0;
  }
  
  private boolean anyActionSelected() {
    ActionTreeContentProvider contentProvider = (ActionTreeContentProvider) fActionTreeViewer.getContentProvider();
    NonPortableResolutionAction[] actions = contentProvider.getEnabledActions();

    for (int i = 0; i < actions.length; i++) {
      if (actions[i].isSelected()) { return true; }
    }

    return false;
  }

  private void setNoAction(boolean checked) {
    fActionTreeViewer.setChecked(NO_ACTION_ITEM, checked);
  }
  
  private boolean isSetNoAction() {
    return fActionTreeViewer.getChecked(NO_ACTION_ITEM);
  }
  
  class ActionSelectionHandler implements ICheckStateListener {
    public void checkStateChanged(CheckStateChangedEvent event) {
      Object obj = event.getElement();

      if (obj == NO_ACTION_ITEM) {
        handleNoActionItemSelected();
      } else {
        NonPortableResolutionAction action = (NonPortableResolutionAction) obj;
        if (action != null) {
          boolean isChecked = fActionTreeViewer.getChecked(action);
          boolean isSelected = isSelected(action);

          action.setSelected(isChecked);
          if (isChecked) {
            setNoAction(false);
          } else if (!anyActionSelected()) {
            setNoAction(true);
            hideActionPanel();
          }

          if (isSelected) {
            if (isChecked) {
              action.showControl(this);
            } else {
              hideActionPanel();
            }
          }
        }
      }

      fApplyButton.setEnabled(anySelectedActions());
    }
  }

  void hideActionPanel() {
    fActionStackLayout.topControl = fNoActionView;
    fActionPanel.layout();
    fActionPanel.redraw();
  }

  void handleNoActionItemSelected() {
    if (isSetNoAction()) {
      hideActionPanel();

      ActionTreeContentProvider contentProvider = (ActionTreeContentProvider) fActionTreeViewer.getContentProvider();
      NonPortableResolutionAction[] actions = contentProvider.getEnabledActions();

      for (int i = 0; i < actions.length; i++) {
        actions[i].setSelected(false);
        fActionTreeViewer.setChecked(actions[i], false);
      }

      TableItem[] selection = fIssueTable.getSelection();
      if (selection.length > 0) {
        selection[0].setImage(BLANK_ICON);
      }
    }
  }

  public void widgetDefaultSelected(SelectionEvent e) {/**/}

  private static final Object NO_ACTION_ITEM = new Object();

  class ActionTreeContentProvider implements ITreeContentProvider {
    NonPortableWorkState          fWorkState;
    NonPortableResolutionAction[] fEnabledActions;

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      fWorkState = (NonPortableWorkState) newInput;
      fEnabledActions = null;
      
      if(fWorkState != null) {
        ArrayList list = new ArrayList();
        NonPortableResolutionAction[] actions = getActions(fWorkState);
  
        if (actions != null && actions.length > 0) {
          for (int i = 0; i < actions.length; i++) {
            NonPortableResolutionAction action = actions[i];
            if (action.isEnabled()) {
              list.add(action);
            }
          }
        }
        fEnabledActions = (NonPortableResolutionAction[]) list.toArray(new NonPortableResolutionAction[0]);
      }
    }

    public NonPortableResolutionAction[] getEnabledActions() {
      return fEnabledActions;
    }

    public Object[] getChildren(Object parentElement) {
      return (parentElement == NO_ACTION_ITEM) ? fEnabledActions : null;
    }

    public Object getParent(Object element) {
      return (element != NO_ACTION_ITEM) ? NO_ACTION_ITEM : null;
    }

    public boolean hasChildren(Object element) {
      return element == NO_ACTION_ITEM;
    }

    public Object[] getElements(Object inputElement) {
      return new Object[] { NO_ACTION_ITEM };
    }

    public void dispose() {/**/}
  }

  class ActionLabelProvider implements ILabelProvider {
    public String getText(Object element) {
      if (element == NO_ACTION_ITEM) return NonPortableMessages.getString("TAKE_NO_ACTION");
      return element.toString();
    }

    public Image getImage(Object element) {
      return null;
    }

    public void addListener(ILabelProviderListener listener) {/**/}

    public void dispose() {/**/}

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {/**/}
  }

  Include ensureIncludeRuleFor(String classExpr) {
    Include include = fConfigHelper.includeRuleFor(classExpr);

    if (include == null) {
      include = fConfigHelper.addIncludeRule(classExpr);
    }

    return include;
  }

  class MakeTransientAction extends NonPortableResolutionAction {
    MakeTransientAction(NonPortableWorkState workState) {
      super(workState);
    }

    public void showControl(Object parentControl) {
      String fieldName = fWorkState.getFieldName();
      String declaringType = fieldName.substring(0, fieldName.lastIndexOf('.'));
      Include include = fConfigHelper.includeRuleFor(declaringType);

      if (include != null) {
        fActionStackLayout.topControl = fIncludeRuleView;
        fIncludeRuleView.setInclude(include);
        fActionPanel.layout();
        fActionPanel.redraw();
      }
    }

    public String getText() {
      return NonPortableMessages.getString("DO_NOT_SHARE"); //$NON-NLS-1$
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);

      int index = fIssueTable.getSelectionIndex();
      TreeItem treeItem = fObjectTree.getSelection()[0];

      fIssueTable.setRedraw(false);
      fObjectTree.setRedraw(false);

      if (selected) {
        fConfigHelper.internalEnsureTransient(fWorkState.getFieldName(), NULL_SIGNALLER);
        treeItem.removeAll();
      } else {
        fConfigHelper.internalEnsureNotTransient(fWorkState.getFieldName(), NULL_SIGNALLER);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeItem.getData();
        for (int i = 0; i < node.getChildCount(); i++) {
          TreeItem childItem = new TreeItem(treeItem, SWT.NONE);
          DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
          initTreeItem(childItem, childNode);
        }
      }

      initIssueList();
      fObjectTree.setSelection(treeItem);
      fIssueTable.setSelection(index);
      fIssueTable.setRedraw(true);
      fObjectTree.setRedraw(true);
      
      resetSearchButtons();
    }

    public boolean isEnabled() {
      String fieldName = fWorkState.getFieldName();
      String declaringType = fieldName.substring(0, fieldName.lastIndexOf('.'));
      return fConfigHelper.isAdaptable(declaringType);
    }
  }

  class IncludeTypeAction extends NonPortableResolutionAction {
    IncludeTypeAction(NonPortableWorkState workState) {
      super(workState);
    }

    String getClassExpression() {
      return fWorkState.getTypeName();
    }

    public void showControl(Object parentControl) {
      fActionStackLayout.topControl = fIncludeRuleView;
      fIncludeRuleView.setInclude(fConfigHelper.includeRuleFor(getClassExpression()));
      fActionPanel.layout();
      fActionPanel.redraw();
    }

    public String getText() {
      return NonPortableMessages.getString("INCLUDE_TYPE_FOR_SHARING"); //$NON-NLS-1$
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);

      String classExpr = getClassExpression();
      if (selected) {
        ensureIncludeRuleFor(classExpr);
      } else {
        fConfigHelper.ensureNotAdaptable(classExpr, NULL_SIGNALLER);
      }

      int index = fIssueTable.getSelectionIndex();

      fIssueTable.setRedraw(false);
      initIssueList();
      fIssueTable.setSelection(index);
      fIssueTable.setRedraw(true);
      
      resetSearchButtons();
    }
  }

  class IncludePackageAction extends IncludeTypeAction {
    IncludePackageAction(NonPortableWorkState workState) {
      super(workState);
    }

    String getClassExpression() {
      String typeName = fWorkState.getTypeName();
      String packageName = typeName.substring(0, typeName.lastIndexOf('.'));
      return packageName + ".*"; //$NON-NLS-1$
    }

    public String getText() {
      return NonPortableMessages.getString("INCLUDE_PACKAGE_FOR_SHARING"); //$NON-NLS-1$
    }
  }

  class AddToBootJarAction extends NonPortableResolutionAction {
    AddToBootJarAction(NonPortableWorkState workState) {
      super(workState);
    }

    public void showControl(Object parentControl) {
      fActionStackLayout.topControl = fBootTypesView;
      fBootTypesView.setRequiredBootTypes(fWorkState.getRequiredBootTypes());
      fActionPanel.layout();
      fActionPanel.redraw();
    }

    public String getText() {
      return NonPortableMessages.getString("ADD_TO_BOOTJAR"); //$NON-NLS-1$
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);

      String[] types = fWorkState.getRequiredBootTypes();

      if (selected) {
        for (int i = 0; i < types.length; i++) {
          fConfigHelper.internalEnsureBootJarClass(types[i], NULL_SIGNALLER);
        }
      } else {
        for (int i = 0; i < types.length; i++) {
          fConfigHelper.internalEnsureNotBootJarClass(types[i], NULL_SIGNALLER);
        }
      }

      int index = fIssueTable.getSelectionIndex();

      fIssueTable.setRedraw(false);
      initIssueList();
      fIssueTable.setSelection(index);
      fIssueTable.setRedraw(true);
      
      resetSearchButtons();
    }
  }

  class IncludeRulePanel extends Composite implements SelectionListener {
    Button  honorTransientButton, doNothingButton, callMethodButton, executeCodeButton;
    Text    includePatternText, executeCodeText;
    Combo   callMethodCombo;
    Include include;

    IncludeRulePanel(Composite parent) {
      super(parent, SWT.NONE);

      setLayout(new GridLayout(2, false));
      CLabel label = new CLabel(this, SWT.NONE);
      label.setText(NonPortableMessages.getString("INCLUDE_PATTERN")); //$NON-NLS-1$
      includePatternText = new Text(this, SWT.BORDER | SWT.READ_ONLY);
      includePatternText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      honorTransientButton = new Button(this, SWT.CHECK);
      honorTransientButton.setText(NonPortableMessages.getString("HONOR_TRANSIENT")); //$NON-NLS-1$
      GridData gridData = new GridData();
      gridData.horizontalSpan = 2;
      honorTransientButton.setLayoutData(gridData);
      Group onLoadGroup = new Group(this, SWT.NO_RADIO_GROUP | SWT.SHADOW_NONE);
      onLoadGroup.setText(NonPortableMessages.getString("ON_LOAD")); //$NON-NLS-1$
      onLoadGroup.setLayout(new GridLayout(2, false));
      doNothingButton = new Button(onLoadGroup, SWT.RADIO);
      doNothingButton.setText(NonPortableMessages.getString("DO_NOTHING")); //$NON-NLS-1$
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      doNothingButton.setLayoutData(gridData);
      doNothingButton.addSelectionListener(this);
      callMethodButton = new Button(onLoadGroup, SWT.RADIO);
      callMethodButton.setText(NonPortableMessages.getString("CALL_METHOD")); //$NON-NLS-1$
      callMethodButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
      callMethodButton.addSelectionListener(this);
      callMethodCombo = new Combo(onLoadGroup, SWT.BORDER);
      callMethodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      callMethodCombo.addFocusListener(new FocusAdapter() {
        public void focusLost(FocusEvent e) {
          OnLoad onLoad = include.getOnLoad();
          onLoad.setMethod(callMethodCombo.getText());
        }
      });
      executeCodeButton = new Button(onLoadGroup, SWT.RADIO);
      executeCodeButton.setText(NonPortableMessages.getString("EXECUTE_CODE")); //$NON-NLS-1$
      executeCodeButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
      executeCodeButton.addSelectionListener(this);
      executeCodeText = new Text(onLoadGroup, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.widthHint = SWTUtil.textColumnsToPixels(executeCodeText, 60);
      gridData.heightHint = SWTUtil.textRowsToPixels(executeCodeText, 3);
      executeCodeText.setLayoutData(gridData);
      executeCodeText.addFocusListener(new FocusAdapter() {
        public void focusLost(FocusEvent e) {
          OnLoad onLoad = include.getOnLoad();
          onLoad.setExecute(executeCodeText.getText());
        }
      });
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 2;
      onLoadGroup.setLayoutData(gridData);
    }

    void setInclude(Include include) {
      this.include = include;
      includePatternText.setText(include.getClassExpression());
      honorTransientButton.setSelection(include.getHonorTransient());
      OnLoad onLoad = include.getOnLoad();
      if (onLoad != null) {
        if (onLoad.isSetExecute()) {
          setExecuteCode();
        } else if (onLoad.isSetMethod()) {
          setCallMethod();
        }
      } else {
        setDoNothing();
      }
    }

    private void setExecuteCode() {
      OnLoad onLoad = include.getOnLoad();

      executeCodeButton.setSelection(true);
      executeCodeText.setEnabled(true);
      executeCodeText.setText(onLoad != null && onLoad.isSetExecute() ? onLoad.getExecute() : EMPTY_STRING);
      doNothingButton.setSelection(false);
      callMethodButton.setSelection(false);
      callMethodCombo.setEnabled(false);
      callMethodCombo.setText(EMPTY_STRING);
    }

    private void setCallMethod() {
      OnLoad onLoad = include.getOnLoad();

      executeCodeButton.setSelection(false);
      executeCodeText.setEnabled(false);
      executeCodeText.setText(EMPTY_STRING);
      doNothingButton.setSelection(false);
      callMethodButton.setSelection(true);
      callMethodCombo.setEnabled(true);
      initMethodCombo();
      callMethodCombo.setText(onLoad != null && onLoad.isSetMethod() ? onLoad.getMethod() : EMPTY_STRING);
    }

    private void setDoNothing() {
      executeCodeButton.setSelection(false);
      executeCodeText.setEnabled(false);
      executeCodeText.setText(EMPTY_STRING);
      doNothingButton.setSelection(true);
      callMethodButton.setSelection(false);
      callMethodCombo.setEnabled(false);
      callMethodCombo.setText(EMPTY_STRING);
    }

    private void initMethodCombo() {
      callMethodCombo.removeAll();

      try {
        IType type = fJavaProject.findType(include.getClassExpression());
        if (type != null) {
          IMethod[] methods = type.getMethods();
          for (int i = 0; i < methods.length; i++) {
            if (methods[i].getParameterNames().length == 0) {
              callMethodCombo.add(methods[i].getElementName());
            }
          }
        }
      } catch (JavaModelException jme) {/**/
      }
    }

    public void widgetDefaultSelected(SelectionEvent e) {/**/}

    public void widgetSelected(SelectionEvent e) {
      OnLoad onLoad = include.getOnLoad();

      if (e.widget == doNothingButton && doNothingButton.getSelection()) {
        setDoNothing();
        include.unsetOnLoad();
      } else if (e.widget == executeCodeButton && executeCodeButton.getSelection()) {
        setExecuteCode();
        if (onLoad != null) {
          onLoad.unsetMethod();
        } else {
          include.addNewOnLoad();
        }
      } else if (e.widget == callMethodButton && callMethodButton.getSelection()) {
        setCallMethod();
        if (onLoad != null) {
          onLoad.unsetExecute();
        } else {
          include.addNewOnLoad();
        }
      }
    }
  }

  class BootTypesPanel extends Composite {
    List fTypeList;

    BootTypesPanel(Composite parent) {
      super(parent, SWT.NONE);
      setLayout(new GridLayout());
      Label label = new Label(this, SWT.NONE);
      label.setText(NonPortableMessages.getString("TYPES_TO_ADD_TO_BOOTJAR")); //$NON-NLS-1$
      label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      fTypeList = new List(this, SWT.BORDER | SWT.V_SCROLL);
      fTypeList.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    void setRequiredBootTypes(String[] types) {
      if (types != null) {
        for (int i = 0; i < types.length; i++) {
          fTypeList.add(types[i]);
        }
      }
    }
  }
}
