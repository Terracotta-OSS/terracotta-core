/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import org.terracotta.toolkit.rejoin.RejoinException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class ToolkitInstanceProxy {

  public static <T> T newDestroyedInstanceProxy(final String name, final Class<T> clazz) {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        throw new IllegalStateException("The toolkit instance with name '" + name + "' (instance of " + clazz.getName()
                                        + ") has already been destroyed");
      }
    };

    T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    return proxy;
  }

  public static <T> T newRejoinInProgressProxy(final String name, final Class<T> clazz) {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // TODO: throw explicit public exception type
        throw new RejoinException("The toolkit instance with name '" + name + "' (instance of " + clazz.getName()
                                   + ") is not usable at the moment as rejoin is in progress");
      }
    };

    T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    return proxy;
  }

}
