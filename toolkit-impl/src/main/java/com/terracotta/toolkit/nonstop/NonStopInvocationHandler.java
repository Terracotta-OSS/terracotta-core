/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonStopInvocationHandler<T extends ToolkitObject> implements InvocationHandler {
  private final NonStopContext             context;
  private final NonStopConfigurationLookup nonStopConfigurationLookup;
  private final ToolkitObjectLookup<T>     toolkitObjectLookup;

  public NonStopInvocationHandler(NonStopContext context, NonStopConfigurationLookup nonStopConfigurationLookup,
                                  ToolkitObjectLookup<T> toolkitObjectLookup) {
    this.context = context;
    this.nonStopConfigurationLookup = nonStopConfigurationLookup;
    this.toolkitObjectLookup = toolkitObjectLookup;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    NonStopConfiguration nonStopConfiguration = nonStopConfigurationLookup.getNonStopConfigurationForMethod(method
        .getName());

    if (!nonStopConfiguration.isEnabled()) { return invokeMethod(method, args,
                                                                 toolkitObjectLookup.getInitializedObject()); }

    if (LocalMethodUtil.isLocal(nonStopConfigurationLookup.getObjectType(), method.getName())) { return invokeLocalMethod(method,
                                                                                                                          args); }

    if (nonStopConfiguration.isImmediateTimeoutEnabled() && !context.getNonStopClusterListener().areOperationsEnabled()) { return invokeMethod(method,
                                                                                                                                               args,
                                                                                                                                               resolveTimeoutBehavior(nonStopConfiguration)); }
    boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
    try {
      context.getNonStopClusterListener().waitUntilOperationsEnabled();
      Object returnValue = invokeMethod(method, args, toolkitObjectLookup.getInitializedObject());
      return createNonStopSubtypeIfNecessary(returnValue, method.getReturnType());
    } catch (ToolkitAbortableOperationException e) {
      return invokeMethod(method, args, resolveTimeoutBehavior(nonStopConfiguration));
    } catch (RejoinException e) {
      // TODO: Review this.. Is this the right place to handle this...
      return invokeMethod(method, args, resolveTimeoutBehavior(nonStopConfiguration));
    } finally {
      if (started) {
        context.getNonStopManager().finish();
      }
    }
  }

  private Object invokeLocalMethod(Method method, Object[] args) throws Throwable {
    Object localDelegate = toolkitObjectLookup.getInitializedObjectOrNull();
    if (localDelegate == null) {
      localDelegate = resolveNoOpBehavior();
    }
    return invokeMethod(method, args, localDelegate);
  }

  private Object resolveNoOpBehavior() {
    return context.getNonstopTimeoutBehaviorResolver().resolveNoOpTimeoutBehavior(nonStopConfigurationLookup
                                                                                      .getObjectType());
  }

  private Object resolveTimeoutBehavior(NonStopConfiguration nonStopConfiguration) {
    return context.getNonstopTimeoutBehaviorResolver()
        .resolveTimeoutBehavior(nonStopConfigurationLookup.getObjectType(), nonStopConfiguration, toolkitObjectLookup);
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
