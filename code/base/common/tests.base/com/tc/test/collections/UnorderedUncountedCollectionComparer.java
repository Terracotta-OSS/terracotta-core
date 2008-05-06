/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.collections;

import com.tc.util.EqualityComparator;
import com.tc.util.Stringifier;

import java.util.List;

/**
 * An {@link UnorderedCollectionComparer}that further ignores whether the two collections have different numbers of
 * instances of the same object, as long as both collections have at least one of that object. (In other words, [ 'A',
 * 'A', 'B', 'C' ] compares equal to [ 'A', 'B', 'C', 'C' ], but not [ 'A', 'A', 'C' ].)
 */
public class UnorderedUncountedCollectionComparer extends UnorderedCollectionComparer {

  public UnorderedUncountedCollectionComparer(EqualityComparator comparator, Stringifier describer) {
    super(comparator, describer);
  }

  protected void mismatchedNumbers(Object[] collectionOne, List mismatches, int i, int numberInOne, int numberInTwo) {
    // Nothing to do here.
  }
  
}