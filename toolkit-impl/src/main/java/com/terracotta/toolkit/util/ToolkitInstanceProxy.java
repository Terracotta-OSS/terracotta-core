/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.nonstop.AbstractToolkitObjectLookup;
import com.terracotta.toolkit.nonstop.NonStopConfigurationLookup;
import com.terracotta.toolkit.nonstop.NonStopContext;
import com.terracotta.toolkit.nonstop.NonStopInvocationHandler;
import com.terracotta.toolkit.nonstop.NonStopLockImpl;
import com.terracotta.toolkit.nonstop.NonStopSubTypeInvocationHandler;
import com.terracotta.toolkit.nonstop.ToolkitObjectLookup;

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

  public static <T extends ToolkitObject> T newNonStopProxy(final String name,
                                                            final ToolkitObjectType toolkitObjectType,
                                                            final NonStopContext context, final Class<T> clazz,
                                                            final ToolkitObjectLookup toolkitObjectLookup) {
    NonStopConfigurationLookup nonStopConfigurationLookup = new NonStopConfigurationLookup(context, toolkitObjectType,
                                                                                           name);
    if (toolkitObjectType == ToolkitObjectType.LOCK) { return (T) new NonStopLockImpl(context,
                                                                                      nonStopConfigurationLookup,
                                                                                      toolkitObjectLookup); }
    InvocationHandler handler = new NonStopInvocationHandler<T>(context, nonStopConfigurationLookup,
                                                                toolkitObjectLookup);

    T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    return proxy;
  }

  public static <T> T newNonStopSubTypeProxy(final NonStopConfigurationLookup nonStopConfigurationLookup,
                                             final NonStopContext context, final T delegate, final Class<T> clazz) {
    if (clazz.equals(ToolkitLock.class)) {
      ToolkitObjectLookup<ToolkitLock> lookup = new AbstractToolkitObjectLookup<ToolkitLock>(
          context.getAbortableOperationManager()) {

        @Override
        protected ToolkitLock lookupObject() {
          return (ToolkitLock) delegate;
        }
      };
      return (T) new NonStopLockImpl(context, nonStopConfigurationLookup, lookup);
    }
    InvocationHandler handler = new NonStopSubTypeInvocationHandler<T>(context, nonStopConfigurationLookup, delegate,
                                                                       clazz);

    T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    return proxy;
  }
}
