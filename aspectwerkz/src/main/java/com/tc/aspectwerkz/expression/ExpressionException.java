/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.expression;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Thrown when error in the expression.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class ExpressionException extends RuntimeException {
  /**
   * Original exception which caused this exception.
   */
  private Throwable m_originalException;

  /**
   * Sets the message for the exception.
   *
   * @param message the message
   */
  public ExpressionException(final String message) {
    super(message);
  }

  /**
   * Sets the message for the exception and the original exception being wrapped.
   *
   * @param message   the detail of the error message
   * @param throwable the original exception
   */
  public ExpressionException(final String message, final Throwable throwable) {
    super(message);
    m_originalException = throwable;
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
  public void printStackTrace(final PrintStream ps) {
    super.printStackTrace(ps);
    if (m_originalException != null) {
      m_originalException.printStackTrace(ps);
    }
  }

  /**
   * Print the full stack trace, including the original exception.
   *
   * @param pw the character stream in which to print the stack trace
   */
  public void printStackTrace(final PrintWriter pw) {
    super.printStackTrace(pw);
    if (m_originalException != null) {
      m_originalException.printStackTrace(pw);
    }
  }
}
