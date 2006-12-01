/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import java.lang.reflect.Proxy;

import junit.framework.TestCase;

/**
 * @author andrew Unit test for {@link GenericInvocationHandler}.
 */
public class GenericInvocationHandlerTest extends TestCase {

  private static interface Foo {
    public String a(String x);

    public int b(int b);

    public String b(String b);

    public String c(String c);
  }

  private static class OurHandler {
    private int    aCallCount;
    private String lastACallArg;
    private int    b1CallCount;
    private int    lastB1CallArg;
    private int    b2CallCount;
    private String lastB2CallArg;

    public String a(String x) {
      ++aCallCount;
      lastACallArg = x;
      return "a!";
    }

    public int b(int b) {
      ++b1CallCount;
      lastB1CallArg = b;
      return 42;
    }

    public String b(String b) {
      ++b2CallCount;
      lastB2CallArg = b;
      return "b2!";
    }

    public void reset() {
      aCallCount = 0;
      lastACallArg = null;
      b1CallCount = 0;
      lastB1CallArg = 0;
      b2CallCount = 0;
      lastB2CallArg = null;
    }

    public int getACallCount() {
      return this.aCallCount;
    }

    public int getB1CallCount() {
      return this.b1CallCount;
    }

    public int getB2CallCount() {
      return this.b2CallCount;
    }

    public String getLastACallArg() {
      return this.lastACallArg;
    }

    public int getLastB1CallArg() {
      return this.lastB1CallArg;
    }

    public String getLastB2CallArg() {
      return this.lastB2CallArg;
    }
  }

  private GenericInvocationHandler invocationHandler;
  private OurHandler               handlerObject;
  private Foo                      theProxy;

  protected void setUp() throws Exception {
    handlerObject = new OurHandler();
    invocationHandler = new GenericInvocationHandler(handlerObject);
    theProxy = (Foo) Proxy.newProxyInstance(GenericInvocationHandlerTest.class.getClassLoader(),
                                            new Class[] { Foo.class }, invocationHandler);
  }

  public void testDelegatesMethodsToHandler() throws Exception {
    checkCallCounts(0, 0, 0);

    assertEquals("a!", theProxy.a("g"));
    checkCallCounts(1, 0, 0);
    assertEquals("g", handlerObject.getLastACallArg());
    handlerObject.reset();

    assertEquals(42, theProxy.b(16));
    checkCallCounts(0, 1, 0);
    assertEquals(16, handlerObject.getLastB1CallArg());
    handlerObject.reset();

    assertEquals("b2!", theProxy.b("h"));
    checkCallCounts(0, 0, 1);
    assertEquals("h", handlerObject.getLastB2CallArg());
    handlerObject.reset();
  }

  public void testThrowsExceptionOnMissingMethod() throws Exception {
    try {
      theProxy.c("a");
      fail("Should've gotten an exception");
    } catch (NoSuchMethodError err) {
      // ok
    }
  }

  private void checkCallCounts(int aCallCount, int bCallCount, int cCallCount) {
    assertEquals(aCallCount, handlerObject.getACallCount());
    assertEquals(bCallCount, handlerObject.getB1CallCount());
    assertEquals(cCallCount, handlerObject.getB2CallCount());
  }

}
