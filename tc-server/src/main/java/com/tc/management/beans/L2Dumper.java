/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.management.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaManagement;
import com.tc.server.TCServerImpl;

import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class L2Dumper extends AbstractTerracottaMBean implements L2DumperMBean {
  private static final Logger logger = LoggerFactory.getLogger(L2Dumper.class);

  private final TCServerImpl        dumper;

  private final MBeanServer     mbs;

  public L2Dumper(TCServerImpl dumper, MBeanServer mbs) throws NotCompliantMBeanException {
    super(L2DumperMBean.class, false);
    this.dumper = dumper;
    this.mbs = mbs;
  }

  @Override
  public void doServerDump() {
    logger.info("Server dump: ");
    dumper.dump();
  }

  @Override
  public void doThreadDump() throws Exception {
    logger.info("Server Threads: ");
    Map<Thread,StackTraceElement[]> threads = Thread.getAllStackTraces();
    StringBuilder allThreads = new StringBuilder("\n\n");
    for (Map.Entry<Thread, StackTraceElement[]> s : threads.entrySet()) {
      allThreads.append(s.getKey().getName());
      allThreads.append(":\n");
      for (StackTraceElement e : s.getValue()) {
        allThreads.append("    ");
        allThreads.append(e.toString());
        allThreads.append('\n');
      }
    }
    logger.info(allThreads.toString());
  }

  @Override
  public void reset() {
    //
  }

  @Override
  public void dumpClusterState() {
    Set<ObjectName> allL2DumperMBeans;
    try {
      allL2DumperMBeans = TerracottaManagement.getAllL2DumperMBeans(mbs);
    } catch (Exception e) {
      logger.error("Exception: ", e);
      return;
    }

    for (ObjectName l2DumperBean : allL2DumperMBeans) {
      try {
        mbs.invoke(l2DumperBean, "doServerDump", new Object[] {}, new String[] {});
      } catch (Exception e) {
        logger.error("error dumping on " + l2DumperBean, e);
      }
    }
  }

}
