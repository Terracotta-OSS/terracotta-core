/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

/**
 * @author andrew A
 */
public class TCJUnitFormatter implements JUnitResultFormatter {

  private final List  currentExceptions;
  private static class TestException {
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

  public TCJUnitFormatter() {
    this.currentExceptions = new ArrayList();
  }

  public void endTestSuite(JUnitTest theTest) throws BuildException {
    boolean passed = (theTest.errorCount() == 0 && theTest.failureCount() == 0);

    if (!passed) {
      System.err.println("*** " + theTest.getName() + " FAILED: " + this.currentExceptions.size() + " exceptions:");
      Iterator iter = this.currentExceptions.iterator();
      while (iter.hasNext()) {
        System.err.println(iter.next().toString());
        System.err.println("");
      }
    }

    this.currentExceptions.clear();
  }

  public void setOutput(OutputStream theStream) {
    //
  }

  public void setSystemError(String arg0) {
    //
  }

  public void setSystemOutput(String arg0) {
    //
  }

  public void startTestSuite(JUnitTest theTest) throws BuildException {
    System.err.println(theTest.getName() + "...");
    System.err.flush();
  }

  public void addError(Test arg0, Throwable arg1) {
    this.currentExceptions.add(new TestException(arg0, arg1));
  }

  public void addFailure(Test arg0, AssertionFailedError arg1) {
    this.currentExceptions.add(new TestException(arg0, arg1));
  }

  public void endTest(Test arg0) {
    //
  }

  public void startTest(Test arg0) {
    //
  }
}
