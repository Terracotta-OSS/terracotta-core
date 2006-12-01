/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.Button;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Spinner;
import org.dijon.SplitPane;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// TODO: This and StatisticPanel should be merged. Maybe StatisticPanel should be a
// special case of this guy.

public class MultiStatisticPanel extends XContainer implements Poller {
  protected ConnectionContext m_cc;
  protected JFreeChart        m_chart;
  protected TimeSeries[]      m_timeSeries;
  protected SplitPane         m_splitter;
  protected Button            m_controlsButton;
  protected Icon              m_showIcon;
  protected Icon              m_hideIcon;
  protected Button            m_startButton;
  protected Button            m_stopButton;
  protected Button            m_clearButton;
  protected Spinner           m_periodSpinner;
  protected Spinner           m_historySpinner;
  protected Timer             m_timer;
  protected int               m_pollPeriod;
  protected ObjectName        m_bean;
  protected String[]          m_statisticNames;
  protected Statistic[]       m_statistics;
  protected boolean           m_shouldAutoStart;

  private static final int    DEFAULT_POLL_PERIOD   = 1000;
  private static final int    DEFAULT_HISTORY_COUNT = 30;

  private static final String SHOW_ICON_URI         = "/com/tc/admin/icons/view_menu.gif";
  private static final String HIDE_ICON_URI         = "/com/tc/admin/icons/hide_menu.gif";

