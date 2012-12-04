/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;

import com.terracotta.toolkit.collections.map.LocalReadsToolkitCacheImpl;
import com.terracotta.toolkit.collections.map.TimeoutBehaviorToolkitCacheImpl;
import com.terracotta.toolkit.collections.map.ValuesResolver;
import com.terracotta.toolkit.type.DistributedToolkitType;

import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicReference;

public class NonstopTimeoutBehaviorResolver {

  private final EnumMap<ToolkitObjectType, Object> exceptionOnTimeoutBehaviorMap = new EnumMap<ToolkitObjectType, Object>(
                                                                                                                          ToolkitObjectType.class);
  private final EnumMap<ToolkitObjectType, Object> noOpOnTimeoutBehaviorMap      = new EnumMap<ToolkitObjectType, Object>(
                                                                                                                          ToolkitObjectType.class);

  public NonstopTimeoutBehaviorResolver() {
    Object cacheExceptionOnTimeOutBehavior = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {
                                                                        ToolkitCacheInternal.class,
                                                                        DistributedToolkitType.class,
                                                                        ValuesResolver.class },
                                                                    new ExceptionOnTimeoutInvocationHandler());
    exceptionOnTimeoutBehaviorMap.put(ToolkitObjectType.CACHE, cacheExceptionOnTimeOutBehavior);
    exceptionOnTimeoutBehaviorMap.put(ToolkitObjectType.STORE, cacheExceptionOnTimeOutBehavior);

    Object cacheNoOpOnTimeOutBehavior = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {
        ToolkitCacheInternal.class, DistributedToolkitType.class, ValuesResolver.class }, new NoOpInvocationHandler());
    noOpOnTimeoutBehaviorMap.put(ToolkitObjectType.CACHE, cacheNoOpOnTimeOutBehavior);
    noOpOnTimeoutBehaviorMap.put(ToolkitObjectType.STORE, cacheNoOpOnTimeOutBehavior);

    Object listExceptionOnTimeOutBehavior = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                                   new Class[] { ToolkitList.class },
                                                                   new ExceptionOnTimeoutInvocationHandler());
    exceptionOnTimeoutBehaviorMap.put(ToolkitObjectType.LIST, listExceptionOnTimeOutBehavior);

    Object listNoOpOnTimeOutBehavior = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                              new Class[] { ToolkitList.class },
                                                              new NoOpInvocationHandler());
    noOpOnTimeoutBehaviorMap.put(ToolkitObjectType.LIST, listNoOpOnTimeOutBehavior);

    Object lockExceptionOnTimeOutBehavior = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                                   new Class[] { ToolkitLock.class },
                                                                   new ExceptionOnTimeoutInvocationHandler());
    exceptionOnTimeoutBehaviorMap.put(ToolkitObjectType.LOCK, lockExceptionOnTimeOutBehavior);

    Object lockNoOpOnTimeOutBehavior = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                              new Class[] { ToolkitLock.class },
                                                              new NoOpInvocationHandler());
    noOpOnTimeoutBehaviorMap.put(ToolkitObjectType.LOCK, lockNoOpOnTimeOutBehavior);

    Object rwLockExceptionOnTimeOutBehavior = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                                     new Class[] { ToolkitReadWriteLock.class },
                                                                     new ExceptionOnTimeoutInvocationHandler());
    exceptionOnTimeoutBehaviorMap.put(ToolkitObjectType.READ_WRITE_LOCK, rwLockExceptionOnTimeOutBehavior);

    Object rwLockNoOpOnTimeOutBehavior = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                                new Class[] { ToolkitReadWriteLock.class },
                                                                new NoOpInvocationHandler());
    noOpOnTimeoutBehaviorMap.put(ToolkitObjectType.READ_WRITE_LOCK, rwLockNoOpOnTimeOutBehavior);

  }

  public <E> E create(ToolkitObjectType objectType,
                      NonStopConfigurationFields.NonStopReadTimeoutBehavior immutableOpBehavior,
                      NonStopConfigurationFields.NonStopWriteTimeoutBehavior mutableOpBehavior, AtomicReference<E> e) {
    if (objectType != ToolkitObjectType.CACHE && objectType != ToolkitObjectType.STORE) { throw new UnsupportedOperationException(); }

    E immutationBehaviourResolver = createImmutableOpBehavior(objectType, immutableOpBehavior, e);
    E mutationBehaviourResolver = createMutableOpBehavior(objectType, mutableOpBehavior, e);
    TimeoutBehaviorToolkitCacheImpl behaviorToolkitCacheImpl = new TimeoutBehaviorToolkitCacheImpl(
                                                                                                   (ToolkitCacheInternal) immutationBehaviourResolver,
                                                                                                   (ToolkitCacheInternal) mutationBehaviourResolver);
    return (E) behaviorToolkitCacheImpl;
  }

  private <E> E createMutableOpBehavior(ToolkitObjectType objectType,
                                        NonStopConfigurationFields.NonStopWriteTimeoutBehavior nonStopBehavior,
                                        AtomicReference<E> e) {
    switch (nonStopBehavior) {
      case EXCEPTION:
        return createExceptionOnTimeOutBehaviour(objectType, e.get());
      case NO_OP:
        return createNoOpBehaviour(objectType, e.get());
    }
    return null;
  }

  private <E> E createImmutableOpBehavior(ToolkitObjectType objectType,
                                          NonStopConfigurationFields.NonStopReadTimeoutBehavior nonStopBehavior,
                                          AtomicReference<E> e) {
    switch (nonStopBehavior) {
      case EXCEPTION:
        return createExceptionOnTimeOutBehaviour(objectType, e.get());
      case NO_OP:
        return createNoOpBehaviour(objectType, e.get());
      case LOCAL_READS:
        E noOpBehavior = createNoOpBehaviour(objectType, e.get());
        return createLocalReadsBehaviour(objectType, e, noOpBehavior);
    }
    return null;
  }

  private <E> E createNoOpBehaviour(ToolkitObjectType objectType, E e) {

    Object noOpOnTimeOutBehavior = noOpOnTimeoutBehaviorMap.get(objectType);
    if (noOpOnTimeOutBehavior == null) { throw new UnsupportedOperationException(
                                                                                 "NonStop Not Supported for ToolkitType "
                                                                                     + objectType); }
    return (E) noOpOnTimeOutBehavior;

  }

  private <E> E createExceptionOnTimeOutBehaviour(ToolkitObjectType objectType, E e) {

    Object exceptionOnTimeOutBehavior = exceptionOnTimeoutBehaviorMap.get(objectType);
    if (exceptionOnTimeOutBehavior == null) { throw new UnsupportedOperationException(
                                                                                      "NonStop Not Supported for ToolkitType "
                                                                                          + objectType); }
    return (E) exceptionOnTimeOutBehavior;

  }

  private <E> E createLocalReadsBehaviour(ToolkitObjectType objectType, AtomicReference<E> delegate, E noOpBehavior) {

    switch (objectType) {
      case CACHE:
      case STORE:
        return (E) new LocalReadsToolkitCacheImpl(delegate, (ToolkitCacheInternal) noOpBehavior);
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
        throw new UnsupportedOperationException("NonStop local reads Not Supported for ToolkitType " + objectType);
    }
    return null;
  }
}
