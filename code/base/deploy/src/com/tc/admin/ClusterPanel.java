/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import EDU.oswego.cs.dl.util.concurrent.misc.SwingWorker;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dijon.Button;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Item;
import org.dijon.List;
import org.dijon.ScrollPane;
import org.dijon.Spinner;
import org.dijon.TabbedPane;
import org.dijon.TextArea;
import org.dijon.ToggleButton;

import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeCellRenderer;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererAlreadyConnectedException;
import com.tc.statistics.retrieval.actions.SRAThreadDump;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;

public class ClusterPanel extends XContainer {
  private AdminClientContext           m_acc;
  private ClusterNode                  m_clusterNode;
  private JTextField                   m_hostField;
  private JTextField                   m_portField;
  private JButton                      m_connectButton;
  static private ImageIcon             m_connectIcon;
  static private ImageIcon             m_disconnectIcon;
  private StatusView                   m_statusView;
  private ProductInfoPanel             m_productInfoPanel;
  private TabbedPane                   m_tabbedPane;

  private Button                       m_threadDumpButton;
  private XTree                        m_threadDumpTree;
  private XTreeModel                   m_threadDumpTreeModel;
  private TextArea                     m_threadDumpTextArea;
  private ScrollPane                   m_threadDumpTextScroller;
  private ThreadDumpTreeNode           m_lastSelectedThreadDumpTreeNode;

  private int                          m_statsTabIndex;
  private StatisticsGathererListener   m_statsGathererListener;
  private ToggleButton                 m_startGatheringStatsButton;
  private ToggleButton                 m_stopGatheringStatsButton;
  private List                         m_statsSessionsList;
  private DefaultListModel             m_statsSessionsListModel;
  private Container                    m_statsConfigPanel;
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

  static {
    m_connectIcon = new ImageIcon(ServerPanel.class.getResource("/com/tc/admin/icons/disconnect_co.gif"));
    m_disconnectIcon = new ImageIcon(ServerPanel.class.getResource("/com/tc/admin/icons/newex_wiz.gif"));
  }

  public ClusterPanel(ClusterNode clusterNode) {
    super(clusterNode);

    m_clusterNode = clusterNode;
    m_clusterNode.setRenderer(new XTreeCellRenderer() {
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                    boolean leaf, int row, boolean focused) {
        Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
        if (m_startGatheringStatsButton.isSelected()) {
          m_label.setForeground(sel ? Color.white : Color.red);
          m_label.setText(m_clusterNode.getBaseLabel() + " (recording stats)");
        }
        return comp;
      }
    });

    m_acc = AdminClient.getContext();

    load((ContainerResource) m_acc.topRes.getComponent("ClusterPanel"));

    m_tabbedPane = (TabbedPane) findComponent("TabbedPane");
    m_hostField = (JTextField) findComponent("HostField");
    m_portField = (JTextField) findComponent("PortField");
    m_connectButton = (JButton) findComponent("ConnectButton");
    m_statusView = (StatusView) findComponent("StatusIndicator");
    m_productInfoPanel = (ProductInfoPanel) findComponent("ProductInfoPanel");

    m_statusView.setLabel("Not connected");
    m_productInfoPanel.setVisible(false);

    m_hostField.addActionListener(new HostFieldHandler());
    m_portField.addActionListener(new PortFieldHandler());
    m_connectButton.addActionListener(new ConnectionButtonHandler());

    m_hostField.setText(m_clusterNode.getHost());
    m_portField.setText(Integer.toString(m_clusterNode.getPort()));

    setupConnectButton();

    m_threadDumpButton = (Button) findComponent("TakeThreadDumpButton");
    m_threadDumpButton.addActionListener(new ThreadDumpButtonHandler());

    m_threadDumpTree = (XTree) findComponent("ThreadDumpTree");
    m_threadDumpTree.getSelectionModel().addTreeSelectionListener(new ThreadDumpTreeSelectionListener());

    m_threadDumpTree.setModel(m_threadDumpTreeModel = new XTreeModel());
    m_threadDumpTree.setShowsRootHandles(true);

    m_threadDumpTextArea = (TextArea) findComponent("ThreadDumpTextArea");
    m_threadDumpTextScroller = (ScrollPane) findComponent("ThreadDumpTextScroller");

    m_statsTabIndex = m_tabbedPane.indexOfComponent((java.awt.Component)m_tabbedPane.findComponent("StatisticsRecorderPage"));
    m_startGatheringStatsButton = (ToggleButton) findComponent("StartGatheringStatsButton");
    m_startGatheringStatsButton.addActionListener(new StartGatheringStatsAction());

