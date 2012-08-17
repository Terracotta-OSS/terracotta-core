/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.express.tests.util.ClusteredStringBuilder;
import org.terracotta.express.tests.util.ClusteredStringBuilderFactory;
import org.terracotta.express.tests.util.ClusteredStringBuilderFactoryImpl;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

public class LockClient2 extends ClientBase {

  public LockClient2(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new LockClient2(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    ClusteredStringBuilderFactory csbFactory = new ClusteredStringBuilderFactoryImpl(toolkit);
    ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder(LockTest.clientBucket);
    ToolkitLock lock = toolkit.getLock("LockClient lock");
    getBarrierForAllClients().await();
    lock.lock();
    try {
      bucket.append(LockClient2.class.getName() + "\n");
    } finally {
      lock.unlock();
    }
    getBarrierForAllClients().await();

    System.out.println(bucket);
  }
}
