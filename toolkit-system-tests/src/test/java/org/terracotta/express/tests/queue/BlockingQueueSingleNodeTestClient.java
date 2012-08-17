/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;

public class BlockingQueueSingleNodeTestClient extends ClientBase {

  public BlockingQueueSingleNodeTestClient(String[] args) {
    super(args);
  }

  private static final int    NUM_OF_PUTS           = 1000;
  private static final int    NUM_OF_LOOPS          = 1;
  private static final int    NUM_OF_PUTTER         = 1;
  private static final int    NUM_OF_GETTER         = 1;

  private static final String linkedBlockingQueueId = "LinkedBlockingQueueSingleNodeTestQueue";

  public static void main(String[] args) {
    new BlockingQueueSingleNodeTestClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    System.out.println("XXXXX starting test...");
    BlockingQueue<Serializable> queue = toolkit.getBlockingQueue(linkedBlockingQueueId, null);

    CyclicBarrier testBarrier = new CyclicBarrier(NUM_OF_PUTTER + NUM_OF_GETTER + 1);
    for (int i = 0; i < NUM_OF_LOOPS; i++) {
      Thread[] putters = new Thread[NUM_OF_PUTTER];
      Thread[] getters = new Thread[NUM_OF_GETTER];
      for (int j = 0; j < NUM_OF_PUTTER; j++) {
        putters[j] = new Thread(new Putter(testBarrier, queue, NUM_OF_GETTER));
      }
      for (int j = 0; j < NUM_OF_GETTER; j++) {
        getters[j] = new Thread(new Getter(testBarrier, queue));
      }
      for (int j = 0; j < NUM_OF_PUTTER; j++) {
        putters[j].start();
      }
      for (int j = 0; j < NUM_OF_GETTER; j++) {
        getters[j].start();
      }
    }
    testBarrier.await();
  }

  private static class Getter implements Runnable {
    private final BlockingQueue queue;
    private final CyclicBarrier barrier;

    public Getter(CyclicBarrier barrier, BlockingQueue queue) {
      this.barrier = barrier;
      this.queue = queue;
    }

    public void run() {
      try {
        while (true) {
          Object o = queue.take();
          if ("STOP".equals(o)) {
            break;
          }
          System.out.println("Getting " + o);
        }
        barrier.await();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  private static class Putter implements Runnable {
    private final CyclicBarrier barrier;
    private final BlockingQueue queue;
    private final int           numOfGetter;

    public Putter(CyclicBarrier barrier, BlockingQueue queue, int numOfGetter) {
      this.barrier = barrier;
      this.queue = queue;
      this.numOfGetter = numOfGetter;
    }

    public void run() {
      try {
        for (int i = 0; i < NUM_OF_PUTS; i++) {
          System.out.println("Putting " + i);
          queue.put(i);
        }
        for (int i = 0; i < numOfGetter; i++) {
          queue.put("STOP");
        }
        barrier.await();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }
}
