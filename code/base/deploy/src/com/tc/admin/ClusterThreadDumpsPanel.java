/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.ScrollPane;

import com.tc.admin.common.FastFileChooser;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class ClusterThreadDumpsPanel extends XContainer {
  private AdminClientContext     m_acc;
  private ClusterThreadDumpsNode m_clusterThreadDumpsNode;
  private Button                 m_threadDumpButton;
  private XTree                  m_threadDumpTree;
  private XTreeModel             m_threadDumpTreeModel;
  private XTextArea              m_threadDumpTextArea;
  private ScrollPane             m_threadDumpTextScroller;
  private ThreadDumpTreeNode     m_lastSelectedThreadDumpTreeNode;
  private Button                 m_exportButton;
  private File                   m_lastExportDir;
  private DeleteAction           m_deleteAction;
  private DeleteAllAction        m_deleteAllAction;
  private ExportAsTextAction     m_exportAsTextAction;

  private static final String    DELETE_ITEM_CMD                 = "DeleteItemCmd";
  private static final String    DEFAULT_EXPORT_ARCHIVE_FILENAME = "tc-cluster-thread-dumps.zip";

  public ClusterThreadDumpsPanel(ClusterThreadDumpsNode clusterThreadDumpsNode) {
    super();

    m_acc = AdminClient.getContext();
    m_clusterThreadDumpsNode = clusterThreadDumpsNode;

    load((ContainerResource) m_acc.getComponent("ThreadDumpsPanel"));

    XSplitPane splitter = (XSplitPane) findComponent("ThreadDumpsSplitter");
    splitter.setPreferences(getPreferences().node(splitter.getName()));

    m_threadDumpButton = (Button) findComponent("TakeThreadDumpButton");
    m_threadDumpButton.addActionListener(new ThreadDumpButtonHandler());

    ScrollPane itemScroller = (ScrollPane) findComponent("ThreadDumpItemScroller");
    itemScroller.setItem(m_threadDumpTree = new XTree());
    m_threadDumpTree.getSelectionModel().addTreeSelectionListener(new ThreadDumpTreeSelectionListener());
    m_threadDumpTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    m_threadDumpTree.setModel(m_threadDumpTreeModel = new XTreeModel());
    m_threadDumpTree.setShowsRootHandles(true);
    m_threadDumpTree.setPopupMenu(createTreePopup());

    m_threadDumpTextArea = (XTextArea) findComponent("ThreadDumpTextArea");
    JPopupMenu textAreaPopup = m_threadDumpTextArea.createPopup();
    textAreaPopup.add(m_exportAsTextAction);
    m_threadDumpTextArea.setPopupMenu(textAreaPopup);
    m_threadDumpTextScroller = (ScrollPane) findComponent("ThreadDumpTextScroller");

    m_exportButton = (Button) findComponent("ExportButton");
    m_exportButton.addActionListener(new ExportAsArchiveHandler());

    ((SearchPanel) findComponent("SearchPanel")).setTextComponent(m_threadDumpTextArea);

    m_threadDumpTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE_ITEM_CMD);
    m_threadDumpTree.getActionMap().put(DELETE_ITEM_CMD, m_deleteAction);
  }

  private JPopupMenu createTreePopup() {
    JPopupMenu popup = new JPopupMenu();
    popup.add(m_deleteAction = new DeleteAction());
    popup.add(m_deleteAllAction = new DeleteAllAction());
    popup.add(m_exportAsTextAction = new ExportAsTextAction());
    return popup;
  }

  private class DeleteAction extends XAbstractAction {
    private DeleteAction() {
      super("Delete");
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      ThreadDumpTreeNode tdtn = (ThreadDumpTreeNode) m_threadDumpTree.getLastSelectedPathComponent();
      m_threadDumpTreeModel.removeNodeFromParent(tdtn);
    }
  }

  private class DeleteAllAction extends XAbstractAction {
    private DeleteAllAction() {
      super("Delete All");
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      XRootNode root = (XRootNode) m_threadDumpTreeModel.getRoot();
      root.removeAllChildren();
      m_threadDumpTreeModel.nodeStructureChanged(root);
    }
  }

  private class ExportAsTextAction extends XAbstractAction {
    private ExportAsTextAction() {
      super("Export as text...");
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      try {
        exportAsText();
      } catch (Exception e) {
        m_acc.log(e);
      }
    }
  }

  protected Preferences getPreferences() {
    return m_acc.getPrefs().node(ClusterThreadDumpsPanel.class.getName());
  }

  private class ThreadDumpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      ClusterThreadDumpEntry tde = m_clusterThreadDumpsNode.takeThreadDump();
      XTreeNode root = (XTreeNode) m_threadDumpTreeModel.getRoot();

      m_threadDumpTreeModel.insertNodeInto(tde, root, root.getChildCount());
      TreePath treePath = new TreePath(tde.getPath());
      m_threadDumpTree.expandPath(treePath);
      m_threadDumpTree.setSelectionPath(treePath);
      m_exportButton.setEnabled(true);
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
      setControlsEnabled(tdtn != null);
    }
  }

  private void setControlsEnabled(boolean haveSelection) {
    m_exportButton.setEnabled(haveSelection);
    m_deleteAction.setEnabled(haveSelection);
    m_deleteAllAction.setEnabled(haveSelection);
    m_exportAsTextAction.setEnabled(haveSelection);
    if (!haveSelection) {
      m_threadDumpTextArea.setText("");
    }
  }

  private void exportAsArchive() throws Exception {
    FastFileChooser chooser = new FastFileChooser();
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

  private class ExportAsArchiveHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        exportAsArchive();
      } catch (Exception e) {
        m_acc.log(e);
      }
    }
  }

  private void exportAsText() throws Exception {
    FastFileChooser chooser = new FastFileChooser();
    if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
    chooser.setDialogTitle("Export thread dump as text");
    chooser.setMultiSelectionEnabled(false);
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "thread-dump.txt"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    FileOutputStream fos = new FileOutputStream(file);
    m_lastExportDir = file.getParentFile();
    fos.write(m_lastSelectedThreadDumpTreeNode.getContent().getBytes("UTF-8"));
    fos.close();
  }
}
