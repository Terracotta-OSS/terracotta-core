/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.Dialog;
import org.dijon.Frame;
import org.dijon.Label;
import org.dijon.Spinner;
import org.dijon.TextArea;
import org.dijon.ToggleButton;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.SearchPanel;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.dso.BasicObjectSetPanel;
import com.tc.admin.dso.locks.ServerLockTableModel.LockSpecWrapper;
import com.tc.admin.model.BasicTcObject;
import com.tc.admin.model.ClusterModel;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterModel;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.lock.stats.LockSpec;
import com.tc.object.ObjectID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreePath;

/**
 * TODO: Retrieve lock stats from ClusterModel instead of going directly through MBean.
 */

public class LocksPanel extends XContainer implements NotificationListener, PropertyChangeListener {
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
  private LockSpecsGetter             fCurrentLockSpecsGetter;
  private JTabbedPane                 fLocksTabbedPane;
  private LockTreeTable               fTreeTable;
  private LockTreeTableModel          fTreeTableModel;
  private SearchPanel                 fClientSearchPanel;
  private XObjectTable                fServerLocksTable;
  private ServerLockTableModel        fServerLockTableModel;
  private SearchPanel                 fServerSearchPanel;
  private TextArea                    fTraceText;
  private Label                       fConfigLabel;
  private TextArea                    fConfigText;
  private InspectLockObjectAction     fInspectLockObjectAction;

  private static Collection<LockSpec> EMPTY_LOCK_SPEC_COLLECTION = new HashSet<LockSpec>();

  private static final int            STATUS_TIMEOUT_SECONDS     = 3;
  private static final int            REFRESH_TIMEOUT_SECONDS    = Integer.MAX_VALUE;

