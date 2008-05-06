/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

/**
 * Knows how to compare two objects for equality. This object must be able to accept <code>null</code> values for
 * either argument &mdash; <code>null</code> should never be equal to anything except itself.
 */
public interface EqualityComparator {

  boolean isEquals(Object one, Object two);

}