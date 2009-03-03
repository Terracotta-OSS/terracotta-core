/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.RolloverButton;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XScrollPane;
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
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
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
  private final TimeSeries   dgcTimeSeries;

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

    add(topPanel, BorderLayout.NORTH);

    table = new GCStatsTable();
    table.setModel(new GCStatsTableModel(appContext));
    add(new XScrollPane(table), BorderLayout.CENTER);

    popupMenu = new JPopupMenu("DGC");
    popupMenu.add(gcAction);
    table.add(popupMenu);
    table.addMouseListener(new TableMouseHandler());

    dgcTimeSeries = new TimeSeries("DGCTimes", Second.class);
    JFreeChart chart = DemoChartFactory.getXYBarChart("", "", "", new TimeSeries[] { dgcTimeSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
    renderer.setDrawBarOutline(false);
    renderer.setShadowVisible(false);
    DateAxis axis = (DateAxis) plot.getDomainAxis();
    axis.setFixedAutoRange(0.0);
    ChartPanel chartPanel = BaseRuntimeStatsPanel.createChartPanel(chart);
    chartPanel.setBorder(BorderFactory.createTitledBorder("Total Elapsed Collection Time (ms.)"));
    chartPanel.setMinimumSize(new Dimension(0, 0));
    chartPanel.setPreferredSize(new Dimension(0, 200));
    add(chartPanel, BorderLayout.SOUTH);

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
              return appContext.getString("dso.gcstats.overview.enabled");
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

  private class HelpButtonHandler implements ActionListener {
    private String getKitID() {
      String kitID = ProductInfo.getInstance().kitID();
      if (ProductInfo.UNKNOWN_VALUE.equals(kitID)) {
        kitID = System.getProperty("com.tc.kitID", "42.0");
      }
      return kitID;
    }

    public void actionPerformed(ActionEvent e) {
      String kitID = getKitID();
      String loc = appContext.format("console.guide.url", kitID) + "#AdminConsoleGuide-DistributedGarbageCollection";
      BrowserLauncher.openURL(loc);
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
            dgcTimeSeries.addOrUpdate(new FixedMillisecond(stat.getStartTime()), stat.getElapsedTime());
          }
        }
      }
      gcAction.setEnabled(true);
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
      ((GCStatsTableModel) table.getModel()).addGCStats(gcStats);
      if (gcAction.isEnabled()) {
        dgcTimeSeries.addOrUpdate(new FixedMillisecond(gcStats.getStartTime()), gcStats.getElapsedTime());
      }
    }
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

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeDGCListener(this);
    }

    super.tearDown();

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      clusterListener = null;
      table = null;
      popupMenu = null;
      gcAction = null;
    }
  }
}
