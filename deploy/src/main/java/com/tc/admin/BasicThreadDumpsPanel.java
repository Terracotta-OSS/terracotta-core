/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTextArea;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

public abstract class BasicThreadDumpsPanel extends XContainer {
  protected final ApplicationContext appContext;

  protected XButton                  threadDumpButton;
  protected XButton                  clusterDumpButton;
  protected XButton                  exportButton;
  protected XScrollPane              itemScroller;
  protected XTextArea                textArea;
  protected XScrollPane              textScroller;
  protected DeleteAction             deleteAction;
  protected DeleteAllAction          deleteAllAction;
  protected ExportAsTextAction       exportAsTextAction;
  protected File                     lastExportDir;

  public BasicThreadDumpsPanel(ApplicationContext appContext) {
    super(new BorderLayout());

    this.appContext = appContext;

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(3, 3, 3, 3);

    topPanel.add(threadDumpButton = new XButton("Take Thread Dump"), gbc);
    threadDumpButton.addActionListener(new ThreadDumpButtonHandler());
    threadDumpButton.setText(appContext.getString("thread.dump.take"));
    gbc.gridx++;
    topPanel.add(exportButton = new XButton("Export All..."), gbc);
    exportButton.addActionListener(new ExportButtonHandler());
    gbc.gridx++;
    gbc.anchor = GridBagConstraints.EAST;
    topPanel.add(clusterDumpButton = new XButton("Take Cluster State Dump"), gbc);
    clusterDumpButton.addActionListener(new ClusterDumpButtonHandler());
    clusterDumpButton.setText(appContext.getString("cluster.dump.take"));

    // filler
    gbc.weightx = 1.0;
    topPanel.add(new XLabel(""), gbc);

    add(topPanel, BorderLayout.NORTH);

    itemScroller = new XScrollPane();
    XContainer contentArea = new XContainer(new BorderLayout());
    contentArea.add(textScroller = new XScrollPane(textArea = new XTextArea()));
    textArea.setEditable(false);
    textArea.setLineWrap(false);
    textArea.setFont((Font) appContext.getObject("textarea.font"));
    contentArea.add(new SearchPanel(appContext, textArea), BorderLayout.SOUTH);

    deleteAction = new DeleteAction();
    deleteAllAction = new DeleteAllAction();
    exportAsTextAction = new ExportAsTextAction();

    JPopupMenu textAreaPopup = textArea.createPopup();
    textAreaPopup.add(exportAsTextAction);
    textArea.setPopupMenu(textAreaPopup);

    XSplitPane splitter = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT, itemScroller, contentArea);
    splitter.setDefaultDividerLocation(0.24);
    splitter.setPreferences(getPreferences().node("ThreadDumpsSplitter"));
    add(splitter);
  }

  protected JPopupMenu createPopupMenu() {
    JPopupMenu popup = new JPopupMenu();
    popup.add(deleteAction);
    popup.add(deleteAllAction);
    popup.add(exportAsTextAction);
    return popup;
  }

  protected Preferences getPreferences() {
    return appContext.getPrefs().node(getClass().getName());
  }

  protected abstract void deleteSelected();

  protected class DeleteAction extends XAbstractAction {
    private DeleteAction() {
      super(appContext.getString("delete"));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      deleteSelected();
    }
  }

  protected abstract void deleteAll();

  protected class DeleteAllAction extends XAbstractAction {
    private DeleteAllAction() {
      super(appContext.getString("delete.all"));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      deleteAll();
    }
  }

  protected abstract void exportAsText() throws Exception;

  protected class ExportAsTextAction extends XAbstractAction {
    private ExportAsTextAction() {
      super(appContext.getString("thread.dump.export.as.text"));
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      try {
        exportAsText();
      } catch (Exception e) {
        // TODO: handle this
        appContext.log(e);
      }
    }
  }

  protected abstract void takeThreadDump();

  protected abstract void takeClusterDump();

  class ThreadDumpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      takeThreadDump();
    }
  }

  class ClusterDumpButtonHandler implements ActionListener {
    private static final String DISPLAY_MESSAGE = "Cluster dump may affect the performance of the cluster. \nAre you sure to take cluster dump?";
    private static final String TITLE           = "Terracotta Cluster State Dump";

    public void actionPerformed(ActionEvent e) {
      Object[] options = { "Yes, Take Cluster Dump", "No, thanks" };
      int choice = JOptionPane.showOptionDialog(null, DISPLAY_MESSAGE, TITLE, JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE, HelpHelper.getHelper().getHelpIcon(),
                                                options, options[1]);
      if (choice == 0) {
        takeClusterDump();
      }
    }
  }

  protected abstract void export() throws Exception;

  class ExportButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        export();
      } catch (Exception e) {
        // TODO: handle this
        appContext.log(e);
      }
    }
  }
}