    m_stopGatheringStatsButton = (ToggleButton) findComponent("StopGatheringStatsButton");
    m_stopGatheringStatsButton.addActionListener(new StopGatheringStatsAction());

    m_currentStatsSessionId = null;
    m_statsSessionsList = (List) findComponent("StatsSessionsList");
    m_statsSessionsList.addListSelectionListener(new StatsSessionsListSelectionListener());
    m_statsSessionsList.setModel(m_statsSessionsListModel = new DefaultListModel());
    m_statsSessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    m_statsConfigPanel = (Container) findComponent("StatsConfigPanel");
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
  }

  void reinitialize() {
    m_hostField.setText(m_clusterNode.getHost());
    m_portField.setText(Integer.toString(m_clusterNode.getPort()));
  }

  class HostFieldHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String host = m_hostField.getText().trim();

      m_clusterNode.setHost(host);
      m_acc.controller.nodeChanged(m_clusterNode);
      m_acc.controller.updateServerPrefs();
    }
  }

  class PortFieldHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String port = m_portField.getText().trim();

      try {
        m_clusterNode.setPort(Integer.parseInt(port));
        m_acc.controller.nodeChanged(m_clusterNode);
        m_acc.controller.updateServerPrefs();
      } catch (Exception e) {
        Toolkit.getDefaultToolkit().beep();
        m_acc.controller.log("'" + port + "' not a number");
        m_portField.setText(Integer.toString(m_clusterNode.getPort()));
      }
    }
  }

  class ConnectionButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      m_connectButton.setEnabled(false);
      if (m_clusterNode.isConnected()) {
        disconnect();
      } else {
        connect();
      }
    }
  }

  class ThreadDumpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        ClusterThreadDumpEntry tde = m_clusterNode.takeThreadDump();
        XTreeNode root = (XTreeNode) m_threadDumpTreeModel.getRoot();
        int index = root.getChildCount();

        root.add(tde);
        // TODO: the following is daft; nodesWereInserted is all that should be needed but for some
        // reason the first node requires nodeStructureChanged on the root; why? I don't know.
        m_threadDumpTreeModel.nodesWereInserted(root, new int[] { index });
        m_threadDumpTreeModel.nodeStructureChanged(root);
      } catch (Exception e) {
        m_acc.log(e);
      }

      if (haveActiveRecordingSession()) {
        m_statisticsGathererMBean.captureStatistic(SRAThreadDump.ACTION_NAME);
      }
    }
  }

  class ThreadDumpTreeSelectionListener implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      if (m_lastSelectedThreadDumpTreeNode != null) {
        m_lastSelectedThreadDumpTreeNode.setViewPosition(m_threadDumpTextScroller.getViewport().getViewPosition());
      }
      ThreadDumpTreeNode tdtn = (ThreadDumpTreeNode) m_threadDumpTree.getLastSelectedPathComponent();
      if (tdtn != null) {
        m_threadDumpTextArea.setText(tdtn.getContent());
        final Point viewPosition = tdtn.getViewPosition();
        if (viewPosition != null) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              m_threadDumpTextScroller.getViewport().setViewPosition(viewPosition);
            }
          });
        }
      }
      m_lastSelectedThreadDumpTreeNode = tdtn;
    }
  }

  private void testSetupStats() {
    if (m_statisticsGathererMBean == null) {
      ConnectionContext cc = m_clusterNode.getConnectionContext();
      m_statisticsGathererMBean = m_clusterNode.getStatisticsGathererMBean();
      try {
        if (m_statsGathererListener == null) {
          m_statsGathererListener = new StatisticsGathererListener();
        }
        cc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATHERER, m_statsGathererListener);
      } catch (Exception e) {
        e.printStackTrace();
      }
      try {
        m_statisticsGathererMBean.connect();
      } catch (Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof StatisticsGathererAlreadyConnectedException) {
          gathererConnected();
        } else {
          e.printStackTrace();
        }
      }
      setupStatsConfigPanel();
    }
  }

  private String[] getAllSessions() {
    return m_statisticsGathererMBean.getAvailableSessionIds();
  }

  boolean haveActiveRecordingSession() {
    return m_currentStatsSessionId != null;
  }

  private void gathererConnected() {
    m_statsSessionsListModel.clear();
    String[] sessions = getAllSessions();
    for (int i = 0; i < sessions.length; i++) {
      m_statsSessionsListModel.addElement(new StatsSessionListItem(sessions[i]));
    }
    m_clearAllStatsSessionsButton.setEnabled(m_statsSessionsListModel.getSize() > 0);
    m_currentStatsSessionId = m_statisticsGathererMBean.getActiveSessionId();
    boolean gathering = m_currentStatsSessionId != null;
    if (gathering) {
      m_statsGathererListener.showRecordingInProgress();
    }
    m_startGatheringStatsButton.setSelected(gathering);
  }

  class StatisticsGathererListener implements NotificationListener {
    private void showRecordingInProgress() {
      m_startGatheringStatsButton.setSelected(true);
      m_stopGatheringStatsButton.setSelected(false);
      m_tabbedPane.setForegroundAt(m_statsTabIndex, Color.red);
      m_clusterNode.notifyChanged();
    }

    private void hideRecordingInProgress() {
      m_currentStatsSessionId = null;
      m_startGatheringStatsButton.setSelected(false);
      m_stopGatheringStatsButton.setSelected(true);
      m_tabbedPane.setForegroundAt(m_statsTabIndex, null);
      m_clusterNode.notifyChanged();
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

  private void tearDownStats() {
    m_statisticsGathererMBean = null;
  }

  private void setupStatsConfigPanel() {
    String[] stats = m_statisticsGathererMBean.getSupportedStatistics();

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
      testSetupStats();
      try {
        m_currentStatsSessionId = new Date().toString();
        m_statisticsGathererMBean.createSession(m_currentStatsSessionId);
        java.util.List<String> statList = getSelectedStats();
        m_statisticsGathererMBean.enableStatistics(statList.toArray(new String[0]));
        long samplePeriodMillis = getSamplePeriodMillis();
        m_statisticsGathererMBean.setSessionParam(StatisticsConfig.KEY_GLOBAL_SCHEDULE_PERIOD,
                                                  new Long(samplePeriodMillis));
        m_statisticsGathererMBean.startCapturing();
      } catch (Exception e) {
        e.printStackTrace();
      }
      m_stopGatheringStatsButton.setSelected(false);
      m_clusterNode.notifyChanged();
    }
  }

  class StopGatheringStatsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      m_startGatheringStatsButton.setSelected(false);
      try {
        m_statisticsGathererMBean.stopCapturing();
        m_statisticsGathererMBean.closeSession();
      } catch (Exception e) {
        AdminClient.getContext().log(e);
      }
      m_clusterNode.notifyChanged();
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
      if (chooser.showOpenDialog(ClusterPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      if (!file.exists()) {
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        String msg = "File '" + file + "' does not exist.";
        JOptionPane.showMessageDialog(ClusterPanel.this, msg, frame.getTitle(), JOptionPane.WARNING_MESSAGE);
        return;
      }
      m_lastExportDir = file.getParentFile();
      Properties statsConfigProps = new Properties();
      FileInputStream in = null;
      try {
        statsConfigProps.load(in = new FileInputStream(file));
        String enabledStats = statsConfigProps.getProperty("enabled.statistics");
        if (enabledStats != null) {
          setSelectedStats(StringUtils.split(enabledStats, ','));
        }
        long defaultPeriodMillis = DEFAULT_STATS_POLL_PERIOD_SECONDS * 1000;
        String periodMillis = statsConfigProps
            .getProperty("schedule.period.millis", Long.toString(defaultPeriodMillis));
        if (periodMillis != null) {
          try {
            m_samplePeriodSpinner.setValue(Long.parseLong(periodMillis) / 1000);
          } catch (NumberFormatException nfe) {/**/
          }
        }
      } catch (IOException ioe) {
        AdminClient.getContext().log(ioe);
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
  }

  private class ExportStatsConfigHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      JFileChooser chooser = new JFileChooser();
      if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
      chooser.setDialogTitle("Export statistics configuration");
      chooser.setMultiSelectionEnabled(false);
      if (chooser.showSaveDialog(ClusterPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      m_lastExportDir = file.getParentFile();
      Properties statsConfigProps = new Properties();
      java.util.List<String> statList = getSelectedStats();
      statsConfigProps.setProperty("enabled.statistics", StringUtils.join(statList.iterator(), ','));
      statsConfigProps.setProperty("schedule.period.millis", Long.toString(getSamplePeriodMillis()));
      FileOutputStream out = null;
      try {
        out = new FileOutputStream(file);
        statsConfigProps.store(out, "Terracotta statistics configuration");
      } catch (IOException ioe) {
        AdminClient.getContext().log(ioe);
      } finally {
        IOUtils.closeQuietly(out);
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
      if (chooser.showSaveDialog(ClusterPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      m_lastExportDir = file.getParentFile();
      GetMethod get = null;
      try {
        String uri = m_clusterNode.getStatsExportServletURI();
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
        Frame frame = (Frame) m_tabbedPane.getAncestorOfClass(Frame.class);
        int result = JOptionPane.showConfirmDialog(m_tabbedPane, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
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
      Frame frame = (Frame) m_tabbedPane.getAncestorOfClass(Frame.class);
      int result = JOptionPane.showConfirmDialog(m_tabbedPane, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
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

  void setupConnectButton() {
    String label;
    Icon icon;
    boolean enabled;
    boolean connected = m_clusterNode.isConnected();

    if (connected) {
      label = "Disconnect";
      icon = m_disconnectIcon;
      enabled = true;
    } else {
      label = "Connect...";
      icon = m_connectIcon;
      enabled = !m_clusterNode.isAutoConnect();
    }

    m_connectButton.setText(label);
    m_connectButton.setIcon(icon);
    m_connectButton.setEnabled(enabled);

    setTabbedPaneEnabled(connected);
  }

  JButton getConnectButton() {
    return m_connectButton;
  }

  private void connect() {
    m_clusterNode.connect();
  }

  void activated() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);

    setupConnectButton();

    Date activateDate = new Date(m_clusterNode.getActivateTime());
    String activateTime = activateDate.toString();

    setStatusLabel(m_acc.format("server.activated.label", new Object[] { activateTime }));
    if (!isProductInfoShowing()) {
      showProductInfo();
    }

    testSetupStats();

    m_acc.controller.setStatus(m_acc.format("server.activated.status", new Object[] { m_clusterNode, activateTime }));
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);

    Date startDate = new Date(m_clusterNode.getStartTime());
    String startTime = startDate.toString();

    setupConnectButton();
    setStatusLabel(m_acc.format("server.started.label", new Object[] { startTime }));
    if (!isProductInfoShowing()) {
      showProductInfo();
    }

    m_acc.controller.setStatus(m_acc.format("server.started.status", new Object[] { m_clusterNode, startTime }));
  }

  void passiveUninitialized() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);

    String startTime = new Date().toString();

    setupConnectButton();
    setStatusLabel(m_acc.format("server.initializing.label", new Object[] { startTime }));
    if (!isProductInfoShowing()) {
      showProductInfo();
    }

    m_acc.controller.setStatus(m_acc.format("server.initializing.status", new Object[] { m_clusterNode, startTime }));
  }

  void passiveStandby() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);

    String startTime = new Date().toString();

    setupConnectButton();
    setStatusLabel(m_acc.format("server.standingby.label", new Object[] { startTime }));
    if (!isProductInfoShowing()) {
      showProductInfo();
    }

    m_acc.controller.setStatus(m_acc.format("server.standingby.status", new Object[] { m_clusterNode, startTime }));
  }

  private void disconnect() {
    m_clusterNode.getDisconnectAction().actionPerformed(null);
  }

  private void setTabbedPaneEnabled(boolean enabled) {
    int tabCount = m_tabbedPane.getTabCount();
    for (int i = 1; i < tabCount; i++) {
      m_tabbedPane.setEnabledAt(i, enabled);
    }
    m_tabbedPane.setSelectedIndex(0);
    m_tabbedPane.revalidate();
    m_tabbedPane.repaint();
  }

  void disconnected() {
    m_hostField.setEditable(true);
    m_portField.setEditable(true);

    String startTime = new Date().toString();

    setupConnectButton();
    setStatusLabel(m_acc.format("server.disconnected.label", new Object[] { startTime }));
    hideRuntimeInfo();
    tearDownStats();
    if (haveActiveRecordingSession()) {
      m_statsGathererListener.hideRecordingInProgress();
    }
    m_acc.controller.setStatus(m_acc.format("server.disconnected.status", new Object[] { m_clusterNode, startTime }));
  }

  void setStatusLabel(String msg) {
    m_statusView.setLabel(msg);
    m_statusView.setIndicator(m_clusterNode.getServerStatusColor());
  }

  boolean isProductInfoShowing() {
    return m_productInfoPanel.isVisible();
  }

  private void showProductInfo() {
    m_productInfoPanel.init(m_clusterNode.getProductInfo());
    m_productInfoPanel.setVisible(true);

    revalidate();
    repaint();
  }

  private void hideRuntimeInfo() {
    m_productInfoPanel.setVisible(false);
    revalidate();
    repaint();
  }

  public void tearDown() {
    super.tearDown();

    m_statusView.tearDown();
    m_productInfoPanel.tearDown();

    m_acc = null;
    m_clusterNode = null;
    m_hostField = null;
    m_portField = null;
    m_connectButton = null;
    m_statusView = null;
    m_productInfoPanel = null;
  }
}
