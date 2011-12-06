/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import static com.tc.admin.model.IClusterNode.POLLED_ATTR_CPU_LOAD;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_CPU_USAGE;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_MAX_MEMORY;
import static com.tc.admin.model.IClusterNode.POLLED_ATTR_USED_MEMORY;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.FixedTimeSeriesCollection;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClusterNode;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.statistics.StatisticData;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public abstract class ClusterNodeRuntimeStatsPanel extends BaseRuntimeStatsPanel {
  protected final IClusterNode        clusterNode;

  protected TimeSeries                onHeapMaxSeries;
  protected StatusView                onHeapMaxLabel;
  protected TimeSeries                onHeapUsedSeries;
  protected StatusView                onHeapUsedLabel;
  protected ChartPanel                cpuPanel;
  protected CpuPanelMode              cpuPanelMode             = CpuPanelMode.LOAD;
  protected FixedTimeSeriesCollection cpuUsageDataset;
  protected TimeSeries                cpuLoadSeries;

  private static final String         ONHEAP_USED_LABEL_FORMAT = "{0} OnHeap Used";
  private static final String         ONHEAP_MAX_LABEL_FORMAT  = "{0} OnHeap Max";

  private CpuModeChangeAction         cpuModeChangeAction;

  private static enum CpuPanelMode {
    LOAD {
      @Override
      public CpuPanelMode toggle() {
        return USAGE;
      }

      @Override
      public String toggleMessage() {
        return "Switch to Usage";
      }
    },
    USAGE {
      @Override
      public CpuPanelMode toggle() {
        return LOAD;
      }

      @Override
      public String toggleMessage() {
        return "Switch to Load";
      }
    };

    public abstract CpuPanelMode toggle();

    public abstract String toggleMessage();
  }

  public ClusterNodeRuntimeStatsPanel(ApplicationContext appContext, IClusterNode clusterNode) {
    super(appContext);
    this.clusterNode = clusterNode;
    setName(clusterNode.toString());
  }

  @Override
  public void startMonitoringRuntimeStats() {
    setupCpuPanel();
    addPolledAttributeListener();
    super.startMonitoringRuntimeStats();
  }

  protected abstract void addPolledAttributeListener();

  protected void removePolledAttributeListener() {
    switch (cpuPanelMode) {
      case LOAD:
        clusterNode.removePolledAttributeListener(POLLED_ATTR_CPU_LOAD, this);
        break;
      case USAGE:
        clusterNode.removePolledAttributeListener(POLLED_ATTR_CPU_USAGE, this);
        break;
    }
  }

  @Override
  public void stopMonitoringRuntimeStats() {
    removePolledAttributeListener();
    super.stopMonitoringRuntimeStats();
  }

  @Override
  protected void setMaximumItemAge(int maxItemAge) {
    super.setMaximumItemAge(maxItemAge);
    if (cpuUsageDataset != null) {
      cpuUsageDataset.setMaximumItemAge(maxItemAge);
    }
  }

  protected void setupOnHeapPanel(XContainer parent) {
    onHeapMaxSeries = createTimeSeries(appContext.getString("onheap.usage.max"));
    onHeapUsedSeries = createTimeSeries(appContext.getString("onheap.usage.used"));
    JFreeChart chart = createChart(new TimeSeries[] { onHeapMaxSeries, onHeapUsedSeries }, false);
    XYPlot plot = (XYPlot) chart.getPlot();
    XYItemRenderer renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, Color.red);
    renderer.setSeriesPaint(1, Color.blue);
    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setStandardTickUnits(DemoChartFactory.DEFAULT_MEMORY_TICKS);
    numberAxis.setAutoRangeIncludesZero(true);
    ChartPanel memoryPanel = createChartPanel(chart);
    parent.add(memoryPanel);
    memoryPanel.setPreferredSize(fDefaultGraphSize);
    memoryPanel.setBorder(new TitledBorder(appContext.getString("stats.heap.usage")));
    memoryPanel.setToolTipText(appContext.getString("stats.heap.usage.tip"));
    memoryPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(onHeapMaxLabel = createStatusLabel(Color.red));
    labelHolder.add(onHeapUsedLabel = createStatusLabel(Color.blue));
    labelHolder.setOpaque(false);
    memoryPanel.add(labelHolder, gbc);
  }

  private void setupSigarMissing() {
    cpuPanel.removeAll();
    cpuPanel.setLayout(new BorderLayout());
    XLabel label = createOverlayLabel();
    label.setText("Sigar is disabled or missing.");
    label
        .setToolTipText("<html>To re-enable Sigar, add the following to your tc-config.xml<br>then restart your cluster:<br><br>"
                        + "&lt;tc-properties&gt;<br>"
                        + "&nbsp;&nbsp;&nbsp;&lt;property name=\"sigar.enabled\" value=\"true\"/&gt;<br>"
                        + "&lt;/tc-properties&gt;<p>&nbsp;</html>");
    cpuPanel.add(label);
  }

  protected void setupCpuLoadSeries(TimeSeries cpuLoadSeries) {
    this.cpuLoadSeries = cpuLoadSeries;
    JFreeChart cpuChart = cpuPanel.getChart();
    if (cpuChart == null) {
      cpuChart = createChart(cpuLoadSeries, false);
      XYPlot plot = (XYPlot) cpuChart.getPlot();
      NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
      numberAxis.setRange(0.0, 1.0);
      cpuPanel.setChart(cpuChart);
      cpuPanel.setDomainZoomable(false);
      cpuPanel.setRangeZoomable(false);
    } else {
      if (cpuUsageDataset != null) {
        cpuUsageDataset.clear();
        cpuUsageDataset = null;
      }
      XYPlot plot = (XYPlot) cpuChart.getPlot();
      plot.setDataset(DemoChartFactory.createTimeSeriesDataset(cpuLoadSeries));
    }
    cpuPanel.setToolTipText(appContext.getString("stats.cpu.load.tip"));

    if (clusterNode.getCpuStatNames().length == 0) {
      setupSigarMissing();
    } else {
      clusterNode.removePolledAttributeListener(POLLED_ATTR_CPU_USAGE, this);
      clusterNode.addPolledAttributeListener(POLLED_ATTR_CPU_LOAD, this);
    }
    cpuPanel.setBorder(BorderFactory.createTitledBorder(appContext.getString("stats.cpu.load")));
    updateFixedRangeAxisSpace(chartsPanel);
  }

  protected void setupCpuUsageSeries(FixedTimeSeriesCollection cpuDataset) {
    this.cpuUsageDataset = cpuDataset;
    JFreeChart cpuChart = cpuPanel.getChart();
    if (cpuChart == null) {
      cpuChart = createChart(cpuUsageDataset, false);
      XYPlot plot = (XYPlot) cpuChart.getPlot();
      NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
      numberAxis.setRange(0.0, 1.0);
      cpuPanel.setChart(cpuChart);
      cpuPanel.setDomainZoomable(false);
      cpuPanel.setRangeZoomable(false);
    } else {
      if (cpuLoadSeries != null) {
        cpuLoadSeries.clear();
        cpuLoadSeries = null;
      }
      XYPlot plot = (XYPlot) cpuChart.getPlot();
      plot.setDataset(cpuUsageDataset);
    }
    cpuPanel.setToolTipText(appContext.getString("stats.cpu.usage.tip"));

    if (clusterNode.getCpuStatNames().length == 0) {
      setupSigarMissing();
    } else {
      clusterNode.removePolledAttributeListener(POLLED_ATTR_CPU_LOAD, this);
      clusterNode.addPolledAttributeListener(POLLED_ATTR_CPU_USAGE, this);
    }
    cpuPanel.setBorder(BorderFactory.createTitledBorder(appContext.getString("stats.cpu.usage")));
    updateFixedRangeAxisSpace(chartsPanel);
  }

  protected class CpuLoadPanelWorker extends BasicWorker<TimeSeries> {
    public CpuLoadPanelWorker() {
      super(new Callable<TimeSeries>() {
        public TimeSeries call() throws Exception {
          return createCpuLoadSeries(clusterNode);
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(e);
        setupInstructions();
      } else {
        setupCpuLoadSeries(getResult());
      }
    }
  }

  protected class CpuUsagePanelWorker extends BasicWorker<FixedTimeSeriesCollection> {
    public CpuUsagePanelWorker() {
      super(new Callable<FixedTimeSeriesCollection>() {
        public FixedTimeSeriesCollection call() throws Exception {
          return createCpuUsageDataset(clusterNode);
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(e);
        setupInstructions();
      } else {
        setupCpuUsageSeries(getResult());
      }
    }
  }

  private void setupInstructions() {
    setupHypericInstructions(cpuPanel);
  }

  private void switchCpuPanelMode() {
    setCpuPanelMode(cpuPanelMode.toggle());
  }

  private void setCpuPanelMode(CpuPanelMode cpuPanelMode) {
    this.cpuPanelMode = cpuPanelMode;
    setupCpuPanel();
  }

  private void setupCpuPanel() {
    switch (cpuPanelMode) {
      case LOAD:
        appContext.execute(new CpuLoadPanelWorker());
        break;
      case USAGE:
        appContext.execute(new CpuUsagePanelWorker());
        break;
    }

    cpuModeChangeAction.setName(cpuPanelMode.toggleMessage());
  }

  public class CpuModeChangeAction extends XAbstractAction {
    CpuModeChangeAction() {
      super(cpuPanelMode.toggleMessage());
    }

    public void actionPerformed(ActionEvent ae) {
      switchCpuPanelMode();
    }
  }

  protected void setupCpuPanel(XContainer parent) {
    parent.add(cpuPanel = createChartPanel(null));
    JPopupMenu popup = cpuPanel.getPopupMenu();
    popup.add(cpuModeChangeAction = new CpuModeChangeAction());
    cpuPanel.setPreferredSize(fDefaultGraphSize);
    cpuPanel.setBorder(BorderFactory.createTitledBorder("CPU"));
  }

  protected void handleSysStats(final PolledAttributesResult result) {
    final Number memoryMax = (Number) result.getPolledAttribute(clusterNode, POLLED_ATTR_MAX_MEMORY);
    final Number memoryUsed = (Number) result.getPolledAttribute(clusterNode, POLLED_ATTR_USED_MEMORY);
    final StatisticData cpuLoadData = (StatisticData) result.getPolledAttribute(clusterNode, POLLED_ATTR_CPU_LOAD);
    final StatisticData[] cpuUsageData = (StatisticData[]) result
        .getPolledAttribute(clusterNode, POLLED_ATTR_CPU_USAGE);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (memoryMax != null) {
          onHeapMaxLabel.setText(MessageFormat.format(ONHEAP_MAX_LABEL_FORMAT, convertBytes(memoryMax.longValue())));
        }
        if (memoryUsed != null) {
          onHeapUsedLabel.setText(MessageFormat.format(ONHEAP_USED_LABEL_FORMAT, convertBytes(memoryUsed.longValue())));
        }
        updateSeries(onHeapMaxSeries, memoryMax);
        updateSeries(onHeapUsedSeries, memoryUsed);

        if (cpuLoadSeries != null && cpuLoadData != null) {
          handleCpuUsage(cpuLoadSeries, cpuLoadData);
        }
        if (cpuUsageDataset != null && cpuUsageData != null) {
          handleCpuUsage(cpuUsageDataset, cpuUsageData);
        }
      }
    });
  }

  protected void clearAllTimeSeries() {
    if (onHeapMaxSeries != null) {
      onHeapMaxSeries.clear();
    }
    if (onHeapUsedSeries != null) {
      onHeapUsedSeries.clear();
    }
    if (cpuLoadSeries != null) {
      cpuLoadSeries.clear();
    }
    if (cpuUsageDataset != null) {
      cpuUsageDataset.clear();
    }
  }

  @Override
  public void tearDown() {
    stopMonitoringRuntimeStats();
    clearAllTimeSeries();
    super.tearDown();
  }
}
