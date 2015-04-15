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