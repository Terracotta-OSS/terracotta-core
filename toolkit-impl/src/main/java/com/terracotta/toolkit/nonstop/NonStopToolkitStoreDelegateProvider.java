/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;

import com.terracotta.toolkit.AsyncToolkitInitializer;

public class NonStopToolkitStoreDelegateProvider<V> extends AbstractNonStopDelegateProvider<ToolkitStore<String, V>> {

  private final String        name;
  private final Class<V>      klazz;
  private final Configuration actualConfiguration;

  public NonStopToolkitStoreDelegateProvider(NonStopConfigRegistryImpl nonStopConfigRegistry,
                                             NonstopTimeoutBehaviorResolver behaviorResolver,
                                             AsyncToolkitInitializer asyncToolkitInitializer, String name,
                                             Class<V> klazz, Configuration actualConfiguration) {
    super(asyncToolkitInitializer, nonStopConfigRegistry, behaviorResolver, name);
    this.name = name;
    this.actualConfiguration = actualConfiguration;
    this.klazz = klazz;
  }

  @Override
  public ToolkitStore<String, V> getToolkitObject() {
    return getToolkit().getStore(name, actualConfiguration, klazz);
  }

  @Override
  public ToolkitObjectType getToolkitObjectType() {
    return ToolkitObjectType.STORE;
  }

}
