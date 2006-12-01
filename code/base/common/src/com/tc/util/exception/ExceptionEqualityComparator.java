/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.exception;

/**
 * An object that knows how to compare two exceptions for equality.
 */
public interface ExceptionEqualityComparator {

  /**
   * Indicates whether two exceptions are equal. Note that one or both of the exceptions may be <code>null</code>; a
   * <code>null</code> exception object should never be considered equal to any other exception object, except for a
   * <code>null</code> exception object.
   */
  boolean exceptionsEqual(Throwable exceptionOne, Throwable exceptionTwo);

}
