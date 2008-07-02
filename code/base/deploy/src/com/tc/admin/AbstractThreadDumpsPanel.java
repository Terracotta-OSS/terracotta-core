/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.List;
import org.dijon.ScrollPane;
import org.dijon.SplitPane;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ThreadDumpEntry;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextArea;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class AbstractThreadDumpsPanel extends XContainer {
  protected AdminClientContext m_acc;

  private Button               m_threadDumpButton;
  private SplitPane            m_threadDumpsSplitter;
  private Integer              m_dividerLoc;
  private DividerListener      m_dividerListener;
  private List                 m_threadDumpList;
  private DefaultListModel     m_threadDumpListModel;
  private XTextArea            m_threadDumpTextArea;
  private ScrollPane           m_threadDumpTextScroller;
  private ThreadDumpEntry      m_lastSelectedEntry;

  public AbstractThreadDumpsPanel() {
    super();

    m_acc = AdminClient.getContext();

    load((ContainerResource) m_acc.topRes.getComponent("NodeThreadDumpsPanel"));

    m_threadDumpButton = (Button) findComponent("TakeThreadDumpButton");
    m_threadDumpButton.addActionListener(new ThreadDumpButtonHandler());

    m_threadDumpsSplitter = (SplitPane) findComponent("ThreadDumpsSplitter");
    m_dividerLoc = Integer.valueOf(getThreadDumpSplitPref());
    m_dividerListener = new DividerListener();

    m_threadDumpList = (List) findComponent("ThreadDumpList");
    m_threadDumpList.setModel(m_threadDumpListModel = new DefaultListModel());
    m_threadDumpList.addListSelectionListener(new ThreadDumpListSelectionListener());
    m_threadDumpTextArea = (XTextArea) findComponent("ThreadDumpTextArea");
    m_threadDumpTextScroller = (ScrollPane) findComponent("ThreadDumpTextScroller");
  }

  protected abstract String getThreadDumpText() throws Exception;
  protected abstract Preferences getPreferences();

  private ThreadDumpEntry createThreadDumpEntry() throws Exception {
    return new ThreadDumpEntry(getThreadDumpText());
  }

  class ThreadDumpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        m_threadDumpListModel.addElement(createThreadDumpEntry());
        m_threadDumpList.setSelectedIndex(m_threadDumpListModel.getSize() - 1);
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
      m_threadDumpTextArea.setText(tde.getThreadDumpText());
      final Point viewPosition = tde.getViewPosition();
      if (viewPosition != null) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            m_threadDumpTextScroller.getViewport().setViewPosition(viewPosition);
          }
        });
      }
      m_lastSelectedEntry = tde;
    }
  }

  public void addNotify() {
    super.addNotify();
    m_threadDumpsSplitter.addPropertyChangeListener(m_dividerListener);
  }

  public void removeNotify() {
    m_threadDumpsSplitter.removePropertyChangeListener(m_dividerListener);
    super.removeNotify();
  }

  public void doLayout() {
    super.doLayout();

    if (m_dividerLoc != null) {
      m_threadDumpsSplitter.setDividerLocation(m_dividerLoc.intValue());
    } else {
      m_threadDumpsSplitter.setDividerLocation(0.7);
    }
  }

  private int getThreadDumpSplitPref() {
    Preferences prefs = getPreferences();
    Preferences splitPrefs = prefs.node(m_threadDumpsSplitter.getName());
    return splitPrefs.getInt("Split", -1);
  }

  protected void storePreferences() {
    AdminClientContext acc = AdminClient.getContext();
    acc.client.storePrefs();
  }

  private class DividerListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent pce) {
      JSplitPane splitter = (JSplitPane) pce.getSource();
      String propName = pce.getPropertyName();

      if (splitter.isShowing() == false || JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(propName) == false) { return; }

      int divLoc = splitter.getDividerLocation();
      Integer divLocObj =  Integer.valueOf(divLoc);
      Preferences prefs = getPreferences();
      String name = splitter.getName();
      Preferences node = prefs.node(name);

      node.putInt("Split", divLoc);
      storePreferences();

      m_dividerLoc = divLocObj;
    }
  }

  public void tearDown() {
    super.tearDown();

    m_acc = null;
    m_threadDumpButton = null;
    m_threadDumpsSplitter = null;
    m_dividerLoc = null;
    m_dividerListener = null;
    m_threadDumpList = null;
    m_threadDumpListModel = null;
    m_threadDumpTextArea = null;
    m_threadDumpTextScroller = null;
    m_lastSelectedEntry = null;
  }
}
