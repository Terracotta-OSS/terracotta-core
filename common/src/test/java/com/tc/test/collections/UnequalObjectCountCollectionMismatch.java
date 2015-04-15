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
package com.tc.test.collections;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.util.Stringifier;

/**
 * A {@link CollectionMismatch}that is used when two collections don't contain the same number of instances of two
 * objects.
 */
class UnequalObjectCountCollectionMismatch extends CollectionMismatch {

  private final int numInCollectionOne;
  private final int numInCollectionTwo;

  public UnequalObjectCountCollectionMismatch(Object theObject, int objectIndexInCollectionOne, int numInCollectionOne,
                                              int numInCollectionTwo, Stringifier describer) {
    super(theObject, null, true, objectIndexInCollectionOne, -1, describer);

    this.numInCollectionOne = numInCollectionOne;
    this.numInCollectionTwo = numInCollectionTwo;
  }

  @Override
  public String toString() {
    return "Unequal number of objects: " + originatingString() + " occurs " + this.numInCollectionOne + " times "
           + "in collection one, but " + this.numInCollectionTwo + " times in collection two.";
  }

  @Override
  public boolean equals(Object that) {
    if (!(that instanceof UnequalObjectCountCollectionMismatch)) return false;

    UnequalObjectCountCollectionMismatch misThat = (UnequalObjectCountCollectionMismatch) that;

    return new EqualsBuilder().appendSuper(super.equals(that)).append(this.numInCollectionOne,
                                                                      misThat.numInCollectionOne)
        .append(this.numInCollectionTwo, misThat.numInCollectionTwo).isEquals();
  }

}