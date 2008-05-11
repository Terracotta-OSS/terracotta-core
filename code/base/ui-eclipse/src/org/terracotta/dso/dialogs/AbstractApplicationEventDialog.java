/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.MultiChangeSignaller;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.ConfigurationEditor;
import org.terracotta.ui.util.EmptyIterator;
import org.terracotta.ui.util.SWTUtil;

import com.tc.admin.common.AbstractResolutionAction;
import com.tc.admin.common.AbstractWorkState;
import com.tc.admin.common.NonPortableMessages;
import com.tc.admin.common.NonPortableWorkState;
import com.tc.object.appevent.AbstractApplicationEvent;
import com.tc.object.appevent.NonPortableObjectState;
import com.terracottatech.config.Include;
import com.terracottatech.config.OnLoad;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public abstract class AbstractApplicationEventDialog extends MessageDialog {
  protected IJavaProject                      fJavaProject;
  private AbstractApplicationEvent            fEvent;
  private FormToolkit                         fFormToolkit;
  private SashForm                            fTopSashForm;
  private SashForm                            fBottomSashForm;
  protected Table                             fIssueTable;
  protected Tree                              fObjectTree;
  protected CLabel                            fSummaryLabel;
  private Group                               fDetailsPanel;
  private Group                               fResolutionsPanel;
  protected CheckboxTreeViewer                fActionTreeViewer;
  protected Composite                         fActionPanel;
  protected StackLayout                       fActionStackLayout;
  protected IncludeRulePanel                  fIncludeRuleView;
  private Label                               fNoActionView;
  protected TcConfig                          fNewConfig;
  protected ConfigurationHelper               fConfigHelper;

  private static final int[]                  DEFAULT_TOP_SASH_WEIGHTS     = new int[] { 100, 100 };
  private static final int[]                  DEFAULT_BOTTOM_SASH_WEIGHTS  = new int[] { 100, 100 };

  private static final String                 DIALOG_TOP_SASH_WEIGHTS_1    = TcPlugin.PLUGIN_ID
                                                                             + ".DIALOG_TOP_SASH_WEIGHTS_1";
  private static final String                 DIALOG_TOP_SASH_WEIGHTS_2    = TcPlugin.PLUGIN_ID
                                                                             + ".DIALOG_TOP_SASH_WEIGHTS_2";

  private static final String                 DIALOG_BOTTOM_SASH_WEIGHTS_1 = TcPlugin.PLUGIN_ID
                                                                             + ".DIALOG_BOTTOM_SASH_WEIGHTS_1";
  private static final String                 DIALOG_BOTTOM_SASH_WEIGHTS_2 = TcPlugin.PLUGIN_ID
                                                                             + ".DIALOG_BOTTOM_SASH_WEIGHTS_2";

  protected static final Image                NOT_PORTABLE_ICON            = TcPlugin
                                                                               .createImage(AbstractApplicationEventDialog.class.getResource("/com/tc/admin/icons/field_private_obj.gif"));
  protected static final Image                NEVER_PORTABLE_ICON          = TcPlugin
                                                                               .createImage(AbstractApplicationEventDialog.class.getResource("/com/tc/admin/icons/field_private_obj.gif"));
  protected static final Image                TRANSIENT_ICON               = TcPlugin
                                                                               .createImage(AbstractApplicationEventDialog.class.getResource("/com/tc/admin/icons/field_public_obj.gif"));
  protected static final Image                PORTABLE_ICON                = TcPlugin
                                                                               .createImage(AbstractApplicationEventDialog.class.getResource("/com/tc/admin/icons/field_default_obj.gif"));
  protected static final Image                PRE_INSTRUMENTED_ICON        = TcPlugin
                                                                               .createImage(AbstractApplicationEventDialog.class.getResource("/com/tc/admin/icons/field_protected_obj.gif"));
  protected static final Image                OBJ_CYCLE_ICON               = TcPlugin
                                                                               .createImage("/images/eclipse/obj_cycle.gif");
  protected static final Image                RESOLVED_ICON                = TcPlugin
                                                                               .createImage("/images/eclipse/nature.gif");
  protected static final Image                HELP_ICON                    = TcPlugin
                                                                               .createImage("/images/eclipse/help.gif");

  protected static final String               EMPTY_STRING                 = "";

  protected static final Iterator             EMPTY_ITERATOR               = new EmptyIterator();

  protected static final MultiChangeSignaller NULL_SIGNALLER               = new MultiChangeSignaller() {
                                                                             public void signal(IProject project) {/**/
                                                                             }
                                                                           };

  /*
   * In Eclipse 3.3 JavaElementImageDescriptor, used by TcPlugin.createImage, started having problems with transparent
   * images. The code below creates a transparent image manually.
   */
  protected static Image                      BLANK_ICON;

  static {
    final ImageData blankImageData = new ImageData(16, 16, 1, new PaletteData(new RGB[] { new RGB(255, 0, 0) }));
    blankImageData.transparentPixel = 0;
    BLANK_ICON = ImageDescriptor.createFromImageData(blankImageData).createImage();
  }

  public AbstractApplicationEventDialog(Shell parentShell, String dialogTitle, AbstractApplicationEvent event,
                                        String[] buttonLabels, int defaultButtonIndex) {
    super(parentShell, dialogTitle, null, event.getMessage(), MessageDialog.ERROR, buttonLabels, defaultButtonIndex);
    setShellStyle(SWT.DIALOG_TRIM | getDefaultOrientation() | SWT.RESIZE);
    fEvent = event;
    fJavaProject = getJavaProject(fEvent);
    fNewConfig = (TcConfig) TcPlugin.getDefault().getConfiguration(fJavaProject.getProject()).copy();
    fConfigHelper = new ConfigHelper();
    fFormToolkit = new FormToolkit(parentShell.getDisplay());
    fFormToolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
  }

  public AbstractApplicationEvent getApplicationEvent() {
    return fEvent;
  }

  protected IDialogSettings getDialogBoundsSettings() {
    return getDialogSettings();
  }

  protected FormToolkit getFormToolkit() {
    return fFormToolkit;
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jface.window.Window#initializeBounds()
   */
  protected void initializeBounds() {
    IDialogSettings settings = getDialogSettings();
    if (fTopSashForm != null) {
      int w1, w2;
      try {
        w1 = settings.getInt(DIALOG_TOP_SASH_WEIGHTS_1);
        w2 = settings.getInt(DIALOG_TOP_SASH_WEIGHTS_2);
      } catch (NumberFormatException nfe) {
        w1 = DEFAULT_TOP_SASH_WEIGHTS[0];
        w2 = DEFAULT_TOP_SASH_WEIGHTS[1];
      }
      fTopSashForm.setWeights(new int[] { w1, w2 });
    }
    if (fBottomSashForm != null) {
      int w1, w2;
      try {
        w1 = settings.getInt(DIALOG_BOTTOM_SASH_WEIGHTS_1);
        w2 = settings.getInt(DIALOG_BOTTOM_SASH_WEIGHTS_2);
      } catch (NumberFormatException nfe) {
        w1 = DEFAULT_BOTTOM_SASH_WEIGHTS[0];
        w2 = DEFAULT_BOTTOM_SASH_WEIGHTS[1];
      }
      fBottomSashForm.setWeights(new int[] { w1, w2 });
    }
    super.initializeBounds();
  }

  protected void persistSashWeights() {
    IDialogSettings settings = getDialogSettings();
    if (fTopSashForm != null) {
      int[] sashWeights = fTopSashForm.getWeights();
      settings.put(DIALOG_TOP_SASH_WEIGHTS_1, sashWeights[0]);
      settings.put(DIALOG_TOP_SASH_WEIGHTS_2, sashWeights[1]);
    }
    if (fBottomSashForm != null) {
      int[] sashWeights = fTopSashForm.getWeights();
      settings.put(DIALOG_BOTTOM_SASH_WEIGHTS_1, sashWeights[0]);
      settings.put(DIALOG_BOTTOM_SASH_WEIGHTS_2, sashWeights[1]);
    }
  }

  protected IDialogSettings getDialogSettings() {
    IDialogSettings settings = TcPlugin.getDefault().getDialogSettings();
    IDialogSettings section = settings.getSection(getDialogSettingsSectionName());
    if (section == null) {
      section = settings.addNewSection(getDialogSettingsSectionName());
    }
    return section;
  }

  public boolean close() {
    persistSashWeights();
    return super.close();
  }

  private IJavaProject getJavaProject(AbstractApplicationEvent event) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    String name = event.getApplicationEventContext().getProjectName();
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

  protected Control createCustomArea(Composite parent) {
    fFormToolkit.setBackground(parent.getBackground());

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    composite.setLayout(new GridLayout());

    SashForm sash = new SashForm(composite, SWT.SMOOTH);
    sash.setOrientation(SWT.HORIZONTAL);
    sash.setLayoutData(new GridData(GridData.FILL_BOTH));
    sash.setFont(parent.getFont());
    sash.setVisible(true);
    fTopSashForm = sash;

    Group listGroup = new Group(sash, SWT.SHADOW_NONE);
    listGroup.setText(NonPortableMessages.getString("ISSUES")); //$NON-NLS-1$
    listGroup.setLayout(new GridLayout());
    listGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    fIssueTable = new Table(listGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.SINGLE);
    fIssueTable.addSelectionListener(new IssueTableSelectionListener());
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.widthHint = SWTUtil.textColumnsToPixels(fIssueTable, 60);
    gridData.heightHint = SWTUtil.tableRowsToPixels(fIssueTable, 10);
    fIssueTable.setLayoutData(gridData);

    Group treePanel = new Group(sash, SWT.SHADOW_NONE);
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

    createIssueDescriptionArea(fDetailsPanel);

    fResolutionsPanel = new Group(composite, SWT.SHADOW_NONE);
    fResolutionsPanel.setText(NonPortableMessages.getString("RESOLUTIONS")); //$NON-NLS-1$
    fResolutionsPanel.setLayout(new GridLayout(2, false));
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    fResolutionsPanel.setLayoutData(gridData);

    sash = new SashForm(fResolutionsPanel, SWT.SMOOTH);
    sash.setOrientation(SWT.HORIZONTAL);
    sash.setLayoutData(new GridData(GridData.FILL_BOTH));
    sash.setFont(parent.getFont());
    sash.setVisible(true);
    fBottomSashForm = sash;

    Group actionGroup = new Group(sash, SWT.NONE);
    actionGroup.setText(NonPortableMessages.getString("ACTIONS"));
    actionGroup.setLayout(new GridLayout());
    actionGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    fActionTreeViewer = new CheckboxTreeViewer(actionGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
    fActionTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
    fActionTreeViewer.addCheckStateListener(new ActionCheckStateHandler());
    fActionTreeViewer.addSelectionChangedListener(new ActionSelectionChangedHandler());
    fActionTreeViewer.setContentProvider(new ActionTreeContentProvider());
    fActionTreeViewer.setLabelProvider(new ActionLabelProvider());

    Group actionPanelGroup = new Group(sash, SWT.SHADOW_NONE);
    actionPanelGroup.setText(NonPortableMessages.getString("SELECTED_ACTION")); //$NON-NLS-1$
    actionPanelGroup.setLayout(new GridLayout());
    actionPanelGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    fActionPanel = new Composite(actionPanelGroup, SWT.NONE);
    fActionPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    fActionPanel.setLayout(fActionStackLayout = new StackLayout());

    fIncludeRuleView = new IncludeRulePanel(fActionPanel);
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
        cancelPressed();
        break;
      case 1:
        apply();
        okPressed();
        break;
    }
  }

  protected void apply() {
    TcPlugin plugin = TcPlugin.getDefault();
    TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();

    configDoc.setTcConfig(fNewConfig);
    try {
      IProject project = fJavaProject.getProject();
      plugin.setConfigurationFromString(project, plugin.configDocumentAsString(configDoc));
      ConfigurationEditor configEditor = plugin.getConfigurationEditor(project);
      if (configEditor == null) {
        plugin.saveConfiguration(project);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected static Image imageFor(AbstractWorkState workState) {
    if (workState instanceof NonPortableWorkState) {
      NonPortableWorkState nonPortableWorkState = (NonPortableWorkState) workState;

      if (nonPortableWorkState.isRepeated()) {
        return OBJ_CYCLE_ICON;
      } else if (nonPortableWorkState.isPreInstrumented()) {
        return PRE_INSTRUMENTED_ICON;
      } else if (nonPortableWorkState.isNeverPortable()) {
        return NEVER_PORTABLE_ICON;
      } else if (!nonPortableWorkState.isPortable()) {
        return NOT_PORTABLE_ICON;
      } else if (nonPortableWorkState.isTransient()) {
        return TRANSIENT_ICON;
      } else {
        return PORTABLE_ICON;
      }
    }

    return null;
  }

  protected void initTreeItem(TreeItem item, DefaultMutableTreeNode node) {
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
    DefaultTreeModel treeModel = fEvent.getApplicationEventContext().getTreeModel();

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

  private void handleTableSelectionChange() {
    TableItem[] selection = fIssueTable.getSelection();

    if (selection.length > 0) {
      TreeItem treeItem = (TreeItem) selection[0].getData();
      fObjectTree.setSelection(treeItem);
      handleTreeSelectionChange();
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

  protected void hideResolutionsPanel() {
    fResolutionsPanel.setVisible(false);
  }

  protected void showResolutionsPanel() {
    fResolutionsPanel.setVisible(true);
  }

  protected AbstractResolutionAction[] getActions(AbstractWorkState state) {
    AbstractResolutionAction[] actions = state.getActions();

    if (actions == null) {
      state.setActions(actions = createActions(state));
    }

    return actions;
  }

  private void handleActionSelectionChanged(IStructuredSelection selection) {
    Object obj = selection.getFirstElement();

    if (obj == NO_ACTION_ITEM) {
      hideActionPanel();
    } else {
      AbstractResolutionAction action = (AbstractResolutionAction) obj;
      if (action != null) {
        if (action.isSelected()) {
          action.showControl(this);
        } else {
          hideActionPanel();
        }
      }
    }
  }

  class ActionSelectionChangedHandler implements ISelectionChangedListener {
    public void selectionChanged(SelectionChangedEvent event) {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      handleActionSelectionChanged(selection);
    }
  }

  protected boolean haveAnyActions() {
    ActionTreeContentProvider contentProvider = (ActionTreeContentProvider) fActionTreeViewer.getContentProvider();
    AbstractResolutionAction[] actions = contentProvider.getEnabledActions();
    return actions != null && actions.length > 0;
  }

  protected boolean anyActionSelected() {
    ActionTreeContentProvider contentProvider = (ActionTreeContentProvider) fActionTreeViewer.getContentProvider();
    AbstractResolutionAction[] actions = contentProvider.getEnabledActions();

    if (actions != null) {
      for (AbstractResolutionAction action : actions) {
        if (action.isSelected()) { return true; }
      }
    }

    return false;
  }

  protected void setNoAction(boolean checked) {
    fActionTreeViewer.setChecked(NO_ACTION_ITEM, checked);
    fIssueTable.getSelection()[0].setImage(checked ? BLANK_ICON : RESOLVED_ICON);
  }

  private boolean isSetNoAction() {
    return fActionTreeViewer.getChecked(NO_ACTION_ITEM);
  }

  class ActionCheckStateHandler implements ICheckStateListener {
    public void checkStateChanged(CheckStateChangedEvent event) {
      Object obj = event.getElement();

      if (obj == NO_ACTION_ITEM) {
        handleNoActionItemSelected();
      } else {
        AbstractResolutionAction action = (AbstractResolutionAction) obj;
        if (action != null) {
          boolean isChecked = fActionTreeViewer.getChecked(action);

          action.setSelected(isChecked);
          if (isChecked) {
            setNoAction(false);
          } else if (!anyActionSelected()) {
            setNoAction(true);
          }
        }
      }

      StructuredSelection newSelection = new StructuredSelection(obj);
      fActionTreeViewer.setSelection(newSelection);
      handleActionSelectionChanged(newSelection);
      updateButtons();
    }
  }

  protected void updateButtons() {
    /**/
  }

  protected void hideActionPanel() {
    fActionStackLayout.topControl = fNoActionView;
    fActionPanel.layout();
    fActionPanel.redraw();
  }

  private void handleNoActionItemSelected() {
    if (isSetNoAction()) {
      hideActionPanel();

      ActionTreeContentProvider contentProvider = (ActionTreeContentProvider) fActionTreeViewer.getContentProvider();
      AbstractResolutionAction[] actions = contentProvider.getEnabledActions();

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

  public void widgetDefaultSelected(SelectionEvent e) {/**/
  }

  protected static final Object NO_ACTION_ITEM = new Object();

  class ActionTreeContentProvider implements ITreeContentProvider {
    AbstractWorkState          fWorkState;
    AbstractResolutionAction[] fEnabledActions;

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      fWorkState = (AbstractWorkState) newInput;
      fEnabledActions = null;

      if (fWorkState != null) {
        ArrayList list = new ArrayList();
        AbstractResolutionAction[] actions = getActions(fWorkState);

        if (actions != null && actions.length > 0) {
          for (AbstractResolutionAction action : actions) {
            if (action.isEnabled()) {
              list.add(action);
            }
          }
        }
        fEnabledActions = (AbstractResolutionAction[]) list.toArray(new AbstractResolutionAction[0]);
      }
    }

    public AbstractResolutionAction[] getEnabledActions() {
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

    public void dispose() {/**/
    }
  }

  private static class ActionLabelProvider implements ILabelProvider {
    public String getText(Object element) {
      if (element == NO_ACTION_ITEM) return NonPortableMessages.getString("TAKE_NO_ACTION");
      return element.toString();
    }

    public Image getImage(Object element) {
      return null;
    }

    public void addListener(ILabelProviderListener listener) {/**/
    }

    public void dispose() {/**/
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {/**/
    }
  }

  protected Include ensureIncludeRuleFor(String classExpr) {
    Include include = fConfigHelper.includeRuleFor(classExpr);

    if (include == null) {
      include = fConfigHelper.addIncludeRule(classExpr);
    }

    return include;
  }

  protected class IncludeRulePanel extends Composite implements SelectionListener {
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
      honorTransientButton.addSelectionListener(this);
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
      executeCodeText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          if (include == null) return;
          OnLoad onLoad = include.getOnLoad();
          if (onLoad == null) return;
          String executeText = executeCodeText.getText();
          onLoad.setExecute(executeText);
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

    public void widgetDefaultSelected(SelectionEvent e) {/**/
    }

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
      } else if (e.widget == honorTransientButton) {
        include.setHonorTransient(honorTransientButton.getSelection());
      }
    }
  }

  protected abstract String getDialogSettingsSectionName();

  protected abstract void initIssueList();

  protected abstract AbstractResolutionAction[] createActions(AbstractWorkState workState);

  protected abstract boolean anySelectedActions();

  protected abstract void handleTreeSelectionChange();

  protected abstract void createIssueDescriptionArea(Composite parent);
}
