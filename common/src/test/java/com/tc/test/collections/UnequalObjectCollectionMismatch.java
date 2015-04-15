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

  @Override
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