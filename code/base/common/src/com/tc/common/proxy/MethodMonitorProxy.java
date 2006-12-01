/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A helper class to create proxies that will generate events for any interface method invocations
 */
public class MethodMonitorProxy {
  public static Object createProxy(final Object objectToMonitor, MethodInvocationEventListener listener) {
    return createProxy(objectToMonitor, new MethodInvocationEventListener[] { listener });
  }

  public static Object createProxy(final Object objectToMonitor, MethodInvocationEventListener listeners[]) {
    if (null == objectToMonitor) { throw new NullPointerException("Cannot proxy a null instance"); }
    if (null == listeners) { throw new NullPointerException("Listener list cannot be null"); }

    for (int i = 0; i < listeners.length; i++) {
      if (null == listeners[i]) { throw new NullPointerException("Null listener in list at position " + i); }
    }

    final Class clazz = objectToMonitor.getClass();
    final Class[] interfaces = clazz.getInterfaces();

    if (0 == interfaces.length) { throw new IllegalArgumentException("Class (" + clazz.getName()
                                                                     + ") does not implement any interfaces"); }

    return Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, new MonitorHandler(objectToMonitor, listeners));
  }

  private static class MonitorHandler implements InvocationHandler {
    private final MethodInvocationEventListener[] listeners;
    private final Object                          delegate;

    MonitorHandler(Object delegate, MethodInvocationEventListener[] listeners) {
      this.listeners = (MethodInvocationEventListener[]) listeners.clone();
      this.delegate = delegate;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Throwable exception = null;

      Object returnValue = null;

      final long end;
      final long start = System.currentTimeMillis();
      try {
        returnValue = method.invoke(delegate, args);
      } catch (InvocationTargetException ite) {
        exception = ite.getCause();
      } finally {
        end = System.currentTimeMillis();
      }

      final MethodInvocationEvent event = new MethodInvocationEventImpl(start, end, delegate, method, args, exception,
                                                                        returnValue);
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].methodInvoked(event);
      }

      if (null != exception) {
        throw exception;
      } else {
        return returnValue;
      }
    }
  }
}