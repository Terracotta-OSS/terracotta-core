/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import java.security.SecureRandom;

import org.junit.Assert;

import com.tc.test.TCTestCase;


public class RandomWeightGeneratorTest extends TCTestCase {
  public void testUnlikelyCollision() throws Exception {
    // Assume that 3 different weights will each be unique.  While it is possible for this to collide, the odds against are
    // overwhelming and there seems to be no way to force deterministic values from SecureRandom, even with a seed.
    
    SecureRandom random = new SecureRandom();
    long weight1 = new RandomWeightGenerator(random, true).getWeight();
    long weight2 = new RandomWeightGenerator(random, true).getWeight();
    long weight3 = new RandomWeightGenerator(random, true).getWeight();
    Assert.assertTrue(weight1 != weight2);
    Assert.assertTrue(weight1 != weight3);
    Assert.assertTrue(weight2 != weight3);
  }
}
