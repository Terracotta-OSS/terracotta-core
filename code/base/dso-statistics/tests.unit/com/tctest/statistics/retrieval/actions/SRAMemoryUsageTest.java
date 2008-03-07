/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;

import java.lang.reflect.Array;
import java.util.Date;

import junit.framework.TestCase;

public class SRAMemoryUsageTest extends TestCase {
  public void testRetrieval() throws Exception {
    StatisticRetrievalAction action = new SRAMemoryUsage();

    Date before1 = new Date();
    StatisticData[] data1 = action.retrieveStatisticData();
    Date after1 = new Date();

    long[] values1 = assertMemUsageData(data1, before1, after1);
    long free1 = values1[0];
    long used1 = values1[1];
    long max1 = values1[2];

    int memsize = 1024 * 1024 * 10;
    byte[] mem = (byte[])Array.newInstance(byte.class, memsize);

    Date before2 = new Date();
    StatisticData[] data2 = action.retrieveStatisticData();
    Date after2 = new Date();

    long[] values2 = assertMemUsageData(data2, before2, after2);
    long free2 = values2[0];
    long used2 = values2[1];
    long max2 = values2[2];

    long difference = free1 - free2;
    assertTrue(free1 > free2);
    assertTrue(used2 > used1);
    assertTrue(difference <= used2 - used1);
    assertEquals(max1, max2);

    mem = null;

    System.gc();
    Thread.sleep(500);
    System.gc();
    Thread.sleep(500);

    Date before3 = new Date();
    StatisticData[] data3 = action.retrieveStatisticData();
    Date after3 = new Date();

    long[] values3 = assertMemUsageData(data3, before3, after3);
    long free3 = values3[0];
    long used3 = values3[1];
    long max3 = values3[2];

    assertTrue(free3 > free2);
    assertTrue(used2 > used3);
    assertTrue(free3 - free2 >= difference);
    assertTrue(used3 - used2 <= difference);
    assertEquals(max3, max2);
  }

  private long[] assertMemUsageData(StatisticData[] data, Date before, Date after) throws Exception {
    long[] values = new long[3];
    for (int i = 0; i < data.length; i++) {
      assertTrue(data[i].getName().startsWith(SRAMemoryUsage.ACTION_NAME));
      assertNull(data[i].getAgentIp());
      assertNull(data[i].getAgentDifferentiator());
      assertTrue(before.compareTo(data[i].getMoment()) <= 0);
      assertTrue(after.compareTo(data[i].getMoment()) >= 0);
      switch (i) {
        case 0:
          assertEquals(SRAMemoryUsage.DATA_NAME_FREE, data[i].getName());
          break;
        case 1:
          assertEquals(SRAMemoryUsage.DATA_NAME_USED, data[i].getName());
          break;
        case 2:
          assertEquals(SRAMemoryUsage.DATA_NAME_MAX, data[i].getName());
          break;
        default:
          fail();
          break;
      }
      values[i] = ((Long)data[i].getData()).longValue();
    }

    return values;
  }
}