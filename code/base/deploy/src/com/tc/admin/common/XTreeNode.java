/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Toolkit;
import java.awt.event.MouseEvent;

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
  private TreeCellRenderer m_renderer;
  private Icon             m_icon;
  private ActionMap        m_actionMap;
  private InputMap         m_inputMap;

  protected static final int MENU_SHORTCUT_KEY_MASK =
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  public XTreeNode() {
    super();
  }

  public XTreeNode(Object userObject) {
    super(userObject);
  }

  public TreeCellRenderer getRenderer() {
    return m_renderer;
  }
  
  public void setRenderer(TreeCellRenderer renderer) {
    m_renderer = renderer;
  }
  
  public Icon getIcon() {
    return m_icon;
  }

  public void setIcon(Icon icon) {
    m_icon = icon;
  }

  public JPopupMenu getPopupMenu() {
    return null;
  }

  public void tearDownChildren() {
    if(children != null) {
      XTreeNode node;

      for(int i = getChildCount()-1; i >= 0; i--) {
        if((node = (XTreeNode)children.get(i)) != null) {
          node.tearDown();
          node.removeFromParent();
        }
      }
    }
  }

  public void tearDown() {
    tearDownChildren();
    setUserObject(null);
    setIcon(null);
  }

  public ActionMap getActionMap() {
    return m_actionMap;
  }

  public ActionMap ensureActionMap() {
    if(m_actionMap == null) {
      m_actionMap = new ActionMap();
    }

    return m_actionMap;
  }

  public void setActionMap(ActionMap actionMap) {
    m_actionMap = actionMap;
  }

  public InputMap getInputMap() {
    return m_inputMap;
  }

  public InputMap ensureInputMap() {
    if(m_inputMap == null) {
      m_inputMap = new InputMap();
    }

    return m_inputMap;
  }

  public void setInputMap(InputMap inputMap) {
    m_inputMap = inputMap;
  }

  public void addActionBinding(Object binding, XAbstractAction action) {
    ensureActionMap().put(binding, action);

    KeyStroke ks = action.getAccelerator();
    if(ks != null) {
      ensureInputMap().put(ks, binding);
    }
  }

  public XTreeModel getModel() {
    TreeNode   root  = getRoot();
    XTreeModel model = null;
    
    if(root instanceof XRootNode) {
      model = ((XRootNode)getRoot()).getModel();
    }
    
    return model;
  }

  public int getIndex() {
    XTreeNode parentNode = (XTreeNode)getParent();
    return (parentNode != null) ? parentNode.getIndex(this) : -1;
  }

  public void nodeSelected(TreeSelectionEvent e) {/**/}
  public void nodeClicked(MouseEvent me) {/**/}
  
  protected void nodeChanged() {
    DefaultTreeModel model = getModel();
    if(model != null) {
      model.nodeChanged(this);
    }
  }
  
  protected void nodeStructureChanged() {
    DefaultTreeModel model = getModel();
    if(model != null) {
      model.nodeStructureChanged(this);
    }
  }
}
