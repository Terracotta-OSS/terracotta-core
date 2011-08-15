/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;


/**
 * An {@link EqualityComparator} that compares objects with {@link Object#equals(Object)}.
 */
public class EqualsEqualityComparator implements EqualityComparator {
  
  public static final EqualsEqualityComparator INSTANCE = new EqualsEqualityComparator();
  
  private EqualsEqualityComparator() {
    // Use INSTANCE instead.
  }

  public boolean isEquals(Object one, Object two) {
    if ((one == null) != (two == null)) return false;
    if (one == null) return true;
    
    return one.equals(two);
  }

}
