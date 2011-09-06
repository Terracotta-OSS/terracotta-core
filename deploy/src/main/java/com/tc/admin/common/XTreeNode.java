/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;

public class XTreeNode extends DefaultMutableTreeNode {
  private String             name;
  private TreeCellRenderer   renderer;
  private Icon               icon;
  private boolean            enabled                = true;
  private ActionMap          actionMap;
  private InputMap           inputMap;

  protected static final int MENU_SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  public XTreeNode() {
    super();
  }

  public XTreeNode(Object userObject) {
    super(userObject);
  }

  public TreeCellRenderer getRenderer() {
    return renderer;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setRenderer(TreeCellRenderer renderer) {
    this.renderer = renderer;
  }

  public Icon getIcon() {
    return icon;
  }

  public void setIcon(Icon icon) {
    this.icon = icon;
  }

  public JPopupMenu getPopupMenu() {
    return null;
  }

  public void addChild(XTreeNode child) {
    insertChild(child, getChildCount());
  }

  public void insertChild(XTreeNode child, int index) {
    XTreeModel model = getModel();
    if (model != null) {
      model.insertNodeInto(child, this, index);
    } else {
      insert(child, index);
    }
  }

  public void removeChild(XTreeNode child) {
    XTreeModel model = getModel();
    if (model != null) {
      model.removeNodeFromParent(child);
    } else {
      remove(child);
    }
  }

  public XTreeNode findNodeByName(String theName) {
    String key = getName();
    if (key == null) {
      key = getClass().getSimpleName();
    }
    if (theName.equals(key)) { return this; }
    Enumeration e = children();
    while (e.hasMoreElements()) {
      XTreeNode child = (XTreeNode) e.nextElement();
      if ((child = child.findNodeByName(theName)) != null) { return child; }
    }
    return null;
  }

  public void tearDownChildren() {
    if (children != null) {
      XTreeNode node;
      for (int i = getChildCount() - 1; i >= 0; i--) {
        if ((node = (XTreeNode) children.get(i)) != null) {
          node.removeFromParent();
          node.tearDown();
        }
      }
    }
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;

    Enumeration e = children();
    while (e.hasMoreElements()) {
      XTreeNode child = (XTreeNode) e.nextElement();
      child.setEnabled(enabled);
    }

    nodeChanged();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void tearDown() {
    tearDownChildren();
    setUserObject(null);
    setIcon(null);
  }

  public ActionMap getActionMap() {
    return actionMap;
  }

  public ActionMap ensureActionMap() {
    if (actionMap == null) {
      actionMap = new ActionMap();
    }
    return actionMap;
  }

  public void setActionMap(ActionMap actionMap) {
    this.actionMap = actionMap;
  }

  public InputMap getInputMap() {
    return inputMap;
  }

  public InputMap ensureInputMap() {
    if (inputMap == null) {
      inputMap = new InputMap();
    }

    return inputMap;
  }

  public void setInputMap(InputMap inputMap) {
    this.inputMap = inputMap;
  }

  public void addActionBinding(Object binding, XAbstractAction action) {
    ensureActionMap().put(binding, action);
    KeyStroke ks = action.getAccelerator();
    if (ks != null) {
      ensureInputMap().put(ks, binding);
    }
  }

  public XTreeModel getModel() {
    TreeNode root = getRoot();
    XTreeModel model = null;
    if (root instanceof XRootNode) {
      model = ((XRootNode) getRoot()).getModel();
    }
    return model;
  }

  public int getIndex() {
    XTreeNode parentNode = (XTreeNode) getParent();
    return (parentNode != null) ? parentNode.getIndex(this) : -1;
  }

  public void nodeSelected(TreeSelectionEvent e) {/**/
  }

  public void nodeClicked(MouseEvent me) {/**/
  }

  public void nodeChanged() {
    DefaultTreeModel model = getModel();
    if (model != null) {
      model.nodeChanged(this);
    }
  }

  public void nodeStructureChanged() {
    DefaultTreeModel model = getModel();
    if (model != null) {
      model.nodeStructureChanged(this);
    }
  }
}
