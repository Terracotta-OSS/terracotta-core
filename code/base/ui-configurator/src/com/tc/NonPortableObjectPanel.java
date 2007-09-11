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
import org.dijon.EmptyBorder;
import org.dijon.Label;
import org.dijon.PagedView;
import org.dijon.RadioButton;
import org.dijon.TextArea;
import org.dijon.TextField;

import com.tc.admin.common.AbstractResolutionAction;
import com.tc.admin.common.AbstractWorkState;
import com.tc.admin.common.NonPortableMessages;
import com.tc.admin.common.NonPortableResolutionAction;
import com.tc.admin.common.NonPortableWalkNode;
import com.tc.admin.common.NonPortableWorkState;
import com.tc.admin.common.XCellEditor;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XList;
import com.tc.admin.common.XTextField;
import com.tc.admin.common.XTextPane;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeCellRenderer;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.NonPortableObjectState;
import com.terracottatech.config.Include;
import com.terracottatech.config.OnLoad;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class NonPortableObjectPanel extends XContainer implements TreeSelectionListener {
  private NonPortableObjectEvent    fEvent;
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
  private XList                     fIncludeTypesList;
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
                                                              .getResource("/com/tc/admin/icons/blank12x12.gif"));

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

    fMessageLabel = (Label) findComponent("MessageLabel");

    fIssueList = (XList) findComponent("IssueList");
    fIssueList.setModel(fIssueListModel = new DefaultListModel());
    fIssueList.setCellRenderer(new IssueListCellRenderer());
    fIssueList.addListSelectionListener(fIssueListSelectionHandler = new IssueListSelectionHandler());
    fIssueList.setVisibleRowCount(15);

    fObjectTree = (XTree) findComponent("Tree");
    fObjectTree.setCellRenderer(new ObjectTreeCellRenderer());
    ((DefaultTreeSelectionModel) fObjectTree.getSelectionModel())
        .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    fObjectTree.addTreeSelectionListener(this);

    fIssueDetailsPanel = (Container) findComponent("IssueDetailsPanel");

    fSummaryLabel = (Label) findComponent("SummaryLabel");
    fDescriptionText = (XTextPane) findComponent("DescriptionText");
    fResolutionsPanel = (Container) findComponent("ResolutionsPanel");

    fActionTree = (XTree) findComponent("ActionTree");
    fActionTree.addTreeSelectionListener(new ActionTreeSelectionHandler());
    fActionTree.setCellRenderer(new ActionTreeNodeRenderer());
    fActionTree.setCellEditor(new ActionTreeNodeEditor());
    fActionTree.setRootVisible(true);
    fActionTree.setEditable(true);
    ((DefaultTreeModel) fActionTree.getModel()).setRoot(new ActionTreeRootNode());
    fActionTree.setInvokesStopCellEditing(true);

    fActionPanel = (PagedView) findComponent("ActionPanel");

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
    fIncludeTypesList = (XList) findComponent("IncludeTypesList");

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
    fMainFrame.saveAndStart();
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

  // NOTE: this is copied in ui-eclipse/src/org/terracotta/dso/dialogs/NonPortableObjectDialog.
  // ConfigHelper needs to be unified before we can move this to NonPortableWorkState.testIsIssue(ConfigHelper).

  boolean testIsIssue(NonPortableWorkState workState) {
    String fieldName = workState.getFieldName();
    boolean isTransientField = fieldName != null && (fConfigHelper.isTransient(fieldName) || workState.isTransient());

    if(workState.isNull() || workState.isRepeated() || isTransientField) return false;
    
    if (workState.isNeverPortable() || workState.extendsLogicallyManagedType()) { return true; }

    if (workState.hasRequiredBootTypes()) {
      java.util.List types = workState.getRequiredBootTypes();
      for (Iterator iter = types.iterator(); iter.hasNext();) {
        if (!fConfigHelper.isBootJarClass((String)iter.next())) return true;
      }
    }

    if (workState.hasRequiredIncludeTypes()) {
      java.util.List types = workState.getRequiredIncludeTypes();
      for (Iterator iter = types.iterator(); iter.hasNext();) {
        if (!fConfigHelper.isAdaptable((String)iter.next())) return true;
      }
    }

    if(!workState.isPortable() && workState.isSystemType() && !fConfigHelper.isBootJarClass(workState.getTypeName())) return true;
    
    if(workState.getExplaination() != null) return true;
    
    return !workState.isPortable() && !fConfigHelper.isAdaptable(workState.getTypeName());
  }

  boolean checkAddToIssueList(NonPortableWorkState workState) {
    if (workState.hasSelectedActions()) { return true; }
    return testIsIssue(workState);
  }

  void initIssueList() {
    fIssueList.removeListSelectionListener(fIssueListSelectionHandler);
    fIssueListModel.clear();
    Enumeration e = ((DefaultMutableTreeNode) fObjectTree.getModel().getRoot()).preorderEnumeration();
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
        int index = fIssueList.getSelectedIndex();
        IssueListItem item = (IssueListItem) fIssueList.getModel().getElementAt(index);
        DefaultMutableTreeNode node = item.fTreeNode;
        TreePath path = new TreePath(node.getPath());

        fObjectTree.setSelectionPath(path);
        fObjectTree.scrollPathToVisible(path);
      }
    }
  }

  class IssueListCellRenderer extends DefaultListCellRenderer {
    public IssueListCellRenderer() {
      setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
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

    fEvent = event;
    fMessageLabel.setText(event.getNonPortableEventReason().getMessage());

    fObjectTree.setModel(event.getNonPortableEventContext().getTreeModel());

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

    if (node != null) {
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
    fSummaryLabel.setText("<html><p>"+workState.summary()+"</p></html>");
    fSummaryLabel.setIcon(iconFor(workState));
    fDescriptionText.setText(workState.descriptionFor(fEvent.getNonPortableEventContext()));
    fDescriptionText.select(0, 0);
    fResolutionsPanel.setVisible(true);
    DefaultMutableTreeNode actionTreeRoot = (DefaultMutableTreeNode) fActionTree.getModel().getRoot();
    actionTreeRoot.removeAllChildren();
    createActionNodes(workState);
    ((DefaultTreeModel) fActionTree.getModel()).nodeStructureChanged(actionTreeRoot);
    if (actionTreeRoot.getChildCount() == 0) {
      hideResolutionsPanel();
      return;
    }
    ((DefaultTreeModel) fActionTree.getModel()).reload();
    fActionTree.expandRow(0);
    fResolutionsPanel.revalidate();
    fResolutionsPanel.repaint();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        fActionTree.startEditingAtPath(fActionTree.getPathForRow(0));
      }
    });
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
    ActionTreeNode(AbstractResolutionAction action) {
      super(action);
    }

    public void setUserObject(Object o) {/**/
    }

    AbstractResolutionAction getAction() {
      Object obj = getUserObject();
      if (obj instanceof AbstractResolutionAction) { return (AbstractResolutionAction) obj; }
      return null;
    }

    boolean isSelected() {
      AbstractResolutionAction action = getAction();
      return action != null ? action.isSelected() : false;
    }

    void setSelected(boolean selected) {
      AbstractResolutionAction action = getAction();
      if (action != null) {
        action.setSelected(selected);
        if (selected) {
          ((ActionTreeRootNode) getParent()).setSelected(false);
        } else {
          ((ActionTreeRootNode) getParent()).testSelect();
        }
        fireNodeChanged();
      }
    }

    void showControl(NonPortableObjectPanel panel) {
      AbstractResolutionAction action = getAction();
      if (action != null && action.isSelected()) {
        action.showControl(panel);
      } else {
        hideActionPanel();
      }
    }

    void fireNodeChanged() {
      ((DefaultTreeModel) fActionTree.getModel()).nodeChanged(this);
    }
  }

  class ActionTreeRootNode extends ActionTreeNode {
    boolean fSelected;

    ActionTreeRootNode() {
      super(null);
      userObject = NonPortableMessages.getString("TAKE_NO_ACTION");
    }

    boolean isSelected() {
      return fSelected;
    }

    void setSelected(boolean selected) {
      if ((fSelected = selected) == true) {
        for (int i = 0; i < getChildCount(); i++) {
          ((ActionTreeNode) getChildAt(i)).setSelected(false);
        }
      }
      fireNodeChanged();
    }

    void testSelect() {
      for (int i = 0; i < getChildCount(); i++) {
        if (((ActionTreeNode) getChildAt(i)).isSelected()) { return; }
      }
      fSelected = true;
      fireNodeChanged();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fActionTree.selectTop();
          fActionTree.startEditingAtPath(fActionTree.getPathForRow(0));
        }
      });
    }
  }

  static class ActionTreeCellRendererComponent extends Container {
    public XCheckBox checkBox;
    public XLabel    label;
    public Container container;

    public ActionTreeCellRendererComponent() {
      super();
      setLayout(new FlowLayout(SwingConstants.CENTER, 3, 1));
      add(checkBox = new XCheckBox());
      add(label = new XLabel());
      checkBox.setBorder(new EmptyBorder());
      setBorder(new EmptyBorder());
      setFocusCycleRoot(true);
    }

    void setColors(Color fg, Color bg) {
      checkBox.setBackground(null);
      checkBox.setForeground(null);
      label.setBackground(null);
      label.setForeground(null);
      setBackground(bg);
      setForeground(fg);
      setOpaque(true);
    }
  }

  class ActionTreeNodeRenderer extends DefaultTreeCellRenderer {
    ActionTreeCellRendererComponent atcrc = new ActionTreeCellRendererComponent();

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean focused) {
      Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);

      if (value instanceof ActionTreeNode) {
        ActionTreeNode actionNode = (ActionTreeNode) value;
        atcrc.checkBox.setSelected(actionNode.isSelected());
        atcrc.label.setText(actionNode.toString());
        Color bg = sel ? getBackgroundSelectionColor() : getBackgroundNonSelectionColor();
        Color fg = sel ? getTextSelectionColor() : getTextNonSelectionColor();
        atcrc.setColors(fg, bg);
        comp = atcrc;
      }

      return comp;
    }
  }

  class ActionSelectionHandler implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      TreePath treePath = fActionTree.getEditingPath();
      ActionTreeNode actionNode = (ActionTreeNode) treePath.getLastPathComponent();
      if (actionNode != null) {
        XCheckBox checkBox = (XCheckBox) e.getSource();
        actionNode.setSelected(checkBox.isSelected());

        AbstractResolutionAction action = actionNode.getAction();
        if (action != null && action.isSelected()) {
          action.showControl(NonPortableObjectPanel.this);
        } else {
          hideActionPanel();
        }
        fApplyButton.setEnabled(anySelectedActions());
      }
    }
  }

  class CellEditor extends XCellEditor {
    ActionTreeCellRendererComponent atcrc;

    CellEditor() {
      super(new XTextField());
      atcrc = new ActionTreeCellRendererComponent();
      m_editorComponent = atcrc;
      atcrc.checkBox.addActionListener(new ActionSelectionHandler());
    }

    public boolean isCellEditable(EventObject event) {
      return true;
    }

    protected void fireEditingStopped() {/**/
    }

    public boolean shouldSelectCell(EventObject event) {
      if (event instanceof MouseEvent) {
        MouseEvent me = (MouseEvent) event;
        Point p = SwingUtilities.convertPoint(fActionTree, new Point(me.getX(), me.getY()), atcrc);
        Component activeComponent = SwingUtilities.getDeepestComponentAt(atcrc, p.x, p.y);

        if (activeComponent instanceof XCheckBox) {
          TreePath path = fActionTree.getPathForLocation(me.getX(), me.getY());
          if (path != null) {
            ActionTreeNode node = (ActionTreeNode) path.getLastPathComponent();
            node.setSelected(!node.isSelected());
            atcrc.checkBox.setSelected(node.isSelected());
            fApplyButton.setEnabled(anySelectedActions());
          }
        }
      }
      return true;
    }

    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
                                                boolean leaf, int row) {
      Component comp = super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);

      isSelected = true;
      if (value instanceof ActionTreeNode) {
        ActionTreeNode actionNode = (ActionTreeNode) value;
        atcrc.checkBox.setSelected(actionNode.isSelected());
        atcrc.label.setText(actionNode.toString());
        Color fg = isSelected ? UIManager.getColor("Tree.selectionForeground") : UIManager
            .getColor("Tree.textForeground");
        Color bg = isSelected ? UIManager.getColor("Tree.selectionBackground") : UIManager
            .getColor("Tree.textBackground");
        atcrc.setColors(fg, bg);
        comp = atcrc;
      }

      return comp;
    }
  }

  class ActionTreeNodeEditor extends DefaultTreeCellEditor {
    ActionTreeNodeEditor() {
      super(fActionTree, new ActionTreeNodeRenderer(), new CellEditor());
    }

    protected boolean canEditImmediately(EventObject event) {
      return true;
    }

    protected void determineOffset(JTree theTree, Object value, boolean isSelected, boolean expanded, boolean leaf,
                                   int row) {
      editingIcon = null;
      offset = 0;
    }
  }

  private boolean requiresPortabilityAction(NonPortableWorkState workState) {
    if(workState.isTransient() || workState.extendsLogicallyManagedType()) return false;
    if(workState.hasRequiredBootTypes()) {
      java.util.List types = workState.getRequiredBootTypes();
      for(Iterator iter = types.iterator(); iter.hasNext();) {
        if(!fConfigHelper.isBootJarClass((String)iter.next())) return true;
      }
    }
    if(workState.hasNonPortableBaseTypes()) {
      java.util.List types = workState.getNonPortableBaseTypes();
      for(Iterator iter = types.iterator(); iter.hasNext();) {
        if(!fConfigHelper.isAdaptable((String)iter.next())) return true;
      }
    }
    return !fConfigHelper.isAdaptable(workState.getTypeName());
  }
  
  AbstractResolutionAction[] createActions(AbstractWorkState workState) {
    ArrayList<AbstractResolutionAction> list = new ArrayList<AbstractResolutionAction>();
    
    if(workState instanceof NonPortableWorkState) {
      NonPortableWorkState nonPortableWorkState = (NonPortableWorkState)workState;
      String fieldName = nonPortableWorkState.getFieldName();
  
      if (nonPortableWorkState.isNeverPortable() || nonPortableWorkState.isPortable()) {
        if (fieldName != null && !nonPortableWorkState.isTransient() && !fConfigHelper.isTransient(fieldName)) {
          list.add(new MakeTransientAction(nonPortableWorkState));
        }
      } else if (!nonPortableWorkState.isPortable()) {
        if(requiresPortabilityAction(nonPortableWorkState)) {
          list.add(new MakePortableAction(nonPortableWorkState));
        }
        if (fieldName != null && !nonPortableWorkState.isTransient() && !fConfigHelper.isTransient(fieldName)) {
          list.add(new MakeTransientAction(nonPortableWorkState));
        }
      }
    }

    return list.toArray(new AbstractResolutionAction[0]);
  }

  AbstractResolutionAction[] getActions(AbstractWorkState state) {
    AbstractResolutionAction[] actions = state.getActions();

    if (actions == null) {
      state.setActions(actions = createActions(state));
    }

    return actions;
  }

  void createActionNodes(AbstractWorkState state) {
    ActionTreeRootNode actionTreeRoot = (ActionTreeRootNode) fActionTree.getModel().getRoot();
    AbstractResolutionAction[] actions = getActions(state);
    AbstractResolutionAction action;

    actionTreeRoot.fSelected = true;
    if (actions != null && actions.length > 0) {
      for (int i = 0; i < actions.length; i++) {
        action = actions[i];

        if (action.isEnabled()) {
          actionTreeRoot.add(new ActionTreeNode(action));
          if (action.isSelected()) {
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

  private boolean anySelectedActions() {
    return selectedActions().hasNext();
  }

  private Iterator selectedActions() {
    ArrayList<AbstractResolutionAction> list = new ArrayList<AbstractResolutionAction>();

    for (int i = 0; i < fIssueList.getModel().getSize(); i++) {
      IssueListItem item = (IssueListItem) fIssueList.getModel().getElementAt(i);
      DefaultMutableTreeNode node = item.fTreeNode;
      AbstractWorkState workState = (AbstractWorkState) node.getUserObject();
      AbstractResolutionAction[] actions = workState.getActions();

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

  class ActionTreeSelectionHandler implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      ActionTreeNode actionNode = (ActionTreeNode) fActionTree.getLastSelectedPathComponent();
      if (actionNode != null) {
        AbstractResolutionAction action = actionNode.getAction();

        if (action != null && action.isSelected()) {
          action.showControl(NonPortableObjectPanel.this);
        } else {
          hideActionPanel();
        }
      }
    }
  }

  void setIncludeTypes(java.util.List<String> types) {
    fIncludeTypesList.setListData(types.toArray(new String[0]));
  }
  
  void setBootTypes(java.util.List<String> types) {
    fBootTypesList.setListData(types.toArray(new String[0]));
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
    ArrayList<DefaultMutableTreeNode> fRemovedChildNodes;

    MakeTransientAction(NonPortableWorkState workState) {
      super(workState);
    }

    public void showControl(Object parentControl) {
      NonPortableObjectPanel parent = (NonPortableObjectPanel) parentControl;
      String fieldName = fWorkState.getFieldName();
      String declaringType = fieldName.substring(0, fieldName.lastIndexOf('.'));
      Include include = fConfigHelper.ensureIncludeRuleFor(declaringType);

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
        if (fRemovedChildNodes == null) fRemovedChildNodes = new ArrayList<DefaultMutableTreeNode>();
        for (int i = 0; i < treeItem.getChildCount(); i++) {
          fRemovedChildNodes.add((DefaultMutableTreeNode)treeItem.getChildAt(i));
        }
        treeItem.removeAllChildren();
      } else {
        fConfigHelper.ensureNotTransient(fWorkState.getFieldName());

        for (int i = 0; i < fRemovedChildNodes.size(); i++) {
          treeItem.add(fRemovedChildNodes.get(i));
        }
        fRemovedChildNodes.clear();
      }
      ((DefaultTreeModel) fObjectTree.getModel()).reload(treeItem);

      initIssueList();
      fObjectTree.setSelectionPath(new TreePath(treeItem.getPath()));
      fIssueList.setSelectedIndex(index);
      resetSearchButtons();
    }
  }

  class MakePortableAction extends NonPortableResolutionAction {
    MakePortableAction(NonPortableWorkState workState) {
      super(workState);
    }

    public void showControl(Object parentControl) {
      NonPortableObjectPanel parent = (NonPortableObjectPanel) parentControl;
      parent.setIncludeTypes(fWorkState.getRequiredIncludeTypes());
      parent.setBootTypes(fWorkState.getRequiredBootTypes());
      parent.fActionPanel.setPage("IncludeTypesPage");
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
            fConfigHelper.ensureBootJarClass((String)iter.next());
          }
        } else {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureNotBootJarClass((String)iter.next());
          }
        }
      }

      if (fWorkState.hasRequiredIncludeTypes()) {
        java.util.List types = fWorkState.getRequiredIncludeTypes();
        if (selected) {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureAdaptable((String)iter.next());
          }
        } else {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureNotAdaptable((String)iter.next());
          }
        }
      }

      int index = fIssueList.getSelectedIndex();
      initIssueList();
      fIssueList.setSelectedIndex(index);
      resetSearchButtons();
    }
  }
}
