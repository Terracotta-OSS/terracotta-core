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
package com.tc.services;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class SingleThreadedTimerTest {
  private static final long INTERVAL_MILLIS = 100;

  private TestTimeSource source;
  private SingleThreadedTimer timer;


  @Before
  public void setUp() throws Exception {
    this.source = new TestTimeSource(1);
    this.timer = new SingleThreadedTimer(this.source);
    this.timer.start();
  }

  @After
  public void tearDown() throws Exception {
    this.timer.stop();
  }

  @Test
  public void testStartStop() throws Exception {
  }

  @Test
  public void testRunOneTask() throws Exception {
    long startTime = this.source.currentTimeMillis() + INTERVAL_MILLIS;
    final CountDownLatch latch = new CountDownLatch(1);
    this.timer.addDelayed(new Runnable() {
      @Override
      public void run() {
        latch.countDown();
      }}, startTime);
    // Advance, poke, and make sure it happens.
    this.source.passTime(INTERVAL_MILLIS);
    this.timer.poke();
    latch.await();
  }

  @Test
  public void testRunPeriodicTask() throws Exception {
    long startTime = this.source.currentTimeMillis() + INTERVAL_MILLIS;
    final CountDownLatch latch = new CountDownLatch(2);
    this.timer.addPeriodic(new Runnable() {
      @Override
      public void run() {
        latch.countDown();
      }}, startTime, INTERVAL_MILLIS);
    // Advance, poke, and make sure it happens.
    this.source.passTime(2 * INTERVAL_MILLIS);
    this.timer.poke();
    latch.await();
  }

  @Test
  public void cancelTask() throws Exception {
    long startTime = this.source.currentTimeMillis() + INTERVAL_MILLIS;
    final AtomicBoolean bool = new AtomicBoolean(false);
    long id = this.timer.addDelayed(new Runnable() {
      @Override
      public void run() {
        bool.set(true);
      }}, startTime);
    Assert.assertTrue(id > 0);
    boolean didCancel = this.timer.cancel(id);
    Assert.assertTrue(didCancel);
    // Advance, poke, and make sure it happens.
    this.source.passTime(INTERVAL_MILLIS);
    this.timer.poke();
    Assert.assertFalse(bool.get());
  }

  @Test
  public void cancelPeriodTaskWhileRunning() throws Exception {
    long startTime = this.source.currentTimeMillis() + INTERVAL_MILLIS;
    SelfDestructingRunnable runnable = new SelfDestructingRunnable();
    final long id = this.timer.addPeriodic(runnable, startTime, INTERVAL_MILLIS);
    Assert.assertTrue(id > 0);
    runnable.setToCancel(this.timer, id);
    // Advance, poke, and make sure it happens.
    this.source.passTime(2 * INTERVAL_MILLIS);
    this.timer.poke();
    Assert.assertTrue(1 == runnable.getCounter());
  }


  private static class SelfDestructingRunnable implements Runnable {
    private SingleThreadedTimer timer;
    private long id;
    private int counter = 0;
    
    public void setToCancel(SingleThreadedTimer timer, long id) {
      this.timer = timer;
      this.id = id;
    }
    
    public int getCounter() {
      return counter;
    }
    
    @Override
    public void run() {
      this.counter += 1;
      boolean didCancel = timer.cancel(id);
      Assert.assertTrue(didCancel);
    }
  }


  private static class TestTimeSource implements SingleThreadedTimer.TimeSource {
    private long currentTimeMillis;
    
    public TestTimeSource(long currentTimeMillis) {
      this.currentTimeMillis = currentTimeMillis;
    }
    
    public void passTime(long millis) {
      this.currentTimeMillis += millis;
    }
    
    @Override
    public long currentTimeMillis() {
      return this.currentTimeMillis;
    }
  }
}
