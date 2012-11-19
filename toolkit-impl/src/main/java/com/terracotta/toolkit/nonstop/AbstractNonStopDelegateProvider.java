/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfigFields.NonStopTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopConfigRegistry;
import org.terracotta.toolkit.object.ToolkitObject;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractNonStopDelegateProvider<T extends ToolkitObject> implements NonStopDelegateProvider<T> {

  private final NonStopConfigRegistry           nonStopConfigRegistry;
  private final NonstopTimeoutBehaviorResolver  behaviorResolver;
  private final String                         toolkitObjectName;
  private final AtomicReference<T>             delegate = new AtomicReference<T>();

  public AbstractNonStopDelegateProvider(NonStopConfigRegistryImpl nonStopConfigRegistry,
                                         NonstopTimeoutBehaviorResolver behaviorResolver, String toolkitObjectName) {
    this.nonStopConfigRegistry = nonStopConfigRegistry;
    this.behaviorResolver = behaviorResolver;
    this.toolkitObjectName = toolkitObjectName;
  }

  @Override
  public long getTimeout(String methodName) {
    return nonStopConfigRegistry.getConfigForInstanceMethod(methodName, toolkitObjectName, getTolkitObjectType())
        .getTimeout();
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
