/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * A mock {@link Difference}, for use in tests.
 */
public class MockDifference extends Difference {

  private final Object a;
  private final Object b;

  public MockDifference(DifferenceContext where, Object a, Object b) {
    super(where);

    this.a = a;
    this.b = b;
  }

  public MockDifference(DifferenceContext where) {
    this(where, new Object(), new Object());
  }

  public Object a() {
    return this.a;
  }

  public Object b() {
    return this.b;
  }

  public String toString() {
    return "<MockDifference: " + a() + ", " + b() + ">";
  }

  public boolean equals(Object that) {
    if (!(that instanceof MockDifference)) return false;

    MockDifference mockThat = (MockDifference) that;

    return new EqualsBuilder().appendSuper(super.equals(that)).append(this.a, mockThat.a).append(this.b, mockThat.b)
        .isEquals();
  }

}