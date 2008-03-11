/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.cvt;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYImageAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYAnnotationEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.tc.statistics.StatisticData;
import com.tc.statistics.database.exceptions.StatisticsDatabaseException;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.statistics.retrieval.actions.SRACpuConstants;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastCount;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastPerTransaction;
import com.tc.statistics.retrieval.actions.SRAL2ChangesPerBroadcast;
import com.tc.statistics.retrieval.actions.SRAL2ToL1FaultRate;
import com.tc.statistics.retrieval.actions.SRAL2TransactionCount;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStageQueueDepths;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.statistics.retrieval.actions.SRASystemProperties;
import com.tc.statistics.retrieval.actions.SRAThreadDump;
import com.tc.statistics.store.StatisticDataUser;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStoreImportListener;
import com.tc.statistics.store.exceptions.StatisticsStoreException;
import com.tc.statistics.store.exceptions.StatisticsStoreSetupErrorException;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

public class ClusterVisualizerFrame extends JFrame {
  private ImportAction                     fImportAction;
  private RetrieveAction                   fRetrieveAction;
  private SessionInfo[]                    fSessions;
  private JComboBox                        fSessionSelector;
  private JPanel                           fToolBar;
  private JSlider                          fHeightSlider;
  private SessionInfo                      fSessionInfo;
  private JPanel                           fBottomPanel;
  private JPanel                           fControlPanel;
  private JPanel                           fGraphPanel;
  private AxisSpace                        fRangeAxisSpace;
  private MyStatisticsStore                fStore;
  private List<NodeControl>                fNodeControlList;
  private Map<String, AbstractStatHandler> fStatHandlerMap;
  private JLabel                           fStatusLine;
  private JProgressBar                     fProgressBar;
  private File                             fLastDir;

  private Dimension                        fDefaultGraphSize = new Dimension(ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
                                                                             ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT);

