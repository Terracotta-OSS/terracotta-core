/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.collections;

import com.tc.util.EqualityComparator;
import com.tc.util.Stringifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link CollectionComparer}that requires the collections to be in the same order.
 */
public class OrderedCollectionComparer extends CollectionComparer {

  public OrderedCollectionComparer(EqualityComparator comparator, Stringifier describer) {
    super(comparator, describer);
  }

  protected CollectionMismatch[] doComparison(Object[] collectionOne, Object[] collectionTwo) {
    List mismatches = new ArrayList();

    for (int i = 0; i < collectionOne.length; ++i) {
      Object objectOne = collectionOne[i];

      if (i >= collectionTwo.length) {
        mismatches.add(new MissingObjectCollectionMismatch(objectOne, true, i, describer()));
      } else {
        Object objectTwo = collectionTwo[i];
        boolean isEqual = isEqual(objectOne, true, objectTwo, true, i, i);

        if (!isEqual) {
          mismatches.add(new UnequalObjectCollectionMismatch(objectOne, objectTwo, true, i, i, describer()));
        }
      }
    }

    for (int i = collectionOne.length; i < collectionTwo.length; ++i) {
      mismatches.add(new MissingObjectCollectionMismatch(collectionTwo[i], false, i, describer()));
    }

    return (CollectionMismatch[]) mismatches.toArray(new CollectionMismatch[mismatches.size()]);
  }

}