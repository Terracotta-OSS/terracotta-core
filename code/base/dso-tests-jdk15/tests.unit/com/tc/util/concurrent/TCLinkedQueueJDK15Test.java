/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.util.Date;

import junit.framework.TestCase;

public class TCLinkedQueueJDK15Test extends TestCase {
  public static final int              NUMBER_OF_TRANSACTIONS = 10000000;
  public static final int              TIMEOUT                = 500;
  private static final SynchronizedInt nodeId                 = new SynchronizedInt(0);

  public void testLinkedQueue() {
    System.out.println(" --TEST CASE : testLinkedQueue");
    if (!Vm.isJDK15Compliant()) {
      System.out.println("This test is supposed to run only for JDK 1.5 and above. Exiting the test...");
      return;
    }
    FastQueue queue = QueueFactory.createInstance();
    Assert.assertTrue(queue instanceof TCLinkedQueue);
  }

  public void testTCQueuePutPrformance() throws Exception {
    System.out.println(" --TEST CASE : testTCQueuePutPrformance");
    if (!Vm.isJDK15Compliant()) {
      System.out.println("This test is supposed to run only for JDK 1.5 and above. Exiting the test...");
      return;
    }
    FastQueue queue = QueueFactory.createInstance();

    Thread producer = new Producer("Producer", queue);
    long startTime = (new Date()).getTime();
    producer.start();
    producer.join();
    long endTime = (new Date()).getTime();
    long timeTakenProducer = endTime - startTime;
    
    Thread consumer = new Consumer("Consumer", queue);
    startTime = (new Date()).getTime();
    consumer.start();
    consumer.join();
    endTime = (new Date()).getTime();
    long timeTakenConsumer = endTime - startTime;

    System.out.println("Inserted " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTakenProducer + " milliseconds");
    System.out.println("Removed " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTakenConsumer + " milliseconds");
    
    ThreadUtil.reallySleep(10000);
    
  }

  public void testTCQueueMultiThreadPrformance() throws Exception {
    System.out.println(" --TEST CASE : testTCQueueMultiThreadPrformance");
    if (!Vm.isJDK15Compliant()) {
      System.out.println("This test is supposed to run only for JDK 1.5 and above. Exiting the test...");
      return;
    }
    FastQueue queue = QueueFactory.createInstance();
    queue.setCapacity(NUMBER_OF_TRANSACTIONS);
    nodeId.set(0);

    Thread producer1 = new Producer("Producer1", queue);
    Thread producer2 = new Producer("Producer2", queue);
    Thread producer3 = new Producer("Producer3", queue);
    Thread producer4 = new Producer("Producer4", queue);

    Thread consumer1 = new Consumer("Consumer1", queue);
    Thread consumer2 = new Consumer("Consumer2", queue);
    Thread consumer3 = new Consumer("Consumer3", queue);
    Thread consumer4 = new Consumer("Consumer4", queue);
    
    long startTime = (new Date()).getTime();
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
    
    long endTime = (new Date()).getTime();
    long timeTaken = endTime - startTime;
    
    System.out.println("Operated on " + NUMBER_OF_TRANSACTIONS + " nodes in " + timeTaken + " milliseconds");
    
  }
  
  private synchronized static int getNextNodeID() {
    return nodeId.increment();
  }

  private synchronized static int getCurrentNodeID() {
    return nodeId.get();
  }

  private static class Producer extends Thread {
    private FastQueue queue;

    public Producer(String name, FastQueue queue) {
      this.setName(name);
      this.queue = queue;
    }

    public void run() {
      while (true) {
        int id = getNextNodeID();
        if (id > NUMBER_OF_TRANSACTIONS) break;
        MyNode node = new MyNode(id);
        try {
          if (id % 100000 == 0) System.out.println("Thread " + this.getName() + " inserted node number " + id);
          queue.put(node);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }

  private static class Consumer extends Thread {
    private FastQueue queue;

    public Consumer(String name, FastQueue queue) {
      this.setName(name);
      this.queue = queue;
    }

    public void run() {
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
        if (id % 100000 == 0) System.out.println("Thread " + this.getName() + " removed node number " + id);
        if (id > NUMBER_OF_TRANSACTIONS) break;
      }
    }
  }

  private static class MyNode {
    private int id;

    public MyNode() {
      this(-1);
    }

    public MyNode(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }
}
