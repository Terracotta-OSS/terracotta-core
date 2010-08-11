/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.FastFileChooser;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class ClusterThreadDumpsPanel extends BasicThreadDumpsPanel implements TreeModelListener {
  private ClusterThreadDumpProvider clusterThreadDumpProvider;
  private XTree                     threadDumpTree;
  private XTreeModel                threadDumpTreeModel;
  private ThreadDumpTreeNode        lastSelectedThreadDumpTreeNode;

  private static final String       DELETE_ITEM_CMD                 = "DeleteItemCmd";
  private static final String       DEFAULT_EXPORT_ARCHIVE_FILENAME = "tc-cluster-thread-dumps.zip";

  public ClusterThreadDumpsPanel(ApplicationContext appContext, ClusterThreadDumpProvider clusterThreadDumpProvider) {
    super(appContext);

    this.clusterThreadDumpProvider = clusterThreadDumpProvider;

    itemScroller.setViewportView(threadDumpTree = new XTree());
    threadDumpTree.getSelectionModel().addTreeSelectionListener(new ThreadDumpTreeSelectionListener());
    threadDumpTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    threadDumpTree.setModel(threadDumpTreeModel = new XTreeModel());
    threadDumpTree.setShowsRootHandles(true);
    threadDumpTreeModel.addTreeModelListener(this);
    threadDumpTree.setPopupMenu(createPopupMenu());

    threadDumpTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE_ITEM_CMD);
    threadDumpTree.getActionMap().put(DELETE_ITEM_CMD, deleteAction);
  }

  @Override
  protected void deleteSelected() {
    ThreadDumpTreeNode tdtn = (ThreadDumpTreeNode) threadDumpTree.getLastSelectedPathComponent();
    tdtn.cancel();
    threadDumpTreeModel.removeNodeFromParent(tdtn);
  }

  @Override
  protected void deleteAll() {
    XRootNode root = (XRootNode) threadDumpTreeModel.getRoot();
    for (int i = 0; i < root.getChildCount(); i++) {
      ClusterThreadDumpEntry ctde = (ClusterThreadDumpEntry) root.getChildAt(i);
      ctde.cancel();
    }
    root.removeAllChildren();
    threadDumpTreeModel.nodeStructureChanged(root);
  }

  public int getThreadDumpCount() {
    return ((XRootNode) threadDumpTreeModel.getRoot()).getChildCount();
  }

  private boolean haveAnyEntries() {
    return getThreadDumpCount() > 0;
  }

  private boolean isWaiting() {
    return threadDumpButton.getText().equals(appContext.getString("cancel"))
           || clusterDumpButton.getText().equals(appContext.getString("cancel"));
  }

  public ClusterThreadDumpEntry newEntry() {
    return clusterThreadDumpProvider.takeThreadDump();
  }

  private ClusterThreadDumpEntry newClusterDumpEntry() {
    return clusterThreadDumpProvider.takeClusterDump();
  }

  @Override
  public void takeThreadDump() {
    XTreeNode root = (XTreeNode) threadDumpTreeModel.getRoot();
    if (!isWaiting()) {
      exportButton.setEnabled(false);
      threadDumpButton.setText(appContext.getString("cancel"));

      ClusterThreadDumpEntry tde = newEntry();
      threadDumpTreeModel.insertNodeInto(tde, root, 0);
      threadDumpTree.setSelectionPath(new TreePath(tde.getPath()));
    } else {
      ClusterThreadDumpEntry tde = (ClusterThreadDumpEntry) root.getLastChild();
      tde.cancel();
    }
  }

  @Override
  protected void takeClusterDump() {
    XTreeNode root = (XTreeNode) threadDumpTreeModel.getRoot();
    if (!isWaiting()) {
      exportButton.setEnabled(false);
      clusterDumpButton.setText(appContext.getString("cancel"));

      ClusterThreadDumpEntry tde = newClusterDumpEntry();
      threadDumpTreeModel.insertNodeInto(tde, root, 0);
      threadDumpTree.setSelectionPath(new TreePath(tde.getPath()));
    } else {
      ClusterThreadDumpEntry tde = (ClusterThreadDumpEntry) root.getLastChild();
      tde.cancel();
    }

  }

  private class ThreadDumpTreeSelectionListener implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      if (tornDown.get()) { return; }
      ThreadDumpTreeNode tdtn = updateSelectedContent();
      lastSelectedThreadDumpTreeNode = tdtn;
      setControlsEnabled(tdtn != null);
    }
  }

  private static final Point ORIGIN = new Point();

  private ThreadDumpTreeNode updateSelectedContent() {
    if (lastSelectedThreadDumpTreeNode != null) {
      lastSelectedThreadDumpTreeNode.setViewPosition(textScroller.getViewport().getViewPosition());
    }
    ThreadDumpTreeNode tdtn = null;
    Object node = threadDumpTree.getLastSelectedPathComponent();
    if (node instanceof ThreadDumpTreeNode) {
      tdtn = (ThreadDumpTreeNode) node;
      textArea.setText(tdtn.getContent());
      final Point viewPosition = tdtn.getViewPosition();
      if (viewPosition != null && !viewPosition.equals(ORIGIN)) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            textScroller.getViewport().setViewPosition(viewPosition);
          }
        });
      }
    }
    if (tdtn == null) {
      textArea.setText("");
    }
    return tdtn;
  }

  private void setControlsEnabled(boolean haveSelection) {
    deleteAction.setEnabled(haveSelection);
    deleteAllAction.setEnabled(haveSelection);
    exportAsTextAction.setEnabled(haveSelection);
    if (!haveSelection) {
      textArea.setText("");
    }
    exportButton.setEnabled(haveAnyEntries());
  }

  private void exportAsArchive() throws Exception {
    FastFileChooser chooser = new FastFileChooser();
    if (lastExportDir != null) chooser.setCurrentDirectory(lastExportDir);
    chooser.setDialogTitle("Export thread dumps");
    chooser.setMultiSelectionEnabled(false);
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), DEFAULT_EXPORT_ARCHIVE_FILENAME));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    ZipOutputStream zipstream = new ZipOutputStream(new FileOutputStream(file));
    zipstream.setLevel(9);
    zipstream.setMethod(ZipOutputStream.DEFLATED);
    lastExportDir = file.getParentFile();
    XTreeNode root = (XTreeNode) threadDumpTreeModel.getRoot();
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

  @Override
  protected void export() throws Exception {
    exportAsArchive();
  }

  @Override
  protected void exportAsText() throws Exception {
    FastFileChooser chooser = new FastFileChooser();
    if (lastExportDir != null) chooser.setCurrentDirectory(lastExportDir);
    chooser.setDialogTitle("Export thread dump as text");
    chooser.setMultiSelectionEnabled(false);
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "thread-dump.txt"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    FileOutputStream fos = new FileOutputStream(file);
    lastExportDir = file.getParentFile();
    fos.write(lastSelectedThreadDumpTreeNode.getContent().getBytes("UTF-8"));
    fos.close();
  }

  public void treeNodesChanged(TreeModelEvent e) {
    if (tornDown.get()) { return; }
    ClusterThreadDumpEntry tde = (ClusterThreadDumpEntry) e.getTreePath().getPathComponent(1);
    boolean isDone = tde.isDone();
    exportButton.setEnabled(isDone);
    if (isDone) {
      updateSelectedContent();
      threadDumpButton.setText(appContext.getString("thread.dump.take"));
      clusterDumpButton.setText(appContext.getString("cluster.dump.take"));
    }
  }

  public void treeNodesInserted(TreeModelEvent e) {
    /**/
  }

  public void treeNodesRemoved(TreeModelEvent e) {
    /**/
  }

  public void treeStructureChanged(TreeModelEvent e) {
    /**/
  }

  private final AtomicBoolean tornDown = new AtomicBoolean(false);

  @Override
  public void tearDown() {
    if (!tornDown.compareAndSet(false, true)) { return; }

    threadDumpTreeModel.removeTreeModelListener(this);

    super.tearDown();

    synchronized (this) {
      clusterThreadDumpProvider = null;
      threadDumpTree = null;
      threadDumpTreeModel.tearDown();
      threadDumpTreeModel = null;
      lastSelectedThreadDumpTreeNode = null;
    }
  }
}
