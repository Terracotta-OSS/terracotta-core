/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.Label;
import org.dijon.Spinner;
import org.dijon.TextArea;
import org.dijon.ToggleButton;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.lock.stats.LockSpec;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreePath;

public class LocksPanel extends XContainer implements NotificationListener {
  private AdminClientContext          fAdminClientContext;
  private ConnectionContext           fConnectionContext;
  private LocksNode                   fLocksNode;
  private LockStatisticsMonitorMBean  fLockStats;
  private ToggleButton                fEnableButton;
  private ToggleButton                fDisableButton;
  private boolean                     fLocksPanelEnabled;
  private Spinner                     fTraceDepthSpinner;
  private ChangeListener              fTraceDepthSpinnerChangeListener;
  private Timer                       fTraceDepthChangeTimer;
  private int                         fLastTraceDepth;
  private Button                      fRefreshButton;
  private JTabbedPane                 fLocksTabbedPane;
  private LockTreeTable               fTreeTable;
  private LockTreeTableModel          fTreeTableModel;
  private JTextField                  fClientLocksFindField;
  private JButton                     fClientLocksFindNextButton;
  private JButton                     fClientLocksFindPreviousButton;
  private XObjectTable                fServerLocksTable;
  private ServerLockTableModel        fServerLockTableModel;
  private JTextField                  fServerLocksFindField;
  private JButton                     fServerLocksFindNextButton;
  private JButton                     fServerLocksFindPreviousButton;
  private TextArea                    fTraceText;
  private Label                       fConfigLabel;
  private TextArea                    fConfigText;

  private static Collection<LockSpec> EMPTY_LOCK_SPEC_COLLECTION = new HashSet<LockSpec>();

  private static final int            STATUS_TIMEOUT_SECONDS     = 3;
  private static final int            REFRESH_TIMEOUT_SECONDS    = 5;

  public LocksPanel(LocksNode locksNode) {
    super();

    fAdminClientContext = AdminClient.getContext();
    fConnectionContext = locksNode.getConnectionContext();
    fLocksNode = locksNode;

    load((ContainerResource) fAdminClientContext.topRes.getComponent("LocksPanel"));

    fLockStats = (LockStatisticsMonitorMBean) MBeanServerInvocationProxy
        .newProxyInstance(fConnectionContext.mbsc, L2MBeanNames.LOCK_STATISTICS, LockStatisticsMonitorMBean.class,
                          false);

    // We do this to force an early error if the server we're connecting to is old and doesn't
    // have the LockStatisticsMonitorMBean. DSONode catches the error and doesn't display the LocksNode.
    fLastTraceDepth = fLockStats.getTraceDepth();

    fEnableButton = (ToggleButton) findComponent("EnableButton");
    fEnableButton.addActionListener(new EnablementButtonHandler());

    fDisableButton = (ToggleButton) findComponent("DisableButton");
    fDisableButton.addActionListener(new EnablementButtonHandler());

    fTraceDepthSpinner = (Spinner) findComponent("TraceDepthSpinner");
    ((SpinnerNumberModel) fTraceDepthSpinner.getModel()).setValue(Integer.valueOf(fLastTraceDepth));
    fTraceDepthSpinner.addFocusListener(new TraceDepthSpinnerFocusListener());
    fTraceDepthChangeTimer = new Timer(1000, new TraceDepthChangeTimerHandler());
    fTraceDepthChangeTimer.setRepeats(false);
    fTraceDepthSpinnerChangeListener = new TraceDepthSpinnerChangeListener();
    fTraceDepthSpinner.addChangeListener(fTraceDepthSpinnerChangeListener);

    fRefreshButton = (Button) findComponent("RefreshButton");
    fRefreshButton.addActionListener(new RefreshButtonHandler());

    fLocksTabbedPane = (JTabbedPane) findComponent("LocksTabbedPane");
    fTreeTableModel = new LockTreeTableModel(EMPTY_LOCK_SPEC_COLLECTION);
    fTreeTable = (LockTreeTable) findComponent("LockTreeTable");
    fTreeTable.setTreeTableModel(fTreeTableModel);
    fTreeTable.setPreferences(fAdminClientContext.prefs.node("LockTreeTable"));
    fTreeTable.addTreeSelectionListener(new LockSelectionHandler());

    ActionListener findNextAction = new FindNextHandler();
    ActionListener findPreviousAction = new FindPreviousHandler();

    fClientLocksFindField = (JTextField) findComponent("ClientLocksFindField");
    fClientLocksFindField.addActionListener(findNextAction);
    fClientLocksFindNextButton = (JButton) findComponent("ClientLocksFindNextButton");
    fClientLocksFindNextButton.addActionListener(findNextAction);
    fClientLocksFindPreviousButton = (JButton) findComponent("ClientLocksFindPreviousButton");
    fClientLocksFindPreviousButton.addActionListener(findPreviousAction);

    fServerLocksTable = (XObjectTable) findComponent("ServerLocksTable");
    fServerLockTableModel = new ServerLockTableModel(EMPTY_LOCK_SPEC_COLLECTION);
    fServerLocksTable.setModel(fServerLockTableModel);

    fServerLocksFindField = (JTextField) findComponent("ServerLocksFindField");
    fServerLocksFindField.addActionListener(findNextAction);
    fServerLocksFindNextButton = (JButton) findComponent("ServerLocksFindNextButton");
    fServerLocksFindNextButton.addActionListener(findNextAction);
    fServerLocksFindPreviousButton = (JButton) findComponent("ServerLocksFindPreviousButton");
    fServerLocksFindPreviousButton.addActionListener(findPreviousAction);

    fTraceText = (TextArea) findComponent("TraceText");
    fConfigLabel = (Label) findComponent("ConfigLabel");
    fConfigText = (TextArea) findComponent("ConfigText");

    try {
      fConnectionContext.addNotificationListener(L2MBeanNames.LOCK_STATISTICS, this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    fAdminClientContext.executorService.execute(new LocksPanelEnabledWorker());
  }

  private class FindNextHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      doSearch(true);
    }
  }

