/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

public class QueueFactoryTest extends TCTestCase {

  public void testQueueFactory() {
    QueueFactory qFactory = new QueueFactory();
    TCQueue queue = qFactory.createInstance();

    Assert.assertTrue(queue instanceof TCLinkedBlockingQueue);

    qFactory = new QueueFactory();
    queue = qFactory.createInstance(100);

    Assert.assertTrue(queue instanceof TCLinkedBlockingQueue);
  }
}
