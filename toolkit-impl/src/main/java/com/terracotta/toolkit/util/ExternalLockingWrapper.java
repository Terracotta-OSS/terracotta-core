/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.object.TCObject;
import com.tc.platform.PlatformService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ExternalLockingWrapper {

  public static <T> T newExternalLockingProxy(final Class<T> clazz, final PlatformService service, final TCObject t) {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (service.isLockedBeforeRejoin()) {
          throw new RejoinException("Lock is not usable anymore after rejoin has occured,Operation failed");
        } else {
          return method.invoke(t, args);
        }
      }
    };

    T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    return proxy;
  }
}
