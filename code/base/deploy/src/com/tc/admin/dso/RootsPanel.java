/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeNode;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public class RootsPanel extends XContainer {
  private ConnectionContext m_cc;
  private XTree             m_tree;

  public RootsPanel(ConnectionContext cc, DSORoot[] roots) {
    super(new BorderLayout());

    m_cc   = cc;
    m_tree = new XTree();
    m_tree.setShowsRootHandles(true);
    m_tree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        TreePath  path = m_tree.getPathForLocation(me.getX(), me.getY());

        if(path != null) {
          m_tree.requestFocus();

          XTreeNode node = (XTreeNode)path.getLastPathComponent();
          if(node != null) {
            node.nodeClicked(me);
          }
        }
      }
    });
    m_tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent tse) {
        TreePath  path = tse.getNewLeadSelectionPath();
        XTreeNode node;

        if(path != null) {
          m_tree.requestFocus();
          
          node = (XTreeNode)path.getLastPathComponent();
          if(node != null) {
            node.nodeSelected(tse);
          }
        }
      }
    });
    add(new JScrollPane(m_tree), BorderLayout.CENTER);
    setRoots(roots);
  }

  public void setRoots(DSORoot[] roots) {
    m_tree.setModel(new RootTreeModel(m_cc, roots));
    m_tree.revalidate();
    m_tree.repaint();
  }

  public void refresh() {
    ((RootTreeModel)m_tree.getModel()).refresh();
  }

  public void add(DSORoot root) {
    ((RootTreeModel)m_tree.getModel()).add(root);
    m_tree.revalidate();
    m_tree.repaint();
  }
}
