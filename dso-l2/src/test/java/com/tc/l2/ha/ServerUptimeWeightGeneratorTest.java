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
package com.tc.l2.ha;

import org.junit.Assert;

import com.tc.test.TCTestCase;


public class ServerUptimeWeightGeneratorTest extends TCTestCase {
  public void testMonotonicTime() throws Exception {
    // Since we don't know the precision of System's millisecond clock, we can only ensure that the sequence is monotonic.
    ServerUptimeWeightGenerator generator = new ServerUptimeWeightGenerator();
    
    long previous = generator.getWeight();
    for (int i = 0; i < 1000; ++i) {
      // The sleep is added to spread the numbers a little but it will also slow the test to take at least 2 seconds.
      Thread.sleep(2);
      long next = generator.getWeight();
      Assert.assertTrue(next >= previous);
      previous = next;
    }
  }
}
