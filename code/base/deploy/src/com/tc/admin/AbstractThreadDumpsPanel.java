/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.List;
import org.dijon.ScrollPane;

import com.tc.admin.common.FastFileChooser;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTextArea;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class AbstractThreadDumpsPanel extends XContainer {
  protected AdminClientContext m_acc;
  private Button               m_threadDumpButton;
  private List                 m_threadDumpList;
  private DefaultListModel     m_threadDumpListModel;
  private XTextArea            m_threadDumpTextArea;
  private ScrollPane           m_threadDumpTextScroller;
  private ThreadDumpEntry      m_lastSelectedEntry;
  private Button               m_exportButton;
  private File                 m_lastExportDir;
  private DeleteAction         m_deleteAction;
  private DeleteAllAction      m_deleteAllAction;
  private ExportAsTextAction   m_exportAsTextAction;

  private static final String  DELETE_ITEM_CMD = "DeleteItemCmd";

  public AbstractThreadDumpsPanel() {
    super();

    m_acc = AdminClient.getContext();
    load((ContainerResource) m_acc.getComponent("ThreadDumpsPanel"));

    m_threadDumpButton = (Button) findComponent("TakeThreadDumpButton");
    m_threadDumpButton.addActionListener(new ThreadDumpButtonHandler());
    m_threadDumpButton.setText(m_acc.getString("thread.dump.take"));

    XSplitPane splitter = (XSplitPane) findComponent("ThreadDumpsSplitter");
    splitter.setPreferences(getPreferences().node(splitter.getName()));

    ScrollPane itemScroller = (ScrollPane) findComponent("ThreadDumpItemScroller");
    itemScroller.setItem(m_threadDumpList = new List());
    m_threadDumpList.setModel(m_threadDumpListModel = new DefaultListModel());
    m_threadDumpList.addListSelectionListener(new ThreadDumpListSelectionListener());
    m_threadDumpList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    m_threadDumpList.setComponentPopupMenu(createListPopup());

    m_threadDumpTextArea = (XTextArea) findComponent("ThreadDumpTextArea");
    JPopupMenu textAreaPopup = m_threadDumpTextArea.createPopup();
    textAreaPopup.add(m_exportAsTextAction);
    m_threadDumpTextArea.setPopupMenu(textAreaPopup);
    m_threadDumpTextScroller = (ScrollPane) findComponent("ThreadDumpTextScroller");

    m_exportButton = (Button) findComponent("ExportButton");
    m_exportButton.addActionListener(new ExportAsArchiveHandler());

    ((SearchPanel) findComponent("SearchPanel")).setTextComponent(m_threadDumpTextArea);

    m_threadDumpList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE_ITEM_CMD);
    m_threadDumpList.getActionMap().put(DELETE_ITEM_CMD, m_deleteAction);
  }

  private JPopupMenu createListPopup() {
    JPopupMenu popup = new JPopupMenu();
    popup.add(m_deleteAction = new DeleteAction());
    popup.add(m_deleteAllAction = new DeleteAllAction());
    popup.add(m_exportAsTextAction = new ExportAsTextAction());
    return popup;
  }

  private class DeleteAction extends XAbstractAction {
    private DeleteAction() {
      super(m_acc.getString("delete"));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      int row = m_threadDumpList.getSelectedIndex();
      DefaultListModel listModel = (DefaultListModel) m_threadDumpList.getModel();
      listModel.remove(row);
    }
  }

  private class DeleteAllAction extends XAbstractAction {
    private DeleteAllAction() {
      super(m_acc.getString("delete.all"));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      DefaultListModel listModel = (DefaultListModel) m_threadDumpList.getModel();
      listModel.removeAllElements();
    }
  }

  private class ExportAsTextAction extends XAbstractAction {
    private ExportAsTextAction() {
      super(m_acc.getString("thread.dump.export.as.text"));
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

  protected abstract String getNodeName();

  protected abstract Future<String> getThreadDumpText() throws Exception;

  protected Preferences getPreferences() {
    return m_acc.getPrefs().node(getClass().getName());
  }

  private ThreadDumpEntry createThreadDumpEntry() throws Exception {
    return new TDE(getThreadDumpText());
  }

  private class TDE extends ThreadDumpEntry {
    TDE(Future<String> threadDumpFuture) {
      super(threadDumpFuture);
    }

    public void run() {
      super.run();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          int row = m_threadDumpListModel.indexOf(TDE.this);
          if (m_threadDumpList.isSelectedIndex(row)) {
            m_threadDumpTextArea.setText(getContent());
          }
          m_threadDumpButton.setText(m_acc.getString("thread.dump.take"));
          m_exportButton.setEnabled(true);
        }
      });
    }
  }
  
  private boolean isWaiting() {
    return m_threadDumpButton.getText().equals(m_acc.getString("cancel"));
  }

  class ThreadDumpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        if (!isWaiting()) {
          m_exportButton.setEnabled(false);
          m_threadDumpButton.setText(m_acc.getString("cancel"));
          m_threadDumpListModel.addElement(createThreadDumpEntry());
          m_threadDumpList.setSelectedIndex(m_threadDumpListModel.getSize() - 1);
        } else {
          ThreadDumpEntry tde = (ThreadDumpEntry) m_threadDumpListModel
              .getElementAt(m_threadDumpListModel.getSize() - 1);
          tde.cancel();
        }
      } catch (Exception e) {
        AdminClient.getContext().log(e);
      }
    }
  }

  class ThreadDumpListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent lse) {
      if (lse.getValueIsAdjusting()) return;
      if (m_lastSelectedEntry != null) {
        m_lastSelectedEntry.setViewPosition(m_threadDumpTextScroller.getViewport().getViewPosition());
      }
      ThreadDumpEntry tde = (ThreadDumpEntry) m_threadDumpList.getSelectedValue();
      if (tde != null) {
        m_threadDumpTextArea.setText(tde.getContent());
        final Point viewPosition = tde.getViewPosition();
        if (viewPosition != null) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              m_threadDumpTextScroller.getViewport().setViewPosition(viewPosition);
            }
          });
        }
      }
      m_lastSelectedEntry = tde;
      setControlsEnabled(tde != null);
    }
  }

  private void setControlsEnabled(boolean haveSelection) {
    m_deleteAction.setEnabled(haveSelection);
    m_deleteAllAction.setEnabled(haveSelection);
    m_exportAsTextAction.setEnabled(haveSelection);
    if (!haveSelection) {
      m_exportButton.setEnabled(haveSelection);
      m_threadDumpTextArea.setText("");
    }
  }

  private void exportAsArchive() throws Exception {
    FastFileChooser chooser = new FastFileChooser();
    if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
    chooser.setDialogTitle(m_acc.getString("export.all.thread.dumps.dialog.title"));
    chooser.setMultiSelectionEnabled(false);
    String nodeName = getNodeName().replace(':', '-');
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), nodeName + "-thread-dumps.zip"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    ZipOutputStream zipstream = new ZipOutputStream(new FileOutputStream(file));
    zipstream.setLevel(9);
    zipstream.setMethod(ZipOutputStream.DEFLATED);
    m_lastExportDir = file.getParentFile();
    int count = m_threadDumpList.getModel().getSize();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'/'HH.mm.ss.SSSZ");
    for (int i = 0; i < count; i++) {
      ThreadDumpEntry tde = (ThreadDumpEntry) m_threadDumpList.getModel().getElementAt(i);
      String filenameBase = dateFormat.format(tde.getTime());
      ZipEntry zipentry = new ZipEntry(filenameBase);
      zipstream.putNextEntry(zipentry);
      zipstream.write(tde.getContent().getBytes("UTF-8"));
      zipstream.closeEntry();
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
    chooser.setDialogTitle(m_acc.getString("export.thread.dump.as.text.dialog.title"));
    chooser.setMultiSelectionEnabled(false);
    String nodeName = getNodeName().replace(':', '-');
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), nodeName + "-thread-dump.txt"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    FileOutputStream fos = new FileOutputStream(file);
    m_lastExportDir = file.getParentFile();
    int row = m_threadDumpList.getSelectedIndex();
    ThreadDumpEntry tde = (ThreadDumpEntry) m_threadDumpList.getModel().getElementAt(row);
    fos.write(tde.getContent().getBytes("UTF-8"));
    fos.close();
  }

  public void tearDown() {
    super.tearDown();

    m_acc = null;
    m_threadDumpButton = null;
    m_threadDumpList = null;
    m_threadDumpListModel = null;
    m_threadDumpTextArea = null;
    m_threadDumpTextScroller = null;
    m_lastSelectedEntry = null;
    m_lastExportDir = null;
    m_deleteAction = null;
    m_deleteAllAction = null;
    m_exportAsTextAction = null;
  }
}
