/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.AbstractResolutionAction;
import com.tc.admin.common.AbstractWorkState;
import com.tc.admin.common.NonPortableMessages;
import com.tc.admin.common.NonPortableResolutionAction;
import com.tc.admin.common.NonPortableWalkNode;
import com.tc.admin.common.NonPortableWorkState;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCellEditor;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XList;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTextField;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeCellRenderer;
import com.tc.admin.common.XTreeNode;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.NonPortableObjectState;
import com.terracottatech.config.Include;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Iterator;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSplitPane;
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
  private NonPortableObjectEvent     fEvent;
  private SessionIntegratorFrame     fMainFrame;
  private XLabel                     fMessageLabel;
  private XSplitPane                 fIssuesSplitter;
  private XList                      fIssueList;
  private DefaultListModel           fIssueListModel;
  private IssueListSelectionHandler  fIssueListSelectionHandler;
  private XTree                      fObjectTree;
  private XContainer                 fIssueDetailsPanel;
  private XLabel                     fSummaryLabel;
  private XTextArea                  fDescriptionText;
  private XContainer                 fResolutionsPanel;
  private XTree                      fActionTree;
  private ActionTreeSelectionHandler fActionTreeSelectionHandler;
  private PagedView                  fActionPanel;
  private OnLoadPanel                fOnLoadPanel;

  private XList                      fIncludeTypesList;
  private XList                      fBootTypesList;

  private XButton                    fPreviousIssueButton;
  private XButton                    fNextIssueButton;
  private XButton                    fApplyButton;
  private XButton                    fCancelButton;
  private ConfigHelper               fConfigHelper;
  private TcConfig                   fNewConfig;

  private static final ImageIcon     NOT_PORTABLE_ICON     = new ImageIcon(
                                                                           NonPortableWalkNode.class
                                                                               .getResource("/com/tc/admin/icons/field_private_obj.gif"));
  private static final ImageIcon     NEVER_PORTABLE_ICON   = new ImageIcon(
                                                                           NonPortableWalkNode.class
                                                                               .getResource("/com/tc/admin/icons/field_private_obj.gif"));
  private static final ImageIcon     TRANSIENT_ICON        = new ImageIcon(NonPortableWalkNode.class
                                                               .getResource("/com/tc/admin/icons/field_public_obj.gif"));
  private static final ImageIcon     PORTABLE_ICON         = new ImageIcon(
                                                                           NonPortableWalkNode.class
                                                                               .getResource("/com/tc/admin/icons/field_default_obj.gif"));
  private static final ImageIcon     PRE_INSTRUMENTED_ICON = new ImageIcon(
                                                                           NonPortableWalkNode.class
                                                                               .getResource("/com/tc/admin/icons/field_protected_obj.gif"));
  private static final ImageIcon     OBJ_CYCLE_ICON        = new ImageIcon(NonPortableWalkNode.class
                                                               .getResource("/com/tc/admin/icons/obj_cycle.gif"));
  private static final ImageIcon     RESOLVED_ICON         = new ImageIcon(NonPortableWalkNode.class
                                                               .getResource("/com/tc/admin/icons/installed_ovr.gif"));
  private static final ImageIcon     BLANK_ICON            = new ImageIcon(NonPortableWalkNode.class
                                                               .getResource("/com/tc/admin/icons/blank12x12.gif"));

  public NonPortableObjectPanel(SessionIntegratorFrame frame) {
    super(new GridBagLayout());

    fMainFrame = frame;
    fNewConfig = (TcConfig) frame.getConfigHelper().getConfig().copy();
    fConfigHelper = new ConfigCopyHelper();

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.NORTH;

    add(fMessageLabel = new XLabel(), gbc);
    gbc.gridy++;

    gbc.insets = new Insets(1, 1, 1, 1);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 1.0;

    fIssueList = new XList();
    fIssueList.setModel(fIssueListModel = new DefaultListModel());
    fIssueList.setCellRenderer(new IssueListCellRenderer());
    fIssueList.addListSelectionListener(fIssueListSelectionHandler = new IssueListSelectionHandler());

    fObjectTree = new XTree();
    fObjectTree.setCellRenderer(new ObjectTreeCellRenderer());
    ((DefaultTreeSelectionModel) fObjectTree.getSelectionModel())
        .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    fObjectTree.addTreeSelectionListener(this);

    Preferences prefs = frame.getPreferences().node(getClass().getSimpleName());
    fIssuesSplitter = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT, new XScrollPane(fIssueList),
                                     new XScrollPane(fObjectTree));
    fIssuesSplitter.setPreferences(prefs.node("IssuesSplitter"));
    fIssuesSplitter.setResizeWeight(0.5);

    XSplitPane mainSplitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, fIssuesSplitter, createIssueDetailsPanel());
    mainSplitter.setResizeWeight(0.5);
    mainSplitter.setPreferences(prefs.node("MainSplitter"));
    add(mainSplitter, gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    add(createButtonPanel(), gbc);
  }

  public XSplitPane getIssuesSplitter() {
    return fIssuesSplitter;
  }

  private JComponent createIssueDetailsPanel() {
    fIssueDetailsPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 1, 1, 1);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    fSummaryLabel = new XLabel();
    fSummaryLabel.setHorizontalAlignment(SwingConstants.LEFT);
    fIssueDetailsPanel.add(fSummaryLabel, gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    fDescriptionText = new XTextArea();
    fDescriptionText.setEditable(false);
    fDescriptionText.setRows(10);
    fDescriptionText.setColumns(40);
    fDescriptionText.setWrapStyleWord(true);
    fDescriptionText.setLineWrap(true);
    fDescriptionText.setFont(new Font("Dialog", Font.PLAIN, 10));
    XScrollPane descriptionScroller = new XScrollPane(fDescriptionText);
    fIssueDetailsPanel.add(descriptionScroller, gbc);
    gbc.gridy++;

    fIssueDetailsPanel.add(createResolutionsPanel(), gbc);

    fIssueDetailsPanel.setBorder(BorderFactory.createTitledBorder("Issue Details"));

    return fIssueDetailsPanel;
  }

  private JComponent createResolutionsPanel() {
    fResolutionsPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 1, 1, 1);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    fResolutionsPanel.add(new XLabel("Resolutions"), gbc);
    gbc.gridy++;

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    fActionTree = new XTree();
    fActionTreeSelectionHandler = new ActionTreeSelectionHandler();
    fActionTree.addTreeSelectionListener(fActionTreeSelectionHandler);
    fActionTree.setCellRenderer(new ActionTreeNodeRenderer());
    fActionTree.setCellEditor(new ActionTreeNodeEditor());
    fActionTree.setRootVisible(true);
    fActionTree.setEditable(true);
    ((DefaultTreeModel) fActionTree.getModel()).setRoot(new ActionTreeRootNode());
    fActionTree.setInvokesStopCellEditing(true);
    fResolutionsPanel.add(new XScrollPane(fActionTree), gbc);
    gbc.gridx++;

    fResolutionsPanel.add(createActionPanel(), gbc);

    return fResolutionsPanel;
  }

  private JComponent createActionPanel() {
    fActionPanel = new PagedView();

    XContainer emptyPage = new XContainer();
    emptyPage.setName("EmptyPage");
    fActionPanel.addPage(emptyPage);

    fOnLoadPanel = new OnLoadPanel();
    fOnLoadPanel.setName("IncludeRulePage");
    fActionPanel.addPage(fOnLoadPanel);

    XContainer includeTypesPanel = new XContainer(new GridLayout(0, 1));
    XContainer includePanel = new XContainer(new BorderLayout());
    includePanel.add(new XScrollPane(fIncludeTypesList = new XList()));
    fIncludeTypesList.setVisibleRowCount(3);
    includePanel.setBorder(BorderFactory.createTitledBorder("Types to be include for instrumentation:"));
    includeTypesPanel.add(includePanel);

    XContainer bootTypesPanel = new XContainer(new BorderLayout());
    bootTypesPanel.add(new XScrollPane(fBootTypesList = new XList()));
    fBootTypesList.setVisibleRowCount(3);
    bootTypesPanel.setBorder(BorderFactory.createTitledBorder("Types to add to BootJar:"));
    includeTypesPanel.add(bootTypesPanel);
    includeTypesPanel.setName("IncludeTypesPage");

    fActionPanel.addPage(includeTypesPanel);

    fActionPanel.setBorder(BorderFactory.createLineBorder(Color.blue));
    return fActionPanel;
  }

  private JComponent createButtonPanel() {
    XContainer buttonPanel = new XContainer(new BorderLayout());

    XContainer buttonGrid = new XContainer(new GridLayout(1, 0, 3, 1));

    buttonGrid.add(fPreviousIssueButton = new XButton("Previous Issue"));
    fPreviousIssueButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        gotoPreviousIssue();
      }
    });

    buttonGrid.add(fNextIssueButton = new XButton("Next Issue"));
    fNextIssueButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        gotoNextIssue();
      }
    });

    buttonGrid.add(fApplyButton = new XButton("Apply"));
    fApplyButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        apply();
      }
    });

    buttonGrid.add(fCancelButton = new XButton("Cancel"));
    fCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        cancel();
      }
    });

    buttonPanel.add(buttonGrid, BorderLayout.EAST);

    return buttonPanel;
  }

  class ConfigCopyHelper extends ConfigHelper {
    public TcConfig getConfig() {
      return fNewConfig;
    }
  }

  private void apply() {
    fActionTreeSelectionHandler.testApplySelectedAction();
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
    Dialog dialog = (Dialog) SwingUtilities.getAncestorOfClass(Dialog.class, this);
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
      JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);

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
    fSummaryLabel.setText("<html><p>" + workState.summary() + "</p></html>");
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

  class ActionTreeNode extends XTreeNode {
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
        if(action.isSelected() == selected) return;
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
      if(fSelected == selected) return;
      if ((fSelected = selected) == true) {
        for (int i = 0; i < getChildCount(); i++) {
          ((ActionTreeNode) getChildAt(i)).setSelected(false);
        }
      } else {
        if (getChildCount() > 0) {
          ((ActionTreeNode) getChildAt(0)).setSelected(true);
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

  static class ActionTreeCellRendererComponent extends XContainer {
    public XCheckBox checkBox;
    public XLabel    label;
    public Container container;

    public ActionTreeCellRendererComponent() {
      super();
      setLayout(new FlowLayout(SwingConstants.CENTER, 3, 1));
      add(checkBox = new XCheckBox());
      add(label = new XLabel());
      checkBox.setBorder(BorderFactory.createEmptyBorder());
      setBorder(BorderFactory.createEmptyBorder());
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
      editorComponent = atcrc;
      atcrc.checkBox.addActionListener(new ActionSelectionHandler());
    }

    public boolean isCellEditable(EventObject event) {
      return true;
    }

    protected void fireEditingStopped() {/**/
    }

    public boolean shouldSelectCell(final EventObject event) {
      if (event instanceof MouseEvent) {
        MouseEvent me = (MouseEvent) event;
        if (me.getID() != MouseEvent.MOUSE_RELEASED) return true;
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

  AbstractResolutionAction[] createActions(AbstractWorkState workState) {
    ArrayList<AbstractResolutionAction> list = new ArrayList<AbstractResolutionAction>();

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
            actionTreeRoot.fSelected = false;
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
    AbstractResolutionAction fLastAction;

    public void valueChanged(TreeSelectionEvent e) {
      if (fLastAction != null) {
        fLastAction.apply();
        fLastAction = null;
      }
      ActionTreeNode actionNode = (ActionTreeNode) fActionTree.getLastSelectedPathComponent();
      if (actionNode != null) {
        fLastAction = actionNode.getAction();
        if (fLastAction != null && fLastAction.isSelected()) {
          fLastAction.showControl(NonPortableObjectPanel.this);
        } else {
          hideActionPanel();
        }
      }
    }

    void testApplySelectedAction() {
      if (fLastAction != null) {
        fLastAction.apply();
      }
    }
  }

  void setIncludeTypes(java.util.List<String> types) {
    fIncludeTypesList.setListData(types.toArray(new String[0]));
  }

  void setBootTypes(java.util.List<String> types) {
    fBootTypesList.setListData(types.toArray(new String[0]));
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
        fOnLoadPanel.setInclude(include);
        parent.fActionPanel.setPage("IncludeRulePage");
      }
    }

    public String getText() {
      return NonPortableMessages.getString("DO_NOT_SHARE");
    }

    public void setSelected(boolean selected) {
      if (!fActionPanel.getPage().equals("IncludeRulePage")) {
        showControl(NonPortableObjectPanel.this);
      }

      super.setSelected(selected);

      int index = fIssueList.getSelectedIndex();
      DefaultMutableTreeNode treeItem = (DefaultMutableTreeNode) fObjectTree.getLastSelectedPathComponent();
      if (fRemovedChildNodes == null) fRemovedChildNodes = new ArrayList<DefaultMutableTreeNode>();
      if (selected) {
        fConfigHelper.ensureTransient(fWorkState.getFieldName());
        for (int i = 0; i < treeItem.getChildCount(); i++) {
          fRemovedChildNodes.add((DefaultMutableTreeNode) treeItem.getChildAt(i));
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
      fOnLoadPanel.updateInclude();
    }

    public void apply() {
      fOnLoadPanel.updateInclude();
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
      return NonPortableMessages.getString("MAKE_PORTABLE");
    }

    public void setSelected(boolean selected) {
      if (!fActionPanel.getPage().equals("IncludeTypesPage")) {
        showControl(NonPortableObjectPanel.this);
      }

      super.setSelected(selected);

      if (fWorkState.hasRequiredBootTypes()) {
        java.util.List types = fWorkState.getRequiredBootTypes();
        if (selected) {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureBootJarClass((String) iter.next());
          }
        } else {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureNotBootJarClass((String) iter.next());
          }
        }
      }

      if (fWorkState.hasRequiredIncludeTypes()) {
        java.util.List types = fWorkState.getRequiredIncludeTypes();
        if (selected) {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureAdaptable((String) iter.next());
          }
        } else {
          for (Iterator iter = types.iterator(); iter.hasNext();) {
            fConfigHelper.ensureNotAdaptable((String) iter.next());
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
