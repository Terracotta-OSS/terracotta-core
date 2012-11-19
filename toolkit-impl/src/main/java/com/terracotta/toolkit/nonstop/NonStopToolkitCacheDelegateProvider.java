/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

public class NonStopToolkitCacheDelegateProvider<V> extends AbstractNonStopDelegateProvider<ToolkitCache<String, V>> {

  private final String          name;
  private final ToolkitInternal toolkit;
  private final Class<V>        klazz;
  private final Configuration   actualConfiguration;

  public NonStopToolkitCacheDelegateProvider(NonStopConfigRegistryImpl nonStopConfigRegistry,
                                             NonstopTimeoutBehaviorResolver behaviorResolver, ToolkitInternal toolkit,
                                             String name, Class<V> klazz, Configuration actualConfiguration) {
    super(nonStopConfigRegistry, behaviorResolver, name);
    this.toolkit = toolkit;
    this.name = name;
    this.actualConfiguration = actualConfiguration;
    this.klazz = klazz;
  }

  @Override
  public ToolkitCache<String, V> getToolkitObject() {
    return toolkit.getCache(name, actualConfiguration, klazz);
  }

  @Override
  public ToolkitObjectType getTolkitObjectType() {
    return ToolkitObjectType.CACHE;
  }

}
