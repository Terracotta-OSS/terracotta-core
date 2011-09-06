/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlOptions;

import EDU.oswego.cs.dl.util.concurrent.misc.SwingWorker;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.FastFileChooser;
import com.tc.admin.common.ProgressDialog;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XList;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSpinner;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.IClusterStatsListener;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.admin.model.ServerStateListener;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererAlreadyConnectedException;
import com.terracottatech.config.TcStatsConfigDocument;
import com.terracottatech.config.TcStatsConfigDocument.TcStatsConfig;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

public class StatsRecorderPanel extends XContainer implements ClientConnectionListener, IClusterStatsListener {
  private final ApplicationContext   appContext;
  private final IClusterModel        clusterModel;
  private final ClusterListener      clusterListener;
  private final ServerListener       serverListener;

  private JToggleButton              startGatheringStatsButton;
  private JToggleButton              stopGatheringStatsButton;
  private XList                      statsSessionsList;
  private DefaultListModel           statsSessionsListModel;
  private XContainer                 availableStatsArea;
  private HashMap<String, JCheckBox> statsControls;
  private XCheckBox                  selectAllToggle;
  private XSpinner                   samplePeriodSpinner;
  private XButton                    importStatsConfigButton;
  private XButton                    exportStatsConfigButton;
  private String                     currentStatsSessionId;
  private boolean                    isRecording;
  private XButton                    exportStatsButton;
  private XButton                    viewStatsButton;
  private File                       lastExportDir;
  private XButton                    clearStatsSessionButton;
  private XButton                    clearAllStatsSessionsButton;
  private boolean                    haveClientStats;
  private AuthScope                  authScope;

  private final XContainer           mainPanel;
  private final XContainer           messagePanel;
  private final XContainer           errorPanel;

  private XLabel                     messageLabel;
  private XTextArea                  errorText;

  private static final int           DEFAULT_STATS_POLL_PERIOD_SECONDS = 5;
  private static final String        DEFAULT_STATS_CONFIG_FILENAME     = "tc-stats-config.xml";

  public StatsRecorderPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;

    mainPanel = createMainPanel();
    messagePanel = createMessagePanel();
    errorPanel = createErrorPanel();

