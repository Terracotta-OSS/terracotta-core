/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.abortable.AbortableOperationManager;

import java.util.concurrent.FutureTask;

public class NonStopToolkitCacheDelegateProvider<V> extends AbstractNonStopDelegateProvider<ToolkitCache<String, V>> {

  private final String                      name;
  private final Class<V>                    klazz;
  private final Configuration               actualConfiguration;

  public NonStopToolkitCacheDelegateProvider(AbortableOperationManager abortableOperationManager,
                                             NonStopConfigRegistryImpl nonStopConfigRegistry,
                                             NonstopTimeoutBehaviorResolver behaviorResolver,
                                             FutureTask<ToolkitInternal> toolkitDelegateFutureTask, String name,
                                             Class<V> klazz, Configuration actualConfiguration) {
    super(toolkitDelegateFutureTask, abortableOperationManager, nonStopConfigRegistry, behaviorResolver, name);
    this.name = name;
    this.actualConfiguration = actualConfiguration;
    this.klazz = klazz;
  }

  @Override
  public ToolkitCache<String, V> getToolkitObject() {
    return getToolkit().getCache(name, actualConfiguration, klazz);
  }

  @Override
  public ToolkitObjectType getToolkitObjectType() {
    return ToolkitObjectType.CACHE;
  }

}
