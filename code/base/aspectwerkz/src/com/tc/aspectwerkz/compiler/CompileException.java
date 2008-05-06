/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.compiler;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * An exception occured during compilation
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class CompileException extends Exception {
  private Throwable nested;

  public CompileException(String msg, Throwable e) {
    super(msg);
    nested = e;
  }

  public void printStackTrace() {
    printStackTrace(System.err);
  }

  public void printStackTrace(PrintWriter writer) {
    super.printStackTrace(writer);
    if (nested != null) {
      writer.println("nested:");
      nested.printStackTrace(writer);
    }
  }

  public void printStackTrace(PrintStream out) {
    super.printStackTrace(out);
    if (nested != null) {
      out.println("nested:");
      nested.printStackTrace(out);
    }
  }
}