  public LocksPanel(LocksNode locksNode) {
    super();

    fAdminClientContext = AdminClient.getContext();
    fConnectionContext = locksNode.getConnectionContext();
    fLocksNode = locksNode;

    load((ContainerResource) fAdminClientContext.getComponent("LocksPanel"));

    fLockStats = MBeanServerInvocationProxy.newMBeanProxy(fConnectionContext.mbsc, L2MBeanNames.LOCK_STATISTICS,
                                                          LockStatisticsMonitorMBean.class, false);

    // We do this to force an early error if the server we're connecting to is old and doesn't
    // have the LockStatisticsMonitorMBean. DSONode catches the error and doesn't display the LocksNode.
    fLastTraceDepth = fLockStats.getTraceDepth();

    fEnableButton = (ToggleButton) findComponent("EnableButton");
    fEnableButton.addActionListener(new EnablementButtonHandler());

    fDisableButton = (ToggleButton) findComponent("DisableButton");
    fDisableButton.addActionListener(new EnablementButtonHandler());

    fTraceDepthSpinner = (Spinner) findComponent("TraceDepthSpinner");
    fLastTraceDepth = Math.max(0, fLastTraceDepth);
    fTraceDepthSpinner.setModel(new SpinnerNumberModel(Integer.valueOf(fLastTraceDepth), Integer.valueOf(0), null,
                                                       Integer.valueOf(1)));
    fTraceDepthSpinner.addFocusListener(new TraceDepthSpinnerFocusListener());
    fTraceDepthChangeTimer = new Timer(1000, new TraceDepthChangeTimerHandler());
    fTraceDepthChangeTimer.setRepeats(false);
    fTraceDepthSpinnerChangeListener = new TraceDepthSpinnerChangeListener();
    fTraceDepthSpinner.addChangeListener(fTraceDepthSpinnerChangeListener);

    fRefreshButton = (Button) findComponent("RefreshButton");
    fRefreshButton.addActionListener(new RefreshButtonHandler());

    fInspectLockObjectAction = new InspectLockObjectAction();
    TableMouseListener tableMouseListener = new TableMouseListener();

    fLocksTabbedPane = (JTabbedPane) findComponent("LocksTabbedPane");
    fTreeTableModel = new LockTreeTableModel(EMPTY_LOCK_SPEC_COLLECTION);
    fTreeTable = (LockTreeTable) findComponent("LockTreeTable");
    fTreeTable.setTreeTableModel(fTreeTableModel);
    fTreeTable.setPreferences(fAdminClientContext.getPrefs().node(fTreeTable.getName()));
    fTreeTable.addTreeSelectionListener(new ClientLockSelectionHandler());
    JPopupMenu clientLocksPopup = new JPopupMenu();
    clientLocksPopup.add(fInspectLockObjectAction);
    fTreeTable.setPopupMenu(clientLocksPopup);
    fTreeTable.addMouseListener(tableMouseListener);

    ActionListener findNextAction = new FindNextHandler();
    ActionListener findPreviousAction = new FindPreviousHandler();

    fClientSearchPanel = (SearchPanel) findComponent("ClientLocksSearchPanel");
    fClientSearchPanel.setHandlers(findNextAction, findPreviousAction);

    fServerLocksTable = (XObjectTable) findComponent("ServerLocksTable");
    fServerLockTableModel = new ServerLockTableModel(EMPTY_LOCK_SPEC_COLLECTION);
    fServerLocksTable.setModel(fServerLockTableModel);
    fServerLocksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    fServerLocksTable.getSelectionModel().addListSelectionListener(new ServerLockSelectionHandler());
    JPopupMenu serverLocksPopup = new JPopupMenu();
    serverLocksPopup.add(fInspectLockObjectAction);
    fServerLocksTable.setPopupMenu(serverLocksPopup);
    fServerLocksTable.addMouseListener(tableMouseListener);

    fServerSearchPanel = (SearchPanel) findComponent("ServerLocksSearchPanel");
    fServerSearchPanel.setHandlers(findNextAction, findPreviousAction);

    fTraceText = (TextArea) findComponent("TraceText");
    fConfigLabel = (Label) findComponent("ConfigLabel");
    fConfigText = (TextArea) findComponent("ConfigText");

    try {
      fConnectionContext.addNotificationListener(L2MBeanNames.LOCK_STATISTICS, this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    fAdminClientContext.execute(new LocksPanelEnabledWorker());
    locksNode.getClusterModel().addPropertyChangeListener(this);
  }

  public void propertyChange(PropertyChangeEvent evt) {
    if (IClusterModel.PROP_ACTIVE_SERVER.equals(evt.getPropertyName())) {
      if (((IClusterModel) evt.getSource()).getActiveServer() != null) {
        if (fAdminClientContext != null) {
          fAdminClientContext.execute(new NewConnectionContextWorker());
        }
      }
    }
  }

  private class TableMouseListener extends MouseAdapter {
    public void mousePressed(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int row = table.rowAtPoint(e.getPoint());
      if (row != -1) {
        table.setRowSelectionInterval(row, row);
      }
    }
  }

  private class InspectLockObjectAction extends XAbstractAction {
    private long fObjectID;

    InspectLockObjectAction() {
      super("Inspect lock object");
    }

    public void actionPerformed(ActionEvent ae) {
      ObjectID oid = new ObjectID(fObjectID);
      try {
        int maxFields = ConnectionContext.DSO_SMALL_BATCH_SIZE;
        ManagedObjectFacade mof = fLocksNode.getClusterModel().lookupFacade(oid, maxFields);
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        Dialog dialog = new Dialog(frame, Long.toString(fObjectID), false);
        ClusterModel clusterModel = (ClusterModel) fLocksNode.getClusterModel();
        BasicTcObject dsoObject = new BasicTcObject(clusterModel, "", mof, mof.getClassName(), null);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(new BasicObjectSetPanel(clusterModel, new IBasicObject[] { dsoObject }));
        dialog.pack();
        dialog.center(frame);
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      } catch (Exception e) {
        fAdminClientContext.log(e);
      }
    }

    void setLockID(LockID lockID) {
      fObjectID = getLockObjectID(lockID);
      setEnabled(fObjectID != -1);
    }
  }

  private static long getLockObjectID(LockID lockID) {
    String s = lockID.asString();
    if (s.charAt(0) == '@') {
      s = s.substring(1);
      try {
        return Long.parseLong(s);
      } catch (NumberFormatException nfe) {
        /**/
      }
    }
    return -1;
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
      if (fCurrentLockSpecsGetter != null) {
        fCurrentLockSpecsGetter.cancel(true);
        fCurrentLockSpecsGetter = null;
      } else {
        refresh();
      }
    }
  }

  private class ClientLockSelectionHandler implements TreeSelectionListener {
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

      LockSpec lockSpec = lockSpecNode.getSpec();
      LockID lockID = lockSpec.getLockID();
      int index = fServerLockTableModel.wrapperIndex(lockID);
      fServerLocksTable.setSelectedRows(new int[] { index });
      Rectangle cellRect = fServerLocksTable.getCellRect(index, 0, false);
      if (cellRect != null) {
        fServerLocksTable.scrollRectToVisible(cellRect);
      }

      fInspectLockObjectAction.setLockID(lockID);
    }
  }

