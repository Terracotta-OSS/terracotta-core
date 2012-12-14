/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.AsyncToolkitInitializer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractNonStopDelegateProvider<T extends ToolkitObject> implements NonStopDelegateProvider<T> {

  private final NonStopConfigurationRegistry   nonStopConfigRegistry;
  private final NonstopTimeoutBehaviorResolver behaviorResolver;
  private final String                         toolkitObjectName;
  private final AsyncToolkitInitializer        asyncToolkitInitializer;
  private final AtomicReference<T>             delegate  = new AtomicReference<T>();

  private final ConcurrentMap<Wrapper, T>      behaviors = new ConcurrentHashMap<Wrapper, T>();

  public AbstractNonStopDelegateProvider(AsyncToolkitInitializer asyncToolkitInitializer,
                                         NonStopConfigRegistryImpl nonStopConfigRegistry,
                                         NonstopTimeoutBehaviorResolver behaviorResolver, String toolkitObjectName) {
    this.asyncToolkitInitializer = asyncToolkitInitializer;
    this.nonStopConfigRegistry = nonStopConfigRegistry;
    this.behaviorResolver = behaviorResolver;
    this.toolkitObjectName = toolkitObjectName;
  }

  protected ToolkitInternal getToolkit() {
    return this.asyncToolkitInitializer.getToolkit();
  }

  @Override
  public NonStopConfiguration getNonStopConfiguration(String methodName) {
    return nonStopConfigRegistry.getConfigForInstanceMethod(methodName, toolkitObjectName, getToolkitObjectType());
  }

  @Override
  public T getTimeoutBehavior() {
    NonStopConfiguration config = nonStopConfigRegistry.getConfigForInstance(toolkitObjectName, getToolkitObjectType());
    final NonStopConfigurationFields.NonStopReadTimeoutBehavior immutableBehavior = config
        .getReadOpNonStopTimeoutBehavior();
    final NonStopConfigurationFields.NonStopWriteTimeoutBehavior mutableBehavior = config
        .getWriteOpNonStopTimeoutBehavior();
    Wrapper wrapper = new Wrapper(immutableBehavior, mutableBehavior);

    T t = behaviors.get(wrapper);
    if (t == null) {
      t = behaviorResolver.create(getToolkitObjectType(), immutableBehavior, mutableBehavior, delegate);
      T old = behaviors.putIfAbsent(wrapper, t);
      t = old == null ? t : old;
    }
    return t;
  }

  @Override
  public T getDelegate() {
    if (delegate.get() == null) {
      delegate.set(getToolkitObject());
    }
    return delegate.get();
  }

  public abstract T getToolkitObject();

  public abstract ToolkitObjectType getToolkitObjectType();

  private static class Wrapper {
    private final NonStopConfigurationFields.NonStopReadTimeoutBehavior  immutableBehavior;
    private final NonStopConfigurationFields.NonStopWriteTimeoutBehavior mutableBehavior;

    public Wrapper(NonStopConfigurationFields.NonStopReadTimeoutBehavior immutableBehavior,
                   NonStopConfigurationFields.NonStopWriteTimeoutBehavior mutableBehavior) {
      this.immutableBehavior = immutableBehavior;
      this.mutableBehavior = mutableBehavior;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((immutableBehavior == null) ? 0 : immutableBehavior.hashCode());
      result = prime * result + ((mutableBehavior == null) ? 0 : mutableBehavior.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Wrapper other = (Wrapper) obj;
      if (immutableBehavior != other.immutableBehavior) return false;
      if (mutableBehavior != other.mutableBehavior) return false;
      return true;
    }

  }
}
