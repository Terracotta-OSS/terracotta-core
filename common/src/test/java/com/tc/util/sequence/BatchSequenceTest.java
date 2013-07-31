/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.sequence;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class BatchSequenceTest extends TestCase {

  public void test() throws Exception {
    TestRemoteBatchIDProvider remote = new TestRemoteBatchIDProvider();
    final BatchSequence sequence = new BatchSequence(remote, 5);
    final BlockingQueue<Long> longs = new LinkedBlockingQueue<Long>();

    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        longs.add(Long.valueOf(sequence.next()));
      }
    }, "BatchIDProviderTestThread");
    t.start();
    assertTrue(longs.poll(2000, TimeUnit.MILLISECONDS) == null);
    assertTrue(remote.take() == sequence);
    assertTrue(remote.size == 5);

    remote.clear();
    sequence.setNextBatch(0, 5);
    assertTrue(remote.take() != null);
    remote.clear();
    sequence.setNextBatch(5, 10);

    assertTrue(longs.take().longValue() == 0);
    assertTrue(sequence.next() == 1);
    assertTrue(sequence.next() == 2);
    assertTrue(sequence.next() == 3);
    assertTrue(sequence.next() == 4);
    assertTrue(remote.isEmpty());
    assertTrue(sequence.next() == 5);
    assertFalse(remote.isEmpty());
    assertTrue(remote.take() != null);
    sequence.setNextBatch(10, 15);
    assertTrue(sequence.next() == 6);
  }

  private static class TestRemoteBatchIDProvider implements BatchSequenceProvider {
    public volatile int      size  = -1;
    public final BlockingQueue<BatchSequenceReceiver> queue = new LinkedBlockingQueue<BatchSequenceReceiver>();

    @Override
    public void requestBatch(BatchSequenceReceiver theProvider, int theSize) {
      this.size = theSize;
      try {
        queue.put(theProvider);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    public boolean isEmpty() {
      return queue.isEmpty();
    }

    public Object take() throws InterruptedException {
      return queue.take();
    }

    public void clear() throws InterruptedException {
      this.size = -1;
      while (!queue.isEmpty()) {
        queue.take();
      }
    }
  }
}