  public MultiStatisticPanel(ConnectionContext cc, ObjectName bean, String[] stats, String[] labels, String header,
                             String xAxis, String yAxis) {
    super();

    m_shouldAutoStart = true;

    AdminClientContext cntx = AdminClient.getContext();

    load((ContainerResource) cntx.topRes.getComponent("StatisticPanel"));

    Container chartHolder = (Container) findComponent("ChartHolder");
    chartHolder.setLayout(new BorderLayout());

    m_bean = bean;
    m_statisticNames = stats;
    m_timeSeries = new TimeSeries[stats.length];

    for (int i = 0; i < stats.length; i++) {
      m_timeSeries[i] = new TimeSeries(labels[i], Second.class);
      m_timeSeries[i].setMaximumItemCount(DEFAULT_HISTORY_COUNT);
    }

    m_startButton = (Button) findComponent("StartButton");
    m_startButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        start();
      }
    });

    m_stopButton = (Button) findComponent("StopButton");
    m_stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        stop();
      }
    });

    m_clearButton = (Button) findComponent("ClearButton");
    m_clearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        clear();
      }
    });

    m_periodSpinner = (Spinner) findComponent("PeriodSpinner");
    m_periodSpinner.setModel(new SpinnerNumberModel(new Integer(1), new Integer(1), null, new Integer(1)));
    m_periodSpinner.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        SpinnerNumberModel model = (SpinnerNumberModel) m_periodSpinner.getModel();
        Integer i = (Integer) model.getNumber();

        setPollPeriod(i.intValue() * 1000);
      }
    });

    m_historySpinner = (Spinner) findComponent("HistorySpinner");
    m_historySpinner.setModel(new SpinnerNumberModel(new Integer(DEFAULT_HISTORY_COUNT), new Integer(5), null,
                                                     new Integer(10)));
    m_historySpinner.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        SpinnerNumberModel model = (SpinnerNumberModel) m_historySpinner.getModel();
        Integer i = (Integer) model.getNumber();

        for (int j = 0; j < m_timeSeries.length; j++) {
          m_timeSeries[j].setMaximumItemCount(i.intValue());
        }
        ((XYPlot) m_chart.getPlot()).getDomainAxis().setFixedAutoRange(i.intValue() * 1000);
      }
    });

    m_chart = getChart(header, xAxis, yAxis);
    chartHolder.add(new ChartPanel(m_chart, false));

    m_controlsButton = (Button) findComponent("ControlsButton");

    m_showIcon = new ImageIcon(getClass().getResource(SHOW_ICON_URI));
    m_hideIcon = new ImageIcon(getClass().getResource(HIDE_ICON_URI));
    m_controlsButton.setIcon(m_showIcon);
    m_controlsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if (m_splitter.isRightShowing()) {
          m_splitter.hideRight();
          m_controlsButton.setIcon(m_showIcon);
        } else {
          m_splitter.showRight();
          m_controlsButton.setIcon(m_hideIcon);
        }
      }
    });

    m_splitter = (SplitPane) findComponent("Splitter");
    m_splitter.hideRight();

    m_cc = cc;
    m_pollPeriod = DEFAULT_POLL_PERIOD;
    m_timer = new Timer(m_pollPeriod, new TaskPerformer());
  }

  public void setPollPeriod(int millis) {
    m_timer.setDelay(m_pollPeriod = Math.abs(millis));
  }

  public int getPollPeriod() {
    return m_pollPeriod;
  }

  private Date m_date = new Date();

  public void setStatistics(Statistic[] statistics) {
    m_statistics = statistics;

    for (int i = 0; i < m_timeSeries.length; i++) {
      CountStatistic countStat = (CountStatistic) statistics[i];
      long ts = countStat.getLastSampleTime();
      long count = countStat.getCount();

      m_date.setTime(ts);

      m_timeSeries[i].addOrUpdate(new Second(m_date), count);
    }
  }

  public Statistic[] getStatistics() {
    return m_statistics;
  }

  class TaskPerformer implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      if (m_cc.isConnected()) {
        try {
          String op = "getStatistics";
          Object[] args = { m_statisticNames };
          String[] types = { "[Ljava.lang.String;" };

          if (m_cc.isRegistered(m_bean)) {
            setStatistics((Statistic[]) m_cc.invoke(m_bean, op, args, types));
          }
        } catch (Exception e) {
          stop();
        }
      } else {
        stop();
      }
    }
  }

  public JFreeChart createChart() {
    return DemoChartFactory.getXYBarChart("", "", "", m_timeSeries);
  }

  public JFreeChart getChart(String header, String xAxis, String yAxis) {
    if (m_chart == null) {
      m_chart = createChart();
      m_chart.setTitle(header);
      setTimeAxisLabel(xAxis);
      setValueAxisLabel(yAxis);
    }

    return m_chart;
  }

  public XYPlot getXYPlot() {
    return m_chart.getXYPlot();
  }

  public ValueAxis getDomainAxis() {
    return getXYPlot().getDomainAxis();
  }

  public ValueAxis getRangeAxis() {
    return getXYPlot().getRangeAxis();
  }

  public void setTimeAxisLabel(String label) {
    getDomainAxis().setLabel(label);
  }

  public void setValueAxisLabel(String label) {
    getRangeAxis().setLabel(label);
  }

  public boolean isRunning() {
    return m_timer.isRunning();
  }

  public void start() {
    if (!isRunning()) {
      m_timer.start();
      m_startButton.setEnabled(false);
      m_stopButton.setEnabled(true);
    }
  }

  public void stop() {
    if (isRunning()) {
      m_timer.stop();
      m_startButton.setEnabled(true);
      m_stopButton.setEnabled(false);
    }
  }

  public void clear() {
    for (int i = 0; i < m_timeSeries.length; i++) {
      m_timeSeries[i].clear();
    }
  }

  public void addNotify() {
    super.addNotify();

    if (m_shouldAutoStart) {
      start();
      m_shouldAutoStart = false;
    }
  }

  public void tearDown() {
    stop();

    super.tearDown();

    m_cc = null;
    m_chart = null;
    m_timeSeries = null;
    m_startButton = null;
    m_stopButton = null;
    m_clearButton = null;
    m_periodSpinner = null;
    m_historySpinner = null;
    m_timer = null;
    m_bean = null;
    m_statisticNames = null;
    m_statistics = null;
  }
}
