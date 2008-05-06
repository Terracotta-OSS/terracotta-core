/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

/**
 * An {@link EqualityComparator}that tells if objects are the same actual object.
 */
public class SameObjectEqualityComparator implements EqualityComparator {

  public static final SameObjectEqualityComparator INSTANCE = new SameObjectEqualityComparator();

  private SameObjectEqualityComparator() {
    // Use INSTANCE instead.
  }

  public boolean isEquals(Object one, Object two) {
    return one == two;
  }

}