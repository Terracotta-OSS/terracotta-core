/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.Sigar;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.SigarUtil;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import junit.framework.TestCase;

public class SRACpuTest extends TestCase {
  private Sigar sigar;

  @Override
  public void setUp() {
    sigar = SigarUtil.newSigar();
  }

  @Override
  public void tearDown() {
    sigar.close();
  }

  public void testRetrieval() throws Exception {

    int cpuCount = sigar.getCpuInfoList().length;

    StatisticRetrievalAction action = new SRACpu();

    StatisticData[] data1;
    BigDecimal[][] values1;
    
    final MathContext mathcontext = new MathContext(1, RoundingMode.UP);

    // continue looping until the CPU measurements are within the expected ranges to do the test
    while (true) {
      // wait a while before kicking off this test, trying to ensure that the CPU is doing as little as possible
      Thread.sleep(5000);

      data1 = action.retrieveStatisticData();
      values1 = assertCpuData(cpuCount, data1);
      
      int cpuOkCount = 0;
      for (int i = 0; i < cpuCount; i++) {
        // applying rounding for some occasional sampling errors compensation
        BigDecimal values1_0 = values1[i][0].round(mathcontext);
        BigDecimal values1_1 = values1[i][1].round(mathcontext);
        
        System.out.println("baseline test values for cpu " + i + ": " + values1_0 + " <= 0.9, " + values1_1 + " >= 0.0");

        if (values1_0.compareTo(new BigDecimal("0.9")) <= 0 &&
            values1_1.compareTo(new BigDecimal("0.0")) >= 0) {
          cpuOkCount++;
        }
      }
      
      if (cpuCount == cpuOkCount) {
        System.out.println("baseline test values are acceptable, proceeding to next test phase");
        break;
      }
      System.out.println("baseline test values not acceptable, doing another sampling");
    }

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
      // applying rounding for some occasional sampling errors compensation
      BigDecimal values1_0 = values1[i][0].round(mathcontext);
      BigDecimal values1_1 = values1[i][1].round(mathcontext);
      BigDecimal values2_0 = values2[i][0].round(mathcontext);
      BigDecimal values2_1 = values2[i][1].round(mathcontext);

      System.out.println("test values for cpu " + i + ": " + values1_0 + " <= " + values2_0 + ", "
                         + values1_1 + " >= " + values2_1);
      assertTrue(values1_0.compareTo(values2_0) <= 0);
      assertTrue(values1_1.compareTo(values2_1) >= 0);
    }
  }

  private class UseCpuThread extends Thread {
    @Override
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

  private BigDecimal[][] assertCpuData(final int cpuCount, final StatisticData[] data) throws Exception {
    BigDecimal[][] values = new BigDecimal[cpuCount][6];
    assertEquals(cpuCount * 6, data.length);
    for (int i = 0; i < data.length; i++) {
      assertTrue(data[i].getName().startsWith(SRACpuConstants.ACTION_NAME));
      assertNull(data[i].getAgentIp()); // will be filled in with default
      assertNull(data[i].getAgentDifferentiator()); // will be filled in with default
      assertNull(data[i].getMoment()); // will be filled in with default

      int part = i % 6;
      int cpu = i / 6;

      assertEquals("cpu " + cpu, data[i].getElement());
      switch (part) {
        case 0:
          assertEquals(SRACpuConstants.DATA_NAME_COMBINED, data[i].getName());
          break;
        case 1:
          assertEquals(SRACpuConstants.DATA_NAME_IDLE, data[i].getName());
          break;
        case 2:
          assertEquals(SRACpuConstants.DATA_NAME_NICE, data[i].getName());
          break;
        case 3:
          assertEquals(SRACpuConstants.DATA_NAME_SYS, data[i].getName());
          break;
        case 4:
          assertEquals(SRACpuConstants.DATA_NAME_USER, data[i].getName());
          break;
        case 5:
          assertEquals(SRACpuConstants.DATA_NAME_WAIT, data[i].getName());
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
