/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;

import java.lang.reflect.Array;

import junit.framework.TestCase;

public class SRAMemoryUsageTest extends TestCase {
  public void testRetrieval() throws Exception {
    StatisticRetrievalAction action = new SRAMemoryUsage();

    StatisticData[] data1 = action.retrieveStatisticData();

    long[] values1 = assertMemUsageData(data1);
    long free1 = values1[0];
    long used1 = values1[1];
    long max1 = values1[2];

    int memsize = 1024 * 1024 * 10;
    byte[] mem = (byte[]) Array.newInstance(byte.class, memsize);

    StatisticData[] data2 = action.retrieveStatisticData();

    long[] values2 = assertMemUsageData(data2);
    long free2 = values2[0];
    long used2 = values2[1];
    long max2 = values2[2];

    long difference = free1 - free2;
    assertTrue(free1 > free2);
    assertTrue(used2 > used1);
    assertTrue(difference <= used2 - used1);
    assertEquals(max1, max2);

    if (mem != null) {
      mem = null;
    }

    System.gc();
    Thread.sleep(500);
    System.gc();
    Thread.sleep(500);

    StatisticData[] data3 = action.retrieveStatisticData();

    long[] values3 = assertMemUsageData(data3);
    long free3 = values3[0];
    long used3 = values3[1];
    long max3 = values3[2];

    assertTrue(free3 > free2);
    assertTrue(used2 > used3);
    assertTrue(free3 - free2 >= difference);
    assertTrue(used3 - used2 <= difference);
    assertEquals(max3, max2);
  }

  private long[] assertMemUsageData(StatisticData[] data) throws Exception {
    long[] values = new long[3];
    for (int i = 0; i < data.length; i++) {
      assertTrue(data[i].getName().startsWith(SRAMemoryUsage.ACTION_NAME));
      assertNull(data[i].getAgentIp());
      assertNull(data[i].getAgentDifferentiator());
      assertNull(data[i].getMoment());
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
      values[i] = getLongValue((String) data[i].getData());
    }

    return values;
  }

  private long getLongValue(String data) {
    if (data.endsWith("MB")) {
      return (long) (Double.parseDouble(data.replace("MB", "")) * SRAMemoryUsage.MB);
    } else if (data.endsWith("KB")) {
      return (long) (Double.parseDouble(data.replace("KB", "")) * SRAMemoryUsage.KB);
    } else if (data.endsWith("Bytes")) { return Long.parseLong(data.replace("Bytes", "")); }
    return 0;
  }
}