/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.FastFileChooser;
import com.tc.admin.common.XList;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class AbstractThreadDumpsPanel extends BasicThreadDumpsPanel {
  private XList               entryList;
  private DefaultListModel    entryListModel;
  private ThreadDumpEntry     lastSelectedEntry;

  private static final String DELETE_ITEM_CMD = "DeleteItemCmd";

  public AbstractThreadDumpsPanel(ApplicationContext appContext) {
    super(appContext);

    itemScroller.setViewportView(entryList = new XList());
    entryList.setModel(entryListModel = new DefaultListModel());
    entryList.addListSelectionListener(new ThreadDumpListSelectionListener());
    entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    entryList.setComponentPopupMenu(createPopupMenu());

    entryList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE_ITEM_CMD);
    entryList.getActionMap().put(DELETE_ITEM_CMD, deleteAction);
  }

  @Override
  protected void deleteSelected() {
    int row = entryList.getSelectedIndex();
    DefaultListModel listModel = (DefaultListModel) entryList.getModel();
    listModel.remove(row);
  }

  @Override
  protected void deleteAll() {
    DefaultListModel listModel = (DefaultListModel) entryList.getModel();
    listModel.removeAllElements();
  }

  protected abstract String getNodeName();

  protected abstract Future<String> getThreadDumpText() throws Exception;

  protected abstract Future<String> getClusterDump() throws Exception;

  private ThreadDumpEntry createThreadDumpEntry() throws Exception {
    return new TDE(getThreadDumpText());
  }

  private class TDE extends ThreadDumpEntry {
    TDE(Future<String> threadDumpFuture) {
      super(AbstractThreadDumpsPanel.this.appContext, threadDumpFuture);
    }

    @Override
    public void run() {
      super.run();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          int row = entryListModel.indexOf(TDE.this);
          if (entryList.isSelectedIndex(row)) {
            textArea.setText(getContent());
          }
          threadDumpButton.setText(appContext.getString("thread.dump.take"));
          exportButton.setEnabled(true);
        }
      });
    }
  }

  private ThreadDumpEntry createClusterDumpEntry() throws Exception {
    return new CDE(getClusterDump());
  }

  private class CDE extends ThreadDumpEntry {
    CDE(Future<String> threadDumpFuture) {
      super(AbstractThreadDumpsPanel.this.appContext, threadDumpFuture);
    }

    @Override
    public void run() {
      super.run();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          int row = entryListModel.indexOf(CDE.this);
          if (entryList.isSelectedIndex(row)) {
            textArea.setText(getContent());
          }
          clusterDumpButton.setText(appContext.getString("cluster.dump.take"));
          exportButton.setEnabled(true);
        }
      });
    }
  }

  private boolean isWaiting() {
    return threadDumpButton.getText().equals(appContext.getString("cancel"))
           || clusterDumpButton.getText().equals(appContext.getString("cancel"));
  }

  @Override
  public void takeThreadDump() {
    try {
      if (!isWaiting()) {
        exportButton.setEnabled(false);
        threadDumpButton.setText(appContext.getString("cancel"));
        entryListModel.addElement(createThreadDumpEntry());
        entryList.setSelectedIndex(entryListModel.getSize() - 1);
      } else {
        ThreadDumpEntry tde = (ThreadDumpEntry) entryListModel.getElementAt(entryListModel.getSize() - 1);
        tde.cancel();
      }
    } catch (Exception e) {
      appContext.log(e);
    }
  }

  @Override
  public void takeClusterDump() {
    try {
      if (!isWaiting()) {
        exportButton.setEnabled(false);
        clusterDumpButton.setText(appContext.getString("cancel"));
        entryListModel.addElement(createClusterDumpEntry());
        entryList.setSelectedIndex(entryListModel.getSize() - 1);
      } else {
        ThreadDumpEntry tde = (ThreadDumpEntry) entryListModel.getElementAt(entryListModel.getSize() - 1);
        tde.cancel();
      }
    } catch (Exception e) {
      appContext.log(e);
    }
  }

  class ThreadDumpListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent lse) {
      if (lse.getValueIsAdjusting()) return;
      if (lastSelectedEntry != null) {
        lastSelectedEntry.setViewPosition(textScroller.getViewport().getViewPosition());
      }
      ThreadDumpEntry tde = (ThreadDumpEntry) entryList.getSelectedValue();
      if (tde != null) {
        textArea.setText(tde.getContent());
        final Point viewPosition = tde.getViewPosition();
        if (viewPosition != null) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              textScroller.getViewport().setViewPosition(viewPosition);
            }
          });
        }
      }
      lastSelectedEntry = tde;
      setControlsEnabled(tde != null);
    }
  }

  private void setControlsEnabled(boolean haveSelection) {
    deleteAction.setEnabled(haveSelection);
    deleteAllAction.setEnabled(haveSelection);
    exportAsTextAction.setEnabled(haveSelection);
    if (!haveSelection) {
      exportButton.setEnabled(haveSelection);
      textArea.setText("");
    }
  }

  private void exportAsArchive() throws Exception {
    FastFileChooser chooser = new FastFileChooser();
    if (lastExportDir != null) chooser.setCurrentDirectory(lastExportDir);
    chooser.setDialogTitle(appContext.getString("export.all.thread.dumps.dialog.title"));
    chooser.setMultiSelectionEnabled(false);
    String nodeName = getNodeName().replace(':', '-');
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), nodeName + "-thread-dumps.zip"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    ZipOutputStream zipstream = new ZipOutputStream(new FileOutputStream(file));
    zipstream.setLevel(9);
    zipstream.setMethod(ZipOutputStream.DEFLATED);
    lastExportDir = file.getParentFile();
    int count = entryList.getModel().getSize();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'/'HH.mm.ss.SSSZ");
    for (int i = 0; i < count; i++) {
      ThreadDumpEntry tde = (ThreadDumpEntry) entryList.getModel().getElementAt(i);
      String filenameBase = dateFormat.format(tde.getTime());
      ZipEntry zipentry = new ZipEntry(filenameBase);
      zipstream.putNextEntry(zipentry);
      zipstream.write(tde.getContent().getBytes("UTF-8"));
      zipstream.closeEntry();
    }
    zipstream.close();
  }

  @Override
  protected void export() {
    try {
      exportAsArchive();
    } catch (Exception e) {
      appContext.log(e);
    }
  }

  @Override
  protected void exportAsText() throws Exception {
    FastFileChooser chooser = new FastFileChooser();
    if (lastExportDir != null) chooser.setCurrentDirectory(lastExportDir);
    chooser.setDialogTitle(appContext.getString("export.thread.dump.as.text.dialog.title"));
    chooser.setMultiSelectionEnabled(false);
    String nodeName = getNodeName().replace(':', '-');
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), nodeName + "-thread-dump.txt"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    lastExportDir = file.getParentFile();
    int row = entryList.getSelectedIndex();
    ThreadDumpEntry tde = (ThreadDumpEntry) entryList.getModel().getElementAt(row);
    FileOutputStream fos = new FileOutputStream(file);
    try {
      fos.write(tde.getContent().getBytes("UTF-8"));
    } finally {
      fos.close();
    }
  }

  @Override
  public void tearDown() {
    super.tearDown();

    entryList = null;
    entryListModel = null;
    lastSelectedEntry = null;
  }
}
