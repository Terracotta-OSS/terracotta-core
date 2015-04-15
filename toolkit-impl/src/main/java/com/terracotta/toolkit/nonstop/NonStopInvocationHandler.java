/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.nonstop.NonStopToolkitInstantiationException;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonStopInvocationHandler<T extends ToolkitObject> implements InvocationHandler {
  private static final TCLogger            LOGGER = TCLogging.getLogger(NonStopInvocationHandler.class);
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

    if (!nonStopConfiguration.isEnabled()) {
      Object returnValue = invokeMethod(method, args, toolkitObjectLookup.getInitializedObject());
      return createNonStopSubtypeIfNecessary(returnValue, method.getReturnType());
    }

    if (LocalMethodUtil.isLocal(nonStopConfigurationLookup.getObjectType(), method.getName())) {
      return invokeLocalMethod(method, args);
    }

    if (nonStopConfiguration.isImmediateTimeoutEnabled() && !context.getNonStopClusterListener().areOperationsEnabled()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Executing NonStop behaviour for method - " + method.getName());
      }
      return handleNonStopBehavior(method, args, nonStopConfiguration, null);
    }
    
    boolean started = context.getNonStopManager().tryBegin(getTimeout(nonStopConfiguration));
    try {
      context.getNonStopClusterListener().waitUntilOperationsEnabled();
      Object returnValue = invokeMethod(method, args, toolkitObjectLookup.getInitializedObject());
      return createNonStopSubtypeIfNecessary(returnValue, method.getReturnType());
    } catch (NonStopToolkitInstantiationException e) {
      LOGGER.error(nonStopConfigurationLookup.getObjectType().name() + " instantiation failed.", e);
      return handleNonStopToolkitInstantiationException(method, args, nonStopConfiguration, e);
    } catch (ToolkitAbortableOperationException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Invocation failed for method - " + method.getName() + ". Exception occurred - " + e.getMessage());
      }
      return handleNonStopBehavior(method, args, nonStopConfiguration, e);
    } catch (RejoinException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Invocation failed for method - " + method.getName() + ". Exception occurred - " + e.getMessage());
      }
      // TODO: Review this.. Is this the right place to handle this...
      return handleNonStopBehavior(method, args, nonStopConfiguration, e);
    } finally {
      if (started) {
        context.getNonStopManager().finish();
      }
    }
  }

  private Object handleNonStopToolkitInstantiationException(Method method, Object[] args,
        NonStopConfiguration nonStopConfiguration, NonStopToolkitInstantiationException e) throws Throwable {
    try {
      return invokeMethod(method, args, resolveTimeoutBehavior(nonStopConfiguration));
    } catch (NonStopException nse) {
        throw new NonStopException(e.getMessage(), e);
    }
  }

  private Object handleNonStopBehavior(Method method, Object[] args, NonStopConfiguration nonStopConfiguration,
                                       Exception expection) throws Throwable {
    try {
      return invokeMethod(method, args, resolveTimeoutBehavior(nonStopConfiguration));
    } catch (NonStopException e) {
      if(context.getNonStopClusterListener().isNodeError()) {
        throw new NonStopException(context.getNonStopClusterListener().getNodeErrorMessage());
      } else {
        throw new NonStopException(e.getMessage(), expection);
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

  Object resolveTimeoutBehavior(NonStopConfiguration nonStopConfiguration) {
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

  Object invokeMethod(Method method, Object[] args, Object object) throws Throwable {
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
