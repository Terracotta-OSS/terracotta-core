/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DestroyedInstanceProxy {

  public static <T> T createNewInstance(final Class<T> clazz, final String name) {
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
}
