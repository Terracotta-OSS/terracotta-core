/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.AbstractTerracottaMBean;

import java.lang.reflect.Method;

import javax.management.NotCompliantMBeanException;

public class L2Dumper extends AbstractTerracottaMBean implements L2DumperMBean {
  private static final boolean DEBUG                         = false;

  public static final String   THREAD_DUMP_METHOD_NAME       = "dumpThreadsMany";
  public static final Class[]  THREAD_DUMP_METHOD_PARAMETERS = new Class[] { int.class, long.class };
  public static final int      DEFAULT_THREAD_DUMP_COUNT     = 3;
  public static final long     DEFAULT_THREAD_DUMP_INTERVAL  = 1000;

  public int                   threadDumpCount               = DEFAULT_THREAD_DUMP_COUNT;
  public long                  threadDumpInterval            = DEFAULT_THREAD_DUMP_INTERVAL;

  private final TCDumper       dumper;

  public L2Dumper(TCDumper dumper) throws NotCompliantMBeanException {
    super(L2DumperMBean.class, false);
    this.dumper = dumper;
  }

  public void doServerDump() {
    dumper.dump();
  }

  public void setThreadDumpCount(int count) {
    threadDumpCount = count;
  }

  public void setThreadDumpInterval(long interval) {
    threadDumpInterval = interval;
  }

  public int doThreadDump() throws Exception {
    debugPrintln("ThreadDumping:  count=[" + threadDumpCount + "] interval=[" + threadDumpInterval + "]");
    Class threadDumpClass = getClass().getClassLoader().loadClass("com.tc.util.runtime.ThreadDump");
    Method method = threadDumpClass.getMethod(THREAD_DUMP_METHOD_NAME, THREAD_DUMP_METHOD_PARAMETERS);
    Object[] args = { new Integer(threadDumpCount), new Long(threadDumpInterval) };
    int pid = ((Integer) method.invoke(null, args)).intValue();
    return pid;
  }

  public void reset() {
    //
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println("##### L2Dumper: " + s);
    }
  }

}
