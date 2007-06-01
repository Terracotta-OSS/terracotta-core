/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc;

import org.dijon.Button;
import org.dijon.CheckBox;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Dialog;
import org.dijon.Label;
import org.dijon.PagedView;
import org.dijon.RadioButton;
import org.dijon.TextArea;
import org.dijon.TextField;

import com.tc.admin.common.NonPortableMessages;
import com.tc.admin.common.NonPortableResolutionAction;
import com.tc.admin.common.NonPortableWalkNode;
import com.tc.admin.common.NonPortableWorkState;
import com.tc.admin.common.XCellEditor;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XList;
import com.tc.admin.common.XTextPane;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeCellRenderer;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.NonPortableObjectState;
import com.terracottatech.config.Include;
import com.terracottatech.config.OnLoad;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class NonPortableObjectPanel extends XContainer implements TreeSelectionListener {
  private SessionIntegratorFrame    fMainFrame;
  private Label                     fMessageLabel;
  private XList                     fIssueList;
  private DefaultListModel          fIssueListModel;
  private IssueListSelectionHandler fIssueListSelectionHandler;
  private XTree                     fObjectTree;
  private Container                 fIssueDetailsPanel;
  private Label                     fSummaryLabel;
  private XTextPane                 fDescriptionText;
  private Container                 fResolutionsPanel;
  private XTree                     fActionTree;
  private PagedView                 fActionPanel;
  private TextField                 fIncludePatternField;
  private CheckBox                  fHonorTransientToggle;
  private ButtonGroup               fOnLoadGroup;
  private ActionListener            fOnLoadButtonGroupHandler;
  private RadioButton               fOnLoadDoNothingToggle;
  private RadioButton               fOnLoadMethodToggle;
  private TextField                 fOnLoadMethodField;
  private RadioButton               fOnLoadCodeToggle;
  private TextArea                  fOnLoadCodeText;
  private XList                     fBootTypesList;
  private Button                    fPreviousIssueButton;
  private Button                    fNextIssueButton;
  private Button                    fApplyButton;
  private Button                    fCancelButton;
  private ConfigHelper              fConfigHelper;
  private TcConfig                  fNewConfig;

  private static final ImageIcon    NOT_PORTABLE_ICON     = new ImageIcon(NonPortableWalkNode.class
                                                              .getResource("/com/tc/admin/icons/field_private_obj.gif"));
  private static final ImageIcon    NEVER_PORTABLE_ICON   = new ImageIcon(NonPortableWalkNode.class
                                                              .getResource("/com/tc/admin/icons/field_private_obj.gif"));
  private static final ImageIcon    TRANSIENT_ICON        = new ImageIcon(NonPortableWalkNode.class
                                                              .getResource("/com/tc/admin/icons/field_public_obj.gif"));
  private static final ImageIcon    PORTABLE_ICON         = new ImageIcon(NonPortableWalkNode.class
                                                              .getResource("/com/tc/admin/icons/field_default_obj.gif"));
  private static final ImageIcon    PRE_INSTRUMENTED_ICON = new ImageIcon(
                                                                          NonPortableWalkNode.class
                                                                              .getResource("/com/tc/admin/icons/field_protected_obj.gif"));
  private static final ImageIcon    OBJ_CYCLE_ICON        = new ImageIcon(NonPortableWalkNode.class
                                                              .getResource("/com/tc/admin/icons/obj_cycle.gif"));
  private static final ImageIcon    RESOLVED_ICON         = new ImageIcon(NonPortableWalkNode.class
                                                              .getResource("/com/tc/admin/icons/installed_ovr.gif"));
  private static final ImageIcon    BLANK_ICON            = new ImageIcon(NonPortableWalkNode.class
                                                              .getResource("/com/tc/admin/icons/blank.gif"));

  public NonPortableObjectPanel(ContainerResource res, SessionIntegratorFrame frame) {
    super();
    load(res);
    fMainFrame = frame;
    fNewConfig = (TcConfig) frame.getConfigHelper().getConfig().copy();
    fConfigHelper = new ConfigCopyHelper();
  }

  class ConfigCopyHelper extends ConfigHelper {
    public TcConfig getConfig() {
      return fNewConfig;
    }
  }

  public void load(ContainerResource res) {
    super.load(res);

    fMessageLabel = (Label)findComponent("MessageLabel");
    
    fIssueList = (XList) findComponent("IssueList");
    fIssueList.setModel(fIssueListModel = new DefaultListModel());
    fIssueList.setCellRenderer(new IssueListCellRenderer());
    fIssueList.addListSelectionListener(fIssueListSelectionHandler = new IssueListSelectionHandler());
    fIssueList.setVisibleRowCount(15);

    fObjectTree = (XTree) findComponent("Tree");
    fObjectTree.setCellRenderer(new ObjectTreeCellRenderer());
    ((DefaultTreeSelectionModel) fObjectTree.getSelectionModel()).setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    fObjectTree.addTreeSelectionListener(this);

    fIssueDetailsPanel = (Container) findComponent("IssueDetailsPanel");
    
    fSummaryLabel = (Label) findComponent("SummaryLabel");
    fDescriptionText = (XTextPane) findComponent("DescriptionText");
    fResolutionsPanel = (Container) findComponent("ResolutionsPanel");

    fActionTree = (XTree) findComponent("ActionTree");
    fActionTree.addTreeSelectionListener(new ActionTreeSelectionHandler());
    fActionTree.setCellRenderer(new ActionTreeNodeRenderer());
    fActionTree.setRootVisible(true);
    fActionTree.addMouseListener(new ActionTreeMouseListener());
    ((DefaultTreeModel)fActionTree.getModel()).setRoot(new ActionTreeRootNode());
    
    fActionPanel = (PagedView)findComponent("ActionPanel");

    fIncludePatternField = (TextField) findComponent("PatternField");
    fHonorTransientToggle = (CheckBox) findComponent("HonorTransientToggle");
    fOnLoadGroup = new ButtonGroup();
    fOnLoadButtonGroupHandler = new OnLoadButtonGroupHandler();
    fOnLoadGroup.add(fOnLoadDoNothingToggle = (RadioButton) findComponent("DoNothingButton"));
    fOnLoadDoNothingToggle.addActionListener(fOnLoadButtonGroupHandler);
    fOnLoadGroup.add(fOnLoadMethodToggle = (RadioButton) findComponent("CallMethodButton"));
    fOnLoadMethodToggle.addActionListener(fOnLoadButtonGroupHandler);
    fOnLoadGroup.add(fOnLoadCodeToggle = (RadioButton) findComponent("ExecuteCodeButton"));
    fOnLoadCodeToggle.addActionListener(fOnLoadButtonGroupHandler);
    fOnLoadMethodField = (TextField) findComponent("CallMethodField");
    fOnLoadCodeText = (TextArea) findComponent("ExecuteCodeText");
    fOnLoadCodeText.setRows(3);

    fBootTypesList = (XList) findComponent("BootTypesList");

    fNextIssueButton = (Button) findComponent("NextIssueButton");
    fNextIssueButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        gotoNextIssue();
      }
    });
    fPreviousIssueButton = (Button) findComponent("PreviousIssueButton");
    fPreviousIssueButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        gotoPreviousIssue();
      }
    });
    fApplyButton = (Button) findComponent("ApplyButton");
    fApplyButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        apply();
      }
    });
    fCancelButton = (Button) findComponent("CancelButton");
    fCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        cancel();
      }
    });
  }

  private void apply() {
    ConfigHelper configHelper = fMainFrame.getConfigHelper();
    configHelper.setConfig(fNewConfig);
    fMainFrame.modelChanged();
    close();
  }

  private void cancel() {
    close();
  }

  private void close() {
    Dialog dialog = (Dialog) getAncestorOfClass(Dialog.class);
    dialog.setVisible(false);
  }

  void gotoNextIssue() {
    DefaultMutableTreeNode nextIssue = getNextIssue();
    if (nextIssue != null) {
      TreePath treePath = new TreePath(nextIssue.getPath());
      fObjectTree.setSelectionPath(treePath);
      fObjectTree.scrollPathToVisible(treePath);
    }
  }

  private DefaultMutableTreeNode getNextIssue(DefaultMutableTreeNode item) {
    while ((item = item.getNextNode()) != null) {
      Object userObject = item.getUserObject();

      if (userObject instanceof NonPortableWorkState) {
        NonPortableWorkState workState = (NonPortableWorkState) userObject;
        if (testIsIssue(workState)) { return item; }
      }
    }

    return null;
  }

  DefaultMutableTreeNode getNextIssue() {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) fObjectTree.getLastSelectedPathComponent();
    if (node != null) { return getNextIssue(node); }
    return null;
  }

  void gotoPreviousIssue() {
    DefaultMutableTreeNode previousIssue = getPreviousIssue();
    if (previousIssue != null) {
      TreePath treePath = new TreePath(previousIssue.getPath());
      fObjectTree.setSelectionPath(treePath);
      fObjectTree.scrollPathToVisible(treePath);
    }
  }

  DefaultMutableTreeNode getPreviousIssue(DefaultMutableTreeNode item) {
    while ((item = item.getPreviousNode()) != null) {
      Object userObject = item.getUserObject();

      if (userObject instanceof NonPortableWorkState) {
        NonPortableWorkState workState = (NonPortableWorkState) userObject;
        if (testIsIssue(workState)) { return item; }
      }
    }

    return null;
  }

  DefaultMutableTreeNode getPreviousIssue() {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) fObjectTree.getLastSelectedPathComponent();
    if (node != null) { return getPreviousIssue(node); }
    return null;
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
    fIssueList.removeListSelectionListener(fIssueListSelectionHandler);
    fIssueListModel.clear();
    Enumeration e = ((DefaultMutableTreeNode) fObjectTree.getModel().getRoot()).breadthFirstEnumeration();
    while (e.hasMoreElements()) {
      Object obj = e.nextElement();

      if (obj instanceof DefaultMutableTreeNode) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
        Object userObject = node.getUserObject();

        if (userObject instanceof NonPortableWorkState) {
          NonPortableWorkState workState = (NonPortableWorkState) userObject;

          if (checkAddToIssueList(workState)) {
            String label = workState.shortSummary();
            Icon icon = workState.hasSelectedActions() ? RESOLVED_ICON : BLANK_ICON;
            fIssueListModel.addElement(new IssueListItem(node, label, icon));
          }
        }
      }
    }
    fIssueList.addListSelectionListener(fIssueListSelectionHandler);
  }

  class IssueListSelectionHandler implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      if (!e.getValueIsAdjusting() && fIssueList.getModel().getSize() > 0) {
        int index = e.getFirstIndex();
        IssueListItem item = (IssueListItem) fIssueList.getModel().getElementAt(index);
        DefaultMutableTreeNode node = item.fTreeNode;
        TreePath path = new TreePath(node.getPath());

        fObjectTree.setSelectionPath(path);
      }
    }
  }

  class IssueListCellRenderer extends JLabel implements ListCellRenderer {
    public IssueListCellRenderer() {
      setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus) {
      if (value instanceof IssueListItem) {
        IssueListItem item = (IssueListItem) value;

        setText(item.fLabel);
        setIcon(item.fIcon);
      }

      return this;
    }
  }

  class IssueListItem {
    DefaultMutableTreeNode fTreeNode;
    String                 fLabel;
    Icon                   fIcon;

    IssueListItem(DefaultMutableTreeNode treeNode, String label, Icon icon) {
      fTreeNode = treeNode;
      fLabel = label;
      fIcon = icon;
    }

    void setIcon(Icon icon) {
      fIcon = icon;
    }
  }

  public void setEvent(NonPortableObjectEvent event) {
    if (event == null) { return; }

    fMessageLabel.setText(event.getReason().getMessage());
    
    fObjectTree.setModel(event.getContext().getTreeModel());

    DefaultMutableTreeNode root = (DefaultMutableTreeNode) fObjectTree.getModel().getRoot();
    Enumeration enumeration = root.preorderEnumeration();
    while (enumeration.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
      Object userObject = node.getUserObject();
      if (userObject instanceof NonPortableObjectState) {
        userObject = new NonPortableWorkState((NonPortableObjectState) userObject);
        node.setUserObject(userObject);
      }
    }

    initIssueList();
    resetSearchButtons();
    
    if (fIssueList.getModel().getSize() > 0) {
      fIssueList.setSelectedIndex(0);
    }
  }

  private void resetSearchButtons() {
    fPreviousIssueButton.setEnabled(getPreviousIssue() != null);
    fNextIssueButton.setEnabled(getNextIssue() != null);
  }
  
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) fObjectTree.getLastSelectedPathComponent();
    
    if(node != null) {
      Object userObject = node.getUserObject();
  
      selectIssueListItem(node);
      if (userObject instanceof NonPortableWorkState) {
        NonPortableWorkState workState = (NonPortableWorkState) userObject;
        setWorkState(workState);
      } else {
        hideIssueDetailsPanel();
      }
      fPreviousIssueButton.setEnabled(getPreviousIssue() != null);
      fNextIssueButton.setEnabled(getNextIssue() != null);
    } else {
      fIssueList.removeListSelectionListener(fIssueListSelectionHandler);
      fIssueList.clearSelection();
      fIssueList.addListSelectionListener(fIssueListSelectionHandler);
    }
  }

  private void selectIssueListItem(DefaultMutableTreeNode node) {
    fIssueList.removeListSelectionListener(fIssueListSelectionHandler);
    for (int i = 0; i < fIssueListModel.getSize(); i++) {
      IssueListItem item = (IssueListItem) fIssueListModel.get(i);
      if (item.fTreeNode == node) {
        fIssueList.setSelectedIndex(i);
        fIssueList.addListSelectionListener(fIssueListSelectionHandler);
        return;
      }
    }
    fIssueList.clearSelection();
    fIssueList.addListSelectionListener(fIssueListSelectionHandler);
  }

  Icon iconFor(NonPortableWorkState workState) {
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

  class ObjectTreeCellRenderer extends XTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean focused) {
      Label label = (Label) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);

      label.setIcon(PORTABLE_ICON);
      if (value instanceof DefaultMutableTreeNode) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();

        if (userObject instanceof NonPortableWorkState) {
          NonPortableWorkState workState = (NonPortableWorkState) userObject;
          label.setText(workState.getLabel());
          label.setIcon(iconFor(workState));
        }
      }

      return label;
    }
  }

  void setWorkState(NonPortableWorkState workState) {
    showIssueDetailsPanel();
    fSummaryLabel.setText(workState.summary());
    fSummaryLabel.setIcon(iconFor(workState));
    fDescriptionText.setText(workState.descriptionFor());
    fDescriptionText.select(0,0);
    fResolutionsPanel.setVisible(true);
    DefaultMutableTreeNode actionTreeRoot = (DefaultMutableTreeNode)fActionTree.getModel().getRoot();
    actionTreeRoot.removeAllChildren();
    createActionNodes(workState);
    ((DefaultTreeModel)fActionTree.getModel()).nodeStructureChanged(actionTreeRoot);
    if (actionTreeRoot.getChildCount() == 0) {
      hideResolutionsPanel();
      return;
    }
    fActionTree.expandRow(0);
    fResolutionsPanel.revalidate();
    fResolutionsPanel.repaint();
  }

  private void hideIssueDetailsPanel() {
    fIssueDetailsPanel.setVisible(false);
  }

  private void showIssueDetailsPanel() {
    fIssueDetailsPanel.setVisible(true);
  }

  private void hideResolutionsPanel() {
    fResolutionsPanel.setVisible(false);
  }

  class ActionTreeNode extends DefaultMutableTreeNode {
    ActionTreeNode(NonPortableResolutionAction action) {
      super(action);
    }
    
    NonPortableResolutionAction getAction() {
      Object obj = getUserObject();
      if(obj instanceof NonPortableResolutionAction) {
        return (NonPortableResolutionAction)obj;
      }
      return null;
    }

    boolean isSelected() {
      NonPortableResolutionAction action = getAction();
      return action != null ? action.isSelected() : false;
    }
    
    void setSelected(boolean selected) {
      NonPortableResolutionAction action = getAction();
      if(action != null) {
        action.setSelected(selected);
        if(selected) {
          ((ActionTreeRootNode)getParent()).setSelected(false);
        } else {
          ((ActionTreeRootNode)getParent()).testSelect();
        }
      }
    }
    
    void showControl(NonPortableObjectPanel panel) {
      NonPortableResolutionAction action = getAction();
      if(action != null && action.isSelected()) {
        action.showControl(panel);
      } else {
        hideActionPanel();
      }
    }
  }
  
  class ActionTreeRootNode extends ActionTreeNode {
    boolean fSelected;
  
    ActionTreeRootNode() {
      super(null);
      setUserObject("Take no action");
    }
    
    boolean isSelected() {
      return fSelected;
    }
    
    void setSelected(boolean selected) {
      if((fSelected = selected) == true) {
        for(int i = 0; i < getChildCount(); i++) {
          ((ActionTreeNode)getChildAt(i)).setSelected(false);
        }
      }
    }
    
    void testSelect() {
      for(int i = 0; i < getChildCount(); i++) {
        if(((ActionTreeNode)getChildAt(i)).isSelected()) {
          return;
        }
      }
      fSelected = true;
    }
  }
  
  class ActionTreeNodeRenderer extends DefaultTreeCellRenderer {
    XCheckBox fCheckBox = new XCheckBox();
    XCellEditor fCellRenderer = new XCellEditor(fCheckBox);
    
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                  boolean expanded, boolean leaf, int row,
                                                  boolean focused)
    {
      Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
      
      if(value instanceof ActionTreeNode) {
        ActionTreeNode actionNode = (ActionTreeNode)value;
        comp = fCellRenderer.getTreeCellEditorComponent(tree, value, sel, expanded, leaf, row);
        fCheckBox.setSelected(actionNode.isSelected());
        fCheckBox.setText(actionNode.toString());
        fCheckBox.setBackground(tree.getBackground());
      }
      
      return comp;
    }
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

  void createActionNodes(NonPortableWorkState state) {
    DefaultMutableTreeNode actionTreeRoot = (DefaultMutableTreeNode)fActionTree.getModel().getRoot();
    NonPortableResolutionAction[] actions = getActions(state);
    NonPortableResolutionAction action;

    if (actions != null && actions.length > 0) {
      for (int i = 0; i < actions.length; i++) {
        action = actions[i];
        
        if (action.isEnabled()) {
          actionTreeRoot.add(new ActionTreeNode(action));
          if(action.isSelected()) {
            ((IssueListItem) fIssueList.getSelectedValue()).setIcon(RESOLVED_ICON);
            fApplyButton.setEnabled(true);
          }
        }
      }
    }
  }

  private void hideActionPanel() {
    fActionPanel.setPage("EmptyPage");
  }
  
  private Iterator selectedActions() {
    ArrayList list = new ArrayList();

    for (int i = 0; i < fIssueList.getModel().getSize(); i++) {
      IssueListItem item = (IssueListItem) fIssueList.getModel().getElementAt(i);
      DefaultMutableTreeNode node = item.fTreeNode;
      NonPortableWorkState workState = (NonPortableWorkState) node.getUserObject();
      NonPortableResolutionAction[] actions = workState.getActions();

      if (actions != null && actions.length > 0) {
        for (int j = 0; j < actions.length; j++) {
          if (actions[j].isSelected()) {
            list.add(actions[j]);
          }
        }
      }
    }

    return list.iterator();
  }

  private boolean anySelectedActions() {
    return selectedActions().hasNext();
  }

  class ActionTreeMouseListener extends MouseAdapter {
    public void mouseClicked(MouseEvent e) {
      TreePath path = fActionTree.getPathForLocation(e.getX(), e.getY());
      if(path != null) {
        ActionTreeNode actionNode = (ActionTreeNode)path.getLastPathComponent();

        if(actionNode != null) {
          boolean actionSelected = !actionNode.isSelected();
          
          actionNode.setSelected(actionSelected);
          actionNode.showControl(NonPortableObjectPanel.this);

          fActionTree.setSelectionPath(path);
          fApplyButton.setEnabled(actionSelected ? true : anySelectedActions());
          fActionTree.repaint();
        }
      }
    }
  }
  
  class ActionTreeSelectionHandler implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      ActionTreeNode actionNode = (ActionTreeNode)fActionTree.getLastSelectedPathComponent();
      if(actionNode != null) {
        NonPortableResolutionAction action = actionNode.getAction();
        
        if (action != null) {
          if(action.isSelected()) {
            action.showControl(NonPortableObjectPanel.this);
          } else {
            hideActionPanel();
          }
        }
      }
    }
  }

  void setRequiredBootTypes(String[] types) {
    fBootTypesList.setListData(types);
  }

  void setInclude(Include include) {
    fIncludePatternField.setText(include.getClassExpression());
    fHonorTransientToggle.setSelected(include.getHonorTransient());
    fOnLoadMethodField.setText("");
    fOnLoadCodeText.setText("");
    if (include.isSetOnLoad()) {
      OnLoad onLoad = include.getOnLoad();
      if (onLoad.isSetMethod()) {
        fOnLoadMethodToggle.setSelected(true);
        fOnLoadMethodField.setText(onLoad.getMethod());
      } else if (onLoad.isSetExecute()) {
        fOnLoadCodeToggle.setSelected(true);
        fOnLoadCodeText.setText(onLoad.getExecute());
      } else {
        fOnLoadDoNothingToggle.setSelected(false);
      }
    } else {
      fOnLoadDoNothingToggle.setSelected(true);
    }
  }

  class OnLoadButtonGroupHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      RadioButton radioButton = (RadioButton) ae.getSource();

      if (radioButton == fOnLoadDoNothingToggle) {
        fOnLoadMethodField.setText("");
        fOnLoadMethodField.setEnabled(false);

        fOnLoadCodeText.setText("");
        fOnLoadCodeText.setEnabled(false);
      } else if (radioButton == fOnLoadMethodToggle) {
        fOnLoadCodeText.setText("");
        fOnLoadCodeText.setEnabled(false);
      } else {
        fOnLoadMethodField.setText("");
        fOnLoadMethodField.setEnabled(false);
      }
    }
  }

  class MakeTransientAction extends NonPortableResolutionAction {
    ArrayList fRemovedChildNodes;

    MakeTransientAction(NonPortableWorkState workState) {
      super(workState);
    }

    public void showControl(Object parentControl) {
      NonPortableObjectPanel parent = (NonPortableObjectPanel) parentControl;
      String fieldName = fWorkState.getFieldName();
      String declaringType = fieldName.substring(0, fieldName.lastIndexOf('.'));
      Include include = fConfigHelper.includeRuleFor(declaringType);

      if (include != null) {
        parent.fActionPanel.setPage("IncludeRulePage");
        parent.setInclude(include);
      }
    }

    public String getText() {
      return NonPortableMessages.getString("DO_NOT_SHARE"); //$NON-NLS-1$
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);

      int index = fIssueList.getSelectedIndex();
      DefaultMutableTreeNode treeItem = (DefaultMutableTreeNode) fObjectTree.getLastSelectedPathComponent();

      if (selected) {
        fConfigHelper.ensureTransient(fWorkState.getFieldName());
        if (fRemovedChildNodes == null) fRemovedChildNodes = new ArrayList();
        for (int i = 0; i < treeItem.getChildCount(); i++) {
          fRemovedChildNodes.add(treeItem.getChildAt(i));
        }
        treeItem.removeAllChildren();
      } else {
        fConfigHelper.ensureNotTransient(fWorkState.getFieldName());

        for (int i = 0; i < fRemovedChildNodes.size(); i++) {
          treeItem.add((DefaultMutableTreeNode) fRemovedChildNodes.get(i));
        }
        fRemovedChildNodes.clear();
      }
      ((DefaultTreeModel) fObjectTree.getModel()).reload(treeItem);

      initIssueList();
      fObjectTree.setSelectionPath(new TreePath(treeItem.getPath()));
      fIssueList.setSelectedIndex(index);
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
      NonPortableObjectPanel parent = (NonPortableObjectPanel) parentControl;
      parent.fActionPanel.setPage("IncludeRulePage");
      parent.setInclude(fConfigHelper.includeRuleFor(getClassExpression()));
    }

    public String getText() {
      return NonPortableMessages.getString("INCLUDE_TYPE_FOR_SHARING"); //$NON-NLS-1$
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);

      String classExpr = getClassExpression();
      if (selected) {
        fConfigHelper.ensureIncludeRuleFor(classExpr);
      } else {
        fConfigHelper.ensureNotAdaptable(classExpr);
      }

      int index = fIssueList.getSelectedIndex();
      initIssueList();
      fIssueList.setSelectedIndex(index);
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
      NonPortableObjectPanel parent = (NonPortableObjectPanel) parentControl;
      parent.setRequiredBootTypes(fWorkState.getRequiredBootTypes());
      parent.fActionPanel.setPage("BootTypesPage");
    }

    public String getText() {
      return NonPortableMessages.getString("ADD_TO_BOOTJAR"); //$NON-NLS-1$
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);

      String[] types = fWorkState.getRequiredBootTypes();

      if (selected) {
        for (int i = 0; i < types.length; i++) {
          fConfigHelper.ensureBootJarClass(types[i]);
        }
      } else {
        for (int i = 0; i < types.length; i++) {
          fConfigHelper.ensureNotBootJarClass(types[i]);
        }
      }

      int index = fIssueList.getSelectedIndex();
      initIssueList();
      fIssueList.setSelectedIndex(index);
      resetSearchButtons();
    }
  }
}
