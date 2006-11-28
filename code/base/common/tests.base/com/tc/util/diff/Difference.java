/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
  public abstract String toString();
  
  public boolean equals(Object that) {
    if (! (that instanceof Difference)) return false;
    
    Difference diffThat = (Difference) that;
    
    return this.where.rawEquals(diffThat.where);
  }

}