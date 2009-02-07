/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.Test;

public class TestException {
  private final Test      theTest;
  private final Throwable exception;

  public TestException(Test theTest, Throwable exception) {
    this.theTest = theTest;
    this.exception = exception;
  }

  public String toString() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println(this.theTest + " FAILED:");
    this.exception.printStackTrace(pw);

    pw.flush();
    return sw.toString();
  }
}