    add(messagePanel);

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    clusterModel.addServerStateListener(serverListener = new ServerListener());
    if (clusterModel.isReady()) {
      IServer activeCoord = clusterModel.getActiveCoordinator();
      if (activeCoord != null && activeCoord.isClusterStatsSupported()) {
        activeCoord.addClusterStatsListener(this);
        initiateStatsGathererConnectWorker();
      }
    } else {
      messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
    }
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(appContext.getString("initializing"));
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) appContext.getObject("message.label.font"));
    return panel;
  }

  private XContainer createErrorPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(new JScrollPane(errorText = new XTextArea()));
    errorText.setEditable(false);
    return panel;
  }

  private XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(createTopPanel(), BorderLayout.NORTH);
    panel.add(createCenterPanel(), BorderLayout.CENTER);
    panel.add(createBottomPanel(), BorderLayout.SOUTH);
    return panel;
  }

  private XContainer createTopPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(1, 3, 1, 3);

    panel.add(stopGatheringStatsButton = new JToggleButton("Stop recording"), gbc);
    stopGatheringStatsButton.addActionListener(new StopGatheringStatsAction());
    gbc.gridx++;

    panel.add(startGatheringStatsButton = new JToggleButton("Start recording"), gbc);
    startGatheringStatsButton.addActionListener(new StartGatheringStatsAction());
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    panel.add(new XLabel(), gbc);

    return panel;
  }

  private XTabbedPane createCenterPanel() {
    XContainer selectionPanel = new XContainer(new BorderLayout());
    availableStatsArea = new XContainer();
    availableStatsArea.setLayout(new GridLayout(0, 3));
    selectionPanel.add(new XScrollPane(availableStatsArea), BorderLayout.CENTER);

    selectAllToggle = new XCheckBox("Select / deselect all");
    selectAllToggle.setSelected(true);
    selectAllToggle.addActionListener(new SelectAllHandler());
    selectionPanel.add(selectAllToggle, BorderLayout.SOUTH);
    selectionPanel.setBorder(BorderFactory.createTitledBorder("Available statistics"));
    XContainer bottomPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    bottomPanel.add(new XLabel("Sample period (secs.)"), gbc);
    gbc.gridx++;
    bottomPanel.add(samplePeriodSpinner = new XSpinner(), gbc);
    samplePeriodSpinner.setModel(new SpinnerNumberModel(Integer.valueOf(DEFAULT_STATS_POLL_PERIOD_SECONDS), Integer
        .valueOf(1), Integer.valueOf(999), Integer.valueOf(1)));
    gbc.gridx++;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.EAST;

    XContainer buttonPanel = new XContainer(new GridLayout(2, 1, 3, 3));
    buttonPanel.add(importStatsConfigButton = new XButton("Import configuration..."));
    importStatsConfigButton.addActionListener(new ImportStatsConfigHandler());
    buttonPanel.add(exportStatsConfigButton = new XButton("Export configuration..."));
    exportStatsConfigButton.addActionListener(new ExportStatsConfigHandler());
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    bottomPanel.add(buttonPanel, gbc);

    XContainer panel = new XContainer(new BorderLayout());
    panel.add(selectionPanel, BorderLayout.CENTER);
    panel.add(bottomPanel, BorderLayout.SOUTH);

    XTabbedPane tabbedPane = new XTabbedPane();
    tabbedPane.addTab("Configuration", panel);
    return tabbedPane;
  }

  private XContainer createBottomPanel() {
    XContainer panel = new XContainer(new BorderLayout());

    statsSessionsList = new XList();
    panel.add(new XScrollPane(statsSessionsList), BorderLayout.CENTER);
    statsSessionsList.addListSelectionListener(new StatsSessionsListSelectionListener());
    statsSessionsList.setModel(statsSessionsListModel = new DefaultListModel());
    statsSessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    XContainer buttonPanel = new XContainer(new GridLayout(4, 1, 3, 3));
    buttonPanel.add(exportStatsButton = new XButton("Export All Sessions..."));
    exportStatsButton.addActionListener(new ExportStatsHandler());
    buttonPanel.add(clearStatsSessionButton = new XButton("Clear Session"));
    clearStatsSessionButton.addActionListener(new ClearStatsSessionHandler());
    buttonPanel.add(clearAllStatsSessionsButton = new XButton("Clear All Sessions"));
    clearAllStatsSessionsButton.addActionListener(new ClearAllStatsSessionsHandler());
    buttonPanel.add(viewStatsButton = new XButton("View..."));
    viewStatsButton.addActionListener(new ViewStatsSessionsHandler());
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    panel.add(buttonPanel, BorderLayout.EAST);

    panel.setBorder(BorderFactory.createTitledBorder("Statistics capture sessions"));

    return panel;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      if (clusterModel.isReady()) {
        IServer activeCoord = clusterModel.getActiveCoordinator();
        if (activeCoord != null) {
          messageLabel.setText(appContext.getString("initializing"));
          if (activeCoord.isClusterStatsSupported()) {
            activeCoord.addClusterStatsListener(StatsRecorderPanel.this);
            initiateStatsGathererConnectWorker();
          }
          activeCoord.addClientConnectionListener(StatsRecorderPanel.this);
        }
      } else {
        removeAll();
        messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
        add(messagePanel);
        revalidate();
        repaint();
      }
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(StatsRecorderPanel.this);
        oldActive.removeClusterStatsListener(StatsRecorderPanel.this);
      }

      if (newActive != null) {
        if (newActive.isClusterStatsSupported()) {
          newActive.addClusterStatsListener(StatsRecorderPanel.this);
          initiateStatsGathererConnectWorker();
        }
        newActive.addClientConnectionListener(StatsRecorderPanel.this);
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      appContext.log(e);
    }
  }

  private class ServerListener implements ServerStateListener {
    public void serverStateChanged(IServer server, PropertyChangeEvent pce) {
      if (isRecording) {
        String prop = pce.getPropertyName();
        if (prop.equals(IClusterModelElement.PROP_READY)) {
          if (server.isReady() && !server.isActiveCoordinator() && server.isClusterStatsSupported()) {
            java.util.List<String> statList = getSelectedStats();
            String[] stats = statList.toArray(new String[0]);
            long samplePeriodMillis = getSamplePeriodMillis();

            appContext.execute(new JoinStatsGatheringWorker(server, stats, samplePeriodMillis));
          }
        }
      }
    }
  }

  private class JoinStatsGatheringWorker extends BasicWorker<Void> {
    JoinStatsGatheringWorker(final IServer server, final String[] statsToRecord, final long samplePeriodMillis) {
      super(new Callable<Void>() {
        public Void call() {
          server.startupClusterStats();
          server.startClusterStatsSession(currentStatsSessionId, statsToRecord, samplePeriodMillis);
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        showError(e);
      }
    }
  }

  private void initiateStatsGathererConnectWorker() {
    appContext.execute(new StatsGathererConnectWorker());
  }

  class StatsGathererConnectWorker extends BasicWorker<Void> {
    StatsGathererConnectWorker() {
      super(new Callable() {
        public Void call() throws Exception {
          for (IServerGroup serverGroup : clusterModel.getServerGroups()) {
            for (IServer server : serverGroup.getMembers()) {
              if (server.isReady() && server.isClusterStatsSupported()) {
                server.startupClusterStats();
              }
            }
          }
          return null;
        }
      });
    }

    private boolean isAlreadyConnectedException(Throwable t) {
      return ExceptionHelper.getCauseOfType(t, StatisticsGathererAlreadyConnectedException.class) != null;
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        if (isAlreadyConnectedException(e)) {
          gathererConnected();
        } else {
          showError(e);
        }
      }
    }
  }

  private void showError(Throwable t) {
    appContext.log(t);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.close();
    errorText.setText(sw.toString());
    removeAll();
    add(errorPanel);
    revalidate();
    repaint();
  }

  private void gathererConnected() {
    statsSessionsListModel.clear();
    appContext.execute(new GathererConnectedWorker());
  }

  private void gathererDisconnected() {
    clearAuthScope();
  }

  private void gathererReinitialized() {
    clearAuthScope();
  }

  private static class GathererConnectedState {
    private final boolean  fIsCapturing;
    private final String[] fSessions;
    private final String   fActiveStatsSessionId;
    private final String[] fSupportedStats;

    GathererConnectedState(boolean isCapturing, String[] sessions, String activeSessionId, String[] supportedStats) {
      fIsCapturing = isCapturing;
      fSessions = sessions;
      fActiveStatsSessionId = activeSessionId;
      fSupportedStats = supportedStats;
    }

    boolean isCapturing() {
      return fIsCapturing;
    }

    String[] getAllSessions() {
      return fSessions;
    }

    String getActiveStatsSessionId() {
      return fActiveStatsSessionId;
    }

    String[] getSupportedStats() {
      return fSupportedStats;
    }
  }

  class GathererConnectedWorker extends BasicWorker<GathererConnectedState> {
    GathererConnectedWorker() {
      super(new Callable<GathererConnectedState>() {
        public GathererConnectedState call() {
          IServer activeCoord = clusterModel.getActiveCoordinator();
          if (activeCoord != null) {
            boolean isCapturing = activeCoord.isActiveClusterStatsSession();
            String[] allSessions = activeCoord.getAllClusterStatsSessions();
            String activeSessionId = activeCoord.getActiveClusterStatsSession();
            String[] supportedStats = activeCoord.getSupportedClusterStats();

            return new GathererConnectedState(isCapturing, allSessions, activeSessionId, supportedStats);
          }
          return null;
        }
      });
    }

    @Override
    public void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          showError(e);
        }
      } else {
        GathererConnectedState connectedState = getResult();
        if (connectedState == null) return;
        String[] allSessions = connectedState.getAllSessions();
        String[] supportedStats = connectedState.getSupportedStats();
        boolean sessionInProgress = connectedState.isCapturing();

        currentStatsSessionId = connectedState.getActiveStatsSessionId();
        for (String sessionId : allSessions) {
          if (sessionInProgress && sessionId.equals(currentStatsSessionId)) {
            continue;
          }
          statsSessionsListModel.addElement(new StatsSessionListItem(sessionId));
        }
        boolean haveAnySessions = allSessions.length > 0;
        clearAllStatsSessionsButton.setEnabled(haveAnySessions);
        exportStatsButton.setEnabled(haveAnySessions);
        viewStatsButton.setEnabled(haveAnySessions);
        init(sessionInProgress);
        setupStatsConfigPanel(supportedStats);
        removeAll();
        add(mainPanel);
        revalidate();
        repaint();
      }
    }
  }

  private void init(boolean recording) {
    if (recording) {
      showRecordingInProgress();
    } else {
      hideRecordingInProgress();
    }
  }

  private void setRecording(boolean recording) {
    isRecording = recording;

    startGatheringStatsButton.setSelected(recording);
    startGatheringStatsButton.setEnabled(!recording);

    stopGatheringStatsButton.setSelected(!recording);
    stopGatheringStatsButton.setEnabled(recording);

    updateSessionsControls();
  }

  private void showRecordingInProgress() {
    setRecording(true);
  }

  private void hideRecordingInProgress() {
    setRecording(false);
  }

  private void setupStatsConfigPanel(String[] stats) {
    availableStatsArea.removeAll();
    Map<String, Boolean> selectedStates = new HashMap<String, Boolean>();
    if (statsControls == null) {
      statsControls = new HashMap<String, JCheckBox>();
    } else {
      Iterator<String> statIter = statsControls.keySet().iterator();
      while (statIter.hasNext()) {
        String stat = statIter.next();
        JCheckBox control = statsControls.get(stat);
        if (control != null) {
          selectedStates.put(stat, control.isSelected());
        }
      }
      statsControls.clear();
    }
    if (stats != null) {
      for (String stat : stats) {
        JCheckBox control = new JCheckBox();
        control.setText(stat);
        control.setName(stat);
        statsControls.put(stat, control);
        availableStatsArea.add(control);
        Boolean state = selectedStates.get(stat);
        if (state == null) {
          state = selectAllToggle.isSelected();
        }
        control.setSelected(state);
      }
    }
    availableStatsArea.revalidate();
    availableStatsArea.repaint();
  }

  private class SelectAllHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      boolean selected = selectAllToggle.isSelected();
      Iterator<JCheckBox> iter = statsControls.values().iterator();
      while (iter.hasNext()) {
        iter.next().setSelected(selected);
      }
    }
  }

  private long getSamplePeriodMillis() {
    Number samplePeriod = (Number) samplePeriodSpinner.getValue();
    return samplePeriod.longValue() * 1000;
  }

  private java.util.List<String> getSelectedStats() {
    Iterator<String> iter = statsControls.keySet().iterator();
    ArrayList<String> statList = new ArrayList<String>();
    while (iter.hasNext()) {
      String stat = iter.next();
      JCheckBox control = statsControls.get(stat);
      if (control.isSelected()) {
        statList.add(stat);
      }
    }
    return statList;
  }

  private void disableAllStats() {
    Iterator<String> iter = statsControls.keySet().iterator();
    while (iter.hasNext()) {
      JCheckBox control = statsControls.get(iter.next());
      if (control != null) {
        control.setSelected(false);
      }
    }
  }

  private void setSelectedStats(String[] stats) {
    disableAllStats();
    for (String stat : stats) {
      JCheckBox control = statsControls.get(stat);
      if (control != null) {
        control.setSelected(true);
      }
    }
  }

  class StartGatheringStatsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      java.util.List<String> statList = getSelectedStats();
      String[] stats = statList.toArray(new String[0]);
      long samplePeriodMillis = getSamplePeriodMillis();

      appContext.execute(new StartGatheringStatsWorker(stats, samplePeriodMillis));
    }
  }

  class StartGatheringStatsWorker extends BasicWorker<Void> {
    StartGatheringStatsWorker(final String[] statsToRecord, final long samplePeriodMillis) {
      super(new Callable<Void>() {
        public Void call() {
          currentStatsSessionId = new Date().toString();
          for (IServerGroup group : clusterModel.getServerGroups()) {
            for (IServer server : group.getMembers()) {
              if (server.isReady() && server.isClusterStatsSupported()) {
                server.startClusterStatsSession(currentStatsSessionId, statsToRecord, samplePeriodMillis);
              }
            }
          }
          return null;
        }
      });
    }

    @Override
    public void finished() {
      stopGatheringStatsButton.setSelected(false);
    }
  }

  class StopGatheringStatsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      startGatheringStatsButton.setSelected(false);
      appContext.execute(new StopGatheringStatsWorker());
    }
  }

  class StopGatheringStatsWorker extends BasicWorker<Void> {
    StopGatheringStatsWorker() {
      super(new Callable<Void>() {
        public Void call() {
          for (IServerGroup serverGroup : clusterModel.getServerGroups()) {
            for (IServer server : serverGroup.getMembers()) {
              server.endCurrentClusterStatsSession();
            }
          }
          return null;
        }
      });
    }

    @Override
    public void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(e);
      }
    }
  }

  private void updateSessionsControls() {
    boolean haveAnySessions = statsSessionsListModel.getSize() > 0;
    boolean haveSelectedSession = getSelectedSessionId() != null;
    boolean recording = isRecording();
    exportStatsButton.setEnabled(haveAnySessions);
    clearStatsSessionButton.setEnabled(haveSelectedSession);
    clearAllStatsSessionsButton.setEnabled(haveAnySessions && !recording);
    viewStatsButton.setEnabled(haveAnySessions);
  }

  private class StatsSessionsListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      updateSessionsControls();
    }
  }

  private static class StatsSessionListItem {
    private final String fSessionId;

    StatsSessionListItem(String sessionId) {
      fSessionId = sessionId;
    }

    String getSessionId() {
      return fSessionId;
    }

    @Override
    public String toString() {
      return fSessionId;
    }
  }

  public boolean isRecording() {
    return isRecording;
  }

  private String getSelectedSessionId() {
    if (statsSessionsList == null) return null;
    StatsSessionListItem item = (StatsSessionListItem) statsSessionsList.getSelectedValue();
    return item != null ? item.getSessionId() : null;
  }

  private class ImportStatsConfigHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      FastFileChooser chooser = new FastFileChooser();
      if (lastExportDir != null) {
        chooser.setCurrentDirectory(lastExportDir);
      }
      chooser.setDialogTitle("Import statistics configuration");
      chooser.setMultiSelectionEnabled(false);
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), DEFAULT_STATS_CONFIG_FILENAME));
      if (chooser.showOpenDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      if (!file.exists()) {
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, StatsRecorderPanel.this);
        String msg = "File '" + file + "' does not exist.";
        JOptionPane.showMessageDialog(StatsRecorderPanel.this, msg, frame.getTitle(), JOptionPane.WARNING_MESSAGE);
        return;
      }
      lastExportDir = file.getParentFile();
      try {
        TcStatsConfigDocument tcStatsConfigDoc = TcStatsConfigDocument.Factory.parse(file);
        TcStatsConfig tcStatsConfig = tcStatsConfigDoc.getTcStatsConfig();
        if (tcStatsConfig.isSetRetrievalPollPeriod()) {
          samplePeriodSpinner.setValue(tcStatsConfig.getRetrievalPollPeriod().longValue() / 1000);
        }
        if (tcStatsConfig.isSetEnabledStatistics()) {
          setSelectedStats(tcStatsConfig.getEnabledStatistics().getNameArray());
        }
      } catch (RuntimeException re) {
        throw re;
      } catch (Exception e) {
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, StatsRecorderPanel.this);
        String msg = "Unable to parse '" + file.getName() + "' as a Terracotta stats config document";
        JOptionPane.showMessageDialog(StatsRecorderPanel.this, msg, frame.getTitle(), JOptionPane.ERROR_MESSAGE);
        return;
      }
    }
  }

  private class ExportStatsConfigHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      FastFileChooser chooser = new FastFileChooser();
      if (lastExportDir != null) {
        chooser.setCurrentDirectory(lastExportDir);
      }
      chooser.setDialogTitle("Export statistics configuration");
      chooser.setMultiSelectionEnabled(false);
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), DEFAULT_STATS_CONFIG_FILENAME));
      if (chooser.showSaveDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      lastExportDir = file.getParentFile();
      java.util.List<String> statList = getSelectedStats();
      InputStream is = null;
      OutputStream os = null;
      try {
        TcStatsConfigDocument tcStatsConfigDoc = TcStatsConfigDocument.Factory.newInstance();
        TcStatsConfig tcStatsConfig = tcStatsConfigDoc.addNewTcStatsConfig();
        tcStatsConfig.setRetrievalPollPeriod(BigInteger.valueOf(getSamplePeriodMillis()));
        tcStatsConfig.addNewEnabledStatistics().setNameArray(statList.toArray(new String[0]));
        XmlOptions opts = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
        is = tcStatsConfigDoc.newInputStream(opts);
        os = new FileOutputStream(file);
        IOUtils.copy(is, os);
      } catch (IOException ioe) {
        appContext.log(ioe);
      } finally {
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
      }
    }
  }

  private synchronized void clearAuthScope() {
    authScope = null;
  }

  private synchronized AuthScope getAuthScope() {
    if (authScope == null) {
      IServer activeCoord = clusterModel.getActiveCoordinator();
      String host = activeCoord.getHost();
      int dsoPort = activeCoord.getDSOListenPort();
      authScope = new AuthScope(host, dsoPort);
    }
    return authScope;
  }

  private static class ZipFileFilter extends FileFilter {
    @Override
    public boolean accept(File file) {
      return file.isDirectory() || file.getName().endsWith(".zip");
    }

    @Override
    public String getDescription() {
      return "ZIP files";
    }
  }

  class ExportStatsHandler implements ActionListener {
    private UsernamePasswordCredentials credentials;

    private JFileChooser createFileChooser() {
      FastFileChooser chooser = new FastFileChooser();
      if (lastExportDir != null) {
        chooser.setCurrentDirectory(lastExportDir);
      }
      chooser.setDialogTitle("Export statistics");
      chooser.setMultiSelectionEnabled(false);
      chooser.setFileFilter(new ZipFileFilter());
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "tc-stats.zip"));
      return chooser;
    }

    public void actionPerformed(ActionEvent ae) {
      if (statsSessionsListModel.getSize() == 0) return;
      JFileChooser chooser = createFileChooser();
      if (chooser.showSaveDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      final File file = chooser.getSelectedFile();
      lastExportDir = file.getParentFile();
      final List<GetMethod> getList = new ArrayList<GetMethod>();
      try {
        for (IServerGroup group : clusterModel.getServerGroups()) {
          for (IServer server : group.getMembers()) {
            if (!server.isReady() || !server.isClusterStatsSupported()) continue;
            String uri = server.getStatsExportServletURI();
            URL url = new URL(uri);
            HttpClient httpClient = new HttpClient();
            GetMethod get = new GetMethod(url.toString());
            get.setFollowRedirects(true);
            if (credentials != null) {
              httpClient.getState().setCredentials(getAuthScope(), credentials);
              get.setDoAuthentication(true);
            }
            int status = httpClient.executeMethod(get);
            while (status == HttpStatus.SC_UNAUTHORIZED) {
              UsernamePasswordCredentials creds = getCredentials();
              if (creds == null) return;
              if (creds.getUserName().length() == 0 || creds.getPassword().length() == 0) {
                credentials = null;
                continue;
              }
              httpClient = new HttpClient();
              httpClient.getState().setCredentials(getAuthScope(), creds);
              get.setDoAuthentication(true);
              status = httpClient.executeMethod(get);
            }
            if (status != HttpStatus.SC_OK) {
              appContext.log("The http client has encountered a status code other than ok for the url: " + url
                             + " status: " + HttpStatus.getStatusText(status));
              return;
            }
            getList.add(get);
          }
        }
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, StatsRecorderPanel.this);
        final ProgressDialog progressDialog = showProgressDialog(frame, "Exporting statistics to '" + file.getName()
                                                                        + ".' Please wait...");
        progressDialog.addWindowListener(new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent e) {
            new Thread(new StreamCopierRunnable(getList, file, progressDialog)).start();
          }
        });
      } catch (Exception e) {
        appContext.log(e);
        for (GetMethod get : getList) {
          get.releaseConnection();
        }
      }
    }

    private UsernamePasswordCredentials getCredentials() {
      if (credentials != null) { return credentials; }
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, StatsRecorderPanel.this);
      LoginPanel loginPanel = new LoginPanel();
      int answer = JOptionPane.showConfirmDialog(frame, loginPanel, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
      if (answer == JOptionPane.OK_OPTION) {
        credentials = new UsernamePasswordCredentials(loginPanel.getUserName(), loginPanel.getPassword());
      } else {
        credentials = null;
      }
      return credentials;
    }
  }

  class StreamCopierRunnable implements Runnable {
    List<GetMethod> fGetList;
    File            fOutFile;
    ProgressDialog  fProgressDialog;
    File            fTmpFile;
    String          fOutFileName;

    StreamCopierRunnable(List<GetMethod> getList, File outFile, ProgressDialog progressDialog) {
      fGetList = getList;
      fOutFile = outFile;
      fProgressDialog = progressDialog;
    }

    public void run() {
      BufferedWriter outWriter = null;
      boolean ignoreFirstLine = false;

      try {
        fTmpFile = File.createTempFile("foo", null);
        outWriter = new BufferedWriter(new FileWriter(fTmpFile));

        for (GetMethod getMethod : fGetList) {
          BufferedReader inReader = null;

          try {
            boolean isFirstLine = true;
            ZipInputStream zin = new ZipInputStream(getMethod.getResponseBodyAsStream());
            ZipEntry ze = null;

            while ((ze = zin.getNextEntry()) != null) {
              if (fOutFileName == null) {
                fOutFileName = ze.getName();
              }
              inReader = new BufferedReader(new InputStreamReader(zin));
              String line;

              while ((line = inReader.readLine()) != null) {
                if (!isFirstLine || !ignoreFirstLine) {
                  outWriter.write(line);
                  outWriter.newLine();
                }
                isFirstLine = false;
                ignoreFirstLine = true;
              }
            }
          } catch (Exception e) {
            appContext.log(e);
          } finally {
            IOUtils.closeQuietly(inReader);
            getMethod.releaseConnection();
          }
        }
        outWriter.close();
        FileOutputStream fout = new FileOutputStream(fOutFile);
        ZipOutputStream zout = new ZipOutputStream(fout);
        zout.setLevel(9);
        ZipEntry ze = new ZipEntry(fOutFileName);
        FileInputStream fin = new FileInputStream(fTmpFile);
        zout.putNextEntry(ze);
        IOUtils.copy(fin, zout);
        fin.close();
        zout.close();
      } catch (IOException e) {
        appContext.log(e);
      } finally {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            fProgressDialog.setVisible(false);
            appContext.setStatus("Wrote '" + fOutFile.getAbsolutePath() + "'");
          }
        });
      }
    }
  }

  class ClearStatsSessionHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      final StatsSessionListItem item = (StatsSessionListItem) statsSessionsList.getSelectedValue();
      if (item != null) {
        String msg = "Really clear statistics from session '" + item + "?'";
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, StatsRecorderPanel.this);
        int result = JOptionPane.showConfirmDialog(StatsRecorderPanel.this, msg, frame.getTitle(),
                                                   JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
          final ProgressDialog progressDialog = showProgressDialog(frame, "Clearing statistics from session '" + item
                                                                          + ".' Please wait...");
          SwingWorker worker = new SwingWorker() {
            @Override
            public Object construct() throws Exception {
              for (IServerGroup serverGroup : clusterModel.getServerGroups()) {
                for (IServer server : serverGroup.getMembers()) {
                  if (server.isReady() && server.isClusterStatsSupported()) {
                    server.clearClusterStatsSession(item.getSessionId());
                  }
                }
              }
              return null;
            }

            @Override
            public void finished() {
              progressDialog.setVisible(false);
              InvocationTargetException ite = getException();
              if (ite != null) {
                Throwable cause = ite.getCause();
                showError(cause != null ? cause : ite);
                return;
              }
            }
          };
          worker.start();
        }
      }
    }
  }

  class ClearAllStatsSessionsHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String msg = "Really clear all recorded statistics?";
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, StatsRecorderPanel.this);
      int result = JOptionPane.showConfirmDialog(StatsRecorderPanel.this, msg, frame.getTitle(),
                                                 JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        final ProgressDialog progressDialog = showProgressDialog(frame,
                                                                 "Clearing all recorded statistics. Please wait...");
        SwingWorker worker = new SwingWorker() {
          @Override
          public Object construct() throws Exception {
            for (IServerGroup serverGroup : clusterModel.getServerGroups()) {
              for (IServer server : serverGroup.getMembers()) {
                if (server.isReady() && server.isClusterStatsSupported()) {
                  server.clearAllClusterStats();
                }
              }
            }
            return null;
          }

          @Override
          public void finished() {
            progressDialog.setVisible(false);
            InvocationTargetException ite = getException();
            if (ite != null) {
              Throwable cause = ite.getCause();
              showError(cause != null ? cause : ite);
              return;
            }
          }
        };
        worker.start();
      }
    }
  }

  private ProgressDialog showProgressDialog(Frame owner, String msg) {
    ProgressDialog progressDialog = new ProgressDialog(owner, appContext.getString("stats.recorder.node.label"), msg);
    progressDialog.pack();
    WindowHelper.center(progressDialog, StatsRecorderPanel.this);
    progressDialog.setVisible(true);
    return progressDialog;
  }

  protected AdminClientPanel getAdminClientPanel() {
    AdminClientPanel topPanel = (AdminClientPanel) SwingUtilities.getAncestorOfClass(AdminClientPanel.class,
                                                                                     StatsRecorderPanel.this);
    return topPanel;
  }

  class ViewStatsSessionsHandler implements ActionListener, PropertyChangeListener {
    private JFrame svtFrame;
    private Method retrieveMethod;
    private Method setSessionMethod;

    public void actionPerformed(ActionEvent ae) {
      if (svtFrame == null) {
        AdminClientPanel topPanel = getAdminClientPanel();
        if ((svtFrame = topPanel.getSVTFrame()) != null) {
          svtFrame.addPropertyChangeListener("newStore", this);
        } else {
          /*
           * AdminClientPanel.getSVTFrame will open the user's browser on the website page with download instructions.
           */
          return;
        }
      }
      if (retrieveMethod == null) {
        try {
          retrieveMethod = svtFrame.getClass().getMethod("retrieveFromAddress", String.class);
        } catch (Exception e) {
          log(e);
        }
      }
      svtFrame.setVisible(true);
      if (retrieveMethod != null) {
        try {
          String addr = getActiveCoordinatorAddress();
          if (addr != null) {
            retrieveMethod.invoke(svtFrame, addr);
          }
        } catch (Exception e) {
          log(e);
        }
      }
    }

    private String getActiveCoordinatorAddress() {
      IServer activeCoord = clusterModel.getActiveCoordinator();
      if (activeCoord != null) { return activeCoord.getHost() + ":" + activeCoord.getPort(); }
      return null;
    }

    public void propertyChange(PropertyChangeEvent evt) {
      String name = evt.getPropertyName();
      String selectedSession = getSelectedSessionId();
      if ("newStore".equals(name) && selectedSession != null) {
        if (setSessionMethod == null) {
          try {
            setSessionMethod = svtFrame.getClass().getMethod("setSession", String.class);
          } catch (Exception e) {
            log(e);
          }
        }
        if (setSessionMethod != null) {
          try {
            setSessionMethod.invoke(svtFrame, selectedSession);
          } catch (Exception e) {
            log(e);
          }
        }
      }
    }

    private void log(Throwable t) {
      appContext.log(t);
    }
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();
    clusterModel.removeServerStateListener(serverListener);

    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClusterStatsListener(this);
    }

    availableStatsArea.tearDown();

    super.tearDown();
  }

  private static class LoginPanel extends JPanel {
    private final JTextField     userNameField;
    private final JPasswordField passwordField;

    private LoginPanel() {
      super();
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridx = gbc.gridy = 0;
      gbc.insets = new Insets(2, 2, 2, 2);
      add(new JLabel("Username:"), gbc);
      gbc.gridx++;
      userNameField = new JTextField(20);
      add(userNameField, gbc);
      gbc.gridx--;
      gbc.gridy++;
      add(new JLabel("Password:"), gbc);
      gbc.gridx++;
      passwordField = new JPasswordField(20);
      add(passwordField, gbc);

      userNameField.requestFocusInWindow();
    }

    String getUserName() {
      return userNameField.getText();
    }

    String getPassword() {
      return new String(passwordField.getPassword());
    }
  }

  class UpdateSupportedStatsWorker extends BasicWorker<String[]> {
    UpdateSupportedStatsWorker() {
      super(new Callable<String[]>() {
        public String[] call() {
          IServer activeCoord = clusterModel.getActiveCoordinator();
          return activeCoord != null ? activeCoord.getSupportedClusterStats() : new String[0];
        }
      });
    }

    @Override
    public void finished() {
      Exception e = getException();
      if (e == null) {
        setupStatsConfigPanel(getResult());
      } else {
        showError(e);
      }
    }
  }

  /*
   * IClusterStatsListener implementation
   */

  public void clientConnected(IClient client) {
    if (!haveClientStats) {
      appContext.execute(new UpdateSupportedStatsWorker());
      haveClientStats = true;
    }
  }

  public void clientDisconnected(IClient client) {
    IClient[] clients = clusterModel.getClients();
    haveClientStats = clients.length > 0;
    if (!haveClientStats) {
      appContext.execute(new UpdateSupportedStatsWorker());
    }
  }

  /*
   * IClusterStatsListener implementation
   */

  public void allSessionsCleared() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        statsSessionsListModel.clear();
        currentStatsSessionId = null;
      }
    });
  }

  public void connected() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        gathererConnected();
      }
    });
  }

  public void disconnected() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        gathererDisconnected();
      }
    });
  }

  public void reinitialized() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        gathererReinitialized();
      }
    });
  }

  public void sessionCleared(final String sessionId) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        int sessionCount = statsSessionsListModel.getSize();
        for (int i = 0; i < sessionCount; i++) {
          StatsSessionListItem item = (StatsSessionListItem) statsSessionsListModel.elementAt(i);
          if (sessionId.equals(item.getSessionId())) {
            statsSessionsListModel.remove(i);
            break;
          }
        }
      }
    });
  }

  public void sessionCreated(String sessionId) {
    currentStatsSessionId = sessionId;
  }

  public void sessionStarted(String sessionId) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showRecordingInProgress();
      }
    });
  }

  public void sessionStopped(final String sessionId) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (currentStatsSessionId != null && currentStatsSessionId.equals(sessionId)) {
          statsSessionsListModel.addElement(new StatsSessionListItem(sessionId));
          statsSessionsList.setSelectedIndex(statsSessionsListModel.getSize() - 1);
          currentStatsSessionId = null;
          hideRecordingInProgress();
        }
      }
    });
  }
}
