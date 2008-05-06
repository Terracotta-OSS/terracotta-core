/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.exception;

import java.io.PrintStream;

public final class ExceptionUtil {

  /**
   * Does no filtering of duplicate stacks or causes like {@link Throwable#printStackTrace(PrintStream)} does, just
   * prints out the entire stack trace.
   */
  public static void dumpFullStackTrace(final Throwable exception, final PrintStream outputStream) {
    outputStream.println(exception.toString());
    final StackTraceElement[] stack = exception.getStackTrace();
    for (int pos = 0; pos < stack.length; ++pos) {
      outputStream.println("\tat " + stack[pos]);
    }
    final Throwable cause = exception.getCause();
    if (cause != null && cause != exception) {
      outputStream.print("Caused by: ");
      dumpFullStackTrace(cause, outputStream);
    }
  }

}
