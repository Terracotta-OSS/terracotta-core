/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.collections;

import com.tc.util.Stringifier;
import com.tc.util.diff.DifferenceBuilder;
import com.tc.util.diff.Differenceable;

/**
 * A {@link CollectionMismatch}that is used when two objects aren't equal to each other.
 */
class UnequalObjectCollectionMismatch extends CollectionMismatch {

  public UnequalObjectCollectionMismatch(Object originating, Object comparedAgainst,
                                         boolean originatingIsInCollectionOne, int originatingIndex,
                                         int comparedAgainstIndex, Stringifier describer) {
    super(originating, comparedAgainst, originatingIsInCollectionOne, originatingIndex, comparedAgainstIndex, describer);
  }

  public String toString() {
    if (originating() != null && comparedAgainst() != null && (originating() instanceof Differenceable)
        && (comparedAgainst() instanceof Differenceable)) {
      // formatting
      return "Unequal objects: differences are: "
             + DifferenceBuilder.describeDifferences((Differenceable) originating(),
                                                     (Differenceable) comparedAgainst(), describer()) + "\n"
             + originatingString() + "\nis not equal to\n" + comparedAgainstString() + "\n";
    }

    return "Unequal objects: " + originatingString() + " is not equal to " + comparedAgainstString();
  }
  
}