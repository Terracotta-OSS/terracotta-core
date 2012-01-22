/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Wrappes the original throwable in a RuntimeException.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class WrappedRuntimeException extends RuntimeException {
  /**
   * The original throwable instance.
   */
  private final Throwable m_throwable;

  /**
   * The exception user provided message when the exception is wrapped
   */
  private final String m_message;

  /**
   * Creates a new WrappedRuntimeException.
   *
   * @param throwable the non-RuntimeException to be wrapped.
   */
  public WrappedRuntimeException(final Throwable throwable) {
    m_throwable = throwable;
    m_message = throwable.getMessage();
  }

  /**
   * Creates a new WrappedRuntimeException.
   *
   * @param message
   * @param throwable the non-RuntimeException to be wrapped.
   */
  public WrappedRuntimeException(final String message, final Throwable throwable) {
    m_throwable = throwable;
    m_message = message;
  }

  /**
   * Returns the error message string of the wrapped exception.
   *
   * @return the error message string of the wrapped exception
   */
  public String getMessage() {
    return m_message;
  }

  /**
   * Returns the localized description of the wrapped exception in order to produce a locale-specific message.
   *
   * @return the localized description of the wrapped exception.
   */
  public String getLocalizedMessage() {
    return m_throwable.getLocalizedMessage();
  }

  /**
   * Returns the original exception.
   *
   * @return the cause
   */
  public Throwable getCause() {
    return m_throwable;
  }

  /**
   * Returns a short description of the wrapped exception.
   *
   * @return a string representation of the wrapped exception.
   */
  public String toString() {
    return (m_message==null ? "" : m_message) + "; " + m_throwable.toString();
  }

  ///CLOVER:OFF

  /**
   * Prints the wrapped exception A its backtrace to the standard error stream.
   */
  public void printStackTrace() {
    m_throwable.printStackTrace();
  }

  /**
   * Prints the wrapped excpetion A its backtrace to the specified print stream.
   *
   * @param s the print stream
   */
  public void printStackTrace(final PrintStream s) {
    m_throwable.printStackTrace(s);
  }

  /**
   * Prints the wrapped exception A its backtrace to the specified print writer.
   *
   * @param s the print writer
   */
  public void printStackTrace(final PrintWriter s) {
    m_throwable.printStackTrace(s);
  }

  ///CLOVER:ON
}
