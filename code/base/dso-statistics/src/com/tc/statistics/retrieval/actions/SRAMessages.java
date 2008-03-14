/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This statistic gives details about messages in this node.
 * <p/>
 * The statistic contains {@link StatisticData} with the following elements
 * <ul>
 * <li>incoming count</li>
 * <li>incoming data</li>
 * <li>outgoing count</li>
 * <li>outgoing data</li>
 * </ul>
 * Each element is a combination of the message type and the above names separated by a colon (:)
 * <p/>
 * The {@link com.tc.statistics.retrieval.StatisticsRetriever} samples this data at the global frequency.
 * The property {@code tcm.monitor.enabled} needs to be {@code true} to collect this statistic.
 */
public class SRAMessages implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "message monitor";
  private static final String ENABLED_PROP = "tcm.monitor.enabled";

  private static TCLogger logger = TCLogging.getLogger(SRAMessages.class);

  private final MessageMonitorImpl monitor;

  public SRAMessages(final MessageMonitor monitor) {
    if (monitor instanceof MessageMonitorImpl) {
      this.monitor = (MessageMonitorImpl)monitor;
    } else {
      this.monitor = null;
      logger.info("\"" + ACTION_NAME + "\" statistic is not enabled. Please enable the property \"" + ENABLED_PROP + "\" to collect this statistics.");
    }
  }

  public StatisticData[] retrieveStatisticData() {
    if (monitor == null) return EMPTY_STATISTIC_DATA;
    List data = new ArrayList();
    synchronized (monitor) {
      for (Iterator it = monitor.getCounters().values().iterator(); it.hasNext();) {
        MessageMonitorImpl.MessageCounter counter = (MessageMonitorImpl.MessageCounter)it.next();
        data.add(new StatisticData(ACTION_NAME, counter.getName() + ":incoming count", new Long(counter.getIncomingCount().get())));
        data.add(new StatisticData(ACTION_NAME, counter.getName() + ":incoming data", new Long(counter.getIncomingData().get())));
        data.add(new StatisticData(ACTION_NAME, counter.getName() + ":outgoing count", new Long(counter.getOutgoingCount().get())));
        data.add(new StatisticData(ACTION_NAME, counter.getName() + ":outgoing data", new Long(counter.getOutgoingData().get())));
      }
    }
    return (StatisticData[])data.toArray(new StatisticData[data.size()]);
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}
