/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonStopInvocationHandler<T> implements InvocationHandler {
  private final NonStopContext      context;
  private final ToolkitObjectType   toolkitObjectType;
  private final String              toolkitObjectName;
  private final ToolkitObjectLookup toolkitObjectLookup;

  public NonStopInvocationHandler(NonStopContext context, ToolkitObjectType toolkitObjectType,
                                  String toolkitObjectName, ToolkitObjectLookup toolkitObjectLookup) {
    this.context = context;
    this.toolkitObjectName = toolkitObjectName;
    this.toolkitObjectType = toolkitObjectType;
    this.toolkitObjectLookup = toolkitObjectLookup;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    NonStopConfiguration nonStopConfiguration = context.getNonStopConfigurationRegistry()
        .getConfigForInstanceMethod(method.getName(), toolkitObjectName, toolkitObjectType);

    if (!nonStopConfiguration.isEnabled()) { return invokeMethod(method, args,
                                                                 toolkitObjectLookup.getInitializedObject()); }

    if (LocalMethodUtil.isLocal(toolkitObjectType, method.getName())) { return invokeLocalMethod(method, args); }

    if (nonStopConfiguration.isImmediateTimeoutEnabled() && !context.getNonStopClusterListener().areOperationsEnabled()) { return invokeMethod(method,
                                                                                                                                               args,
                                                                                                                                               resolveTimeoutBehavior(nonStopConfiguration)); }
    boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
    try {
      context.getNonStopClusterListener().waitUntilOperationsEnabled();
      Object returnValue = invokeMethod(method, args, toolkitObjectLookup.getInitializedObject());
      return workOnReturnValue(returnValue);
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
    return context.getNonstopTimeoutBehaviorResolver().resolveNoOpTimeoutBehavior(toolkitObjectType);
  }

  private Object resolveTimeoutBehavior(NonStopConfiguration nonStopConfiguration) {
    return context.getNonstopTimeoutBehaviorResolver().resolveTimeoutBehavior(toolkitObjectType, nonStopConfiguration,
                                                                              toolkitObjectLookup);
  }

  protected Object workOnReturnValue(Object returnValue) {
    return returnValue;
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
