/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

  public String toString() {
    return "Unequal number of objects: " + originatingString() + " occurs " + this.numInCollectionOne + " times "
           + "in collection one, but " + this.numInCollectionTwo + " times in collection two.";
  }

  public boolean equals(Object that) {
    if (!(that instanceof UnequalObjectCountCollectionMismatch)) return false;

    UnequalObjectCountCollectionMismatch misThat = (UnequalObjectCountCollectionMismatch) that;

    return new EqualsBuilder().appendSuper(super.equals(that)).append(this.numInCollectionOne,
                                                                      misThat.numInCollectionOne)
        .append(this.numInCollectionTwo, misThat.numInCollectionTwo).isEquals();
  }

}