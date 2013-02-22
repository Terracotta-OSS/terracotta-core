/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.net.utils;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Shelestovich
 */
public class L2UtilsTest {

  @Test
  public void testShouldCorrectlyCalculateOptimalThreadsCount() {
    assertEquals(6, L2Utils.calculateOptimalThreadsCount(4, 30, 70, BigDecimal.ONE));
    assertEquals(20, L2Utils.calculateOptimalThreadsCount(2, 90, 10, BigDecimal.ONE));
    assertEquals(1, L2Utils.calculateOptimalThreadsCount(1, 30, 70, BigDecimal.ONE));
    assertEquals(2, L2Utils.calculateOptimalThreadsCount(1, 0, 100, BigDecimal.ONE));
    assertEquals(22, L2Utils.calculateOptimalThreadsCount(16, 30, 70, BigDecimal.ONE));
    assertEquals(112, L2Utils.calculateOptimalThreadsCount(80, 30, 70, BigDecimal.ONE));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShouldThrowIAEOnInvalidUtilization() {
    L2Utils.calculateOptimalThreadsCount(4, 30, 70, BigDecimal.ZERO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShouldThrowIAEOnInvalidCpusCount() {
    L2Utils.calculateOptimalThreadsCount(0, 30, 70, BigDecimal.ONE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testShouldThrowIAEOnInvalidCompute() {
    L2Utils.calculateOptimalThreadsCount(4, 30, 0, BigDecimal.ONE);
  }
}
