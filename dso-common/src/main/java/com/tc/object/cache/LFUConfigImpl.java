/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.properties.TCProperties;
import com.tc.util.Assert;

public class LFUConfigImpl implements LFUConfig {

  private final float agingFactor;
  private final int   ignorePercentage;

  public LFUConfigImpl(TCProperties lfuProperties) {
    agingFactor = lfuProperties.getFloat("agingFactor");
    Assert.assertTrue("Invalid agingFactor in properties file", agingFactor >= 0.0 && agingFactor <= 1.0);
    ignorePercentage = lfuProperties.getInt("recentlyAccessedIgnorePercentage");
    Assert.assertTrue("Invalid recentlyAccessedIgnorePercentage in properties file", ignorePercentage >= 0
                                                                                     && ignorePercentage <= 100);
  }

  public float getAgingFactor() {
    return agingFactor;
  }

  public int getRecentlyAccessedIgnorePercentage() {
    return ignorePercentage;
  }

}
