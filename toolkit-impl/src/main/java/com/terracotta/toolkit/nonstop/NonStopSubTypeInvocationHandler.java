/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopException;
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

    if (!nonStopConfiguration.isEnabled()) {
      Object returnValue = invokeMethod(method, args, delegate);
      return createNonStopSubtypeIfNecessary(returnValue, method.getReturnType());
    }

    if (nonStopConfiguration.isImmediateTimeoutEnabled() && !context.getNonStopClusterListener().areOperationsEnabled()) {
      return handleNonStopBehavior(method, args, nonStopConfiguration);
    }
    
    boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
    try {
      context.getNonStopClusterListener().waitUntilOperationsEnabled();
      Object returnValue = invokeMethod(method, args, delegate);
      return createNonStopSubtypeIfNecessary(returnValue, method.getReturnType());
    } catch (ToolkitAbortableOperationException e) {
      return handleNonStopBehavior(method, args, nonStopConfiguration);
    } catch (RejoinException e) {
      // TODO: Review this.. Is this the right place to handle this...
      return handleNonStopBehavior(method, args, nonStopConfiguration);
    } finally {
      if (started) {
        context.getNonStopManager().finish();
      }
    }
  }
  
  
  private Object handleNonStopBehavior(Method method, Object[] args, NonStopConfiguration nonStopConfiguration) throws Throwable {
    try {
      return invokeMethod(method, args, resolveTimeoutBehavior(nonStopConfiguration));
    } catch (NonStopException e) {
      if(context.getNonStopClusterListener().isNodeError()) {
        throw new NonStopException(context.getNonStopClusterListener().getNodeErrorMessage());
      } else {
        throw e;
      }
    }
  }

  Object resolveTimeoutBehavior(NonStopConfiguration nonStopConfiguration) {
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
