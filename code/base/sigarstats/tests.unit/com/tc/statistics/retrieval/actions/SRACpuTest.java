/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import junit.framework.TestCase;

import org.hyperic.sigar.Sigar;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.SigarUtil;

import java.math.BigDecimal;

public class SRACpuTest extends TestCase {
  static {
    SigarUtil.sigarInit();
  }
  private Sigar sigar;

  public void setUp() {
    sigar = new Sigar();
  }

  public void tearDown() {
    sigar.close();
  }

  public void testRetrieval() throws Exception {
    int cpuCount = sigar.getCpuInfoList().length;

    StatisticRetrievalAction action = new SRACpu();

    StatisticData[] data1 = action.retrieveStatisticData();

    BigDecimal[][] values1 = assertCpuData(cpuCount, data1);

    // creating more threads than CPUs, this should have at least one of these threads running on each CPU
    int threadCount = cpuCount*2;
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] = new UseCpuThread();
      threads[i].start();
    }

    Thread.sleep(2000);

    StatisticData[] data2 = action.retrieveStatisticData();

    BigDecimal[][] values2 = assertCpuData(cpuCount, data2);

    // stop the threads and wait for them to finish
    for (int i = 0; i < threadCount; i++) {
      threads[i].interrupt();
    }

    for (int i = 0; i < threadCount; i++) {
      threads[i].join();
    }

    // assert that the cpu usage was higher during the second data collection
    for (int i = 0; i < cpuCount; i++) {
      assertTrue(values1[i][0].compareTo(values2[i][0]) < 0);
      assertTrue(values1[i][1].compareTo(values2[i][1]) > 0);
    }

    // assert that the cpu usage was almost the maximum during the second data collection
    for (int i = 0; i < cpuCount; i++) {
      assertTrue(values2[i][0].compareTo(new BigDecimal("0.95")) > 0);
      assertTrue(values2[i][1].compareTo(new BigDecimal("0.05")) < 0);
    }
  }

  private class UseCpuThread extends Thread {
    public void run() {
      for (int i = 0; i < Integer.MAX_VALUE; i++) {
        if (0 == i % 1000) {
          if (isInterrupted()) {
            return;
          }
          Thread.yield();
        }
      }
    }
  }

  private BigDecimal[][] assertCpuData(int cpuCount, StatisticData[] data) throws Exception {
    BigDecimal[][] values = new BigDecimal[cpuCount][6];
    assertEquals(cpuCount * 6, data.length);
    for (int i = 0; i < data.length; i++) {
      assertTrue(data[i].getName().startsWith(SRACpu.ACTION_NAME));
      assertNull(data[i].getAgentIp()); // will be filled in with default
      assertNull(data[i].getAgentDifferentiator()); // will be filled in with default
      assertNull(data[i].getMoment()); // will be filled in with default

      int part = i % 6;
      int cpu = i / 6;

      assertEquals("cpu " + cpu, data[i].getElement());
      switch (part) {
        case 0:
          assertEquals(SRACpu.DATA_NAME_COMBINED, data[i].getName());
          break;
        case 1:
          assertEquals(SRACpu.DATA_NAME_IDLE, data[i].getName());
          break;
        case 2:
          assertEquals(SRACpu.DATA_NAME_NICE, data[i].getName());
          break;
        case 3:
          assertEquals(SRACpu.DATA_NAME_SYS, data[i].getName());
          break;
        case 4:
          assertEquals(SRACpu.DATA_NAME_USER, data[i].getName());
          break;
        case 5:
          assertEquals(SRACpu.DATA_NAME_WAIT, data[i].getName());
          break;
        default:
          fail();
          break;
      }
      values[cpu][part] = (BigDecimal)data[i].getData();
    }

    return values;
  }
}