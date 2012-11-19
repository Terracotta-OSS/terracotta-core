/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitRuntimeException;

import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonStopInvocationHandler<T> implements InvocationHandler {

  private final NonStopManager             nonStopManager;
  private final NonStopDelegateProvider<T> nonStopDelegateProvider;

  public NonStopInvocationHandler(NonStopManager nonStopManager, NonStopDelegateProvider<T> nonStopDelegateProvider) {
    this.nonStopManager = nonStopManager;
    this.nonStopDelegateProvider = nonStopDelegateProvider;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    nonStopManager.begin(nonStopDelegateProvider.getTimeout(method.getName()));
    try {
      return invokeMethod(method, args, nonStopDelegateProvider.getDelegate());
    } catch (ToolkitAbortableOperationException e) {
      return invokeMethod(method, args, nonStopDelegateProvider.getTimeoutBehavior());
    } finally {
      nonStopManager.finish();
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
