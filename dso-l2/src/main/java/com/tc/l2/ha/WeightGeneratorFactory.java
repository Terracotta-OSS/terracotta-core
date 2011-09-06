/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.util.Assert;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class WeightGeneratorFactory {
  
  private final List generators = new ArrayList();
  
  public static final WeightGenerator RANDOM_WEIGHT_GENERATOR = new WeightGenerator() {
    public long getWeight() {
      SecureRandom r = new SecureRandom();
      return r.nextLong();
    }
  };
  
  public WeightGeneratorFactory() {
    super();
  }
  
  public synchronized void add(WeightGenerator g) {
    Assert.assertNotNull(g);
    generators.add(g);
  }
  
  public synchronized void remove(WeightGenerator g) {
    Assert.assertNotNull(g);
    generators.remove(g);
  }
  
  public synchronized long[] generateWeightSequence() {
    long weights[] = new long[generators.size()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = ((WeightGenerator) generators.get(i)).getWeight();
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
  
  public static WeightGeneratorFactory createDefaultFactory() {
    WeightGeneratorFactory wgf = new WeightGeneratorFactory();
    wgf.add(RANDOM_WEIGHT_GENERATOR);
    wgf.add(RANDOM_WEIGHT_GENERATOR);
    return wgf;
  }

  public static interface WeightGenerator {
    public long getWeight();
  }

}
