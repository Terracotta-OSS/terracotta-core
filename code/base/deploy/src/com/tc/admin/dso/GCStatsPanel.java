/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.FastFileChooser;
import com.tc.admin.common.RolloverButton;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.model.DGCListener;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.objectserver.api.GCStats;
import com.tc.util.ProductInfo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

public class GCStatsPanel extends XContainer implements DGCListener {
  private ApplicationContext appContext;
  private IClusterModel      clusterModel;
  private ClusterListener    clusterListener;
  private XObjectTable       table;
  private XLabel             overviewLabel;
  private JPopupMenu         popupMenu;
  private RunGCAction        gcAction;
  private boolean            inited;
  private final ChartPanel   chartPanel;
  private JPopupMenu         fChartPopupMenu;
  private TimeSeries         endObjectCountSeries;
  private XYPlot             endObjectCountPlot;
  private DGCIntervalMarker  currentDGCMarker;
  private DomainZoomListener fDomainZoomListener;
  private boolean            fHandlingAxisChange;
  private boolean            fZoomed;
  private AbstractAction     fRestoreDefaultRangeAction;
  private ExportAction       exportAction;
  private File               lastExportDir;

  public GCStatsPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridy = gbc.gridx = 0;
    gbc.weightx = 0.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(3, 3, 3, 3);

    topPanel.add(overviewLabel = new XLabel(), gbc);
    overviewLabel.setText(appContext.getString("dso.gcstats.overview.pending"));
    gbc.gridx++;

    RolloverButton helpButton = new RolloverButton();
    helpButton.setIcon(new ImageIcon(getClass().getResource("/com/tc/admin/icons/help.gif")));
    helpButton.addActionListener(new HelpButtonHandler());
    helpButton.setFocusable(false);
    topPanel.add(helpButton);
    gbc.gridx++;

    gbc.weightx = 1.0;
    XButton runDGCButton = new XButton();
    runDGCButton.setAction(gcAction = new RunGCAction());
    gbc.anchor = GridBagConstraints.EAST;
    topPanel.add(runDGCButton, gbc);
    gbc.gridx++;

    XButton exportButton = new XButton();
    exportButton.setAction(exportAction = new ExportAction());
    topPanel.add(exportButton, gbc);
    gbc.gridx++;

    XContainer gcStatsPanel = new XContainer(new BorderLayout());
    gcStatsPanel.add(topPanel, BorderLayout.NORTH);

    table = new GCStatsTable();
    table.setModel(new GCStatsTableModel(appContext));
    gcStatsPanel.add(new XScrollPane(table), BorderLayout.CENTER);

    popupMenu = new JPopupMenu("DGC");
    popupMenu.add(gcAction);
    table.add(popupMenu);
    table.addMouseListener(new TableMouseHandler());

    endObjectCountSeries = new TimeSeries(appContext.getString("end.object.count"), Second.class);
    JFreeChart chart = DemoChartFactory.getXYLineChart("", "", "", new TimeSeries[] { endObjectCountSeries }, false);
    endObjectCountPlot = (XYPlot) chart.getPlot();
    chartPanel = BaseRuntimeStatsPanel.createChartPanel(chart);
    chartPanel.setDomainZoomable(true);
    chartPanel.setRangeZoomable(false);
    chart.getXYPlot().getDomainAxis().addChangeListener(getDomainZoomListener());
    ((NumberAxis) chart.getXYPlot().getRangeAxis()).setAutoRangeIncludesZero(true);
    chartPanel.setPopupMenu(getChartPopupMenu());
    chartPanel.setToolTipText(appContext.getString("dgc.tip"));
    chartPanel.setBorder(BorderFactory.createTitledBorder(appContext.getString("end.object.count")));

    chartPanel.setMinimumSize(new Dimension(0, 0));
    chartPanel.setPreferredSize(new Dimension(0, 200));
    chartPanel.addMouseListener(new ChartMouseHandler());

