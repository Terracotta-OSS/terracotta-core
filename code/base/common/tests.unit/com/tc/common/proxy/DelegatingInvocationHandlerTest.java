/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import com.tc.common.proxy.subpkg.Factory;
import com.tc.common.proxy.subpkg.TestInterface;

import java.io.IOException;
import java.lang.reflect.Proxy;

import junit.framework.TestCase;

/**
 * @author andrew Unit test for {@link DelegatingInvocationHandlerTest}.
 */
public class DelegatingInvocationHandlerTest extends TestCase {

  public static interface TheInterface {
    String a(String arg);

    String b(String arg);

    String c(String arg);

    void d() throws IOException;

    void e() throws IOException;
  }

  public static class Handler {
    private int    numACalls;
    private String lastAArg;

    public Handler() {
      reset();
    }

    public String a(String arg) {
      ++numACalls;
      lastAArg = arg;
      return "handler.a";
    }

    public void reset() {
      numACalls = 0;
      lastAArg = null;
    }

    public String getLastAArg() {
      return this.lastAArg;
    }

    public int getNumACalls() {
      return this.numACalls;
    }

    public void d() throws IOException {
      throw new IOException("handler");
    }
  }

  public static class Delegate {
    private int    numACalls;
    private String lastAArg;
    private int    numBCalls;
    private String lastBArg;

    public Delegate() {
      reset();
    }

    public String a(String arg) {
      ++numACalls;
      lastAArg = arg;
      return "delegate.a";
    }

    public String b(String arg) {
      ++numBCalls;
      lastBArg = arg;
      return "delegate.b";
    }

    public void d() {
      //;
    }

    public void e() throws IOException {
      throw new IOException("delegate");
    }

    public void reset() {
      numACalls = 0;
      lastAArg = null;
    }

    public String getLastAArg() {
      return this.lastAArg;
    }

    public String getLastBArg() {
      return this.lastBArg;
    }

    public int getNumACalls() {
      return this.numACalls;
    }

    public int getNumBCalls() {
      return this.numBCalls;
    }
  }

  private Handler                     handler;
  private Delegate                    delegate;
  private DelegatingInvocationHandler invocationHandler;
  private TheInterface                theProxy;

  protected void setUp() throws Exception {
    this.handler = new Handler();
    this.delegate = new Delegate();
    this.invocationHandler = new DelegatingInvocationHandler(delegate, handler);
    theProxy = (TheInterface) Proxy.newProxyInstance(DelegatingInvocationHandlerTest.class.getClassLoader(),
                                                     new Class[] { TheInterface.class }, invocationHandler);
  }

  public void testDelegates() throws Exception {
    String value = theProxy.a("x");
    assertEquals("handler.a", value);
    assertEquals(1, handler.getNumACalls());
    assertEquals("x", handler.getLastAArg());
    assertEquals(0, delegate.getNumACalls());
    assertEquals(0, delegate.getNumBCalls());

    handler.reset();

    value = theProxy.b("y");
    assertEquals("delegate.b", value);
    assertEquals(0, handler.getNumACalls());
    assertEquals(0, delegate.getNumACalls());
    assertEquals(1, delegate.getNumBCalls());
    assertEquals("y", delegate.getLastBArg());
  }

  public void testException() throws Exception {
    try {
      theProxy.e();
      fail("e() didn't throw exception");
    } catch (IOException ioe) {
      assertTrue(ioe.getMessage().indexOf("delegate") >= 0);
    }

    try {
      theProxy.d();
      fail("d() didn't throw exception");
    } catch (IOException ioe) {
      assertTrue(ioe.getMessage().indexOf("handler") >= 0);
    }
  }

  public void testNonPublicDelegate() {
    TestInterface proxy = (TestInterface) DelegateHelper.createDelegate(TestInterface.class, Factory.getInstance());
    proxy.method();
  }

}
