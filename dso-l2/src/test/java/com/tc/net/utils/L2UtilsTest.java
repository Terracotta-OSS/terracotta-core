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
