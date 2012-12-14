/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.toolkit.AsyncToolkitInitializer;

public class NonstopToolkitReadWriteLockDelegateProvider extends AbstractNonStopDelegateProvider<ToolkitReadWriteLock> {

  private final String name;

  public NonstopToolkitReadWriteLockDelegateProvider(NonStopConfigRegistryImpl nonStopConfigRegistry,
                                                     NonstopTimeoutBehaviorResolver behaviorResolver,
                                                     AsyncToolkitInitializer asyncToolkitInitializer, String name) {
    super(asyncToolkitInitializer, nonStopConfigRegistry, behaviorResolver, name);
    this.name = name;
  }

  @Override
  public ToolkitReadWriteLock getToolkitObject() {
    return getToolkit().getReadWriteLock(name);
  }

  @Override
  public ToolkitObjectType getToolkitObjectType() {
    return ToolkitObjectType.READ_WRITE_LOCK;
  }

}
