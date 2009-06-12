/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.properties.TCPropertiesConsts;
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

  public final static TCLogger     LOGGER                 = TCLogging.getLogger(StatisticRetrievalAction.class);

  public static final String       ACTION_NAME            = "message monitor";

  private final MessageMonitorImpl monitor;

  /**
   * Element name for incoming count
   */
  public static final String       ELEMENT_INCOMING_COUNT = "incoming count";

  /**
   * Element name for incoming data
   */
  public static final String       ELEMENT_INCOMING_DATA  = "incoming data";

  /**
   * Element name for outgoing count
   */
  public static final String       ELEMENT_OUTGOING_COUNT = "outgoing count";

  /**
   * Element name for outgoing data
   */
  public static final String       ELEMENT_OUTGOING_DATA  = "outgoing data";

  /**
   * Delimiter String that is between the mesage type and the data in an element
   */
  public static final String       ELEMENT_NAME_DELIMITER = ":";

  public SRAMessages(final MessageMonitor monitor) {
    if (monitor instanceof MessageMonitorImpl) {
      this.monitor = (MessageMonitorImpl) monitor;
    } else {
      this.monitor = null;
      LOGGER.info("\"" + ACTION_NAME + "\" statistic is not enabled. Please enable the property \""
                  + TCPropertiesConsts.TCM_MONITOR_DELAY + "\" to collect this statistics.");
    }
  }

  public StatisticData[] retrieveStatisticData() {
    if (monitor == null) return EMPTY_STATISTIC_DATA;
    List data = new ArrayList();
    synchronized (monitor) {
      for (Iterator it = monitor.getCounters().values().iterator(); it.hasNext();) {
        MessageMonitorImpl.MessageCounter counter = (MessageMonitorImpl.MessageCounter)it.next();
        data.add(new StatisticData(ACTION_NAME, counter.getName() + ELEMENT_NAME_DELIMITER + ELEMENT_INCOMING_COUNT, new Long(counter
          .getIncomingCount().get())));
        data.add(new StatisticData(ACTION_NAME, counter.getName() + ELEMENT_NAME_DELIMITER + ELEMENT_INCOMING_DATA, new Long(counter
          .getIncomingData().get())));
        data.add(new StatisticData(ACTION_NAME, counter.getName() + ELEMENT_NAME_DELIMITER + ELEMENT_OUTGOING_COUNT, new Long(counter
          .getOutgoingCount().get())));
        data.add(new StatisticData(ACTION_NAME, counter.getName() + ELEMENT_NAME_DELIMITER + ELEMENT_OUTGOING_DATA, new Long(counter
          .getOutgoingData().get())));
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
