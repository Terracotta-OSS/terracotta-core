/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.awt.AWTException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;

public class ShareExceptionsTestApp extends AbstractErrorCatchingTransparentApp {

  private static final int INITIAL      = 0;
  private static final int INTERMEDIATE = 1;
  private static final int END          = 2;

  final List               root         = new ArrayList();
  final CyclicBarrier      barrier;

  public ShareExceptionsTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ShareExceptionsTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");

    CyclicBarrierSpec cbspec = new CyclicBarrierSpec();
    cbspec.visit(visitor, config);

    // Include everything to be instrumented.
    config.addIncludePattern("*..*", false);
  }

  protected void runTest() throws Throwable {
    moveToStage(INITIAL);
    List localCopy = createVariousExceptions();
    int n = barrier.barrier();
    if (n == 0) {
      add2Root(localCopy);
      moveToStage(INTERMEDIATE);
    } else {
      moveToStageAndWait(INTERMEDIATE);
      synchronized (root) {
        verify(localCopy, root);
      }
    }
    moveToStage(END);
  }

  private void verify(List localCopy, List actual) {
    Assert.assertEquals(localCopy.size(), actual.size());
    for (int i = 0; i < actual.size(); i++) {
      Throwable tl = (Throwable) localCopy.get(i);
      Throwable ta = (Throwable) actual.get(i);
      verify(tl, ta);
    }
  }

  private void verify(Throwable tl, Throwable ta) {
    if (tl == null) {
      Assert.assertTrue(ta == null);
      return;
    }
    System.err.println(" Local Copy = " + tl);
    tl.printStackTrace();
    System.err.println(" Actual = " + ta);
    ta.printStackTrace();
    if(tl instanceof MyLocalException) {
      Assert.assertTrue(ta instanceof MyLocalException);
      Assert.assertTrue(((MyLocalException)tl).localInt == ((MyLocalException)ta).localInt);
    }
    Assert.assertEquals(tl.getMessage(), ta.getMessage());
    Assert.assertEquals(tl.getLocalizedMessage(), ta.getLocalizedMessage());
    if (tl.getCause() != tl) {
      Assert.assertTrue(ta.getCause() != ta);
      verify(tl.getCause(), ta.getCause());
    }
    Assert.assertEquals(tl.toString(), ta.toString());
    Assert.assertEquals(getPrintString(tl), getPrintString(ta));
    verify(tl.getStackTrace(), ta.getStackTrace());
  }

  private void verify(StackTraceElement[] stackTrace1, StackTraceElement[] stackTrace2) {
    Assert.assertEquals(stackTrace1.length, stackTrace2.length);
    for (int i = 0; i < stackTrace1.length; i++) {
      Assert.assertEquals(stackTrace1[i], stackTrace2[i]);
    }
  }

  private String getPrintString(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.close();
    return sw.toString();
  }

  private void add2Root(List localCopy) {
    synchronized (root) {
      root.addAll(localCopy);
    }
  }

  private List createVariousExceptions() {
    List l = new ArrayList();
    addThrownException(l);
    addThrownAndPrintedException(l);
    addNewlyCreatedException(l);
    addNewlyCreatedAndStackTraceComputedException(l);
    addInitCausedException(l);
    addSetStackTraceException(l);
    addSomeRandomExceptions(l);
    return l;
  }

  // TODO:: ADD MORE (ALL) EXCEPTIONS HERE
  private void addSomeRandomExceptions(List l) {
    l.add(new Exception("666"));
    l.add(new RuntimeException("This is a runtime exception"));
    l.add(new AWTException("This is awt exception"));
    l.add(new IOException("This is IO exception"));
    l.add(new FileNotFoundException("C:\\windows.sucks"));
    l.add(new Error("Serious Error"));
    l.add(new ConcurrentModificationException("Who touched my list ?"));
    l.add(new NoSuchElementException("No one named saro"));

  }

  private void addSetStackTraceException(List l) {
    Throwable t1 = getException1();
    Throwable t2 = getException2();
    t1.setStackTrace(t2.getStackTrace());
    l.add(t1);
    l.add(t2);
  }

  private Throwable getException1() {
    return new MyLocalException("MyException1", count++);
  }

  private void addInitCausedException(List l) {
    Throwable t = new MyLocalException("InitCausedException", count++);
    t.initCause(new Exception("Hello Sollu"));
    l.add(t);
  }

  private void addNewlyCreatedAndStackTraceComputedException(List l) {
    Throwable t = new MyLocalException("NewlyCreatedAndStackTraceComputedException", count++);
    t.getStackTrace();
    l.add(t);
  }

  private void addNewlyCreatedException(List l) {
    l.add(new MyLocalException("NewlyCreatedException", count++));
  }

  private void addThrownException(List l) {
    try {
      getSomeException();
    } catch (Throwable t) {
      l.add(t);
    }
  }

  private void addThrownAndPrintedException(List l) {
    try {
      getSomeException();
    } catch (Throwable t) {
      t.printStackTrace();
      l.add(t);
    }
  }

  int count = 0;

  private void getSomeException() throws Throwable {
    throw new MyLocalException("My Local Exception", count++);
  }

  private Throwable getException2() {
    return new MyLocalException("MyException2", count++);
  }

  public static class MyLocalException extends Throwable {

    int localInt;

    public MyLocalException(String message, int i) {
      super(message + " - " + i);
      localInt = i;
    }

    public MyLocalException(String message, Throwable cause) {
      super(message, cause);
    }

    public boolean equals(Object o) {
      System.err.println("Equals Method called on " + this.getMessage() + " and " + o);
      if (o instanceof MyLocalException) {
        MyLocalException other = (MyLocalException) o;
        boolean b = this.localInt == other.localInt;
        if (!b) return b;
        b = this.getMessage().equals(other.getMessage());
        if (!b) return b;
        return true;
      }
      return false;
    }

  }
}
