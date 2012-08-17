/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class BlockingQueueMultiThreadTestClient extends ClientBase {

  private static final int NUM_OF_PUTS    = 1000;
  private static final int NUM_OF_THREADS = 5;
  private static final int CAPACITY       = 2;

  public BlockingQueueMultiThreadTestClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new BlockingQueueMultiThreadTestClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    final BlockingQueue<Serializable> queue = toolkit.getBlockingQueue("test-blocking-queue", CAPACITY, null);
    final ToolkitAtomicLong putCount = toolkit.getAtomicLong("putCount");
    final ToolkitAtomicLong getCount = toolkit.getAtomicLong("getCount");
    int index = getBarrierForAllClients().await();

    final CyclicBarrier localBarrier = new CyclicBarrier(NUM_OF_THREADS + 1);
    if (index == 0) {
      Thread[] putter = new Thread[NUM_OF_THREADS];
      for (int i = 0; i < NUM_OF_THREADS; i++) {
        final int k = i;
        putter[k] = new Thread(new Runnable() {
          public void run() {
            try {
              localBarrier.await();
              for (int j = 0; j < NUM_OF_PUTS; j++) {
                int seed = (this.hashCode() ^ (int) System.nanoTime());
                putCount.addAndGet(seed);
                queue.put(Integer.valueOf(seed));
              }
              localBarrier.await();
            } catch (Throwable t) {
              throw new AssertionError(t);
            }
          }

        });
      }
      for (int i = 0; i < NUM_OF_THREADS; i++) {
        putter[i].start();
      }
      localBarrier.await();
      localBarrier.await();
    } else {
      Thread[] getter = new Thread[NUM_OF_THREADS];
      for (int i = 0; i < NUM_OF_THREADS; i++) {
        final int k = i;
        getter[k] = new Thread(new Runnable() {
          public void run() {
            try {
              localBarrier.await();
              for (int j = 0; j < NUM_OF_PUTS; j++) {
                Integer o = (Integer) queue.take();
                getCount.addAndGet(o.intValue());
              }
              localBarrier.await();
            } catch (Throwable t) {
              throw new AssertionError(t);
            }

          }
        });
      }
      for (int i = 0; i < NUM_OF_THREADS; i++) {
        getter[i].start();
      }
      localBarrier.await();
      localBarrier.await();
    }
    getBarrierForAllClients().await();

    if (index == 0) {
      Assert.assertEquals(putCount.longValue(), getCount.longValue());
    }

    getBarrierForAllClients().await();
  }
}
