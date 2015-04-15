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
package com.tc.util;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link SameObjectEqualityComparator}.
 */
public class SameObjectEqualityComparatorTest extends TCTestCase {
  
  public void testEquals() {
    SameObjectEqualityComparator comparator = SameObjectEqualityComparator.INSTANCE;
    
    assertTrue(comparator.isEquals(null, null));
    assertFalse(comparator.isEquals(new Integer(4), null));
    assertFalse(comparator.isEquals(null, new Integer(4)));
    
    Integer x = new Integer(5);
    Integer y = new Integer(5);
    
    assertTrue(comparator.isEquals(x, x));
    assertFalse(comparator.isEquals(x, y));
    assertFalse(comparator.isEquals(y, x));
    assertTrue(comparator.isEquals(y, y));
  }

}
