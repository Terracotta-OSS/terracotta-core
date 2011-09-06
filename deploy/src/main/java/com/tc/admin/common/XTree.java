/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class XTree extends JTree implements TreeModelListener {
  protected JPopupMenu popupMenu;

  public XTree() {
    super();

    setShowsRootHandles(true);
    setRootVisible(false);
    setCellRenderer(new XTreeCellRendererDelegate());
    setVisibleRowCount(5);

    MouseListener ml = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path != null) {
          requestFocus();
          setSelectionPath(path);
        }
        testPopup(e);
      }

      @Override
      public void mouseReleased(MouseEvent me) {
        testPopup(me);
      }

      public void testPopup(MouseEvent me) {
        if (me.isPopupTrigger()) {
          TreePath path = getPathForLocation(me.getX(), me.getY());
          Object comp = path != null ? path.getLastPathComponent() : null;
          XTreeNode node = comp instanceof XTreeNode ? (XTreeNode) comp : null;
          JPopupMenu menu = null;

          if (node != null) menu = node.getPopupMenu();
          if (menu == null) menu = XTree.this.getPopupMenu();
          if (menu != null) {
            menu.show(XTree.this, me.getX(), me.getY());
          }
        }
      }

      @Override
      public void mouseClicked(MouseEvent me) {
        TreePath path = getPathForLocation(me.getX(), me.getY());
        if (path != null) {
          requestFocus();
          Object comp = path.getLastPathComponent();
          if (comp instanceof XTreeNode) {
            ((XTreeNode) comp).nodeClicked(me);
          }
        }
      }
    };
    addMouseListener(ml);

    addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent tse) {
        TreePath path = tse.getNewLeadSelectionPath();
        if (path != null) {
          requestFocus();
          Object comp = path.getLastPathComponent();
          if (comp instanceof XTreeNode) {
            ((XTreeNode) comp).nodeSelected(tse);
          }
        }
      }
    });
  }

  public XTree(TreeModel model) {
    this();
    setModel(model);
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    if (popupMenu != null) {
      add(this.popupMenu = popupMenu);
    }
  }

  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  private Action getAction(KeyStroke ks) {
    Action result = null;
    Object object = getLastSelectedPathComponent();

    if (object instanceof XTreeNode) {
      XTreeNode node = (XTreeNode) object;

      while (node != null) {
        ActionMap actionMap = node.getActionMap();
        if (actionMap != null) {
          InputMap inputMap = node.getInputMap();
          if (inputMap != null) {
            Object binding = inputMap.get(ks);
            if (binding != null) {
              if ((result = actionMap.get(binding)) != null) { return result; }
            }
          }
        }
        node = (XTreeNode) node.getParent();
      }
    }

    return null;
  }

  @Override
  protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
    Action action = getAction(ks);

    if (action != null) {
      return SwingUtilities.notifyAction(action, ks, e, this, e.getModifiers());
    } else {
      return super.processKeyBinding(ks, e, condition, pressed);
    }
  }

  @Override
  public void setModel(TreeModel model) {
    TreeModel oldModel = getModel();
    if (oldModel != null) {
      oldModel.removeTreeModelListener(this);
    }
    super.setModel(model);
    if (model != null) {
      model.addTreeModelListener(this);
      selectTop();
    }
  }

  public void selectTop() {
    if (getRowCount() > 0) {
      setSelectionRow(0);
      expandTop();
    }
  }

  public void expandTop() {
    expandRow(0);
  }

  public void expandAll() {
    expandAll(this, true);
  }

  public void collapseAll() {
    expandAll(this, false);
  }

  public static void expandAll(JTree tree, boolean expand) {
    TreeNode root = (TreeNode) tree.getModel().getRoot();
    expandAll(tree, new TreePath(root), expand);
  }

  public static void expandAll(JTree tree, TreePath parent, boolean expand) {
    TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      for (Enumeration e = node.children(); e.hasMoreElements();) {
        TreeNode n = (TreeNode) e.nextElement();
        TreePath path = parent.pathByAddingChild(n);
        expandAll(tree, path, expand);
      }
    }

    if (expand) {
      tree.expandPath(parent);
    } else {
      tree.collapsePath(parent);
    }
  }

  public Dimension getMaxItemSize() {
    Dimension result = new Dimension();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
    Enumeration e = root.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
      Component comp = getRendererComponent(new TreePath(node.getPath()));
      if (comp != null) {
        Dimension d = comp.getPreferredSize();
        result.width = Math.max(result.width, d.width);
        result.height = Math.max(result.height, d.height);
      }
    }
    return result;
  }

  public Component getRendererComponent(TreePath path) {
    if (isVisible(path)) {
      TreeCellRenderer r = getCellRenderer();
      if (r != null) {
        Object obj = path.getLastPathComponent();
        int row = getRowForPath(path);
        boolean selected = false;
        boolean expanded = true;
        boolean hasFocus = false;
        boolean isLeaf = getModel().isLeaf(obj);
        return r.getTreeCellRendererComponent(this, obj, selected, expanded, isLeaf, row, hasFocus);
      }
    }
    return null;
  }

  public void createOverlayListener() {
    /**/
  }

  public void treeNodesChanged(TreeModelEvent e) {
    /**/
  }

  public void treeNodesInserted(TreeModelEvent e) {
    /**/
  }

  public void treeNodesRemoved(TreeModelEvent e) {
    if (e == null) return;

    TreePath parentPath = e.getTreePath();
    Object[] children = e.getChildren();

    if (children == null) return;

    TreePath selPath = getSelectionPath();
    for (int i = 0; i < children.length; i++) {
      XTreeNode node = (XTreeNode) children[i];
      TreePath nodePath = parentPath.pathByAddingChild(node);

      if (nodePath.isDescendant(selPath)) {
        setSelectionPath(selPath = parentPath);
      }
      node.tearDown();
    }
  }

  public void treeStructureChanged(TreeModelEvent e) {
    /**/
  }
}
