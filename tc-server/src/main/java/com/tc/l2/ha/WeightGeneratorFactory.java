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

import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WeightGeneratorFactory {
  private final List<WeightGenerator> generators;
  private final boolean complete;

  public WeightGeneratorFactory() {
    generators = new ArrayList<>();
    complete = false;
  }

  private WeightGeneratorFactory(List<WeightGenerator> generators) {
    this.generators = Collections.unmodifiableList(generators);
    this.complete = true;
  }

  public void add(WeightGenerator g) {
    Assert.assertNotNull(g);
    generators.add(g);
  }
  
  public WeightGeneratorFactory complete() {
    return new WeightGeneratorFactory(generators);
  }
  
  public boolean isComplete() {
    return complete;
  }
  
  public long[] generateWeightSequence() {
    Assert.assertTrue(isComplete());
    long weights[] = new long[generators.size()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = generators.get(i).getWeight();
    }
    return weights;
  }
  
  public long[] generateMaxWeightSequence() {
    Assert.assertTrue(isComplete());
    long weights[] = new long[generators.size()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = Long.MAX_VALUE;
    }
    return weights;
  }
  
  public long[] generateVerificationSequence() {
    Assert.assertTrue(isComplete());
    long weights[] = new long[generators.size()];
    for (int i=0;i<generators.size();i++) {
      WeightGenerator gen = generators.get(i);
      // generation weight generator is for information sharing only
      if (gen.isVerificationWeight()) {
        weights[i] = gen.getWeight();
      } else {
        weights[i] = Long.MAX_VALUE;
      }
    }
    return weights;
  }
  
  public int size() {
    Assert.assertTrue(isComplete());
    return generators.size();
  }

  /**
   * The interface of any weight generator which wants to be registered in this factory.
   */
  public static interface WeightGenerator {
    public long getWeight();
    default boolean isVerificationWeight() {
      return false;
    }
  }
}
