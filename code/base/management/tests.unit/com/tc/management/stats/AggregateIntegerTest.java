/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.stats;

import java.util.Date;

import com.tc.test.TCTestCase;

public class AggregateIntegerTest extends TCTestCase {

  private AggregateInteger stat;

  public AggregateIntegerTest() {
    disableTestUntil("testGetSampleRate", new Date(Long.MAX_VALUE));
  }

  protected void setUp() throws Exception {
    stat = new AggregateInteger("Testing statistic");
    super.setUp();
  }

  protected void tearDown() throws Exception {
    stat = null;
    super.tearDown();
  }

  public void testGetSampleRate() throws InterruptedException {
    final String failureExplanation = "Since this is a timing test it is likely that it might fail from time to time on a loaded machine";
    final long SECOND = 1000;
    final long HALF_SECOND = SECOND / 2;
    final long QUARTER_SECOND = SECOND / 4;
    stat = new AggregateInteger("Testing statistic with history", 10);
    populate(new int[] { -100, -50, -10, -1, 0, 1, 10, 50, 100, 1000 });
    populate(new int[] { -100, -50, -10, -1, 0, 1, 10, 50, 100, 1000 });
    Thread.sleep(900);
    assertTrue(failureExplanation, stat.getSampleRate(QUARTER_SECOND) >= 2);
    assertTrue(failureExplanation, stat.getSampleRate(HALF_SECOND) >= 5);
    assertTrue(failureExplanation, stat.getSampleRate(SECOND) >= 10);
  }

  public void testGetName() {
    assertEquals("Testing statistic", stat.getName());
  }

  public void testGetMaximum() {
    populate(new int[] { -100, -50, -10, -1, 0, 1, 10, 50, 100 });
    assertEquals(100, stat.getMaximum());
  }

  public void testGetMinimum() {
    populate(new int[] { -100, -50, -10, -1, 0, 1, 10, 50, 100 });
    assertEquals(-100, stat.getMinimum());
  }

  public void testGetN() {
    populate(new int[] { -100, -50, -10, -1, 0, 1, 10, 50, 100 });
    assertEquals(9, stat.getN());
  }

  public void testGetSum() {
    populate(new int[] { -100, -50, -10, -1, 0, 1, 10, 50, 100 });
    assertEquals(0, stat.getSum());
    stat.addSample(100);
    assertEquals(100, stat.getSum());
    stat.addSample(1000);
    assertEquals(1100, stat.getSum());
  }

  public void testGetAverage() {
    populate(new int[] { -100, -50, -10, -1, 0, 1, 10, 50, 100 });
    assertEquals(0, (int) stat.getAverage());
    stat.addSample(10);
    assertEquals(1, (int) stat.getAverage());
    stat.reset();
    assertEquals(0, (int) stat.getAverage());
    stat.addSample(10);
    stat.addSample(20);
    assertEquals(15, (int) stat.getAverage());
    stat.addSample(0);
    assertEquals(10, (int) stat.getAverage());
  }

  private void populate(final int[] samples) {
    for (int pos = 0; pos < samples.length; ++pos)
      stat.addSample(samples[pos]);
  }

}
