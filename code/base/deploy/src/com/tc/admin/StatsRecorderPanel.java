/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlOptions;
import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.Item;
import org.dijon.List;
import org.dijon.Spinner;
import org.dijon.ToggleButton;

import EDU.oswego.cs.dl.util.concurrent.misc.SwingWorker;

import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XContainer;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererAlreadyConnectedException;
import com.tc.statistics.retrieval.actions.SRAThreadDump;
import com.terracottatech.config.TcStatsConfigDocument;
import com.terracottatech.config.TcStatsConfigDocument.TcStatsConfig;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

public class StatsRecorderPanel extends XContainer {
  private AdminClientContext           m_acc;
  private StatsRecorderNode            m_statsRecorderNode;

  private StatisticsGathererListener   m_statsGathererListener;
  private ToggleButton                 m_startGatheringStatsButton;
  private ToggleButton                 m_stopGatheringStatsButton;
  private List                         m_statsSessionsList;
  private DefaultListModel             m_statsSessionsListModel;
  private XContainer                   m_statsConfigPanel;
  private HashMap                      m_statsControls;
  private Spinner                      m_samplePeriodSpinner;
  private Button                       m_importStatsConfigButton;
  private Button                       m_exportStatsConfigButton;
  private StatisticsLocalGathererMBean m_statisticsGathererMBean;
  private String                       m_currentStatsSessionId;
  private Button                       m_exportStatsButton;
  private JProgressBar                 m_progressBar;
  private File                         m_lastExportDir;
  private Button                       m_clearStatsSessionButton;
  private Button                       m_clearAllStatsSessionsButton;

  private static final int             DEFAULT_STATS_POLL_PERIOD_SECONDS = 2;
  private static final String          DEFAULT_STATS_CONFIG_FILENAME     = "tc-stats-config.xml";

  public StatsRecorderPanel(StatsRecorderNode statsRecorderNode) {
    super();

    m_acc = AdminClient.getContext();
    m_statsRecorderNode = statsRecorderNode;

    load((ContainerResource) m_acc.topRes.getComponent("StatsRecorderPanel"));

    m_startGatheringStatsButton = (ToggleButton) findComponent("StartGatheringStatsButton");
    m_startGatheringStatsButton.addActionListener(new StartGatheringStatsAction());

    m_stopGatheringStatsButton = (ToggleButton) findComponent("StopGatheringStatsButton");
    m_stopGatheringStatsButton.addActionListener(new StopGatheringStatsAction());

    m_currentStatsSessionId = null;
    m_statsSessionsList = (List) findComponent("StatsSessionsList");
    m_statsSessionsList.addListSelectionListener(new StatsSessionsListSelectionListener());
    m_statsSessionsList.setModel(m_statsSessionsListModel = new DefaultListModel());
    m_statsSessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    m_statsConfigPanel = (XContainer) findComponent("StatsConfigPanel");
    m_samplePeriodSpinner = (Spinner) findComponent("SamplePeriodSpinner");
    m_samplePeriodSpinner.setValue(new Long(DEFAULT_STATS_POLL_PERIOD_SECONDS));

    m_importStatsConfigButton = (Button) findComponent("ImportStatsConfigButton");
    m_importStatsConfigButton.addActionListener(new ImportStatsConfigHandler());

    m_exportStatsConfigButton = (Button) findComponent("ExportStatsConfigButton");
    m_exportStatsConfigButton.addActionListener(new ExportStatsConfigHandler());

    m_exportStatsButton = (Button) findComponent("ExportStatsButton");
    m_exportStatsButton.addActionListener(new ExportStatsHandler());

    m_clearStatsSessionButton = (Button) findComponent("ClearStatsSessionButton");
    m_clearStatsSessionButton.addActionListener(new ClearStatsSessionHandler());

    m_clearAllStatsSessionsButton = (Button) findComponent("ClearAllStatsSessionsButton");
    m_clearAllStatsSessionsButton.addActionListener(new ClearAllStatsSessionsHandler());

    Item exportProgressBarHolder = (Item) findComponent("ExportProgressBarHolder");
    exportProgressBarHolder.add(m_progressBar = new JProgressBar());
    m_progressBar.setIndeterminate(true);
    m_progressBar.setVisible(false);

    m_acc.executorService.execute(new StatsGathererConnectWorker());
  }

