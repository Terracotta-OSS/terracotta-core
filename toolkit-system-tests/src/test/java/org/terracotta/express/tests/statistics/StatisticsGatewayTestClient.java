/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.statistics;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.TimeUnit;

public class StatisticsGatewayTestClient extends ClientBase {

  public StatisticsGatewayTestClient(String[] args) {
    super(args);

  }

  public static void main(String[] args) {
    new StatisticsGatewayTestClient(args).run();
  }

  public static final int NODE_COUNT = 2;

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    try {
      getBarrierForAllClients().await();
    } catch (InterruptedException ie) {
      throw new AssertionError();
    }
    TimeUnit.SECONDS.sleep(60);

  }
}
