/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.stats;

import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.CircularLossyQueue;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

public class CircularLossyQueueTest extends TCTestCase {
  public static final int                            SIZE = 30;
  public CircularLossyQueue<TimeStampedCounterValue> queue;

  public void testUnfilled() {
    checkAll(15);
  }

  public void testJustFull() {
    checkAll(SIZE);
  }

  public void testOverFilled() {
    checkAll(500);
  }

  public void testOrder() {
    LossyStack lossyStack = new LossyStack(SIZE);

    for (int i = 1; i <= SIZE; i++) {
      queue.push(new TimeStampedCounterValue(System.currentTimeMillis(), i));
      lossyStack.push(new TimeStampedCounterValue(System.currentTimeMillis(), i));
    }

    TimeStampedCounterValue[] arrLossyStack = (TimeStampedCounterValue[]) lossyStack
        .toArray(new TimeStampedCounterValue[SIZE]);
    TimeStampedCounterValue[] arrQueue = queue.toArray(new TimeStampedCounterValue[SIZE]);

    Assert.assertEquals(arrLossyStack.length, arrQueue.length);

    for (int i = 0; i < arrLossyStack.length; i++) {
      Assert.assertEquals(arrLossyStack[i].getCounterValue(), arrQueue[i].getCounterValue());
    }
  }

  private void checkAll(int numberOfElements) {
    // check isEmpty
    Assert.assertTrue(queue.isEmtpy());

    for (int i = 1; i <= numberOfElements; i++) {
      queue.push(new TimeStampedCounterValue(System.currentTimeMillis(), i));
      // check depth
      int depth = i > SIZE ? SIZE : i;
      Assert.assertEquals(depth, queue.depth());
    }

    // check toArray
    int size = numberOfElements > SIZE ? SIZE : numberOfElements;
    TimeStampedCounterValue[] arr = queue.toArray(new TimeStampedCounterValue[size]);

    Assert.assertEquals(size, arr.length);

    // check peek
    Assert.assertEquals(numberOfElements, queue.peek().getCounterValue());

    for (int i = 0; i < size; i++) {
      Assert.assertEquals(numberOfElements - i, arr[i].getCounterValue());
      System.out.println("Got numbers " + arr[i].getCounterValue());
    }
  }

  public void testWithMultiThreaded() {
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        for (int i = 1; i <= SIZE / 2; i++) {
          queue.push(new TimeStampedCounterValue(System.currentTimeMillis(), i));
        }
      }
    };

    Thread t1 = new Thread(runnable);
    t1.start();
    for (int i = 1; i <= SIZE / 2; i++) {
      queue.push(new TimeStampedCounterValue(System.currentTimeMillis(), i));
    }

    try {
      t1.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    Assert.assertEquals(SIZE, queue.depth());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    queue = new CircularLossyQueue(SIZE);
  }
}
