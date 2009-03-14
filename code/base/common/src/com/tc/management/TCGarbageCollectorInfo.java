/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TCGarbageCollectorInfo {

  private final String       CMS_NAME      = "ConcurrentMarkSweep";
  public static final String CMS_WARN_MESG = "Terracotta does not recommend ConcurrentMarkSweep Collector.";

  private final boolean      isSetCMS;
  private final List         gcNames       = new ArrayList<String>();

  public TCGarbageCollectorInfo() {
    List<GarbageCollectorMXBean> gcmbeans = ManagementFactory.getGarbageCollectorMXBeans();
    boolean foundCMS = false;
    for (GarbageCollectorMXBean mbean : gcmbeans) {
      String gcname = mbean.getName();
      gcNames.add(gcname);
      if (CMS_NAME.equals(gcname)) foundCMS = true;
    }
    isSetCMS = foundCMS;
  }

  public boolean isCMS() {
    return isSetCMS;
  }

  public List<String> getGcNames() {
    return Collections.unmodifiableList(gcNames);
  }

}
