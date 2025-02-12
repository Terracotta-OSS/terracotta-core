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

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;


public class RandomWeightGenerator implements WeightGenerator {
  private final long randomNumber;

  public RandomWeightGenerator(SecureRandom generator, boolean isAvailable) {
    this.randomNumber = generator.nextLong();
  }

  @Override
  public long getWeight() {
    return this.randomNumber;
  }
  
  @Override
  public boolean isVerificationWeight() {
    return false;
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
      wgf.add(new RandomWeightGenerator(new SecureRandom(), true));
    }
    return wgf.complete();
  }
}
