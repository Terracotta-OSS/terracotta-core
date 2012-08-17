/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class BlockingQueueTestClient extends ClientBase {

  private static final int CAPACITY  = 100;
  private final int        numOfPut  = 1000;
  private final int        numOfLoop = 25;

  public BlockingQueueTestClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new BlockingQueueTestClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    testIllegalCapacity(toolkit, 0);
    testIllegalCapacity(toolkit, -1);
    testIllegalCapacity(toolkit, -100);

    BlockingQueue<Serializable> queue = toolkit.getBlockingQueue("test-blocking-queue", CAPACITY, null);
    testBasic(toolkit, queue, true);
    testIterator(toolkit, queue, true);

    BlockingQueue<Serializable> unboundedQueue = toolkit.getBlockingQueue("test-unbounded-blocking-queue", null);
    testBasic(toolkit, unboundedQueue, false);
    testIterator(toolkit, unboundedQueue, false);
  }

  private void testIllegalCapacity(Toolkit toolkit, int capacity) {
    try {
      toolkit.getBlockingQueue("abcdef", capacity, null);
      Assert.fail("should have thrown exception for illegal capacity - " + capacity);
    } catch (IllegalArgumentException expected) {
      //
    }
  }

  private void testBasic(Toolkit toolkit, BlockingQueue<Serializable> queue, boolean isBounded)
      throws InterruptedException, BrokenBarrierException, Exception {
    System.out.println("Client going to wait for barrier-1");
    int index = getBarrierForAllClients().await();

    for (int i = 0; i < numOfLoop; i++) {
      if (index == 0) {
        doPut(queue, isBounded);
      } else {
        doGet(queue);
      }
      getBarrierForAllClients().await();
    }

    getBarrierForAllClients().await();
  }

  private void testIterator(Toolkit toolkit, BlockingQueue<Serializable> queue, boolean isBounded)
      throws InterruptedException, BrokenBarrierException, Exception {
    System.out.println("Client going to wait for barrier-1");
    int index = getBarrierForAllClients().await();

    for (int i = 0; i < numOfLoop; i++) {
      if (index == 0) {
        doBulkPut(queue, isBounded);
      } else {
        doIterate(queue);
      }
      getBarrierForAllClients().await();
    }

    getBarrierForAllClients().await();
  }

  private void doIterate(BlockingQueue<Serializable> queue) throws Exception {
    Iterator i = queue.iterator();
    System.out.println("queue size: " + queue.size());
    while (i.hasNext()) {
      Object o = i.next();
      String wi = (String) o;
      System.out.println("Removing WorkItem: " + wi);
      i.remove();
    }
    System.out.println("queue size: " + queue.size());
  }

  private void doBulkPut(BlockingQueue<Serializable> queue, boolean isBounded) throws Exception {
    for (int i = 0; i < numOfPut; i++) {
      System.out.println("Putting " + i);
      queue.put("WorkItem(i)" + i);
      if (isBounded) Assert.assertTrue(queue.size() <= CAPACITY);
    }
  }

  private void doGet(BlockingQueue<Serializable> queue) throws Exception {
    while (true) {
      Object o = queue.take();
      System.out.println("Removed " + o);
      if ("STOP".equals(o)) {
        break;
      }
    }
  }

  private void doPut(BlockingQueue queue, boolean isBounded) throws Exception {
    for (int i = 0; i < numOfPut; i++) {
      queue.put("Item-" + i);
      System.out.println("Added Item-" + i);
      if (isBounded) Assert.assertTrue(queue.size() <= CAPACITY);
    }
    int numOfGet = getParticipantCount() - 1;
    for (int i = 0; i < numOfGet; i++) {
      queue.put("STOP");
      System.out.println("Added Stop");
    }
  }

}
