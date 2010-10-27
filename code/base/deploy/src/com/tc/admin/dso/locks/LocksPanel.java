/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.ConnectionContext;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.SearchPanel;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XFrame;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSpinner;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.dso.BasicObjectSetPanel;
import com.tc.admin.dso.locks.ServerLockTableModel.LockSpecWrapper;
import com.tc.admin.model.BasicTcObject;
import com.tc.admin.model.ClusterModel;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.management.lock.stats.LockSpec;
import com.tc.object.ObjectID;
import com.tc.object.locks.DsoLockID;
import com.tc.object.locks.LockID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreePath;

public class LocksPanel extends XContainer implements PropertyChangeListener {
  private IAdminClientContext           adminClientContext;
  private LocksNode                     fLocksNode;
  private JToggleButton                 fEnableButton;
  private JToggleButton                 fDisableButton;
  private XSpinner                      fTraceDepthSpinner;
  private ChangeListener                fTraceDepthSpinnerChangeListener;
  private Timer                         fTraceDepthChangeTimer;
  private int                           fLastTraceDepth;
  private XButton                       fRefreshButton;
  private LockSpecsGetter               fCurrentLockSpecsGetter;
  private JTabbedPane                   fLocksTabbedPane;
  private LockTreeTable                 fTreeTable;
  private LockTreeTableModel            fTreeTableModel;
  private ClientLockSelectionHandler    fClientLockSelectionHandler;
  private SearchPanel                   fClientSearchPanel;
  private XObjectTable                  fServerLocksTable;
  private ServerLockTableModel          fServerLockTableModel;
  private ServerLockSelectionHandler    fServerLockSelectionHandler;
  private SearchPanel                   fServerSearchPanel;
  private XTextArea                     fTraceText;
  private TitledBorder                  fConfigBorder;
  private XTextArea                     fConfigText;
  private final InspectLockObjectAction fInspectLockObjectAction;

  private static Collection<LockSpec>   EMPTY_LOCK_SPEC_COLLECTION = new HashSet<LockSpec>();

  private static final int              STATUS_TIMEOUT_SECONDS     = Integer.MAX_VALUE;
  private static final int              REFRESH_TIMEOUT_SECONDS    = Integer.MAX_VALUE;

  public LocksPanel(IAdminClientContext adminClientContext, LocksNode locksNode) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    fLocksNode = locksNode;

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    topPanel.add(new XLabel("Enable lock profiling:"), gbc);
    gbc.gridx++;

    topPanel.add(fDisableButton = new JToggleButton("Off"), gbc);
    fDisableButton.addActionListener(new EnablementButtonHandler());
    gbc.gridx++;

    topPanel.add(fEnableButton = new JToggleButton("On"), gbc);
    fEnableButton.addActionListener(new EnablementButtonHandler());
    gbc.gridx++;

    topPanel.add(new XLabel("Trace depth:"), gbc);
    gbc.gridx++;
    topPanel.add(fTraceDepthSpinner = new XSpinner(), gbc);
    fLastTraceDepth = Math.max(0, fLastTraceDepth);
    fTraceDepthSpinner.setModel(new SpinnerNumberModel(Integer.valueOf(fLastTraceDepth), Integer.valueOf(0), Integer
        .valueOf(999), Integer.valueOf(1)));
    fTraceDepthSpinner.addFocusListener(new TraceDepthSpinnerFocusListener());
    fTraceDepthChangeTimer = new Timer(1000, new TraceDepthChangeTimerHandler());
    fTraceDepthChangeTimer.setRepeats(false);
    fTraceDepthSpinnerChangeListener = new TraceDepthSpinnerChangeListener();
    fTraceDepthSpinner.addChangeListener(fTraceDepthSpinnerChangeListener);
    gbc.gridx++;

    topPanel.add(fRefreshButton = new XButton(adminClientContext.getString("refresh")), gbc);
    fRefreshButton.addActionListener(new RefreshButtonHandler());

    add(topPanel, BorderLayout.NORTH);

    fLocksTabbedPane = new XTabbedPane();
    TableMouseListener tableMouseListener = new TableMouseListener();
    fInspectLockObjectAction = new InspectLockObjectAction();

    /** Clients * */
    ActionListener findNextAction = new FindNextHandler();
    ActionListener findPreviousAction = new FindPreviousHandler();

