/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TCClient;
import com.tc.util.runtime.ThreadDumpUtil;

import javax.management.NotCompliantMBeanException;

public class L1Dumper extends AbstractTerracottaMBean implements L1DumperMBean {

  private static final boolean  DEBUG                         = false;

  private static final TCLogger logger                        = TCLogging.getLogger(L1Dumper.class);
  private static final int      DEFAULT_THREAD_DUMP_COUNT     = 3;
  private static final long     DEFAULT_THREAD_DUMP_INTERVAL  = 1000;

  public int                    threadDumpCount               = DEFAULT_THREAD_DUMP_COUNT;
  public long                   threadDumpInterval            = DEFAULT_THREAD_DUMP_INTERVAL;

  private final TCClient        tclient;

  public L1Dumper(TCClient tclient) throws NotCompliantMBeanException {
    super(L1DumperMBean.class, false);
    this.tclient = tclient;
  }

  public void doClientDump() {
    logger.info("Client dump: ");
    tclient.dump();
    try {
      doThreadDump();
    } catch (Exception e) {
      // ignore
    }
  }

  public void setThreadDumpCount(int count) {
    threadDumpCount = count;
  }

  public void setThreadDumpInterval(long interval) {
    threadDumpInterval = interval;
  }

  public void doThreadDump() throws Exception {
    debugPrintln("ThreadDumping:  count=[" + threadDumpCount + "] interval=[" + threadDumpInterval + "]");
    for (int i = 0; i < threadDumpCount; i++) {
      TCLogging.getDumpLogger().info(ThreadDumpUtil.getThreadDump());
      Thread.sleep(threadDumpInterval);
    }
  }

  public void reset() {
    //
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println("##### L1Dumper: " + s);
    }
  }

}
