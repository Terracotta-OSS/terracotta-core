/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.l2.ha;

import org.junit.Assert;

import com.tc.test.TCTestCase;


public class ServerUptimeWeightGeneratorTest extends TCTestCase {
  public void testMonotonicTime() throws Exception {
    // Since we don't know the precision of System's millisecond clock, we can only ensure that the sequence is monotonic.
    ServerUptimeWeightGenerator generator = new ServerUptimeWeightGenerator(true);
    
    long previous = generator.getWeight();
    for (int i = 0; i < 1000; ++i) {
      // The sleep is added to spread the numbers a little but it will also slow the test to take at least 2 seconds.
      Thread.sleep(2);
      long next = generator.getWeight();
      Assert.assertTrue(next == previous || next > previous);
      previous = next;
    }
  }
}
