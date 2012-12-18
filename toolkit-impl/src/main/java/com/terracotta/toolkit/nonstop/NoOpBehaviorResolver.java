/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import com.tc.util.concurrent.ConcurrentHashMap;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentMap;

public class NoOpBehaviorResolver {
  private final NoOpInvocationHandler        handler              = new NoOpInvocationHandler();
  private final ConcurrentMap<Class, Object> noOpTimeoutBehaviors = new ConcurrentHashMap<Class, Object>();

  public <E> E resolve(Class<E> klazz) {
    Object rv = noOpTimeoutBehaviors.get(klazz);
    if (rv == null) {
      Object newProxyInstance = Proxy.newProxyInstance(klazz.getClassLoader(), new Class[] { klazz }, handler);
      Object oldProxyInstance = noOpTimeoutBehaviors.putIfAbsent(klazz, newProxyInstance);
      rv = oldProxyInstance != null ? oldProxyInstance : newProxyInstance;
    }
    return (E) rv;
  }

}
