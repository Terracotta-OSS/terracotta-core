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
import com.tc.objectserver.lockmanager.api.LockHolder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerInvocationHandler;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

public class LocksPanel extends XContainer {
  private XComboBox                  m_typeCombo;
  private Spinner                    m_countSpinner;
  private Spinner                    m_traceDepthSpinner;
  private Spinner                    m_gatherIntervalSpinner;
  private XButton                    m_refreshButton;
  private JScrollPane                m_tableScroller;
  private LockElementTable           m_lockTable;
  private LockHolderTable            m_lockHolderTable;
  private LockStatTable              m_lockStatTable;
  private XObjectTableModel          m_lockTableModel;
  private LockStatisticsMonitorMBean lockStatsMBean;
  private XPopupListener             m_popupListener;

  private final int                  DEFAULT_LOCK_COUNT        = 100;
  private final int                  DEFAULT_TRACE_DEPTH       = 4;
  private final int                  DEFAULT_GATHER_INTERVAL   = 1;  

  private final String               HOLDER_TYPE_HELD          = "Held";
  private final String               HOLDER_TYPE_WAITING_LOCKS = "WaitingLocks";

  private final String               STAT_TYPE_REQUESTED       = "Requested";
  private final String               STAT_TYPE_CONTENDED_LOCKS = "ContendedLocks";
  private final String               STAT_TYPE_LOCK_HOPS       = "LockHops";

  private final String[]             ALL_TYPES                 = { HOLDER_TYPE_HELD, HOLDER_TYPE_WAITING_LOCKS,
      STAT_TYPE_REQUESTED, STAT_TYPE_CONTENDED_LOCKS, STAT_TYPE_LOCK_HOPS };

  private static final String        REFRESH                   = "Refresh";

  public LocksPanel(ConnectionContext cc) {
    super();

    AdminClientContext cntx = AdminClient.getContext();

    load((ContainerResource) cntx.topRes.getComponent("LocksPanel"));

    lockStatsMBean = (LockStatisticsMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, L2MBeanNames.LOCK_STATISTICS, LockStatisticsMonitorMBean.class, false);

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

    m_traceDepthSpinner = (Spinner) findComponent("TraceDepthSpinner");
    m_traceDepthSpinner.setValue(Integer.valueOf(DEFAULT_TRACE_DEPTH));

    m_gatherIntervalSpinner = (Spinner) findComponent("GatherIntervalSpinner");
    m_gatherIntervalSpinner.setValue(Integer.valueOf(DEFAULT_GATHER_INTERVAL));

    RefreshAction refreshAction = new RefreshAction();
    m_refreshButton = (XButton) findComponent("RefreshButton");
    m_refreshButton.addActionListener(refreshAction);

    m_popupListener = new XPopupListener();

    m_lockHolderTable = new LockHolderTable();
    m_lockStatTable = new LockStatTable();

    XObjectTable table = (XObjectTable) findComponent("LockTable");
    m_tableScroller = (JScrollPane) table.getAncestorOfClass(JScrollPane.class);
    setType((String) m_typeCombo.getSelectedItem());

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    getActionMap().put(REFRESH, refreshAction);
    getInputMap().put(ks, REFRESH);

    JPopupMenu popup = new JPopupMenu();
    popup.add(new JMenuItem(new EnableStatsAction()));
    popup.add(new JMenuItem(new EnableAllStatsAction()));
    popup.add(new JMenuItem(new DisableStatsAction()));
    popup.add(new JMenuItem(new DisableAllStatsAction()));
    popup.addSeparator();
    popup.add(new JMenuItem(new GatherStackTrace()));
    popup.add(new JMenuItem(new GatherAllStackTraces()));
    popup.addSeparator();
    popup.add(new JMenuItem(refreshAction));
    m_popupListener.setPopupMenu(popup);
  }

  private void setType(String type) {
    lockStatsMBean.enableLockStatistics();

    if (type == HOLDER_TYPE_HELD || type == HOLDER_TYPE_WAITING_LOCKS) {
      m_lockTableModel = new LockHolderTableModel(type);
      m_lockTable = m_lockHolderTable;
    } else {
      m_lockTableModel = new LockStatTableModel(type);
      m_lockTable = m_lockStatTable;
    }
    m_lockTable.setModel(m_lockTableModel);
    m_popupListener.setTarget(m_lockTable);
    m_tableScroller.setViewportView(m_lockTable);
  }

  private int getMaxLocks() {
    Object o = m_countSpinner.getValue();
    if (o instanceof Integer) { return ((Integer) o).intValue(); }
    return DEFAULT_LOCK_COUNT;
  }

  private int getTraceDepth() {
    Object o = m_traceDepthSpinner.getValue();
    if (o instanceof Integer) { return ((Integer) o).intValue(); }
    return DEFAULT_TRACE_DEPTH;
  }

