/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Abhishek Sanoujam
 */
public class InterceptedProxy {

  public static interface Interceptor<T> {
    Object intercept(T actualDelegate, Method method, Object[] args) throws Exception;
  }

  /**
   * Returns an intercepted proxy. All method invocations on the proxy is intercepted and goes through the
   * interceptor.intercept() method
   */
  public static <T> T createInterceptedProxy(final T actualDelegate, Class<T> interfaceClass,
                                                      final Interceptor interceptor) {
    InvocationHandler handler = new InvocationHandler() {

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return interceptor.intercept(actualDelegate, method, args);
      }
    };
    return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass }, handler);
  }
}
