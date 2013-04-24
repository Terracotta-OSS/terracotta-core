/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import org.terracotta.toolkit.ToolkitFeature;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.feature.FeatureNotSupportedException;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.rejoin.RejoinException;

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

  private static final Method TOOLKIT_FEATURE_IS_ENABLED_METHOD;
  static {
    Method m = null;
    try {
      m = ToolkitFeature.class.getMethod("isEnabled", new Class[0]);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    if (m == null) { throw new AssertionError("isEnabled() method not found in ToolkitFeature"); }
    TOOLKIT_FEATURE_IS_ENABLED_METHOD = m;
  }

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

    InvocationHandler handler = new NonStopInvocationHandler<T>(context, nonStopConfigurationLookup,
                                                                toolkitObjectLookup);

    T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    return proxy;
  }


  public static <T extends ToolkitObject> T newNonStopProxy(final NonStopConfigurationLookup nonStopConfigurationLookup,
                                                            final NonStopContext context, final Class<T> clazz,
                                                            final ToolkitObjectLookup toolkitObjectLookup) {
    InvocationHandler handler = new NonStopInvocationHandler<T>(context, nonStopConfigurationLookup,
                                                                toolkitObjectLookup);

    T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    return proxy;
  }


  public static <T> T newNonStopSubTypeProxy(final NonStopConfigurationLookup nonStopConfigurationLookup,
                                             final NonStopContext context, final T delegate, final Class<T> clazz) {
    if (clazz.equals(ToolkitLock.class)) {
      ToolkitObjectLookup<ToolkitLock> lookup = new ToolkitObjectLookup<ToolkitLock>() {

        @Override
        public ToolkitLock getInitializedObject() {
          return (ToolkitLock) delegate;
        }

        @Override
        public ToolkitLock getInitializedObjectOrNull() {
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

  public static <T extends ToolkitFeature> T newFeatureNotSupportedProxy(final Class<T> clazz) {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.equals(TOOLKIT_FEATURE_IS_ENABLED_METHOD)) { return false; }
        throw new FeatureNotSupportedException("Feature specified by '" + clazz.getName() + "' is not supported!");
      }
    };
    T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    return proxy;
  }
}
