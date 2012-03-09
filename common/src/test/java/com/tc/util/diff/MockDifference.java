/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  @Override
  public Object a() {
    return this.a;
  }

  @Override
  public Object b() {
    return this.b;
  }

  @Override
  public String toString() {
    return "<MockDifference: " + a() + ", " + b() + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((a == null) ? 0 : a.hashCode());
    result = prime * result + ((b == null) ? 0 : b.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object that) {
    if (!(that instanceof MockDifference)) return false;

    MockDifference mockThat = (MockDifference) that;

    return new EqualsBuilder().appendSuper(super.equals(that)).append(this.a, mockThat.a).append(this.b, mockThat.b)
        .isEquals();
  }

}