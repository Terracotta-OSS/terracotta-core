/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.nonstop.NonStopConfigFields.NonStopTimeoutBehavior;

import com.terracotta.toolkit.collections.map.LocalReadsToolkitCacheImpl;

import java.lang.reflect.Proxy;

public class NonstopTimeoutBehaviorResolver {

  private final NoOpInvocationHandler               noOpInvocationHandler               = new NoOpInvocationHandler();
  private final ExceptionOnTimeoutInvocationHandler exceptionOnTimeoutInvocationHandler = new ExceptionOnTimeoutInvocationHandler();

  public <E> E create(ToolkitObjectType objectType, NonStopTimeoutBehavior nonStopBehavior, E e) {
    switch (nonStopBehavior) {
      case EXCEPTION_ON_TIMEOUT:
        return createExceptionOnTimeOutBehaviour(objectType, e);
      case NO_OP:
        return createNoOpBehaviour(objectType, e);
      case EXCEPTION_ON_MUTATE_AND_LOCAL_READS:
        return createLocalReadsBehaviour(objectType, e, createExceptionOnTimeOutBehaviour(objectType, e));
      case LOCAL_READS:
        return createLocalReadsBehaviour(objectType, e, createNoOpBehaviour(objectType, e));
    }
    return null;
  }

  private <E> E createNoOpBehaviour(ToolkitObjectType objectType, E delegate) {
    switch (objectType) {
      case CACHE:
      case STORE:
        return (E) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { delegate.getClass() },
                                          noOpInvocationHandler);
      case ATOMIC_LONG:
      case BARRIER:
      case BLOCKING_QUEUE:
      case LIST:
      case LOCK:
      case MAP:
      case NOTIFIER:
      case READ_WRITE_LOCK:
      case SET:
      case SORTED_MAP:
      case SORTED_SET:
        throw new UnsupportedOperationException("NonStop Not Supported for ToolkitType " + objectType);
    }
    return null;

  }

  private <E> E createExceptionOnTimeOutBehaviour(ToolkitObjectType objectType, E delegate) {

    switch (objectType) {
      case CACHE:
      case STORE:
        return (E) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { ToolkitCacheInternal.class },
                                          exceptionOnTimeoutInvocationHandler);
      case ATOMIC_LONG:
      case BARRIER:
      case BLOCKING_QUEUE:
      case LIST:
      case LOCK:
      case MAP:
      case NOTIFIER:
      case READ_WRITE_LOCK:
      case SET:
      case SORTED_MAP:
      case SORTED_SET:
        throw new UnsupportedOperationException("NonStop Not Supported for ToolkitType " + objectType);
    }
    return null;

  }

  private <E> E createLocalReadsBehaviour(ToolkitObjectType objectType, E delegate, E mutationBehaviour) {

    switch (objectType) {
      case CACHE:
      case STORE:
        return (E) new LocalReadsToolkitCacheImpl((ToolkitCacheInternal) delegate,
                                                  (ToolkitCacheInternal) mutationBehaviour);
      case ATOMIC_LONG:
      case BARRIER:
      case BLOCKING_QUEUE:
      case LIST:
      case LOCK:
      case MAP:
      case NOTIFIER:
      case READ_WRITE_LOCK:
      case SET:
      case SORTED_MAP:
      case SORTED_SET:
        throw new UnsupportedOperationException("NonStop Not Supported for ToolkitType " + objectType);
    }
    return null;
  }
}
