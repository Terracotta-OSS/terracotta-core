/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.ScrollPane;
import org.dijon.TextArea;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class ClusterThreadDumpsPanel extends XContainer {
  private AdminClientContext     m_acc;
  private ClusterThreadDumpsNode m_clusterThreadDumpsNode;

  private Button                 m_threadDumpButton;
  private XTree                  m_threadDumpTree;
  private XTreeModel             m_threadDumpTreeModel;
  private TextArea               m_threadDumpTextArea;
  private ScrollPane             m_threadDumpTextScroller;
  private ThreadDumpTreeNode     m_lastSelectedThreadDumpTreeNode;

  public ClusterThreadDumpsPanel(ClusterThreadDumpsNode clusterThreadDumpsNode) {
    super();

    m_acc = AdminClient.getContext();
    m_clusterThreadDumpsNode = clusterThreadDumpsNode;

    load((ContainerResource) m_acc.topRes.getComponent("ClusterThreadDumpsPanel"));

    m_threadDumpButton = (Button) findComponent("TakeThreadDumpButton");
    m_threadDumpButton.addActionListener(new ThreadDumpButtonHandler());

    m_threadDumpTree = (XTree) findComponent("ThreadDumpTree");
    m_threadDumpTree.getSelectionModel().addTreeSelectionListener(new ThreadDumpTreeSelectionListener());

    m_threadDumpTree.setModel(m_threadDumpTreeModel = new XTreeModel());
    m_threadDumpTree.setShowsRootHandles(true);

    m_threadDumpTextArea = (TextArea) findComponent("ThreadDumpTextArea");
    m_threadDumpTextScroller = (ScrollPane) findComponent("ThreadDumpTextScroller");
  }

  private class ThreadDumpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        ClusterThreadDumpEntry tde = m_clusterThreadDumpsNode.takeThreadDump();
        XTreeNode root = (XTreeNode) m_threadDumpTreeModel.getRoot();
        int index = root.getChildCount();

        root.add(tde);
        // TODO: the following is daft; nodesWereInserted is all that should be needed but for some
        // reason the first node requires nodeStructureChanged on the root; why? I don't know.
        m_threadDumpTreeModel.nodesWereInserted(root, new int[] { index });
        m_threadDumpTreeModel.nodeStructureChanged(root);
      } catch (Exception e) {
        m_acc.log(e);
      }
    }
  }

  private class ThreadDumpTreeSelectionListener implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      if (m_lastSelectedThreadDumpTreeNode != null) {
        m_lastSelectedThreadDumpTreeNode.setViewPosition(m_threadDumpTextScroller.getViewport().getViewPosition());
      }
      ThreadDumpTreeNode tdtn = (ThreadDumpTreeNode) m_threadDumpTree.getLastSelectedPathComponent();
      if (tdtn != null) {
        m_threadDumpTextArea.setText(tdtn.getContent());
        final Point viewPosition = tdtn.getViewPosition();
        if (viewPosition != null) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              m_threadDumpTextScroller.getViewport().setViewPosition(viewPosition);
            }
          });
        }
      }
      m_lastSelectedThreadDumpTreeNode = tdtn;
    }
  }
}
