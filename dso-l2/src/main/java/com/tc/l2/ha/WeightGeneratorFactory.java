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

import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.List;


public class WeightGeneratorFactory {
  private final List<WeightGenerator> generators = new ArrayList<>();

  public synchronized void add(WeightGenerator g) {
    Assert.assertNotNull(g);
    generators.add(g);
  }
  
  public synchronized long[] generateWeightSequence() {
    long weights[] = new long[generators.size()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = generators.get(i).getWeight();
    }
    return weights;
  }
  
  public synchronized long[] generateMaxWeightSequence() {
    long weights[] = new long[generators.size()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = Long.MAX_VALUE;
    }
    return weights;
  }

  /**
   * The interface of any weight generator which wants to be registered in this factory.
   */
  public static interface WeightGenerator {
    public long getWeight();
  }
}