  private class ServerLockSelectionHandler implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      int index = fServerLocksTable.getSelectedRow();
      if (index != -1) {
        LockSpecWrapper lockSpecWrapper = (LockSpecWrapper) fServerLockTableModel.getObjectAt(index);
        LockID lockID = lockSpecWrapper.getLockID();
        TreePath lockNodePath = fTreeTableModel.getLockNodePath(lockID);
        if (lockNodePath != null) {
          fTreeTable.getTree().setSelectionPath(lockNodePath);
          int row = fTreeTable.getTree().getRowForPath(lockNodePath);
          Rectangle cellRect = fTreeTable.getCellRect(row, 0, false);
          if (cellRect != null) {
            fTreeTable.scrollRectToVisible(cellRect);
          }
        }
        fInspectLockObjectAction.setLockID(lockID);
      }
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
      textfield = fClientSearchPanel.getField();
    } else {
      table = fServerLocksTable;
      textfield = fServerSearchPanel.getField();
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

  class TraceDepthSpinnerChangeListener implements ChangeListener, Serializable {
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
    fAdminClientContext.execute(new LockStatsStateWorker(lockStatsEnabled));
  }

  private void setLocksPanelEnabled(boolean enabled) {
    fRefreshButton.setEnabled(enabled);

    fLocksPanelEnabled = enabled;

    fEnableButton.setSelected(enabled);
    fEnableButton.setEnabled(!enabled);

    fDisableButton.setSelected(!enabled);
    fDisableButton.setEnabled(enabled);

    fLocksNode.showProfiling(enabled);
  }

  private void refresh() {
    final String label = fRefreshButton.getText();
    fRefreshButton.setText("Cancel");
    fCurrentLockSpecsGetter = new LockSpecsGetter(label);
    fAdminClientContext.submit(fCurrentLockSpecsGetter);
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
      fRefreshButton.setText("Wait...");
      fRefreshButton.setEnabled(false);
      fCurrentLockSpecsGetter = null;

      Exception e = getException();
      if (e != null) {
        String msg;
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (rootCause instanceof IOException) {
          return;
        } else if (rootCause instanceof TimeoutException) {
          msg = "timed-out after '" + REFRESH_TIMEOUT_SECONDS + "' seconds";
        } else if (rootCause instanceof CancellationException) {
          msg = "cancelled";
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
    fAdminClientContext.execute(new TraceDepthWorker(traceDepth));
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
    IClusterModel clusterModel = fLocksNode.getClusterModel();
    if (clusterModel != null) {
      clusterModel.removePropertyChangeListener(this);
    }

    super.tearDown();

    try {
      fConnectionContext.removeNotificationListener(L2MBeanNames.LOCK_STATISTICS, this);
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
    fClientSearchPanel = null;
    fServerLocksTable = null;
    fServerLockTableModel = null;
    fServerSearchPanel = null;
    fTraceText = null;
    fConfigLabel = null;
    fConfigText = null;
  }
}
