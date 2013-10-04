/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExceptionOnTimeoutBehaviorResolver {
  private final ExceptionOnTimeoutInvocationHandler handler                     = new ExceptionOnTimeoutInvocationHandler();
  private final ConcurrentMap<Class, Object>        exceptionOnTimeoutBehaviors = new ConcurrentHashMap<Class, Object>();

  public <E> E resolve(Class<E> klazz) {
    Object rv = exceptionOnTimeoutBehaviors.get(klazz);
    if (rv == null) {
      Object newProxyInstance = Proxy.newProxyInstance(klazz.getClassLoader(), new Class[] { klazz }, handler);
      Object oldProxyInstance = exceptionOnTimeoutBehaviors.putIfAbsent(klazz, newProxyInstance);
      rv = oldProxyInstance != null ? oldProxyInstance : newProxyInstance;
    }
    return (E) rv;
  }
}
