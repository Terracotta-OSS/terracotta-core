/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
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
  private Button                 m_exportButton;
  private File                   m_lastExportDir;

  private static final String    DEFAULT_EXPORT_ARCHIVE_FILENAME = "tc-cluster-thread-dumps.zip";

  public ClusterThreadDumpsPanel(ClusterThreadDumpsNode clusterThreadDumpsNode) {
    super();

    m_acc = AdminClient.getContext();
    m_clusterThreadDumpsNode = clusterThreadDumpsNode;

    load((ContainerResource) m_acc.getComponent("ClusterThreadDumpsPanel"));

    m_threadDumpButton = (Button) findComponent("TakeThreadDumpButton");
    m_threadDumpButton.addActionListener(new ThreadDumpButtonHandler());

    m_threadDumpTree = (XTree) findComponent("ThreadDumpTree");
    m_threadDumpTree.getSelectionModel().addTreeSelectionListener(new ThreadDumpTreeSelectionListener());

    m_threadDumpTree.setModel(m_threadDumpTreeModel = new XTreeModel());
    m_threadDumpTree.setShowsRootHandles(true);

    m_threadDumpTextArea = (TextArea) findComponent("ThreadDumpTextArea");
    m_threadDumpTextScroller = (ScrollPane) findComponent("ThreadDumpTextScroller");

    m_exportButton = (Button) findComponent("ExportButton");
    m_exportButton.addActionListener(new ExportHandler());
  }

  private class ThreadDumpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        ClusterThreadDumpEntry tde = m_clusterThreadDumpsNode.takeThreadDump();
        XTreeNode root = (XTreeNode) m_threadDumpTreeModel.getRoot();
        int count = root.getChildCount();

        root.add(tde);
        // TODO: the following is daft; nodesWereInserted is all that should be needed but for some
        // reason the first node requires nodeStructureChanged on the root; why? I don't know.
        m_threadDumpTreeModel.nodesWereInserted(root, new int[] { count });
        m_threadDumpTreeModel.nodeStructureChanged(root);
        m_exportButton.setEnabled(true);
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
      m_exportButton.setEnabled(tdtn != null);
    }
  }

  private void handleExport() throws Exception {
    JFileChooser chooser = new JFileChooser();
    if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
    chooser.setDialogTitle("Export thread dumps");
    chooser.setMultiSelectionEnabled(false);
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), DEFAULT_EXPORT_ARCHIVE_FILENAME));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    ZipOutputStream zipstream = new ZipOutputStream(new FileOutputStream(file));
    zipstream.setLevel(9);
    zipstream.setMethod(ZipOutputStream.DEFLATED);
    m_lastExportDir = file.getParentFile();
    XTreeNode root = (XTreeNode) m_threadDumpTreeModel.getRoot();
    int count = root.getChildCount();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'/'HH.mm.ss.SSSZ");
    for (int i = 0; i < count; i++) {
      ClusterThreadDumpEntry ctde = (ClusterThreadDumpEntry) root.getChildAt(i);
      String filenameBase = dateFormat.format(ctde.getTime()) + "/";
      int entryCount = ctde.getChildCount();
      for (int j = 0; j < entryCount; j++) {
        ThreadDumpElement tde = (ThreadDumpElement) ctde.getChildAt(j);
        ZipEntry zipentry = new ZipEntry(filenameBase + tde.toString().replace(':', '-'));
        zipstream.putNextEntry(zipentry);
        zipstream.write(tde.getThreadDump().getBytes("UTF-8"));
        zipstream.closeEntry();
      }
    }
    zipstream.close();
  }

  private class ExportHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        handleExport();
      } catch (Exception e) {
        m_acc.log(e);
      }
    }
  }
}
