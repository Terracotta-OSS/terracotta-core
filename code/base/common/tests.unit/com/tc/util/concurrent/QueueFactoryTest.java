/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.concurrent.LinkedBlockingQueue;

public class QueueFactoryTest extends TCTestCase {

  public void testQueueFactory() {
    QueueFactory qFactory = new QueueFactory(BoundedLinkedQueue.class.getName());
    TCQueue queue = qFactory.createInstance();

    Assert.assertTrue(queue instanceof TCBoundedLinkedQueue);

    qFactory = new QueueFactory(LinkedBlockingQueue.class.getName());
    queue = qFactory.createInstance();

    Assert.assertTrue(queue instanceof TCLinkedBlockingQueue);

    qFactory = new QueueFactory(BoundedLinkedQueue.class.getName());
    queue = qFactory.createInstance(100);

    Assert.assertTrue(queue instanceof TCBoundedLinkedQueue);

    qFactory = new QueueFactory(LinkedBlockingQueue.class.getName());
    queue = qFactory.createInstance(100);

    Assert.assertTrue(queue instanceof TCLinkedBlockingQueue);
  }
}
