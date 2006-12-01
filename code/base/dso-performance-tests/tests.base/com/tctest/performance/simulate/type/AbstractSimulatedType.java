/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.simulate.type;

import com.tc.util.Assert;

public abstract class AbstractSimulatedType implements SimulatedType {
  
  /**
   * @param percentUnique - approximate percentage of unique object values
   */
  public Object clone(int percentUnique) {
    Assert.assertTrue(percentUnique >= 0);
    Assert.assertTrue(percentUnique <= 100);
    long randomValue = Math.round(Math.floor(100 * Math.random()));
    if (percentUnique >= randomValue + 1) return cloneUnique();
    return clone();
  }
  
  public abstract Object clone();
  
  public abstract Object cloneUnique();
  
  public abstract Class getType();
}
