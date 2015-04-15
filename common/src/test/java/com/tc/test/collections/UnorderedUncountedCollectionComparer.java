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

  @Override
  protected void mismatchedNumbers(Object[] collectionOne, List mismatches, int i, int numberInOne, int numberInTwo) {
    // Nothing to do here.
  }
  
}