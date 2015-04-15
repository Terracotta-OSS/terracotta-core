/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
    return where() + ": primitive fields of type " + ClassUtils.getShortClassName(this.a.getClass()) + " differ: "
           + this.a + " vs. " + this.b;
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
    if (!(that instanceof PrimitiveDifference)) return false;

    PrimitiveDifference primThat = (PrimitiveDifference) that;

    return new EqualsBuilder().appendSuper(super.equals(that)).append(this.a, primThat.a).append(this.b, primThat.b)
        .isEquals();
  }

}