/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import com.tc.test.AppServerInfo;
import com.tc.text.Banner;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.SystemMemory;

public class LowMemWorkaround {
  static int computeNumberOfNodes(int defaultNum, AppServerInfo appServerInfo) {
    if (Os.isSolaris() && appServerInfo.getId() == AppServerInfo.GLASSFISH) {
      long mem = SystemMemory.getTotalSystemMemory();
      if (mem < (SystemMemory.GB * 2)) {
        Banner.warnBanner("Using 2 nodes (instead of " + defaultNum + ") since this machine has limited memory (" + mem
                          + ")");
        return 2;
      }
    }
    return defaultNum;
  }
}
