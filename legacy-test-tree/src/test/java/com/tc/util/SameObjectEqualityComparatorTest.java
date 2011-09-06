/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
