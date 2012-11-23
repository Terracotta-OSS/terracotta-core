/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.object.ToolkitObject;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractNonStopDelegateProvider<T extends ToolkitObject> implements NonStopDelegateProvider<T> {

  private final NonStopConfigurationRegistry   nonStopConfigRegistry;
  private final NonstopTimeoutBehaviorResolver behaviorResolver;
  private final String                         toolkitObjectName;
  private final FutureTask<ToolkitInternal>    toolkitDelegateFutureTask;
  private final AtomicReference<T>             delegate = new AtomicReference<T>();
  private final AbortableOperationManager      abortableOperationManager;

  public AbstractNonStopDelegateProvider(FutureTask<ToolkitInternal> toolkitDelegateFutureTask,
                                         AbortableOperationManager abortableOperationManager,
                                         NonStopConfigRegistryImpl nonStopConfigRegistry,
                                         NonstopTimeoutBehaviorResolver behaviorResolver, String toolkitObjectName) {
    this.toolkitDelegateFutureTask = toolkitDelegateFutureTask;
    this.abortableOperationManager = abortableOperationManager;
    this.nonStopConfigRegistry = nonStopConfigRegistry;
    this.behaviorResolver = behaviorResolver;
    this.toolkitObjectName = toolkitObjectName;
  }


  protected ToolkitInternal getToolkit() {
    try {
      return toolkitDelegateFutureTask.get();
    } catch (InterruptedException e) {
      handleInterruptedException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
    throw new AssertionError("Should not come here");
  }

  private void handleInterruptedException(InterruptedException e) {
    if (abortableOperationManager.isAborted()) { throw new ToolkitAbortableOperationException(); }
    throw new RuntimeException(e);
  }

  @Override
  public long getTimeout(String methodName) {
    NonStopConfiguration nonStopConfiguration = nonStopConfigRegistry.getConfigForInstanceMethod(methodName,
                                                                                                 toolkitObjectName,
                                                                                                 getTolkitObjectType());
    if (nonStopConfiguration.isEnabled()) {
      return nonStopConfiguration.getTimeoutMillis();
    } else {
      return -1;
    }
  }

  @Override
  public T getTimeoutBehavior() {
    NonStopTimeoutBehavior nonStopBehavior = nonStopConfigRegistry.getConfigForInstance(toolkitObjectName,
                                                                                        getTolkitObjectType())
        .getNonStopTimeoutBehavior();
    return behaviorResolver.create(getTolkitObjectType(), nonStopBehavior, delegate);
  }

  @Override
  public T getDelegate() {
    if (delegate.get() == null) {
      delegate.set(getToolkitObject());
    }
    return delegate.get();
  }

  public abstract T getToolkitObject();

  public abstract ToolkitObjectType getTolkitObjectType();
}
