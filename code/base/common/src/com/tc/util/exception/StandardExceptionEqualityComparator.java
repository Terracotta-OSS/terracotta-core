/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.exception;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * The standard implementation of {@link ExceptionEqualityComparator}.
 */
public class StandardExceptionEqualityComparator implements ExceptionEqualityComparator {

  private static final StandardExceptionEqualityComparator INSTANCE = new StandardExceptionEqualityComparator();

  private StandardExceptionEqualityComparator() {
    // Nothing here yet.
  }

  public static StandardExceptionEqualityComparator getInstance() {
    return INSTANCE;
  }

  public boolean exceptionsEqual(Throwable exceptionOne, Throwable exceptionTwo) {
    EqualsBuilder builder = new EqualsBuilder();

    builder.append(exceptionOne == null, exceptionTwo == null);

    if (exceptionOne != null && exceptionTwo != null) {
      builder.append(exceptionOne.getClass(), exceptionTwo.getClass());
      builder.append(exceptionOne.getMessage(), exceptionTwo.getMessage());
      builder.append(exceptionOne.getStackTrace(), exceptionTwo.getStackTrace());
    }

    return builder.isEquals();
  }

}
