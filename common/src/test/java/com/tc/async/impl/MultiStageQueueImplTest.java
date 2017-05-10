/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.async.impl;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.logging.DefaultLoggerProvider;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.QueueFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author mscott
 */
public class MultiStageQueueImplTest {


  public MultiStageQueueImplTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getSource method, of class StageQueueImpl.
   */
  @Test
  public void testBasicMultiContext() {
    System.out.println("multi context");
    int index = 0;
    int size = 4;
    TCLoggerProvider logger = new DefaultLoggerProvider();
    final List<BlockingQueue<Object>> cxts = new ArrayList<BlockingQueue<Object>>();

    QueueFactory<ContextWrapper<Object>> context = mock(QueueFactory.class);
    when(context.createInstance(Matchers.anyInt())).thenAnswer(new Answer<BlockingQueue<Object>>() {

      @Override
      public BlockingQueue<Object> answer(InvocationOnMock invocation) throws Throwable {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>((Integer) invocation.getArguments()[0]);
        cxts.add(queue);
        return queue;
      }

    });
    StageQueue<Object> instance = new MultiStageQueueImpl(size, context, logger, "mock", 16);
    for (int x = 0; x < cxts.size(); x++) {
      assertNotNull(instance.getSource(index));
    }
    assertNull(instance.getSource(cxts.size()));
    assertEquals(cxts.size(), size);

    MultiThreadedEventContext context1 = mock(MultiThreadedEventContext.class);
    when(context1.getSchedulingKey()).thenReturn(null);
    System.out.println("test add");
    instance.addMultiThreaded(context1);
    boolean found = false;
    for (Queue<Object> q : cxts) {
      if (q.poll() != null) {
        found = true;
      }
    }
    assertTrue(found);
    System.out.println("test even distribution with no key");
    for (int x = 0; x < size * 2; x++) {
      instance.addMultiThreaded(context1);
    }
    for (Queue<Object> q : cxts) {
      assertThat(q.size(), org.hamcrest.Matchers.lessThanOrEqualTo(2));
      q.clear();
    }

    System.out.println("test specific queue");
    when(context1.getSchedulingKey()).thenReturn(1);
    instance.addMultiThreaded(context1);
    //  int should hash to int
    for (int x = 0; x < cxts.size(); x++) {
      if (x != 1) {
        assertTrue(cxts.get(x).isEmpty());
      } else {
        assertEquals(cxts.get(x).poll(), context1);
      }
    }

    int rand = (int) (Math.random() * Integer.MAX_VALUE);
    when(context1.getSchedulingKey()).thenReturn(rand);
    instance.addMultiThreaded(context1);
    //  tests specific implementation.  test expectation
    assertEquals(cxts.get(rand % cxts.size()).poll(), context1);
  }

  @Test
  public void testShortestRollover() throws Exception {
    TCLoggerProvider logger = new DefaultLoggerProvider();
    final List<BlockingQueue<Object>> cxts = new ArrayList<BlockingQueue<Object>>();

    QueueFactory<ContextWrapper<Object>> context = mock(QueueFactory.class);
    when(context.createInstance(Matchers.anyInt())).thenAnswer(new Answer<BlockingQueue<Object>>() {

      @Override
      public BlockingQueue<Object> answer(InvocationOnMock invocation) throws Throwable {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>((Integer) invocation.getArguments()[0]);
        cxts.add(queue);
        return queue;
      }

    });
    System.setProperty(MultiStageQueueImpl.FINDSTRATEGY_PROPNAME, MultiStageQueueImpl.ShortestFindStrategy.BRUTE.name());
    StageQueue impl = new MultiStageQueueImpl(6, context, logger, "mock", 16);
    MultiThreadedEventContext cxt = mock(MultiThreadedEventContext.class);
    when(cxt.getSchedulingKey()).thenReturn(null);
    // fcheck starts at zero and should stay at zero because first queue is always empty
    for (int x = 0; x < 6; x++) {
      Assert.assertTrue(impl.getSource(0).isEmpty());
      impl.addMultiThreaded(cxt);
      Assert.assertNotNull(impl.getSource(0).poll(0));
    }
    //  now try and fill one each on the the queues
    for (int x = 0; x < 6; x++) {
      Assert.assertTrue(impl.getSource(x).isEmpty());
      impl.addMultiThreaded(cxt);
      Assert.assertFalse(cxts.get(x).isEmpty());
    }
    //  now clear the last three and re-fill them
    for (int x = 3; x < 6; x++) {
      Assert.assertFalse(impl.getSource(x).isEmpty());
      Assert.assertNotNull(impl.getSource(x).poll(0));
      Assert.assertTrue(cxts.get(x).isEmpty());
      impl.addMultiThreaded(cxt);
      Assert.assertFalse(cxts.get(x).isEmpty());
    }
    //  now clear all again
    for (int x = 0; x < 6; x++) {
      Assert.assertFalse(impl.getSource(x).isEmpty());
      Assert.assertNotNull(impl.getSource(x).poll(0));
      Assert.assertTrue(cxts.get(x).isEmpty());
    }
    // now add one more and make sure it is at the last queue since that was the
    // last to be cleared
    impl.addMultiThreaded(cxt);
    Assert.assertFalse(cxts.get(5).isEmpty());
  }