  public ClusterVisualizerFrame(String[] args) {
    super("Terracotta Cluster Visualizer");
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    JMenu fileMenu = new JMenu("File");
    fImportAction = new ImportAction();
    fileMenu.add(fImportAction);
    fRetrieveAction = new RetrieveAction();
    fileMenu.add(fRetrieveAction);
    fileMenu.addSeparator();
    ExitAction exitAction = new ExitAction();
    fileMenu.add(exitAction);
    menuBar.add(fileMenu);

    fToolBar = new JPanel(new GridBagLayout());
    getContentPane().add(fToolBar, BorderLayout.NORTH);
    fToolBar.add(new JButton(fImportAction));
    fToolBar.add(new JButton(fRetrieveAction));
    fToolBar.add(fSessionSelector = new JComboBox());
    fToolBar.add(fHeightSlider = new JSlider(100, 600, 250));
    fHeightSlider.addChangeListener(new SliderHeightListener());
    fSessionSelector.addActionListener(new SessionSelectorHandler());
    getContentPane().add(fBottomPanel = new JPanel(new BorderLayout()), BorderLayout.CENTER);
    fBottomPanel.setBorder(LineBorder.createBlackLineBorder());
    fGraphPanel = new JPanel();
    fGraphPanel.setLayout(new GridLayout(0, 1));
    fBottomPanel.add(new JScrollPane(fGraphPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
    statusPanel.add(fStatusLine = new JLabel());
    statusPanel.add(fProgressBar = new JProgressBar(), BorderLayout.EAST);
    getContentPane().add(statusPanel, BorderLayout.SOUTH);
    fProgressBar.setIndeterminate(true);
    fProgressBar.setVisible(false);
    fSessionSelector.setVisible(false);
    fHeightSlider.setVisible(false);

    fNodeControlList = new ArrayList<NodeControl>();
    fStatHandlerMap = new HashMap<String, AbstractStatHandler>();
    putStatHandler(new MemoryUsageHandler());
    putStatHandler(new L2toL1FaultHandler());
    putStatHandler(new L2BroadcastHandler());
    putStatHandler(new L2BroadcastsPerTxHandler());
    putStatHandler(new L2ChangesPerBroadcastHandler());
    putStatHandler(new TxRateHandler());
    putStatHandler(new CPUUsageHandler());
    putStatHandler(new StageQueueDepthsHandler());

    fRangeAxisSpace = new AxisSpace();

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  class SliderHeightListener implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      int newHeight = fHeightSlider.getValue();
      Dimension newDim = new Dimension(ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH, newHeight);
      for (Component comp : fGraphPanel.getComponents()) {
        if (comp instanceof ChartPanel) {
          ChartPanel chartPanel = (ChartPanel) comp;
          chartPanel.setPreferredSize(newDim);
        }
      }
      fGraphPanel.revalidate();
      fGraphPanel.repaint();
    }
  }

  private void handleSessionSelection() {
    fSessionInfo = (SessionInfo) fSessionSelector.getSelectedItem();
    if (fSessionInfo == null) return;
    fGraphPanel.removeAll();
    fGraphPanel.revalidate();
    fGraphPanel.repaint();
    fProgressBar.setVisible(true);
    new NodeStatMappingThread().start();
  }

  private class SessionSelectorHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      handleSessionSelection();
    }
  }

  private class GenerateGraphsAction extends AbstractAction implements Runnable {
    List<NodeGroupHandler> fNodeGroupHandlers;

    GenerateGraphsAction() {
      super("Generate graphs");
    }

    List<NodeGroupHandler> getNodeGroupHandlers() {
      ArrayList<NodeGroupHandler> list = new ArrayList<NodeGroupHandler>();
      Iterator<NodeControl> nodeControlIter = fNodeControlList.iterator();
      while (nodeControlIter.hasNext()) {
        NodeControl nodeControl = nodeControlIter.next();
        if (nodeControl.isSelected()) {
          List<String> statList = nodeControl.getSelectedStats();
          if (statList.size() > 0) {
            list.add(new NodeGroupHandler(nodeControl.getNode(), statList));
          }
        }
      }
      return list;
    }

    public void actionPerformed(ActionEvent ae) {
      fGraphPanel.removeAll();
      fGraphPanel.setBorder(LineBorder.createGrayLineBorder());
      fProgressBar.setVisible(true);
      setToolBarEnabled(false);

      fNodeGroupHandlers = getNodeGroupHandlers();

      Thread graphBuilderThread = new Thread(this);
      graphBuilderThread.start();
    }

    public void run() {
      fRangeAxisSpace.setLeft(0);
      fRangeAxisSpace.setRight(10);

      Iterator<NodeGroupHandler> nodeGroupHandlerIter = fNodeGroupHandlers.iterator();
      while (nodeGroupHandlerIter.hasNext()) {
        NodeGroupHandler ngh = nodeGroupHandlerIter.next();
        List<DataDisplay> displayList = ngh.generateDisplay();
        if (displayList.size() > 0) {
          buildNodeGraphsLater(displayList, ngh.fThreadDumps);
        }
      }

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          setAllRangeAxes();
          fProgressBar.setVisible(false);
          fStatusLine.setText("");
          fHeightSlider.setVisible(true);
          setToolBarEnabled(true);
        }
      });
    }

    private void setAllRangeAxes() {
      for (Component comp : fGraphPanel.getComponents()) {
        if (!(comp instanceof ChartPanel)) continue;
        ChartPanel chartPanel = (ChartPanel) comp;
        JFreeChart chart = chartPanel.getChart();
        Plot plot = (chart.getPlot());
        if (!(plot instanceof XYPlot)) continue;
        XYPlot xyPlot = (XYPlot) plot;
        xyPlot.setFixedRangeAxisSpace(fRangeAxisSpace);
      }
    }

    private void buildNodeGraphsLater(final List<DataDisplay> displayList, final List<StatisticData> threadDumps) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          buildNodeGraphs(displayList, threadDumps);
          fGraphPanel.revalidate();
          fGraphPanel.repaint();
        }
      });
    }
  }


  class ImportFileFilter extends FileFilter {
    public boolean accept(File file) {
      String name = file.getName();
      return file.isDirectory() || name.endsWith(".zip") || name.endsWith(".csv"); 
    }
    
    public String getDescription() {
      return "ZIP, CSV";
    }
  }

  class ImportAction extends AbstractAction {
    ImportAction() {
      super("Import...");
    }

    public void actionPerformed(ActionEvent ae) {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setDialogTitle("Import statistics");
      chooser.setMultiSelectionEnabled(false);
      if (fLastDir != null) {
        chooser.setCurrentDirectory(fLastDir);
      }
      chooser.setFileFilter(new ImportFileFilter());
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "tc-stats.zip"));
      if (chooser.showOpenDialog(ClusterVisualizerFrame.this) != JFileChooser.APPROVE_OPTION) return;
      setToolBarEnabled(false);
      File file = chooser.getSelectedFile();
      fLastDir = file.getParentFile();
      fProgressBar.setVisible(true);
      new Thread(new ImportRunner(file)).start();
    }
  }

  class ImportRunner implements Runnable {
    File fFile;

    ImportRunner(File file) {
      fFile = file;
    }

    public void run() {
      String fileName = fFile.getName();
      if (fileName.endsWith(".zip")) {
        FileInputStream fis = null;

        try {
          fis = new FileInputStream(fFile);
          ZipInputStream zis = null;
          try {
            zis = new ZipInputStream(fis);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
              String name = entry.getName();
              if (name.endsWith(".csv")) {
                populateStore(zis);
                zis.closeEntry();
                return;
              }
              zis.closeEntry();
            }
          } finally {
            IOUtils.closeQuietly(zis);
          }
        } catch (Exception e) {
          IOUtils.closeQuietly(fis);
          e.printStackTrace();
          return;
        }
      } else if (fileName.endsWith(".csv")) {
        FileInputStream fis = null;

        try {
          fis = new FileInputStream(fFile);
          populateStore(fis);
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          IOUtils.closeQuietly(fis);
        }
      }
    }
  }

  private void setStatusLineLater(final String s) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        fStatusLine.setText(s);
      }
    });
  }

  private void populateStore(InputStream in) throws Exception {
    resetStore();

    try {
      fStore.importCsvStatistics(new InputStreamReader(in), new StatisticsStoreImportListener() {
        public void started() {
          fStatusLine.setText("");
        }

        public void imported(final long count) {
          setStatusLineLater("Read " + count + " entries");
        }

        public void optimizing() {
          setStatusLineLater(fStatusLine.getText() + " (optimizing...)");
        }

        public void finished(final long total) {
          setStatusLineLater("Imported " + total + " entries");
        }
      });
    } finally {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            newStore();
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            fProgressBar.setVisible(false);
            informAvailableSessions();
            setToolBarEnabled(true);
          }
        }
      });
    }
  }

  private void informAvailableSessions() {
    // If there's only a single session, prepare to view it.
    if (fSessions.length == 1) {
      handleSessionSelection();
    } else {
      JPanel panel = new JPanel(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = 0;
      gbc.anchor = GridBagConstraints.NORTHWEST;
      panel.add(new JLabel("Select session to view:"), gbc);
      JList list = new JList(fSessions);
      panel.add(new JScrollPane(list), gbc);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      int answer = JOptionPane.showConfirmDialog(ClusterVisualizerFrame.this, panel, getTitle(),
                                                 JOptionPane.OK_CANCEL_OPTION);
      if (answer == JOptionPane.OK_OPTION) {
        SessionInfo session = (SessionInfo) list.getSelectedValue();
        fSessionSelector.setSelectedItem(session);
      }
    }
  }

  void resetStore() throws Exception {
    if (fStore != null) {
      fStore.close();
    }
    File dir = new File(System.getProperty("java.io.tmpdir"), "tc-stats-store");
    try {
      FileUtils.deleteDirectory(dir);
    } catch (Exception e) {
      e.printStackTrace();
    }
    dir.mkdir();
    fStore = new MyStatisticsStore(dir);
    fStore.open();
  }

  void newStore() throws Exception {
    fGraphPanel.removeAll();
    fGraphPanel.revalidate();
    fGraphPanel.repaint();
    List<SessionInfo> sessionList = fStore.getAllSessions();
    fSessions = sessionList.toArray(new SessionInfo[0]);
    DefaultComboBoxModel comboModel = new DefaultComboBoxModel(fSessions);
    fSessionSelector.setModel(comboModel);
    fSessionSelector.setVisible(true);
  }

  /**
   * This is just a quick, temporary, way to grab the stats data from the L2 at localhost:9520.
   */
  class RetrieveAction extends AbstractAction {
    RetrieveAction() {
      super("Retrieve...");
    }

    public void actionPerformed(ActionEvent ae) {
      String addr = (String) JOptionPane.showInputDialog(fBottomPanel, "Enter server address:", getTitle(),
                                                         JOptionPane.QUESTION_MESSAGE, null, null, "localhost:9510");
      if (addr == null) return;
      GetMethod get = null;
      try {
        fProgressBar.setVisible(true);
        String uri = "http://" + addr + "/statistics-gatherer/retrieveStatistics?format=txt";
        URL url = new URL(uri);
        HttpClient httpClient = new HttpClient();

        get = new GetMethod(url.toString());
        get.setFollowRedirects(true);
        int status = httpClient.executeMethod(get);
        if (status != HttpStatus.SC_OK) {
          System.err.println("The http client has encountered a status code other than ok for the url: " + url
                             + " status: " + HttpStatus.getStatusText(status));
          return;
        }
        setToolBarEnabled(false);
        new Thread(new StreamCopierRunnable(get)).start();
      } catch (Exception e) {
        fProgressBar.setVisible(false);
        setToolBarEnabled(true);
        e.printStackTrace();
        if (get != null) {
          get.releaseConnection();
        }
      }
    }
  }

  class StreamCopierRunnable implements Runnable {
    GetMethod fGetMethod;

    StreamCopierRunnable(GetMethod getMethod) {
      fGetMethod = getMethod;
    }

    public void run() {
      InputStream in = null;

      try {
        populateStore(in = fGetMethod.getResponseBodyAsStream());
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        IOUtils.closeQuietly(in);
        fGetMethod.releaseConnection();
      }
    }
  }

  private void filterStats(List<String> stats) {
    if (stats.contains(SRAMemoryUsage.DATA_NAME_USED)) {
      Iterator<String> iter = stats.iterator();
      while (iter.hasNext()) {
        String stat = iter.next();
        if (stat.startsWith("memory ")) {
          iter.remove();
        }
      }
      stats.add("memory usage");
    }

    if (stats.contains(SRACpuConstants.DATA_NAME_COMBINED)) {
      Iterator<String> iter = stats.iterator();
      while (iter.hasNext()) {
        String stat = iter.next();
        if (stat.startsWith("cpu ")) {
          iter.remove();
        }
      }
      stats.add("cpu usage");
    }
  }

  private void setToolBarEnabled(boolean enabled) {
    fToolBar.setEnabled(enabled);
    fImportAction.setEnabled(enabled);
    fRetrieveAction.setEnabled(enabled);
    fSessionSelector.setEnabled(enabled);
    fHeightSlider.setEnabled(enabled);
  }

  class NodeStatMappingThread extends Thread {
    public void run() {
      List<Node> nodes = fSessionInfo.fNodeList;
      Iterator<Node> nodeIter = nodes.iterator();
      fSessionInfo.fNodeStatsMap.clear();
      while (nodeIter.hasNext()) {
        Node node = nodeIter.next();
        setStatusLineLater("Determining stats for '" + node + "'");
        List<String> nodeStats = getAvailableStatsForNode(node);
        filterStats(nodeStats);
        fSessionInfo.fNodeStatsMap.put(node, nodeStats);
      }

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fStatusLine.setText("");
          setupControlPanel();
          fProgressBar.setVisible(false);
          setToolBarEnabled(true);
        }
      });
    }
  }

  private List<String> getAvailableStatsForNode(Node node) {
    try {
      return fStore.getAvailableStatsForNode(fSessionInfo.fId, node);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<String>();
    }
  }

  private void setupControlPanel() {
    if (fControlPanel == null) {
      fControlPanel = new JPanel(new BorderLayout());
      fControlPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
      fBottomPanel.add(new JScrollPane(fControlPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.WEST);
    } else {
      fControlPanel.removeAll();
    }
    List<Node> nodes = fSessionInfo.fNodeList;
    Iterator<Node> nodeIter = nodes.iterator();
    JPanel buttonPanel = new JPanel(new GridBagLayout());
    buttonPanel.add(new JButton(new GenerateGraphsAction()));
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel gridPanel = new JPanel(new GridBagLayout());
    fControlPanel.add(gridPanel, BorderLayout.NORTH);
    gbc.gridx = 0;
    gbc.ipady = gbc.ipadx = 10;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gridPanel.add(buttonPanel);
    while (nodeIter.hasNext()) {
      Node node = nodeIter.next();
      List<String> nodeStats = fSessionInfo.fNodeStatsMap.get(node);
      if (nodeStats != null && nodeStats.size() > 0) {
        gridPanel.add(createNodeGroup(node, nodeStats), gbc);
      }
    }
    fControlPanel.revalidate();
    fControlPanel.repaint();
  }

  private Container createNodeGroup(Node node, List<String> stats) {
    JPanel panel = new JPanel(new GridLayout(stats.size() + 1, 1));
    Insets insets = new Insets(0, 20, 0, 0);
    NodeControl nodeControl = new NodeControl(node);
    panel.add(nodeControl);
    Iterator<String> iter = stats.iterator();
    while (iter.hasNext()) {
      String stat = iter.next();
      StatControl statControl = new StatControl(stat);
      statControl.setMargin(insets);
      nodeControl.addStat(statControl);
      panel.add(statControl);
    }
    fNodeControlList.add(nodeControl);
    return panel;
  }

  private void buildNodeGraphs(List<DataDisplay> displayList, List<StatisticData> threadDumps) {
    Iterator<DataDisplay> displayIter = displayList.iterator();
    while (displayIter.hasNext()) {
      buildNodeGraph(displayIter.next(), threadDumps);
    }
  }

  private JFreeChart buildNodeGraph(DataDisplay display, List<StatisticData> threadDumps) {
    DateAxis timeAxis = new DateAxis("");
    timeAxis.setLowerMargin(0.02);
    timeAxis.setUpperMargin(0.02);
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
    XYToolTipGenerator toolTipGenerator = StandardXYToolTipGenerator.getTimeSeriesInstance();
    renderer.setBaseToolTipGenerator(toolTipGenerator);
    XYPlot plot = new XYPlot(display.fXYDataset, timeAxis, display.fAxis, renderer);

    Iterator<StatisticData> threadDumpIter = threadDumps.iterator();
    while (threadDumpIter.hasNext()) {
      StatisticData threadDump = threadDumpIter.next();
      Date moment = threadDump.getMoment();
      String text = (String) threadDump.getData();
      ThreadDumpImageAnnotation annotation = new ThreadDumpImageAnnotation(text, moment);
      plot.addAnnotation(annotation);
    }

    JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, display.fShowLegend);
    final ChartPanel chartPanel = new ChartPanel(chart, false);
    chartPanel.setBorder(new TitledBorder(display.getTitle()));
    fGraphPanel.add(chartPanel);
    chartPanel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          ChartEntity ce = chartPanel.getEntityForPoint(e.getX(), e.getY());
          if (ce != null) {
            if (ce instanceof ThreadDumpAnnotationEntity) {
              showThreadDump(((ThreadDumpAnnotationEntity) ce).fText);
            }
          }
        }
      }
    });
    chartPanel.setPreferredSize(fDefaultGraphSize);
    reserveRangeAxisSpace((Graphics2D) chartPanel.getGraphics(), plot);

    return chart;
  }

  private void reserveRangeAxisSpace(Graphics2D graphics, XYPlot plot) {
    ValueAxis axis = plot.getRangeAxis();
    double currLeft = fRangeAxisSpace.getLeft();
    double tickWidth = getRangeAxisTickWidth(graphics, plot);
    Rectangle2D labelArea = getLabelEnclosure(axis, graphics, RectangleEdge.LEFT);
    double totalWidth = tickWidth + labelArea.getWidth();
    if (totalWidth > currLeft) {
      fRangeAxisSpace.setLeft(totalWidth);
    }
  }

  private Rectangle2D getLabelEnclosure(Axis axis, Graphics2D g2, RectangleEdge edge) {
    Rectangle2D result = new Rectangle2D.Double();
    String axisLabel = axis.getLabel();
    if (axisLabel != null && !axisLabel.equals("")) {
      FontMetrics fm = g2.getFontMetrics(axis.getLabelFont());
      Rectangle2D bounds = TextUtilities.getTextBounds(axisLabel, g2, fm);
      RectangleInsets insets = axis.getLabelInsets();
      bounds = insets.createOutsetRectangle(bounds);
      double angle = axis.getLabelAngle();
      if (edge == RectangleEdge.LEFT || edge == RectangleEdge.RIGHT) {
        angle = angle - Math.PI / 2.0;
      }
      double x = bounds.getCenterX();
      double y = bounds.getCenterY();
      AffineTransform transformer = AffineTransform.getRotateInstance(angle, x, y);
      Shape labelBounds = transformer.createTransformedShape(bounds);
      result = labelBounds.getBounds2D();
    }

    return result;
  }

  private double getRangeAxisTickWidth(Graphics graphics, XYPlot plot) {
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    RectangleInsets tickLabelInsets = numberAxis.getTickLabelInsets();
    NumberTickUnit unit = numberAxis.getTickUnit();

    // look at lower and upper bounds...
    FontMetrics fm = graphics.getFontMetrics(numberAxis.getTickLabelFont());
    Range range = numberAxis.getRange();
    double lower = range.getLowerBound();
    double upper = range.getUpperBound();
    String lowerStr = "";
    String upperStr = "";
    NumberFormat formatter = numberAxis.getNumberFormatOverride();
    if (formatter != null) {
      lowerStr = formatter.format(lower);
      upperStr = formatter.format(upper);
    } else {
      lowerStr = unit.valueToString(lower);
      upperStr = unit.valueToString(upper);
    }
    double w1 = fm.stringWidth(lowerStr);
    double w2 = fm.stringWidth(upperStr);
    return Math.max(w1, w2) + tickLabelInsets.getLeft() + tickLabelInsets.getRight();
  }

  private void showThreadDump(String text) {
    JTextArea textArea = new JTextArea(text);
    JScrollPane scrollPane = new JScrollPane(textArea);
    JDialog dialog = new JDialog(this, getTitle());
    dialog.getContentPane().add(scrollPane);
    textArea.setRows(20);
    textArea.setColumns(80);
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);
  }

  class NodeGroupHandler {
    Node                fNode;
    List<String>        fStats;
    List<StatisticData> fThreadDumps;

    NodeGroupHandler(Node node, List<String> stats) {
      fNode = node;
      fStats = stats;
    }

    private List<StatisticData> getThreadDumps() {
      final List<StatisticData> list = new ArrayList<StatisticData>();
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAThreadDump.ACTION_NAME);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          list.add(sd);
          return true;
        }
      });
      return list;
    }

    List<DataDisplay> generateDisplay() {
      fThreadDumps = getThreadDumps();
      ArrayList<DataDisplay> displayList = new ArrayList<DataDisplay>();
      Iterator<String> statIter = fStats.iterator();
      while (statIter.hasNext()) {
        String stat = statIter.next();
        AbstractStatHandler handler = getStatHandler(stat);
        if (handler != null) {
          handler.setNode(fNode);
          handler.setLegend(stat);
          setStatusLineLater("Generating display for '" + fNode + ":" + stat + "'");
          DataDisplay display = handler.generateDisplay();
          if (display.fXYDataset.getSeriesCount() > 0) {
            displayList.add(display);
          }
        }
      }
      return displayList;
    }
  }

  abstract class AbstractStatHandler {
    Node   fNode;
    String fLegend;

    void setNode(Node node) {
      fNode = node;
    }

    void setLegend(String legend) {
      fLegend = legend;
    }

    abstract String getName();

    abstract TimeSeriesCollection generateTimeSeriesCollection();

    abstract DataDisplay generateDisplay();

    abstract NumberAxis generateAxis();
  }

  class MemoryUsageHandler extends AbstractStatHandler {
    private Long fMax;

    String getName() {
      return "memory usage";
    }

    TimeSeriesCollection generateTimeSeriesCollection() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAMemoryUsage.DATA_NAME_MAX);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          fMax = (Long) sd.getData();
          return false;
        }
      });

      criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAMemoryUsage.DATA_NAME_USED);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          Long value = (Long) sd.getData();
          Date moment = sd.getMoment();
          if (value != null && moment != null) {
            double d = (value / ((double) fMax));
            series.addOrUpdate(new Second(moment), d);
          }
          return true;
        }
      });

      return new TimeSeriesCollection(series);
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis();
      axis.setRange(0.0, 1.0);
      return axis;
    }

    DataDisplay generateDisplay() {
      TimeSeriesCollection tsc = generateTimeSeriesCollection();
      DataDisplay display = new DataDisplay(fNode + " (Memory Usage)", tsc, generateAxis(), false);
      return display;
    }

  }

  class StageQueueDepthsHandler extends AbstractStatHandler {
    String getName() {
      return SRAStageQueueDepths.ACTION_NAME;
    }

    TimeSeriesCollection generateTimeSeriesCollection() {
      final TimeSeriesCollection result = new TimeSeriesCollection();
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAStageQueueDepths.ACTION_NAME);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final Map<String, TimeSeries> seriesMap = new HashMap<String, TimeSeries>();
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          Number value = (Number) sd.getData();
          Date moment = sd.getMoment();
          if (moment != null && value != null) {
            String element = sd.getElement();
            TimeSeries series = seriesMap.get(element);
            if (series == null) {
              seriesMap.put(element, series = new TimeSeries(element, Second.class));
              result.addSeries(series);
            }
            series.addOrUpdate(new Second(moment), value.longValue());
          }
          return true;
        }
      });

      int seriesCount = result.getSeriesCount();
      for (int i = seriesCount - 1; i >= 0; i--) {
        TimeSeries series = result.getSeries(i);
        int itemCount = series.getItemCount();
        boolean foundData = false;
        for (int j = 0; j < itemCount; j++) {
          TimeSeriesDataItem item = series.getDataItem(j);
          if (item.getValue().doubleValue() > 0.0) {
            foundData = true;
            break;
          }
        }
        if (!foundData) {
          result.removeSeries(i);
        }
      }

      return result;
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis();
      axis.setRangeType(RangeType.POSITIVE);
      axis.setNumberFormatOverride(new DecimalFormat("0.0"));
      return axis;
    }

    DataDisplay generateDisplay() {
      TimeSeriesCollection tsc = generateTimeSeriesCollection();
      DataDisplay display = new DataDisplay(fNode + " (SEDA Queue Depths)", tsc, generateAxis(), true);
      return display;
    }

  }

  class CPUUsageHandler extends AbstractStatHandler {
    String getName() {
      return "cpu usage";
    }

    TimeSeriesCollection generateTimeSeriesCollection() {
      final TimeSeriesCollection result = new TimeSeriesCollection();
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRACpuConstants.DATA_NAME_COMBINED);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final Map<String, TimeSeries> seriesMap = new HashMap<String, TimeSeries>();
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          Number value = (Number) sd.getData();
          Date moment = sd.getMoment();
          if (moment != null && value != null) {
            String element = sd.getElement();
            String key = fNode.fIpAddr + ":" + element;
            TimeSeries series = seriesMap.get(key);
            if (series == null) {
              seriesMap.put(key, series = new TimeSeries(element, Second.class));
              result.addSeries(series);
            }
            series.addOrUpdate(new Second(moment), value.doubleValue());
          }
          return true;
        }
      });

      return result;
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis();
      axis.setRange(0.0, 1.0);
      return axis;
    }

    DataDisplay generateDisplay() {
      TimeSeriesCollection tsc = generateTimeSeriesCollection();
      DataDisplay display = new DataDisplay(fNode + " (CPU)", tsc, generateAxis(), tsc.getSeriesCount() > 1);
      return display;
    }

  }

  class L2toL1FaultHandler extends AbstractStatHandler {
    String getName() {
      return SRAL2ToL1FaultRate.ACTION_NAME;
    }

    TimeSeriesCollection generateTimeSeriesCollection() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAL2ToL1FaultRate.ACTION_NAME);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          Number value = (Number) sd.getData();
          Date moment = sd.getMoment();
          if (value != null && moment != null) {
            series.addOrUpdate(new Second(moment), value);
          }
          return true;
        }
      });

      return new TimeSeriesCollection(series);
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis();
      axis.setRangeType(RangeType.POSITIVE);
      axis.setNumberFormatOverride(new DecimalFormat("0.0"));
      return axis;
    }

    DataDisplay generateDisplay() {
      TimeSeriesCollection tsc = generateTimeSeriesCollection();
      DataDisplay display = new DataDisplay(fNode + " (L2-L1 Faults)", tsc, generateAxis(), false);
      return display;
    }
  }

  class L2BroadcastHandler extends AbstractStatHandler {
    String getName() {
      return SRAL2BroadcastCount.ACTION_NAME;
    }

    TimeSeriesCollection generateTimeSeriesCollection() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAL2BroadcastCount.ACTION_NAME);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          Number value = (Number) sd.getData();
          Date moment = sd.getMoment();
          if (value != null && moment != null) {
            series.addOrUpdate(new Second(moment), value);
          }
          return true;
        }
      });

      return new TimeSeriesCollection(series);
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis();
      axis.setRangeType(RangeType.POSITIVE);
      axis.setNumberFormatOverride(new DecimalFormat("0.0"));
      return axis;
    }

    DataDisplay generateDisplay() {
      TimeSeriesCollection tsc = generateTimeSeriesCollection();
      DataDisplay display = new DataDisplay(fNode + " (Broadcasts/sec.)", tsc, generateAxis(), false);
      return display;
    }
  }

  class L2BroadcastsPerTxHandler extends AbstractStatHandler {
    String getName() {
      return SRAL2BroadcastPerTransaction.ACTION_NAME;
    }

    TimeSeriesCollection generateTimeSeriesCollection() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAL2BroadcastPerTransaction.ACTION_NAME);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          Number value = (Number) sd.getData();
          Date moment = sd.getMoment();
          if (value != null && moment != null) {
            series.addOrUpdate(new Second(moment), value);
          }
          return true;
        }
      });

      return new TimeSeriesCollection(series);
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis();
      axis.setRangeType(RangeType.POSITIVE);
      axis.setNumberFormatOverride(new DecimalFormat("0.0"));
      return axis;
    }

    DataDisplay generateDisplay() {
      TimeSeriesCollection tsc = generateTimeSeriesCollection();
      DataDisplay display = new DataDisplay(fNode + " (Broadcasts/txn.)", tsc, generateAxis(), false);
      return display;
    }
  }

  class L2ChangesPerBroadcastHandler extends AbstractStatHandler {
    String getName() {
      return SRAL2ChangesPerBroadcast.ACTION_NAME;
    }

    TimeSeriesCollection generateTimeSeriesCollection() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAL2ChangesPerBroadcast.ACTION_NAME);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          Number value = (Number) sd.getData();
          Date moment = sd.getMoment();
          if (value != null && moment != null) {
            series.addOrUpdate(new Second(moment), value);
          }
          return true;
        }
      });

      return new TimeSeriesCollection(series);
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis();
      axis.setRangeType(RangeType.POSITIVE);
      axis.setNumberFormatOverride(new DecimalFormat("0.0"));
      return axis;
    }

    DataDisplay generateDisplay() {
      TimeSeriesCollection tsc = generateTimeSeriesCollection();
      DataDisplay display = new DataDisplay(fNode + " (Changes/Broadcast)", tsc, generateAxis(), false);
      return display;
    }
  }

  class TxRateHandler extends AbstractStatHandler {
    String getName() {
      return SRAL2TransactionCount.ACTION_NAME;
    }

    TimeSeriesCollection generateTimeSeriesCollection() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName(SRAL2TransactionCount.ACTION_NAME);
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          Number value = (Number) sd.getData();
          Date moment = sd.getMoment();
          if (value != null && moment != null) {
            series.addOrUpdate(new Second(moment), value);
          }
          return true;
        }
      });

      return new TimeSeriesCollection(series);
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis();
      axis.setRangeType(RangeType.POSITIVE);
      return axis;
    }

    DataDisplay generateDisplay() {
      TimeSeriesCollection tsc = generateTimeSeriesCollection();
      DataDisplay display = new DataDisplay(fNode + " (Transaction Rate)", tsc, generateAxis(), false);
      return display;
    }
  }

  void putStatHandler(AbstractStatHandler handler) {
    fStatHandlerMap.put(handler.getName(), handler);
  }

  AbstractStatHandler getStatHandler(String stat) {
    return fStatHandlerMap.get(stat);
  }

  void safeRetrieveStatistics(StatisticsRetrievalCriteria criteria, StatisticDataUser consumer) {
    try {
      fStore.retrieveStatistics(criteria, consumer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class ExitAction extends AbstractAction {
    ExitAction() {
      super("Exit");
    }

    public void actionPerformed(ActionEvent ae) {
      try {
        if (fStore != null) {
          fStore.close();
        }

        Preferences prefs = Preferences.userNodeForPackage(ClusterVisualizerFrame.class);
        Preferences locationPrefs = prefs.node("bounds");
        locationPrefs.putInt("x", getX());
        locationPrefs.putInt("y", getY());
        locationPrefs.putInt("width", getWidth());
        locationPrefs.putInt("height", getHeight());
        locationPrefs.flush();
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.exit(0);
    }
  }

  class NodeControl extends JCheckBox implements ActionListener {
    Node              fNode;
    List<StatControl> fStatControlList = new ArrayList<StatControl>();

    NodeControl(Node node) {
      super(node.toString());
      fNode = node;
      setSelected(true);
      addActionListener(this);
    }

    public void actionPerformed(ActionEvent ae) {
      boolean selected = isSelected();
      Iterator<StatControl> statControlIter = fStatControlList.iterator();
      while (statControlIter.hasNext()) {
        StatControl control = statControlIter.next();
        control.setSelected(selected);
      }
    }

    List<String> getSelectedStats() {
      List<String> statList = new ArrayList<String>();
      Iterator<StatControl> statControlIter = fStatControlList.iterator();
      while (statControlIter.hasNext()) {
        StatControl control = statControlIter.next();
        if (control.isSelected()) {
          statList.add(control.fStat);
        }
      }
      return statList;
    }

    Node getNode() {
      return fNode;
    }

    void addStat(StatControl statControl) {
      fStatControlList.add(statControl);
    }
  }

  class StatControl extends JCheckBox {
    String fStat;

    StatControl(String stat) {
      super(stat);
      fStat = stat;
      setSelected(true);
      setName(stat);
    }

    String getStat() {
      return fStat;
    }
  }

  private static Rectangle getDefaultBounds() {
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension size = tk.getScreenSize();
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice device = env.getDefaultScreenDevice();
    GraphicsConfiguration config = device.getDefaultConfiguration();
    Insets insets = tk.getScreenInsets(config);

    size.width -= (insets.left + insets.right);
    size.height -= (insets.top + insets.bottom);

    int width = (int) (size.width * 0.75f);
    int height = (int) (size.height * 0.66f);

    // center
    int x = size.width / 2 - width / 2;
    int y = size.height / 2 - height / 2;

    return new Rectangle(x, y, width, height);
  }

  public static void main(String[] args) throws Exception {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    ClusterVisualizerFrame frame = new ClusterVisualizerFrame(args);
    Preferences prefs = Preferences.userNodeForPackage(ClusterVisualizerFrame.class);
    Preferences locationPrefs = prefs.node("bounds");
    Rectangle bounds = new Rectangle();
    if (locationPrefs != null) {
      bounds.x = locationPrefs.getInt("x", 0);
      bounds.y = locationPrefs.getInt("y", 0);
      bounds.width = locationPrefs.getInt("width", 800);
      bounds.height = locationPrefs.getInt("height", 700);
    } else {
      bounds = getDefaultBounds();
    }
    frame.setBounds(bounds);
    frame.setVisible(true);
  }
}

class Node {
  String fIpAddr;
  String fName;

  Node(String ipAddr, String name) {
    fIpAddr = ipAddr;
    fName = name;
  }

  public int hashCode() {
    HashCodeBuilder hcb = new HashCodeBuilder();
    return hcb.append(fIpAddr).append(fName).toHashCode();
  }

  public boolean equals(Object other) {
    if (other == null) return false;
    if (!(other instanceof Node)) return false;
    if (other == this) return true;
    Node otherNode = (Node) other;
    return fIpAddr.equals(otherNode.fIpAddr) && fName.equals(otherNode.fName);
  }

  public String toString() {
    return fIpAddr + " " + fName;
  }
}

class DataDisplay {
  String     fTitle;
  XYDataset  fXYDataset;
  NumberAxis fAxis;
  boolean    fShowLegend;

  DataDisplay(String title, XYDataset xyDataset, NumberAxis axis, boolean showLegend) {
    fTitle = title;
    fXYDataset = xyDataset;
    fAxis = axis;
    fShowLegend = showLegend;
  }

  public DataDisplay createCopy(String name) {
    try {
      TimeSeries ts = ((TimeSeriesCollection) fXYDataset).getSeries(0);
      ts.setKey(name);
      TimeSeriesCollection tsc = new TimeSeriesCollection(ts);
      DataDisplay dd = new DataDisplay(fTitle, tsc, (NumberAxis) fAxis.clone(), fShowLegend);
      return dd;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public String getTitle() {
    return fTitle;
  }

  public String toString() {
    return ((TimeSeriesCollection) fXYDataset).getSeries(0).getKey() + ":" + fAxis.getLabel();
  }
}

class SessionInfo {
  String                  fId;
  Date                    fStart;
  Date                    fEnd;
  List<Node>              fNodeList;
  Map<Node, List<String>> fNodeStatsMap;
  List<String>            fStatList;
  Map<String, List<Node>> fStatNodesMap;

  SessionInfo(String id) {
    fId = id;
    fNodeStatsMap = new HashMap<Node, List<String>>();
    fStatNodesMap = new HashMap<String, List<Node>>();
  }

  public String toString() {
    return fId;
  }
}

class ThreadDumpImageAnnotation extends XYImageAnnotation {
  String       fText;
  Date         fMoment;
  static Image fImage;

  static {
    URL url = ThreadDumpImageAnnotation.class.getResource("/com/tc/admin/icons/jspbrkpt_obj.gif");
    fImage = Toolkit.getDefaultToolkit().getImage(url);
  }

  ThreadDumpImageAnnotation(String text, Date moment) {
    super(moment.getTime(), 0, fImage, RectangleAnchor.BOTTOM);
    fText = text;
    fMoment = moment;
    setToolTipText("Thread dump");
  }

  protected void addEntity(PlotRenderingInfo info, Shape hotspot, int rendererIndex, String toolTipText, String urlText) {
    if (info == null) { return; }
    EntityCollection entities = info.getOwner().getEntityCollection();
    if (entities == null) { return; }
    ThreadDumpAnnotationEntity entity = new ThreadDumpAnnotationEntity(hotspot, rendererIndex, toolTipText, urlText);
    entities.add(entity);
    entity.fText = fText;
  }
}

class ThreadDumpTextAnnotation extends XYTextAnnotation {
  String fText;
  Date   fMoment;

  ThreadDumpTextAnnotation(String text, Date moment) {
    super("thread dump", moment.getTime(), 0);
    fText = text;
    fMoment = moment;
    setRotationAngle(Math.PI / 2.0);
    setTextAnchor(TextAnchor.BOTTOM_CENTER);
    setToolTipText("Thread dump");
  }

  protected void addEntity(PlotRenderingInfo info, Shape hotspot, int rendererIndex, String toolTipText, String urlText) {
    if (info == null) { return; }
    EntityCollection entities = info.getOwner().getEntityCollection();
    if (entities == null) { return; }
    ThreadDumpAnnotationEntity entity = new ThreadDumpAnnotationEntity(hotspot, rendererIndex, toolTipText, urlText);
    entities.add(entity);
    entity.fText = fText;
  }
}

class ThreadDumpAnnotationEntity extends XYAnnotationEntity {
  String fText;

  ThreadDumpAnnotationEntity(Shape hotspot, int rendererIndex, String toolTipText, String urlText) {
    super(hotspot, rendererIndex, toolTipText, urlText);
  }
}

class MyStatisticsStore extends H2StatisticsStoreImpl {
  private final static String       SQL_GET_NODES          = "SELECT agentIp, agentDifferentiator FROM cachedstatlogstructure WHERE sessionid = ? GROUP BY agentIp, agentDifferentiator ORDER BY agentIp ASC";
  private final static String       SQL_GET_STATS          = "SELECT statname FROM cachedstatlogstructure WHERE sessionid = ? GROUP BY statname ORDER BY statname ASC";
  private final static String       SQL_GET_STATS_FOR_NODE = "SELECT statname FROM cachedstatlogstructure WHERE sessionid = ? AND agentIp = ? AND agentDifferentiator = ? GROUP BY statname ORDER BY statname ASC";
  private final static String       SQL_GET_NODES_FOR_STAT = "SELECT agentIp, agentDifferentiator FROM cachedstatlogstructure WHERE sessionid = ? AND statname = ? GROUP BY agentIp, agentDifferentiator ORDER BY agentIp ASC";

  private static final List<String> STATS_NON_GRATIS       = new ArrayList<String>();

  static {
    STATS_NON_GRATIS.addAll(Arrays.asList(new String[] { SRAStartupTimestamp.ACTION_NAME,
      SRAShutdownTimestamp.ACTION_NAME, SRASystemProperties.ACTION_NAME, SRAThreadDump.ACTION_NAME,
      SRACpuConstants.DATA_NAME_IDLE, SRACpuConstants.DATA_NAME_NICE, SRACpuConstants.DATA_NAME_WAIT,
      SRACpuConstants.DATA_NAME_SYS, SRACpuConstants.DATA_NAME_USER, SRAMemoryUsage.DATA_NAME_FREE,
      SRAMemoryUsage.DATA_NAME_MAX }));
  }

  public MyStatisticsStore(File dir) {
    super(dir);
  }

  public boolean isNonGratis(String stat) {
    return STATS_NON_GRATIS.contains(stat);
  }

  public synchronized void open() throws StatisticsStoreException {
    super.open();
    try {
      database.createPreparedStatement(SQL_GET_NODES);
      database.createPreparedStatement(SQL_GET_STATS);
      database.createPreparedStatement(SQL_GET_STATS_FOR_NODE);
      database.createPreparedStatement(SQL_GET_NODES_FOR_STAT);
    } catch (StatisticsDatabaseException e) {
      throw new StatisticsStoreSetupErrorException(e);
    }
  }

  public List<SessionInfo> getAllSessions() throws StatisticsStoreException {
    String[] ids = getAvailableSessionIds();
    List<SessionInfo> result = new ArrayList<SessionInfo>();
    for (String id : ids) {
      final SessionInfo sessionInfo = new SessionInfo(id);
      StatisticsRetrievalCriteria critera = new StatisticsRetrievalCriteria();
      critera.addName(SRAStartupTimestamp.ACTION_NAME);
      critera.setSessionId(id);
      retrieveStatistics(critera, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          sessionInfo.fStart = sd.getMoment();
          return false;
        }
      });
      critera = new StatisticsRetrievalCriteria();
      critera.addName(SRAShutdownTimestamp.ACTION_NAME);
      critera.setSessionId(id);
      retrieveStatistics(critera, new StatisticDataUser() {
        public boolean useStatisticData(StatisticData sd) {
          sessionInfo.fEnd = sd.getMoment();
          return false;
        }
      });
      sessionInfo.fNodeList = getAvailableNodes(id);
      sessionInfo.fStatList = getAvailableStats(id);

      result.add(sessionInfo);
    }
    return result;
  }

  public List<Node> getAvailableNodes(String sessionId) throws StatisticsStoreException {
    final List<Node> results = new ArrayList<Node>();
    try {
      database.ensureExistingConnection();
      PreparedStatement ps = database.getPreparedStatement(SQL_GET_NODES);
      ps.setString(1, sessionId);
      JdbcHelper.executeQuery(ps, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            Node node = new Node(resultSet.getString("agentIp"), resultSet.getString("agentDifferentiator"));
            results.add(node);
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new StatisticsStoreException("getAvailableNodes", e);
    }

    return results;
  }

  public List<String> getAvailableStats(String sessionId) throws StatisticsStoreException {
    final List<String> results = new ArrayList<String>();
    try {
      database.ensureExistingConnection();
      PreparedStatement ps = database.getPreparedStatement(SQL_GET_STATS);
      ps.setString(1, sessionId);
      JdbcHelper.executeQuery(ps, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            results.add(new String(resultSet.getString("statname")));
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new StatisticsStoreException("getAvailableStats", e);
    }

    results.removeAll(STATS_NON_GRATIS);

    return results;
  }

  public List<String> getAvailableStatsForNode(String sessionId, Node node) throws StatisticsStoreException {
    final List<String> results = new ArrayList<String>();
    try {
      database.ensureExistingConnection();
      PreparedStatement ps = database.getPreparedStatement(SQL_GET_STATS_FOR_NODE);
      ps.setString(1, sessionId);
      ps.setString(2, node.fIpAddr);
      ps.setString(3, node.fName);
      JdbcHelper.executeQuery(ps, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            results.add(new String(resultSet.getString("statname")));
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new StatisticsStoreException("getAvailableStatsForNode", e);
    }

    results.removeAll(STATS_NON_GRATIS);

    return results;
  }

  public List<Node> getAvailableNodesForStat(String sessionId, String stat) throws StatisticsStoreException {
    final List<Node> results = new ArrayList<Node>();
    try {
      database.ensureExistingConnection();
      PreparedStatement ps = database.getPreparedStatement(SQL_GET_NODES_FOR_STAT);
      ps.setString(1, sessionId);
      ps.setString(2, stat);
      JdbcHelper.executeQuery(ps, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            Node node = new Node(resultSet.getString("agentIp"), resultSet.getString("agentDifferentiator"));
            results.add(node);
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new StatisticsStoreException("getAvailableNodesForStat", e);
    }

    return results;
  }

}