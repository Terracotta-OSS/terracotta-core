/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorContext {
  private final String message;
  private final Thread thread;
  private Throwable    throwable;

  public ErrorContext(String message) {
    this.message = message;
    this.thread = Thread.currentThread();
  }

  public ErrorContext(String message, Throwable throwable) {
    this(message);
    this.throwable = throwable;
  }

  public ErrorContext(Throwable t) {
    this("", t);
  }

  public String getMessage() {
    return message;
  }

  public Thread getThread() {
    return thread;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public String toString() {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    dump(pw);
    pw.flush();
    return sw.getBuffer().toString();
  }

  // Ok. It's totally retarted to have these two dump methods, but I can't for the life of me figure out
  // how collapse the two methods together.  The java.io package has me stumped at the moment
  // --Orion

  public void dump(PrintWriter out) {
    out.println(getClass().getName() + " [" + message + ", thread: " + thread + "]");
    if (throwable != null) {
      throwable.printStackTrace(out);
    }
  }

  public void dump(PrintStream out) {
    out.println(getClass().getName() + " [" + message + ", thread: " + thread + "]");
    if (throwable != null) {
      throwable.printStackTrace(out);
    }
  }
}