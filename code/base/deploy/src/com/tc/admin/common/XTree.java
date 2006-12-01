/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class XTree extends org.dijon.Tree {
  protected JPopupMenu m_popupMenu;
  
  public XTree() {
    super();

    setShowsRootHandles(true);
    setRootVisible(false);
    setCellRenderer(new XTreeCellRendererDelegate());

    MouseListener ml = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        testPopup(e);
      }

      public void mouseReleased(MouseEvent e) {
        testPopup(e);
      }

      public void testPopup(MouseEvent e) {
        if(e.isPopupTrigger()) {
          TreePath   path = getPathForLocation(e.getX(), e.getY());
          XTreeNode  node = path != null ? (XTreeNode)path.getLastPathComponent() : null;
          JPopupMenu menu = node != null ? node.getPopupMenu() : XTree.this.getPopupMenu();

          if(menu != null) {
            menu.show(XTree.this, e.getX(), e.getY());
          }
        }
      }
    };
    addMouseListener(ml);
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    if(popupMenu != null) {
      add(m_popupMenu = popupMenu);
    }
  }
  
  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }
  
  private Action getAction(KeyStroke ks) {
    Action    result = null;
    XTreeNode node   = (XTreeNode)getLastSelectedPathComponent();

    while(node != null) {
      ActionMap actionMap = node.getActionMap();

      if(actionMap != null) {
        InputMap inputMap = node.getInputMap();

        if(inputMap != null) {
          Object binding = inputMap.get(ks);

          if(binding != null) {
            if((result = actionMap.get(binding)) != null) {
              return result;
            }
          }
        }
      }

      node = (XTreeNode)node.getParent();
    }

    return null;
  }

  protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                                      int condition, boolean pressed)
  {
    Action action = getAction(ks);

    if(action != null) {
      return SwingUtilities.notifyAction(action, ks, e, this, e.getModifiers());
    }
    else {
      return super.processKeyBinding(ks, e, condition, pressed);
    }
  }

  public void setModel(TreeModel model) {
    super.setModel(model);
    selectTop();
  }

  public void selectTop() {
    if(getRowCount() > 0) {
      setSelectionRow(0);
      expandTop();
    }
  }

  public void expandTop() {
    expandRow(0);
  }

  public void expandAll() {
    XRootNode root = (XRootNode)((XTreeModel)getModel()).getRoot();
    XTreeNode node = (XTreeNode)root.getFirstLeaf();
    XTreeNode parent;

    while(node != null) {
      parent = (XTreeNode)node.getParent();
      
      if(parent != null) {
        expandPath(new TreePath(parent.getPath()));
      }

      node = (XTreeNode)node.getNextLeaf();
    }

    revalidate();
    repaint();
  }
}
