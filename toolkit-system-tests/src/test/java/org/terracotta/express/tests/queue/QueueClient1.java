/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.BlockingQueue;

public class QueueClient1 extends ClientBase {

  public QueueClient1(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new QueueClient1(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    BlockingQueue queue = toolkit.getBlockingQueue("BarrierClient", null);
    System.out.println(queue.take().toString());
  }
}
