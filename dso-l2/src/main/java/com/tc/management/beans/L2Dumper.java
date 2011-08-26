/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaManagement;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class L2Dumper extends AbstractTerracottaMBean implements L2DumperMBean {
  private static final TCLogger logger                        = TCLogging.getLogger(L2Dumper.class);

  private static final boolean  DEBUG                         = false;

  private static final String   THREAD_DUMP_METHOD_NAME       = "dumpThreadsMany";
  private static final Class[]  THREAD_DUMP_METHOD_PARAMETERS = new Class[] { int.class, long.class };
  private static final int      DEFAULT_THREAD_DUMP_COUNT     = 3;
  private static final long     DEFAULT_THREAD_DUMP_INTERVAL  = 1000;

  private int                   threadDumpCount               = DEFAULT_THREAD_DUMP_COUNT;
  private long                  threadDumpInterval            = DEFAULT_THREAD_DUMP_INTERVAL;

  private final TCDumper        dumper;

  private final MBeanServer     mbs;

  public L2Dumper(TCDumper dumper, MBeanServer mbs) throws NotCompliantMBeanException {
    super(L2DumperMBean.class, false);
    this.dumper = dumper;
    this.mbs = mbs;
  }

  public void doServerDump() {
    logger.info("Server dump: ");
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
    Object[] args = { Integer.valueOf(threadDumpCount), Long.valueOf(threadDumpInterval) };
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

  public void dumpClusterState() {
    Set allL2DumperMBeans;
    try {
      allL2DumperMBeans = TerracottaManagement.getAllL2DumperMBeans(mbs);
    } catch (Exception e) {
      logger.error(e);
      return;
    }

    for (Iterator i = allL2DumperMBeans.iterator(); i.hasNext();) {
      ObjectName l2DumperBean = (ObjectName) i.next();
      try {
        mbs.invoke(l2DumperBean, "doServerDump", new Object[] {}, new String[] {});
      } catch (Exception e) {
        logger.error("error dumping on " + l2DumperBean, e);
      }
    }

    Set allL1DumperMBeans;
    try {
      allL1DumperMBeans = TerracottaManagement.getAllL1DumperMBeans(mbs);
    } catch (Exception e) {
      logger.error(e);
      return;
    }

    for (Iterator i = allL1DumperMBeans.iterator(); i.hasNext();) {
      ObjectName l1DumperBean = (ObjectName) i.next();
      try {
        mbs.invoke(l1DumperBean, "doClientDump", new Object[] {}, new String[] {});
      } catch (Exception e) {
        logger.error("error dumping on " + l1DumperBean, e);
      }
    }
  }

}
