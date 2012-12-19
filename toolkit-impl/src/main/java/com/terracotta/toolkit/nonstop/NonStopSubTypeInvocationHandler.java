/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.rejoin.InvalidLockStateAfterRejoinException;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonStopSubTypeInvocationHandler<T> implements InvocationHandler {
  private final NonStopContext             context;
  private final NonStopConfigurationLookup nonStopConfigurationLookup;
  private final T                          delegate;
  private final Class                      klazz;

  public NonStopSubTypeInvocationHandler(NonStopContext context, NonStopConfigurationLookup nonStopConfigurationLookup,
                                         T delegate, Class<T> klazz) {
    this.context = context;
    this.nonStopConfigurationLookup = nonStopConfigurationLookup;
    this.delegate = delegate;
    this.klazz = klazz;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfiguration();

    if (!nonStopConfiguration.isEnabled()) { return invokeMethod(method, args, delegate); }

    if (nonStopConfiguration.isImmediateTimeoutEnabled() && !context.getNonStopClusterListener().areOperationsEnabled()) { return invokeMethod(method,
                                                                                                                                               args,
                                                                                                                                               resolveTimeoutBehavior(nonStopConfiguration)); }
    boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
    try {
      context.getNonStopClusterListener().waitUntilOperationsEnabled();
      Object returnValue = invokeMethod(method, args, delegate);
      return createNonStopSubtypeIfNecessary(returnValue, method.getReturnType());
    } catch (ToolkitAbortableOperationException e) {
      return invokeMethod(method, args, resolveTimeoutBehavior(nonStopConfiguration));
    } catch (InvalidLockStateAfterRejoinException e) {
      if (klazz != ToolkitLock.class) { return invokeMethod(method, args,
                                                                resolveTimeoutBehavior(nonStopConfiguration)); }
      throw e;
    } catch (RejoinException e) {
      // TODO: Review this.. Is this the right place to handle this...
      return invokeMethod(method, args, resolveTimeoutBehavior(nonStopConfiguration));
    } finally {
      if (started) {
        context.getNonStopManager().finish();
      }
    }
  }

  private Object resolveTimeoutBehavior(NonStopConfiguration nonStopConfiguration) {
    return context.getNonstopTimeoutBehaviorResolver()
        .resolveTimeoutBehaviorForSubType(nonStopConfigurationLookup.getObjectType(), nonStopConfiguration, klazz);
  }

  protected Object createNonStopSubtypeIfNecessary(Object returnValue, Class klazzParam) {
    if (NonStopSubTypeUtil.isNonStopSubtype(klazzParam)) {
      return ToolkitInstanceProxy.newNonStopSubTypeProxy(nonStopConfigurationLookup, context, returnValue, klazzParam);
    } else {
      return returnValue;
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
