/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import junit.framework.TestCase;

public class ReflectiveProxyTest extends TestCase {

  interface Test1Interface {
    void foo();

    void foo(int i);

    void foo(Object o);
  }

  public static class Test1Class {

    int    foo0 = 0;
    int    foo1 = 0;
    Object foo2 = null;

    public void foo() {
      foo0++;
    }

    public void foo(int i) {
      foo1 = i;
    }

    public void foo(Object o) {
      foo2 = o;
    }
  }

  public void test1() throws Exception {
    Test1Class o = new Test1Class();
    Test1Interface proxy = (Test1Interface) ReflectiveProxy.createProxyIfPossible(Test1Interface.class, o);
    assertNotNull(proxy);

    assertEquals(0, o.foo0);
    proxy.foo();
    assertEquals(1, o.foo0);

    assertEquals(0, o.foo1);
    proxy.foo(42);
    assertEquals(42, o.foo1);

    assertEquals(null, o.foo2);
    proxy.foo(this);
    assertEquals(this, o.foo2);
  }

  /*
   * a differing return type disallows proxy
   */
  interface Test2Interface {
    void foo();
  }

  static class Test2Class {
    public Test2Class foo() {
      return this;
    }
  }

  public void test2() throws Exception {
    Object o = new Test2Class();
    Test2Interface proxy = (Test2Interface) ReflectiveProxy.createProxyIfPossible(Test2Interface.class, o);
    assertNull(proxy);
  }

  /*
   * a differing arg list disallows proxy
   */
  interface Test3Interface {
    void foo(int o);
  }

  static class Test3Class {
    public void foo() {
      //
    }
  }

  public void test3() throws Exception {
    Object o = new Test3Class();
    Test3Interface proxy = (Test3Interface) ReflectiveProxy.createProxyIfPossible(Test3Interface.class, o);
    assertNull(proxy);
  }

  /*
   * non-public (but matching method) disallows proxy
   */
  interface Test4Interface {
    void foo();
  }

  static class Test4Class {
    void foo() {
      //
    }
  }

  public void test4() throws Exception {
    Object o = new Test4Class();
    Test4Interface proxy = (Test4Interface) ReflectiveProxy.createProxyIfPossible(Test4Interface.class, o);
    assertNull(proxy);
  }

}
