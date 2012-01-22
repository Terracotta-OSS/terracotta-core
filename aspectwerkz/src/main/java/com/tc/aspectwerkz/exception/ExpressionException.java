/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.exception;

/**
 * Thrown when error in expression AST evaluation or creation.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class ExpressionException extends RuntimeException {
  /**
   * Sets the message for the exception.
   *
   * @param message the message
   */
  public ExpressionException(final String message) {
    super(message);
  }
}
