/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.test.AppServerInfo;
import com.tc.text.Banner;

public class LowMemWorkaround {
  // This number isn't exact of course but it is appropriate for our monkey environments
  private static final long TWO_GIGABYTES = 2000000000L;

  static int computeNumberOfNodes(int defaultNum, AppServerInfo appServerInfo) {
    try {
      Sigar sigar = new Sigar();

      Mem mem = sigar.getMem();

      long memTotal = mem.getTotal();
      if (memTotal < TWO_GIGABYTES) {
        Banner.warnBanner("Using 2 nodes (instead of " + defaultNum + ") since this machine has limited memory ("
                          + memTotal + ")");
        return 2;
      }

      return defaultNum;
    } catch (SigarException se) {
      throw new RuntimeException(se);
    }
  }
}
