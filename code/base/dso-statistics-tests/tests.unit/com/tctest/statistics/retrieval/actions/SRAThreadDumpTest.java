/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.actions.SRAThreadDump;
import com.tc.util.Assert;

import junit.framework.TestCase;

public class SRAThreadDumpTest extends TestCase {

  public void testRetrieval() {
    SRAThreadDump sraThreadDump = new SRAThreadDump();
    StatisticData[] statisticData = sraThreadDump.retrieveStatisticData();

    Assert.assertEquals(statisticData.length, 1);
    Assert.assertEquals(statisticData[0].getName(), SRAThreadDump.ACTION_NAME);
    Assert.assertNull(statisticData[0].getAgentIp());
    Assert.assertNull(statisticData[0].getAgentDifferentiator());
    Assert.eval(statisticData[0].getData() instanceof String);
    Assert.eval(((String)statisticData[0].getData()).length() > 0);
  }
}