    XContainer clientsTop = new XContainer(new BorderLayout());
    clientsTop.add(new XScrollPane(fTreeTable = new LockTreeTable()), BorderLayout.CENTER);
    fTreeTableModel = new LockTreeTableModel(adminClientContext, EMPTY_LOCK_SPEC_COLLECTION);
    fTreeTable.setTreeTableModel(fTreeTableModel);
    fTreeTable.setPreferences(adminClientContext.getPrefs().node("LockTreeTable"));
    fClientLockSelectionHandler = new ClientLockSelectionHandler();
    fTreeTable.addTreeSelectionListener(fClientLockSelectionHandler);
    JPopupMenu clientLocksPopup = new JPopupMenu();
    clientLocksPopup.add(fInspectLockObjectAction);
    fTreeTable.setPopupMenu(clientLocksPopup);
    fTreeTable.addMouseListener(tableMouseListener);
    clientsTop.add(fClientSearchPanel = new SearchPanel(adminClientContext), BorderLayout.SOUTH);
    fClientSearchPanel.setHandlers(findNextAction, findPreviousAction);

    XContainer tracePanel = new XContainer(new BorderLayout());
    tracePanel.add(new XScrollPane(fTraceText = new XTextArea()));
    fTraceText.setEditable(false);
    tracePanel.setBorder(BorderFactory.createTitledBorder("Trace"));

    XContainer configPanel = new XContainer(new BorderLayout());
    configPanel.add(new XScrollPane(fConfigText = new XTextArea()));
    fConfigText.setEditable(false);
    configPanel.setBorder(fConfigBorder = BorderFactory.createTitledBorder("Config"));

