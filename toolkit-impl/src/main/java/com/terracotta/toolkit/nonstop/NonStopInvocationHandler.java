/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.internal.nonstop.NonStopManager;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonStopInvocationHandler<T> implements InvocationHandler {
  private final NonStopManager             nonStopManager;
  private final NonStopDelegateProvider<T> nonStopDelegateProvider;
  private final NonStopClusterListener     clusterListener;

  public NonStopInvocationHandler(NonStopManager nonStopManager, NonStopDelegateProvider<T> nonStopDelegateProvider,
                                  NonStopClusterListener clusterListener) {
    this.nonStopManager = nonStopManager;
    this.nonStopDelegateProvider = nonStopDelegateProvider;
    this.clusterListener = clusterListener;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    NonStopConfiguration nonStopConfiguration = nonStopDelegateProvider.getNonStopConfiguration(method.getName());

    if (!nonStopConfiguration.isEnabled()) { return invokeMethod(method, args, nonStopDelegateProvider.getDelegate()); }

    if (nonStopConfiguration.isImmediateTimeoutEnabled() && !clusterListener.areOperationsEnabled()) { return invokeMethod(method,
                                                                                                                           args,
                                                                                                                           nonStopDelegateProvider
                                                                                                                               .getTimeoutBehavior()); }
    boolean started = nonStopManager.tryBegin(getTimeout(nonStopConfiguration));
    try {
      clusterListener.waitUntilOperationsEnabled();
      return invokeMethod(method, args, nonStopDelegateProvider.getDelegate());
    } catch (ToolkitAbortableOperationException e) {
      return invokeMethod(method, args, nonStopDelegateProvider.getTimeoutBehavior());
    } catch (RejoinException e) {
      // TODO: Review this.. Is this the right place to handle this...
      return invokeMethod(method, args, nonStopDelegateProvider.getTimeoutBehavior());
    } finally {
      if (started) {
        nonStopManager.finish();
      }
    }
  }

  private long getTimeout(NonStopConfiguration nonStopConfiguration) {
    if (nonStopConfiguration.isEnabled()) {
      return nonStopConfiguration.getTimeoutMillis();
    } else {
      return -1;
    }
  }

  private Object invokeMethod(Method method, Object[] args, Object object) throws Throwable {
    try {
      return method.invoke(object, args);
    } catch (InvocationTargetException t) {
      throw t.getTargetException();
    } catch (IllegalArgumentException e) {
      throw new ToolkitRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new ToolkitRuntimeException(e);
    }
  }
}
