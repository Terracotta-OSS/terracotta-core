/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.ui.util.SWTUtil;

import com.tc.admin.common.AbstractResolutionAction;
import com.tc.admin.common.AbstractWorkState;
import com.tc.admin.common.NonPortableMessages;
import com.tc.admin.common.NonPortableResolutionAction;
import com.tc.admin.common.NonPortableWorkState;
import com.tc.object.appevent.NonPortableEventContext;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.terracottatech.config.Include;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;

public class NonPortableObjectDialog extends AbstractApplicationEventDialog {
  private Text              fIssueDescription;
  private IncludeTypesPanel fIncludeTypesView;
  private Button            fPreviousIssueButton, fNextIssueButton, fApplyButton;

  public NonPortableObjectDialog(Shell parentShell, NonPortableObjectEvent event) {
    super(parentShell, NonPortableMessages.getString("PROBLEM_SHARING_DATA"), event, new String[] { //$NON-NLS-1$
        IDialogConstants.CANCEL_LABEL, NonPortableMessages.getString("PREVIOUS_ISSUE"), //$NON-NLS-1$
          NonPortableMessages.getString("NEXT_ISSUE"), //$NON-NLS-1$
          NonPortableMessages.getString("APPLY"), //$NON-NLS-1$
        }, 1);
  }

  private NonPortableObjectEvent getNonPortableObjectEvent() {
    return (NonPortableObjectEvent) getApplicationEvent();
  }

  private NonPortableEventContext getNonPortableEventContext() {
    return getNonPortableObjectEvent().getNonPortableEventContext();
  }

