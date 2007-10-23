/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.ContainerResource;
import org.dijon.Spinner;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.LockElementWrapper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XPopupListener;
import com.tc.management.L2LockStatsManagerImpl.LockStackTracesStat;
import com.tc.management.L2LockStatsManagerImpl.LockStat;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.object.lockmanager.impl.TCStackTraceElement;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerInvocationHandler;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class LocksPanel extends XContainer implements PopupMenuListener, ListSelectionListener {
  private XComboBox                   m_typeCombo;
  private Spinner                     m_countSpinner;
  private Spinner                     m_traceDepthSpinner;
  private Spinner                     m_gatherIntervalSpinner;
  private XButton                     m_refreshButton;
  private JScrollPane                 m_tableScroller;
  private LockElementTable            m_lockTable;
  private LockStatTable               m_lockStatTable;
  private XObjectTableModel           m_lockTableModel;
  private LockStatisticsMonitorMBean  m_lockStatsMBean;
  private JCheckBoxMenuItem           m_clientTracesEnabledToggle;
  private ClientTracesEnabledAction   m_clientTracesEnabledAction;
  private GatherClientTracesAction    m_gatherClientTracesAction;
  private GatherAllClientTracesAction m_gatherAllClientTracesAction;
  private ResetClientTracesAction     m_resetClientTracesAction;
  private ResetAllClientTracesAction  m_resetAllClientTracesAction;
  private XPopupListener              m_popupListener;

  private final int                   DEFAULT_LOCK_COUNT      = 100;
  private final int                   DEFAULT_TRACE_DEPTH     = 4;
  private final int                   DEFAULT_GATHER_INTERVAL = 1;

  private final String[]              ALL_TYPES               = { "AggregateLockHolderStats", "AggregateWaitingLocks",
      "Requested", "ContendedLocks", "LockHops"              };

  private static final String         REFRESH                 = "Refresh";

  public LocksPanel(ConnectionContext cc) {
    super();

    AdminClientContext cntx = AdminClient.getContext();

    load((ContainerResource) cntx.topRes.getComponent("LocksPanel"));

    m_lockStatsMBean = (LockStatisticsMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, L2MBeanNames.LOCK_STATISTICS, LockStatisticsMonitorMBean.class, false);

    // We do this to force an early error if the server we're connecting to is old and doesn't
    // have the LockStatsMBean. DSONode catches the error and doesn't display the LocksNode.
    isLockStatisticsEnabled();

    m_typeCombo = (XComboBox) findComponent("TypeCombo");
    m_typeCombo.setModel(new DefaultComboBoxModel(ALL_TYPES));
    m_typeCombo.setSelectedIndex(0);
    m_typeCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setType((String) m_typeCombo.getSelectedItem());
      }
    });

    m_countSpinner = (Spinner) findComponent("CountSpinner");
    m_countSpinner.setValue(Integer.valueOf(DEFAULT_LOCK_COUNT));
    ((SpinnerNumberModel)m_countSpinner.getModel()).setMinimum(Integer.valueOf(1));
    
    m_traceDepthSpinner = (Spinner) findComponent("TraceDepthSpinner");
    m_traceDepthSpinner.setValue(Integer.valueOf(DEFAULT_TRACE_DEPTH));
    ((SpinnerNumberModel)m_traceDepthSpinner.getModel()).setMinimum(Integer.valueOf(0));

    m_gatherIntervalSpinner = (Spinner) findComponent("GatherIntervalSpinner");
    m_gatherIntervalSpinner.setValue(Integer.valueOf(DEFAULT_GATHER_INTERVAL));
    ((SpinnerNumberModel)m_gatherIntervalSpinner.getModel()).setMinimum(Integer.valueOf(0));

    RefreshAction refreshAction = new RefreshAction();
    m_refreshButton = (XButton) findComponent("RefreshButton");
    m_refreshButton.addActionListener(refreshAction);

    m_popupListener = new XPopupListener();

    m_lockStatTable = new LockStatTable();

    XObjectTable table = (XObjectTable) findComponent("LockTable");
    m_tableScroller = (JScrollPane) table.getAncestorOfClass(JScrollPane.class);
    setType((String) m_typeCombo.getSelectedItem());

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    getActionMap().put(REFRESH, refreshAction);
    getInputMap().put(ks, REFRESH);

    JPopupMenu popup = new JPopupMenu();
    m_clientTracesEnabledAction = new ClientTracesEnabledAction();
    popup.add(m_clientTracesEnabledToggle = new JCheckBoxMenuItem(m_clientTracesEnabledAction));
    popup.addSeparator();
    popup.add(new JMenuItem(m_gatherClientTracesAction = new GatherClientTracesAction()));
    popup.add(new JMenuItem(m_gatherAllClientTracesAction = new GatherAllClientTracesAction()));
    popup.addSeparator();
    popup.add(new JMenuItem(m_resetClientTracesAction = new ResetClientTracesAction()));
    popup.add(new JMenuItem(m_resetAllClientTracesAction = new ResetAllClientTracesAction()));
    popup.addSeparator();
    popup.add(new JMenuItem(refreshAction));
    m_popupListener.setPopupMenu(popup);
    popup.addPopupMenuListener(this);
  }

  public void popupMenuCanceled(PopupMenuEvent e) {/**/
  }

  public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/**/
  }

  public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
    if (wrapper != null) {
      boolean clientTracesEnabled = isClientTracesEnabled(wrapper);
      m_clientTracesEnabledToggle.setSelected(clientTracesEnabled);
      m_gatherClientTracesAction.setEnabled(clientTracesEnabled);
      m_resetClientTracesAction.setEnabled(clientTracesEnabled);
    }
  }

  private void setType(String type) {
    if (m_lockTable != null) {
      m_lockTable.getSelectionModel().removeListSelectionListener(this);
      m_lockTable.setSortColumn(-1);
    }

    m_lockTableModel = new LockStatTableModel(type);
    m_lockTable = m_lockStatTable;

    m_lockTable.setModel(m_lockTableModel);
    m_popupListener.setTarget(m_lockTable);
    m_tableScroller.setViewportView(m_lockTable);
    m_lockTable.getSelectionModel().addListSelectionListener(this);
  }

  private Object getSpinnerValue(JSpinner spinner) {
    try {
      spinner.commitEdit();
    } catch (ParseException pe) {
      // Edited value is invalid, spinner.getValue() will return
      // the last valid value, you could revert the spinner to show that:
      JComponent editor = spinner.getEditor();
      if (editor instanceof JSpinner.DefaultEditor) {
        ((JSpinner.DefaultEditor) editor).getTextField().setValue(spinner.getValue());
      }
    }
    return spinner.getValue();
  }

  private int getMaxLocks() {
    Object o = getSpinnerValue(m_countSpinner);
    if (o instanceof Integer) { return ((Integer) o).intValue(); }
    return DEFAULT_LOCK_COUNT;
  }

  private int getTraceDepth() {
    Object o = getSpinnerValue(m_traceDepthSpinner);
    if (o instanceof Integer) { return ((Integer) o).intValue(); }
    return DEFAULT_TRACE_DEPTH;
  }

  private int getGatherInterval() {
    Object o = getSpinnerValue(m_gatherIntervalSpinner);
    if (o instanceof Integer) { return ((Integer) o).intValue(); }
    return DEFAULT_GATHER_INTERVAL;
  }

  public class RefreshAction extends XAbstractAction {
    RefreshAction() {
      super("Refresh");
    }

    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  public void setLockStatisticsEnabled(boolean lockStatsEnabled) {
    m_lockStatsMBean.setLockStatisticsEnabled(lockStatsEnabled);
    refresh();
  }

  public boolean isLockStatisticsEnabled() {
    return m_lockStatsMBean.isLockStatisticsEnabled();
  }

  public class ClientTracesEnabledAction extends XAbstractAction {
    ClientTracesEnabledAction() {
      super("Client traces enabled");
    }

    public void actionPerformed(ActionEvent ae) {
      LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
      if (wrapper != null) {
        setClientTracesEnabled(wrapper, m_clientTracesEnabledToggle.isSelected());
      }
    }
  }

  private boolean isClientTracesEnabled(LockElementWrapper wrapper) {
    return m_lockStatsMBean.isClientStackTraceEnabled(wrapper.getLockID());
  }

  private void setClientTracesEnabled(LockElementWrapper wrapper, boolean clientTracesEnabled) {
    String lockId = wrapper.getLockID();
    if (clientTracesEnabled) {
      m_lockStatsMBean.enableClientStackTrace(lockId, getTraceDepth(), getGatherInterval());
    } else {
      m_lockStatsMBean.disableClientStackTrace(lockId);
      wrapper.setStackTrace(null);
    }
  }

  public void setAllClientTracesEnabled(boolean allStatsEnabled) {
    int count = m_lockTableModel.getRowCount();
    for (int i = 0; i < count; i++) {
      LockElementWrapper wrapper = m_lockTable.getWrapperAt(i);
      setClientTracesEnabled(wrapper, allStatsEnabled);
    }
  }

  public class GatherClientTracesAction extends XAbstractAction {
    GatherClientTracesAction() {
      super("Gather client traces");
    }

    public void actionPerformed(ActionEvent ae) {
      LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
      if (wrapper != null) {
        gatherTrace(wrapper, true);
      }
    }
  }

  public class ResetClientTracesAction extends XAbstractAction {
    ResetClientTracesAction() {
      super("Reset client traces");
    }

    public void actionPerformed(ActionEvent ae) {
      LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
      if (wrapper != null) {
        setClientTracesEnabled(wrapper, false);
        setClientTracesEnabled(wrapper, true);
      }
    }
  }

  public class ResetAllClientTracesAction extends XAbstractAction {
    ResetAllClientTracesAction() {
      super("Reset all client traces");
    }

    public void actionPerformed(ActionEvent ae) {
      LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
      if (wrapper != null) {
        setAllClientTracesEnabled(false);
        setAllClientTracesEnabled(true);
      }
    }
  }

  private void gatherTrace(LockElementWrapper wrapper, boolean toConsole) {
    String id = wrapper.getLockID();
    Collection<LockStackTracesStat> c = m_lockStatsMBean.getStackTraces(id);
    if (c.isEmpty()) { return; }
    Iterator<LockStackTracesStat> iter = c.iterator();
    HashMap<TCStackTraceElement, TCStackTraceElement> map = new HashMap<TCStackTraceElement, TCStackTraceElement>();
    while (iter.hasNext()) {
      LockStackTracesStat sts = iter.next();
      List<TCStackTraceElement> stackTraces = sts.getStackTraces();
      if (!stackTraces.isEmpty()) {
        for (TCStackTraceElement elem : stackTraces) {
          map.put(elem, elem);
        }
      }
    }
    Iterator<TCStackTraceElement> i = map.keySet().iterator();
    StringBuffer allTraces = new StringBuffer();
    StringBuffer sb = new StringBuffer("<html>");
    while (i.hasNext()) {
      TCStackTraceElement elem = i.next();
      for (StackTraceElement ste : elem.getStackTraceElements()) {
        String s = ste.toString();
        sb.append("<p>");
        sb.append(s);
        sb.append("</p>\n");

        allTraces.append(s);
        allTraces.append("\n");
      }
      if (i.hasNext()) {
        sb.append("<br>");
        allTraces.append("\n");
      }
    }
    sb.append("</html>");
    wrapper.setStackTrace(sb.toString());

    String allStackTraces = allTraces.toString();
    if (toConsole) AdminClient.getContext().log(allStackTraces);
    wrapper.setAllStackTraces(allStackTraces);
  }

  public class GatherAllClientTracesAction extends XAbstractAction {
    GatherAllClientTracesAction() {
      super("Gather all client traces");
    }

    public void actionPerformed(ActionEvent ae) {
      int count = m_lockTableModel.getRowCount();
      for (int i = 0; i < count; i++) {
        LockElementWrapper wrapper = m_lockTable.getWrapperAt(i);
        gatherTrace(wrapper, false);
      }
    }
  }

  private void updateTableModel() {
    setType((String) m_typeCombo.getSelectedItem());
  }

  public void refresh() {
    AdminClientContext acc = AdminClient.getContext();

    acc.controller.setStatus(acc.getMessage("dso.locks.refreshing"));
    updateTableModel();
    acc.controller.clearStatus();
  }

  class LockElementTable extends XObjectTable implements MouseMotionListener {
    LockElementTable() {
      super();
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      addMouseMotionListener(this);
      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          int row = rowAtPoint(e.getPoint());
          setRowSelectionInterval(row, row);
        }
      });
    }

    LockElementWrapper getSelectedWrapper() {
      int row = getSelectedRow();
      return (row != -1) ? getWrapperAt(row) : null;
    }

    LockElementWrapper getWrapperAt(int i) {
      return (LockElementWrapper) m_lockTableModel.getObjectAt(i);
    }

    public void mouseDragged(MouseEvent e) {/**/
    }

    public void mouseMoved(MouseEvent e) {
      LockElementWrapper wrapper = getWrapperAt(rowAtPoint(e.getPoint()));
      String tip = null;
      if (wrapper != null) {
        tip = wrapper.getStackTrace();
      }
      setToolTipText(tip);
    }
  }

  static final String[] LOCK_STAT_ATTRS = { "LockID", "NumOfLockRequested", "NumOfLockReleased",
      "NumOfPendingRequests", "NumOfPendingWaiters", "NumOfLockHopRequests", "AvgWaitTimeInMillis", "AvgHeldTimeInMillis" };

  static final String[] LOCK_STAT_COLS  = { "LockID", "LockRequested", "LockReleased", "PendingRequests",
      "PendingWaiters", "LockHopRequests", "AvgWaitMillis", "AvgHeldMillis" };

  class LockStatTableModel extends XObjectTableModel {
    String lockType;

    LockStatTableModel(String lockType) {
      super(LockStatWrapper.class, LOCK_STAT_ATTRS, LOCK_STAT_COLS);
      this.lockType = lockType;
      init();
    }

    private Collection getCollection() {
      try {
        Method m = m_lockStatsMBean.getClass().getMethod("getTop" + lockType, new Class[] { Integer.TYPE });
        return (Collection) m.invoke(m_lockStatsMBean, new Object[] { Integer.valueOf(getMaxLocks()) });
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Collections.EMPTY_LIST;
    }

    private void init() {
      try {
        set(wrap(getCollection()));
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    Object[] wrap(Collection c) {
      Iterator i = c.iterator();
      ArrayList<LockStatWrapper> l = new ArrayList<LockStatWrapper>();
      while (i.hasNext()) {
        l.add(new LockStatWrapper((LockStat) i.next()));
      }
      return l.toArray(new LockStatWrapper[0]);
    }
  }

  class LockStatTable extends LockElementTable {
    LockStatTable() {
      super();
    }
  }

  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) { return; }

    LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
    if (wrapper != null) {
      String allTraces = wrapper.getAllStackTraces();
      if (allTraces != null) {
        AdminClient.getContext().log(allTraces);
      }
    }
  }
}
