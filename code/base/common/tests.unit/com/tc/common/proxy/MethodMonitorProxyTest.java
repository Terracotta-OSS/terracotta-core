/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import com.tc.util.Util;

import java.lang.reflect.Method;
import java.util.Random;

import junit.framework.TestCase;

public class MethodMonitorProxyTest extends TestCase {

  final static Method i1_m1;
  final static Method i1_m2;

  static {
    try {
      i1_m1 = i1.class.getDeclaredMethod("m1", new Class[] { Integer.TYPE });
      i1_m2 = i1.class.getDeclaredMethod("m2", new Class[] {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void testMethods() {
    c1 obj = new c1();
    Object proxy = MethodMonitorProxy
        .createProxy(obj, new MethodInvocationEventListener[] { new MethodInvocationEventListener() {
          public void methodInvoked(MethodInvocationEvent event) {
            System.out.println("method invoked: " + event.getMethod());
            System.out.println("args: " + Util.enumerateArray(event.getArguments()));
            System.out.println("start time: " + event.getExecutionStartTime());
            System.out.println("end time: " + event.getExecutionEndTime());
            System.out.println("elapsed time: " + (event.getExecutionEndTime() - event.getExecutionStartTime()));
          }
        } });

    assertTrue(proxy instanceof i1);
    i1 iface = (i1) proxy;

    iface.m1(0);

    try {
      iface.m2();
      fail("no exception received in client");
    } catch (Exception e) {
      assertTrue(e.getMessage().equals("yippy"));
      assertTrue(e.getCause().getMessage().equals("skippy"));      
    }
  }

  interface i1 {
    public int m1(int a1);

    public void m2() throws Exception;
  }

  static class c1 implements i1 {
    public int m1(int a1) {
      if (a1 < 0) {
        throw new RuntimeException();
      } else {
        return a1;
      }
    }

    public void m2() throws Exception {
      Thread.sleep(new Random().nextInt(500));
      throw new Exception("yippy", new RuntimeException("skippy"));
    }
  }

}
