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
import com.tc.stats.statistics.Statistic;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// TODO: This and MultiStatisticPanel should be merged.  Maybe this guy should be a
// special case of MultiStatisticPanel.

public class StatisticPanel extends XContainer implements Poller {
  protected ConnectionContext m_cc;
  protected JFreeChart        m_chart;
  protected TimeSeries        m_timeSeries;
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
  protected String            m_beanName;
  protected String            m_statisticName;
  protected Statistic         m_statistic;
  protected boolean           m_shouldAutoStart;

  private static final int DEFAULT_POLL_PERIOD   = 1000;
  private static final int DEFAULT_HISTORY_COUNT = 30;

  private static final String SHOW_ICON_URI= "/com/tc/admin/icons/view_menu.gif";
  private static final String HIDE_ICON_URI= "/com/tc/admin/icons/hide_menu.gif";

  public StatisticPanel(ConnectionContext cc) {
    super();

    m_shouldAutoStart = true;

    AdminClientContext cntx = AdminClient.getContext();

    load((ContainerResource)cntx.topRes.getComponent("StatisticPanel"));

    Container chartHolder = (Container)findComponent("ChartHolder");
    chartHolder.setLayout(new BorderLayout());

    m_timeSeries = new TimeSeries("Rate", Second.class);
    m_timeSeries.setMaximumItemCount(DEFAULT_HISTORY_COUNT);

    m_startButton = (Button)findComponent("StartButton");
    m_startButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        start();
      }
    });

    m_stopButton  = (Button)findComponent("StopButton");
    m_stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        stop();
      }
    });

    m_clearButton = (Button)findComponent("ClearButton");
    m_clearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        clear();
      }
    });

    m_periodSpinner = (Spinner)findComponent("PeriodSpinner");
    m_periodSpinner.setModel(
      new SpinnerNumberModel(new Integer(1),
                             new Integer(1),
                             null,
                             new Integer(1)));
    m_periodSpinner.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        SpinnerNumberModel model  = (SpinnerNumberModel)m_periodSpinner.getModel();
        Integer            i      = (Integer)model.getNumber();

        setPollPeriod(i.intValue() * 1000);
      }
    });

    m_historySpinner = (Spinner)findComponent("HistorySpinner");
    m_historySpinner.setModel(
      new SpinnerNumberModel(new Integer(DEFAULT_HISTORY_COUNT),
                             new Integer(5),
                             null,
                             new Integer(10)));
    m_historySpinner.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        SpinnerNumberModel model  = (SpinnerNumberModel)m_historySpinner.getModel();
        Integer            i      = (Integer)model.getNumber();

        m_timeSeries.setMaximumItemCount(i.intValue());
        ((XYPlot)m_chart.getPlot()).getDomainAxis().setFixedAutoRange(i.intValue()*1000);
      }
    });

    m_chart = getChart();
    chartHolder.add(new ChartPanel(m_chart, false));

    m_controlsButton = (Button)findComponent("ControlsButton");

    m_showIcon = new ImageIcon(getClass().getResource(SHOW_ICON_URI));
    m_hideIcon = new ImageIcon(getClass().getResource(HIDE_ICON_URI));
    m_controlsButton.setIcon(m_showIcon);
    m_controlsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if(m_splitter.isRightShowing()) {
          m_splitter.hideRight();
          m_controlsButton.setIcon(m_showIcon);
        }
        else {
          m_splitter.showRight();
          m_controlsButton.setIcon(m_hideIcon);
        }
      }
    });

    m_splitter = (SplitPane)findComponent("Splitter");
    m_splitter.hideRight();

    m_cc         = cc;
    m_pollPeriod = DEFAULT_POLL_PERIOD;
    m_timer      = new Timer(m_pollPeriod, new TaskPerformer());
  }

  public void setProvider(ObjectName bean, String statisticName) {
    m_bean          = bean;
    m_statisticName = statisticName;
  }

  public void setProvider(String beanName, String statisticName) {
    m_beanName      = beanName;
    m_statisticName = statisticName;
  }

  public void setBeanName(String beanName) {
    m_beanName = beanName;
  }

  public String getBeanName() {
    return m_beanName;
  }

  public void setBean(ObjectName bean) {
    m_bean = bean;
  }

  public ObjectName getBean() {
    return m_bean;
  }

  public void setStatisticName(String statisticName) {
    m_statisticName = statisticName;
  }

  public String getStatisticName() {
    return m_statisticName;
  }

  public void setPollPeriod(int millis) {
    m_timer.setDelay(m_pollPeriod = Math.abs(millis));
  }

  public int getPollPeriod() {
    return m_pollPeriod;
  }

  public void setStatistic(Statistic statistic) {
    m_statistic = statistic;
  }

  public Statistic getStatistic() {
    return m_statistic;
  }

  class TaskPerformer implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      if(m_cc.isConnected()) {
        try {
          ObjectName bean = getBean();

          if(bean == null) {
            bean = m_cc.queryName(getBeanName());
          }

          if(m_cc.isRegistered(bean)) {
            String    name =  getStatisticName();
            Statistic stat = (Statistic)m_cc.getAttribute(bean, name);

            setStatistic(stat);
          }
        } catch(Exception e) {
          stop();
        }
      }
      else {
        stop();
      }
    }
  }

  public JFreeChart createChart() {
    return DemoChartFactory.getXYLineChart("", "", "", m_timeSeries);
  }

  public JFreeChart getChart() {
    if(m_chart == null) {
      m_chart = createChart();
    }

    return m_chart;
  }

  public TimeSeries getTimeSeries() {
    return m_timeSeries;
  }

  public void setSeriesName(String name) {
    m_timeSeries.setDescription(name);
    m_chart.setTitle(name);
  }

  public XYPlot getXYPlot() {
    return getChart().getXYPlot();
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
    return m_timer != null && m_timer.isRunning();
  }

  public void start() {
    if(!isRunning()) {
      m_timer.start();
      m_startButton.setEnabled(false);
      m_stopButton.setEnabled(true);
    }
  }

  public void stop() {
    if(isRunning()) {
      m_timer.stop();
      m_startButton.setEnabled(true);
      m_stopButton.setEnabled(false);
    }
  }

  public void clear() {
    m_timeSeries.clear();
  }

  public void addNotify() {
    super.addNotify();

    if(m_shouldAutoStart && !isRunning()) {
      start();
      m_shouldAutoStart = false;
    }
  }

  public void tearDown() {
    if(isRunning())
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
    m_beanName = null;
    m_statisticName = null;
    m_statistic = null;
  }
}