    XSplitPane bottomSplitter = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT, tracePanel, configPanel);
    bottomSplitter.setDefaultDividerLocation(0.5);
    bottomSplitter.setPreferences(adminClientContext.getPrefs().node("LocksPanel/BottomSplit"));

    XSplitPane mainSplitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, clientsTop, bottomSplitter);
    mainSplitter.setDefaultDividerLocation(0.66);
    mainSplitter.setPreferences(adminClientContext.getPrefs().node("LocksPanel/TopSplit"));

    fLocksTabbedPane.addTab("Clients", mainSplitter);

    /** Server * */
    XContainer serverPanel = new XContainer(new BorderLayout());
    fServerLocksTable = new ServerLocksTable();
    fServerLockTableModel = new ServerLockTableModel(adminClientContext, EMPTY_LOCK_SPEC_COLLECTION);
    fServerLocksTable.setModel(fServerLockTableModel);
    fServerLocksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    fServerLockSelectionHandler = new ServerLockSelectionHandler();
    fServerLocksTable.getSelectionModel().addListSelectionListener(fServerLockSelectionHandler);
    JPopupMenu serverLocksPopup = new JPopupMenu();
    serverLocksPopup.add(fInspectLockObjectAction);
    fServerLocksTable.setPopupMenu(serverLocksPopup);
    fServerLocksTable.addMouseListener(tableMouseListener);
    serverPanel.add(new XScrollPane(fServerLocksTable));

    serverPanel.add(fServerSearchPanel = new SearchPanel(adminClientContext), BorderLayout.SOUTH);
    fClientSearchPanel.setHandlers(findNextAction, findPreviousAction);

    fLocksTabbedPane.addTab("Servers", serverPanel);

    add(fLocksTabbedPane, BorderLayout.CENTER);

    IClusterModel clusterModel = getClusterModel();
    clusterModel.addPropertyChangeListener(this);
    if (clusterModel.isReady()) {
      IServerGroup[] grps = clusterModel.getServerGroups();
      for (IServerGroup grp : grps) {
        grp.addPropertyChangeListener(this);
        IServer server = grp.getActiveServer();
        if (server != null) {
          server.addPropertyChangeListener(this);
        }
      }
      adminClientContext.execute(new LocksPanelEnabledWorker());
    }
  }

  private synchronized IClusterModel getClusterModel() {
    return fLocksNode != null ? fLocksNode.getClusterModel() : null;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel == null) { return; }

    String prop = evt.getPropertyName();
    if (IClusterModelElement.PROP_READY.equals(prop)) {
      if (theClusterModel.isReady()) {
        IServerGroup[] grps = theClusterModel.getServerGroups();
        for (IServerGroup grp : grps) {
          grp.addPropertyChangeListener(this);
          IServer server = grp.getActiveServer();
          if (server != null) {
            server.addPropertyChangeListener(this);
          }
        }

        adminClientContext.execute(new LocksPanelEnabledWorker());
      }
    } else if (IServerGroup.PROP_ACTIVE_SERVER.equals(prop)) {
      IServer newActive = (IServer) evt.getNewValue();
      IServer oldActive = (IServer) evt.getOldValue();
      if (newActive != null) {
        if (adminClientContext != null) {
          adminClientContext.execute(new NewConnectionContextWorker());
        }
      }

      if (oldActive != null) {
        oldActive.removePropertyChangeListener(this);
      }

      if (newActive != null) {
        newActive.addPropertyChangeListener(this);
      }
    } else if (IServer.PROP_LOCK_STATS_TRACE_DEPTH.equals(prop)) {
      IServerGroup[] grps = theClusterModel.getServerGroups();
      int newTraceDepth = 0;
      for (IServerGroup grp : grps) {
        newTraceDepth = Math.max(newTraceDepth, grp.getActiveServer().getLockProfilerTraceDepth());
      }
      if (fLastTraceDepth != newTraceDepth) {
        fLastTraceDepth = newTraceDepth;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ((SpinnerNumberModel) fTraceDepthSpinner.getModel()).setValue(Integer.valueOf(fLastTraceDepth));
          }
        });
      }
    } else if (IServer.PROP_LOCK_STATS_ENABLED.equals(prop)) {
      boolean enabledTemp = true;
      IServerGroup[] grps = theClusterModel.getServerGroups();
      for (IServerGroup grp : grps) {
        enabledTemp = grp.getActiveServer().isLockProfilingEnabled() && enabledTemp;
      }
      final boolean enabled = enabledTemp;
      if (enabled != fRefreshButton.isEnabled()) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setLocksPanelEnabled(enabled);
          }
        });
      }
    }
  }

  private class TableMouseListener extends MouseAdapter {
    @Override
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
        XFrame frame = (XFrame) SwingUtilities.getAncestorOfClass(XFrame.class, LocksPanel.this);
        JDialog dialog = new JDialog(frame, Long.toString(fObjectID), false);
        ClusterModel clusterModel = (ClusterModel) fLocksNode.getClusterModel();
        BasicTcObject dsoObject = new BasicTcObject(clusterModel, "", mof, mof.getClassName(), null);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(new BasicObjectSetPanel(adminClientContext, new IBasicObject[] { dsoObject }));
        dialog.pack();
        WindowHelper.center(dialog, frame);
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      } catch (Exception e) {
        adminClientContext.log(e);
      }
    }

    void setLockID(LockID lockID) {
      fObjectID = getLockObjectID(lockID);
      setEnabled(fObjectID != -1);
    }
  }

  private static long getLockObjectID(LockID lockID) {
    if (lockID instanceof DsoLockID) {
      return ((DsoLockID) lockID).getObjectID().toLong();
    } else {
      return -1;
    }
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
    @Override
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
      fConfigBorder.setTitle(lockSpecNode.toString());
      populateTraceText(path);

      LockSpec lockSpec = lockSpecNode.getSpec();
      LockID lockID = lockSpec.getLockID();
      int index = fServerLockTableModel.wrapperIndex(lockID);
      fServerLocksTable.getSelectionModel().removeListSelectionListener(fServerLockSelectionHandler);
      fServerLocksTable.setSelectedRows(new int[] { index });
      Rectangle cellRect = fServerLocksTable.getCellRect(index, 0, false);
      if (cellRect != null) {
        fServerLocksTable.scrollRectToVisible(cellRect);
      }
      fServerLocksTable.getSelectionModel().addListSelectionListener(fServerLockSelectionHandler);

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
          fTreeTable.removeTreeSelectionListener(fClientLockSelectionHandler);
          fTreeTable.getTree().setSelectionPath(lockNodePath);
          int row = fTreeTable.getTree().getRowForPath(lockNodePath);
          Rectangle cellRect = fTreeTable.getCellRect(row, 0, false);
          if (cellRect != null) {
            fTreeTable.scrollRectToVisible(cellRect);
          }
          fTreeTable.addTreeSelectionListener(fClientLockSelectionHandler);
        }
        fInspectLockObjectAction.setLockID(lockID);
      }
    }
  }

  private class LocksPanelEnabledWorker extends BasicWorker<Boolean> {
    private LocksPanelEnabledWorker() {
      super(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          IClusterModel theClusterModel = getClusterModel();
          boolean enabledTemp = true;
          IServerGroup[] grps = theClusterModel.getServerGroups();
          for (IServerGroup grp : grps) {
            IServer server = grp.getActiveServer();
            enabledTemp = server != null ? server.isLockProfilingEnabled() && enabledTemp : false;
          }
          return enabledTemp;
        }
      }, STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void finished() {
      if (tornDown.get()) { return; }
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
        adminClientContext.log(new Date() + ": Lock profiler: unable to determine statistics subsystem status: " + msg);
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
          IClusterModel theClusterModel = getClusterModel();
          IServerGroup[] grps = theClusterModel.getServerGroups();
          int newTraceDepth = 0;
          boolean enabledTemp = true;
          for (IServerGroup grp : grps) {
            IServer activeServer = grp.getActiveServer();
            if (activeServer != null) {
              newTraceDepth = Math.max(newTraceDepth, activeServer.getLockProfilerTraceDepth());
              enabledTemp = activeServer.isLockProfilingEnabled() && enabledTemp;
            }
          }
          fLastTraceDepth = newTraceDepth;

          return enabledTemp;
        }
      }, STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected void finished() {
      if (tornDown.get()) { return; }
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
        adminClientContext.log(new Date() + ": Lock profiler: unable to determine statistics subsystem status: " + msg);
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
    fConfigBorder.setTitle("Config");
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
          IClusterModel theClusterModel = getClusterModel();
          IServerGroup[] grps = theClusterModel.getServerGroups();
          for (IServerGroup grp : grps) {
            grp.getActiveServer().setLockProfilingEnabled(lockStatsEnabled);
          }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      if (tornDown.get()) { return; }
      Exception e = getException();
      if (e != null) {
        adminClientContext.log(e);
      }

      // Wait for JMX notification to update display
    }
  }

  private void toggleLocksPanelEnabled() {
    IServerGroup[] grps = getClusterModel().getServerGroups();
    for (IServerGroup grp : grps) {
      if (grp.getActiveServer() == null) return;
    }

    boolean lockStatsEnabled = true;
    for (IServerGroup grp : grps) {
      lockStatsEnabled = lockStatsEnabled && grp.getActiveServer().isLockProfilingEnabled();
    }

    lockStatsEnabled = lockStatsEnabled ? false : true;
    adminClientContext.execute(new LockStatsStateWorker(lockStatsEnabled));
  }

  private void setLocksPanelEnabled(boolean enabled) {
    fRefreshButton.setEnabled(enabled);

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
    adminClientContext.submit(fCurrentLockSpecsGetter);
  }

  class LockSpecsGetter extends BasicWorker<Collection<LockSpec>> {
    private final String fRefreshButtonLabel;

    LockSpecsGetter(String refreshButtonLabel) {
      super(new Callable<Collection<LockSpec>>() {
        public Collection<LockSpec> call() throws Exception {
          IServerGroup[] grps = getClusterModel().getServerGroups();
          Collection<LockSpec> c = null;
          for (IServerGroup grp : grps) {
            IServer server = grp.getActiveServer();
            if (server != null) {
              if (c == null) {
                c = server.getLockSpecs();
              } else {
                c.addAll(server.getLockSpecs());
              }
            }
          }

          if (c != null) { return c; }
          return Collections.emptySet();
        }
      }, REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      fRefreshButtonLabel = refreshButtonLabel;
    }

    @Override
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
        adminClientContext.log(new Date() + ": Lock profiler: failed to refresh: " + msg);
      } else {
        Collection<LockSpec> lockSpecs = getResult();

        fTreeTable.setTreeTableModel(fTreeTableModel = new LockTreeTableModel(adminClientContext, lockSpecs));
        fTreeTable.sort();

        fServerLocksTable.setModel(fServerLockTableModel = new ServerLockTableModel(adminClientContext, lockSpecs));
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
          IServerGroup[] grps = getClusterModel().getServerGroups();
          for (IServerGroup grp : grps) {
            grp.getActiveServer().setLockProfilerTraceDepth(fLastTraceDepth = traceDepth);
          }

          return null;
        }
      });
    }

    @Override
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
    adminClientContext.execute(new TraceDepthWorker(traceDepth));
  }

  private int getTraceDepth() {
    return getSpinnerValue(fTraceDepthSpinner);
  }

  private final AtomicBoolean tornDown = new AtomicBoolean(false);

  @Override
  public synchronized void tearDown() {
    if (!tornDown.compareAndSet(false, true)) { return; }

    IClusterModel clusterModel = getClusterModel();
    if (clusterModel != null) {
      clusterModel.removePropertyChangeListener(this);
      IServer activeCoord = clusterModel.getActiveCoordinator();
      if (activeCoord != null) {
        activeCoord.removePropertyChangeListener(this);
      }
    }

    super.tearDown();

    adminClientContext = null;
    fLocksNode = null;
    fEnableButton = null;
    fDisableButton = null;
    fTraceDepthSpinner = null;
    fTraceDepthSpinnerChangeListener = null;
    fTraceDepthChangeTimer = null;
    fRefreshButton = null;
    fLocksTabbedPane = null;
    fTreeTable = null;
    fTreeTableModel = null;
    fClientLockSelectionHandler = null;
    fClientSearchPanel = null;
    fServerLocksTable = null;
    fServerLockTableModel = null;
    fServerLockSelectionHandler = null;
    fServerSearchPanel = null;
    fTraceText = null;
    fConfigBorder = null;
    fConfigText = null;
  }
}
