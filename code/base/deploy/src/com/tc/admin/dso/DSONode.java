/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.jfree.chart.JFreeChart;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.PollerNode;
import com.tc.admin.common.RatePanel;
import com.tc.admin.common.StatisticPanel;

import javax.management.ObjectName;

public class DSONode extends ComponentNode {
  public DSONode(ConnectionContext cc) {
    super();

    AdminClientContext acc = AdminClient.getContext();

    setLabel(acc.getMessage("dso"));
    setComponent(new DSOPanel(cc));

    int i = 0;
    insert(new RootsNode(cc), i++);
    insert(new ClassesNode(cc), i++);
    // insert(new LocksNode(cc), i++);
    insert(new ClientsNode(cc), i++);

    PollerNode node;
    ObjectName bean = DSOHelper.getHelper().getDSOMBean(cc);
    String statName;
    String header;
    StatisticPanel panel;
    String xLabel = null;
    String yLabel = acc.getMessage("dso.cache.rate.range.label");

    CacheActivityPanel cacheActivityPanel = new CacheActivityPanel(cc, bean);
    header = acc.getMessage("dso.cache.activity");
    node = new PollerNode(header, cacheActivityPanel);

    cacheActivityPanel.setNode(node);
    insert(node, i++);

    statName = "TransactionRate";
    header = acc.getMessage("dso.transaction.rate");
    yLabel = acc.getMessage("dso.transaction.rate.range.label");
    panel = new RatePanel(cc, bean, statName, header, xLabel, yLabel) {
      public JFreeChart createChart() {
        return DemoChartFactory.getXYBarChart("", "", "", m_timeSeries);
      }
    };
    node = new PollerNode(header, panel);

    panel.setNode(node);
    insert(node, i++);

    statName = "CacheMissRate";
    header = acc.getMessage("dso.cache.miss.rate");
    yLabel = acc.getMessage("dso.cache.miss.rate.label");
    panel = new RatePanel(cc, bean, statName, header, xLabel, yLabel);
    node = new PollerNode(header, panel);

    panel.setNode(node);
    insert(node, i++);

    insert(new GCStatsNode(cc), i++);

    StatsPanel statsPanel = new StatsPanel(cc, bean);
    node = new PollerNode(acc.getMessage("dso.all.statistics"), statsPanel);

    statsPanel.setNode(node);
    insert(node, i++);
  }
}