  void testTriggerThreadDumpSRA() {
    if (isRecording()) {
      m_statisticsGathererMBean.captureStatistic(SRAThreadDump.ACTION_NAME);
    }
  }
  
  class StatsGathererConnectWorker extends BasicWorker<Void> {
    StatsGathererConnectWorker() {
      super(new Callable() {
        public Void call() {
          ConnectionContext cc = m_statsRecorderNode.getConnectionContext();
          m_statisticsGathererMBean = m_statsRecorderNode.getStatisticsGathererMBean();
          try {
            if (m_statsGathererListener == null) {
              m_statsGathererListener = new StatisticsGathererListener();
            }
            cc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATHERER, m_statsGathererListener);
          } catch (Exception e) {
            e.printStackTrace();
          }
          m_statisticsGathererMBean.connect();
          return null;
        }
      });
    }

    private boolean isAlreadyConnectionException(Throwable t) {
      while (t != null) {
        if (t instanceof StatisticsGathererAlreadyConnectedException) { return true; }
        t = t.getCause();
      }
      return false;
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        if (isAlreadyConnectionException(e)) {
          gathererConnected();
        } else {
          m_acc.log(e);
        }
      }
    }
  }

  private void gathererConnected() {
    m_statsSessionsListModel.clear();
    m_acc.executorService.execute(new GathererConnectedWorker());
  }

  class GathererConnectedState {
    private String[] fSessions;
    private String   fActiveStatsSessionId;
    private String[] fSupportedStats;

    GathererConnectedState(String[] sessions, String activeSessionId, String[] supportedStats) {
      fSessions = sessions;
      fActiveStatsSessionId = activeSessionId;
      fSupportedStats = supportedStats;
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
          String[] allSessions = m_statisticsGathererMBean.getAvailableSessionIds();
          String activeSessionId = m_statisticsGathererMBean.getActiveSessionId();
          String[] supportedStats = m_statisticsGathererMBean.getSupportedStatistics();

          return new GathererConnectedState(allSessions, activeSessionId, supportedStats);
        }
      });
    }

    public void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        GathererConnectedState connectedState = getResult();
        String[] allSessions = connectedState.getAllSessions();
        String[] supportedStats = connectedState.getSupportedStats();

        for (int i = 0; i < allSessions.length; i++) {
          m_statsSessionsListModel.addElement(new StatsSessionListItem(allSessions[i]));
        }
        m_clearAllStatsSessionsButton.setEnabled(m_statsSessionsListModel.getSize() > 0);
        m_currentStatsSessionId = connectedState.getActiveStatsSessionId();
        boolean recording = isRecording();
        if (recording) {
          m_statsGathererListener.showRecordingInProgress();
        }
        m_startGatheringStatsButton.setSelected(recording);
        setupStatsConfigPanel(supportedStats);
      }
    }
  }

  class StatisticsGathererListener implements NotificationListener {
    private void showRecordingInProgress() {
      m_startGatheringStatsButton.setSelected(true);
      m_stopGatheringStatsButton.setSelected(false);
      m_statsRecorderNode.notifyChanged();
    }

    private void hideRecordingInProgress() {
      m_currentStatsSessionId = null;
      m_startGatheringStatsButton.setSelected(false);
      m_stopGatheringStatsButton.setSelected(true);
      m_statsRecorderNode.notifyChanged();
    }

    public void handleNotification(Notification notification, Object handback) {
      String type = notification.getType();
      Object userData = notification.getUserData();

      if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_CONNECTED_TYPE)) {
        gathererConnected();
        return;
      }

      if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_SESSION_CREATED_TYPE)) {
        m_currentStatsSessionId = (String) userData;
        return;
      }

      if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_CAPTURING_STARTED_TYPE)) {
        showRecordingInProgress();
        return;
      }

      if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_CAPTURING_STOPPED_TYPE)) {
        String thisSession = (String) userData;
        if (m_currentStatsSessionId != null && m_currentStatsSessionId.equals(thisSession)) {
          m_statsSessionsListModel.addElement(new StatsSessionListItem(thisSession));
          m_statsSessionsList.setSelectedIndex(m_statsSessionsListModel.getSize() - 1);
          m_currentStatsSessionId = null;

          hideRecordingInProgress();
          return;
        }
      }

      if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_SESSION_CLEARED_TYPE)) {
        String sessionId = (String) userData;
        int sessionCount = m_statsSessionsListModel.getSize();
        for (int i = 0; i < sessionCount; i++) {
          StatsSessionListItem item = (StatsSessionListItem) m_statsSessionsListModel.elementAt(i);
          if (sessionId.equals(item.getSessionId())) {
            m_statsSessionsListModel.remove(i);
            break;
          }
        }
        return;
      }

      if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_ALLSESSIONS_CLEARED_TYPE)) {
        m_statsSessionsListModel.clear();
        m_currentStatsSessionId = null;
        return;
      }
    }
  }

  private void setupStatsConfigPanel(String[] stats) {
    m_statsConfigPanel.removeAll();
    m_statsConfigPanel.setLayout(new GridLayout(0, 3));
    if (m_statsControls == null) {
      m_statsControls = new HashMap();
    } else {
      m_statsControls.clear();
    }
    for (String stat : stats) {
      JCheckBox control = new JCheckBox();
      control.setText(stat);
      control.setName(stat);
      m_statsControls.put(stat, control);
      m_statsConfigPanel.add(control);
      control.setSelected(true);
    }
  }

  private long getSamplePeriodMillis() {
    Number samplePeriod = (Number) m_samplePeriodSpinner.getValue();
    return samplePeriod.longValue() * 1000;
  }

  private java.util.List<String> getSelectedStats() {
    Iterator iter = m_statsControls.keySet().iterator();
    ArrayList<String> statList = new ArrayList<String>();
    while (iter.hasNext()) {
      String stat = (String) iter.next();
      JCheckBox control = (JCheckBox) m_statsControls.get(stat);
      if (control.isSelected()) {
        statList.add(stat);
      }
    }
    return statList;
  }

  private void disableAllStats() {
    Iterator iter = m_statsControls.keySet().iterator();
    while (iter.hasNext()) {
      String stat = (String) iter.next();
      JCheckBox control = (JCheckBox) m_statsControls.get(stat);
      control.setSelected(false);
    }
  }

  private void setSelectedStats(String[] stats) {
    disableAllStats();
    for (String stat : stats) {
      JCheckBox control = (JCheckBox) m_statsControls.get(stat);
      control.setSelected(true);
    }
  }

  class StartGatheringStatsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      java.util.List<String> statList = getSelectedStats();
      String[] stats = statList.toArray(new String[0]);
      long samplePeriodMillis = getSamplePeriodMillis();

      m_acc.executorService.execute(new StartGatheringStatsWorker(stats, samplePeriodMillis));
    }
  }

  class StartGatheringStatsWorker extends BasicWorker<Void> {
    StartGatheringStatsWorker(final String[] stats, final long samplePeriodMillis) {
      super(new Callable<Void>() {
        public Void call() {
          m_currentStatsSessionId = new Date().toString();
          m_statisticsGathererMBean.createSession(m_currentStatsSessionId);
          m_statisticsGathererMBean.enableStatistics(stats);
          m_statisticsGathererMBean.setSessionParam(StatisticsConfig.KEY_RETRIEVER_SCHEDULE_INTERVAL,
                                                    new Long(samplePeriodMillis));
          m_statisticsGathererMBean.startCapturing();
          return null;
        }
      });
    }

    public void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      }
      m_stopGatheringStatsButton.setSelected(false);
      m_statsRecorderNode.notifyChanged();
    }
  }

  class StopGatheringStatsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      m_startGatheringStatsButton.setSelected(false);
      m_acc.executorService.execute(new StopGatheringStatsWorker());
    }
  }

  class StopGatheringStatsWorker extends BasicWorker<Void> {
    StopGatheringStatsWorker() {
      super(new Callable<Void>() {
        public Void call() {
          m_statisticsGathererMBean.stopCapturing();
          m_statisticsGathererMBean.closeSession();
          return null;
        }
      });
    }

    public void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      }
      m_statsRecorderNode.notifyChanged();
    }
  }

  class StatsSessionsListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      boolean haveAnySessions = m_statsSessionsListModel.getSize() > 0;
      boolean haveSelectedSession = getSelectedSessionId() != null;
      m_exportStatsButton.setEnabled(haveAnySessions);
      m_clearStatsSessionButton.setEnabled(haveSelectedSession);
      m_clearAllStatsSessionsButton.setEnabled(haveAnySessions);
    }
  }

  class StatsSessionListItem {
    private String fSessionId;

    StatsSessionListItem(String sessionId) {
      fSessionId = sessionId;
    }

    String getSessionId() {
      return fSessionId;
    }

    public String toString() {
      return fSessionId;
    }
  }

  public boolean isRecording() {
    return m_currentStatsSessionId != null;
  }
  
  private String getSelectedSessionId() {
    StatsSessionListItem item = (StatsSessionListItem) m_statsSessionsList.getSelectedValue();
    return item != null ? item.getSessionId() : null;
  }

  private class ImportStatsConfigHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      JFileChooser chooser = new JFileChooser();
      if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
      chooser.setDialogTitle("Import statistics configuration");
      chooser.setMultiSelectionEnabled(false);
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), DEFAULT_STATS_CONFIG_FILENAME));
      if (chooser.showOpenDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      if (!file.exists()) {
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        String msg = "File '" + file + "' does not exist.";
        JOptionPane.showMessageDialog(StatsRecorderPanel.this, msg, frame.getTitle(), JOptionPane.WARNING_MESSAGE);
        return;
      }
      m_lastExportDir = file.getParentFile();
      try {
        TcStatsConfigDocument tcStatsConfigDoc = TcStatsConfigDocument.Factory.parse(file);
        TcStatsConfig tcStatsConfig = tcStatsConfigDoc.getTcStatsConfig();
        if (tcStatsConfig.isSetRetrievalPollPeriod()) {
          m_samplePeriodSpinner.setValue(tcStatsConfig.getRetrievalPollPeriod().longValue() / 1000);
        }
        if (tcStatsConfig.isSetEnabledStatistics()) {
          setSelectedStats(tcStatsConfig.getEnabledStatistics().getNameArray());
        }
      } catch (Exception e) {
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        String msg = "Unable to parse '" + file.getName() + "' as a Terracotta stats config document";
        JOptionPane.showMessageDialog(StatsRecorderPanel.this, msg, frame.getTitle(), JOptionPane.ERROR_MESSAGE);
        return;
      }
    }
  }

  private class ExportStatsConfigHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      JFileChooser chooser = new JFileChooser();
      if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
      chooser.setDialogTitle("Export statistics configuration");
      chooser.setMultiSelectionEnabled(false);
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), DEFAULT_STATS_CONFIG_FILENAME));
      if (chooser.showSaveDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      m_lastExportDir = file.getParentFile();
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
        AdminClient.getContext().log(ioe);
      } finally {
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
      }
    }
  }

  class ZipFileFilter extends FileFilter {
    public boolean accept(File file) {
      return file.isDirectory() || file.getName().endsWith(".zip");
    }

    public String getDescription() {
      return "ZIP files";
    }
  }

  class ExportStatsHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      JFileChooser chooser = new JFileChooser();
      if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
      if (m_statsSessionsListModel.getSize() == 0) return;
      chooser.setDialogTitle("Export statistics");
      chooser.setMultiSelectionEnabled(false);
      chooser.setFileFilter(new ZipFileFilter());
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "tc-stats.zip"));
      if (chooser.showSaveDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      m_lastExportDir = file.getParentFile();
      GetMethod get = null;
      try {
        String uri = m_statsRecorderNode.getStatsExportServletURI();
        URL url = new URL(uri);
        HttpClient httpClient = new HttpClient();

        get = new GetMethod(url.toString());
        get.setFollowRedirects(true);
        int status = httpClient.executeMethod(get);
        if (status != HttpStatus.SC_OK) {
          AdminClient.getContext().log(
                                       "The http client has encountered a status code other than ok for the url: "
                                           + url + " status: " + HttpStatus.getStatusText(status));
          return;
        }
        m_progressBar.setVisible(true);
        new Thread(new StreamCopierRunnable(get, file)).start();
      } catch (Exception e) {
        AdminClient.getContext().log(e);
        if (get != null) {
          get.releaseConnection();
        }
      }
    }
  }

  class StreamCopierRunnable implements Runnable {
    GetMethod fGetMethod;
    File      fOutFile;

    StreamCopierRunnable(GetMethod getMethod, File outFile) {
      fGetMethod = getMethod;
      fOutFile = outFile;
    }

    public void run() {
      FileOutputStream out = null;

      try {
        out = new FileOutputStream(fOutFile);
        InputStream in = fGetMethod.getResponseBodyAsStream();

        byte[] buffer = new byte[1024 * 8];
        int count;
        try {
          while ((count = in.read(buffer)) >= 0) {
            out.write(buffer, 0, count);
          }
        } finally {
          SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
              m_progressBar.setVisible(false);
              AdminClient.getContext().setStatus("Wrote '" + fOutFile.getAbsolutePath() + "'");
            }
          });
          IOUtils.closeQuietly(in);
          IOUtils.closeQuietly(out);
        }
      } catch (Exception e) {
        AdminClient.getContext().log(e);
      } finally {
        IOUtils.closeQuietly(out);
        fGetMethod.releaseConnection();
      }
    }
  }

  class ClearStatsSessionHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      final StatsSessionListItem item = (StatsSessionListItem) m_statsSessionsList.getSelectedValue();
      if (item != null) {
        String msg = "Really clear statistics from session '" + item + "?'";
        Frame frame = (Frame) StatsRecorderPanel.this.getAncestorOfClass(Frame.class);
        int result = JOptionPane.showConfirmDialog(StatsRecorderPanel.this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
          m_progressBar.setVisible(true);
          SwingWorker worker = new SwingWorker() {
            public Object construct() throws Exception {
              m_statisticsGathererMBean.clearStatistics(item.getSessionId());
              return null;
            }

            public void finished() {
              m_progressBar.setVisible(false);
              InvocationTargetException ite = getException();
              if (ite != null) {
                Throwable cause = ite.getCause();
                AdminClient.getContext().log(cause != null ? cause : ite);
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
      Frame frame = (Frame) StatsRecorderPanel.this.getAncestorOfClass(Frame.class);
      int result = JOptionPane.showConfirmDialog(StatsRecorderPanel.this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        m_progressBar.setVisible(true);
        SwingWorker worker = new SwingWorker() {
          public Object construct() throws Exception {
            m_statisticsGathererMBean.clearAllStatistics();
            return null;
          }

          public void finished() {
            m_progressBar.setVisible(false);
            InvocationTargetException ite = getException();
            if (ite != null) {
              Throwable cause = ite.getCause();
              AdminClient.getContext().log(cause != null ? cause : ite);
              return;
            }
          }
        };
        worker.start();
      }
    }
  }
  public void tearDown() {
    super.tearDown();

    m_statsConfigPanel.tearDown();

    m_acc = null;
    m_statsGathererListener = null;
    m_startGatheringStatsButton = null;
    m_stopGatheringStatsButton = null;
    m_statsSessionsList = null;
    m_statsSessionsListModel = null;
    m_statsConfigPanel = null;
    m_statsControls = null;
    m_samplePeriodSpinner = null;
    m_importStatsConfigButton = null;
    m_exportStatsConfigButton = null;
    m_statisticsGathererMBean = null;
    m_currentStatsSessionId = null;
    m_exportStatsButton = null;
    m_progressBar = null;
    m_lastExportDir = null;
    m_clearStatsSessionButton = null;
    m_clearAllStatsSessionsButton = null;
  }
}
