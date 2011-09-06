/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.util.Assert;

/**
 * A {@link Difference}that represents two object references that aren't equal. (These references cannot be
 * {@link Differenceable}s themselves, because otherwise we'd just look for <em>their</em> differences,
 */
public class BasicObjectDifference extends Difference {

  private final Object a;
  private final Object b;

  public BasicObjectDifference(DifferenceContext where, Object a, Object b) {
    super(where);

    Assert
        .eval((a != null && b != null && ((a instanceof Differenceable) && (b instanceof Differenceable) && (!a
            .getClass().equals(b))))
              || (a == null) || (b == null) || ((!(a instanceof Differenceable)) || (!(b instanceof Differenceable))));

    Assert.eval(!(a == null && b == null));
    Assert.eval((a == null) || (b == null) || (! a.getClass().equals(b.getClass())) || (!(a.equals(b))));

    this.a = a;
    this.b = b;
  }

  public Object a() {
    return this.a;
  }

  public Object b() {
    return this.b;
  }

  public String toString() {
    return where() + ": object fields differ: " + describe(a) + " vs. " + describe(b);
  }

  private String describe(Object o) {
    return where().describe(o);
  }

  public boolean equals(Object that) {
    if (!(that instanceof BasicObjectDifference)) return false;

    BasicObjectDifference basicThat = (BasicObjectDifference) that;

    return new EqualsBuilder().appendSuper(super.equals(that)).append(this.a, basicThat.a).append(this.b, basicThat.b)
        .isEquals();
  }

}