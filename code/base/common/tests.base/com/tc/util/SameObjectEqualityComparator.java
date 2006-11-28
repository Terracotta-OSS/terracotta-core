/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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