  protected String getDialogSettingsSectionName() {
    return TcPlugin.PLUGIN_ID + ".NON_PORTABLE_DIALOG_SECTION"; //$NON-NLS-1$
  }

  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    fPreviousIssueButton = getButton(1);
    fNextIssueButton = getButton(2);
    fApplyButton = getButton(3);
    fPreviousIssueButton.setEnabled(getPreviousIssue() != null);
    fNextIssueButton.setEnabled(getNextIssue() != null);
    fApplyButton.setEnabled(false);
  }

  protected void createIssueDescriptionArea(Composite parent) {
    fIssueDescription = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER | SWT.READ_ONLY);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.widthHint = SWTUtil.textColumnsToPixels(fIssueDescription, 80);
    gridData.heightHint = SWTUtil.textRowsToPixels(fIssueDescription, 8);
    fIssueDescription.setLayoutData(gridData);
  }

  protected Control createCustomArea(Composite parent) {
    Control customArea = super.createCustomArea(parent);
    fIncludeTypesView = new IncludeTypesPanel(fActionPanel);
    return customArea;
  }

  protected void buttonPressed(int buttonId) {
    switch (buttonId) {
      case 0:
        cancelPressed();
        break;
      case 1:
        gotoPreviousIssue();
        break;
      case 2:
        gotoNextIssue();
        break;
      case 3:
        apply();
        okPressed();
        break;
    }
  }

  protected boolean anySelectedActions() {
    for (int i = 0; i < fIssueTable.getItemCount(); i++) {
      TableItem tableItem = fIssueTable.getItem(i);
      TreeItem treeItem = (TreeItem) tableItem.getData();
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeItem.getData();

      if (node != null) {
        Object userObject = node.getUserObject();

        if (userObject instanceof AbstractWorkState) {
          AbstractWorkState workState = (AbstractWorkState) userObject;
          if (workState.hasSelectedActions()) { return true; }
        }
      }
    }

    return false;
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

  // NOTE: this is copied in ui-configurator/src/com/tc/NonPortableObjectPanel. ConfigHelper needs to be
  // unified before we can move this to NonPortableWorkState.testIsIssue(ConfigHelper).

  boolean testIsIssue(NonPortableWorkState workState) {
    String fieldName = workState.getFieldName();
    boolean isTransientField = fieldName != null && (fConfigHelper.isTransient(fieldName) || workState.isTransient());

    if (workState.isNull() || workState.isRepeated() || isTransientField) return false;

    if (workState.isNeverPortable() || workState.extendsLogicallyManagedType()) { return true; }

    if (workState.hasRequiredBootTypes()) {
      java.util.List types = workState.getRequiredBootTypes();
      for (Iterator iter = types.iterator(); iter.hasNext();) {
        if (!fConfigHelper.isBootJarClass((String) iter.next())) return true;
      }
    }

    if (workState.hasRequiredIncludeTypes()) {
      java.util.List types = workState.getRequiredIncludeTypes();
      for (Iterator iter = types.iterator(); iter.hasNext();) {
        if (!fConfigHelper.isAdaptable((String) iter.next())) return true;
      }
    }

    if (!workState.isPortable() && workState.isSystemType() && !fConfigHelper.isBootJarClass(workState.getTypeName())) return true;

    if (workState.getExplaination() != null) return true;

    return !workState.isPortable() && !fConfigHelper.isAdaptable(workState.getTypeName());
  }

  boolean checkAddToIssueList(NonPortableWorkState workState) {
    if (workState.hasSelectedActions()) { return true; }
    return testIsIssue(workState);
  }

  protected void initIssueList() {
    TreeItem treeItem = fObjectTree.getItem(0);

    fIssueTable.setRedraw(false);
    try {
      fIssueTable.setItemCount(0);
      while (treeItem != null) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeItem.getData();
        Object userObject = node.getUserObject();

        if (userObject instanceof NonPortableWorkState) {
          NonPortableWorkState workState = (NonPortableWorkState) userObject;

          if (checkAddToIssueList(workState)) {
            TableItem tableItem = new TableItem(fIssueTable, SWT.NONE);

            tableItem.setData(treeItem);
            tableItem.setImage(workState.hasSelectedActions() ? RESOLVED_ICON : BLANK_ICON);
            tableItem.setText(workState.shortSummary());
          }
        }

        treeItem = getNextItem(treeItem, true);
      }
    } finally {
      fIssueTable.setRedraw(true);
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

  protected void handleTreeSelectionChange() {
    TreeItem[] selection = fObjectTree.getSelection();
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selection[0].getData();
    Object userObject = node.getUserObject();

    syncTableAndTree();
    if (userObject instanceof NonPortableWorkState) {
      setWorkState((NonPortableWorkState) userObject);
    } else {
      hideResolutionsPanel();
    }
    updateButtons();
  }

  void setWorkState(AbstractWorkState workState) {
    fActionTreeViewer.getTree().setRedraw(false);
    try {
      showResolutionsPanel();
      hideActionPanel();

      fSummaryLabel.setText(workState.summary());
      fSummaryLabel.setImage(imageFor(workState));
      fIssueDescription.setText(workState.descriptionFor(getNonPortableEventContext()));
      fActionTreeViewer.setInput(workState);

      AbstractResolutionAction[] actions = getActions(workState);
      if (actions != null && actions.length > 0) {
        boolean showComponent = true;

        for (int i = 0; i < actions.length; i++) {
          AbstractResolutionAction action = actions[i];

          if (action.isEnabled()) {
            boolean isSelected = action.isSelected();

            fActionTreeViewer.setChecked(action, isSelected);
            if (isSelected) {
              if (showComponent) {
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
    } finally {
      fActionTreeViewer.getTree().setRedraw(true);
    }
  }

  private boolean requiresPortabilityAction(NonPortableWorkState workState) {
    if (workState.isTransient() || workState.extendsLogicallyManagedType()) return false;
    if (workState.hasRequiredBootTypes()) {
      java.util.List types = workState.getRequiredBootTypes();
      for (Iterator iter = types.iterator(); iter.hasNext();) {
        if (!fConfigHelper.isBootJarClass((String) iter.next())) return true;
      }
    }
    if (workState.hasNonPortableBaseTypes()) {
      java.util.List types = workState.getNonPortableBaseTypes();
      for (Iterator iter = types.iterator(); iter.hasNext();) {
        if (!fConfigHelper.isAdaptable((String) iter.next())) return true;
      }
    }
    return !fConfigHelper.isAdaptable(workState.getTypeName());
  }

  protected AbstractResolutionAction[] createActions(AbstractWorkState workState) {
    ArrayList list = new ArrayList();

    if (workState instanceof NonPortableWorkState) {
      NonPortableWorkState nonPortableWorkState = (NonPortableWorkState) workState;
      String fieldName = nonPortableWorkState.getFieldName();

      if (nonPortableWorkState.isNeverPortable() || nonPortableWorkState.isPortable()) {
        if (fieldName != null && !nonPortableWorkState.isTransient() && !fConfigHelper.isTransient(fieldName)) {
          list.add(new MakeTransientAction(nonPortableWorkState));
        }
      } else if (!nonPortableWorkState.isPortable()) {
        if (requiresPortabilityAction(nonPortableWorkState)) {
          list.add(new MakePortableAction(nonPortableWorkState));
        }
        if (fieldName != null && !nonPortableWorkState.isTransient() && !fConfigHelper.isTransient(fieldName)) {
          list.add(new MakeTransientAction(nonPortableWorkState));
        }
      }
    }

    return (AbstractResolutionAction[]) list.toArray(new AbstractResolutionAction[0]);
  }

  protected void updateButtons() {
    if(fApplyButton == null) return;
    fApplyButton.setEnabled(anySelectedActions());
    if (fPreviousIssueButton != null) {
      fPreviousIssueButton.setEnabled(getPreviousIssue() != null);
      fNextIssueButton.setEnabled(getNextIssue() != null);
    }
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
      try {
        if (selected) {
          fConfigHelper.ensureTransient(fWorkState.getFieldName(), NULL_SIGNALLER);
          treeItem.removeAll();
        } else {
          fConfigHelper.ensureNotTransient(fWorkState.getFieldName(), NULL_SIGNALLER);

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
      } finally {
        fIssueTable.setRedraw(true);
        fObjectTree.setRedraw(true);
      }
    }
  }

  class MakePortableAction extends NonPortableResolutionAction {
    MakePortableAction(NonPortableWorkState workState) {
      super(workState);
    }

    public void showControl(Object parentControl) {
      fActionStackLayout.topControl = fIncludeTypesView;
      fIncludeTypesView.setIncludeTypes(fWorkState.getRequiredIncludeTypes());
      fIncludeTypesView.setBootTypes(fWorkState.getRequiredBootTypes());
      fActionPanel.layout();
      fActionPanel.redraw();
    }

    public String getText() {
      return NonPortableMessages.getString("MAKE_PORTABLE"); //$NON-NLS-1$
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);

      if (fWorkState.hasRequiredBootTypes()) {
        java.util.List types = fWorkState.getRequiredBootTypes();
        if (selected) {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureBootJarClass((String) iter.next(), NULL_SIGNALLER);
          }
        } else {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureNotBootJarClass((String) iter.next(), NULL_SIGNALLER);
          }
        }
      }

      if (fWorkState.hasRequiredIncludeTypes()) {
        java.util.List types = fWorkState.getRequiredIncludeTypes();
        if (selected) {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureAdaptable((String) iter.next(), NULL_SIGNALLER);
          }
        } else {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureNotAdaptable((String) iter.next(), NULL_SIGNALLER);
          }
        }
      }

      int index = fIssueTable.getSelectionIndex();

      fIssueTable.setRedraw(false);
      try {
        initIssueList();
        fIssueTable.setSelection(index);
      } finally {
        fIssueTable.setRedraw(true);
      }
    }
  }

  class IncludeTypesPanel extends Composite {
    List fIncludeTypesList;
    List fBootTypesList;

    IncludeTypesPanel(Composite parent) {
      super(parent, SWT.NONE);
      setLayout(new GridLayout());
      Label label = new Label(this, SWT.NONE);
      label.setText(NonPortableMessages.getString("TYPES_TO_INCLUDE")); //$NON-NLS-1$
      label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      fIncludeTypesList = new List(this, SWT.BORDER | SWT.V_SCROLL);
      fIncludeTypesList.setLayoutData(new GridData(GridData.FILL_BOTH));

      label = new Label(this, SWT.NONE);
      label.setText(NonPortableMessages.getString("TYPES_TO_ADD_TO_BOOTJAR")); //$NON-NLS-1$
      label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      fBootTypesList = new List(this, SWT.BORDER | SWT.V_SCROLL);
      fBootTypesList.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    void setIncludeTypes(java.util.List types) {
      fIncludeTypesList.setRedraw(false);
      try {
        fIncludeTypesList.setItems(new String[0]);
        if (types != null) {
          ConfigurationHelper configHelper = TcPlugin.getDefault().getConfigurationHelper(fJavaProject.getProject());
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            String type = (String) iter.next();
            if (!configHelper.isAdaptable(type)) {
              fIncludeTypesList.add(type);
            }
          }
        }
      } finally {
        fIncludeTypesList.setRedraw(true);
      }
    }

    void setBootTypes(java.util.List types) {
      fBootTypesList.setRedraw(false);
      try {
        fBootTypesList.setItems(new String[0]);
        if (types != null) {
          ConfigurationHelper configHelper = TcPlugin.getDefault().getConfigurationHelper(fJavaProject.getProject());

          for (Iterator iter = types.iterator(); iter.hasNext();) {
            String type = (String) iter.next();
            if (!configHelper.isBootJarClass(type)) {
              fBootTypesList.add(type);
            }
          }
        }
      } finally {
        fBootTypesList.setRedraw(true);
      }
    }
  }
}
