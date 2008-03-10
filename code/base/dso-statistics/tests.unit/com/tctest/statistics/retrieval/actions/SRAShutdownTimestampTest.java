/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;

import java.util.Date;

import junit.framework.TestCase;

public class SRAShutdownTimestampTest extends TestCase {
  public void testRetrieval() throws Exception {
    StatisticRetrievalAction action = new SRAShutdownTimestamp();
    Date before = new Date();
    StatisticData data = action.retrieveStatisticData()[0];
    Date after = new Date();
    assertEquals(SRAShutdownTimestamp.ACTION_NAME, data.getName());
    assertNull(data.getAgentIp());
    assertNull(data.getAgentDifferentiator());
    assertTrue(before.compareTo((Date)data.getData()) <= 0);
    assertTrue(after.compareTo((Date)data.getData()) >= 0);

    assertNull(data.getElement());
    assertNull(data.getMoment());
  }
}