/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import com.tc.util.Assert;

/**
 * Represents a difference between two objects somewhere in their object graphs.
 */
public abstract class Difference {

  private final DifferenceContext where;

  public Difference(DifferenceContext where) {
    Assert.assertNotNull(where);
    this.where = where;
  }

  public DifferenceContext where() {
    return this.where;
  }

  public abstract Object a();
  public abstract Object b();
  @Override
  public abstract String toString();
  
  @Override
  public boolean equals(Object that) {
    if (! (that instanceof Difference)) return false;
    
    Difference diffThat = (Difference) that;
    
    return this.where.rawEquals(diffThat.where);
  }

}