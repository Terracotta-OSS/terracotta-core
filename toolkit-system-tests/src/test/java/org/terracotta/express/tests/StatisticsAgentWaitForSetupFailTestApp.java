/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.statistics.retrieval.actions.SRASystemProperties;

import junit.framework.Assert;

public class StatisticsAgentWaitForSetupFailTestApp extends ClientBase {
  private static final String    MANAGER_UTIL_CLASS_NAME = "com.tc.object.bytecode.ManagerUtil";

  private final ToolkitBarrier barrier;

  public StatisticsAgentWaitForSetupFailTestApp(String[] args) {
    super(args);
    this.barrier = getClusteringToolkit().getBarrier("test", getParticipantCount());
  }

  public static void main(String[] args) {
    new StatisticsAgentWaitForSetupFailTestApp(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    ClassLoader cl = getClusteringToolkit().getMap("testMap", null, null).getClass().getClassLoader();
    Class managerUtil = cl.loadClass(MANAGER_UTIL_CLASS_NAME);
    Object manager = managerUtil.getMethod("getManager").invoke(null);

    // Assert.assertNull(ManagerUtil.getManager().getStatisticRetrievalActionInstance(SRASystemProperties.ACTION_NAME));
    Assert.assertNull(manager.getClass().getMethod("getStatisticRetrievalActionInstance", String.class)
        .invoke(manager, SRASystemProperties.ACTION_NAME));
    barrier.await();
  }

}