  private int getGatherInterval() {
    Object o = m_gatherIntervalSpinner.getValue();
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

  public void disableLockStatistics() {
    lockStatsMBean.disableLockStatistics();
    refresh();
  }
  
  public void enableLockStatistics() {
    lockStatsMBean.enableLockStatistics();
    refresh();
  }

  public class EnableStatsAction extends XAbstractAction {
    EnableStatsAction() {
      super("Enable stats");
    }

    public void actionPerformed(ActionEvent ae) {
      LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
      if (wrapper != null) {
        String id = wrapper.getLockID();
        lockStatsMBean.enableClientStat(id, getTraceDepth(), getGatherInterval());
      }
    }
  }

  public class EnableAllStatsAction extends XAbstractAction {
    EnableAllStatsAction() {
      super("Enable all stats");
    }

    public void actionPerformed(ActionEvent ae) {
      int count = m_lockTableModel.getRowCount();
      int traceDepth = getTraceDepth();
      int gatherInterval = getGatherInterval();
      for (int i = 0; i < count; i++) {
        LockElementWrapper wrapper = m_lockTable.getWrapperAt(i);
        String id = wrapper.getLockID();
        lockStatsMBean.enableClientStat(id, traceDepth, gatherInterval);
      }
    }
  }

  public class DisableStatsAction extends XAbstractAction {
    DisableStatsAction() {
      super("Disable stats");
    }

    public void actionPerformed(ActionEvent ae) {
      LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
      if (wrapper != null) {
        String id = wrapper.getLockID();
        lockStatsMBean.disableClientStat(id);
        wrapper.setStackTrace(null);
      }
    }
  }

  public class DisableAllStatsAction extends XAbstractAction {
    DisableAllStatsAction() {
      super("Disable all stats");
    }

    public void actionPerformed(ActionEvent ae) {
      int count = m_lockTableModel.getRowCount();
      for (int i = 0; i < count; i++) {
        LockElementWrapper wrapper = m_lockTable.getWrapperAt(i);
        String id = wrapper.getLockID();
        lockStatsMBean.disableClientStat(id);
        wrapper.setStackTrace(null);
      }
    }
  }

  public class GatherStackTrace extends XAbstractAction {
    GatherStackTrace() {
      super("Gather stack trace");
    }

    public void actionPerformed(ActionEvent ae) {
      LockElementWrapper wrapper = m_lockTable.getSelectedWrapper();
      if (wrapper != null) {
        gatherTrace(wrapper, true);
      }
    }
  }

  private void gatherTrace(LockElementWrapper wrapper, boolean toConsole) {
    String id = wrapper.getLockID();
    Collection<LockStackTracesStat> c = lockStatsMBean.getStackTraces(id);
    Iterator<LockStackTracesStat> iter = c.iterator();
    while (iter.hasNext()) {
      LockStackTracesStat sts = iter.next();
      if(toConsole) AdminClient.getContext().log(sts.toString());
      List<TCStackTraceElement> stackTraces = sts.getStackTraces();
      if(!stackTraces.isEmpty()) {
        int size = stackTraces.size();
        int last = size-1;
        TCStackTraceElement elem = stackTraces.get(last);
        StringBuffer sb = new StringBuffer("<html>");
        for(StackTraceElement ste : elem.getStackTraceElements()) {
          String s = ste.toString();
          sb.append("<p>");
          sb.append(s);
          sb.append("</p>\n");
        }
        if(last > 0) {
          sb.append("<p>");
          sb.append("</p>");
          sb.append("<p>");
          sb.append(last + " others...");
          sb.append("</p>");
        }
        sb.append("</html>");
        wrapper.setStackTrace(sb.toString());
      }
    }
  }
  
  public class GatherAllStackTraces extends XAbstractAction {
    GatherAllStackTraces() {
      super("Gather all stack traces");
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

    public void mouseDragged(MouseEvent e) {/**/}

    public void mouseMoved(MouseEvent e) {
      LockElementWrapper wrapper = getWrapperAt(rowAtPoint(e.getPoint()));
      String tip = null;
      if(wrapper != null) {
        tip = wrapper.getStackTrace();
      }
      setToolTipText(tip);
    }
  }

  static final String[] LOCK_HOLDER_ATTRS = { "LockID", "LockLevel", "NodeID", "ChannelAddr", "ThreadID",
      "WaitTimeInMillis", "HeldTimeInMillis" };

  class LockHolderTableModel extends XObjectTableModel {
    String lockType;

    LockHolderTableModel(String lockType) {
      super(LockHolderWrapper.class, LOCK_HOLDER_ATTRS, LOCK_HOLDER_ATTRS);
      this.lockType = lockType;
      init();
    }

    private Collection getCollection() {
      try {
        Method m = lockStatsMBean.getClass().getMethod("getTop" + lockType, new Class[] { Integer.TYPE });
        return (Collection) m.invoke(lockStatsMBean, new Object[] { Integer.valueOf(getMaxLocks()) });
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
      ArrayList<LockHolderWrapper> l = new ArrayList<LockHolderWrapper>();
      while (i.hasNext()) {
        l.add(new LockHolderWrapper((LockHolder) i.next()));
      }
      return l.toArray(new LockHolderWrapper[0]);
    }
  }

  class LockHolderTable extends LockElementTable {
    LockHolderTable() {
      super();
    }
  }

  static final String[] LOCK_STAT_ATTRS = { "LockID", "NumOfLockRequested", "NumOfLockReleased",
      "NumOfPendingRequests", "NumOfPendingWaiters", "NumOfPingPongRequests" };

  static final String[] LOCK_STAT_COLS  = { "LockID", "LockRequested", "LockReleased", "PendingRequests",
      "PendingWaiters", "PingPongRequests" };

  class LockStatTableModel extends XObjectTableModel {
    String lockType;

    LockStatTableModel(String lockType) {
      super(LockStatWrapper.class, LOCK_STAT_ATTRS, LOCK_STAT_COLS);
      this.lockType = lockType;
      init();
    }

    private Collection getCollection() {
      try {
        Method m = lockStatsMBean.getClass().getMethod("getTop" + lockType, new Class[] { Integer.TYPE });
        return (Collection) m.invoke(lockStatsMBean, new Object[] { Integer.valueOf(getMaxLocks()) });
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
}
