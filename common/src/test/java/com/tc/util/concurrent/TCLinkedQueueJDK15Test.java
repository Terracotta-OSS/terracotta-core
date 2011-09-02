/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.TestCase;

public class TCLinkedQueueJDK15Test extends TestCase {
  public static final int              NUMBER_OF_TRANSACTIONS = 1000000;
  public static final int              TIMEOUT                = 500;
  private static final SynchronizedInt nodeId                 = new SynchronizedInt(0);

  public void testLinkedQueue() {
    System.out.println(" --TEST CASE : testLinkedQueue");
    if (!Vm.isJDK15Compliant()) {
      System.out.println("This test is supposed to run only for JDK 1.5 and above. Exiting the test...");
      return;
    }
    TCQueue linkedBlockingQueue = (new QueueFactory()).createInstance();
    Assert.assertTrue(linkedBlockingQueue instanceof TCLinkedBlockingQueue);

    TCQueue boundedLinkedQueue = (new QueueFactory(BoundedLinkedQueue.class.getName())).createInstance();
    Assert.assertTrue(boundedLinkedQueue instanceof TCBoundedLinkedQueue);
  }

  public void testLinkedQueueCapacity() {
    System.out.println(" --TEST CASE : testLinkedQueueCapacity");
    if (!Vm.isJDK15Compliant()) {
      System.out.println("This test is supposed to run only for JDK 1.5 and above. Exiting the test...");
      return;
    }
    int capacity = 100;
    TCQueue linkedBlockingQueue = (new QueueFactory()).createInstance(capacity);
    Assert.assertTrue(linkedBlockingQueue instanceof TCLinkedBlockingQueue);

    for (int i = 0; i < capacity; i++) {
      try {
        linkedBlockingQueue.put(Integer.valueOf(i));
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    // Now try to offer, and it should fail
    try {
      boolean offered = linkedBlockingQueue.offer(Integer.valueOf(1000), 0);
      assertFalse(offered);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    // try creating with negative capacity.
    boolean failed = true;
    try {
      (new QueueFactory()).createInstance(-1);
      failed = false;
    } catch (AssertionError iae) {
      // expected
    }
    if (!failed) { throw new AssertionError("Expected to throw an Exception"); }
  }

  public void testTCQueuePutPerformance() throws Exception {
    System.out.println(" --TEST CASE : testTCQueuePutPrformance");
    if (!Vm.isJDK15Compliant()) {
      System.out.println("This test is supposed to run only for JDK 1.5 and above. Exiting the test...");
      return;
    }
    TCQueue queue = (new QueueFactory(LinkedBlockingQueue.class.getName())).createInstance(NUMBER_OF_TRANSACTIONS);

    Thread producer1 = new Producer("Producer TCLinkedBlockingQueue", queue);
    long startTime = System.currentTimeMillis();
    producer1.start();
    producer1.join();
    long endTime = System.currentTimeMillis();
    long timeTakenProducer = endTime - startTime;

    Thread consumer1 = new Consumer("Consumer TCLinkedBlockingQueue", queue);
    startTime = System.currentTimeMillis();
    consumer1.start();
    consumer1.join();
    endTime = System.currentTimeMillis();
    long timeTakenConsumer = endTime - startTime;

    System.out.println("\n********************************* TCLinkedBlockingQueue *********************************");
    System.out.println("Inserted " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTakenProducer + " milliseconds");
    System.out.println("Removed " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTakenConsumer + " milliseconds");
    System.out.println("*****************************************************************************************\n");

    TCQueue tcBoundedLinkedQueue = new TCBoundedLinkedQueue(NUMBER_OF_TRANSACTIONS);
    nodeId.set(0);

    Thread producer2 = new Producer("Producer TCBoundedLinkedQueue", tcBoundedLinkedQueue);
    startTime = System.currentTimeMillis();
    producer2.start();
    producer2.join();
    endTime = System.currentTimeMillis();
    timeTakenProducer = endTime - startTime;

    Thread consumer2 = new Consumer("Consumer TCBoundedLinkedQueue", tcBoundedLinkedQueue);
    startTime = System.currentTimeMillis();
    consumer2.start();
    consumer2.join();
    endTime = System.currentTimeMillis();
    timeTakenConsumer = endTime - startTime;

    System.out.println("\n********************************* TCBoundedLinkedQueue *********************************");
    System.out.println("Inserted " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTakenProducer + " milliseconds");
    System.out.println("Removed " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTakenConsumer + " milliseconds");
    System.out.println("****************************************************************************************\n");
  }

  public void testTCQueueMultiThreadPrformance() throws Exception {
    System.out.println(" --TEST CASE : testTCQueueMultiThreadPrformance");
    if (!Vm.isJDK15Compliant()) {
      System.out.println("This test is supposed to run only for JDK 1.5 and above. Exiting the test...");
      return;
    }
    TCQueue queue = (new QueueFactory(LinkedBlockingQueue.class.getName())).createInstance(NUMBER_OF_TRANSACTIONS);
    nodeId.set(0);

    Thread producer1 = new Producer("Producer1", queue);
    Thread producer2 = new Producer("Producer2", queue);
    Thread producer3 = new Producer("Producer3", queue);
    Thread producer4 = new Producer("Producer4", queue);

    Thread consumer1 = new Consumer("Consumer1", queue);
    Thread consumer2 = new Consumer("Consumer2", queue);
    Thread consumer3 = new Consumer("Consumer3", queue);
    Thread consumer4 = new Consumer("Consumer4", queue);

    long startTime = System.currentTimeMillis();
    producer1.start();
    producer2.start();
    producer3.start();
    producer4.start();

    consumer1.start();
    consumer2.start();
    consumer3.start();
    consumer4.start();

    producer1.join();
    producer2.join();
    producer3.join();
    producer4.join();

    consumer1.join();
    consumer2.join();
    consumer3.join();
    consumer4.join();

    long endTime = System.currentTimeMillis();
    long timeTaken = endTime - startTime;

    System.out.println("\n********************************* TCLinkedBlockingQueue *********************************");
    System.out.println("Operated on " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTaken + " milliseconds");
    System.out.println("*****************************************************************************************\n");

    TCQueue boundedLinkedueue = new TCBoundedLinkedQueue(NUMBER_OF_TRANSACTIONS);
    nodeId.set(0);

    Thread producer5 = new Producer("Producer1", boundedLinkedueue);
    Thread producer6 = new Producer("Producer2", boundedLinkedueue);
    Thread producer7 = new Producer("Producer3", boundedLinkedueue);
    Thread producer8 = new Producer("Producer4", boundedLinkedueue);

    Thread consumer5 = new Consumer("Consumer1", boundedLinkedueue);
    Thread consumer6 = new Consumer("Consumer2", boundedLinkedueue);
    Thread consumer7 = new Consumer("Consumer3", boundedLinkedueue);
    Thread consumer8 = new Consumer("Consumer4", boundedLinkedueue);

    startTime = System.currentTimeMillis();
    producer5.start();
    producer6.start();
    producer7.start();
    producer8.start();

    consumer5.start();
    consumer6.start();
    consumer7.start();
    consumer8.start();

    producer5.join();
    producer6.join();
    producer7.join();
    producer8.join();

    consumer5.join();
    consumer6.join();
    consumer7.join();
    consumer8.join();

    endTime = System.currentTimeMillis();
    timeTaken = endTime - startTime;

    System.out.println("\n********************************* TCBoundedLinkedQueue *********************************");
    System.out.println("Operated on " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTaken + " milliseconds");
    System.out.println("****************************************************************************************\n");

  }

  private synchronized static int getNextNodeID() {
    return nodeId.increment();
  }

  private synchronized static int getCurrentNodeID() {
    return nodeId.get();
  }

  private static class Producer extends Thread {
    private final TCQueue queue;

    public Producer(String name, TCQueue queue) {
      this.setName(name);
      this.queue = queue;
    }

    @Override
    public void run() {
      System.out.println(this.getName());
      while (true) {
        int id = getNextNodeID();
        if (id > NUMBER_OF_TRANSACTIONS) break;
        MyNode node = new MyNode(id);
        try {
          if (id % 5000 == 0) System.out.println("Thread " + this.getName() + " inserted node number " + id);
          queue.put(node);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }

  private static class Consumer extends Thread {
    private final TCQueue queue;

    public Consumer(String name, TCQueue queue) {
      this.setName(name);
      this.queue = queue;
    }

    @Override
    public void run() {
      System.out.println(this.getName());
      while (true) {
        MyNode myNode;
        try {
          myNode = (MyNode) queue.poll(TIMEOUT);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }

        if (myNode == null) {
          if (getCurrentNodeID() >= NUMBER_OF_TRANSACTIONS) return;
          continue;
        }
        int id = myNode.getId();
        if (id % 5000 == 0) System.out.println("Thread " + this.getName() + " removed node number " + id);
        if (id >= NUMBER_OF_TRANSACTIONS) break;
      }
    }
  }

  private static class MyNode {
    private final int id;

    public MyNode(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }
}
