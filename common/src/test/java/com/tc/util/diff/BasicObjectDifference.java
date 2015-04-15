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

    Assert.eval((a != null && b != null && ((a instanceof Differenceable) && (b instanceof Differenceable) && (!a
        .getClass().equals(b))))
                || (a == null)
                || (b == null)
                || ((!(a instanceof Differenceable)) || (!(b instanceof Differenceable))));

    Assert.eval(!(a == null && b == null));
    Assert.eval((a == null) || (b == null) || (!a.getClass().equals(b.getClass())) || (!(a.equals(b))));

    this.a = a;
    this.b = b;
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
    return where() + ": object fields differ: " + describe(a) + " vs. " + describe(b);
  }

  private String describe(Object o) {
    return where().describe(o);
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
    if (!(that instanceof BasicObjectDifference)) return false;

    BasicObjectDifference basicThat = (BasicObjectDifference) that;

    return new EqualsBuilder().appendSuper(super.equals(that)).append(this.a, basicThat.a).append(this.b, basicThat.b)
        .isEquals();
  }

}