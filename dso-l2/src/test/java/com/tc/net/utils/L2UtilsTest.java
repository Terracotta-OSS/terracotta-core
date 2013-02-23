/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.net.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Eugene Shelestovich
 */
public class L2UtilsTest {

  @Test
  public void testShouldCorrectlyCalculateOptimalThreadsCount() {
    assertEquals(6, L2Utils.calculateOptimalThreadsCount(4, 30, 70, 1.0));
    assertEquals(20, L2Utils.calculateOptimalThreadsCount(2, 90, 10, 1.0));
    assertEquals(1, L2Utils.calculateOptimalThreadsCount(1, 30, 70, 1.0));
    assertEquals(2, L2Utils.calculateOptimalThreadsCount(1, 0, 100, 1.0));
    assertEquals(22, L2Utils.calculateOptimalThreadsCount(16, 30, 70, 1.0));
    assertEquals(112, L2Utils.calculateOptimalThreadsCount(80, 30, 70, 1.0));
    assertEquals(84, L2Utils.calculateOptimalThreadsCount(80, 30, 70, 0.75));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShouldThrowIAEOnInvalidUtilization() {
    L2Utils.calculateOptimalThreadsCount(4, 30, 70, 0.0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShouldThrowIAEOnInvalidCpusCount() {
    L2Utils.calculateOptimalThreadsCount(0, 30, 70, 1.0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShouldThrowIAEOnInvalidCompute() {
    L2Utils.calculateOptimalThreadsCount(4, 30, 0, 1.0);
  }
}