  @Test
  @Ignore
  public void testThroughput8() throws InterruptedException {
    syntheticThroughput(5 * 60, 8, Integer.MAX_VALUE);
  }

  public void syntheticThroughput(int secondsToRun, final int qCount, int qSize) throws InterruptedException {
    TCLoggerProvider logger = new DefaultLoggerProvider();
    QueueFactory<ContextWrapper<MultiThreadedEventContext>> qFactory = new
      QueueFactory<ContextWrapper<MultiThreadedEventContext>>();
    final StageQueue<MultiThreadedEventContext> impl = new MultiStageQueueImpl<MultiThreadedEventContext>(qCount,
                                                                                  qFactory,
                                                                                  logger,
                                                                                  "perf",
                                                                                  qSize);

    AtomicBoolean die = new AtomicBoolean(false);
    final ArrayList<DelayingThread> suppliers = new ArrayList<DelayingThread>(4);
    final MultiThreadedEventContext incoming = new MultiThreadedEventContext() {
      @Override
      public Object getSchedulingKey() {
        return null;
      }

      @Override
      public boolean flush() {
        return false;
      }
    };
    impl.enableStatsCollection(true);
    final long[] incomingCount = new long[1];
    final Random r = new Random(0);
    DelayingThread supplier = DelayingThread.createDelayViaSleepThread(die, "Supplier", new Runnable() {
      @Override
      public void run() {
        incomingCount[0]++;
        impl.addMultiThreaded(incoming);
      }
    }, 1, TimeUnit.MILLISECONDS);
    supplier.start();

    final long[] consumerCount = new long[qCount];
    ArrayList<Thread> consumers=new ArrayList<Thread>();
    for(int i=0;i<qCount;i++) {
      final int finalI = i;
      final Random r2=new Random(i);
      DelayingThread consumer = DelayingThread.createDelayViaSleepThread(die, "Consumer", new Runnable() {
        @Override
        public void run() {
          try {
            Object got = impl.getSource(finalI).poll(0);
            if (got != null) {
              //System.out.println("Consumer: " + finalI + " " + got);
              consumerCount[finalI]++;
            }
            Thread.sleep(r2.nextInt(60));
          } catch (InterruptedException e) {
          }
        }
      }, 0, TimeUnit.MILLISECONDS);
      consumers.add(consumer);
    }
    for(Thread t:consumers) {
      t.start();
    }
    DelayingThread.createDelayViaSleepThread(die, "size", new Runnable() {
                                               @Override
                                               public void run() {
                                                 System.out.println(impl.getStats(1000).getDetails());
                                               }
                                             }, 10, TimeUnit.SECONDS).start();

    Thread.sleep(secondsToRun * 1000);
    die.set(true);
    impl.setClosed(true);
    impl.clear();

    System.out.println(consumerCount[0] +" events, "+(consumerCount[0] /secondsToRun)+"/sec");



  }

  static class DelayingThread extends Thread {
    private final AtomicBoolean die;
    private final Runnable task;
    private final boolean working;
    private volatile long delayNS;
    private volatile long delayMS;
    private double seed = Double.MAX_VALUE;

    private DelayingThread(boolean working, AtomicBoolean die, String name, Runnable task, long delay, TimeUnit unit) {
      super(name);
      this.die = die;
      this.task = task;

      setDelay(delay, unit);
      this.working = working;
    }

    public static DelayingThread createDelayViaSleepThread(AtomicBoolean die,
                                                           String name,
                                                           Runnable task,
                                                           long delay,
                                                           TimeUnit unit) {
      return new DelayingThread(false, die, name, task, delay, unit);
    }

    public static DelayingThread createDelayViaWorkThread(AtomicBoolean die,
                                                          String name,
                                                          Runnable task,
                                                          long delay,
                                                          TimeUnit unit) {
      return new DelayingThread(true, die, name, task, delay, unit);
    }

    public void setDelay(long delay, TimeUnit unit) {
      this.delayNS = TimeUnit.NANOSECONDS.convert(delay, unit);
      this.delayMS = TimeUnit.MILLISECONDS.convert(delay, unit);
    }

    public void incrementDelay(long delay, TimeUnit unit) {
      this.delayNS = delayNS + TimeUnit.NANOSECONDS.convert(delay, unit);
      this.delayMS = delayMS + TimeUnit.MILLISECONDS.convert(delay, unit);
    }

    @Override
    public void run() {
      while (!die.get()) {
        if(delayMS>0) {
          if (working) {
            delayWorking();
          } else {
            delaySleeping();
          }
        }
        try {
          task.run();
        } catch (Throwable t) {
        }
      }
    }

    private void delayWorking() {
      long until = System.nanoTime() + delayNS;
      do {
        seed = Math.sqrt(seed);
        if (seed < 10.0d) {
          seed = Double.MAX_VALUE;
        }
      } while (System.nanoTime() < until);
    }

    private void delaySleeping() {
      try {
        Thread.sleep(delayMS);
      } catch (InterruptedException e) {
      }
    }
  }

}
