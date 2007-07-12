/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.asm.Type;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a proxy to allows a class containing the correct set of method signatures to stand in for an interface (w/o
 * actually implementing it)
 *
 * NOTE: even if createProxyIfPossible() returns a proxy, it doesn't mean you want get ClassCastExceptions on the
 * argument and/or return types -- The only checking that is done is on class names (ignoring loaders completely)
 *
 * NOTE (2): Since reflection is used, this probably isn't the fastest thing in town.
 */
public class ReflectiveProxy {

  private ReflectiveProxy() {
    //
  }

  // TODO: one should really be able to pass an array of interfaces to this method
  public static Object createProxyIfPossible(Class iface, Object candidate) {
    if (iface == null || !iface.isInterface()) { throw new IllegalArgumentException("invalid class to proxy: " + iface); }

    Map ifaceMethods = getMethods(iface);
    Map instanceMethods = getMethods(candidate.getClass());

    if (!instanceMethods.keySet().containsAll(ifaceMethods.keySet())) { return null; }

    // all methods present
    return Proxy.newProxyInstance(iface.getClassLoader(), new Class[] { iface },
                                  new Handler(candidate, instanceMethods));
  }

  private static Map getMethods(Class c) {
    Map rv = new HashMap();

    Method[] methods = c.getMethods();
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];
      String key = makeMethodKey(m);
      Object prev = rv.put(key, m);
      if (prev != null) { throw new AssertionError("replaced mapping for " + key); }
    }

    return rv;
  }

  private static String makeMethodKey(Method m) {
    return m.getName() + Type.getMethodDescriptor(m);
  }

  public static class Handler implements InvocationHandler {
    private final Object obj;
    private final Map    methods;

    public Handler(Object obj, Map methods) {
      this.obj = obj;
      this.methods = methods;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String key = makeMethodKey(method);
      Method targetMethod = (Method) methods.get(key);
      targetMethod.setAccessible(true);
      if (targetMethod == null) { throw new AssertionError("missing method for " + key); }
      return targetMethod.invoke(obj, args);
    }
  }

}
