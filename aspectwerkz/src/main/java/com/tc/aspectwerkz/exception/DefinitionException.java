/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Thrown when error in definition.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class DefinitionException extends RuntimeException {
  /**
   * Original exception which caused this exception.
   */
  private Throwable originalException;

  /**
   * Sets the message for the exception.
   *
   * @param message the message
   */
  public DefinitionException(final String message) {
    super(message);
  }

  /**
   * Sets the message for the exception and the original exception being wrapped.
   *
   * @param message   the detail of the error message
   * @param throwable the original exception
   */
  public DefinitionException(String message, Throwable throwable) {
    super(message);
    this.originalException = throwable;
  }

  /**
   * Print the full stack trace, including the original exception.
   */
  public void printStackTrace() {
    printStackTrace(System.err);
  }

  /**
   * Print the full stack trace, including the original exception.
   *
   * @param ps the byte stream in which to print the stack trace
   */
  public void printStackTrace(PrintStream ps) {
    super.printStackTrace(ps);
    if (this.originalException != null) {
      this.originalException.printStackTrace(ps);
    }
  }

  /**
   * Print the full stack trace, including the original exception.
   *
   * @param pw the character stream in which to print the stack trace
   */
  public void printStackTrace(PrintWriter pw) {
    super.printStackTrace(pw);
    if (this.originalException != null) {
      this.originalException.printStackTrace(pw);
    }
  }
}
