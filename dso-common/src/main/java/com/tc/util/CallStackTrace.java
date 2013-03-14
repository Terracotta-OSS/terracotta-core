/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CallStackTrace {

  public static String getCallStack() {
    boolean enabled = Boolean.getBoolean("stack.trace.enabled");
    if (enabled) {
      Throwable t = new Throwable();
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      return "\n" + Thread.currentThread().getName() + " " + sw.toString();
    }
    return "";
  }
}
