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

  @Override
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