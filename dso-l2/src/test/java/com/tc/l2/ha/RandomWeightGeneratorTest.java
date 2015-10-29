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

import java.security.SecureRandom;

import org.junit.Assert;

import com.tc.test.TCTestCase;


public class RandomWeightGeneratorTest extends TCTestCase {
  public void testUnlikelyCollision() throws Exception {
    // Assume that 3 different weights will each be unique.  While it is possible for this to collide, the odds against are
    // overwhelming and there seems to be no way to force deterministic values from SecureRandom, even with a seed.
    
    SecureRandom random = new SecureRandom();
    RandomWeightGenerator generator = new RandomWeightGenerator(random);
    long weight1 = generator.getWeight();
    long weight2 = generator.getWeight();
    long weight3 = generator.getWeight();
    Assert.assertTrue(weight1 != weight2);
    Assert.assertTrue(weight1 != weight3);
    Assert.assertTrue(weight2 != weight3);
  }
}
