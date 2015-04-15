/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.management.beans;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.L1Info;
import com.tc.management.TCClient;

import javax.management.NotCompliantMBeanException;

public class L1Dumper extends AbstractTerracottaMBean implements L1DumperMBean {

  private static final boolean  DEBUG                        = false;

  private static final TCLogger logger                       = TCLogging.getLogger(L1Dumper.class);
  private static final int      DEFAULT_THREAD_DUMP_COUNT    = 3;
  private static final long     DEFAULT_THREAD_DUMP_INTERVAL = 1000;

  public int                    threadDumpCount              = DEFAULT_THREAD_DUMP_COUNT;
  public long                   threadDumpInterval           = DEFAULT_THREAD_DUMP_INTERVAL;

  private final TCClient        tclient;
  private final L1Info          l1Info;

  public L1Dumper(TCClient tclient, L1Info l1InfoBean) throws NotCompliantMBeanException {
    super(L1DumperMBean.class, false);
    this.tclient = tclient;
    this.l1Info = l1InfoBean;
  }

  @Override
  public void doClientDump() {
    logger.info("Client dump: ");
    tclient.dump();
    try {
      doThreadDump();
    } catch (Exception e) {
      // ignore
    }
  }

  @Override
  public void setThreadDumpCount(int count) {
    threadDumpCount = count;
  }

  @Override
  public void setThreadDumpInterval(long interval) {
    threadDumpInterval = interval;
  }

  @Override
  public void doThreadDump() throws Exception {
    debugPrintln("ThreadDumping:  count=[" + threadDumpCount + "] interval=[" + threadDumpInterval + "]");
    for (int i = 0; i < threadDumpCount; i++) {
      l1Info.takeThreadDump(System.currentTimeMillis());
      Thread.sleep(threadDumpInterval);
    }
  }

  @Override
  public void reset() {
    //
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println("##### L1Dumper: " + s);
    }
  }

}
