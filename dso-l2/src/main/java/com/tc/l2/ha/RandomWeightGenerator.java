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

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;


public class RandomWeightGenerator implements WeightGenerator {
  private final SecureRandom generator;

  public RandomWeightGenerator(SecureRandom generator) {
    this.generator = generator;
  }

  @Override
  public long getWeight() {
    return this.generator.nextLong();
  }
  
  /**
   * A helper used only in tests (and kept here since it is used in a selection of different tests) which creates a generator
   * factory, populated only with random weight generators.
   * 
   * @param generatorsToUse The number of random weight generator instances to add to the factory.
   * @return A generator which will produce generatorsToUse random weights.
   */
  public static WeightGeneratorFactory createTestingFactory(int generatorsToUse) {
    WeightGeneratorFactory wgf = new WeightGeneratorFactory();
    for (int i = 0; i < generatorsToUse; ++i) {
      wgf.add(new RandomWeightGenerator(new SecureRandom()));
    }
    return wgf;
  }
}
