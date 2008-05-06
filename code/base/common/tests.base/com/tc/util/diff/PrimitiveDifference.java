/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.util.Assert;

/**
 * A {@link Difference}representing two primitive fields that aren't equal.
 */
public class PrimitiveDifference extends Difference {

  private final Object a;
  private final Object b;

  public PrimitiveDifference(DifferenceContext where, boolean a, boolean b) {
    super(where);
    Assert.eval(a != b);

    this.a = a ? Boolean.TRUE : Boolean.FALSE;
    this.b = b ? Boolean.TRUE : Boolean.FALSE;
  }

  public PrimitiveDifference(DifferenceContext where, byte a, byte b) {
    super(where);
    Assert.eval(a != b);

    this.a = new Byte(a);
    this.b = new Byte(b);
  }

  public PrimitiveDifference(DifferenceContext where, char a, char b) {
    super(where);
    Assert.eval(a != b);

    this.a = new Character(a);
    this.b = new Character(b);
  }

  public PrimitiveDifference(DifferenceContext where, short a, short b) {
    super(where);
    Assert.eval(a != b);

    this.a = new Short(a);
    this.b = new Short(b);
  }

  public PrimitiveDifference(DifferenceContext where, int a, int b) {
    super(where);
    Assert.eval(a != b);

    this.a = new Integer(a);
    this.b = new Integer(b);
  }

  public PrimitiveDifference(DifferenceContext where, long a, long b) {
    super(where);
    Assert.eval(a != b);

    this.a = new Long(a);
    this.b = new Long(b);
  }

  public PrimitiveDifference(DifferenceContext where, float a, float b) {
    super(where);
    Assert.eval(a != b);

    this.a = new Float(a);
    this.b = new Float(b);
  }

  public PrimitiveDifference(DifferenceContext where, double a, double b) {
    super(where);
    Assert.eval(a != b);

    this.a = new Double(a);
    this.b = new Double(b);
  }

  public Object a() {
    return this.a;
  }

  public Object b() {
    return this.b;
  }

  public String toString() {
    return where() + ": primitive fields of type " + ClassUtils.getShortClassName(this.a.getClass()) + " differ: "
           + this.a + " vs. " + this.b;
  }

  public boolean equals(Object that) {
    if (!(that instanceof PrimitiveDifference)) return false;

    PrimitiveDifference primThat = (PrimitiveDifference) that;

    return new EqualsBuilder().appendSuper(super.equals(that)).append(this.a, primThat.a).append(this.b, primThat.b)
        .isEquals();
  }

}