    XSplitPane splitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, gcStatsPanel, chartPanel);
    splitter.setDefaultDividerLocation(0.65);
    splitter.setPreferences(appContext.getPrefs().node("GCStatsPanel/Split"));
    add(splitter);

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      IServer activeCoord = clusterModel.getActiveCoordinator();
      if (activeCoord != null) {
        activeCoord.addDGCListener(this);
      }
      init();
    } else {
      overviewLabel.setText(appContext.getString("dso.gcstats.overview.not-ready"));
    }
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  private synchronized IServer getActiveCoordinator() {
    IClusterModel theClusterModel = getClusterModel();
    return theClusterModel != null ? theClusterModel.getActiveCoordinator() : null;
  }

  private class InitOverviewTextWorker extends BasicWorker<String> {
    private InitOverviewTextWorker() {
      super(new Callable<String>() {
        public String call() {
          IServer activeCoord = getActiveCoordinator();
          if (activeCoord != null) {
            if (activeCoord.isGarbageCollectionEnabled()) {
              int seconds = activeCoord.getGarbageCollectionInterval();
              float minutes = seconds / 60f;
              return appContext.format("dso.gcstats.overview.enabled", seconds, minutes);
            } else {
              return appContext.getString("dso.gcstats.overview.disabled");
            }
          }
          return "";
        }
      });
      overviewLabel.setText(appContext.getString("dso.gcstats.overview.pending"));
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
      } else {
        overviewLabel.setText(getResult());
      }
    }
  }

  protected String getKitID() {
    String kitID = ProductInfo.getInstance().kitID();
    if (ProductInfo.UNKNOWN_VALUE.equals(kitID)) {
      kitID = System.getProperty("com.tc.kitID", "3.0");
    }
    return kitID;
  }

  protected String getHelpButtonTarget() {
    return appContext.format("console.guide.url", getKitID(), "ConsoleGuide")
           + "#TerracottaDeveloperConsole-DistributedGarbageCollection";
  }

  private class HelpButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      BrowserLauncher.openURL(getHelpButtonTarget());
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    public void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      if (oldActive != null) {
        oldActive.removeDGCListener(GCStatsPanel.this);
      }
      if (newActive != null) {
        newActive.addDGCListener(GCStatsPanel.this);
      }
    }

    @Override
    public void handleReady() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      if (clusterModel.isReady()) {
        if (!inited) {
          init();
        } else {
          appContext.submit(new InitOverviewTextWorker());
        }
      } else {
        overviewLabel.setText(appContext.getString("dso.gcstats.overview.not-ready"));
      }
      gcAction.setEnabled(clusterModel != null && clusterModel.isReady());
      exportAction.setEnabled(clusterModel != null && clusterModel.isReady());
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      if (appContext != null) {
        appContext.log(e);
      } else {
        super.handleUncaughtError(e);
      }
    }
  }

  private void init() {
    if (table != null) {
      GCStatsTableModel model = (GCStatsTableModel) table.getModel();
      model.clear();
      model.fireTableDataChanged();

      appContext.execute(new InitWorker());
      appContext.submit(new InitOverviewTextWorker());

      inited = true;
    }
  }

  private class InitWorker extends BasicWorker<GCStats[]> {
    private InitWorker() {
      super(new Callable<GCStats[]>() {
        public GCStats[] call() throws Exception {
          IServer activeCoord = getActiveCoordinator();
          return activeCoord != null ? activeCoord.getGCStats() : new GCStats[0];
        }
      });
    }

    @Override
    protected void finished() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      Exception e = getException();
      if (e == null) {
        GCStatsTableModel model = (GCStatsTableModel) table.getModel();
        GCStats[] stats = getResult();
        model.setGCStats(stats);
        for (GCStats stat : stats) {
          if (stat.getElapsedTime() != -1) {
            addDGCEvent(stat);
          }
        }
        setRange();
      }
      gcAction.setEnabled(true);
      exportAction.setEnabled(true);
    }
  }

  private void setRange() {
    DateAxis dateAxis = (DateAxis) endObjectCountPlot.getDomainAxis();
    GCStatsTableModel model = (GCStatsTableModel) table.getModel();
    long lower = model.getLastStartTime();
    long upper = model.getFirstEndTime();
    if (lower < upper) {
      dateAxis.setRange(lower, upper);
    }
    chartPanel.restoreAutoRangeBounds();
  }

  private void updateCurrentDGCMarker(GCStats gcStats) {
    if (currentDGCMarker == null) {
      currentDGCMarker = new DGCIntervalMarker(gcStats);
      endObjectCountPlot.addDomainMarker(currentDGCMarker, Layer.FOREGROUND);
    } else {
      currentDGCMarker.setGCStats(gcStats);
    }
    if (gcStats.getElapsedTime() != -1) {
      currentDGCMarker = null;
    }
  }

  class TableMouseHandler extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      testPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      testPopup(e);
    }

    public void testPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        popupMenu.show(table, e.getX(), e.getY());
      }
    }
  }

  class RunGCAction extends XAbstractAction {
    RunGCAction() {
      super("Run DGC");
      setEnabled(clusterModel != null && clusterModel.isReady());
    }

    public void actionPerformed(ActionEvent ae) {
      runGC();
    }
  }

  private class ExportAction extends XAbstractAction {
    public ExportAction() {
      super("Export");
      setEnabled(clusterModel != null && clusterModel.isReady());
    }

    public void actionPerformed(ActionEvent e) {
      try {
        exportAsText();
      } catch (Exception e1) {
        appContext.log(e1);
      }
    }
  }

  private void exportAsText() throws Exception {
    FastFileChooser chooser = new FastFileChooser();
    if (lastExportDir != null) chooser.setCurrentDirectory(lastExportDir);
    chooser.setDialogTitle("Export DGC Cycles");
    chooser.setMultiSelectionEnabled(false);
    chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "dgc.txt"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    FileOutputStream fos = new FileOutputStream(file);
    lastExportDir = file.getParentFile();
    GCStatsTableModel tableModel = (GCStatsTableModel) this.table.getModel();
    String eventsAsText = tableModel.exportAsText();
    fos.write(eventsAsText.getBytes("UTF-8"));
    fos.close();
  }

  public void statusUpdate(GCStats gcStats) {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel == null) { return; }

    SwingUtilities.invokeLater(new ModelUpdater(gcStats));
  }

  private class ModelUpdater implements Runnable {
    private final GCStats gcStats;

    private ModelUpdater(GCStats gcStats) {
      this.gcStats = gcStats;
    }

    public void run() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      gcAction.setEnabled(gcStats.getElapsedTime() != -1);
      exportAction.setEnabled(true);
      GCStatsTableModel model = (GCStatsTableModel) table.getModel();
      model.addGCStats(gcStats);
      if (gcAction.isEnabled()) {
        addDGCEvent(gcStats);
        setRange();
      }
    }
  }

  private void addDGCEvent(GCStats stats) {
    FixedMillisecond t = new FixedMillisecond(stats.getStartTime() + stats.getElapsedTime());
    endObjectCountSeries.addOrUpdate(t, stats.getEndObjectCount());
    updateCurrentDGCMarker(stats);
  }

  private class RunGCWorker extends BasicWorker<Void> {
    private RunGCWorker() {
      super(new Callable<Void>() {
        public Void call() {
          IServer activeCoord = getActiveCoordinator();
          if (activeCoord != null) {
            activeCoord.runGC();
          }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      Exception e = getException();
      if (e != null) {
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, GCStatsPanel.this);
        Throwable cause = ExceptionHelper.getRootCause(e);
        String msg = cause.getMessage();
        String title = frame.getTitle();
        JOptionPane.showMessageDialog(frame, msg, title, JOptionPane.INFORMATION_MESSAGE);
      }
    }
  }

  private void runGC() {
    appContext.execute(new RunGCWorker());
  }

  private JPopupMenu getChartPopupMenu() {
    if (fChartPopupMenu == null) {
      fChartPopupMenu = new JPopupMenu("Chart:");
      fChartPopupMenu.add(fRestoreDefaultRangeAction = new RestoreDefaultRangeAction());
    }
    return fChartPopupMenu;
  }

  private DomainZoomListener getDomainZoomListener() {
    if (fDomainZoomListener == null) fDomainZoomListener = new DomainZoomListener();
    return fDomainZoomListener;
  }

  private class DomainZoomListener implements AxisChangeListener {
    public void axisChanged(AxisChangeEvent ace) {
      if (fHandlingAxisChange) return;
      fHandlingAxisChange = true;
      DateAxis srcAxis = (DateAxis) ace.getAxis();
      Range domainRange = srcAxis.getRange();
      ValueAxis domainAxis = endObjectCountPlot.getDomainAxis();
      if (!domainAxis.equals(srcAxis)) {
        domainAxis.setRange(domainRange);
      }
      ValueAxis rangeAxis = endObjectCountPlot.getRangeAxis();
      if (rangeAxis != null) {
        Range valueRange = iterateXYRangeBounds(endObjectCountPlot.getDataset(), domainRange);
        if (valueRange != null) {
          valueRange = Range.expand(valueRange, 0.0d, 0.05d);
          rangeAxis.setRange(valueRange);
        }
      }
      fHandlingAxisChange = false;
      setZoomed(true);
    }

    /**
     * Iterates over the data item of the xy dataset to find the range bounds wrt domainRange.
     * 
     * @param dataset the dataset (<code>null</code> not permitted).
     * @param domainRange the domain range (<code>null</code> not permitted).
     * @return The range (possibly <code>null</code>).
     */
    public Range iterateXYRangeBounds(XYDataset dataset, Range domainRange) {
      double minimum = Double.POSITIVE_INFINITY;
      double maximum = Double.NEGATIVE_INFINITY;
      int seriesCount = dataset.getSeriesCount();
      for (int series = 0; series < seriesCount; series++) {
        int itemCount = dataset.getItemCount(series);
        for (int item = 0; item < itemCount; item++) {
          double xvalue = dataset.getXValue(series, item);
          if (domainRange.contains(xvalue)) {
            double lvalue = dataset.getYValue(series, item);
            double uvalue = lvalue;
            if (!Double.isNaN(lvalue)) {
              minimum = Math.min(minimum, lvalue);
            }
            if (!Double.isNaN(uvalue)) {
              maximum = Math.max(maximum, uvalue);
            }
          }
        }
      }
      if (minimum == Double.POSITIVE_INFINITY) {
        return null;
      } else {
        return new Range(minimum, maximum);
      }
    }
  }

  private void setZoomed(boolean zoomed) {
    fZoomed = zoomed;
    fRestoreDefaultRangeAction.setEnabled(zoomed);
  }

  private boolean isZoomed() {
    return fZoomed;
  }

  class RestoreDefaultRangeAction extends AbstractAction {
    RestoreDefaultRangeAction() {
      super("Restore default range");
      putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/com/tc/admin/icons/arrow_undo.png")));
      setEnabled(isZoomed());
    }

    public void actionPerformed(ActionEvent ae) {
      DateAxis dateAxis = ((DateAxis) endObjectCountPlot.getDomainAxis());
      dateAxis.removeChangeListener(getDomainZoomListener());
      setRange();
      dateAxis.addChangeListener(getDomainZoomListener());
      setZoomed(false);
    }
  }

  private class ChartMouseHandler extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent me) {
      Collection<?> domainMarkers = new HashSet();
      Collection<?> fgDomainMarkers = endObjectCountPlot.getDomainMarkers(Layer.FOREGROUND);
      if (fgDomainMarkers != null) {
        domainMarkers.addAll(new HashSet(fgDomainMarkers));
      }
      Collection<?> bgDomainMarkers = endObjectCountPlot.getDomainMarkers(Layer.BACKGROUND);
      if (bgDomainMarkers != null) {
        domainMarkers.addAll(new HashSet(bgDomainMarkers));
      }
      if (domainMarkers.size() > 0) {
        PlotRenderingInfo info = chartPanel.getChartRenderingInfo().getPlotInfo();
        Insets insets = getInsets();
        double x = (me.getX() - insets.left) / chartPanel.getScaleX();
        double xx = endObjectCountPlot.getDomainAxis().java2DToValue(x, info.getDataArea(),
                                                                     endObjectCountPlot.getDomainAxisEdge());
        Iterator<?> domainMarkerIter = domainMarkers.iterator();
        while (domainMarkerIter.hasNext()) {
          IntervalMarker marker = (IntervalMarker) domainMarkerIter.next();
          if (marker instanceof DGCIntervalMarker) {
            if (xx >= marker.getStartValue() && xx <= marker.getEndValue()) {
              DGCIntervalMarker dgcIntervalMarker = (DGCIntervalMarker) marker;
              GCStats gcStats = dgcIntervalMarker.getGCStats();
              GCStatsTableModel model = (GCStatsTableModel) table.getModel();
              int row = model.iterationRow(gcStats.getIteration());
              table.setSelectedRow(row);
              return;
            }
          }
        }
      }
    }
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeDGCListener(this);
    }

    super.tearDown();

    endObjectCountSeries.clear();

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      clusterListener = null;
      table = null;
      popupMenu = null;
      gcAction = null;
      endObjectCountSeries = null;
      endObjectCountPlot = null;
      currentDGCMarker = null;
      exportAction = null;
    }
  }
}