  private class FindPreviousHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      doSearch(false);
    }
  }

  private class TraceDepthSpinnerFocusListener extends FocusAdapter {
    public void focusLost(FocusEvent e) {
      testSetTraceDepth();
    }
  }

  private class TraceDepthChangeTimerHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      testSetTraceDepth();
    }
  }

  private class EnablementButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      toggleLocksPanelEnabled();
    }
  }

  private class RefreshButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  private class LockSelectionHandler implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      resetSelectedLockDetails();

      TreePath treePath = e.getNewLeadSelectionPath();
      if (treePath == null) return;

      Object[] path = treePath.getPath();
      LockSpecNode lockSpecNode = (LockSpecNode) path[1];
      Object last = path[path.length - 1];
      String text = "";
      if (last instanceof LockTraceElementNode) {
        LockTraceElementNode lastNode = (LockTraceElementNode) last;
        text = lastNode.getConfigText();
      }
      fConfigText.setText(text);
      fConfigLabel.setText(lockSpecNode.toString());
      populateTraceText(path);
    }
  }

  private class LocksPanelEnabledWorker extends BasicWorker<Boolean> {
    private LocksPanelEnabledWorker() {
      super(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          return fLockStats.isLockStatisticsEnabled();
        }
      }, STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public void finished() {
      Exception e = getException();
      if (e != null) {
        String msg;
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (rootCause instanceof IOException) {
          return;
        } else if (rootCause instanceof TimeoutException) {
          msg = "timed-out after '" + STATUS_TIMEOUT_SECONDS + "' seconds";
        } else {
          msg = rootCause.getMessage();
        }
        fAdminClientContext
            .log(new Date() + ": Lock profiler: unable to determine statistics subsystem status: " + msg);
        setLocksPanelEnabled(false);
      } else {
        setLocksPanelEnabled(getResult());
      }
    }
  }

  private class NewConnectionContextWorker extends BasicWorker<Boolean> {
    private NewConnectionContextWorker() {
      super(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          fConnectionContext = fLocksNode.getConnectionContext();
          fLockStats = (LockStatisticsMonitorMBean) MBeanServerInvocationHandler
              .newProxyInstance(fConnectionContext.mbsc, L2MBeanNames.LOCK_STATISTICS,
                                LockStatisticsMonitorMBean.class, false);
          fLastTraceDepth = fLockStats.getTraceDepth();
          fConnectionContext.addNotificationListener(L2MBeanNames.LOCK_STATISTICS, LocksPanel.this);
          return fLockStats.isLockStatisticsEnabled();
        }
      }, STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        String msg;
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (rootCause instanceof IOException) {
          return;
        } else if (rootCause instanceof TimeoutException) {
          msg = "timed-out after '" + STATUS_TIMEOUT_SECONDS + "' seconds";
        } else {
          msg = rootCause.getMessage();
        }
        fAdminClientContext
            .log(new Date() + ": Lock profiler: unable to determine statistics subsystem status: " + msg);
        setLocksPanelEnabled(false);
      } else {
        ((SpinnerNumberModel) fTraceDepthSpinner.getModel()).setValue(Integer.valueOf(fLastTraceDepth));
        setLocksPanelEnabled(getResult());
      }
    }
  }

  void newConnectionContext() {
    fAdminClientContext.executorService.execute(new NewConnectionContextWorker());
  }

  boolean isProfiling() {
    return fEnableButton.isSelected();
  }

  private boolean testSelectMatch(JTable table, String text, int row) {
    String lockLabel = table.getModel().getValueAt(row, 0).toString();
    if (lockLabel.contains(text)) {
      table.setRowSelectionInterval(row, row);
      Rectangle cellRect = table.getCellRect(row, 0, false);
      if (cellRect != null) {
        table.scrollRectToVisible(cellRect);
      }
      return true;
    }
    return false;
  }

  private void doSearch(boolean next) {
    JTable table;
    JTextField textfield;
    if (fLocksTabbedPane.getSelectedIndex() == 0) {
      table = fTreeTable;
      textfield = fClientLocksFindField;
    } else {
      table = fServerLocksTable;
      textfield = fServerLocksFindField;
    }
    findLock(table, textfield.getText().trim(), next);
  }

  private void findLock(JTable table, String text, boolean next) {
    TableModel model = table.getModel();
    int currRow = table.getSelectedRow();
    int rowCount = model.getRowCount();

    if (next) {
      int startRow = (currRow == rowCount - 1) ? 0 : currRow + 1;
      for (int i = startRow; i < rowCount; i++) {
        if (testSelectMatch(table, text, i)) return;
      }
      for (int i = 0; i < currRow; i++) {
        if (testSelectMatch(table, text, i)) return;
      }
    } else {
      int startRow = (currRow == 0) ? rowCount - 1 : currRow - 1;
      for (int i = startRow; i >= 0; i--) {
        if (testSelectMatch(table, text, i)) return;
      }
      for (int i = rowCount - 1; i > currRow; i--) {
        if (testSelectMatch(table, text, i)) return;
      }
    }
    Toolkit.getDefaultToolkit().beep();
  }

  class TraceDepthSpinnerChangeListener implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      fTraceDepthChangeTimer.stop();
      fTraceDepthChangeTimer.start();
    }
  }

  private void resetSelectedLockDetails() {
    fConfigText.setText("");
    fConfigLabel.setText("");
    fTraceText.setText("");
  }

  private void populateTraceText(Object[] nodePath) {
    String nl = System.getProperty("line.separator");
    fTraceText.setText("");
    if (nodePath != null && nodePath.length > 2) {
      for (int i = 2; i < nodePath.length; i++) {
        fTraceText.append(nodePath[i].toString());
        fTraceText.append(nl);
      }
    }
  }

  private class LockStatsStateWorker extends BasicWorker<Void> {
    private LockStatsStateWorker(final boolean lockStatsEnabled) {
      super(new Callable<Void>() {
        public Void call() {
          fLockStats.setLockStatisticsEnabled(lockStatsEnabled);
          return null;
        }
      });
    }

    protected void finished() {
      // Wait for JMX notification to update display
    }
  }

  private void toggleLocksPanelEnabled() {
    boolean lockStatsEnabled = fLocksPanelEnabled ? false : true;
    fAdminClientContext.executorService.execute(new LockStatsStateWorker(lockStatsEnabled));
  }

  private void setLocksPanelEnabled(boolean enabled) {
    fRefreshButton.setEnabled(enabled);

    fLocksPanelEnabled = enabled;

    fEnableButton.setSelected(enabled);
    fEnableButton.setEnabled(!enabled);

    fDisableButton.setSelected(!enabled);
    fDisableButton.setEnabled(enabled);

    fLocksNode.notifyChanged();
  }

  private void refresh() {
    final String label = fRefreshButton.getText();
    fRefreshButton.setText("Wait...");
    fRefreshButton.setEnabled(false);
    fAdminClientContext.executorService.execute(new LockSpecsGetter(label));
  }

  class LockSpecsGetter extends BasicWorker<Collection<LockSpec>> {
    private String fRefreshButtonLabel;

    LockSpecsGetter(String refreshButtonLabel) {
      super(new Callable<Collection<LockSpec>>() {
        public Collection<LockSpec> call() throws Exception {
          return fLockStats.getLockSpecs();
        }
      }, REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      fRefreshButtonLabel = refreshButtonLabel;
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        String msg;
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (rootCause instanceof IOException) {
          return;
        } else if (rootCause instanceof TimeoutException) {
          msg = "timed-out after '" + REFRESH_TIMEOUT_SECONDS + "' seconds";
        } else {
          msg = rootCause.getMessage();
        }
        AdminClient.getContext().log(new Date() + ": Lock profiler: failed to refresh: " + msg);
      } else {
        Collection<LockSpec> lockSpecs = getResult();

        fTreeTable.setTreeTableModel(fTreeTableModel = new LockTreeTableModel(lockSpecs));
        fTreeTable.sort();

        fServerLocksTable.setModel(fServerLockTableModel = new ServerLockTableModel(lockSpecs));
        fServerLocksTable.sort();
      }

      fRefreshButton.setText(fRefreshButtonLabel);
      fRefreshButton.setEnabled(true);
    }
  }

  private int getSpinnerValue(JSpinner spinner) {
    try {
      spinner.commitEdit();
      spinner.setForeground(null);
    } catch (ParseException pe) {
      // Edited value is invalid, spinner.getValue() will return
      // the last valid value, you could revert the spinner to show that:
      JComponent editor = spinner.getEditor();
      if (editor instanceof JSpinner.DefaultEditor) {
        ((JSpinner.DefaultEditor) editor).getTextField().setValue(spinner.getValue());
      }
      spinner.setForeground(Color.red);
    }
    return ((SpinnerNumberModel) spinner.getModel()).getNumber().intValue();
  }

  private class TraceDepthWorker extends BasicWorker<Void> {
    private TraceDepthWorker(final int traceDepth) {
      super(new Callable<Void>() {
        public Void call() {
          fLockStats.setLockStatisticsConfig(fLastTraceDepth = traceDepth, 1);
          return null;
        }
      });
    }

    protected void finished() {
      // Wait for JMX notification to update display
    }
  }

  private void testSetTraceDepth() {
    int newTraceDepth = getTraceDepth();
    if (newTraceDepth != fLastTraceDepth) {
      setTraceDepth(newTraceDepth);
    }
  }

  private void setTraceDepth(int traceDepth) {
    fAdminClientContext.executorService.execute(new TraceDepthWorker(traceDepth));
  }

  private int getTraceDepth() {
    return getSpinnerValue(fTraceDepthSpinner);
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();
    if (type.equals(LockStatisticsMonitorMBean.TRACE_DEPTH)) {
      int newTraceDepth = fLockStats.getTraceDepth();
      if (fLastTraceDepth != newTraceDepth) {
        fLastTraceDepth = newTraceDepth;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ((SpinnerNumberModel) fTraceDepthSpinner.getModel()).setValue(Integer.valueOf(fLastTraceDepth));
          }
        });
      }
    } else if (type.equals(LockStatisticsMonitorMBean.TRACES_ENABLED)) {
      final boolean enabled = fLockStats.isLockStatisticsEnabled();
      if (enabled != fRefreshButton.isEnabled()) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setLocksPanelEnabled(enabled);
          }
        });
      }
    }
  }

  public void tearDown() {
    super.tearDown();

    try {
      fConnectionContext.addNotificationListener(L2MBeanNames.LOCK_STATISTICS, this);
    } catch (Exception e) {
      // ignore
    }

    fAdminClientContext = null;
    fConnectionContext = null;
    fLocksNode = null;
    fLockStats = null;
    fEnableButton = null;
    fDisableButton = null;
    fTraceDepthSpinner = null;
    fTraceDepthSpinnerChangeListener = null;
    fTraceDepthChangeTimer = null;
    fRefreshButton = null;
    fLocksTabbedPane = null;
    fTreeTable = null;
    fTreeTableModel = null;
    fClientLocksFindField = null;
    fClientLocksFindNextButton = null;
    fClientLocksFindPreviousButton = null;
    fServerLocksTable = null;
    fServerLockTableModel = null;
    fServerLocksFindField = null;
    fServerLocksFindNextButton = null;
    fServerLocksFindPreviousButton = null;
    fTraceText = null;
    fConfigLabel = null;
    fConfigText = null;
  }
}
