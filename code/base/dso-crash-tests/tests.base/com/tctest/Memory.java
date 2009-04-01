/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.statistics.retrieval.SigarUtil;


public class Memory {
  
  
  // This number isn't exact of course but it is appropriate for our monkey environments
  private static final long TWO_GIGABYTES = 2000000000L;

  public static long getCurrentMemorySize() {
    try {
      Sigar sigar = SigarUtil.newSigar();

      Mem mem = sigar.getMem();

      return mem.getTotal();
    } catch (SigarException se) {
      throw new RuntimeException(se);
    }
  }
  
  public static boolean isMemoryLow() {
    return getCurrentMemorySize() < TWO_GIGABYTES;
  }

}
